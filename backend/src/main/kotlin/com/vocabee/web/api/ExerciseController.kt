package com.vocabee.web.api

import com.vocabee.service.ExerciseService
import com.vocabee.service.JwtService
import com.vocabee.service.SpeakingExerciseService
import com.vocabee.web.dto.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/exercises")
class ExerciseController(
    private val exerciseService: ExerciseService,
    private val jwtService: JwtService,
    private val speakingExerciseService: SpeakingExerciseService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getExercises(
        @RequestParam languageCode: String,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<List<ExerciseSummaryDto>> {
        logger.info("Getting exercises: lang=$languageCode, type=$type")
        val exercises = exerciseService.getExercises(languageCode, type)
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

    @GetMapping("/module-progress")
    fun getModuleProgress(
        @RequestHeader("Authorization") authorization: String,
        @RequestParam languageCode: String,
        @RequestParam moduleNumber: Int
    ): ResponseEntity<LegacyModuleProgressDto> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info("Getting module progress for user $userId, language $languageCode, module $moduleNumber")

        val progress = exerciseService.getModuleProgress(userId, languageCode, moduleNumber)
        return ResponseEntity.ok(progress)
    }

    @GetMapping("/stats")
    fun getUserStats(
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<UserStatsDto> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info("Getting user stats for user $userId")

        val stats = exerciseService.getUserStats(userId)
        return ResponseEntity.ok(stats)
    }

    @PostMapping("/{id}/speaking", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun submitSpeakingAttempt(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable id: Long,
        @RequestParam("audio") audio: MultipartFile
    ): ResponseEntity<SpeakingAttemptResult> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info("User $userId submitting speaking attempt for exercise $id, audio size: ${audio.size} bytes")

        return try {
            val result = speakingExerciseService.validateSpeaking(id, audio, userId)
            ResponseEntity.ok(result)
        } catch (e: jakarta.persistence.EntityNotFoundException) {
            logger.warn("Exercise not found: $id")
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Error processing speaking attempt for exercise $id", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/speaking/validate", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun validateSpeakingDirect(
        @RequestHeader("Authorization") authorization: String,
        @RequestParam("audio") audio: MultipartFile,
        @RequestParam("expectedText") expectedText: String,
        @RequestParam("targetLanguage") targetLanguage: String,
        @RequestParam("acceptableVariations", required = false) acceptableVariations: List<String>?
    ): ResponseEntity<SpeakingAttemptResult> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info("User $userId validating direct speaking attempt, expected: '$expectedText'")

        return try {
            val result = speakingExerciseService.validateSpeakingDirect(
                audioFile = audio,
                expectedText = expectedText,
                targetLanguage = targetLanguage,
                acceptableVariations = acceptableVariations ?: emptyList()
            )
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            logger.error("Error processing direct speaking validation", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    private fun getUserIdFromToken(authorization: String): Long? {
        val token = authorization.removePrefix("Bearer ").trim()
        return jwtService.getUserIdFromToken(token)
    }
}
