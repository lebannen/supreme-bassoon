package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*

/**
 * Stage 5 output: Generated exercises for an episode.
 */
@Entity
@Table(name = "generation_exercises")
data class GenerationExercises(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val episodePlanId: UUID,

    /**
     * Array of generated exercise objects.
     * Each exercise follows the standard exercise schema.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var exercises: JsonNode? = null,

    /** Number of exercises generated */
    var exerciseCount: Int? = null,

    /**
     * Which vocabulary words are covered by the exercises.
     * Format: ["bonjour", "café", "merci"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var vocabularyCoverage: JsonNode? = null,

    /**
     * Which grammar rules are tested by the exercises.
     * Format: ["present-er-verbs"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var grammarCoverage: JsonNode? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: GenerationStepStatus = GenerationStepStatus.PENDING,

    /** Raw AI response for debugging */
    @Column(columnDefinition = "TEXT")
    var rawResponse: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
