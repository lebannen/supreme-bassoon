package com.vocabee.web.dto

import com.fasterxml.jackson.databind.JsonNode
import com.vocabee.domain.model.Exercise
import java.time.LocalDateTime

// Exercise DTOs
data class ExerciseSummaryDto(
    val id: Long,
    val type: String,
    val title: String,
    val languageCode: String,
    val moduleNumber: Int?,
    val topic: String?,
    val difficultyRating: Double,
    val estimatedDurationSeconds: Int,
    val pointsValue: Int
)

data class ExerciseDto(
    val id: Long,
    val type: String,
    val languageCode: String,
    val moduleNumber: Int?,
    val topic: String?,
    val cefrLevel: String,
    val title: String,
    val instructions: String,
    val content: JsonNode,
    val estimatedDurationSeconds: Int,
    val pointsValue: Int,
    val difficultyRating: Double
)

data class SubmitAttemptRequest(
    val userResponses: JsonNode,
    val durationSeconds: Int?,
    val hintsUsed: Int = 0
)

data class ValidationResult(
    val isCorrect: Boolean,
    val score: Double,
    val feedback: String,
    val correctAnswers: JsonNode? = null
)

data class AttemptResultDto(
    val attemptId: Long,
    val isCorrect: Boolean,
    val score: Double,
    val feedback: String,
    val correctAnswers: JsonNode? = null,
    val completedAt: LocalDateTime
)

data class UserProgressDto(
    val exerciseId: Long,
    val attemptsCount: Int,
    val bestScore: Double?,
    val lastAttemptAt: LocalDateTime?,
    val isCompleted: Boolean
)

// Renamed to avoid conflict with new course-based ModuleProgressDto
data class LegacyModuleProgressDto(
    val moduleNumber: Int,
    val languageCode: String,
    val totalExercises: Int,
    val completedExercises: Int,
    val masteredExercises: Int,
    val averageScore: Double?,
    val totalTimeSpentSeconds: Int,
    val completionPercentage: Double,
    val exercises: List<ExerciseProgressDto>
)

data class ExerciseProgressDto(
    val exercise: ExerciseSummaryDto,
    val bestScore: Double?,
    val attemptsCount: Int,
    val lastAttemptAt: LocalDateTime?,
    val status: ExerciseStatus
)

enum class ExerciseStatus {
    NOT_STARTED,
    IN_PROGRESS,
    MASTERED
}

data class UserStatsDto(
    val totalExercisesAvailable: Int,
    val totalExercisesCompleted: Int,
    val totalExercisesMastered: Int,
    val overallAverageScore: Double?,
    val totalTimeSpentSeconds: Int,
    val totalAttempts: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val recentActivity: List<RecentActivityDto>,
    val progressByLanguage: Map<String, LanguageProgressDto>
)

data class RecentActivityDto(
    val exerciseId: Long,
    val exerciseTitle: String,
    val exerciseType: String,
    val score: Double,
    val isCorrect: Boolean,
    val completedAt: LocalDateTime
)

data class LanguageProgressDto(
    val languageCode: String,
    val totalExercises: Int,
    val completedExercises: Int,
    val masteredExercises: Int,
    val averageScore: Double?,
    val moduleProgress: Map<Int, ModuleProgressSummary>
)

data class ModuleProgressSummary(
    val moduleNumber: Int,
    val totalExercises: Int,
    val masteredExercises: Int,
    val completionPercentage: Double
)

// Extension functions
fun Exercise.toSummaryDto() = ExerciseSummaryDto(
    id = id!!,
    type = exerciseType.typeKey,
    title = title,
    languageCode = languageCode,
    moduleNumber = null, // No longer stored directly on Exercise
    topic = null, // No longer stored directly on Exercise
    difficultyRating = difficultyRating,
    estimatedDurationSeconds = estimatedDurationSeconds,
    pointsValue = pointsValue
)

fun Exercise.toDto() = ExerciseDto(
    id = id!!,
    type = exerciseType.typeKey,
    languageCode = languageCode,
    moduleNumber = null, // No longer stored directly on Exercise
    topic = null, // No longer stored directly on Exercise
    cefrLevel = cefrLevel,
    title = title,
    instructions = instructions,
    content = content,
    estimatedDurationSeconds = estimatedDurationSeconds,
    pointsValue = pointsValue,
    difficultyRating = difficultyRating
)
