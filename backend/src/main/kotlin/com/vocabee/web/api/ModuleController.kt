package com.vocabee.web.api

import com.vocabee.domain.repository.*
import com.vocabee.service.JwtService
import com.vocabee.web.dto.*
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/modules")
class ModuleController(
    private val moduleRepository: ModuleRepository,
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

        val episodes = episodeRepository.findByModuleIdOrderByEpisodeNumber(module.id!!)
        val episodeSummaries = episodes.map { episode ->
            val contentItems = episodeContentItemRepository.findByEpisodeIdOrderByOrderIndex(episode.id!!)
            val exerciseCount = contentItems.count { it.contentType.name == "EXERCISE" }
            episode.toSummaryDto(totalExercises = exerciseCount)
        }

        return ResponseEntity.ok(module.toDto(episodes = episodeSummaries))
    }
}
