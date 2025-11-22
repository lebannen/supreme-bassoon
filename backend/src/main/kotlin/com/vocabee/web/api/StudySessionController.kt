package com.vocabee.web.api

import com.vocabee.service.JwtService
import com.vocabee.service.StudySessionService
import com.vocabee.service.UserVocabularyService
import com.vocabee.web.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.Instant

@RestController
@RequestMapping("/api/study")
class StudySessionController(
    private val studySessionService: StudySessionService,
    private val dueWordsService: com.vocabee.service.DueWordsService,
    private val jwtService: JwtService
) {

    // ========== Session Management ==========

    @PostMapping("/sessions/start")
    fun startSession(
        @RequestHeader("Authorization") authorization: String,
        @RequestBody request: StartSessionRequest
    ): ResponseEntity<SessionResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val session = studySessionService.startSession(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(session)
    }

    @GetMapping("/sessions/active")
    fun getActiveSession(
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<SessionResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val session = studySessionService.getActiveSession(userId)
            ?: return ResponseEntity.noContent().build()

        return ResponseEntity.ok(session)
    }

    @GetMapping("/sessions/{id}")
    fun getSession(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long
    ): ResponseEntity<SessionResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val session = studySessionService.getSession(id, userId)
        return ResponseEntity.ok(session)
    }

    @DeleteMapping("/sessions/{id}")
    fun abandonSession(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        studySessionService.abandonSession(id, userId)
        return ResponseEntity.noContent().build()
    }

    // ========== Study Flow ==========

    @GetMapping("/sessions/{id}/next-card")
    fun getNextCard(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long
    ): ResponseEntity<NextCardResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val card = studySessionService.getNextCard(id, userId)
            ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(card)
    }

    @PostMapping("/sessions/{id}/answer")
    fun submitAnswer(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long,
        @RequestBody request: AnswerRequest
    ): ResponseEntity<AnswerResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val response = studySessionService.submitAnswer(id, userId, request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/sessions/{id}/complete")
    fun completeSession(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long
    ): ResponseEntity<SessionSummaryResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val summary = studySessionService.completeSession(id, userId)
        return ResponseEntity.ok(summary)
    }

    // ========== Session History ==========

    @GetMapping("/sessions/history")
    fun getSessionHistory(
        @RequestHeader("Authorization") authorization: String,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<SessionHistoryResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val history = studySessionService.getSessionHistory(userId, limit)
        return ResponseEntity.ok(history)
    }

    @GetMapping("/sessions/{id}/summary")
    fun getSessionSummary(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long
    ): ResponseEntity<SessionSummaryResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        // This method attempts to complete the session if not already, then returns the summary.
        // If the session is already completed, `completeSession` might throw an IllegalStateException.
        // In this case, we let the exception propagate to the GlobalExceptionHandler.
        val summary = studySessionService.completeSession(id, userId)
        return ResponseEntity.ok(summary)
    }

    // ========== Due Words ==========

    @GetMapping("/due-words")
    fun getDueWordsCount(
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<DueWordsResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val response = dueWordsService.getDueWordsCount(userId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/due-words/list")
    fun getDueWordsList(
        @RequestHeader("Authorization") authorization: String,
        @RequestParam(defaultValue = "50") limit: Int
    ): ResponseEntity<DueWordsList> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val response = dueWordsService.getDueWordsList(userId, limit)
        return ResponseEntity.ok(response)
    }

    // ========== Helper Methods ==========

    private fun getUserIdFromToken(authorization: String): Long {
        val token = authorization.removePrefix("Bearer ").trim()
        return jwtService.getUserIdFromToken(token)
            ?: throw org.springframework.security.access.AccessDeniedException("Invalid token")
    }
}
