package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*

/**
 * Stage 3 output: Generated dialogue/story content for an episode.
 */
@Entity
@Table(name = "generation_episode_content")
data class GenerationEpisodeContent(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val episodePlanId: UUID,

    /** Content type: DIALOGUE, STORY */
    @Column(length = 50)
    var contentType: String? = null,

    /** Full text of the episode */
    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    /**
     * Structured content for dialogues.
     * Format: [{"speaker": "Marie", "text": "Bonjour!"}, ...]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var contentStructured: JsonNode? = null,

    /** Brief summary for next episode's context */
    @Column(columnDefinition = "TEXT")
    var summary: String? = null,

    /**
     * Character developments revealed in this episode.
     * Format: [{"characterId": "uuid", "note": "Revealed she's from Lyon"}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var characterDevelopments: JsonNode? = null,

    /**
     * Target vocabulary words that were actually used.
     * Format: ["bonjour", "café"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var vocabularyUsed: JsonNode? = null,

    /**
     * Target vocabulary words that couldn't be incorporated naturally.
     * Format: ["word1", "word2"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var vocabularyMissing: JsonNode? = null,

    /**
     * AI-generated image prompts for scene illustrations.
     * Format: [{"description": "...", "sceneContext": "..."}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var imagePrompts: JsonNode? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: GenerationStepStatus = GenerationStepStatus.PENDING,

    /** Raw AI response for debugging */
    @Column(columnDefinition = "TEXT")
    var rawResponse: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
