package com.vocabee.service.generation

import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.model.*
import com.vocabee.domain.repository.*
import com.vocabee.web.dto.CourseDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Publishes a completed generation workflow to actual Course, Module, and Episode entities.
 */
@Service
class CoursePublishService(
    private val generationRepository: CourseGenerationRepository,
    private val blueprintRepository: GenerationBlueprintRepository,
    private val characterRepository: GenerationCharacterRepository,
    private val modulePlanRepository: GenerationModulePlanRepository,
    private val episodePlanRepository: GenerationEpisodePlanRepository,
    private val episodeContentRepository: GenerationEpisodeContentRepository,
    private val exercisesRepository: GenerationExercisesRepository,
    private val mediaRepository: GenerationMediaRepository,
    private val courseRepository: CourseRepository,
    private val moduleRepository: ModuleRepository,
    private val episodeRepository: EpisodeRepository,
    private val exerciseRepository: ExerciseRepository,
    private val exerciseTypeRepository: ExerciseTypeRepository,
    private val episodeContentItemRepository: EpisodeContentItemRepository,
    private val wordSetRepository: WordSetRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun publish(generationId: UUID): CourseDto {
        logger.info("Publishing generation $generationId to course")

        val generation = generationRepository.findById(generationId)
            .orElseThrow { IllegalArgumentException("Generation not found: $generationId") }

        if (generation.currentStage != GenerationStage.COMPLETED) {
            throw IllegalStateException("Generation must be COMPLETED to publish. Current stage: ${generation.currentStage}")
        }

        val blueprint = blueprintRepository.findByGenerationId(generationId)
            ?: throw IllegalStateException("Blueprint not found")

        val characters = characterRepository.findByGenerationId(generationId)

        // 1. Create Course
        val course = createCourse(generation, blueprint, characters)
        logger.info("Created course: ${course.id} - ${course.name}")

        // 2. Create Modules and Episodes
        val modulePlans = modulePlanRepository.findByGenerationIdOrderByModuleNumber(generationId)
        modulePlans.forEach { modulePlan ->
            createModule(course, modulePlan, generation)
        }

        // 3. Update generation with course reference
        generation.courseId = course.id
        generationRepository.save(generation)

        logger.info("Successfully published generation $generationId as course ${course.id}")

        return course.toDto()
    }

    private fun createCourse(
        generation: CourseGeneration,
        blueprint: GenerationBlueprint,
        characters: List<GenerationCharacter>
    ): Course {
        // Generate slug from title
        val baseSlug = blueprint.courseTitle
            ?.lowercase()
            ?.replace(Regex("[^a-z0-9]+"), "-")
            ?.trim('-')
            ?: "course-${generation.id.toString().take(8)}"

        // Ensure unique slug
        var slug = baseSlug
        var counter = 1
        while (courseRepository.findBySlug(slug) != null) {
            slug = "$baseSlug-$counter"
            counter++
        }

        // Build series context from characters
        val seriesContext = buildSeriesContext(blueprint, characters)

        // Build objectives
        val objectives = blueprint.moduleTopics?.let { node ->
            objectMapper.convertValue(
                node,
                object : com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Any>>>() {})
        }?.map { "Learn: ${it["topic"]}" }

        val course = Course(
            slug = slug,
            name = blueprint.courseTitle ?: "Untitled Course",
            languageCode = generation.languageCode,
            cefrLevel = generation.cefrLevel,
            description = blueprint.courseDescription,
            objectives = objectives?.let { objectMapper.valueToTree(it) },
            estimatedHours = generation.moduleCount * generation.episodesPerModule,
            seriesContext = seriesContext,
            status = CourseStatus.DRAFT,
            isPublished = false
        )

        return courseRepository.save(course)
    }

    private fun buildSeriesContext(
        blueprint: GenerationBlueprint,
        characters: List<GenerationCharacter>
    ): String {
        val characterDescriptions = characters.joinToString("\n\n") { char ->
            """
            **${char.name}** (${char.role})
            ${char.initialDescription}
            ${char.appearanceDescription?.let { "\nAppearance: $it" } ?: ""}
            ${char.voiceId?.let { "Voice: $it" } ?: ""}
            """.trimIndent()
        }

        return """
            # Setting
            ${blueprint.setting}

            # Premise
            ${blueprint.premise}

            # Characters
            $characterDescriptions
        """.trimIndent()
    }

    private fun createModule(
        course: Course,
        modulePlan: GenerationModulePlan,
        generation: CourseGeneration
    ): Module {
        val module = Module(
            courseId = course.id!!,
            moduleNumber = modulePlan.moduleNumber,
            title = modulePlan.title ?: "Module ${modulePlan.moduleNumber}",
            theme = modulePlan.theme,
            description = modulePlan.description,
            objectives = modulePlan.objectives,
            vocabularyFocus = null, // Could aggregate from episodes
            grammarFocus = null,
            estimatedMinutes = generation.episodesPerModule * 15,
            status = ModuleStatus.READY
        )

        val savedModule = moduleRepository.save(module)
        logger.info("Created module: ${savedModule.id} - ${savedModule.title}")

        // Link word set to published module
        wordSetRepository.findByGenerationModulePlanId(modulePlan.id)?.let { wordSet ->
            wordSet.moduleId = savedModule.id
            wordSetRepository.save(wordSet)
            logger.info("Linked word set ${wordSet.id} to module ${savedModule.id}")
        }

        // Create episodes
        val episodePlans = episodePlanRepository.findByModulePlanIdOrderByEpisodeNumber(modulePlan.id)
        episodePlans.forEach { episodePlan ->
            createEpisode(savedModule, episodePlan, generation)
        }

        return savedModule
    }

    private fun createEpisode(
        module: Module,
        episodePlan: GenerationEpisodePlan,
        generation: CourseGeneration
    ): Episode {
        val content = episodeContentRepository.findByEpisodePlanId(episodePlan.id)
        val exercises = exercisesRepository.findByEpisodePlanId(episodePlan.id)

        // Get media for this episode
        val episodeMedia = mediaRepository.findByEpisodePlanId(episodePlan.id)
        val audioMedia = episodeMedia.find { it.mediaType == GenerationMediaType.EPISODE_AUDIO.name }
        val sceneImages = episodeMedia.filter {
            it.mediaType == GenerationMediaType.SCENE_IMAGE.name && it.status == GenerationStepStatus.COMPLETED
        }

        // Build episode data (structured content) and serialize to string
        val episodeData = buildEpisodeData(episodePlan, content, exercises, sceneImages)
        val episodeDataString = objectMapper.writeValueAsString(episodeData)

        // Convert string episode type to enum
        val episodeType = try {
            EpisodeType.valueOf(episodePlan.episodeType.uppercase())
        } catch (e: Exception) {
            EpisodeType.DIALOGUE
        }

        val episode = Episode(
            moduleId = module.id!!,
            episodeNumber = episodePlan.episodeNumber,
            episodeType = episodeType,
            title = episodePlan.title ?: "Episode ${episodePlan.episodeNumber}",
            content = content?.content ?: "",
            audioUrl = audioMedia?.url,
            transcript = content?.content,
            summary = content?.summary,
            data = episodeDataString,
            estimatedMinutes = 15,
            status = EpisodeStatus.READY
        )

        val savedEpisode = episodeRepository.save(episode)
        logger.info("Created episode: ${savedEpisode.id} - ${savedEpisode.title}")

        // Create exercises and link them
        exercises?.exercises?.let { exerciseNodes ->
            createEpisodeExercises(savedEpisode, exerciseNodes, generation)
        }

        return savedEpisode
    }

    private fun buildEpisodeData(
        episodePlan: GenerationEpisodePlan,
        content: GenerationEpisodeContent?,
        exercises: GenerationExercises?,
        sceneImages: List<GenerationMedia> = emptyList()
    ): com.fasterxml.jackson.databind.JsonNode {
        val data = mutableMapOf<String, Any?>()

        // Add vocabulary
        episodePlan.vocabulary?.let { vocab ->
            data["vocabulary"] = objectMapper.convertValue(vocab, List::class.java)
        }

        // Add grammar rules
        episodePlan.grammarRules?.let { grammar ->
            data["grammarRules"] = objectMapper.convertValue(grammar, List::class.java)
        }

        // Add structured dialogue if available (frontend expects {lines: [...]} format)
        content?.contentStructured?.let { structured ->
            val lines = objectMapper.convertValue(structured, List::class.java)
            data["dialogue"] = mapOf("lines" to lines)
        }

        // Add vocabulary coverage
        content?.vocabularyUsed?.let { used ->
            data["vocabularyUsed"] = objectMapper.convertValue(used, List::class.java)
        }

        // Add scene images
        if (sceneImages.isNotEmpty()) {
            val images = sceneImages.mapNotNull { media ->
                media.url?.let { url ->
                    val metadata = media.metadata?.let { node ->
                        objectMapper.convertValue(node, Map::class.java) as Map<String, Any?>
                    } ?: emptyMap()

                    mapOf(
                        "url" to url,
                        "description" to (metadata["description"] ?: ""),
                        "sceneContext" to (metadata["sceneContext"] ?: "")
                    )
                }
            }
            if (images.isNotEmpty()) {
                data["images"] = images
            }
        }

        return objectMapper.valueToTree(data)
    }

    private fun createEpisodeExercises(
        episode: Episode,
        exerciseNodes: com.fasterxml.jackson.databind.JsonNode,
        generation: CourseGeneration
    ) {
        if (!exerciseNodes.isArray) return

        var orderIndex = 0
        exerciseNodes.forEach { exerciseNode ->
            try {
                val rawTypeKey = exerciseNode.get("type")?.asText() ?: return@forEach
                val contentNode = exerciseNode.get("content") ?: return@forEach

                // Normalize exercise type key: FILL_IN_THE_BLANK -> fill_in_blank
                val exerciseTypeKey = normalizeExerciseTypeKey(rawTypeKey)

                // Look up exercise type entity
                val exerciseType = exerciseTypeRepository.findByTypeKey(exerciseTypeKey)
                    ?: run {
                        logger.warn("Unknown exercise type: $rawTypeKey (normalized: $exerciseTypeKey), skipping")
                        return@forEach
                    }

                // Generate title from exercise type and content
                val title = "${exerciseType.displayName} - ${episode.title}"

                // Create exercise
                val exercise = Exercise(
                    exerciseType = exerciseType,
                    languageCode = generation.languageCode,
                    cefrLevel = generation.cefrLevel,
                    title = title,
                    instructions = "Complete the exercise",
                    content = contentNode,
                    difficultyRating = 1.0,
                    pointsValue = 10
                )

                val savedExercise = exerciseRepository.save(exercise)

                // Link to episode
                val contentItem = EpisodeContentItem(
                    episodeId = episode.id!!,
                    orderIndex = orderIndex++,
                    contentType = ContentItemType.EXERCISE,
                    contentId = savedExercise.id!!,
                    isRequired = true,
                    pointsValue = 10
                )

                episodeContentItemRepository.save(contentItem)
            } catch (e: Exception) {
                logger.error("Failed to create exercise: ${e.message}", e)
            }
        }

        logger.info("Created $orderIndex exercises for episode ${episode.title}")
    }

    /**
     * Normalize AI-generated exercise type keys to match database values.
     * Handles variations like:
     * - MULTIPLE_CHOICE -> multiple_choice
     * - FILL_IN_THE_BLANK -> fill_in_blank
     * - SENTENCE_SCRAMBLE -> sentence_scramble
     */
    private fun normalizeExerciseTypeKey(rawKey: String): String {
        val normalized = rawKey.lowercase()

        // Handle specific mappings for known variations
        return when (normalized) {
            "fill_in_the_blank" -> "fill_in_blank"
            else -> normalized
        }
    }
}

// Extension function for Course.toDto if not already defined
private fun Course.toDto() = CourseDto(
    id = id!!,
    slug = slug,
    name = name,
    languageCode = languageCode,
    cefrLevel = cefrLevel,
    description = description,
    objectives = objectives?.let { node ->
        if (node.isArray) node.map { it.asText() } else emptyList()
    } ?: emptyList(),
    estimatedHours = estimatedHours,
    modules = emptyList() // Modules loaded separately
)
