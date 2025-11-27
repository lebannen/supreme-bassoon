package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "grammar_rules")
data class GrammarRule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 10)
    val languageCode: String,

    @Column(length = 10)
    val cefrLevel: String = "A1",

    @Column(length = 100)
    val slug: String? = null,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    val examples: JsonNode? = null,

    @Column(columnDefinition = "TEXT")
    val usageNotes: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    val commonErrors: JsonNode? = null,

    @Column(length = 100)
    val category: String? = null,

    val difficultyLevel: Int = 1,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
