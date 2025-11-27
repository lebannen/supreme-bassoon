package com.vocabee.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

data class GeneratedImage(
    val imageData: ByteArray,
    val mimeType: String = "image/png"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GeneratedImage

        if (!imageData.contentEquals(other.imageData)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageData.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

@Service
class ImageGenerationService(
    private val objectMapper: ObjectMapper,
    @Value("\${gemini.api-key}") private val apiKey: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val httpClient: HttpClient = HttpClient.newBuilder().build()
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    // Model selection based on whether reference images are used
    private val basicModel = "gemini-2.5-flash-image" // Fast, cost-effective, 1024px resolution
    private val advancedModel = "gemini-3-pro-image-preview" // Supports reference images

    /**
     * Generate an image using Gemini's image generation API
     * @param prompt Detailed description of the image to generate
     * @param referenceImageUrls Optional list of reference image URLs (for character consistency)
     * @return Generated image as ByteArray (PNG format)
     */
    fun generateImage(prompt: String, referenceImageUrls: List<String> = emptyList()): GeneratedImage {
        logger.info("Generating image with prompt: ${prompt.take(100)}...")

        // First try with reference images if provided
        if (referenceImageUrls.isNotEmpty()) {
            logger.info("Attempting with ${referenceImageUrls.size} reference images for character consistency")
            try {
                return generateImageInternal(prompt, referenceImageUrls, advancedModel)
            } catch (e: Exception) {
                logger.warn("Generation with reference images failed, retrying without: ${e.message}")
            }
        }

        // Fallback: generate without reference images using basic model
        return generateImageInternal(prompt, emptyList(), basicModel)
    }

    private fun generateImageInternal(
        prompt: String,
        referenceImageUrls: List<String>,
        model: String
    ): GeneratedImage {
        val requestBody = buildRequestBody(prompt, referenceImageUrls)
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/models/$model:generateContent"))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            logger.error("Image generation failed with status ${response.statusCode()}: ${response.body()}")
            throw RuntimeException("Image generation failed: HTTP ${response.statusCode()}")
        }

        return parseImageResponse(response.body())
    }

    private fun buildRequestBody(prompt: String, referenceImageUrls: List<String> = emptyList()): String {
        val parts = mutableListOf<Map<String, Any>>()

        // Add text prompt first
        parts.add(mapOf("text" to prompt))

        // Add reference images if provided (up to 5 for Gemini 3 Pro)
        referenceImageUrls.take(5).forEach { imageUrl ->
            try {
                val imageBytes = fetchImageFromUrl(imageUrl)
                val base64Image = Base64.getEncoder().encodeToString(imageBytes)

                parts.add(
                    mapOf(
                        "inlineData" to mapOf(
                            "mimeType" to "image/png",
                            "data" to base64Image
                        )
                    )
                )
                logger.debug("Added reference image from URL: $imageUrl")
            } catch (e: Exception) {
                logger.error("Failed to fetch reference image from $imageUrl: ${e.message}", e)
                // Continue without this reference image
            }
        }

        val requestMap = mapOf(
            "contents" to listOf(
                mapOf("parts" to parts)
            ),
            "generationConfig" to mapOf(
                "responseModalities" to listOf("TEXT", "IMAGE")
            )
        )

        return objectMapper.writeValueAsString(requestMap)
    }

    /**
     * Fetch image from URL and return as byte array
     */
    private fun fetchImageFromUrl(url: String): ByteArray {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())

        if (response.statusCode() != 200) {
            throw RuntimeException("Failed to fetch image from $url: HTTP ${response.statusCode()}")
        }

        return response.body()
    }

    private fun parseImageResponse(responseBody: String): GeneratedImage {
        try {
            val jsonNode = objectMapper.readTree(responseBody)

            // Navigate to the image data in the response
            val candidates = jsonNode.get("candidates")
            if (candidates == null || !candidates.isArray || candidates.size() == 0) {
                throw RuntimeException("No candidates in response")
            }

            val candidate = candidates[0]
            val finishReason = candidate.get("finishReason")?.asText()
            val finishMessage = candidate.get("finishMessage")?.asText()

            // Check for known failure reasons
            if (finishReason != null && finishReason != "STOP") {
                logger.error("Image generation failed: finishReason=$finishReason, message=$finishMessage")
                throw RuntimeException("Image generation blocked: $finishReason - ${finishMessage ?: "No details"}")
            }

            val content = candidate.get("content")
            if (content == null) {
                throw RuntimeException("No content in candidate (finishReason=$finishReason)")
            }

            val parts = content.get("parts")
            if (parts == null || !parts.isArray) {
                throw RuntimeException("No parts in content")
            }

            // Find the part with inlineData
            for (part in parts) {
                val inlineData = part.get("inlineData")
                if (inlineData != null) {
                    val base64Data = inlineData.get("data")?.asText()
                    val mimeType = inlineData.get("mimeType")?.asText() ?: "image/png"

                    if (base64Data != null) {
                        val imageBytes = Base64.getDecoder().decode(base64Data)
                        logger.info("Successfully generated image: ${imageBytes.size} bytes")
                        return GeneratedImage(imageBytes, mimeType)
                    }
                }
            }

            throw RuntimeException("No image data found in response")
        } catch (e: Exception) {
            logger.error("Failed to parse image response: ${e.message}")
            logger.debug("Response body: $responseBody")
            throw RuntimeException("Failed to parse image response: ${e.message}", e)
        }
    }
}
