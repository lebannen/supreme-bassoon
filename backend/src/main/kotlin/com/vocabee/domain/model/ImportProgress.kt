package com.vocabee.domain.model

import java.time.LocalDateTime

data class ImportProgress(
    val importId: String,
    val languageCode: String,
    val languageName: String,
    val status: ImportStatus,
    val totalEntries: Long = 0,
    val processedEntries: Long = 0,
    val successfulEntries: Long = 0,
    val failedEntries: Long = 0,
    val currentBatch: Int = 0,
    val message: String = "",
    val startedAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null,
    val error: String? = null
) {
    val progressPercentage: Int
        get() = if (totalEntries > 0) {
            ((processedEntries.toDouble() / totalEntries) * 100).toInt()
        } else 0
}

enum class ImportStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}
