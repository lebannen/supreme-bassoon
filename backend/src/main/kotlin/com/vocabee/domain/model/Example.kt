package com.vocabee.domain.model

import jakarta.persistence.*

@Entity
@Table(
    name = "examples",
    indexes = [
        Index(name = "idx_examples_definition", columnList = "definition_id")
    ]
)
data class Example(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false)
    val definition: Definition,

    @Column(name = "sentence_text", nullable = false, columnDefinition = "TEXT")
    val sentenceText: String,

    @Column(columnDefinition = "TEXT")
    val translation: String? = null,

    @Column(columnDefinition = "TEXT")
    val source: String? = null
)
