package com.vocabee.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.model.FileType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.io.ByteArrayInputStream
import java.util.*

data class SpeakerVoiceConfig(
    val name: String,
    val voice: String
)

interface AudioGenerationService {
    /**
     * Generate audio from text and return the public URL
     * @param speakers List of speakers for multi-speaker dialogues (max 2)
     * @param stylePrompt Optional prompt to guide the style/emotion of the speech
     */
    fun generateAudio(
        transcript: String,
        languageCode: String,
        moduleNumber: Int,
        voice: String = "Leda",
        speakers: List<SpeakerVoiceConfig> = emptyList(),
        stylePrompt: String? = null
    ): String

    /**
     * Sanitize transcript text to create valid filename
     */
    fun sanitizeFilename(text: String): String
}

@Service
class GeminiAudioGenerationService(
    @Value("\${gemini.api.key}") private val apiKey: String,
    @Value("\${gemini.tts.model:gemini-2.5-flash-preview-tts}") private val model: String,
    private val storageService: StorageService,
    private val objectMapper: ObjectMapper
) : AudioGenerationService {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val webClient: WebClient = WebClient.builder()
        .baseUrl("https://generativelanguage.googleapis.com")
        .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) } // 10MB buffer
        .build()

    override fun generateAudio(
        transcript: String,
        languageCode: String,
        moduleNumber: Int,
        voice: String,
        speakers: List<SpeakerVoiceConfig>,
        stylePrompt: String?
    ): String {
        // Check if API key is configured
        if (apiKey.isBlank()) {
            throw RuntimeException("Gemini API key not configured. Please set GEMINI_API_KEY environment variable.")
        }

        // Multi-speaker API requires exactly 2 speakers
        // For 0 or 1 speakers, use single-speaker API
        val useMultiSpeaker = speakers.size == 2
        logger.info("Generating audio for transcript (length: ${transcript.length}) - multi-speaker: $useMultiSpeaker, speakers: ${speakers.size}")

        try {
            // Call Gemini TTS API
            val (pcmData, sampleRate, bitsPerSample) = if (useMultiSpeaker) {
                callGeminiTTSMultiSpeaker(transcript, speakers, stylePrompt)
            } else {
                // Use single-speaker API with first speaker's voice, or default voice parameter
                val singleVoice = speakers.firstOrNull()?.voice ?: voice
                callGeminiTTS(transcript, singleVoice, stylePrompt)
            }

            // Convert PCM to WAV
            val wavData = convertPcmToWav(pcmData, sampleRate, bitsPerSample)

            // Generate filename
            val filename = "${sanitizeFilename(transcript)}.wav"
            val folder = "$languageCode/module_$moduleNumber"

            // Upload to MinIO
            val uploadResult = storageService.uploadFile(
                inputStream = ByteArrayInputStream(wavData),
                originalFilename = filename,
                contentType = "audio/wav",
                fileType = FileType.AUDIO,
                folder = folder
            )

            logger.info("Audio generated and uploaded successfully: ${uploadResult.url} (${uploadResult.size} bytes)")
            return uploadResult.url

        } catch (e: Exception) {
            logger.error("Failed to generate audio for transcript: '$transcript'", e)
            throw RuntimeException("Audio generation failed: ${e.message}", e)
        }
    }

    private fun callGeminiTTS(text: String, voice: String, stylePrompt: String? = null): Triple<ByteArray, Int, Int> {
        // Combine style prompt with text if provided
        val fullText = if (stylePrompt != null) {
            "$stylePrompt\n\n$text"
        } else {
            text
        }

        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to listOf(
                        mapOf("text" to fullText)
                    )
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to 1.0,
                "responseModalities" to listOf("AUDIO"),
                "speechConfig" to mapOf(
                    "voiceConfig" to mapOf(
                        "prebuiltVoiceConfig" to mapOf(
                            "voiceName" to voice
                        )
                    )
                )
            )
        )

        try {
            // Make API call
            val response = webClient.post()
                .uri("/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus({ status -> status.is4xxClientError || status.is5xxServerError }) { clientResponse ->
                    clientResponse.bodyToMono(String::class.java)
                        .map { errorBody ->
                            logger.error("Gemini API error response: $errorBody")
                            RuntimeException("Gemini API error: $errorBody")
                        }
                }
                .bodyToMono(String::class.java)
                .block() ?: throw RuntimeException("Empty response from Gemini API")

            // Parse SSE response
            val (audioData, mimeType) = parseSSEResponse(response)

            // Parse audio parameters from MIME type
            logger.debug("Received MIME type from Gemini: $mimeType")
            val params = parseAudioMimeType(mimeType)
            logger.debug("Parsed audio parameters: $params")

            val sampleRate = params["rate"] ?: throw RuntimeException("Missing 'rate' in MIME type: $mimeType")
            val bitsPerSample = params["bits_per_sample"] ?: throw RuntimeException("Missing 'bits_per_sample' in MIME type: $mimeType")

            return Triple(audioData, sampleRate, bitsPerSample)

        } catch (e: Exception) {
            logger.error("Gemini API call failed", e)
            throw RuntimeException("Failed to call Gemini TTS API: ${e.message}", e)
        }
    }

    private fun callGeminiTTSMultiSpeaker(
        text: String,
        speakers: List<SpeakerVoiceConfig>,
        stylePrompt: String?
    ): Triple<ByteArray, Int, Int> {
        // Build multi-speaker voice configuration
        // Note: Gemini expects speaker labels to be simple like "A" and "B" or "Speaker 1" and "Speaker 2"
        val speakerLabels = listOf("A", "B")
        val speakerConfigs = speakers.take(2).mapIndexed { index, speaker ->
            mapOf(
                "speaker" to speakerLabels[index],
                "voiceConfig" to mapOf(
                    "prebuiltVoiceConfig" to mapOf(
                        "voiceName" to speaker.voice
                    )
                )
            )
        }

        // Normalize dialogue text to use standardized speaker labels
        // Replace original speaker names with "A:" and "B:"
        var normalizedText = text
        speakers.take(2).forEachIndexed { index, speaker ->
            // Match both "Speaker Name:" and "Speaker Name :" patterns (with optional colon variations)
            normalizedText = normalizedText.replace(
                Regex("${Regex.escape(speaker.name)}\\s*:", RegexOption.IGNORE_CASE),
                "${speakerLabels[index]}:"
            )
        }

        // Combine style prompt with the normalized dialogue text
        val fullText = if (stylePrompt != null) {
            "$stylePrompt\n\n$normalizedText"
        } else {
            normalizedText
        }

        logger.info("Normalized dialogue text for Gemini multi-speaker TTS")
        logger.debug("Original text length: ${text.length}, Normalized text length: ${normalizedText.length}")

        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to listOf(
                        mapOf("text" to fullText)
                    )
                )
            ),
            "generationConfig" to mapOf(
                "responseModalities" to listOf("AUDIO"),
                "speechConfig" to mapOf(
                    "multiSpeakerVoiceConfig" to mapOf(
                        "speakerVoiceConfigs" to speakerConfigs
                    )
                )
            )
        )

        try {
            logger.info("Calling Gemini TTS with ${speakers.size} speakers: ${speakers.map { it.name to it.voice }}")

            // Make API call
            val response = webClient.post()
                .uri("/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus({ status -> status.is4xxClientError || status.is5xxServerError }) { clientResponse ->
                    clientResponse.bodyToMono(String::class.java)
                        .map { errorBody ->
                            logger.error("Gemini API error response: $errorBody")
                            RuntimeException("Gemini API error: $errorBody")
                        }
                }
                .bodyToMono(String::class.java)
                .block() ?: throw RuntimeException("Empty response from Gemini API")

            // Parse SSE response
            val (audioData, mimeType) = parseSSEResponse(response)

            // Parse audio parameters from MIME type
            logger.debug("Received MIME type from Gemini: $mimeType")
            val params = parseAudioMimeType(mimeType)
            logger.debug("Parsed audio parameters: $params")

            val sampleRate = params["rate"] ?: throw RuntimeException("Missing 'rate' in MIME type: $mimeType")
            val bitsPerSample = params["bits_per_sample"] ?: throw RuntimeException("Missing 'bits_per_sample' in MIME type: $mimeType")

            return Triple(audioData, sampleRate, bitsPerSample)

        } catch (e: Exception) {
            logger.error("Gemini multi-speaker API call failed", e)
            throw RuntimeException("Failed to call Gemini multi-speaker TTS API: ${e.message}", e)
        }
    }

    private fun parseSSEResponse(sseResponse: String): Pair<ByteArray, String> {
        // Parse Server-Sent Events format
        // Format: data: {...}\n\ndata: {...}\n\n
        val audioChunks = mutableListOf<ByteArray>()
        var mimeType = ""

        sseResponse.split("\n\n").forEach { event ->
            if (event.startsWith("data: ")) {
                val jsonData = event.removePrefix("data: ").trim()
                if (jsonData.isNotEmpty() && jsonData != "[DONE]") {
                    try {
                        val json = objectMapper.readTree(jsonData)

                        // Extract audio data from candidates[0].content.parts[0].inlineData
                        val candidates = json.get("candidates")
                        if (candidates != null && candidates.isArray && candidates.size() > 0) {
                            val content = candidates[0].get("content")
                            val parts = content?.get("parts")

                            if (parts != null && parts.isArray && parts.size() > 0) {
                                val inlineData = parts[0].get("inlineData")
                                val base64Data = inlineData?.get("data")?.asText()
                                val mime = inlineData?.get("mimeType")?.asText()

                                if (base64Data != null) {
                                    val audioBytes = Base64.getDecoder().decode(base64Data)
                                    audioChunks.add(audioBytes)
                                }

                                if (mime != null && mimeType.isEmpty()) {
                                    mimeType = mime
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to parse SSE event: ${e.message}")
                    }
                }
            }
        }

        if (audioChunks.isEmpty()) {
            throw RuntimeException("No audio data found in API response")
        }

        // Combine all chunks
        val totalSize = audioChunks.sumOf { it.size }
        val combinedAudio = ByteArray(totalSize)
        var offset = 0

        audioChunks.forEach { chunk ->
            chunk.copyInto(combinedAudio, offset)
            offset += chunk.size
        }

        return Pair(combinedAudio, mimeType)
    }

    private fun parseAudioMimeType(mimeType: String): Map<String, Int> {
        // Parse MIME type like "audio/L16;codec=pcm;rate=24000"
        // L16 = Linear PCM 16-bit
        val params = mutableMapOf<String, Int>()

        val parts = mimeType.split(";")

        // Extract bits per sample from audio format (e.g., "audio/L16" -> 16 bits)
        val audioFormat = parts.firstOrNull()?.trim() ?: ""
        if (audioFormat.startsWith("audio/L")) {
            val bitsMatch = Regex("audio/L(\\d+)").find(audioFormat)
            if (bitsMatch != null) {
                params["bits_per_sample"] = bitsMatch.groupValues[1].toInt()
            }
        }

        // Parse remaining parameters
        parts.drop(1).forEach { part ->
            val trimmed = part.trim()
            if (trimmed.contains("=")) {
                val (key, value) = trimmed.split("=", limit = 2)
                try {
                    params[key.trim()] = value.trim().toInt()
                } catch (e: NumberFormatException) {
                    // Skip non-numeric parameters like codec=pcm
                    logger.debug("Skipping non-numeric parameter: $key=$value")
                }
            }
        }

        return params
    }

    private fun convertPcmToWav(pcmData: ByteArray, sampleRate: Int, bitsPerSample: Int): ByteArray {
        val numChannels = 1 // Mono
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = pcmData.size

        // WAV file header (44 bytes)
        val header = ByteArray(44)
        var offset = 0

        // RIFF header
        "RIFF".toByteArray().copyInto(header, offset)
        offset += 4

        // File size - 8
        writeInt(header, offset, 36 + dataSize)
        offset += 4

        // WAVE header
        "WAVE".toByteArray().copyInto(header, offset)
        offset += 4

        // fmt subchunk
        "fmt ".toByteArray().copyInto(header, offset)
        offset += 4

        // Subchunk1 size (16 for PCM)
        writeInt(header, offset, 16)
        offset += 4

        // Audio format (1 for PCM)
        writeShort(header, offset, 1)
        offset += 2

        // Number of channels
        writeShort(header, offset, numChannels)
        offset += 2

        // Sample rate
        writeInt(header, offset, sampleRate)
        offset += 4

        // Byte rate
        writeInt(header, offset, byteRate)
        offset += 4

        // Block align
        writeShort(header, offset, blockAlign)
        offset += 2

        // Bits per sample
        writeShort(header, offset, bitsPerSample)
        offset += 2

        // data subchunk
        "data".toByteArray().copyInto(header, offset)
        offset += 4

        // Data size
        writeInt(header, offset, dataSize)

        // Combine header and PCM data
        return header + pcmData
    }

    private fun writeInt(array: ByteArray, offset: Int, value: Int) {
        array[offset] = (value and 0xFF).toByte()
        array[offset + 1] = ((value shr 8) and 0xFF).toByte()
        array[offset + 2] = ((value shr 16) and 0xFF).toByte()
        array[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShort(array: ByteArray, offset: Int, value: Int) {
        array[offset] = (value and 0xFF).toByte()
        array[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    override fun sanitizeFilename(text: String): String {
        // Convert to lowercase
        var sanitized = text.lowercase()

        // Remove special characters except spaces
        sanitized = sanitized.replace(Regex("[^a-z0-9\\s]"), "")

        // Replace spaces with underscores
        sanitized = sanitized.replace(Regex("\\s+"), "_")

        // Remove multiple underscores
        sanitized = sanitized.replace(Regex("_+"), "_")

        // Trim underscores from ends
        sanitized = sanitized.trim('_')

        // Truncate if too long
        if (sanitized.length > 50) {
            sanitized = sanitized.substring(0, 50)
        }

        return sanitized
    }
}
