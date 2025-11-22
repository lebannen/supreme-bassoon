package com.vocabee.service.external.gemini

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class GeminiTextClient(
    @Value("\${gemini.api.key}") private val apiKey: String,
    @Value("\${gemini.text.model:gemini-2.5-flash}") private val model: String,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val webClient: WebClient = WebClient.builder()
        .baseUrl("https://generativelanguage.googleapis.com")
        .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
        .build()

    fun generateText(prompt: String, mimeType: String = "text/plain"): String {
        if (apiKey.isBlank()) {
            throw RuntimeException("Gemini API key not configured")
        }

        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to listOf(mapOf("text" to prompt))
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to 1.0,
                "topK" to 40,
                "topP" to 0.95,
                "maxOutputTokens" to 8192,
                "responseMimeType" to mimeType
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

            return parseResponse(response)
        } catch (e: Exception) {
            logger.error("Failed to generate text from Gemini", e)
            throw RuntimeException("Gemini text generation failed: ${e.message}", e)
        }
    }

    private fun parseResponse(jsonResponse: String): String {
        val root = objectMapper.readTree(jsonResponse)
        val candidates = root.path("candidates")
        if (candidates.isArray && candidates.size() > 0) {
            val content = candidates[0].path("content")
            val parts = content.path("parts")
            if (parts.isArray && parts.size() > 0) {
                return parts[0].path("text").asText()
            }
        }
        throw RuntimeException("No text found in Gemini response")
    }
}
