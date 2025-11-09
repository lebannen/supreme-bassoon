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

        return try {
            val session = studySessionService.startSession(userId, request)
            ResponseEntity.status(HttpStatus.CREATED).body(session)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
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

        return try {
            val session = studySessionService.getSession(id, userId)
            ResponseEntity.ok(session)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/sessions/{id}")
    fun abandonSession(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return try {
            studySessionService.abandonSession(id, userId)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    // ========== Study Flow ==========

    @GetMapping("/sessions/{id}/next-card")
    fun getNextCard(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long
    ): ResponseEntity<NextCardResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return try {
            val card = studySessionService.getNextCard(id, userId)
                ?: return ResponseEntity.noContent().build()
            ResponseEntity.ok(card)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @PostMapping("/sessions/{id}/answer")
    fun submitAnswer(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long,
        @RequestBody request: AnswerRequest
    ): ResponseEntity<AnswerResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return try {
            val response = studySessionService.submitAnswer(id, userId, request)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @PostMapping("/sessions/{id}/complete")
    fun completeSession(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long
    ): ResponseEntity<SessionSummaryResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return try {
            val summary = studySessionService.completeSession(id, userId)
            ResponseEntity.ok(summary)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
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

        return try {
            // For now, we'll return the complete summary
            // This could be cached or stored separately in the future
            val summary = studySessionService.completeSession(id, userId)
            ResponseEntity.ok(summary)
        } catch (e: IllegalStateException) {
            // Already completed, fetch from session
            try {
                val session = studySessionService.getSession(id, userId)
                // Convert to summary (this would need additional method)
                ResponseEntity.noContent().build()
            } catch (e: IllegalArgumentException) {
                ResponseEntity.notFound().build()
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
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

    private fun getUserIdFromToken(authorization: String): Long? {
        val token = authorization.removePrefix("Bearer ").trim()
        return jwtService.getUserIdFromToken(token)
    }
}
