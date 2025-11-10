package com.vocabee.web.dto

import java.time.LocalDateTime

data class ReadingTextDto(
    val id: Long,
    val title: String,
    val content: String,
    val languageCode: String,
    val level: String?,
    val topic: String?,
    val wordCount: Int?,
    val description: String?,
    val estimatedMinutes: Int?,
    val difficulty: String?,
    val isPublished: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val author: String?,
    val source: String?,
    val audioUrl: String?
)

data class UserReadingProgressDto(
    val id: Long?,
    val userId: Long,
    val textId: Long,
    val currentPage: Int,
    val totalPages: Int,
    val completed: Boolean,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val lastReadAt: LocalDateTime
)

data class UpdateProgressRequest(
    val currentPage: Int,
    val totalPages: Int
)

data class UpdateAudioUrlRequest(
    val audioUrl: String
)

data class ImportTextRequest(
    val title: String,
    val content: String,
    val languageCode: String,
    val level: String?,
    val topic: String?,
    val description: String?,
    val author: String?,
    val source: String?,
    val audioUrl: String?
)
