package com.vocabee.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vocabee.domain.model.*
import com.vocabee.domain.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
    val name: String,
    val voice: String,
    val description: String? = null
)

data class EpisodeImportRequest(
    val episodeNumber: Int,
    val type: String, // DIALOGUE, STORY, ARTICLE, AUDIO_LESSON
    val title: String,
    val content: String,
    val generateAudio: Boolean = false,
    val transcript: String? = null,
    val speakers: List<SpeakerConfig> = emptyList(),
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
        val objectivesNode: JsonNode? = if (moduleRequest.objectives.isNotEmpty()) {
            objectMapper.valueToTree(moduleRequest.objectives)
        } else {
            null
        }

        val module = moduleRepository.findByCourseIdAndModuleNumber(course.id!!, moduleRequest.moduleNumber)
            ?.copy(
                title = moduleRequest.title,
                theme = moduleRequest.theme,
                description = moduleRequest.description,
                objectives = objectivesNode,
                estimatedMinutes = moduleRequest.estimatedMinutes,
                updatedAt = java.time.LocalDateTime.now()
            )
            ?: Module(
                courseId = course.id,
                moduleNumber = moduleRequest.moduleNumber,
                title = moduleRequest.title,
                theme = moduleRequest.theme,
                description = moduleRequest.description,
                objectives = objectivesNode,
                estimatedMinutes = moduleRequest.estimatedMinutes
            )

        val savedModule = moduleRepository.save(module)
        logger.info("Module saved with ID: ${savedModule.id}")

        // Import episodes
        for (episodeRequest in moduleRequest.episodes) {
            try {
                val episodeType = try {
                    EpisodeType.valueOf(episodeRequest.type.uppercase())
                } catch (e: IllegalArgumentException) {
                    logger.error("Invalid episode type: ${episodeRequest.type}")
                    errors.add("Episode ${episodeRequest.episodeNumber}: Invalid type '${episodeRequest.type}'")
                    continue
                }

                // Create episode
                var episode = Episode(
                    moduleId = savedModule.id!!,
                    episodeNumber = episodeRequest.episodeNumber,
                    episodeType = episodeType,
                    title = episodeRequest.title,
                    content = episodeRequest.content,
                    transcript = episodeRequest.transcript,
                    estimatedMinutes = episodeRequest.estimatedMinutes
                )

                // Generate audio if requested
                if (generateAudio && episodeRequest.generateAudio && episodeRequest.type == "DIALOGUE") {
                    try {
                        // Convert SpeakerConfig to SpeakerVoiceConfig
                        val speakerVoiceConfigs = episodeRequest.speakers.map { speaker ->
                            SpeakerVoiceConfig(
                                name = speaker.name,
                                voice = speaker.voice
                            )
                        }

                        val audioUrl = audioGenerationService.generateAudio(
                            transcript = episodeRequest.content,
                            languageCode = course.languageCode,
                            moduleNumber = savedModule.moduleNumber,
                            voice = if (speakerVoiceConfigs.isEmpty()) "Leda" else speakerVoiceConfigs[0].voice,
                            speakers = speakerVoiceConfigs,
                            stylePrompt = episodeRequest.audioStyle
                        )
                        episode = episode.copy(audioUrl = audioUrl)
                        logger.info("Generated audio for episode: ${episode.title} (speakers: ${speakerVoiceConfigs.size})")
                    } catch (e: Exception) {
                        logger.error("Failed to generate audio for episode: ${episode.title}", e)
                        errors.add("Episode ${episodeRequest.episodeNumber}: Audio generation failed - ${e.message}")
                    }
                }

                val savedEpisode = episodeRepository.save(episode)
                episodesImported++
                logger.info("Episode saved with ID: ${savedEpisode.id}")

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

                                val exerciseId = importExercise(
                                    course.languageCode,
                                    savedModule.moduleNumber,
                                    contentItemRequest.exercise,
                                    generateAudio
                                )

                                if (exerciseId != null) {
                                    contentId = exerciseId
                                    exercisesImported++
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
                        logger.error("Failed to import content item ${contentItemRequest.orderIndex} for episode ${episodeRequest.episodeNumber}", e)
                        errors.add("Episode ${episodeRequest.episodeNumber}, content item ${contentItemRequest.orderIndex}: ${e.message}")
                    }
                }

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

                if (transcript != null && (audioUrlField == null || audioUrlField.isBlank())) {
                    try {
                        val audioUrl = audioGenerationService.generateAudio(
                            transcript = transcript,
                            languageCode = languageCode,
                            moduleNumber = moduleNumber
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
}
