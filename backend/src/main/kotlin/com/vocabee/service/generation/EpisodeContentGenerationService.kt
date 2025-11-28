package com.vocabee.service.generation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vocabee.domain.model.*
import com.vocabee.domain.repository.*
import com.vocabee.service.external.gemini.GeminiTextClient
import com.vocabee.web.dto.admin.generation.EpisodeContentGenerationResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Stage 3: Episode Content Generation
 * Generates dialogue/story content for each episode with vocabulary incorporation.
 */
@Service
class EpisodeContentGenerationService(
    private val geminiClient: GeminiTextClient,
    private val blueprintRepository: GenerationBlueprintRepository,
    private val characterRepository: GenerationCharacterRepository,
    private val modulePlanRepository: GenerationModulePlanRepository,
    private val episodePlanRepository: GenerationEpisodePlanRepository,
    private val episodeContentRepository: GenerationEpisodeContentRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Generate content for all episodes sequentially.
     */
    @Transactional
    fun generateAll(generation: CourseGeneration, feedback: String? = null) {
        logger.info("Generating episode content for generation ${generation.id}")

        val blueprint = blueprintRepository.findByGenerationId(generation.id)
            ?: throw IllegalStateException("Blueprint not found")

        val characters = characterRepository.findByGenerationId(generation.id)
        val modulePlans = modulePlanRepository.findByGenerationIdOrderByModuleNumber(generation.id)

        // Process each module's episodes sequentially
        for (modulePlan in modulePlans) {
            val episodePlans = episodePlanRepository.findByModulePlanIdOrderByEpisodeNumber(modulePlan.id)

            for (episodePlan in episodePlans) {
                generateEpisodeContent(
                    generation = generation,
                    blueprint = blueprint,
                    characters = characters,
                    modulePlan = modulePlan,
                    episodePlan = episodePlan,
                    feedback = feedback
                )
            }
        }

        logger.info("All episode content generated for generation ${generation.id}")
    }

    @Transactional
    fun generateEpisodeContent(
        generation: CourseGeneration,
        blueprint: GenerationBlueprint,
        characters: List<GenerationCharacter>,
        modulePlan: GenerationModulePlan,
        episodePlan: GenerationEpisodePlan,
        feedback: String? = null
    ): GenerationEpisodeContent {
        logger.info("Generating content for episode: ${episodePlan.title}")

        // Create or get existing content
        var content = episodeContentRepository.findByEpisodePlanId(episodePlan.id)
        if (content == null) {
            content = GenerationEpisodeContent(
                episodePlanId = episodePlan.id,
                status = GenerationStepStatus.PENDING
            )
            episodeContentRepository.save(content)
        }

        content.status = GenerationStepStatus.IN_PROGRESS
        episodeContentRepository.save(content)

        episodePlan.status = GenerationStepStatus.IN_PROGRESS
        episodePlanRepository.save(episodePlan)

        try {
            // Get previous episode summaries for context
            val previousSummaries = episodeContentRepository.findPreviousEpisodes(
                generation.id,
                modulePlan.moduleNumber,
                episodePlan.episodeNumber
            )

            // Get episode characters
            val episodeCharacterIds = episodePlan.characterIds?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
            } ?: emptyList()

            var episodeCharacters = characters.filter { char ->
                episodeCharacterIds.contains(char.id.toString())
            }

            // Validate and constrain character count for dialogue episodes
            if (episodePlan.episodeType == "DIALOGUE") {
                if (episodeCharacters.size != 2) {
                    logger.warn("Dialogue episode '${episodePlan.title}' has ${episodeCharacters.size} characters, expected 2. Adjusting...")
                    episodeCharacters = when {
                        episodeCharacters.size > 2 -> episodeCharacters.take(2)
                        episodeCharacters.size == 1 -> {
                            // Add another character from the available pool
                            val additionalChar = characters.firstOrNull { !episodeCharacters.contains(it) }
                            if (additionalChar != null) episodeCharacters + additionalChar else episodeCharacters
                        }

                        else -> characters.take(2) // No characters specified, take first 2
                    }
                }
            }

            // Get vocabulary
            val vocabulary = episodePlan.vocabulary?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
            } ?: emptyList()

            // Get grammar rules
            val grammarRules = episodePlan.grammarRules?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
            } ?: emptyList()

            val prompt = buildPrompt(
                generation = generation,
                blueprint = blueprint,
                characters = episodeCharacters,
                modulePlan = modulePlan,
                episodePlan = episodePlan,
                vocabulary = vocabulary,
                grammarRules = grammarRules,
                previousSummaries = previousSummaries,
                feedback = feedback
            )

            val response = geminiClient.generateText(prompt, "application/json")
            content.rawResponse = response

            val result = parseResponse(response)

            // Update content
            content.contentType = episodePlan.episodeType
            content.content = if (result.dialogue != null) {
                result.dialogue.joinToString("\n") { "${it.speaker}: ${it.text}" }
            } else {
                result.story
            }
            content.contentStructured = result.dialogue?.let { objectMapper.valueToTree(it) }
            content.summary = result.summary
            content.characterDevelopments = result.characterDevelopments?.let { objectMapper.valueToTree(it) }
            content.vocabularyUsed = objectMapper.valueToTree(result.vocabularyUsed)
            content.vocabularyMissing = objectMapper.valueToTree(result.vocabularyMissing)

            // Generate image prompts for scene illustrations
            val imagePrompts = try {
                generateImagePrompts(
                    contentText = content.content ?: "",
                    episodePlan = episodePlan,
                    generation = generation
                )
            } catch (e: Exception) {
                logger.error("Failed to generate image prompts for ${episodePlan.title}: ${e.message}", e)
                emptyList()
            }
            content.imagePrompts = if (imagePrompts.isNotEmpty()) {
                objectMapper.valueToTree(imagePrompts)
            } else null

            content.status = GenerationStepStatus.COMPLETED

            episodeContentRepository.save(content)

            // Update episode plan status
            episodePlan.status = GenerationStepStatus.COMPLETED
            episodePlanRepository.save(episodePlan)

            // Update character developments
            result.characterDevelopments?.forEach { dev ->
                val character = characters.find { it.name.equals(dev.characterName, ignoreCase = true) }
                if (character != null) {
                    val currentDevelopments = character.developments?.let { node ->
                        objectMapper.convertValue(
                            node,
                            object :
                                com.fasterxml.jackson.core.type.TypeReference<MutableList<Map<String, String>>>() {})
                    } ?: mutableListOf()

                    currentDevelopments.add(
                        mapOf(
                            "episodeId" to episodePlan.id.toString(),
                            "note" to dev.note
                        )
                    )

                    character.developments = objectMapper.valueToTree(currentDevelopments)
                    characterRepository.save(character)
                }
            }

            logger.info("Episode content generated: ${episodePlan.title}")
            return content

        } catch (e: Exception) {
            logger.error("Episode content generation failed: ${episodePlan.title}", e)
            content.status = GenerationStepStatus.FAILED
            episodeContentRepository.save(content)
            episodePlan.status = GenerationStepStatus.FAILED
            episodePlanRepository.save(episodePlan)
            throw e
        }
    }

    private fun buildPrompt(
        generation: CourseGeneration,
        blueprint: GenerationBlueprint,
        characters: List<GenerationCharacter>,
        modulePlan: GenerationModulePlan,
        episodePlan: GenerationEpisodePlan,
        vocabulary: List<String>,
        grammarRules: List<String>,
        previousSummaries: List<GenerationEpisodeContent>,
        feedback: String?
    ): String {
        val previousContext = if (previousSummaries.isNotEmpty()) {
            """
            ## Story So Far
            ${
                previousSummaries.mapIndexed { index, content ->
                    "Episode ${index + 1}: ${content.summary}"
                }.joinToString("\n")
            }
            """
        } else ""

        val characterContext = """
            ## Characters in This Episode
            ${
            characters.joinToString("\n") { char ->
                val traits = char.personalityTraits?.let { node ->
                    objectMapper.convertValue(
                        node,
                        object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
                }?.joinToString(", ") ?: ""

                val recentDevelopments = char.developments?.let { node ->
                    val devs = objectMapper.convertValue(
                        node,
                        object : com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {})
                    devs.takeLast(3).mapNotNull { it["note"] }.joinToString("; ")
                } ?: ""

                """
                - ${char.name} (${char.role}): ${char.initialDescription}
                  Personality: $traits
                  ${if (recentDevelopments.isNotEmpty()) "Recent developments: $recentDevelopments" else ""}
                """.trimIndent()
            }
        }
        """

        val feedbackSection = feedback?.let { "## Feedback\nPlease incorporate: $it\n" } ?: ""

        val cefrGuidelines = CefrLevelGuidelines.getContentPromptSection(generation.cefrLevel)

        val typeSpecificRules = if (episodePlan.episodeType == "DIALOGUE") {
            """
            ## Dialogue Rules (CRITICAL)
            1. Use EXACTLY 2 speakers (the characters listed above)
            2. Format each line as: "SpeakerName: Dialogue text"
            3. Do NOT use "Narrator" - only the two character names
            4. Do NOT use quotation marks around the dialogue text
            5. Make 12-18 exchanges for rich content
            6. Incorporate vocabulary naturally - don't force words unnaturally
            """
        } else {
            """
            ## Story Rules
            1. Write a continuous narrative in ${generation.languageCode.uppercase()}
            2. Use third person narration
            3. Write 200-300 words
            4. Incorporate vocabulary naturally
            """
        }

        return """
            You are an expert language content creator. Generate episode content for a ${generation.cefrLevel} ${generation.languageCode.uppercase()} course.

            ## Series Context
            - Setting: ${blueprint.setting}
            - Premise: ${blueprint.premise}

            $characterContext

            $previousContext

            ## This Episode
            - Module: ${modulePlan.title} (${modulePlan.theme})
            - Episode: ${episodePlan.title}
            - Type: ${episodePlan.episodeType}
            - Scene: ${episodePlan.sceneDescription}
            - Plot Points: ${episodePlan.plotPoints}

            ## Learning Objectives (MUST incorporate naturally)
            - Vocabulary (use as many as naturally fit): ${vocabulary.joinToString(", ")}
            - Grammar points to demonstrate: ${grammarRules.joinToString(", ")}

            $cefrGuidelines

            $typeSpecificRules

            $feedbackSection

            ## Output Format
            Return ONLY valid JSON:

            {
              ${
            if (episodePlan.episodeType == "DIALOGUE") """
              "dialogue": [
                {"speaker": "CharacterName", "text": "Dialogue in target language"},
                {"speaker": "OtherCharacter", "text": "Response in target language"}
              ],
              "story": null,""" else """
              "dialogue": null,
              "story": "The narrative text in target language...","""
        }
              "summary": "Brief summary of what happened in this episode (in English, 2-3 sentences)",
              "characterDevelopments": [
                {"characterName": "[Character from this episode]", "note": "[Any character reveal or development]"}
              ],
              "vocabularyUsed": ["[words from vocabulary list that were used]"],
              "vocabularyMissing": ["[words from vocabulary list that couldn't fit naturally]"]
            }

            Generate the episode content now:
        """.trimIndent()
    }

    private fun parseResponse(response: String): EpisodeContentGenerationResult {
        val json = extractJson(response)
        return try {
            objectMapper.readValue(json)
        } catch (e: Exception) {
            logger.error("Failed to parse episode content response. Raw: $json", e)
            throw RuntimeException("Failed to parse episode content: ${e.message}")
        }
    }

    private fun extractJson(response: String): String {
        var json = response.trim()
        if (json.startsWith("```json")) {
            json = json.substring(7)
        } else if (json.startsWith("```")) {
            json = json.substring(3)
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length - 3)
        }
        return json.trim()
    }

    /**
     * Generate image prompts for scene illustrations based on episode content.
     */
    private fun generateImagePrompts(
        contentText: String,
        episodePlan: GenerationEpisodePlan,
        generation: CourseGeneration
    ): List<ImagePromptDto> {
        logger.info("Generating image prompts for episode: ${episodePlan.title}")

        val prompt = """
            Based on the following ${episodePlan.episodeType.lowercase()} content from a ${generation.cefrLevel} level ${generation.languageCode.uppercase()} language learning episode, generate 3-4 vivid image prompts that would help visualize key scenes or moments.

            Episode Title: "${episodePlan.title}"
            Episode Type: ${episodePlan.episodeType}
            Scene Description: ${episodePlan.sceneDescription ?: "N/A"}

            Content:
            ```
            $contentText
            ```

            For each image prompt, provide:
            1. A detailed visual description suitable for AI image generation (in English)
            2. The scene context explaining what's happening in this moment of the episode

            Guidelines:
            - Focus on SETTINGS and ENVIRONMENTS rather than people
            - Use descriptive, visual language (colors, lighting, composition, mood)
            - Make images culturally appropriate and educational
            - Avoid text or speech bubbles in the images
            - Each prompt should be distinct and capture a different scene/moment
            - DO NOT include character names in the prompts
            - If people must appear, use generic descriptions like "a person", "two friends", "a group of people" - never specific names or detailed physical descriptions
            - Prefer wide shots of locations over close-ups of people

            Return ONLY valid JSON in this format (no markdown, no extra text):
            {
              "imagePrompts": [
                {
                  "description": "[Detailed visual description of the setting - include lighting, colors, atmosphere, architectural details relevant to the scene]",
                  "sceneContext": "[What's happening in this moment of the episode]"
                },
                {
                  "description": "[Another distinct scene from the episode - different location or moment, with visual details]",
                  "sceneContext": "[Context for this scene in the story]"
                }
              ]
            }

            IMPORTANT: Generate prompts specific to THIS episode's setting and content. Do not default to generic café or tourist landmark images.
        """.trimIndent()

        val response = geminiClient.generateText(prompt, "application/json")
        val json = extractJson(response)

        return try {
            val jsonNode = objectMapper.readTree(json)
            val promptsNode = jsonNode.get("imagePrompts")

            if (promptsNode == null || !promptsNode.isArray) {
                logger.warn("Invalid image prompts response format, returning empty list")
                return emptyList()
            }

            promptsNode.mapNotNull { node ->
                val description = node.get("description")?.asText()
                val sceneContext = node.get("sceneContext")?.asText()
                if (description.isNullOrBlank()) null
                else ImagePromptDto(
                    description = description,
                    sceneContext = sceneContext ?: ""
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to parse image prompts response: ${e.message}", e)
            emptyList()
        }
    }
}

/**
 * DTO for image prompts stored in episode content
 */
data class ImagePromptDto(
    val description: String,
    val sceneContext: String
)
