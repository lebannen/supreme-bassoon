package com.vocabee.domain.model

import java.time.LocalDateTime

data class CourseImportProgress(
    val importId: String,
    val courseSlug: String,
    val courseName: String,
    val status: ImportStatus,
    val totalModules: Int = 0,
    val processedModules: Int = 0,
    val totalEpisodes: Int = 0,
    val processedEpisodes: Int = 0,
    val totalExercises: Int = 0,
    val processedExercises: Int = 0,
    val audioFilesGenerated: Int = 0,
    val currentModule: String? = null,
    val currentEpisode: String? = null,
    val message: String = "",
    val errors: List<String> = emptyList(),
    val startedAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null,
    val error: String? = null
) {
    val progressPercentage: Int
        get() {
            val totalItems = totalModules + totalEpisodes + totalExercises
            if (totalItems == 0) return 0
            val processedItems = processedModules + processedEpisodes + processedExercises
            return ((processedItems.toDouble() / totalItems) * 100).toInt()
        }
}
