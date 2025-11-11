package com.vocabee.web.api

import com.vocabee.service.ExerciseService
import com.vocabee.service.JwtService
import com.vocabee.web.dto.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/exercises")
class ExerciseController(
    private val exerciseService: ExerciseService,
    private val jwtService: JwtService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getExercises(
        @RequestParam languageCode: String,
        @RequestParam(required = false) module: Int?,
        @RequestParam(required = false) topic: String?,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<List<ExerciseSummaryDto>> {
        logger.info("Getting exercises: lang=$languageCode, module=$module, topic=$topic, type=$type")
        val exercises = exerciseService.getExercises(languageCode, module, topic, type)
        return ResponseEntity.ok(exercises)
    }

    @GetMapping("/{id}")
    fun getExercise(@PathVariable id: Long): ResponseEntity<ExerciseDto> {
        logger.info("Getting exercise: $id")
        return try {
            val exercise = exerciseService.getExerciseById(id)
            ResponseEntity.ok(exercise)
        } catch (e: jakarta.persistence.EntityNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{id}/attempt")
    fun submitAttempt(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long,
        @RequestBody request: SubmitAttemptRequest
    ): ResponseEntity<AttemptResultDto> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info("User $userId submitting attempt for exercise $id")

        return try {
            val result = exerciseService.submitAttempt(userId, id, request)
            ResponseEntity.ok(result)
        } catch (e: jakarta.persistence.EntityNotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Error submitting attempt", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/{id}/progress")
    fun getProgress(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long
    ): ResponseEntity<UserProgressDto> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val progress = exerciseService.getUserProgress(userId, id)
        return ResponseEntity.ok(progress)
    }

    private fun getUserIdFromToken(authorization: String): Long? {
        val token = authorization.removePrefix("Bearer ").trim()
        return jwtService.getUserIdFromToken(token)
    }
}
