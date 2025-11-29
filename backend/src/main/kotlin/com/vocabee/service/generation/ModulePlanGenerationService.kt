package com.vocabee.service.generation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vocabee.domain.model.*
import com.vocabee.domain.repository.GenerationBlueprintRepository
import com.vocabee.domain.repository.GenerationCharacterRepository
import com.vocabee.domain.repository.GenerationEpisodePlanRepository
import com.vocabee.domain.repository.GenerationModulePlanRepository
import com.vocabee.service.external.gemini.GeminiTextClient
import com.vocabee.web.dto.admin.generation.GrammarDistributionDto
import com.vocabee.web.dto.admin.generation.ModulePlanGenerationResult
import com.vocabee.web.dto.admin.generation.ModuleTopicDto
import com.vocabee.web.dto.admin.generation.PlotArcPointDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Stage 2: Module Planning
 * Generates detailed module plans with episode outlines, vocabulary, and grammar assignments.
 */
@Service
class ModulePlanGenerationService(
    private val geminiClient: GeminiTextClient,
    private val blueprintRepository: GenerationBlueprintRepository,
    private val characterRepository: GenerationCharacterRepository,
    private val modulePlanRepository: GenerationModulePlanRepository,
    private val episodePlanRepository: GenerationEpisodePlanRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Generate all module plans sequentially.
     */
    @Transactional
    fun generateAll(generation: CourseGeneration, feedback: String? = null) {
        logger.info("Generating ${generation.moduleCount} module plans for generation ${generation.id}")

        val blueprint = blueprintRepository.findByGenerationId(generation.id)
            ?: throw IllegalStateException("Blueprint not found")

        val characters = characterRepository.findByGenerationId(generation.id)

        // Parse blueprint data
        val moduleTopics = blueprint.moduleTopics?.let { node ->
            objectMapper.convertValue(
                node,
                object : com.fasterxml.jackson.core.type.TypeReference<List<ModuleTopicDto>>() {})
        } ?: throw IllegalStateException("Module topics not found in blueprint")

        val plotArc = blueprint.plotArc?.let { node ->
            objectMapper.convertValue(
                node,
                object : com.fasterxml.jackson.core.type.TypeReference<List<PlotArcPointDto>>() {})
        } ?: emptyList()

        val grammarDistribution = blueprint.grammarDistribution?.let { node ->
            objectMapper.convertValue(
                node,
                object : com.fasterxml.jackson.core.type.TypeReference<List<GrammarDistributionDto>>() {})
        } ?: emptyList()

        // Generate each module sequentially
        val previousModulePlans = mutableListOf<GenerationModulePlan>()

        for (moduleNumber in 1..generation.moduleCount) {
            val topic = moduleTopics.find { it.module == moduleNumber }
                ?: throw IllegalStateException("Topic not found for module $moduleNumber")

            val arcPoint = plotArc.find { it.module == moduleNumber }?.arcPoint ?: ""
            val grammarRules = grammarDistribution.find { it.module == moduleNumber }?.rules ?: emptyList()

            val modulePlan = generateModule(
                generation = generation,
                blueprint = blueprint,
                characters = characters,
                moduleNumber = moduleNumber,
                topic = topic.topic,
                arcPoint = arcPoint,
                grammarRules = grammarRules,
                previousModulePlans = previousModulePlans,
                feedback = feedback
            )

            previousModulePlans.add(modulePlan)
        }

        logger.info("All ${generation.moduleCount} module plans generated")
    }

    @Transactional
    fun generateModule(
        generation: CourseGeneration,
        blueprint: GenerationBlueprint,
        characters: List<GenerationCharacter>,
        moduleNumber: Int,
        topic: String,
        arcPoint: String,
        grammarRules: List<String>,
        previousModulePlans: List<GenerationModulePlan>,
        feedback: String? = null
    ): GenerationModulePlan {
        logger.info("Generating module $moduleNumber: $topic")

        // Create or get existing module plan
        var modulePlan = modulePlanRepository.findByGenerationIdAndModuleNumber(generation.id, moduleNumber)
        if (modulePlan == null) {
            modulePlan = GenerationModulePlan(
                generationId = generation.id,
                moduleNumber = moduleNumber,
                status = GenerationStepStatus.PENDING
            )
            modulePlanRepository.save(modulePlan)
        }

        modulePlan.status = GenerationStepStatus.IN_PROGRESS
        modulePlanRepository.save(modulePlan)

        try {
            val prompt = buildPrompt(
                generation = generation,
                blueprint = blueprint,
                characters = characters,
                moduleNumber = moduleNumber,
                topic = topic,
                arcPoint = arcPoint,
                grammarRules = grammarRules,
                previousModulePlans = previousModulePlans,
                feedback = feedback
            )

            val response = geminiClient.generateText(prompt, "application/json")
            modulePlan.rawResponse = response

            val result = parseResponse(response)

            // Update module plan
            modulePlan.title = result.title
            modulePlan.theme = result.theme
            modulePlan.description = result.description
            modulePlan.objectives = objectMapper.valueToTree(result.objectives)
            modulePlan.plotSummary = result.plotSummary
            modulePlan.status = GenerationStepStatus.COMPLETED

            modulePlanRepository.save(modulePlan)

            // Create episode plans
            result.episodes.forEach { episodeResult ->
                val characterIds = episodeResult.characterNames.mapNotNull { name ->
                    characters.find { it.name.equals(name, ignoreCase = true) }?.id?.toString()
                }

                val episodePlan = GenerationEpisodePlan(
                    modulePlanId = modulePlan.id,
                    episodeNumber = episodeResult.episodeNumber,
                    title = episodeResult.title,
                    sceneDescription = episodeResult.sceneDescription,
                    episodeType = episodeResult.episodeType,
                    vocabulary = objectMapper.valueToTree(episodeResult.vocabulary),
                    grammarRules = objectMapper.valueToTree(episodeResult.grammarRules),
                    characterIds = objectMapper.valueToTree(characterIds),
                    plotPoints = episodeResult.plotPoints,
                    status = GenerationStepStatus.PENDING
                )
                episodePlanRepository.save(episodePlan)
                logger.info("Created episode plan: ${episodeResult.title}")
            }

            logger.info("Module $moduleNumber generation completed: ${result.title}")
            return modulePlan

        } catch (e: Exception) {
            logger.error("Module $moduleNumber generation failed", e)
            modulePlan.status = GenerationStepStatus.FAILED
            modulePlanRepository.save(modulePlan)
            throw e
        }
    }

    private fun buildPrompt(
        generation: CourseGeneration,
        blueprint: GenerationBlueprint,
        characters: List<GenerationCharacter>,
        moduleNumber: Int,
        topic: String,
        arcPoint: String,
        grammarRules: List<String>,
        previousModulePlans: List<GenerationModulePlan>,
        feedback: String?
    ): String {
        val previousContext = if (previousModulePlans.isNotEmpty()) {
            """
            ## Previous Modules (for continuity)
            ${
                previousModulePlans.joinToString("\n") { plan ->
                    "Module ${plan.moduleNumber}: ${plan.title} - ${plan.plotSummary}"
                }
            }
            """
        } else ""

        val feedbackSection = feedback?.let {
            """
            ## Feedback
            Please incorporate: $it
            """
        } ?: ""

        val cefrVocabGuidelines = CefrLevelGuidelines.getVocabularyPromptSection(generation.cefrLevel)
        val cefrGuidelines = CefrLevelGuidelines.getCurriculumPromptSection(generation.cefrLevel)
        val functionalConstraints = CefrLevelGuidelines.getModuleFunctionalConstraints(
            generation.cefrLevel,
            moduleNumber,
            generation.moduleCount
        )

        return """
            You are an expert language curriculum designer. Create a detailed module plan for a language learning course.

            ## Course Context
            - Language: ${generation.languageCode.uppercase()}
            - Level: ${generation.cefrLevel}
            - Course Title: ${blueprint.courseTitle}
            - Setting: ${blueprint.setting}
            - Premise: ${blueprint.premise}

            ## Characters Available
            ${
            characters.joinToString("\n") { char ->
                "- ${char.name} (${char.role}): ${char.initialDescription}"
            }
        }

            $previousContext

            ## This Module
            - Module Number: $moduleNumber of ${generation.moduleCount}
            - Topic: $topic
            - Plot Arc Point: $arcPoint
            - Grammar Rules to Cover: ${grammarRules.joinToString(", ")}
            - Episodes Required: ${generation.episodesPerModule}

            $cefrGuidelines

            $cefrVocabGuidelines

            $functionalConstraints

            $feedbackSection

            ## Requirements

            ### Module Overview
            Create a compelling module that:
            - Advances the overall story
            - Teaches the assigned topic naturally
            - Strictly adheres to ${generation.cefrLevel} level constraints

            ### Episode Plans
            For each episode, provide:
            1. **title**: Descriptive episode title
            2. **sceneDescription**: What happens in this episode (2-3 sentences)
            3. **episodeType**: Either "DIALOGUE" or "STORY"
            4. **vocabulary**: 15-20 target words/phrases for this episode (MUST be ${generation.cefrLevel} appropriate)
            5. **grammarRules**: Which grammar rules from the module are demonstrated
            6. **characterNames**: EXACTLY 2 characters for DIALOGUE episodes (use exact names from Characters list). Story episodes can have 1-3 characters.
            7. **plotPoints**: Key plot developments in this episode

            ## Output Format
            Return ONLY valid JSON (no markdown):

            {
              "title": "[Module Title]",
              "theme": "[Module Theme]",
              "description": "[2-3 paragraph description of the module]",
              "objectives": [
                "By the end of this module, learners will be able to...",
                "Learners will understand how to..."
              ],
              "plotSummary": "[Summary of what happens in this module]",
              "episodes": [
                {
                  "episodeNumber": 1,
                  "title": "[Episode Title]",
                  "sceneDescription": "[Description of the scene and what happens]",
                  "episodeType": "DIALOGUE",
                  "vocabulary": ["[${generation.cefrLevel}-appropriate word]", "[phrase]"],
                  "grammarRules": ["[grammar-rule-slug]"],
                  "characterNames": ["[Character1]", "[Character2]"],
                  "plotPoints": "[Key things that happen in this episode]"
                }
              ]
            }

            Generate the module plan now:
        """.trimIndent()
    }

    private fun parseResponse(response: String): ModulePlanGenerationResult {
        val json = extractJson(response)
        return try {
            objectMapper.readValue(json)
        } catch (e: Exception) {
            logger.error("Failed to parse module plan response. Raw: $json", e)
            throw RuntimeException("Failed to parse module plan: ${e.message}")
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
}
