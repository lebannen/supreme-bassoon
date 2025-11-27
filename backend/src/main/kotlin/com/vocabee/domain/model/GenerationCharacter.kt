package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*

/**
 * Character registry that evolves during episode generation.
 * Characters are seeded in the blueprint stage and enriched as episodes are generated.
 */
@Entity
@Table(name = "generation_characters")
data class GenerationCharacter(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val generationId: UUID,

    /** Character name */
    @Column(nullable = false, length = 100)
    val name: String,

    /** Role in the story: PROTAGONIST, SUPPORTING, MINOR, RECURRING */
    @Column(length = 50)
    var role: String? = null,

    /** Initial description from blueprint */
    @Column(columnDefinition = "TEXT")
    var initialDescription: String? = null,

    /** Age range (e.g., "mid-20s", "elderly") */
    @Column(length = 50)
    var ageRange: String? = null,

    /** Gender: MALE or FEMALE */
    @Column(length = 20)
    var gender: String? = null,

    /**
     * Personality traits.
     * Format: ["friendly", "curious", "shy"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var personalityTraits: JsonNode? = null,

    /** Character background/history */
    @Column(columnDefinition = "TEXT")
    var background: String? = null,

    /** Detailed physical appearance (built up during generation, used for image gen) */
    @Column(columnDefinition = "TEXT")
    var appearanceDescription: String? = null,

    /** Assigned voice ID for TTS */
    @Column(length = 50)
    var voiceId: String? = null,

    /** URL to generated reference image */
    @Column(columnDefinition = "TEXT")
    var referenceImageUrl: String? = null,

    /**
     * Character developments across episodes.
     * Format: [{"episodeId": "uuid", "note": "Revealed she's from Lyon"}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var developments: JsonNode? = null,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Character roles in the story
 */
enum class CharacterRole {
    PROTAGONIST,
    SUPPORTING,
    MINOR,
    RECURRING
}
