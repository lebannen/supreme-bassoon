package com.vocabee.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.repository.ExerciseRepository
import com.vocabee.web.dto.SpeakingAttemptResult
import com.vocabee.web.dto.SpeakingExerciseContent
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Service
class SpeakingExerciseService(
    @Value("\${gemini.api.key}") private val apiKey: String,
    @Value("\${gemini.text.model:gemini-3-pro-preview}") private val model: String,
    private val exerciseRepository: ExerciseRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val webClient: WebClient = WebClient.builder()
        .baseUrl("https://generativelanguage.googleapis.com")
        .codecs { it.defaultCodecs().maxInMemorySize(20 * 1024 * 1024) } // 20MB buffer for audio
        .build()

    /**
     * Validate a speaking exercise attempt by transcribing the audio and comparing to expected text
     */
    fun validateSpeaking(
        exerciseId: Long,
        audioFile: MultipartFile,
        userId: Long
    ): SpeakingAttemptResult {
        val exercise = exerciseRepository.findById(exerciseId)
            .orElseThrow { EntityNotFoundException("Exercise not found: $exerciseId") }

        // Parse the exercise content to get expected text and language
        val content = parseExerciseContent(exercise.content)

        logger.info("Validating speaking attempt for exercise $exerciseId, user $userId, expected: '${content.targetText}'")

        // Determine MIME type from file
        val mimeType = audioFile.contentType ?: detectMimeType(audioFile.originalFilename)

        // Call Gemini to transcribe and validate
        return transcribeAndValidate(
            audioData = audioFile.bytes,
            mimeType = mimeType,
            targetLanguage = content.targetLanguage,
            expectedText = content.targetText,
            acceptableVariations = content.acceptableVariations
        )
    }

    /**
     * Validate a speaking attempt with explicit expected text (for non-exercise contexts like vocabulary practice)
     */
    fun validateSpeakingDirect(
        audioFile: MultipartFile,
        expectedText: String,
        targetLanguage: String,
        acceptableVariations: List<String> = emptyList()
    ): SpeakingAttemptResult {
        val mimeType = audioFile.contentType ?: detectMimeType(audioFile.originalFilename)

        return transcribeAndValidate(
            audioData = audioFile.bytes,
            mimeType = mimeType,
            targetLanguage = targetLanguage,
            expectedText = expectedText,
            acceptableVariations = acceptableVariations
        )
    }

    private fun transcribeAndValidate(
        audioData: ByteArray,
        mimeType: String,
        targetLanguage: String,
        expectedText: String,
        acceptableVariations: List<String>
    ): SpeakingAttemptResult {
        if (apiKey.isBlank()) {
            throw RuntimeException("Gemini API key not configured")
        }

        val base64Audio = Base64.getEncoder().encodeToString(audioData)
        val languageName = getLanguageName(targetLanguage)

        // Build acceptable variations text for the prompt
        val variationsText = if (acceptableVariations.isNotEmpty()) {
            "\n\nAcceptable variations (also count as correct):\n" +
                    acceptableVariations.joinToString("\n") { "- \"$it\"" }
        } else ""

        val prompt = """
            You are a STRICT $languageName pronunciation coach evaluating a student's speech.

            The student was asked to say: "$expectedText"$variationsText

            Listen carefully to the audio and evaluate BOTH correctness AND pronunciation quality.

            Respond in this exact JSON format:
            {
              "transcription": "what the student actually said",
              "accuracy": 75,
              "isCorrect": true,
              "feedback": "specific feedback about pronunciation"
            }

            STRICT scoring rules - be critical, not generous:
            - 95-100%: Native-like pronunciation, perfect rhythm and intonation (RARE - reserve for truly excellent)
            - 85-94%: Very good, minor accent but natural flow
            - 70-84%: Understandable, noticeable accent or hesitation
            - 50-69%: Understandable but heavy accent, wrong stress, or choppy delivery
            - Below 50%: Difficult to understand or wrong words

            IMPORTANT GUIDELINES:
            - Do NOT give 95%+ unless pronunciation is truly native-like
            - A correct transcription does NOT mean perfect pronunciation
            - Listen for: vowel sounds, consonant clarity, rhythm, stress, liaison (for French), umlauts (for German)
            - ALWAYS provide specific, actionable feedback about what to improve
            - Even for good attempts, mention ONE specific thing to work on
            - Be encouraging but honest - learners need real feedback to improve

            Consider "isCorrect" as true if accuracy >= 70% (words are understandable, even if accent needs work).

            Example good feedback:
            - "Good! Try making the 'r' sound more guttural, from the back of your throat."
            - "The words are correct. Work on linking 'je suis' together more smoothly."
            - "Nice try! The 'u' in 'tu' should be more rounded - purse your lips tighter."
            - "Clear pronunciation! Focus on the nasal sound in 'bon' - let air flow through your nose."

            If you cannot understand the audio or it's silent, set accuracy to 0 and explain in feedback.
        """.trimIndent()

        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to listOf(
                        mapOf("text" to prompt),
                        mapOf(
                            "inlineData" to mapOf(
                                "mimeType" to mimeType,
                                "data" to base64Audio
                            )
                        )
                    )
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to 0.2,  // Lower for more consistent evaluation
                "responseMimeType" to "application/json"
            )
        )

        try {
            val response = webClient.post()
                .uri("/v1beta/models/$model:generateContent?key=$apiKey")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String::class.java)
                .block() ?: throw RuntimeException("Empty response from Gemini API")

            return parseGeminiResponse(response)

        } catch (e: Exception) {
            logger.error("Failed to transcribe audio with Gemini", e)
            throw RuntimeException("Speech recognition failed: ${e.message}", e)
        }
    }

    private fun parseGeminiResponse(jsonResponse: String): SpeakingAttemptResult {
        try {
            val root = objectMapper.readTree(jsonResponse)
            val candidates = root.path("candidates")

            if (candidates.isArray && candidates.size() > 0) {
                val content = candidates[0].path("content")
                val parts = content.path("parts")

                if (parts.isArray && parts.size() > 0) {
                    val text = parts[0].path("text").asText()

                    // Parse the JSON response from Gemini
                    val resultJson = objectMapper.readTree(text)

                    val attemptResult = SpeakingAttemptResult(
                        transcription = resultJson.path("transcription").asText(""),
                        accuracy = resultJson.path("accuracy").asInt(0),
                        isCorrect = resultJson.path("isCorrect").asBoolean(false),
                        feedback = resultJson.path("feedback").asText("Unable to evaluate your pronunciation.")
                    )

                    logger.info(
                        "Speaking result - Accuracy: ${attemptResult.accuracy}%, Correct: ${attemptResult.isCorrect}, " +
                                "Transcription: '${attemptResult.transcription}', Feedback: '${attemptResult.feedback}'"
                    )

                    return attemptResult
                }
            }

            logger.error("Unexpected Gemini response structure: $jsonResponse")
            throw RuntimeException("Invalid response structure from Gemini API")

        } catch (e: Exception) {
            logger.error("Failed to parse Gemini response: $jsonResponse", e)
            throw RuntimeException("Failed to parse speech recognition result: ${e.message}", e)
        }
    }

    private fun parseExerciseContent(contentNode: com.fasterxml.jackson.databind.JsonNode): SpeakingExerciseContent {
        return SpeakingExerciseContent(
            mode = contentNode.path("mode").asText("read_aloud"),
            targetText = contentNode.path("targetText").asText(),
            targetLanguage = contentNode.path("targetLanguage").asText("fr"),
            nativeAudioUrl = contentNode.path("nativeAudioUrl").asText(null),
            prompt = contentNode.path("prompt").asText(null),
            promptAudioUrl = contentNode.path("promptAudioUrl").asText(null),
            hint = contentNode.path("hint").asText(null),
            acceptableVariations = contentNode.path("acceptableVariations")
                .takeIf { it.isArray }
                ?.map { it.asText() }
                ?: emptyList()
        )
    }

    private fun detectMimeType(filename: String?): String {
        return when {
            filename == null -> "audio/webm"
            filename.endsWith(".webm") -> "audio/webm"
            filename.endsWith(".mp4") || filename.endsWith(".m4a") -> "audio/mp4"
            filename.endsWith(".wav") -> "audio/wav"
            filename.endsWith(".mp3") -> "audio/mp3"
            filename.endsWith(".ogg") -> "audio/ogg"
            else -> "audio/webm"
        }
    }

    private fun getLanguageName(languageCode: String): String {
        return when (languageCode.lowercase()) {
            "fr" -> "French"
            "en" -> "English"
            "de" -> "German"
            "es" -> "Spanish"
            "it" -> "Italian"
            "pt" -> "Portuguese"
            "nl" -> "Dutch"
            "ru" -> "Russian"
            "ja" -> "Japanese"
            "zh" -> "Chinese"
            "ko" -> "Korean"
            else -> languageCode
        }
    }
}
