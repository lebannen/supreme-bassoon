package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "user_vocabulary")
data class UserVocabulary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "word_id", nullable = false)
    val word: Word,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "added_at", nullable = false, updatable = false)
    val addedAt: Instant = Instant.now(),

    // Spaced Repetition System (SRS) fields
    @Column(name = "next_review_at")
    var nextReviewAt: Instant? = null,

    @Column(name = "review_count", nullable = false)
    var reviewCount: Int = 0,

    @Column(name = "consecutive_successes", nullable = false)
    var consecutiveSuccesses: Int = 0,

    @Column(name = "current_interval_hours", nullable = false)
    var currentIntervalHours: Int = 20,

    @Column(name = "last_reviewed_at")
    var lastReviewedAt: Instant? = null,

    @Column(name = "ease_factor", nullable = false)
    var easeFactor: Double = 1.0
)
