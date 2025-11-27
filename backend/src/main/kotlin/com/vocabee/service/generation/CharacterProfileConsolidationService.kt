package com.vocabee.service.generation

import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.model.*
import com.vocabee.domain.repository.*
import com.vocabee.service.ImageGenerationService
import com.vocabee.service.StorageService
import com.vocabee.service.external.gemini.GeminiTextClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Stage 4: Character Profile Consolidation
 * Consolidates character information from all episodes, generates detailed descriptions,
 * creates reference images, and assigns voices.
 */
@Service
class CharacterProfileConsolidationService(
    private val geminiClient: GeminiTextClient,
    private val imageGenerationService: ImageGenerationService,
    private val storageService: StorageService,
    private val blueprintRepository: GenerationBlueprintRepository,
    private val characterRepository: GenerationCharacterRepository,
    private val episodeContentRepository: GenerationEpisodeContentRepository,
    private val mediaRepository: GenerationMediaRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Voice assignments by language
    private val voicesByLanguage = mapOf(
        "fr" to listOf("Leda" to "female", "Charon" to "male", "Kore" to "female", "Aoede" to "female"),
        "de" to listOf("Kore" to "female", "Charon" to "male", "Leda" to "female", "Fenrir" to "male"),
        "es" to listOf("Leda" to "female", "Charon" to "male", "Aoede" to "female", "Orus" to "male"),
        "en" to listOf("Puck" to "neutral", "Charon" to "male", "Leda" to "female", "Kore" to "female")
    )

    @Transactional
    fun consolidateAll(generation: CourseGeneration) {
        logger.info("Consolidating character profiles for generation ${generation.id}")

        val blueprint = blueprintRepository.findByGenerationId(generation.id)
            ?: throw IllegalStateException("Blueprint not found")

        val characters = characterRepository.findByGenerationId(generation.id)
        val episodeContents = episodeContentRepository.findAllByGenerationIdOrdered(generation.id)

        // Get all dialogue lines for each character
        val characterDialogues = extractCharacterDialogues(characters, episodeContents)

        // Process each character
        characters.forEach { character ->
            try {
                consolidateCharacter(
                    generation = generation,
                    blueprint = blueprint,
                    character = character,
                    dialogueLines = characterDialogues[character.name] ?: emptyList()
                )
            } catch (e: Exception) {
                logger.error("Failed to consolidate character ${character.name}: ${e.message}", e)
            }
        }

        logger.info("Character profile consolidation completed for generation ${generation.id}")
    }

    @Transactional
    fun consolidateCharacter(
        generation: CourseGeneration,
        blueprint: GenerationBlueprint,
        character: GenerationCharacter,
        dialogueLines: List<String>
    ) {
        logger.info("Consolidating profile for character: ${character.name}")

        // 1. Generate detailed appearance description
        val appearanceDescription = generateAppearanceDescription(
            character = character,
            dialogueLines = dialogueLines,
            setting = blueprint.setting ?: "",
            languageCode = generation.languageCode
        )

        character.appearanceDescription = appearanceDescription
        characterRepository.save(character)

        // 2. Generate reference image
        val referenceImageUrl = generateReferenceImage(
            character = character,
            generation = generation
        )

        character.referenceImageUrl = referenceImageUrl
        characterRepository.save(character)

        // 3. Assign voice
        val voiceId = assignVoice(character, generation.languageCode)
        character.voiceId = voiceId
        characterRepository.save(character)

        logger.info("Character ${character.name} consolidated: voice=$voiceId, image=$referenceImageUrl")
    }

    private fun extractCharacterDialogues(
        characters: List<GenerationCharacter>,
        episodeContents: List<GenerationEpisodeContent>
    ): Map<String, List<String>> {
        val dialoguesByCharacter = mutableMapOf<String, MutableList<String>>()

        characters.forEach { char ->
            dialoguesByCharacter[char.name] = mutableListOf()
        }

        episodeContents.forEach { content ->
            content.contentStructured?.let { structured ->
                try {
                    val lines = objectMapper.convertValue(
                        structured,
                        object : com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {}
                    )
                    lines.forEach { line ->
                        val speaker = line["speaker"]
                        val text = line["text"]
                        if (speaker != null && text != null && dialoguesByCharacter.containsKey(speaker)) {
                            dialoguesByCharacter[speaker]?.add(text)
                        }
                    }
                } catch (e: Exception) {
                    logger.debug("Could not parse structured content: ${e.message}")
                }
            }
        }

        return dialoguesByCharacter
    }

    private fun generateAppearanceDescription(
        character: GenerationCharacter,
        dialogueLines: List<String>,
        setting: String,
        languageCode: String
    ): String {
        val traits = character.personalityTraits?.let { node ->
            objectMapper.convertValue(node, object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
        }?.joinToString(", ") ?: ""

        val developments = character.developments?.let { node ->
            objectMapper.convertValue(
                node,
                object : com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {})
        }?.mapNotNull { it["note"] }?.joinToString("; ") ?: ""

        val genderDescription = when (character.gender?.uppercase()) {
            "MALE" -> "man"
            "FEMALE" -> "woman"
            else -> "person"
        }

        val prompt = """
            Based on the following character information, create a detailed physical appearance description
            suitable for generating a consistent character portrait image.

            ## Character Information
            - Name: ${character.name}
            - Gender: ${character.gender ?: "unspecified"} ($genderDescription)
            - Role: ${character.role}
            - Age: ${character.ageRange}
            - Personality: $traits
            - Background: ${character.background}
            - Initial Description: ${character.initialDescription}
            ${if (developments.isNotEmpty()) "- Story Developments: $developments" else ""}

            ## Story Setting
            $setting

            ## Sample Dialogue (shows personality/speech style)
            ${dialogueLines.take(5).joinToString("\n") { "- \"$it\"" }}

            ## Requirements
            Create a detailed physical appearance description for this $genderDescription that:
            1. Is culturally appropriate for a ${languageCode.uppercase()} language course
            2. Clearly describes a ${character.gender?.lowercase() ?: "person"} character
            3. Matches the age and role of the character
            4. Reflects their personality in their appearance
            5. Includes specific details about:
               - Face shape, skin tone, eye color
               - Hair color, style, length
               - Typical clothing style
               - Any distinctive features
            6. Is suitable for photorealistic portrait generation

            Return ONLY the appearance description as plain text (2-3 sentences, no JSON).
        """.trimIndent()

        return try {
            geminiClient.generateText(prompt, "text/plain").trim()
        } catch (e: Exception) {
            logger.error("Failed to generate appearance for ${character.name}: ${e.message}", e)
            "A person in their ${character.ageRange ?: "mid-twenties"}, dressed casually, with a friendly expression."
        }
    }

    private fun generateReferenceImage(
        character: GenerationCharacter,
        generation: CourseGeneration
    ): String {
        val imagePrompt = """
            Professional portrait photo, realistic style, high quality:

            ${character.appearanceDescription}

            Style: Photorealistic, neutral background, soft natural lighting,
            front-facing portrait, professional headshot quality, clear facial features,
            natural and friendly expression, shoulders visible.
        """.trimIndent()

        logger.info("Generating reference image for character: ${character.name}")

        val generatedImage = imageGenerationService.generateImage(imagePrompt)

        // Upload to storage
        val uploadResult = storageService.uploadFile(
            inputStream = generatedImage.imageData.inputStream(),
            originalFilename = "${character.name.lowercase().replace(" ", "_")}_reference.png",
            contentType = generatedImage.mimeType,
            fileType = FileType.IMAGE,
            folder = "generations/${generation.id}/characters"
        )

        // Record media entry
        val media = GenerationMedia(
            generationId = generation.id,
            characterId = character.id,
            mediaType = GenerationMediaType.CHARACTER_IMAGE.name,
            url = uploadResult.url,
            metadata = objectMapper.valueToTree(
                mapOf(
                    "characterName" to character.name,
                    "prompt" to imagePrompt.take(500)
                )
            ),
            status = GenerationStepStatus.COMPLETED
        )
        mediaRepository.save(media)

        logger.info("Generated reference image for ${character.name}: ${uploadResult.url}")
        return uploadResult.url
    }

    private fun assignVoice(character: GenerationCharacter, languageCode: String): String {
        val voices = voicesByLanguage[languageCode] ?: voicesByLanguage["en"]!!

        // Use explicit gender if available, otherwise infer from description
        val characterGender = when (character.gender?.uppercase()) {
            "MALE" -> "male"
            "FEMALE" -> "female"
            else -> {
                // Fallback: infer from description
                val description = (character.initialDescription ?: "") + (character.appearanceDescription ?: "")
                val isFemale = description.lowercase().let { desc ->
                    desc.contains("she") || desc.contains("her") || desc.contains("woman") ||
                            desc.contains("female") || desc.contains("girl") || desc.contains("lady")
                }
                if (isFemale) "female" else "male"
            }
        }

        val matchingVoices = voices.filter { (_, voiceGender) ->
            voiceGender == characterGender || voiceGender == "neutral"
        }

        val selectedVoice = matchingVoices.firstOrNull()?.first ?: voices.first().first

        logger.info("Assigned voice '$selectedVoice' to character ${character.name} (gender: $characterGender)")
        return selectedVoice
    }
}
