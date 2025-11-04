package com.vocabee.domain.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "definitions",
    indexes = [Index(name = "idx_definitions_word", columnList = "word_id")]
)
data class Definition(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    val word: Word,

    @Column(name = "definition_number", nullable = false)
    val definitionNumber: Int,

    @Column(name = "definition_text", nullable = false, columnDefinition = "TEXT")
    val definitionText: String,

    @Column(columnDefinition = "TEXT")
    val etymology: String? = null,

    @Column(name = "usage_notes", columnDefinition = "TEXT")
    val usageNotes: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val metadata: Map<String, Any>? = null,

    @OneToMany(mappedBy = "definition", cascade = [CascadeType.ALL], orphanRemoval = true)
    val examples: MutableList<Example> = mutableListOf()
)
