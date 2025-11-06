package com.vocabee.domain.model

import jakarta.persistence.*

@Entity
@Table(
    name = "pronunciations",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["word_id", "ipa", "dialect"])
    ],
    indexes = [Index(name = "idx_pronunciations_word", columnList = "word_id")]
)
data class Pronunciation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    val word: Word,

    @Column(columnDefinition = "TEXT")
    val ipa: String? = null,

    @Column(name = "audio_url", columnDefinition = "TEXT")
    val audioUrl: String? = null,

    @Column(length = 200)
    val dialect: String? = null
)
