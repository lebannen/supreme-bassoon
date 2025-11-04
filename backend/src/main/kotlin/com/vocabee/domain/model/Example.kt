package com.vocabee.domain.model

import jakarta.persistence.*

@Entity
@Table(
    name = "examples",
    indexes = [
        Index(name = "idx_examples_definition", columnList = "definition_id"),
        Index(name = "idx_examples_word", columnList = "word_id")
    ]
)
data class Example(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_id")
    val definition: Definition? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id")
    val word: Word? = null,

    @Column(name = "sentence_text", nullable = false, columnDefinition = "TEXT")
    val sentenceText: String,

    @Column(columnDefinition = "TEXT")
    val translation: String? = null,

    @Column(columnDefinition = "TEXT")
    val source: String? = null
)
