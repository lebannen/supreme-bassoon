package com.vocabee.service.generation

import com.vocabee.domain.model.*
import com.vocabee.domain.repository.*
import com.vocabee.web.dto.admin.generation.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Main orchestrator for the course generation pipeline.
 * Manages workflow state and coordinates between stage-specific services.
 */
@Service
class CourseGenerationPipelineService(
    private val generationRepository: CourseGenerationRepository,
    private val blueprintRepository: GenerationBlueprintRepository,
    private val characterRepository: GenerationCharacterRepository,
    private val modulePlanRepository: GenerationModulePlanRepository,
    private val episodePlanRepository: GenerationEpisodePlanRepository,
    private val episodeContentRepository: GenerationEpisodeContentRepository,
    private val exercisesRepository: GenerationExercisesRepository,
    private val mediaRepository: GenerationMediaRepository,
    private val blueprintGenerator: BlueprintGenerationService,
    private val modulePlanGenerator: ModulePlanGenerationService,
    private val episodeContentGenerator: EpisodeContentGenerationService,
    private val characterProfileService: CharacterProfileConsolidationService,
    private val exerciseGenerator: PipelineExerciseGenerationService,
    private val mediaGenerator: PipelineMediaGenerationService,
    private val publishService: CoursePublishService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Start a new course generation workflow.
     */
    @Transactional
    fun startGeneration(request: StartGenerationRequest): GenerationProgressResponse {
        logger.info("Starting course generation: ${request.languageCode} ${request.cefrLevel}, ${request.moduleCount} modules")

        // Create generation record
        val generation = CourseGeneration(
            languageCode = request.languageCode,
            cefrLevel = request.cefrLevel,
            moduleCount = request.moduleCount,
            episodesPerModule = request.episodesPerModule,
            themePreferences = request.themePreferences,
            autoMode = request.autoMode,
            currentStage = GenerationStage.BLUEPRINT
        )
        generationRepository.save(generation)

        // Create empty blueprint record
        val blueprint = GenerationBlueprint(
            generationId = generation.id,
            status = GenerationStepStatus.PENDING
        )
        blueprintRepository.save(blueprint)

        // Start blueprint generation
        try {
            blueprintGenerator.generate(generation, blueprint)

            // If auto-mode, continue to next stages
            if (generation.autoMode) {
                logger.info("Auto-mode enabled, proceeding through all stages")
                runAllStages(generation.id)
            }
        } catch (e: Exception) {
            logger.error("Blueprint generation failed", e)
            generation.currentStage = GenerationStage.FAILED
            generation.errorMessage = e.message
            generationRepository.save(generation)
        }

        return getProgress(generation.id)
    }

    /**
     * Run all remaining stages automatically (for auto-mode).
     */
    private fun runAllStages(generationId: UUID) {
        var shouldContinue = true
        while (shouldContinue) {
            val generation = generationRepository.findById(generationId).orElse(null) ?: break

            if (generation.currentStage == GenerationStage.COMPLETED ||
                generation.currentStage == GenerationStage.FAILED
            ) {
                shouldContinue = false
            } else {
                try {
                    proceedToNextStageInternal(generation)
                } catch (e: Exception) {
                    logger.error("Auto-mode stage failed: ${e.message}", e)
                    shouldContinue = false
                }
            }
        }
    }

    /**
     * Get current progress of a generation workflow.
     */
    @Transactional(readOnly = true)
    fun getProgress(generationId: UUID): GenerationProgressResponse {
        val generation = generationRepository.findById(generationId)
            .orElseThrow { IllegalArgumentException("Generation not found: $generationId") }

        val blueprint = blueprintRepository.findByGenerationId(generationId)
        val characters = characterRepository.findByGenerationId(generationId)
        val modulePlans = modulePlanRepository.findByGenerationIdOrderByModuleNumber(generationId)

        // Build episode plans with content
        val modulePlanDtos = modulePlans.map { modulePlan ->
            val episodePlans = episodePlanRepository.findByModulePlanIdOrderByEpisodeNumber(modulePlan.id)
            val episodePlanDtos = episodePlans.map { episodePlan ->
                val content = episodeContentRepository.findByEpisodePlanId(episodePlan.id)
                val exercises = exercisesRepository.findByEpisodePlanId(episodePlan.id)
                mapEpisodePlan(episodePlan, content, exercises, characters)
            }
            mapModulePlan(modulePlan, episodePlanDtos)
        }

        val stageProgress = calculateStageProgress(generation, blueprint, modulePlans)
        val canProceed = canProceedToNextStage(generation, blueprint, modulePlans)
        val canPublish = generation.currentStage == GenerationStage.COMPLETED

        return GenerationProgressResponse(
            id = generation.id,
            languageCode = generation.languageCode,
            cefrLevel = generation.cefrLevel,
            moduleCount = generation.moduleCount,
            episodesPerModule = generation.episodesPerModule,
            themePreferences = generation.themePreferences,
            currentStage = generation.currentStage,
            stageProgress = stageProgress,
            blueprint = blueprint?.let { mapBlueprint(it) },
            modulePlans = modulePlanDtos.takeIf { it.isNotEmpty() },
            characters = characters.map { mapCharacter(it) }.takeIf { it.isNotEmpty() },
            canProceed = canProceed,
            canPublish = canPublish,
            errorMessage = generation.errorMessage,
            createdAt = generation.createdAt,
            completedAt = generation.completedAt
        )
    }

    /**
     * Approve current stage and proceed to the next one.
     */
    @Transactional
    fun proceedToNextStage(generationId: UUID): GenerationProgressResponse {
        val generation = generationRepository.findById(generationId)
            .orElseThrow { IllegalArgumentException("Generation not found: $generationId") }

        proceedToNextStageInternal(generation)

        return getProgress(generationId)
    }

    /**
     * Internal method to execute the next stage.
     */
    private fun proceedToNextStageInternal(generation: CourseGeneration) {
        logger.info("Proceeding from stage ${generation.currentStage} for generation ${generation.id}")

        try {
            when (generation.currentStage) {
                GenerationStage.BLUEPRINT -> {
                    generation.currentStage = GenerationStage.MODULE_PLANNING
                    generationRepository.save(generation)
                    modulePlanGenerator.generateAll(generation)
                }

                GenerationStage.MODULE_PLANNING -> {
                    generation.currentStage = GenerationStage.EPISODE_CONTENT
                    generationRepository.save(generation)
                    episodeContentGenerator.generateAll(generation)
                }

                GenerationStage.EPISODE_CONTENT -> {
                    generation.currentStage = GenerationStage.CHARACTER_PROFILES
                    generationRepository.save(generation)
                    characterProfileService.consolidateAll(generation)
                }

                GenerationStage.CHARACTER_PROFILES -> {
                    generation.currentStage = GenerationStage.EXERCISES
                    generationRepository.save(generation)
                    exerciseGenerator.generateAll(generation)
                }

                GenerationStage.EXERCISES -> {
                    generation.currentStage = GenerationStage.MEDIA
                    generationRepository.save(generation)
                    mediaGenerator.generateAll(generation)
                }

                GenerationStage.MEDIA -> {
                    generation.currentStage = GenerationStage.COMPLETED
                    generation.completedAt = LocalDateTime.now()
                    generationRepository.save(generation)
                }

                else -> {
                    throw IllegalStateException("Cannot proceed from stage ${generation.currentStage}")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to proceed to next stage", e)
            generation.currentStage = GenerationStage.FAILED
            generation.errorMessage = e.message
            generationRepository.save(generation)
            throw e
        }
    }

    /**
     * Regenerate the current stage.
     */
    @Transactional
    fun regenerateCurrentStage(generationId: UUID, feedback: RegenerateFeedback?): GenerationProgressResponse {
        val generation = generationRepository.findById(generationId)
            .orElseThrow { IllegalArgumentException("Generation not found: $generationId") }

        logger.info("Regenerating stage ${generation.currentStage} for generation $generationId")

        try {
            when (generation.currentStage) {
                GenerationStage.BLUEPRINT -> {
                    val blueprint = blueprintRepository.findByGenerationId(generationId)
                        ?: throw IllegalStateException("Blueprint not found")
                    blueprint.status = GenerationStepStatus.PENDING
                    blueprintRepository.save(blueprint)
                    blueprintGenerator.generate(generation, blueprint, feedback?.feedback)
                }

                GenerationStage.MODULE_PLANNING -> {
                    // Clear existing module plans and regenerate
                    val existingPlans = modulePlanRepository.findByGenerationId(generationId)
                    existingPlans.forEach { plan ->
                        val episodePlans = episodePlanRepository.findByModulePlanId(plan.id)
                        episodePlans.forEach { episodePlanRepository.delete(it) }
                        modulePlanRepository.delete(plan)
                    }
                    modulePlanGenerator.generateAll(generation, feedback?.feedback)
                }

                GenerationStage.EPISODE_CONTENT -> {
                    // Clear existing content and regenerate
                    val episodePlans = episodePlanRepository.findAllByGenerationIdOrdered(generationId)
                    episodePlans.forEach { plan ->
                        episodeContentRepository.findByEpisodePlanId(plan.id)?.let {
                            episodeContentRepository.delete(it)
                        }
                    }
                    episodeContentGenerator.generateAll(generation, feedback?.feedback)
                }

                GenerationStage.CHARACTER_PROFILES -> {
                    // Clear character profiles (appearance, voice, image) and regenerate
                    val characters = characterRepository.findByGenerationId(generationId)
                    characters.forEach { character ->
                        character.appearanceDescription = null
                        character.voiceId = null
                        character.referenceImageUrl = null
                        characterRepository.save(character)
                    }
                    // Clear character media
                    mediaRepository.deleteByGenerationIdAndMediaType(generationId, "CHARACTER_IMAGE")
                    characterProfileService.consolidateAll(generation)
                }

                GenerationStage.EXERCISES -> {
                    // Clear existing exercises and regenerate
                    val episodePlans = episodePlanRepository.findAllByGenerationIdOrdered(generationId)
                    episodePlans.forEach { plan ->
                        exercisesRepository.findByEpisodePlanId(plan.id)?.let {
                            exercisesRepository.delete(it)
                        }
                    }
                    exerciseGenerator.generateAll(generation)
                }

                GenerationStage.MEDIA -> {
                    // Clear media (audio and scene images) and regenerate
                    mediaRepository.deleteByGenerationIdAndMediaType(generationId, "EPISODE_AUDIO")
                    mediaRepository.deleteByGenerationIdAndMediaType(generationId, "SCENE_IMAGE")
                    mediaGenerator.generateAll(generation)
                }

                else -> {
                    throw IllegalStateException("Cannot regenerate stage ${generation.currentStage}")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to regenerate stage", e)
            generation.errorMessage = e.message
            generationRepository.save(generation)
        }

        return getProgress(generationId)
    }

    /**
     * List all generation workflows.
     */
    @Transactional(readOnly = true)
    fun listGenerations(): List<GenerationSummaryDto> {
        return generationRepository.findAllOrderByCreatedAtDesc().map { generation ->
            val blueprint = blueprintRepository.findByGenerationId(generation.id)
            GenerationSummaryDto(
                id = generation.id,
                languageCode = generation.languageCode,
                cefrLevel = generation.cefrLevel,
                moduleCount = generation.moduleCount,
                currentStage = generation.currentStage,
                blueprintTitle = blueprint?.courseTitle,
                createdAt = generation.createdAt,
                completedAt = generation.completedAt
            )
        }
    }

    /**
     * Cancel and delete a generation workflow.
     */
    @Transactional
    fun cancelGeneration(generationId: UUID) {
        val generation = generationRepository.findById(generationId)
            .orElseThrow { IllegalArgumentException("Generation not found: $generationId") }

        logger.info("Cancelling generation $generationId")
        generationRepository.delete(generation)
    }

    /**
     * Get debug data for a generation including all raw AI responses.
     */
    @Transactional(readOnly = true)
    fun getDebugData(generationId: UUID): GenerationDebugResponse {
        val generation = generationRepository.findById(generationId)
            .orElseThrow { IllegalArgumentException("Generation not found: $generationId") }

        val blueprint = blueprintRepository.findByGenerationId(generationId)
        val characters = characterRepository.findByGenerationId(generationId)
        val modulePlans = modulePlanRepository.findByGenerationIdOrderByModuleNumber(generationId)
        val media = mediaRepository.findByGenerationId(generationId)

        val modulePlanDebugDtos = modulePlans.map { modulePlan ->
            val episodePlans = episodePlanRepository.findByModulePlanIdOrderByEpisodeNumber(modulePlan.id)
            val episodePlanDebugDtos = episodePlans.map { episodePlan ->
                val content = episodeContentRepository.findByEpisodePlanId(episodePlan.id)
                val exercises = exercisesRepository.findByEpisodePlanId(episodePlan.id)
                mapEpisodePlanDebug(episodePlan, content, exercises)
            }
            mapModulePlanDebug(modulePlan, episodePlanDebugDtos)
        }

        return GenerationDebugResponse(
            id = generation.id,
            languageCode = generation.languageCode,
            cefrLevel = generation.cefrLevel,
            moduleCount = generation.moduleCount,
            episodesPerModule = generation.episodesPerModule,
            themePreferences = generation.themePreferences,
            currentStage = generation.currentStage,
            autoMode = generation.autoMode,
            errorMessage = generation.errorMessage,
            createdAt = generation.createdAt,
            completedAt = generation.completedAt,
            blueprint = blueprint?.let { mapBlueprintDebug(it) },
            modulePlans = modulePlanDebugDtos,
            characters = characters.map { mapCharacterDebug(it) },
            media = media.map { mapMediaDebug(it) }
        )
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private fun calculateStageProgress(
        generation: CourseGeneration,
        blueprint: GenerationBlueprint?,
        modulePlans: List<GenerationModulePlan>
    ): StageProgressDto {
        return when (generation.currentStage) {
            GenerationStage.BLUEPRINT -> StageProgressDto(
                completed = if (blueprint?.status == GenerationStepStatus.COMPLETED) 1 else 0,
                total = 1,
                currentItem = if (blueprint?.status == GenerationStepStatus.IN_PROGRESS) "Generating blueprint..." else null
            )

            GenerationStage.MODULE_PLANNING -> {
                val completed = modulePlans.count { it.status == GenerationStepStatus.COMPLETED }
                val inProgress = modulePlans.find { it.status == GenerationStepStatus.IN_PROGRESS }
                StageProgressDto(
                    completed = completed,
                    total = generation.moduleCount,
                    currentItem = inProgress?.let { "Generating Module ${it.moduleNumber}..." }
                )
            }

            GenerationStage.EPISODE_CONTENT -> {
                val totalEpisodes = generation.moduleCount * generation.episodesPerModule
                val completed = episodeContentRepository.countByGenerationIdAndStatus(
                    generation.id, GenerationStepStatus.COMPLETED
                )
                StageProgressDto(
                    completed = completed,
                    total = totalEpisodes,
                    currentItem = "Generating episodes..."
                )
            }

            else -> StageProgressDto(0, 0, null)
        }
    }

    private fun canProceedToNextStage(
        generation: CourseGeneration,
        blueprint: GenerationBlueprint?,
        modulePlans: List<GenerationModulePlan>
    ): Boolean {
        return when (generation.currentStage) {
            GenerationStage.BLUEPRINT -> blueprint?.status == GenerationStepStatus.COMPLETED
            GenerationStage.MODULE_PLANNING -> {
                modulePlans.size == generation.moduleCount &&
                        modulePlans.all { it.status == GenerationStepStatus.COMPLETED }
            }

            GenerationStage.EPISODE_CONTENT -> {
                val totalEpisodes = generation.moduleCount * generation.episodesPerModule
                val completed = episodeContentRepository.countByGenerationIdAndStatus(
                    generation.id, GenerationStepStatus.COMPLETED
                )
                completed == totalEpisodes
            }

            GenerationStage.CHARACTER_PROFILES -> true // TODO: implement check
            GenerationStage.EXERCISES -> {
                val totalEpisodes = generation.moduleCount * generation.episodesPerModule
                val completed = exercisesRepository.countByGenerationIdAndStatus(
                    generation.id, GenerationStepStatus.COMPLETED
                )
                completed == totalEpisodes
            }

            GenerationStage.MEDIA -> {
                val media = mediaRepository.findByGenerationId(generation.id)
                media.isNotEmpty() && media.all { it.status == GenerationStepStatus.COMPLETED }
            }

            else -> false
        }
    }

    // ========================================================================
    // Mapping methods
    // ========================================================================

    private fun mapBlueprint(blueprint: GenerationBlueprint): BlueprintDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        return BlueprintDto(
            id = blueprint.id,
            courseTitle = blueprint.courseTitle,
            courseDescription = blueprint.courseDescription,
            setting = blueprint.setting,
            premise = blueprint.premise,
            plotArc = blueprint.plotArc?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<PlotArcPointDto>>() {})
            },
            moduleTopics = blueprint.moduleTopics?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<ModuleTopicDto>>() {})
            },
            grammarDistribution = blueprint.grammarDistribution?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<GrammarDistributionDto>>() {})
            },
            status = blueprint.status
        )
    }

    private fun mapCharacter(character: GenerationCharacter): CharacterDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        return CharacterDto(
            id = character.id,
            name = character.name,
            role = character.role,
            initialDescription = character.initialDescription,
            ageRange = character.ageRange,
            personalityTraits = character.personalityTraits?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
            },
            background = character.background,
            appearanceDescription = character.appearanceDescription,
            voiceId = character.voiceId,
            referenceImageUrl = character.referenceImageUrl
        )
    }

    private fun mapModulePlan(modulePlan: GenerationModulePlan, episodes: List<EpisodePlanDto>): ModulePlanDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        return ModulePlanDto(
            id = modulePlan.id,
            moduleNumber = modulePlan.moduleNumber,
            title = modulePlan.title,
            theme = modulePlan.theme,
            description = modulePlan.description,
            objectives = modulePlan.objectives?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
            },
            plotSummary = modulePlan.plotSummary,
            episodes = episodes,
            status = modulePlan.status
        )
    }

    private fun mapEpisodePlan(
        episodePlan: GenerationEpisodePlan,
        content: GenerationEpisodeContent?,
        exercises: GenerationExercises?,
        characters: List<GenerationCharacter>
    ): EpisodePlanDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()

        val vocabulary = episodePlan.vocabulary?.let { node ->
            objectMapper.convertValue(node, object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
        }
        val grammarRules = episodePlan.grammarRules?.let { node ->
            objectMapper.convertValue(node, object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
        }
        val characterIds = episodePlan.characterIds?.let { node ->
            objectMapper.convertValue(node, object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
        }
        val characterNames = characterIds?.mapNotNull { id ->
            characters.find { it.id.toString() == id }?.name
        }

        return EpisodePlanDto(
            id = episodePlan.id,
            episodeNumber = episodePlan.episodeNumber,
            title = episodePlan.title,
            sceneDescription = episodePlan.sceneDescription,
            episodeType = episodePlan.episodeType,
            vocabulary = vocabulary,
            grammarRules = grammarRules,
            characterNames = characterNames,
            plotPoints = episodePlan.plotPoints,
            content = content?.let { mapEpisodeContent(it) },
            exercises = exercises?.let { mapExercises(it) },
            status = episodePlan.status
        )
    }

    private fun mapEpisodeContent(content: GenerationEpisodeContent): EpisodeContentDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        return EpisodeContentDto(
            id = content.id,
            contentType = content.contentType,
            content = content.content,
            summary = content.summary,
            vocabularyUsed = content.vocabularyUsed?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
            },
            vocabularyMissing = content.vocabularyMissing?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
            },
            status = content.status
        )
    }

    private fun mapExercises(exercises: GenerationExercises): EpisodeExercisesDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        return EpisodeExercisesDto(
            id = exercises.id,
            exerciseCount = exercises.exerciseCount,
            vocabularyCoverage = exercises.vocabularyCoverage?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
            },
            grammarCoverage = exercises.grammarCoverage?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
            },
            status = exercises.status
        )
    }

    // ========================================================================
    // Debug mapping methods
    // ========================================================================

    private fun mapBlueprintDebug(blueprint: GenerationBlueprint): BlueprintDebugDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        return BlueprintDebugDto(
            id = blueprint.id,
            courseTitle = blueprint.courseTitle,
            courseDescription = blueprint.courseDescription,
            setting = blueprint.setting,
            premise = blueprint.premise,
            plotArc = blueprint.plotArc?.let { objectMapper.writeValueAsString(it) },
            moduleTopics = blueprint.moduleTopics?.let { objectMapper.writeValueAsString(it) },
            grammarDistribution = blueprint.grammarDistribution?.let { objectMapper.writeValueAsString(it) },
            status = blueprint.status,
            rawResponse = blueprint.rawResponse,
            createdAt = blueprint.createdAt
        )
    }

    private fun mapModulePlanDebug(
        modulePlan: GenerationModulePlan,
        episodes: List<EpisodePlanDebugDto>
    ): ModulePlanDebugDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        return ModulePlanDebugDto(
            id = modulePlan.id,
            moduleNumber = modulePlan.moduleNumber,
            title = modulePlan.title,
            theme = modulePlan.theme,
            description = modulePlan.description,
            objectives = modulePlan.objectives?.let { objectMapper.writeValueAsString(it) },
            plotSummary = modulePlan.plotSummary,
            status = modulePlan.status,
            rawResponse = modulePlan.rawResponse,
            createdAt = modulePlan.createdAt,
            episodes = episodes
        )
    }

    private fun mapEpisodePlanDebug(
        episodePlan: GenerationEpisodePlan,
        content: GenerationEpisodeContent?,
        exercises: GenerationExercises?
    ): EpisodePlanDebugDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        return EpisodePlanDebugDto(
            id = episodePlan.id,
            episodeNumber = episodePlan.episodeNumber,
            title = episodePlan.title,
            sceneDescription = episodePlan.sceneDescription,
            episodeType = episodePlan.episodeType,
            vocabulary = episodePlan.vocabulary?.let { objectMapper.writeValueAsString(it) },
            grammarRules = episodePlan.grammarRules?.let { objectMapper.writeValueAsString(it) },
            characterIds = episodePlan.characterIds?.let { objectMapper.writeValueAsString(it) },
            plotPoints = episodePlan.plotPoints,
            status = episodePlan.status,
            createdAt = episodePlan.createdAt,
            content = content?.let { mapEpisodeContentDebug(it) },
            exercises = exercises?.let { mapExercisesDebug(it) }
        )
    }

    private fun mapEpisodeContentDebug(content: GenerationEpisodeContent): EpisodeContentDebugDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        return EpisodeContentDebugDto(
            id = content.id,
            contentType = content.contentType,
            content = content.content,
            contentStructured = content.contentStructured?.let { objectMapper.writeValueAsString(it) },
            summary = content.summary,
            characterDevelopments = content.characterDevelopments?.let { objectMapper.writeValueAsString(it) },
            vocabularyUsed = content.vocabularyUsed?.let { objectMapper.writeValueAsString(it) },
            vocabularyMissing = content.vocabularyMissing?.let { objectMapper.writeValueAsString(it) },
            imagePrompts = content.imagePrompts?.let { objectMapper.writeValueAsString(it) },
            status = content.status,
            rawResponse = content.rawResponse,
            createdAt = content.createdAt
        )
    }

    private fun mapExercisesDebug(exercises: GenerationExercises): EpisodeExercisesDebugDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        return EpisodeExercisesDebugDto(
            id = exercises.id,
            exercises = exercises.exercises?.let { objectMapper.writeValueAsString(it) },
            exerciseCount = exercises.exerciseCount,
            vocabularyCoverage = exercises.vocabularyCoverage?.let { objectMapper.writeValueAsString(it) },
            grammarCoverage = exercises.grammarCoverage?.let { objectMapper.writeValueAsString(it) },
            status = exercises.status,
            rawResponse = exercises.rawResponse,
            createdAt = exercises.createdAt
        )
    }

    private fun mapCharacterDebug(character: GenerationCharacter): CharacterDebugDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        return CharacterDebugDto(
            id = character.id,
            name = character.name,
            role = character.role,
            gender = character.gender,
            initialDescription = character.initialDescription,
            ageRange = character.ageRange,
            personalityTraits = character.personalityTraits?.let { objectMapper.writeValueAsString(it) },
            background = character.background,
            appearanceDescription = character.appearanceDescription,
            voiceId = character.voiceId,
            referenceImageUrl = character.referenceImageUrl,
            developments = character.developments?.let { objectMapper.writeValueAsString(it) },
            createdAt = character.createdAt,
            updatedAt = character.updatedAt
        )
    }

    private fun mapMediaDebug(media: GenerationMedia): MediaDebugDto {
        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        return MediaDebugDto(
            id = media.id,
            mediaType = media.mediaType,
            episodePlanId = media.episodePlanId,
            characterId = media.characterId,
            url = media.url,
            metadata = media.metadata?.let { objectMapper.writeValueAsString(it) },
            status = media.status,
            errorMessage = media.errorMessage,
            createdAt = media.createdAt
        )
    }
}
