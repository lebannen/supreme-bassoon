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
import java.time.LocalDate
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

        val exercise = exerciseRepository.findById(exerciseId)
            .orElseThrow { EntityNotFoundException("Exercise not found: $exerciseId") }

        val validation = validationService.validate(
            exercise.exerciseType.typeKey,
            exercise.content,
            request.userResponses
        )

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

    @Transactional(readOnly = true)
    fun getUserStats(userId: Long): UserStatsDto {
        val summary = attemptRepository.getUserStatsSummary(userId)
        val totalExercisesAvailable = exerciseRepository.countPublishedAndValid()

        val bestScoresByExercise = attemptRepository.findBestScoresByExercise(userId)
            .associate { (it[0] as Long) to (it[1] as Double) }

        val totalExercisesCompleted = bestScoresByExercise.keys.size
        val totalExercisesMastered = bestScoresByExercise.values.count { it >= 80.0 }

        val activeDays = attemptRepository.findActiveDays(userId)
        val (currentStreak, longestStreak) = calculateStreaks(activeDays)

        val recentActivity = attemptRepository.findRecentActivity(userId).take(10)
            .map {
                RecentActivityDto(
                    it.exerciseId,
                    it.exerciseTitle,
                    it.exerciseType,
                    it.score ?: 0.0,
                    it.isCorrect ?: false,
                    it.completedAt
                )
            }

        val progressByLanguage = calculateProgressByLanguage(userId)

        return UserStatsDto(
            totalExercisesAvailable = totalExercisesAvailable.toInt(),
            totalExercisesCompleted = totalExercisesCompleted,
            totalExercisesMastered = totalExercisesMastered,
            overallAverageScore = summary.averageScore,
            totalTimeSpentSeconds = summary.totalTimeSpent.toInt(),
            totalAttempts = summary.totalAttempts.toInt(),
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            recentActivity = recentActivity,
            progressByLanguage = progressByLanguage
        )
    }

    private fun calculateStreaks(activeDays: List<LocalDate>): Pair<Int, Int> {
        if (activeDays.isEmpty()) return Pair(0, 0)

        var currentStreak = 0
        val today = LocalDate.now()
        if (activeDays.first() == today || activeDays.first() == today.minusDays(1)) {
            var lastDate = activeDays.first()
            for (day in activeDays) {
                if (day == lastDate || day == lastDate.minusDays(1)) {
                    currentStreak++
                    lastDate = day
                } else {
                    break
                }
            }
        }
        
        var longestStreak = 0
        var tempStreak = 0
        if (activeDays.isNotEmpty()) {
            tempStreak = 1
            longestStreak = 1
            for (i in 0 until activeDays.size - 1) {
                if (activeDays[i].minusDays(1) == activeDays[i + 1]) {
                    tempStreak++
                } else {
                    longestStreak = maxOf(longestStreak, tempStreak)
                    tempStreak = 1
                }
            }
            longestStreak = maxOf(longestStreak, tempStreak)
        }

        return Pair(currentStreak, longestStreak)
    }

    private fun calculateProgressByLanguage(userId: Long): Map<String, LanguageProgressDto> {
        val totalExercisesByLang = exerciseRepository.countPublishedAndValidByLanguage()
            .associate { (it[0] as String) to (it[1] as Long) }

        val userStatsByLang = attemptRepository.getStatsByLanguage(userId)
            .associate { (it["language"] as String) to (it["new"] as com.vocabee.domain.repository.LanguageStats) }

        return totalExercisesByLang.mapValues { (langCode, total) ->
            val userStats = userStatsByLang[langCode]
            LanguageProgressDto(
                languageCode = langCode,
                totalExercises = total.toInt(),
                completedExercises = userStats?.completed?.toInt() ?: 0,
                masteredExercises = userStats?.mastered?.toInt() ?: 0,
                averageScore = userStats?.averageScore,
                moduleProgress = emptyMap()
            )
        }
    }

    fun createExercise(exercise: Exercise): Exercise {
        return exerciseRepository.save(exercise)
    }
}
