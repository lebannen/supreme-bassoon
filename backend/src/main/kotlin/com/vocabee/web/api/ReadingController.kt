package com.vocabee.web.api

import com.vocabee.domain.model.ReadingText
import com.vocabee.domain.model.UserReadingProgress
import com.vocabee.service.JwtService
import com.vocabee.service.ReadingTextService
import com.vocabee.service.UserService
import com.vocabee.web.dto.ImportTextRequest
import com.vocabee.web.dto.ReadingTextDto
import com.vocabee.web.dto.UpdateProgressRequest
import com.vocabee.web.dto.UserReadingProgressDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reading")
class ReadingController(
    private val readingTextService: ReadingTextService,
    private val userService: UserService,
    private val jwtService: JwtService
) {
    private val logger = LoggerFactory.getLogger(ReadingController::class.java)

    @GetMapping("/texts")
    fun getTexts(
        @RequestParam(required = false) languageCode: String?,
        @RequestParam(required = false) level: String?,
        @RequestParam(required = false) topic: String?
    ): ResponseEntity<List<ReadingTextDto>> {
        val texts = readingTextService.getAllTexts(
            languageCode = languageCode,
            level = level,
            topic = topic,
            publishedOnly = true
        )

        return ResponseEntity.ok(texts.map { it.toDto() })
    }

    @GetMapping("/texts/{id}")
    fun getText(@PathVariable id: Long): ResponseEntity<ReadingTextDto> {
        val text = readingTextService.getTextById(id)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(text.toDto())
    }

    @PostMapping("/texts/import")
    fun importText(@RequestBody request: ImportTextRequest): ResponseEntity<ReadingTextDto> {
        logger.info("Importing text: ${request.title}")

        val text = readingTextService.createText(
            title = request.title,
            content = request.content,
            languageCode = request.languageCode,
            level = request.level,
            topic = request.topic,
            description = request.description,
            author = request.author,
            source = request.source,
            audioUrl = request.audioUrl
        )

        return ResponseEntity.ok(text.toDto())
    }

    @PatchMapping("/texts/{id}/audio")
    fun updateAudioUrl(
        @PathVariable id: Long,
        @RequestBody request: com.vocabee.web.dto.UpdateAudioUrlRequest
    ): ResponseEntity<ReadingTextDto> {
        logger.info("Updating audio URL for text $id")

        return try {
            val updatedText = readingTextService.updateAudioUrl(id, request.audioUrl)
            ResponseEntity.ok(updatedText.toDto())
        } catch (e: IllegalArgumentException) {
            logger.error("Text not found: ${e.message}")
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/texts/{id}/progress")
    fun getProgress(
        @PathVariable id: Long,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<UserReadingProgressDto> {
        val userId = getUserIdFromToken(authorization) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val progress = readingTextService.getUserProgress(userId, id)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(progress.toDto())
    }

    @PostMapping("/texts/{id}/progress")
    fun updateProgress(
        @PathVariable id: Long,
        @RequestBody request: UpdateProgressRequest,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<UserReadingProgressDto> {
        val userId = getUserIdFromToken(authorization) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val progress = readingTextService.updateProgress(
            userId = userId,
            textId = id,
            currentPage = request.currentPage,
            totalPages = request.totalPages
        )

        return ResponseEntity.ok(progress.toDto())
    }

    @PostMapping("/texts/{id}/complete")
    fun completeText(
        @PathVariable id: Long,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<UserReadingProgressDto> {
        val userId = getUserIdFromToken(authorization) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val progress = readingTextService.markCompleted(userId, id)

        return ResponseEntity.ok(progress.toDto())
    }

    @GetMapping("/progress")
    fun getAllProgress(@RequestHeader("Authorization") authorization: String): ResponseEntity<List<UserReadingProgressDto>> {
        val userId = getUserIdFromToken(authorization) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val progress = readingTextService.getAllUserProgress(userId)

        return ResponseEntity.ok(progress.map { it.toDto() })
    }

    @GetMapping("/progress/completed")
    fun getCompletedTexts(@RequestHeader("Authorization") authorization: String): ResponseEntity<List<UserReadingProgressDto>> {
        val userId = getUserIdFromToken(authorization) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val progress = readingTextService.getCompletedTexts(userId)

        return ResponseEntity.ok(progress.map { it.toDto() })
    }

    @GetMapping("/progress/in-progress")
    fun getInProgressTexts(@RequestHeader("Authorization") authorization: String): ResponseEntity<List<UserReadingProgressDto>> {
        val userId = getUserIdFromToken(authorization) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val progress = readingTextService.getInProgressTexts(userId)

        return ResponseEntity.ok(progress.map { it.toDto() })
    }

    private fun getUserIdFromToken(authorization: String): Long? {
        val token = authorization.removePrefix("Bearer ")
        val email = jwtService.getUserEmailFromToken(token) ?: return null
        val user = userService.findByEmail(email) ?: return null
        return user.id
    }

    private fun ReadingText.toDto() = ReadingTextDto(
        id = id!!,
        title = title,
        content = content,
        languageCode = languageCode,
        level = level,
        topic = topic,
        wordCount = wordCount,
        description = description,
        estimatedMinutes = estimatedMinutes,
        difficulty = difficulty,
        isPublished = isPublished,
        createdAt = createdAt,
        updatedAt = updatedAt,
        author = author,
        source = source,
        audioUrl = audioUrl
    )

    private fun UserReadingProgress.toDto() = UserReadingProgressDto(
        id = id,
        userId = userId,
        textId = textId,
        currentPage = currentPage,
        totalPages = totalPages,
        completed = completed,
        startedAt = startedAt,
        completedAt = completedAt,
        lastReadAt = lastReadAt
    )
}
