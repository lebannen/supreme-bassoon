package com.vocabee.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.generation.CharacterProfile
import com.vocabee.domain.model.FileType
import com.vocabee.service.external.gemini.GeminiTextClient
import com.vocabee.web.dto.admin.ModuleGenerationResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CharacterProfileService(
    private val geminiTextClient: GeminiTextClient,
    private val imageGenerationService: ImageGenerationService,
    private val storageService: StorageService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Extract unique character names from all module dialogues
     */
    fun extractCharacters(modules: List<ModuleGenerationResult>): List<String> {
        val characterNames = mutableSetOf<String>()

        modules.forEach { module ->
            module.episodes.forEach { episode ->
                episode.content?.dialogue?.lines?.forEach { line ->
                    characterNames.add(line.speaker)
                }
            }
        }

        logger.info("Extracted ${characterNames.size} unique characters: $characterNames")
        return characterNames.toList()
    }

    /**
     * Generate character description based on dialogues they appear in
     */
    fun generateCharacterDescription(
        characterName: String,
        modules: List<ModuleGenerationResult>,
        courseContext: String
    ): Pair<String, String> {
        // Collect all dialogue lines for this character
        val characterLines = mutableListOf<String>()
        modules.forEach { module ->
            module.episodes.forEach { episode ->
                episode.content?.dialogue?.lines
                    ?.filter { it.speaker == characterName }
                    ?.forEach { characterLines.add(it.text) }
            }
        }

        val prompt = """
            Based on the following dialogue lines spoken by $characterName in a language learning course,
            create a detailed character description that can be used to generate a consistent character image.

            Course Context: $courseContext

            Character Name: $characterName
            Sample Dialogue Lines:
            ${characterLines.take(10).joinToString("\n") { "- \"$it\"" }}

            Please provide:
            1. A brief character description (personality, role, approximate age)
            2. Detailed physical appearance for realistic image generation (face, hair, clothing, distinctive features)

            Guidelines:
            - Make the character culturally appropriate for the course language/context
            - Use realistic, photographic style descriptions
            - Be specific about visual details (e.g., "warm brown eyes", "shoulder-length dark hair")
            - Include typical clothing that fits their role

            Return ONLY valid JSON in this format (no markdown, no extra text):
            {
              "description": "Brief character description including personality and role",
              "appearanceDetails": "Detailed physical appearance for image generation"
            }
        """.trimIndent()

        return try {
            val response = geminiTextClient.generateText(prompt, "application/json")
            val json = extractJson(response)
            val jsonNode = objectMapper.readTree(json)

            val description = jsonNode.get("description")?.asText() ?: ""
            val appearanceDetails = jsonNode.get("appearanceDetails")?.asText() ?: ""

            logger.info("Generated description for character '$characterName'")
            Pair(description, appearanceDetails)
        } catch (e: Exception) {
            logger.error("Failed to generate description for character '$characterName': ${e.message}", e)
            // Fallback descriptions
            Pair(
                "$characterName - a character in the language learning course",
                "A realistic portrait of a person, neutral expression, professional photo quality"
            )
        }
    }

    /**
     * Generate character reference image
     */
    fun generateCharacterReferenceImage(
        characterName: String,
        appearanceDetails: String,
        courseSlug: String
    ): String {
        val imagePrompt = """
            Professional portrait photo, realistic style, high quality:

            $appearanceDetails

            Style: Photorealistic, neutral background, good lighting, front-facing portrait,
            professional headshot quality, clear facial features, natural expression.
        """.trimIndent()

        logger.info("Generating reference image for character: $characterName")

        val generatedImage = imageGenerationService.generateImage(imagePrompt)

        // Upload to storage
        val uploadResult = storageService.uploadFile(
            inputStream = generatedImage.imageData.inputStream(),
            originalFilename = "${characterName.lowercase().replace(" ", "_")}_reference.png",
            contentType = generatedImage.mimeType,
            fileType = FileType.IMAGE,
            folder = "character-images/$courseSlug"
        )

        logger.info("Uploaded character reference image for '$characterName': ${uploadResult.url}")
        return uploadResult.url
    }

    /**
     * Generate all character profiles for a course
     */
    fun generateCharacterProfiles(
        modules: List<ModuleGenerationResult>,
        courseSlug: String,
        courseContext: String
    ): List<CharacterProfile> {
        val characterNames = extractCharacters(modules)

        if (characterNames.isEmpty()) {
            logger.info("No characters found in course dialogues")
            return emptyList()
        }

        logger.info("Generating character profiles for ${characterNames.size} characters")

        return characterNames.mapNotNull { characterName ->
            try {
                val (description, appearanceDetails) = generateCharacterDescription(
                    characterName,
                    modules,
                    courseContext
                )

                val referenceImageUrl = generateCharacterReferenceImage(
                    characterName,
                    appearanceDetails,
                    courseSlug
                )

                CharacterProfile(
                    name = characterName,
                    description = description,
                    referenceImageUrl = referenceImageUrl,
                    appearanceDetails = appearanceDetails
                )
            } catch (e: Exception) {
                logger.error("Failed to generate profile for character '$characterName': ${e.message}", e)
                null // Skip this character if generation fails
            }
        }
    }

    private fun extractJson(response: String): String {
        val jsonStart = response.indexOf('{')
        val jsonEnd = response.lastIndexOf('}') + 1
        return if (jsonStart >= 0 && jsonEnd > jsonStart) {
            response.substring(jsonStart, jsonEnd)
        } else {
            response
        }
    }
}
