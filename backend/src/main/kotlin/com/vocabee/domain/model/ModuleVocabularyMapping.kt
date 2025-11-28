package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Tracks which vocabulary phrases from episode generation were mapped to dictionary words.
 * Used for debugging and reporting on vocabulary coverage.
 */
@Entity
@Table(name = "module_vocabulary_mappings")
data class ModuleVocabularyMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "module_plan_id", nullable = false)
    val modulePlanId: UUID,

    @Column(name = "original_phrase", nullable = false, length = 500)
    val originalPhrase: String,

    @Column(name = "word_id")
    val wordId: Long? = null,

    @Column(nullable = false)
    val matched: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
