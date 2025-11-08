package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "user_imported_word_sets")
data class UserImportedWordSet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_set_id", nullable = false)
    val wordSet: WordSet,

    @Column(name = "imported_at", nullable = false, updatable = false)
    val importedAt: Instant = Instant.now()
)
