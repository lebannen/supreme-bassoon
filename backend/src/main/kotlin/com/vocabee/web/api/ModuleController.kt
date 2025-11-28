package com.vocabee.web.api

import com.vocabee.domain.repository.CourseRepository
import com.vocabee.domain.repository.EpisodeContentItemRepository
import com.vocabee.domain.repository.EpisodeRepository
import com.vocabee.domain.repository.ModuleRepository
import com.vocabee.service.JwtService
import com.vocabee.web.dto.ModuleDto
import com.vocabee.web.dto.toDto
import com.vocabee.web.dto.toSummaryDto
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/modules")
class ModuleController(
    private val moduleRepository: ModuleRepository,
    private val courseRepository: CourseRepository,
    private val episodeRepository: EpisodeRepository,
    private val episodeContentItemRepository: EpisodeContentItemRepository,
    private val jwtService: JwtService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/{id}")
    fun getModule(@PathVariable id: Long): ResponseEntity<ModuleDto> {
        logger.info("Getting module: $id")

        val module = moduleRepository.findById(id)
            .orElse(null) ?: return ResponseEntity.notFound().build()

        val course = courseRepository.findById(module.courseId)
            .orElse(null) ?: return ResponseEntity.notFound().build()

        val episodes = episodeRepository.findByModuleIdOrderByEpisodeNumber(module.id!!)
        val episodeSummaries = episodes.map { episode ->
            val contentItems = episodeContentItemRepository.findByEpisodeIdOrderByOrderIndex(episode.id!!)
            val exerciseCount = contentItems.count { it.contentType.name == "EXERCISE" }
            episode.toSummaryDto(totalExercises = exerciseCount)
        }

        return ResponseEntity.ok(module.toDto(courseSlug = course.slug, episodes = episodeSummaries))
    }
}
