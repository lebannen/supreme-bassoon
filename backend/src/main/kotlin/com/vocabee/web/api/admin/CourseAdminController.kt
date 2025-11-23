package com.vocabee.web.api.admin

import com.vocabee.domain.generation.GeneratedEpisodeSummary
import com.vocabee.domain.repository.*
import com.vocabee.service.AudioGenerationService
import com.vocabee.service.EpisodeValidationService
import com.vocabee.service.StorageService
import com.vocabee.service.content.ContentGenerationService
import com.vocabee.web.dto.CourseAdminDto
import com.vocabee.web.dto.ModuleAdminDto
import com.vocabee.web.dto.ModuleDetailDto
import com.vocabee.web.dto.admin.*
import com.vocabee.web.dto.admin.generation.GenerateEpisodeContentRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/courses")
class CourseAdminController(
    private val courseRepository: CourseRepository,
    private val moduleRepository: ModuleRepository,
    private val episodeRepository: EpisodeRepository,
    private val episodeContentItemRepository: EpisodeContentItemRepository,
    private val exerciseRepository: ExerciseRepository,
    private val exerciseTypeRepository: ExerciseTypeRepository,
    private val audioGenerationService: AudioGenerationService,
    private val storageService: StorageService,
    private val contentGenerationService: ContentGenerationService,
    private val episodeValidationService: EpisodeValidationService,
    private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getAllCourses(): ResponseEntity<List<CourseAdminDto>> {
        logger.info("Admin: Getting all courses")

        val courses = courseRepository.findAll()
        val courseAdminDtos = courses.map { course ->
            CourseAdminDto(
                id = course.id!!,
                slug = course.slug,
                name = course.name,
                languageCode = course.languageCode,
                cefrLevel = course.cefrLevel,
                estimatedHours = course.estimatedHours,
                isPublished = course.isPublished,
                seriesContext = course.seriesContext
            )
        }

        return ResponseEntity.ok(courseAdminDtos)
    }

    @GetMapping("/{courseId}")
    fun getCourse(@PathVariable courseId: Long): ResponseEntity<CourseAdminDto> {
        logger.info("Admin: Getting course $courseId")

        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }

        return ResponseEntity.ok(
            CourseAdminDto(
                id = course.id!!,
                slug = course.slug,
                name = course.name,
                languageCode = course.languageCode,
                cefrLevel = course.cefrLevel,
                estimatedHours = course.estimatedHours,
                isPublished = course.isPublished,
                seriesContext = course.seriesContext
            )
        )
    }

    @GetMapping("/{courseId}/modules")
    fun getModulesForCourse(@PathVariable courseId: Long): ResponseEntity<List<ModuleAdminDto>> {
        logger.info("Admin: Getting modules for course $courseId")

        val modules = moduleRepository.findByCourseIdOrderByModuleNumber(courseId)
        val moduleAdminDtos = modules.map { module ->
            val episodeCount = episodeRepository.findByModuleIdOrderByEpisodeNumber(module.id!!).size
            ModuleAdminDto(
                id = module.id,
                moduleNumber = module.moduleNumber,
                title = module.title,
                episodeCount = episodeCount
            )
        }

        return ResponseEntity.ok(moduleAdminDtos)
    }

    @PutMapping("/{courseId}/publish")
    @Transactional
    fun togglePublishStatus(
        @PathVariable courseId: Long,
        @RequestParam publish: Boolean
    ): ResponseEntity<CourseAdminDto> {
        logger.info("Admin: Toggling publish status for course $courseId to $publish")

        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }

        course.isPublished = publish
        courseRepository.save(course)

        return ResponseEntity.ok(
            CourseAdminDto(
                id = course.id!!,
                slug = course.slug,
                name = course.name,
                languageCode = course.languageCode,
                cefrLevel = course.cefrLevel,
                estimatedHours = course.estimatedHours,
                isPublished = course.isPublished,
                seriesContext = course.seriesContext
            )
        )
    }

    @DeleteMapping("/{courseId}")
    @Transactional
    fun deleteCourse(@PathVariable courseId: Long): ResponseEntity<Void> {
        logger.info("Admin: Deleting course $courseId")

        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }

        // Delete all modules and their episodes (cascade should handle this, but doing it explicitly for safety)
        val modules = moduleRepository.findByCourseIdOrderByModuleNumber(courseId)
        modules.forEach { module ->
            val episodes = episodeRepository.findByModuleIdOrderByEpisodeNumber(module.id!!)
            episodes.forEach { episode ->
                episodeContentItemRepository.deleteByEpisodeId(episode.id!!)
            }
            episodeRepository.deleteByModuleId(module.id)
        }
        moduleRepository.deleteByCourseId(courseId)

        // Finally delete the course
        courseRepository.delete(course)

        logger.info("Admin: Successfully deleted course $courseId and all associated content")
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/modules/{moduleId}")
    fun getModule(@PathVariable moduleId: Long): ResponseEntity<ModuleDetailDto> {
        logger.info("Admin: Getting module $moduleId")

        val module = moduleRepository.findById(moduleId)
            .orElseThrow { IllegalArgumentException("Module not found: $moduleId") }

        val episodeCount = episodeRepository.findByModuleIdOrderByEpisodeNumber(module.id!!).size

        val objectives = module.objectives?.let { node ->
            if (node.isArray) node.map { it.asText() } else null
        }

        val vocabularyFocus = module.vocabularyFocus?.let { node ->
            if (node.isArray) node.map { it.asText() } else null
        }

        val grammarFocus = module.grammarFocus?.let { node ->
            if (node.isArray) node.map { it.asText() } else null
        }

        return ResponseEntity.ok(
            ModuleDetailDto(
                id = module.id,
                moduleNumber = module.moduleNumber,
                title = module.title,
                theme = module.theme,
                description = module.description,
                objectives = objectives,
                vocabularyFocus = vocabularyFocus,
                grammarFocus = grammarFocus,
                episodeOutline = module.episodeOutline,
                episodeCount = episodeCount
            )
        )
    }

    @DeleteMapping("/modules/{moduleId}")
    @Transactional
    fun deleteModule(@PathVariable moduleId: Long): ResponseEntity<Void> {
        logger.info("Admin: Deleting module $moduleId")

        val module = moduleRepository.findById(moduleId)
            .orElseThrow { IllegalArgumentException("Module not found: $moduleId") }

        // Get all episodes for this module
        val episodes = episodeRepository.findByModuleIdOrderByEpisodeNumber(moduleId)

        // Delete audio files and content items for each episode
        episodes.forEach { episode ->
            // Delete audio file from storage if it exists
            episode.audioUrl?.let { audioUrl ->
                try {
                    val (bucket, key) = parseStorageUrl(audioUrl)
                    storageService.deleteFile(bucket, key)
                    logger.info("Deleted audio file: $audioUrl")
                } catch (e: Exception) {
                    logger.error("Failed to delete audio file: $audioUrl", e)
                }
            }

            episodeContentItemRepository.deleteByEpisodeId(episode.id!!)
        }

        // Delete all episodes
        episodeRepository.deleteByModuleId(moduleId)

        // Delete the module
        moduleRepository.delete(module)

        logger.info("Admin: Successfully deleted module $moduleId with ${episodes.size} episodes")

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @PostMapping
    @Transactional
    fun createCourse(@RequestBody request: CreateCourseRequest): ResponseEntity<CourseAdminDto> {
        logger.info("Admin: Creating course for ${request.targetLanguage} ${request.level}")

        // 1. Create Course
        val slug = "${request.targetLanguage.lowercase()}-${request.level.lowercase()}-${System.currentTimeMillis()}"
        val course = com.vocabee.domain.model.Course(
            slug = slug,
            name = request.name,
            languageCode = request.targetLanguage,
            cefrLevel = request.level,
            description = "Generated course based on series context: ${request.seriesContext.take(50)}...",
            seriesContext = request.seriesContext,
            status = com.vocabee.domain.model.CourseStatus.DRAFT
        )
        val savedCourse = courseRepository.save(course)

        // 2. Create Modules from Syllabus (or enriched modules if provided)
        if (request.enrichedModules != null) {
            // Use enriched modules with full planning data
            request.enrichedModules.forEach { enrichedModule ->
                val module = com.vocabee.domain.model.Module(
                    courseId = savedCourse.id!!,
                    moduleNumber = enrichedModule.moduleNumber,
                    title = enrichedModule.title,
                    theme = enrichedModule.theme,
                    description = enrichedModule.detailedDescription ?: enrichedModule.description,
                    objectives = enrichedModule.objectives?.let { objectMapper.valueToTree(it) },
                    vocabularyFocus = enrichedModule.vocabularyFocus?.let { objectMapper.valueToTree(it) },
                    grammarFocus = enrichedModule.grammarFocus?.let { objectMapper.valueToTree(it) },
                    episodeOutline = enrichedModule.episodeOutline?.let { objectMapper.valueToTree(it) },
                    status = com.vocabee.domain.model.ModuleStatus.PLANNED
                )
                moduleRepository.save(module)
            }
        } else {
            // Fall back to basic syllabus if no enrichment
            request.syllabus.modules.forEach { moduleSummary ->
                val module = com.vocabee.domain.model.Module(
                    courseId = savedCourse.id!!,
                    moduleNumber = moduleSummary.moduleNumber,
                    title = moduleSummary.title,
                    theme = moduleSummary.theme,
                    description = moduleSummary.description,
                    status = com.vocabee.domain.model.ModuleStatus.PLANNED
                )
                moduleRepository.save(module)
            }
        }

        return ResponseEntity.ok(CourseAdminDto(
            id = savedCourse.id!!,
            slug = savedCourse.slug,
            name = savedCourse.name,
            languageCode = savedCourse.languageCode,
            cefrLevel = savedCourse.cefrLevel,
            estimatedHours = savedCourse.estimatedHours,
            isPublished = savedCourse.isPublished,
            seriesContext = savedCourse.seriesContext
        ))
    }

    @PutMapping("/modules/{moduleId}")
    @Transactional
    fun saveModule(
        @PathVariable moduleId: Long,
        @RequestBody request: com.vocabee.web.dto.admin.SaveModuleRequest
    ): ResponseEntity<ModuleAdminDto> {
        logger.info("Admin: Saving module $moduleId with ${request.episodes.size} episodes")

        val module = moduleRepository.findById(moduleId)
            .orElseThrow { IllegalArgumentException("Module not found: $moduleId") }

        // Delete existing episodes to replace with new ones
        val existingEpisodes = episodeRepository.findByModuleIdOrderByEpisodeNumber(moduleId)
        existingEpisodes.forEach { episode ->
            episodeContentItemRepository.deleteByEpisodeId(episode.id!!)
        }
        episodeRepository.deleteByModuleId(moduleId)

        // Get course for languageCode and level
        val course = courseRepository.findById(module.courseId)
            .orElseThrow { IllegalArgumentException("Course not found: ${module.courseId}") }

        // Create new episodes
        request.episodes.forEach { episodeRequest ->
            val contentJson = objectMapper.writeValueAsString(episodeRequest.content)

            // Extract plain text content for the content column
            val plainContent = if (episodeRequest.type == "DIALOGUE") {
                episodeRequest.content.dialogue?.lines?.joinToString("\n") { "${it.speaker}: ${it.text}" } ?: ""
            } else {
                episodeRequest.content.story ?: ""
            }

            var episode = com.vocabee.domain.model.Episode(
                moduleId = module.id!!,
                episodeNumber = episodeRequest.episodeNumber,
                title = episodeRequest.title,
                episodeType = com.vocabee.domain.model.EpisodeType.valueOf(episodeRequest.type),
                summary = episodeRequest.summary,
                content = plainContent,
                data = contentJson,
                status = com.vocabee.domain.model.EpisodeStatus.DRAFTING
            )

            // Generate audio if requested
            if (request.generateAudio && plainContent.isNotBlank()) {
                try {
                    logger.info("Generating audio for ${episodeRequest.type} episode: ${episodeRequest.title}")

                    // Get speaker configurations based on episode type
                    val speakers = if (episodeRequest.type == "DIALOGUE") {
                        // Extract unique speaker names from dialogue
                        val speakerNames = episodeRequest.content.dialogue?.lines
                            ?.map { it.speaker }
                            ?.distinct()
                            ?.take(2)  // Max 2 speakers for Gemini TTS
                            ?: listOf("Narrator")

                        logger.info("Dialogue speakers: $speakerNames")
                        com.vocabee.service.getVoicesForLanguage(course.languageCode, speakerNames)
                    } else {
                        // STORY type: single narrator voice
                        com.vocabee.service.getVoiceForLanguage(course.languageCode)
                    }

                    val audioUrl = audioGenerationService.generateAudio(
                        transcript = plainContent,
                        languageCode = course.languageCode,
                        moduleNumber = module.moduleNumber,
                        speakers = speakers
                    )

                    // Update episode with audio URL
                    episode = episode.copy(audioUrl = audioUrl)
                    logger.info("Successfully generated audio for episode: ${episodeRequest.title}")
                } catch (e: Exception) {
                    logger.error("Failed to generate audio for episode ${episodeRequest.title}: ${e.message}", e)
                    // Continue without audio - don't fail the whole process
                }
            }

            val savedEpisode = episodeRepository.save(episode)

            // Save exercises as EpisodeContentItems
            episodeRequest.content.exercises?.forEachIndexed { index, generatedExercise ->
                try {
                    // Map exercise type from AI format (MULTIPLE_CHOICE) to DB format (multiple_choice)
                    val dbTypeKey = mapExerciseTypeToDb(generatedExercise.exercise.type)

                    // Find or get exercise type
                    val exerciseType = exerciseTypeRepository.findByTypeKey(dbTypeKey)
                        ?: throw IllegalArgumentException("Unknown exercise type: $dbTypeKey (original: ${generatedExercise.exercise.type})")

                    // Create and save exercise
                    val exercise = com.vocabee.domain.model.Exercise(
                        exerciseType = exerciseType,
                        languageCode = course.languageCode,
                        cefrLevel = course.cefrLevel,
                        title = "Exercise ${index + 1}",
                        instructions = "Complete the exercise", // Could be extracted from content if needed
                        content = objectMapper.valueToTree(generatedExercise.exercise.content),
                        estimatedDurationSeconds = 60,
                        pointsValue = 10,
                        isPublished = true
                    )
                    val savedExercise = exerciseRepository.save(exercise)

                    // Create content item linking episode to exercise
                    val contentItem = com.vocabee.domain.model.EpisodeContentItem(
                        episodeId = savedEpisode.id!!,
                        orderIndex = index,
                        contentType = com.vocabee.domain.model.ContentItemType.EXERCISE,
                        contentId = savedExercise.id!!,
                        isRequired = true,
                        pointsValue = 10
                    )
                    episodeContentItemRepository.save(contentItem)

                    logger.debug("Saved exercise ${index + 1} for episode ${savedEpisode.title}")
                } catch (e: Exception) {
                    logger.error(
                        "Failed to save exercise ${index + 1} for episode ${savedEpisode.title}: ${e.message}",
                        e
                    )
                }
            }
        }

        // Update module status
        module.status = com.vocabee.domain.model.ModuleStatus.DRAFTING
        moduleRepository.save(module)

        return ResponseEntity.ok(ModuleAdminDto(
            id = module.id!!,
            moduleNumber = module.moduleNumber,
            title = module.title,
            episodeCount = request.episodes.size
        ))
    }

    @PostMapping("/{courseId}/generate-all")
    fun generateAllCourseContent(
        @PathVariable courseId: Long
    ): ResponseEntity<GenerateCourseContentResponse> {
        logger.info("Admin: Starting batch generation for course $courseId")

        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }

        val modules = moduleRepository.findByCourseIdOrderByModuleNumber(courseId)
        if (modules.isEmpty()) {
            throw IllegalArgumentException("No modules found for course $courseId")
        }

        val moduleResults = mutableListOf<ModuleGenerationResult>()
        val allCharacters = mutableMapOf<String, MutableList<EpisodeReference>>()
        var totalDialogueLines = 0

        // Generate content for each module
        modules.forEach { module ->
            logger.info("Processing module ${module.moduleNumber}: ${module.title}")

            // Parse episode outline
            val episodeOutline = try {
                val outlineNode = module.episodeOutline
                    ?: throw IllegalArgumentException("Module ${module.id} has no episode outline")
                objectMapper.treeToValue(outlineNode, Array<GeneratedEpisodeSummary>::class.java).toList()
            } catch (e: Exception) {
                logger.error("Failed to parse episode outline for module ${module.id}: ${e.message}", e)
                emptyList()
            }

            if (episodeOutline.isEmpty()) {
                logger.warn("Module ${module.moduleNumber} has no episodes in outline, skipping")
                moduleResults.add(
                    ModuleGenerationResult(
                        moduleId = module.id!!,
                        moduleNumber = module.moduleNumber,
                        title = module.title,
                        episodes = emptyList()
                    )
                )
                return@forEach
            }

            val episodeResults = mutableListOf<EpisodeGenerationResult>()

            // Generate each episode
            episodeOutline.forEach { outlineItem ->
                logger.info("Generating episode ${outlineItem.episodeNumber}: ${outlineItem.title} (${outlineItem.type})")

                try {
                    // Generate content
                    val generatedContent = contentGenerationService.generateEpisodeContent(
                        GenerateEpisodeContentRequest(
                            targetLanguage = course.languageCode,
                            level = course.cefrLevel,
                            seriesContext = course.seriesContext,
                            moduleTheme = module.theme ?: "",
                            episodeTitle = outlineItem.title,
                            episodeType = outlineItem.type,
                            episodeSummary = outlineItem.summary
                        )
                    )

                    // Validate content
                    val validation = episodeValidationService.validateEpisodeContent(
                        content = generatedContent,
                        type = outlineItem.type
                    )

                    logger.info("Episode ${outlineItem.episodeNumber} validation: isValid=${validation.isValid}, errors=${validation.errors.size}, warnings=${validation.warnings.size}")

                    // Extract characters from dialogues
                    if (outlineItem.type == "DIALOGUE" && generatedContent.dialogue != null) {
                        generatedContent.dialogue.lines.forEach { line ->
                            val speaker = line.speaker
                            allCharacters.getOrPut(speaker) { mutableListOf() }.add(
                                EpisodeReference(
                                    moduleNumber = module.moduleNumber,
                                    episodeNumber = outlineItem.episodeNumber,
                                    lineCount = 1
                                )
                            )
                            totalDialogueLines++
                        }
                    }

                    episodeResults.add(
                        EpisodeGenerationResult(
                            episodeNumber = outlineItem.episodeNumber,
                            title = outlineItem.title,
                            type = outlineItem.type,
                            content = generatedContent,
                            validation = validation
                        )
                    )

                } catch (e: Exception) {
                    logger.error("Failed to generate episode ${outlineItem.episodeNumber}: ${e.message}", e)

                    // Add failed episode with critical error
                    episodeResults.add(
                        EpisodeGenerationResult(
                            episodeNumber = outlineItem.episodeNumber,
                            title = outlineItem.title,
                            type = outlineItem.type,
                            content = null,
                            validation = com.vocabee.domain.validation.EpisodeValidationResult(
                                isValid = false,
                                issues = listOf(
                                    com.vocabee.domain.validation.ValidationIssue(
                                        field = "general",
                                        message = "Generation failed: ${e.message}",
                                        severity = com.vocabee.domain.validation.ErrorSeverity.CRITICAL
                                    )
                                )
                            )
                        )
                    )
                }
            }

            moduleResults.add(
                ModuleGenerationResult(
                    moduleId = module.id!!,
                    moduleNumber = module.moduleNumber,
                    title = module.title,
                    episodes = episodeResults
                )
            )
        }

        // Build character analysis
        val characterInfo = allCharacters.map { (name, episodes) ->
            // Group by episode to count lines per episode
            val episodeGroups = episodes.groupBy { Pair(it.moduleNumber, it.episodeNumber) }
            CharacterInfo(
                name = name,
                appearances = episodeGroups.values.sumOf { it.size },
                episodes = episodeGroups.map { (key, lines) ->
                    EpisodeReference(
                        moduleNumber = key.first,
                        episodeNumber = key.second,
                        lineCount = lines.size
                    )
                }.sortedWith(compareBy({ it.moduleNumber }, { it.episodeNumber }))
            )
        }.sortedByDescending { it.appearances }

        val characterAnalysis = CharacterAnalysis(
            characters = characterInfo,
            totalDialogues = totalDialogueLines
        )

        // Build validation summary
        val allEpisodes = moduleResults.flatMap { it.episodes }
        val validationSummary = ValidationSummary(
            totalEpisodes = allEpisodes.size,
            validEpisodes = allEpisodes.count { it.validation.isValid },
            episodesWithWarnings = allEpisodes.count { it.validation.hasWarnings && it.validation.isValid },
            episodesWithErrors = allEpisodes.count { !it.validation.isValid }
        )

        logger.info("Batch generation complete: ${validationSummary.validEpisodes}/${validationSummary.totalEpisodes} episodes valid")
        logger.info("Found ${characterInfo.size} characters across ${totalDialogueLines} dialogue lines")

        return ResponseEntity.ok(
            GenerateCourseContentResponse(
                modules = moduleResults,
                characterAnalysis = characterAnalysis,
                validationSummary = validationSummary
            )
        )
    }

    @PostMapping("/save-course-content")
    @Transactional
    fun saveCourseContent(
        @RequestBody request: SaveCourseContentRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Admin: Saving complete course content for course ${request.courseId} with ${request.modules.size} modules")

        val course = courseRepository.findById(request.courseId)
            .orElseThrow { IllegalArgumentException("Course not found: ${request.courseId}") }

        // Build voice assignment map
        val voiceMap = request.voiceAssignments.associateBy { it.characterName }

        var totalEpisodesSaved = 0
        var totalAudioGenerated = 0

        // Process each module
        request.modules.forEach { moduleRequest ->
            val module = moduleRepository.findById(moduleRequest.moduleId)
                .orElseThrow { IllegalArgumentException("Module not found: ${moduleRequest.moduleId}") }

            logger.info("Processing module ${module.moduleNumber}: ${module.title} with ${moduleRequest.episodes.size} episodes")

            // Delete existing episodes to replace with new ones
            val existingEpisodes = episodeRepository.findByModuleIdOrderByEpisodeNumber(moduleRequest.moduleId)
            existingEpisodes.forEach { episode ->
                episodeContentItemRepository.deleteByEpisodeId(episode.id!!)
            }
            episodeRepository.deleteByModuleId(moduleRequest.moduleId)

            // Create new episodes
            moduleRequest.episodes.forEach { episodeRequest ->
                val contentJson = objectMapper.writeValueAsString(episodeRequest.content)

                // Extract plain text content for the content column
                val plainContent = if (episodeRequest.type == "DIALOGUE") {
                    episodeRequest.content.dialogue?.lines?.joinToString("\n") { "${it.speaker}: ${it.text}" } ?: ""
                } else {
                    episodeRequest.content.story ?: ""
                }

                var episode = com.vocabee.domain.model.Episode(
                    moduleId = module.id!!,
                    episodeNumber = episodeRequest.episodeNumber,
                    title = episodeRequest.title,
                    episodeType = com.vocabee.domain.model.EpisodeType.valueOf(episodeRequest.type),
                    summary = episodeRequest.summary,
                    content = plainContent,
                    data = contentJson,
                    status = com.vocabee.domain.model.EpisodeStatus.DRAFTING
                )

                // Generate audio if requested
                if (request.generateAudio && plainContent.isNotBlank()) {
                    try {
                        logger.info("Generating audio for ${episodeRequest.type} episode: ${episodeRequest.title}")

                        // Get speaker configurations based on episode type
                        val speakers = if (episodeRequest.type == "DIALOGUE") {
                            // Extract unique speaker names from dialogue and map to assigned voices
                            val speakerNames = episodeRequest.content.dialogue?.lines
                                ?.map { it.speaker }
                                ?.distinct()
                                ?.take(2)  // Max 2 speakers for Gemini TTS
                                ?: listOf("Narrator")

                            logger.info("Dialogue speakers: $speakerNames")

                            // Use voice assignments if available
                            speakerNames.map { speakerName ->
                                val assignment = voiceMap[speakerName]
                                if (assignment != null) {
                                    logger.info("Using assigned voice for $speakerName: ${assignment.voiceName}")
                                    com.vocabee.service.SpeakerVoiceConfig(
                                        name = speakerName,
                                        voice = assignment.voiceName
                                    )
                                } else {
                                    // Fallback to default voice
                                    logger.warn("No voice assignment for $speakerName, using default")
                                    val defaultVoice = when (course.languageCode) {
                                        "fr" -> "Leda"
                                        "en" -> "Puck"
                                        "de" -> "Kore"
                                        else -> "Leda"
                                    }
                                    com.vocabee.service.SpeakerVoiceConfig(
                                        name = speakerName,
                                        voice = defaultVoice
                                    )
                                }
                            }
                        } else {
                            // STORY type: single narrator voice
                            com.vocabee.service.getVoiceForLanguage(course.languageCode)
                        }

                        val audioUrl = audioGenerationService.generateAudio(
                            transcript = plainContent,
                            languageCode = course.languageCode,
                            moduleNumber = module.moduleNumber,
                            speakers = speakers
                        )

                        // Update episode with audio URL
                        episode = episode.copy(audioUrl = audioUrl)
                        totalAudioGenerated++
                        logger.info("Successfully generated audio for episode: ${episodeRequest.title}")
                    } catch (e: Exception) {
                        logger.error("Failed to generate audio for episode ${episodeRequest.title}: ${e.message}", e)
                        // Continue without audio - don't fail the whole process
                    }
                }

                val savedEpisode = episodeRepository.save(episode)
                totalEpisodesSaved++

                // Save exercises as EpisodeContentItems
                episodeRequest.content.exercises?.forEachIndexed { index, generatedExercise ->
                    try {
                        // Map exercise type from AI format (MULTIPLE_CHOICE) to DB format (multiple_choice)
                        val dbTypeKey = mapExerciseTypeToDb(generatedExercise.exercise.type)

                        // Find or get exercise type
                        val exerciseType = exerciseTypeRepository.findByTypeKey(dbTypeKey)
                            ?: throw IllegalArgumentException("Unknown exercise type: $dbTypeKey (original: ${generatedExercise.exercise.type})")

                        // Create and save exercise
                        val exercise = com.vocabee.domain.model.Exercise(
                            exerciseType = exerciseType,
                            languageCode = course.languageCode,
                            cefrLevel = course.cefrLevel,
                            title = "Exercise ${index + 1}",
                            instructions = "Complete the exercise",
                            content = objectMapper.valueToTree(generatedExercise.exercise.content),
                            estimatedDurationSeconds = 60,
                            pointsValue = 10,
                            isPublished = true
                        )
                        val savedExercise = exerciseRepository.save(exercise)

                        // Create content item linking episode to exercise
                        val contentItem = com.vocabee.domain.model.EpisodeContentItem(
                            episodeId = savedEpisode.id!!,
                            orderIndex = index,
                            contentType = com.vocabee.domain.model.ContentItemType.EXERCISE,
                            contentId = savedExercise.id!!,
                            isRequired = true,
                            pointsValue = 10
                        )
                        episodeContentItemRepository.save(contentItem)

                        logger.debug("Saved exercise ${index + 1} for episode ${savedEpisode.title}")
                    } catch (e: Exception) {
                        logger.error(
                            "Failed to save exercise ${index + 1} for episode ${savedEpisode.title}: ${e.message}",
                            e
                        )
                    }
                }
            }

            // Update module status
            module.status = com.vocabee.domain.model.ModuleStatus.DRAFTING
            moduleRepository.save(module)
        }

        logger.info("Course content saved successfully: $totalEpisodesSaved episodes, $totalAudioGenerated audio files generated")

        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "episodesSaved" to totalEpisodesSaved,
                "audioGenerated" to totalAudioGenerated
            )
        )
    }

    /**
     * Parse storage URL to extract bucket and key
     * URL format: http://localhost:9000/bucket-name/path/to/file.ext
     */
    private fun parseStorageUrl(url: String): Pair<String, String> {
        val uri = java.net.URI(url)
        val path = uri.path.trimStart('/')
        val parts = path.split('/', limit = 2)

        if (parts.size < 2) {
            throw IllegalArgumentException("Invalid storage URL format: $url")
        }

        return Pair(parts[0], parts[1])
    }

    /**
     * Map exercise type from AI-generated format to database format
     * AI uses: MULTIPLE_CHOICE, FILL_IN_THE_BLANK, MATCHING
     * DB uses: multiple_choice, fill_in_blank, matching
     */
    private fun mapExerciseTypeToDb(aiType: String): String {
        return when (aiType.uppercase()) {
            "MULTIPLE_CHOICE", "MULTIPLE-CHOICE" -> "multiple_choice"
            "FILL_IN_THE_BLANK", "FILL_IN_BLANK", "FILL-IN-THE-BLANK" -> "fill_in_blank"
            "MATCHING" -> "matching"
            "LISTENING", "LISTENING_COMPREHENSION" -> "listening"
            "SENTENCE_SCRAMBLE", "SENTENCE-SCRAMBLE" -> "sentence_scramble"
            "CLOZE_READING", "CLOZE-READING" -> "cloze_reading"
            else -> aiType.lowercase().replace('-', '_')
        }
    }
}
