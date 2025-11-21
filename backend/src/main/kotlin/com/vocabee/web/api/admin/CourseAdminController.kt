package com.vocabee.web.api.admin

import com.vocabee.domain.repository.CourseRepository
import com.vocabee.domain.repository.EpisodeContentItemRepository
import com.vocabee.domain.repository.EpisodeRepository
import com.vocabee.domain.repository.ModuleRepository
import com.vocabee.service.StorageService
import com.vocabee.web.dto.CourseAdminDto
import com.vocabee.web.dto.ModuleAdminDto
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
    private val storageService: StorageService
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
                isPublished = course.isPublished
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
