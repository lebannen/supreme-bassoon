package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "exercises")
data class Exercise(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_type_id", nullable = false)
    val exerciseType: ExerciseType,

    @Column(nullable = false, length = 10)
    val languageCode: String,

    val moduleNumber: Int? = null,

    @Column(length = 100)
    val topic: String? = null,

    @Column(length = 10)
    val cefrLevel: String = "A1",

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val instructions: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSONB")
    val content: JsonNode,

    val estimatedDurationSeconds: Int = 60,
    val pointsValue: Int = 10,
    val difficultyRating: Double = 1.0,

    val isPublished: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: String = "system"
)
