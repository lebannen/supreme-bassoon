package com.vocabee.web.api

import com.vocabee.domain.model.ContentItemType
import com.vocabee.domain.repository.*
import com.vocabee.service.JwtService
import com.vocabee.web.dto.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/episodes")
class EpisodeController(
    private val episodeRepository: EpisodeRepository,
    private val episodeContentItemRepository: EpisodeContentItemRepository,
    private val exerciseRepository: ExerciseRepository,
    private val grammarRuleRepository: GrammarRuleRepository,
    private val userEpisodeProgressRepository: UserEpisodeProgressRepository,
    private val jwtService: JwtService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/{id}")
    fun getEpisode(@PathVariable id: Long): ResponseEntity<EpisodeDto> {
        logger.info("Getting episode: $id")

        val episode = episodeRepository.findById(id)
            .orElse(null) ?: return ResponseEntity.notFound().build()

        val contentItems = episodeContentItemRepository.findByEpisodeIdOrderByOrderIndex(episode.id!!)

        val contentItemDtos = contentItems.map { contentItem ->
            when (contentItem.contentType) {
                ContentItemType.EXERCISE -> {
                    val exercise = exerciseRepository.findById(contentItem.contentId).orElse(null)
                    EpisodeContentItemDto(
                        id = contentItem.id!!,
                        orderIndex = contentItem.orderIndex,
                        contentType = contentItem.contentType.name,
                        isRequired = contentItem.isRequired,
                        exercise = exercise?.toDto(),
                        grammarRule = null
                    )
                }
                ContentItemType.GRAMMAR_RULE -> {
                    val grammarRule = grammarRuleRepository.findById(contentItem.contentId).orElse(null)
                    EpisodeContentItemDto(
                        id = contentItem.id!!,
                        orderIndex = contentItem.orderIndex,
                        contentType = contentItem.contentType.name,
                        isRequired = contentItem.isRequired,
                        exercise = null,
                        grammarRule = grammarRule?.toDto()
                    )
                }
                else -> {
                    EpisodeContentItemDto(
                        id = contentItem.id!!,
                        orderIndex = contentItem.orderIndex,
                        contentType = contentItem.contentType.name,
                        isRequired = contentItem.isRequired,
                        exercise = null,
                        grammarRule = null
                    )
                }
            }
        }

        return ResponseEntity.ok(episode.toDto(contentItems = contentItemDtos))
    }

    @GetMapping("/{id}/progress")
    fun getEpisodeProgress(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long
    ): ResponseEntity<EpisodeProgressDto> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info("Getting episode progress for user $userId, episode $id")

        val progress = userEpisodeProgressRepository.findByUserIdAndEpisodeId(userId, id)
            ?: return ResponseEntity.ok(
                EpisodeProgressDto(
                    episodeId = id,
                    userId = userId,
                    hasReadContent = false,
                    hasListenedAudio = false,
                    completedExercises = emptyList(),
                    totalContentItems = 0,
                    requiredContentItemsCompleted = 0,
                    isCompleted = false,
                    timeSpentSeconds = 0,
                    averageScore = null
                )
            )

        val completedExerciseIds = if (progress.completedContentItems?.isArray == true) {
            progress.completedContentItems.map { it.asLong() }
        } else {
            emptyList()
        }

        return ResponseEntity.ok(
            EpisodeProgressDto(
                episodeId = progress.episodeId,
                userId = progress.userId,
                hasReadContent = progress.hasReadContent,
                hasListenedAudio = progress.hasListenedAudio,
                completedExercises = completedExerciseIds,
                totalContentItems = progress.totalContentItems,
                requiredContentItemsCompleted = progress.requiredContentItemsCompleted,
                isCompleted = progress.isCompleted,
                timeSpentSeconds = progress.timeSpentSeconds,
                averageScore = progress.averageScore
            )
        )
    }

    @PostMapping("/{id}/complete-content")
    fun markContentAsRead(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long
    ): ResponseEntity<EpisodeProgressDto> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info("Marking episode content as read for user $userId, episode $id")

        var progress = userEpisodeProgressRepository.findByUserIdAndEpisodeId(userId, id)
            ?: com.vocabee.domain.model.UserEpisodeProgress(
                userId = userId,
                episodeId = id,
                hasReadContent = true
            )

        if (!progress.hasReadContent) {
            progress = progress.copy(hasReadContent = true)
            userEpisodeProgressRepository.save(progress)
        }

        return getEpisodeProgress(authorization, id)
    }

    @PostMapping("/{episodeId}/complete-exercise/{exerciseId}")
    fun markExerciseAsCompleted(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable episodeId: Long,
        @PathVariable exerciseId: Long
    ): ResponseEntity<EpisodeProgressDto> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info("Marking exercise $exerciseId as completed for user $userId in episode $episodeId")

        var progress = userEpisodeProgressRepository.findByUserIdAndEpisodeId(userId, episodeId)
            ?: com.vocabee.domain.model.UserEpisodeProgress(
                userId = userId,
                episodeId = episodeId
            )

        // Get current completed exercises
        val currentCompleted = progress.completedContentItems
        val completedExercises = if (currentCompleted?.isArray == true) {
            currentCompleted.map { it.asLong() }.toMutableList()
        } else {
            mutableListOf()
        }

        // Add exercise if not already completed
        if (!completedExercises.contains(exerciseId)) {
            completedExercises.add(exerciseId)

            // Convert to JsonNode
            val mapper = com.fasterxml.jackson.databind.ObjectMapper()
            val completedJson = mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(completedExercises)

            progress = progress.copy(
                completedContentItems = completedJson,
                lastActivityAt = java.time.LocalDateTime.now()
            )

            userEpisodeProgressRepository.save(progress)
            logger.info("Exercise $exerciseId marked as completed. Total completed: ${completedExercises.size}")
        }

        return getEpisodeProgress(authorization, episodeId)
    }

    private fun getUserIdFromToken(authorization: String): Long? {
        val token = authorization.removePrefix("Bearer ").trim()
        return jwtService.getUserIdFromToken(token)
    }
}
