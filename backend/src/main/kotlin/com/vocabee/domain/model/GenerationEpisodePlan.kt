package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*

/**
 * Episode outline within a module plan.
 * Contains vocabulary and grammar assignments for the episode.
 */
@Entity
@Table(
    name = "generation_episode_plans",
    uniqueConstraints = [UniqueConstraint(columnNames = ["module_plan_id", "episode_number"])]
)
data class GenerationEpisodePlan(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val modulePlanId: UUID,

    @Column(nullable = false)
    val episodeNumber: Int,

    /** Episode title */
    @Column(length = 200)
    var title: String? = null,

    /** Scene/situation description */
    @Column(columnDefinition = "TEXT")
    var sceneDescription: String? = null,

    /** Episode type: DIALOGUE, STORY, ARTICLE */
    @Column(length = 50)
    var episodeType: String = "DIALOGUE",

    /**
     * Target vocabulary for this episode.
     * Format: ["bonjour", "café", "merci", "s'il vous plaît"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var vocabulary: JsonNode? = null,

    /**
     * Assigned grammar rules for this episode.
     * Format: ["present-er-verbs", "definite-articles"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var grammarRules: JsonNode? = null,

    /**
     * UUIDs of characters appearing in this episode.
     * Format: ["uuid1", "uuid2"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var characterIds: JsonNode? = null,

    /** Plot points to cover in this episode */
    @Column(columnDefinition = "TEXT")
    var plotPoints: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: GenerationStepStatus = GenerationStepStatus.PENDING,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
