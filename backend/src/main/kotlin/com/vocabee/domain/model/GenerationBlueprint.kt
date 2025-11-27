package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*

/**
 * Stage 1 output: Course blueprint containing structure, plot, and grammar distribution.
 */
@Entity
@Table(name = "generation_blueprints")
data class GenerationBlueprint(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val generationId: UUID,

    /** Generated course title */
    @Column(length = 200)
    var courseTitle: String? = null,

    /** Generated course description */
    @Column(columnDefinition = "TEXT")
    var courseDescription: String? = null,

    /** Story setting (e.g., "Modern Paris, a cozy café") */
    @Column(columnDefinition = "TEXT")
    var setting: String? = null,

    /** Story premise (e.g., "An American student arrives in Paris...") */
    @Column(columnDefinition = "TEXT")
    var premise: String? = null,

    /**
     * Plot arc across modules.
     * Format: [{"module": 1, "arcPoint": "Introduction & Setup"}, ...]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var plotArc: JsonNode? = null,

    /**
     * Topics for each module.
     * Format: [{"module": 1, "topic": "Greetings & Introductions"}, ...]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var moduleTopics: JsonNode? = null,

    /**
     * Grammar rules distribution across modules.
     * Format: [{"module": 1, "rules": ["present-er-verbs", "definite-articles"]}, ...]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var grammarDistribution: JsonNode? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: GenerationStepStatus = GenerationStepStatus.PENDING,

    /** Raw AI response for debugging */
    @Column(columnDefinition = "TEXT")
    var rawResponse: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
