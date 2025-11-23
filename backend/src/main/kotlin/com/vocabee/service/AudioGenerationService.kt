package com.vocabee.service

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

/**
 * Get voice configurations for a language
 * For multi-speaker dialogues, returns 2 different voices
 * speakerNames: actual speaker names from the dialogue (e.g., ["Sam", "Le Conservateur"])
 */
fun getVoicesForLanguage(
    languageCode: String,
    speakerNames: List<String> = listOf("Narrator")
): List<SpeakerVoiceConfig> {
    // If only one speaker requested, use single voice
    if (speakerNames.size == 1) {
        val voice = when (languageCode) {
            "fr" -> "Leda"  // French Female
            "en" -> "Puck"  // English Neutral
            "de" -> "Kore"  // German Female
            else -> "Leda"
        }
        return listOf(SpeakerVoiceConfig(speakerNames[0], voice))
    }

    // For multi-speaker (dialogue), use 2 different voices
    val (voice1, voice2) = when (languageCode) {
        "fr" -> Pair("Leda", "Charon")  // French Female + Male
        "en" -> Pair("Puck", "Charon")  // English Neutral + Male
        "de" -> Pair("Kore", "Charon")  // German Female + Male
        else -> Pair("Leda", "Charon")
    }

    return listOf(
        SpeakerVoiceConfig(speakerNames[0], voice1),
        SpeakerVoiceConfig(speakerNames.getOrElse(1) { "Speaker2" }, voice2)
    )
}

// Backwards compatibility
fun getVoiceForLanguage(languageCode: String): List<SpeakerVoiceConfig> {
    return getVoicesForLanguage(languageCode, listOf("Narrator"))
}

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
        speakers: List<SpeakerVoiceConfig>,
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
        speakers: List<SpeakerVoiceConfig>,
        stylePrompt: String?
    ): String {
        if (apiKey.isBlank()) {
            throw RuntimeException("Gemini API key not configured. Please set GEMINI_API_KEY environment variable.")
        }

        val useMultiSpeaker = speakers.size == 2
        logger.info("Generating audio for transcript (length: ${transcript.length}) - multi-speaker: $useMultiSpeaker, speakers: ${speakers.size}")

        try {
            val (pcmData, sampleRate, bitsPerSample) = if (useMultiSpeaker) {
                callGeminiTTSMultiSpeaker(transcript, speakers, stylePrompt)
            } else {
                val singleVoice = speakers.firstOrNull()?.voice ?: throw RuntimeException("No speakers provided")
                callGeminiTTS(transcript, singleVoice, stylePrompt)
            }

            val wavData = convertPcmToWav(pcmData, sampleRate, bitsPerSample)
            val filename = "${sanitizeFilename(transcript)}.wav"
            val folder = "$languageCode/module_$moduleNumber"

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
        val fullText = if (stylePrompt != null) "$stylePrompt\n\n$text" else text

        val requestBody = mapOf(
            "contents" to listOf(mapOf("role" to "user", "parts" to listOf(mapOf("text" to fullText)))),
            "generationConfig" to mapOf(
                "responseModalities" to listOf("AUDIO"),
                "speechConfig" to mapOf(
                    "voiceConfig" to mapOf(
                        "prebuiltVoiceConfig" to mapOf("voiceName" to voice)
                    )
                )
            )
        )
        return callGeminiAPI(requestBody, "single-speaker")
    }

    private fun callGeminiTTSMultiSpeaker(
        text: String,
        speakers: List<SpeakerVoiceConfig>,
        stylePrompt: String?
    ): Triple<ByteArray, Int, Int> {
        val speakerLabels = listOf("A", "B")
        val speakerConfigs = speakers.take(2).mapIndexed { index, speaker ->
            mapOf(
                "speaker" to speakerLabels[index],
                "voiceConfig" to mapOf("prebuiltVoiceConfig" to mapOf("voiceName" to speaker.voice))
            )
        }

        var normalizedText = text
        speakers.take(2).forEachIndexed { index, speaker ->
            normalizedText = normalizedText.replace(
                Regex("${Regex.escape(speaker.name)}\\s*:", RegexOption.IGNORE_CASE),
                "${speakerLabels[index]}:"
            )
        }

        val fullText = if (stylePrompt != null) "$stylePrompt\n\n$normalizedText" else normalizedText
        logger.info("Normalized dialogue text for Gemini multi-speaker TTS")
        logger.debug("Original text length: ${text.length}, Normalized text length: ${normalizedText.length}")

        val requestBody = mapOf(
            "contents" to listOf(mapOf("role" to "user", "parts" to listOf(mapOf("text" to fullText)))),
            "generationConfig" to mapOf(
                "responseModalities" to listOf("AUDIO"),
                "speechConfig" to mapOf("multiSpeakerVoiceConfig" to mapOf("speakerVoiceConfigs" to speakerConfigs))
            )
        )

        logger.info("Calling Gemini TTS with ${speakers.size} speakers: ${speakers.map { it.name to it.voice }}")
        return callGeminiAPI(requestBody, "multi-speaker")
    }

    private fun callGeminiAPI(requestBody: Map<String, Any>, logContext: String): Triple<ByteArray, Int, Int> {
        try {
            val response = webClient.post()
                .uri("/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus({ status -> status.is4xxClientError || status.is5xxServerError }) { clientResponse ->
                    clientResponse.bodyToMono(String::class.java)
                        .map { errorBody ->
                            logger.error("Gemini API error response ($logContext): $errorBody")
                            RuntimeException("Gemini API error ($logContext): $errorBody")
                        }
                }
                .bodyToMono(String::class.java)
                .block() ?: throw RuntimeException("Empty response from Gemini API ($logContext)")

            val (audioData, mimeType) = parseSSEResponse(response)
            logger.debug("Received MIME type from Gemini ($logContext): $mimeType")

            val params = parseAudioMimeType(mimeType)
            logger.debug("Parsed audio parameters ($logContext): $params")

            val sampleRate =
                params["rate"] ?: throw RuntimeException("Missing 'rate' in MIME type ($logContext): $mimeType")
            val bitsPerSample = params["bits_per_sample"]
                ?: throw RuntimeException("Missing 'bits_per_sample' in MIME type ($logContext): $mimeType")

            return Triple(audioData, sampleRate, bitsPerSample)

        } catch (e: Exception) {
            logger.error("Gemini API call failed ($logContext)", e)
            throw RuntimeException("Failed to call Gemini TTS API ($logContext): ${e.message}", e)
        }
    }

    private fun parseSSEResponse(sseResponse: String): Pair<ByteArray, String> {
        val audioChunks = mutableListOf<ByteArray>()
        var mimeType = ""

        sseResponse.split("\n\n").forEach { event ->
            if (event.startsWith("data: ")) {
                val jsonData = event.removePrefix("data: ").trim()
                if (jsonData.isNotEmpty() && jsonData != "[DONE]") {
                    try {
                        val json = objectMapper.readTree(jsonData)
                        val candidates = json.get("candidates")
                        if (candidates != null && candidates.isArray && candidates.size() > 0) {
                            val content = candidates[0].get("content")
                            val parts = content?.get("parts")
                            if (parts != null && parts.isArray && parts.size() > 0) {
                                val inlineData = parts[0].get("inlineData")
                                val base64Data = inlineData?.get("data")?.asText()
                                val mime = inlineData?.get("mimeType")?.asText()

                                if (base64Data != null) {
                                    audioChunks.add(Base64.getDecoder().decode(base64Data))
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
        val params = mutableMapOf<String, Int>()
        val parts = mimeType.split(";")
        val audioFormat = parts.firstOrNull()?.trim() ?: ""
        if (audioFormat.startsWith("audio/L")) {
            val bitsMatch = Regex("audio/L(\\d+)").find(audioFormat)
            if (bitsMatch != null) {
                params["bits_per_sample"] = bitsMatch.groupValues[1].toInt()
            }
        }

        parts.drop(1).forEach { part ->
            val trimmed = part.trim()
            if (trimmed.contains("=")) {
                val (key, value) = trimmed.split("=", limit = 2)
                try {
                    params[key.trim()] = value.trim().toInt()
                } catch (e: NumberFormatException) {
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
        val header = ByteArray(44)
        var offset = 0

        "RIFF".toByteArray().copyInto(header, offset); offset += 4
        writeInt(header, offset, 36 + dataSize); offset += 4
        "WAVE".toByteArray().copyInto(header, offset); offset += 4
        "fmt ".toByteArray().copyInto(header, offset); offset += 4
        writeInt(header, offset, 16); offset += 4
        writeShort(header, offset, 1); offset += 2
        writeShort(header, offset, numChannels); offset += 2
        writeInt(header, offset, sampleRate); offset += 4
        writeInt(header, offset, byteRate); offset += 4
        writeShort(header, offset, blockAlign); offset += 2
        writeShort(header, offset, bitsPerSample); offset += 2
        "data".toByteArray().copyInto(header, offset); offset += 4
        writeInt(header, offset, dataSize)

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
        var sanitized = text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')

        if (sanitized.length > 50) {
            sanitized = sanitized.substring(0, 50)
        }
        return sanitized
    }
}
