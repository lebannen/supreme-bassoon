package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "reading_texts")
data class ReadingText(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false, length = 10)
    val languageCode: String,

    @Column(length = 10)
    val level: String? = null,

    @Column(length = 100)
    val topic: String? = null,

    @Column
    val wordCount: Int? = null,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column
    val estimatedMinutes: Int? = null,

    @Column(length = 50)
    val difficulty: String? = null,

    @Column(nullable = false)
    val isPublished: Boolean = false,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(length = 100)
    val author: String? = null,

    @Column(length = 100)
    val source: String? = null
)
