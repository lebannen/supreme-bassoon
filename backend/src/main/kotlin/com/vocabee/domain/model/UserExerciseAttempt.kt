package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "user_exercise_attempts")
data class UserExerciseAttempt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val exerciseId: Long,

    @Column(nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),

    var completedAt: LocalDateTime? = null,
    var durationSeconds: Int? = null,

    var score: Double? = null,
    val maxScore: Double = 100.0,
    var isCorrect: Boolean? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var userResponses: JsonNode? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var correctAnswers: JsonNode? = null,

    var hintsUsed: Int = 0,
    val attemptsCount: Int = 1
)
