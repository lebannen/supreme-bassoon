package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_module_progress",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "module_id"])]
)
data class UserModuleProgress(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val moduleId: Long,

    val episodesCompleted: Int = 0,
    val totalEpisodes: Int = 0,
    val completionPercentage: Double = 0.0,

    val timeSpentSeconds: Int = 0,
    val averageScore: Double? = null,

    val startedAt: LocalDateTime = LocalDateTime.now(),
    val lastActivityAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null
)
