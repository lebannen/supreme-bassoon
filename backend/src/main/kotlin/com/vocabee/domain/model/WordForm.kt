package com.vocabee.domain.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "word_forms",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["word_id", "form", "form_type"])
    ],
    indexes = [
        Index(name = "idx_word_forms_word", columnList = "word_id"),
        Index(name = "idx_word_forms_form", columnList = "form")
    ]
)
data class WordForm(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    val word: Word,

    @Column(nullable = false)
    val form: String,

    @Column(name = "form_type", length = 50)
    val formType: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val metadata: Map<String, Any>? = null
)
