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
        typeKey: String? = null
    ): List<ExerciseSummaryDto> {
        return exerciseRepository.findByFilters(languageCode, typeKey)
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

    fun getModuleProgress(userId: Long, languageCode: String, moduleNumber: Int): LegacyModuleProgressDto {
        // DEPRECATED: Module numbers no longer exist on exercises
        // Use the new Course/Module/Episode structure for progress tracking
        logger.warn("Legacy getModuleProgress called - this method is deprecated")

        return LegacyModuleProgressDto(
            moduleNumber = moduleNumber,
            languageCode = languageCode,
            totalExercises = 0,
            completedExercises = 0,
            masteredExercises = 0,
            averageScore = null,
            totalTimeSpentSeconds = 0,
            completionPercentage = 0.0,
            exercises = emptyList()
        )
    }

    fun getUserStats(userId: Long): UserStatsDto {
        // Get all valid exercises (published and with audio if listening type)
        val allExercises = exerciseRepository.findAllPublishedAndValid()

        // Get all user attempts
        val allAttempts = attemptRepository.findByUserIdOrderByStartedAtDesc(userId)

        // Calculate best scores per exercise
        val bestAttemptsByExercise = allAttempts
            .filter { it.completedAt != null }
            .groupBy { it.exerciseId }
            .mapValues { (_, attempts) -> attempts.maxByOrNull { it.score ?: 0.0 } }

        // Overall stats
        val totalExercisesAvailable = allExercises.size
        val totalExercisesCompleted = bestAttemptsByExercise.keys.size
        val totalExercisesMastered = bestAttemptsByExercise.values.count { (it?.score ?: 0.0) >= 80.0 }
        val overallAverageScore = allAttempts
            .mapNotNull { it.score }
            .takeIf { it.isNotEmpty() }
            ?.average()
        val totalTimeSpent = allAttempts.sumOf { it.durationSeconds ?: 0 }
        val totalAttempts = allAttempts.size

        // Calculate streaks
        val (currentStreak, longestStreak) = calculateStreaks(allAttempts)

        // Recent activity (last 10)
        val recentActivity = allAttempts
            .filter { it.completedAt != null }
            .take(10)
            .map { attempt ->
                val exercise = exerciseRepository.findById(attempt.exerciseId).orElse(null)
                RecentActivityDto(
                    exerciseId = attempt.exerciseId,
                    exerciseTitle = exercise?.title ?: "Unknown",
                    exerciseType = exercise?.exerciseType?.typeKey ?: "unknown",
                    score = attempt.score ?: 0.0,
                    isCorrect = attempt.isCorrect ?: false,
                    completedAt = attempt.completedAt!!
                )
            }

        // Progress by language
        val progressByLanguage = calculateProgressByLanguage(allExercises, bestAttemptsByExercise, allAttempts)

        return UserStatsDto(
            totalExercisesAvailable = totalExercisesAvailable,
            totalExercisesCompleted = totalExercisesCompleted,
            totalExercisesMastered = totalExercisesMastered,
            overallAverageScore = overallAverageScore,
            totalTimeSpentSeconds = totalTimeSpent,
            totalAttempts = totalAttempts,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            recentActivity = recentActivity,
            progressByLanguage = progressByLanguage
        )
    }

    private fun calculateStreaks(attempts: List<UserExerciseAttempt>): Pair<Int, Int> {
        if (attempts.isEmpty()) return Pair(0, 0)

        // Get unique days when user completed exercises
        val activeDays = attempts
            .mapNotNull { it.completedAt }
            .map { it.toLocalDate() }
            .distinct()
            .sortedDescending()

        if (activeDays.isEmpty()) return Pair(0, 0)

        // Calculate current streak
        var currentStreak = 0
        var currentDate = java.time.LocalDate.now()

        for (day in activeDays) {
            if (day.isEqual(currentDate) || day.isEqual(currentDate.minusDays(1))) {
                currentStreak++
                currentDate = day.minusDays(1)
            } else {
                break
            }
        }

        // Calculate longest streak
        var longestStreak = 0
        var tempStreak = 1

        for (i in 0 until activeDays.size - 1) {
            if (activeDays[i].minusDays(1).isEqual(activeDays[i + 1])) {
                tempStreak++
            } else {
                longestStreak = maxOf(longestStreak, tempStreak)
                tempStreak = 1
            }
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        return Pair(currentStreak, longestStreak)
    }

    private fun calculateProgressByLanguage(
        allExercises: List<Exercise>,
        bestAttempts: Map<Long, UserExerciseAttempt?>,
        allAttempts: List<UserExerciseAttempt>
    ): Map<String, LanguageProgressDto> {
        return allExercises
            .groupBy { it.languageCode }
            .mapValues { (languageCode, exercises) ->
                val exerciseIds = exercises.map { it.id!! }
                val languageAttempts = bestAttempts.filterKeys { it in exerciseIds }
                val allLanguageAttempts = allAttempts.filter { it.exerciseId in exerciseIds }

                val totalExercises = exercises.size
                val completedExercises = languageAttempts.keys.size
                val masteredExercises = languageAttempts.values.count { (it?.score ?: 0.0) >= 80.0 }
                val averageScore = allLanguageAttempts
                    .mapNotNull { it.score }
                    .takeIf { it.isNotEmpty() }
                    ?.average()

                // Module progress is no longer tracked at exercise level
                // Use the new Course/Module/Episode structure for module progress
                val moduleProgress = emptyMap<Int, ModuleProgressSummary>()

                LanguageProgressDto(
                    languageCode = languageCode,
                    totalExercises = totalExercises,
                    completedExercises = completedExercises,
                    masteredExercises = masteredExercises,
                    averageScore = averageScore,
                    moduleProgress = moduleProgress
                )
            }
    }

    fun createExercise(exercise: Exercise): Exercise {
        return exerciseRepository.save(exercise)
    }
}
