package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "study_session_attempts")
data class StudySessionAttempt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_item_id", nullable = false)
    val sessionItem: StudySessionItem,

    @Column(name = "attempted_at", nullable = false)
    val attemptedAt: Instant = Instant.now(),

    @Column(name = "was_correct", nullable = false)
    val wasCorrect: Boolean,

    @Column(name = "response_time_ms")
    val responseTimeMs: Int? = null
)
