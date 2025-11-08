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
    val addedAt: Instant = Instant.now()
)
