package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_reading_progress",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "text_id"])
    ]
)
data class UserReadingProgress(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val textId: Long,

    @Column(nullable = false)
    val currentPage: Int = 1,

    @Column(nullable = false)
    val totalPages: Int = 1,

    @Column(nullable = false)
    val completed: Boolean = false,

    @Column(nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),

    @Column
    val completedAt: LocalDateTime? = null,

    @Column(nullable = false)
    val lastReadAt: LocalDateTime = LocalDateTime.now()
)
