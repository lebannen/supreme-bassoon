package com.vocabee.web.dto.admin

import com.fasterxml.jackson.databind.JsonNode

/**
 * Complete course export structure for AI review and backup purposes
 */
data class CourseExportDto(
    val exportedAt: String,
    val exportVersion: String,
    val course: CourseExportInfo,
    val modules: List<ModuleExportDto>,
    val statistics: CourseStatistics
)

data class CourseExportInfo(
    val id: Long,
    val slug: String,
    val name: String,
    val languageCode: String,
    val cefrLevel: String,
    val description: String?,
    val seriesContext: String?,
    val estimatedHours: Int,
    val isPublished: Boolean
)

data class ModuleExportDto(
    val id: Long,
    val moduleNumber: Int,
    val title: String,
    val theme: String?,
    val description: String?,
    val objectives: List<String>?,
    val vocabularyFocus: List<String>?,
    val grammarFocus: List<String>?,
    val episodes: List<EpisodeExportDto>
)

data class EpisodeExportDto(
    val id: Long,
    val episodeNumber: Int,
    val type: String,
    val title: String,
    val summary: String?,
    val content: String,
    val data: JsonNode?,
    val audioUrl: String?,
    val exercises: List<ExerciseExportDto>
)

data class ExerciseExportDto(
    val id: Long,
    val type: String,
    val title: String,
    val instructions: String,
    val content: JsonNode
)

data class CourseStatistics(
    val totalModules: Int,
    val totalEpisodes: Int,
    val totalExercises: Int,
    val episodesWithAudio: Int
)
