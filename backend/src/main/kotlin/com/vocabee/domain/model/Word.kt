package com.vocabee.domain.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(
    name = "words",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["language_code", "lemma", "part_of_speech"])
    ],
    indexes = [
        Index(name = "idx_words_language", columnList = "language_code"),
        Index(name = "idx_words_normalized", columnList = "normalized"),
        Index(name = "idx_words_frequency", columnList = "frequency_rank"),
        Index(name = "idx_words_lemma_id", columnList = "lemma_id")
    ]
)
data class Word(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "language_code", length = 10, nullable = false)
    val languageCode: String,

    @Column(nullable = false)
    val lemma: String,

    @Column(nullable = false)
    val normalized: String,

    @Column(name = "part_of_speech", length = 50)
    val partOfSpeech: String? = null,

    @Column(columnDefinition = "TEXT")
    val etymology: String? = null,

    @Column(name = "usage_notes", columnDefinition = "TEXT")
    val usageNotes: String? = null,

    @Column(name = "frequency_rank")
    val frequencyRank: Int? = null,

    @Column(name = "is_inflected_form", nullable = false)
    val isInflectedForm: Boolean = false,

    @Column(name = "lemma_id")
    val lemmaId: Long? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grammatical_features", columnDefinition = "jsonb")
    val grammaticalFeatures: Map<String, Any>? = null,

    @OneToMany(mappedBy = "word", cascade = [CascadeType.ALL], orphanRemoval = true)
    val definitions: MutableList<Definition> = mutableListOf(),

    @OneToMany(mappedBy = "word", cascade = [CascadeType.ALL], orphanRemoval = true)
    val pronunciations: MutableList<Pronunciation> = mutableListOf(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
)
