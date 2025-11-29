package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * AI-generated vocabulary card with consistent quality and multi-language translations.
 * Alternative to Wiktionary-based dictionary entries.
 */
@Entity
@Table(
    name = "vocabulary_cards",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_vocabulary_cards_lemma",
            columnNames = ["language_code", "lemma", "part_of_speech"]
        )
    ],
    indexes = [
        Index(name = "idx_vocabulary_cards_language", columnList = "language_code"),
        Index(name = "idx_vocabulary_cards_lemma", columnList = "lemma"),
        Index(name = "idx_vocabulary_cards_cefr", columnList = "cefr_level"),
        Index(name = "idx_vocabulary_cards_word_set", columnList = "word_set_id"),
        Index(name = "idx_vocabulary_cards_status", columnList = "review_status")
    ]
)
data class VocabularyCard(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // Core word identification
    @Column(name = "language_code", nullable = false)
    val languageCode: String,

    @Column(nullable = false)
    val lemma: String,

    @Column(name = "part_of_speech")
    val partOfSpeech: String? = null,

    // Pronunciation
    val ipa: String? = null,

    @Column(name = "audio_url")
    val audioUrl: String? = null,

    // Grammar information
    val gender: String? = null,

    @Column(name = "plural_form")
    val pluralForm: String? = null,

    @Column(name = "verb_group")
    val verbGroup: String? = null,

    @Column(name = "grammar_notes", columnDefinition = "TEXT")
    val grammarNotes: String? = null,

    // Definitions (JSON array of strings)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val definitions: JsonNode,

    // Examples (JSON array of {sentence, translation})
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val examples: JsonNode? = null,

    // Multi-language translations {en: [...], es: [...], ru: [...]}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val translations: JsonNode,

    // Common inflections
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val inflections: JsonNode? = null,

    // Metadata
    @Column(name = "cefr_level")
    val cefrLevel: String? = null,

    @Column(name = "frequency_rank")
    val frequencyRank: Int? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val tags: JsonNode? = null,

    // Source tracking
    @Column(name = "word_set_id")
    val wordSetId: Long? = null,

    @Column(name = "source_word_id")
    val sourceWordId: Long? = null,

    // Generation metadata
    @Column(name = "model_used")
    val modelUsed: String? = null,

    @Column(name = "generated_at")
    val generatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "reviewed_at")
    val reviewedAt: LocalDateTime? = null,

    @Column(name = "review_status")
    @Enumerated(EnumType.STRING)
    val reviewStatus: ReviewStatus = ReviewStatus.PENDING,

    // Timestamps
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED
}
