package com.vocabee.web.api

import com.vocabee.domain.repository.*
import com.vocabee.service.JwtService
import com.vocabee.web.dto.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/courses")
class CourseController(
    private val courseRepository: CourseRepository,
    private val moduleRepository: ModuleRepository,
    private val episodeRepository: EpisodeRepository,
    private val episodeContentItemRepository: EpisodeContentItemRepository,
    private val exerciseRepository: ExerciseRepository,
    private val grammarRuleRepository: GrammarRuleRepository,
    private val jwtService: JwtService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getAllCourses(): ResponseEntity<List<CourseSummaryDto>> {
        logger.info("Getting all published courses")

        val courses = courseRepository.findByIsPublished(true)
        val courseSummaries = courses.map { course ->
            val modules = moduleRepository.findByCourseIdOrderByModuleNumber(course.id!!)
            val totalEpisodes = modules.sumOf { module ->
                episodeRepository.findByModuleIdOrderByEpisodeNumber(module.id!!).size
            }
            course.toSummaryDto(totalModules = modules.size, totalEpisodes = totalEpisodes)
        }

        return ResponseEntity.ok(courseSummaries)
    }

    @GetMapping("/{slug}")
    fun getCourse(@PathVariable slug: String): ResponseEntity<CourseDto> {
        logger.info("Getting course: $slug")

        val course = courseRepository.findBySlug(slug)
            ?: return ResponseEntity.notFound().build()

        val modules = moduleRepository.findByCourseIdOrderByModuleNumber(course.id!!)
        val moduleSummaries = modules.map { module ->
            val episodes = episodeRepository.findByModuleIdOrderByEpisodeNumber(module.id!!)
            module.toSummaryDto(totalEpisodes = episodes.size)
        }

        return ResponseEntity.ok(course.toDto(modules = moduleSummaries))
    }

    @GetMapping("/{slug}/modules")
    fun getCourseModules(@PathVariable slug: String): ResponseEntity<List<ModuleSummaryDto>> {
        logger.info("Getting modules for course: $slug")

        val course = courseRepository.findBySlug(slug)
            ?: return ResponseEntity.notFound().build()

        val modules = moduleRepository.findByCourseIdOrderByModuleNumber(course.id!!)
        val moduleSummaries = modules.map { module ->
            val episodes = episodeRepository.findByModuleIdOrderByEpisodeNumber(module.id!!)
            module.toSummaryDto(totalEpisodes = episodes.size)
        }

        return ResponseEntity.ok(moduleSummaries)
    }
}
