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

// Extension functions
fun Exercise.toSummaryDto() = ExerciseSummaryDto(
    id = id!!,
    type = exerciseType.typeKey,
    title = title,
    languageCode = languageCode,
    moduleNumber = moduleNumber,
    topic = topic,
    difficultyRating = difficultyRating,
    estimatedDurationSeconds = estimatedDurationSeconds,
    pointsValue = pointsValue
)

fun Exercise.toDto() = ExerciseDto(
    id = id!!,
    type = exerciseType.typeKey,
    languageCode = languageCode,
    moduleNumber = moduleNumber,
    topic = topic,
    cefrLevel = cefrLevel,
    title = title,
    instructions = instructions,
    content = content,
    estimatedDurationSeconds = estimatedDurationSeconds,
    pointsValue = pointsValue,
    difficultyRating = difficultyRating
)
