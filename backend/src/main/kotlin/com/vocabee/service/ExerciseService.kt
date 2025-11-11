package com.vocabee.service

import com.vocabee.domain.model.Exercise
import com.vocabee.domain.model.UserExerciseAttempt
import com.vocabee.domain.repository.ExerciseRepository
import com.vocabee.domain.repository.UserExerciseAttemptRepository
import com.vocabee.web.dto.*
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ExerciseService(
    private val exerciseRepository: ExerciseRepository,
    private val attemptRepository: UserExerciseAttemptRepository,
    private val validationService: ExerciseValidationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getExercises(
        languageCode: String,
        moduleNumber: Int? = null,
        topic: String? = null,
        typeKey: String? = null
    ): List<ExerciseSummaryDto> {
        return exerciseRepository.findByFilters(languageCode, moduleNumber, topic, typeKey)
            .map { it.toSummaryDto() }
    }

    fun getExerciseById(id: Long): ExerciseDto {
        val exercise = exerciseRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Exercise not found: $id") }
        return exercise.toDto()
    }

    @Transactional
    fun submitAttempt(
        userId: Long,
        exerciseId: Long,
        request: SubmitAttemptRequest
    ): AttemptResultDto {
        logger.info("Submitting attempt for user $userId, exercise $exerciseId")

        // Get exercise
        val exercise = exerciseRepository.findById(exerciseId)
            .orElseThrow { EntityNotFoundException("Exercise not found: $exerciseId") }

        // Validate response
        val validation = validationService.validate(
            exercise.exerciseType.typeKey,
            exercise.content,
            request.userResponses
        )

        // Create attempt record
        val attempt = UserExerciseAttempt(
            userId = userId,
            exerciseId = exerciseId,
            completedAt = LocalDateTime.now(),
            durationSeconds = request.durationSeconds,
            score = validation.score,
            isCorrect = validation.isCorrect,
            userResponses = request.userResponses,
            correctAnswers = validation.correctAnswers,
            hintsUsed = request.hintsUsed
        )

        val savedAttempt = attemptRepository.save(attempt)

        return AttemptResultDto(
            attemptId = savedAttempt.id!!,
            isCorrect = validation.isCorrect,
            score = validation.score,
            feedback = validation.feedback,
            correctAnswers = validation.correctAnswers,
            completedAt = savedAttempt.completedAt!!
        )
    }

    fun getUserProgress(userId: Long, exerciseId: Long): UserProgressDto {
        val attempts = attemptRepository.findByUserIdAndExerciseId(userId, exerciseId)

        return UserProgressDto(
            exerciseId = exerciseId,
            attemptsCount = attempts.size,
            bestScore = attempts.mapNotNull { it.score }.maxOrNull(),
            lastAttemptAt = attempts.maxOfOrNull { it.startedAt },
            isCompleted = attempts.any { it.isCorrect == true }
        )
    }

    fun createExercise(exercise: Exercise): Exercise {
        return exerciseRepository.save(exercise)
    }
}
