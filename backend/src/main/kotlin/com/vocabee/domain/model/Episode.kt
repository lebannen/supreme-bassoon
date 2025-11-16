package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class EpisodeType {
    DIALOGUE,      // Conversation between 2+ people
    STORY,         // Narrative text
    ARTICLE,       // Educational article
    AUDIO_LESSON   // Primarily audio with transcript
}

@Entity
@Table(
    name = "episodes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["module_id", "episode_number"])]
)
data class Episode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val moduleId: Long,

    @Column(nullable = false)
    val episodeNumber: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val episodeType: EpisodeType,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(columnDefinition = "TEXT")
    val audioUrl: String? = null,

    @Column(columnDefinition = "TEXT")
    val transcript: String? = null,

    val estimatedMinutes: Int = 15,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
