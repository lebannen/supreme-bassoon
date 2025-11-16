package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_episode_progress",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "episode_id"])]
)
data class UserEpisodeProgress(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val episodeId: Long,

    val hasReadContent: Boolean = false,
    val hasListenedAudio: Boolean = false,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    val completedContentItems: JsonNode? = null,

    val totalContentItems: Int = 0,
    val requiredContentItemsCompleted: Int = 0,

    val timeSpentSeconds: Int = 0,
    val averageScore: Double? = null,

    val isCompleted: Boolean = false,

    val startedAt: LocalDateTime = LocalDateTime.now(),
    val lastActivityAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null
)
