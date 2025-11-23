package com.vocabee.web.api.admin

import com.vocabee.domain.repository.CourseRepository
import com.vocabee.domain.repository.EpisodeContentItemRepository
import com.vocabee.domain.repository.EpisodeRepository
import com.vocabee.domain.repository.ModuleRepository
import com.vocabee.service.StorageService
import com.vocabee.web.dto.CourseAdminDto
import com.vocabee.web.dto.ModuleAdminDto
import com.vocabee.web.dto.admin.CreateCourseRequest
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
    private val storageService: StorageService,
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
            name = "${request.targetLanguage} ${request.level} Course",
            languageCode = request.targetLanguage,
            cefrLevel = request.level,
            description = "Generated course based on series context: ${request.seriesContext.take(50)}...",
            seriesContext = request.seriesContext,
            status = com.vocabee.domain.model.CourseStatus.DRAFT
        )
        val savedCourse = courseRepository.save(course)

        // 2. Create Modules from Syllabus
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

        // Create new episodes
        request.episodes.forEach { episodeRequest ->
            val contentJson = objectMapper.writeValueAsString(episodeRequest.content)
            
            // Extract plain text content for the content column
            val plainContent = if (episodeRequest.type == "DIALOGUE") {
                episodeRequest.content.dialogue?.lines?.joinToString("\n") { "${it.speaker}: ${it.text}" } ?: ""
            } else {
                episodeRequest.content.story ?: ""
            }

            val episode = com.vocabee.domain.model.Episode(
                moduleId = module.id!!,
                episodeNumber = episodeRequest.episodeNumber,
                title = episodeRequest.title,
                episodeType = com.vocabee.domain.model.EpisodeType.valueOf(episodeRequest.type),
                summary = episodeRequest.summary,
                content = plainContent,
                data = contentJson,
                status = com.vocabee.domain.model.EpisodeStatus.DRAFTING
            )
            episodeRepository.save(episode)
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
}
