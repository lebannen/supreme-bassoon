package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*

/**
 * Stage 6 output: Generated media (audio, images) for the course.
 */
@Entity
@Table(name = "generation_media")
data class GenerationMedia(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val generationId: UUID,

    /** Episode this media belongs to (for episode audio/scene images) */
    val episodePlanId: UUID? = null,

    /** Character this media belongs to (for character reference images) */
    val characterId: UUID? = null,

    /** Type of media: EPISODE_AUDIO, CHARACTER_IMAGE, SCENE_IMAGE */
    @Column(nullable = false, length = 50)
    val mediaType: String,

    /** URL to the generated media file */
    @Column(columnDefinition = "TEXT")
    var url: String? = null,

    /**
     * Additional metadata.
     * For audio: {"duration": 120, "speakers": ["Marie", "Alex"]}
     * For images: {"width": 1024, "height": 1024, "prompt": "..."}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var metadata: JsonNode? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: GenerationStepStatus = GenerationStepStatus.PENDING,

    /** Error message if generation failed */
    @Column(columnDefinition = "TEXT")
    var errorMessage: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Types of media that can be generated
 */
enum class GenerationMediaType {
    EPISODE_AUDIO,
    CHARACTER_IMAGE,
    SCENE_IMAGE
}
