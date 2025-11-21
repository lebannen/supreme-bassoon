package com.vocabee.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.model.*
import com.vocabee.domain.repository.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class CourseImportRequest(
    val slug: String,
    val name: String,
    val languageCode: String,
    val cefrLevel: String = "A1",
    val description: String? = null,
    val objectives: List<String> = emptyList(),
    val estimatedHours: Int = 40
)

data class ModuleImportRequest(
    val moduleNumber: Int,
    val title: String,
    val theme: String? = null,
    val description: String? = null,
    val objectives: List<String> = emptyList(),
    val estimatedMinutes: Int = 120,
    val episodes: List<EpisodeImportRequest> = emptyList()
)

data class SpeakerConfig(
    val voice: String,
    val description: String? = null
)

data class DialogueLine(
    val speaker: String,
    val text: String
)

data class StructuredDialogue(
    val lines: List<DialogueLine>,
    val speakers: Map<String, SpeakerConfig>,
    val audioStyle: String? = null
)

data class EpisodeImportRequest(
    val episodeNumber: Int,
    val type: String, // DIALOGUE, STORY, ARTICLE, AUDIO_LESSON
    val title: String,
    val dialogue: StructuredDialogue,
    val generateAudio: Boolean = false,
    val transcript: String? = null,
    val audioStyle: String? = null,
    val estimatedMinutes: Int = 15,
    val contentItems: List<ContentItemImportRequest> = emptyList()
)

data class ContentItemImportRequest(
    val orderIndex: Int,
    val type: String, // EXERCISE, GRAMMAR_RULE
    val isRequired: Boolean = true,
    val exercise: ExerciseImportRequest? = null,
    val grammarRule: GrammarRuleImportRequest? = null
)

data class ExerciseImportRequest(
    val type: String,
    val title: String,
    val instructions: String,
    val content: JsonNode,
    val estimatedDurationSeconds: Int = 60,
    val pointsValue: Int = 10,
    val difficultyRating: Double = 1.0,
    val generateAudio: Boolean = false
)

data class GrammarRuleImportRequest(
    val title: String,
    val explanation: String,
    val examples: List<String> = emptyList(),
    val category: String? = null
)

data class CourseImportResult(
    val courseId: Long,
    val modulesImported: Int,
    val episodesImported: Int,
    val exercisesImported: Int,
    val errors: List<String> = emptyList()
)

@Service
class CourseImportService(
    private val courseRepository: CourseRepository,
    private val moduleRepository: ModuleRepository,
    private val episodeRepository: EpisodeRepository,
    private val episodeContentItemRepository: EpisodeContentItemRepository,
    private val exerciseRepository: ExerciseRepository,
    private val exerciseTypeRepository: ExerciseTypeRepository,
    private val grammarRuleRepository: GrammarRuleRepository,
    private val audioGenerationService: AudioGenerationService,
    private val validationService: ExerciseValidationService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val progressMap = ConcurrentHashMap<String, CourseImportProgress>()

    fun getProgress(importId: String): CourseImportProgress? = progressMap[importId]

    fun getAllProgress(): List<CourseImportProgress> = progressMap.values.toList()

    fun createImport(courseSlug: String, courseName: String): String {
        val importId = UUID.randomUUID().toString()
        val progress = CourseImportProgress(
            importId = importId,
            courseSlug = courseSlug,
            courseName = courseName,
            status = ImportStatus.QUEUED,
            message = "Import queued"
        )
        progressMap[importId] = progress
        return importId
    }

    private fun updateProgress(importId: String, updater: (CourseImportProgress) -> CourseImportProgress) {
        progressMap.computeIfPresent(importId) { _, current -> updater(current) }
    }

    @Transactional
    fun importCourse(courseRequest: CourseImportRequest): CourseImportResult {
        logger.info("Importing course: ${courseRequest.name}")

        val errors = mutableListOf<String>()

        // Create or update course
        val courseObjectives: JsonNode = objectMapper.valueToTree(courseRequest.objectives)

        val course = courseRepository.findBySlug(courseRequest.slug)
            ?.copy(
                name = courseRequest.name,
                description = courseRequest.description,
                objectives = courseObjectives,
                estimatedHours = courseRequest.estimatedHours,
                updatedAt = java.time.LocalDateTime.now()
            )
            ?: Course(
                slug = courseRequest.slug,
                name = courseRequest.name,
                languageCode = courseRequest.languageCode,
                cefrLevel = courseRequest.cefrLevel,
                description = courseRequest.description,
                objectives = courseObjectives,
                estimatedHours = courseRequest.estimatedHours,
                isPublished = true
            )

        val savedCourse = courseRepository.save(course)
        logger.info("Course saved with ID: ${savedCourse.id}")

        return CourseImportResult(
            courseId = savedCourse.id!!,
            modulesImported = 0,
            episodesImported = 0,
            exercisesImported = 0,
            errors = errors
        )
    }

    @Transactional
    fun importModule(
        courseSlug: String,
        moduleRequest: ModuleImportRequest,
        generateAudio: Boolean = true
    ): CourseImportResult {
        logger.info("Importing module ${moduleRequest.moduleNumber} for course $courseSlug")

        val course = courseRepository.findBySlug(courseSlug)
            ?: throw IllegalArgumentException("Course not found: $courseSlug")

        val errors = mutableListOf<String>()
        var episodesImported = 0
        var exercisesImported = 0

        // Create or update module
        val savedModule = createOrUpdateModule(course.id!!, moduleRequest)
        logger.info("Module saved with ID: ${savedModule.id}")

        // Import episodes
        for (episodeRequest in moduleRequest.episodes) {
            try {
                val savedEpisode = processEpisode(
                    episodeRequest,
                    savedModule.id!!,
                    course,
                    savedModule.moduleNumber,
                    generateAudio
                )
                episodesImported++
                logger.info("Episode saved with ID: ${savedEpisode.id}")

                // Import content items
                val contentItemResult = importContentItems(
                    savedEpisode.id!!,
                    episodeRequest.episodeNumber,
                    episodeRequest.contentItems,
                    course.languageCode,
                    course.cefrLevel,
                    savedModule.moduleNumber,
                    generateAudio
                )
                exercisesImported += contentItemResult.exercisesImported
                errors.addAll(contentItemResult.errors)

            } catch (e: Exception) {
                logger.error("Failed to import episode ${episodeRequest.episodeNumber}", e)
                errors.add("Episode ${episodeRequest.episodeNumber}: ${e.message}")
            }
        }

        return CourseImportResult(
            courseId = course.id,
            modulesImported = 1,
            episodesImported = episodesImported,
            exercisesImported = exercisesImported,
            errors = errors
        )
    }

    @Async
    fun importModuleAsync(
        importId: String,
        courseSlug: String,
        moduleRequest: ModuleImportRequest,
        generateAudio: Boolean = true
    ) {
        updateProgress(importId) {
            it.copy(
                status = ImportStatus.PROCESSING,
                message = "Starting module import..."
            )
        }

        try {
            logger.info("Starting async import $importId for module ${moduleRequest.moduleNumber} in course $courseSlug")

            val course = courseRepository.findBySlug(courseSlug)
                ?: throw IllegalArgumentException("Course not found: $courseSlug")

            val errors = mutableListOf<String>()
            var episodesImported = 0
            var exercisesImported = 0
            var audioGenerated = 0

            // Count totals for progress tracking
            val totalEpisodes = moduleRequest.episodes.size
            val totalExercises = moduleRequest.episodes.sumOf { it.contentItems.count { ci -> ci.exercise != null } }

            updateProgress(importId) {
                it.copy(
                    totalModules = 1,
                    totalEpisodes = totalEpisodes,
                    totalExercises = totalExercises,
                    message = "Creating module: ${moduleRequest.title}"
                )
            }

            // Create or update module
            val savedModule = createOrUpdateModule(course.id!!, moduleRequest)
            logger.info("Module saved with ID: ${savedModule.id}")

            updateProgress(importId) {
                it.copy(
                    processedModules = 1,
                    currentModule = moduleRequest.title,
                    message = "Module created. Processing episodes..."
                )
            }

            // Import episodes
            for (episodeRequest in moduleRequest.episodes) {
                try {
                    updateProgress(importId) {
                        it.copy(
                            currentEpisode = episodeRequest.title,
                            message = "Processing episode ${episodeRequest.episodeNumber}: ${episodeRequest.title}"
                        )
                    }

                    // Pre-audio generation progress update
                    if (generateAudio && episodeRequest.generateAudio &&
                        (episodeRequest.type == "DIALOGUE" || episodeRequest.type == "STORY")) {
                        updateProgress(importId) {
                            it.copy(message = "Generating audio for: ${episodeRequest.title}")
                        }
                    }

                    val savedEpisode = try {
                        val episode = processEpisode(
                            episodeRequest,
                            savedModule.id!!,
                            course,
                            savedModule.moduleNumber,
                            generateAudio
                        )
                        if (episode.audioUrl != null) audioGenerated++
                        episode
                    } catch (e: Exception) {
                        logger.error("Failed to process episode: ${episodeRequest.title}", e)
                        errors.add("Episode ${episodeRequest.episodeNumber}: ${e.message}")
                        continue
                    }
                    episodesImported++
                    logger.info("Episode saved with ID: ${savedEpisode.id}")

                    updateProgress(importId) {
                        it.copy(
                            processedEpisodes = episodesImported,
                            audioFilesGenerated = audioGenerated,
                            message = "Episode ${episodeRequest.episodeNumber} saved. Processing content items..."
                        )
                    }

                    // Delete existing content items for this episode (if updating)
                    episodeContentItemRepository.deleteByEpisodeId(savedEpisode.id!!)

                    // Import content items (exercises, grammar rules)
                    for (contentItemRequest in episodeRequest.contentItems) {
                        try {
                            val contentType = ContentItemType.valueOf(contentItemRequest.type.uppercase())
                            var contentId: Long? = null

                            when (contentType) {
                                ContentItemType.EXERCISE -> {
                                    if (contentItemRequest.exercise == null) {
                                        errors.add("Episode ${episodeRequest.episodeNumber}, content item ${contentItemRequest.orderIndex}: Missing exercise data")
                                        continue
                                    }

                                    updateProgress(importId) {
                                        it.copy(message = "Importing exercise: ${contentItemRequest.exercise.title}")
                                    }

                                    val exerciseId = importExercise(
                                        course.languageCode,
                                        savedModule.moduleNumber,
                                        contentItemRequest.exercise,
                                        generateAudio
                                    )

                                    if (exerciseId != null) {
                                        contentId = exerciseId
                                        exercisesImported++
                                        updateProgress(importId) {
                                            it.copy(
                                                processedExercises = exercisesImported,
                                                message = "Exercise imported: ${contentItemRequest.exercise.title}"
                                            )
                                        }
                                    } else {
                                        errors.add("Episode ${episodeRequest.episodeNumber}, content item ${contentItemRequest.orderIndex}: Failed to import exercise")
                                        continue
                                    }
                                }

                                ContentItemType.GRAMMAR_RULE -> {
                                    if (contentItemRequest.grammarRule == null) {
                                        errors.add("Episode ${episodeRequest.episodeNumber}, content item ${contentItemRequest.orderIndex}: Missing grammar rule data")
                                        continue
                                    }

                                    val grammarRuleId = importGrammarRule(
                                        course.languageCode,
                                        course.cefrLevel,
                                        contentItemRequest.grammarRule
                                    )
                                    contentId = grammarRuleId
                                }

                                else -> {
                                    errors.add("Episode ${episodeRequest.episodeNumber}, content item ${contentItemRequest.orderIndex}: Unsupported content type '${contentItemRequest.type}'")
                                    continue
                                }
                            }

                            // Create content item link
                            val contentItem = EpisodeContentItem(
                                episodeId = savedEpisode.id,
                                orderIndex = contentItemRequest.orderIndex,
                                contentType = contentType,
                                contentId = contentId!!,
                                isRequired = contentItemRequest.isRequired
                            )

                            episodeContentItemRepository.save(contentItem)

                        } catch (e: Exception) {
                            logger.error(
                                "Failed to import content item ${contentItemRequest.orderIndex} for episode ${episodeRequest.episodeNumber}",
                                e
                            )
                            errors.add("Episode ${episodeRequest.episodeNumber}, content item ${contentItemRequest.orderIndex}: ${e.message}")
                        }
                    }

                } catch (e: Exception) {
                    logger.error("Failed to import episode ${episodeRequest.episodeNumber}", e)
                    errors.add("Episode ${episodeRequest.episodeNumber}: ${e.message}")
                }
            }

            // Complete the import
            updateProgress(importId) {
                it.copy(
                    status = ImportStatus.COMPLETED,
                    message = "Import completed: $episodesImported episodes, $exercisesImported exercises, $audioGenerated audio files",
                    errors = errors,
                    completedAt = java.time.LocalDateTime.now()
                )
            }
            logger.info("Import $importId completed successfully")

        } catch (e: Exception) {
            logger.error("Import failed for $importId: ${e.javaClass.simpleName} - ${e.message}", e)
            updateProgress(importId) {
                it.copy(
                    status = ImportStatus.FAILED,
                    error = "${e.javaClass.simpleName}: ${e.message}",
                    message = "Import failed",
                    completedAt = java.time.LocalDateTime.now()
                )
            }
        }
    }

    private fun importExercise(
        languageCode: String,
        moduleNumber: Int,
        exerciseRequest: ExerciseImportRequest,
        generateAudio: Boolean
    ): Long? {
        try {
            val exerciseType = exerciseTypeRepository.findByTypeKey(exerciseRequest.type)
                ?: throw IllegalArgumentException("Unknown exercise type: ${exerciseRequest.type}")

            // Create exercise
            var exercise = Exercise(
                exerciseType = exerciseType,
                languageCode = languageCode,
                cefrLevel = "A1", // TODO: Get from course
                title = exerciseRequest.title,
                instructions = exerciseRequest.instructions,
                content = exerciseRequest.content,
                estimatedDurationSeconds = exerciseRequest.estimatedDurationSeconds,
                pointsValue = exerciseRequest.pointsValue,
                difficultyRating = exerciseRequest.difficultyRating,
                isPublished = true
            )

            // Generate audio for listening exercises if requested
            if (generateAudio && exerciseRequest.generateAudio && exerciseRequest.type == "listening") {
                val audioUrlField = exerciseRequest.content.get("audioUrl")?.asText()
                val transcript = exerciseRequest.content.get("transcript")?.asText()

                if (transcript != null && (audioUrlField == null || audioUrlField.isBlank() || audioUrlField == "placeholder_will_be_generated")) {
                    try {
                        val audioUrl = audioGenerationService.generateAudio(
                            transcript = transcript,
                            languageCode = languageCode,
                            moduleNumber = moduleNumber,
                            speakers = getVoiceForLanguage(languageCode)
                        )

                        // Update content with audio URL
                        val updatedContent = (exerciseRequest.content as com.fasterxml.jackson.databind.node.ObjectNode)
                            .put("audioUrl", audioUrl)
                        exercise = exercise.copy(content = updatedContent)

                        logger.info("Generated audio for listening exercise: ${exercise.title}")
                    } catch (e: Exception) {
                        logger.error("Failed to generate audio for listening exercise: ${exercise.title}", e)
                    }
                }
            }

            val savedExercise = exerciseRepository.save(exercise)
            return savedExercise.id

        } catch (e: Exception) {
            logger.error("Failed to import exercise: ${exerciseRequest.title}", e)
            return null
        }
    }

    private fun importGrammarRule(
        languageCode: String,
        cefrLevel: String,
        grammarRequest: GrammarRuleImportRequest
    ): Long {
        val grammarRule = GrammarRule(
            languageCode = languageCode,
            cefrLevel = cefrLevel,
            title = grammarRequest.title,
            explanation = grammarRequest.explanation,
            examples = if (grammarRequest.examples.isNotEmpty()) {
                objectMapper.valueToTree(grammarRequest.examples)
            } else null,
            category = grammarRequest.category
        )

        val savedRule = grammarRuleRepository.save(grammarRule)
        return savedRule.id!!
    }

    /**
     * Creates or updates a module based on the request
     */
    private fun createOrUpdateModule(
        courseId: Long,
        moduleRequest: ModuleImportRequest
    ): Module {
        val objectivesNode: JsonNode? = if (moduleRequest.objectives.isNotEmpty()) {
            objectMapper.valueToTree(moduleRequest.objectives)
        } else null

        val module = moduleRepository.findByCourseIdAndModuleNumber(courseId, moduleRequest.moduleNumber)
            ?.copy(
                title = moduleRequest.title,
                theme = moduleRequest.theme,
                description = moduleRequest.description,
                objectives = objectivesNode,
                estimatedMinutes = moduleRequest.estimatedMinutes,
                updatedAt = java.time.LocalDateTime.now()
            )
            ?: Module(
                courseId = courseId,
                moduleNumber = moduleRequest.moduleNumber,
                title = moduleRequest.title,
                theme = moduleRequest.theme,
                description = moduleRequest.description,
                objectives = objectivesNode,
                estimatedMinutes = moduleRequest.estimatedMinutes
            )

        return moduleRepository.save(module)
    }

    /**
     * Data class to hold processed dialogue information
     */
    private data class ProcessedDialogue(
        val content: String,
        val speakers: List<SpeakerVoiceConfig>,
        val audioStyle: String?
    )

    /**
     * Processes structured dialogue from episode request
     */
    private fun processDialogue(dialogue: StructuredDialogue): ProcessedDialogue {
        // Validate: all speakers in lines exist in speakers map
        val speakerNames = dialogue.lines.map { it.speaker }.distinct()
        val missingSpeakers = speakerNames.filterNot { dialogue.speakers.containsKey(it) }
        if (missingSpeakers.isNotEmpty()) {
            throw IllegalArgumentException("Speakers not defined in dialogue.speakers: ${missingSpeakers.joinToString()}")
        }

        // Validate: max 2 speakers for multi-speaker TTS
        if (speakerNames.size > 2) {
            throw IllegalArgumentException("Maximum 2 speakers supported for dialogue, found: ${speakerNames.size}")
        }

        // Build content from dialogue lines
        val content = dialogue.lines.joinToString("\n\n") { line ->
            "${line.speaker}: ${line.text}"
        }

        // Extract speaker voice configs
        val speakers = speakerNames.map { name ->
            val config = dialogue.speakers[name]!!
            SpeakerVoiceConfig(name = name, voice = config.voice)
        }

        return ProcessedDialogue(content, speakers, dialogue.audioStyle)
    }

    /**
     * Processes a single episode and returns the saved episode
     */
    private fun processEpisode(
        episodeRequest: EpisodeImportRequest,
        moduleId: Long,
        course: Course,
        moduleNumber: Int,
        generateAudio: Boolean
    ): Episode {
        val episodeType = try {
            EpisodeType.valueOf(episodeRequest.type.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid episode type: ${episodeRequest.type}")
        }

        // Process dialogue
        val processedDialogue = processDialogue(episodeRequest.dialogue)

        // Create episode
        var episode = Episode(
            moduleId = moduleId,
            episodeNumber = episodeRequest.episodeNumber,
            episodeType = episodeType,
            title = episodeRequest.title,
            content = processedDialogue.content,
            transcript = episodeRequest.transcript,
            estimatedMinutes = episodeRequest.estimatedMinutes
        )

        // Generate audio if requested
        if (generateAudio && episodeRequest.generateAudio &&
            (episodeRequest.type == "DIALOGUE" || episodeRequest.type == "STORY")
        ) {
            try {
                val audioUrl = audioGenerationService.generateAudio(
                    transcript = processedDialogue.content,
                    languageCode = course.languageCode,
                    moduleNumber = moduleNumber,
                    speakers = processedDialogue.speakers,
                    stylePrompt = processedDialogue.audioStyle
                )
                episode = episode.copy(audioUrl = audioUrl)
                logger.info("Generated audio for episode: ${episode.title} (speakers: ${processedDialogue.speakers.size})")
            } catch (e: Exception) {
                logger.error("Failed to generate audio for episode: ${episode.title}", e)
                throw e
            }
        }

        return episodeRepository.save(episode)
    }

    /**
     * Result of importing content items
     */
    private data class ContentItemImportResult(
        val exercisesImported: Int,
        val errors: List<String>
    )

    /**
     * Imports all content items for an episode
     */
    private fun importContentItems(
        episodeId: Long,
        episodeNumber: Int,
        contentItems: List<ContentItemImportRequest>,
        languageCode: String,
        cefrLevel: String,
        moduleNumber: Int,
        generateAudio: Boolean
    ): ContentItemImportResult {
        val errors = mutableListOf<String>()
        var exercisesImported = 0

        // Delete existing content items for this episode (if updating)
        episodeContentItemRepository.deleteByEpisodeId(episodeId)

        for (contentItemRequest in contentItems) {
            try {
                val contentType = ContentItemType.valueOf(contentItemRequest.type.uppercase())
                var contentId: Long? = null

                when (contentType) {
                    ContentItemType.EXERCISE -> {
                        if (contentItemRequest.exercise == null) {
                            errors.add("Episode $episodeNumber, content item ${contentItemRequest.orderIndex}: Missing exercise data")
                            continue
                        }

                        val exerciseId = importExercise(
                            languageCode,
                            moduleNumber,
                            contentItemRequest.exercise,
                            generateAudio
                        )

                        if (exerciseId != null) {
                            contentId = exerciseId
                            exercisesImported++
                        } else {
                            errors.add("Episode $episodeNumber, content item ${contentItemRequest.orderIndex}: Failed to import exercise")
                            continue
                        }
                    }

                    ContentItemType.GRAMMAR_RULE -> {
                        if (contentItemRequest.grammarRule == null) {
                            errors.add("Episode $episodeNumber, content item ${contentItemRequest.orderIndex}: Missing grammar rule data")
                            continue
                        }

                        val grammarRuleId = importGrammarRule(
                            languageCode,
                            cefrLevel,
                            contentItemRequest.grammarRule
                        )
                        contentId = grammarRuleId
                    }

                    else -> {
                        errors.add("Episode $episodeNumber, content item ${contentItemRequest.orderIndex}: Unsupported content type '${contentItemRequest.type}'")
                        continue
                    }
                }

                // Create content item link
                val contentItem = EpisodeContentItem(
                    episodeId = episodeId,
                    orderIndex = contentItemRequest.orderIndex,
                    contentType = contentType,
                    contentId = contentId,
                    isRequired = contentItemRequest.isRequired
                )

                episodeContentItemRepository.save(contentItem)

            } catch (e: Exception) {
                logger.error(
                    "Failed to import content item ${contentItemRequest.orderIndex} for episode $episodeNumber",
                    e
                )
                errors.add("Episode $episodeNumber, content item ${contentItemRequest.orderIndex}: ${e.message}")
            }
        }

        return ContentItemImportResult(exercisesImported, errors)
    }
}
