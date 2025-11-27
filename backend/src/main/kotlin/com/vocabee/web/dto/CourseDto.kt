package com.vocabee.web.dto

import com.fasterxml.jackson.databind.JsonNode
import com.vocabee.domain.model.Course
import com.vocabee.domain.model.Episode
import com.vocabee.domain.model.GrammarRule
import com.vocabee.domain.model.Module

// ============================================================================
// COURSE DTOs
// ============================================================================

data class CourseSummaryDto(
    val id: Long,
    val slug: String,
    val name: String,
    val languageCode: String,
    val cefrLevel: String,
    val description: String?,
    val estimatedHours: Int,
    val totalModules: Int = 0,
    val totalEpisodes: Int = 0
)

data class CourseDto(
    val id: Long,
    val slug: String,
    val name: String,
    val languageCode: String,
    val cefrLevel: String,
    val description: String?,
    val objectives: List<String> = emptyList(),
    val estimatedHours: Int,
    val modules: List<ModuleSummaryDto> = emptyList()
)

data class CourseAdminDto(
    val id: Long,
    val slug: String,
    val name: String,
    val languageCode: String,
    val cefrLevel: String,
    val estimatedHours: Int,
    val isPublished: Boolean,
    val seriesContext: String? = null
)

// ============================================================================
// MODULE DTOs
// ============================================================================

data class ModuleSummaryDto(
    val id: Long,
    val courseId: Long,
    val moduleNumber: Int,
    val title: String,
    val theme: String?,
    val estimatedMinutes: Int,
    val totalEpisodes: Int = 0
)

data class ModuleDto(
    val id: Long,
    val courseId: Long,
    val moduleNumber: Int,
    val title: String,
    val theme: String?,
    val description: String?,
    val objectives: List<String> = emptyList(),
    val estimatedMinutes: Int,
    val episodes: List<EpisodeSummaryDto> = emptyList()
)

data class ModuleAdminDto(
    val id: Long,
    val moduleNumber: Int,
    val title: String,
    val episodeCount: Int
)

data class ModuleDetailDto(
    val id: Long,
    val moduleNumber: Int,
    val title: String,
    val theme: String?,
    val description: String?,
    val objectives: List<String>?,
    val vocabularyFocus: List<String>?,
    val grammarFocus: List<String>?,
    val episodeOutline: JsonNode?,
    val episodeCount: Int
)

// ============================================================================
// EPISODE DTOs
// ============================================================================

data class EpisodeSummaryDto(
    val id: Long,
    val moduleId: Long,
    val episodeNumber: Int,
    val type: String,
    val title: String,
    val estimatedMinutes: Int,
    val hasAudio: Boolean,
    val totalExercises: Int = 0
)

data class EpisodeDto(
    val id: Long,
    val moduleId: Long,
    val episodeNumber: Int,
    val type: String,
    val title: String,
    val content: String,
    val data: String?,  // JSON string containing structured content (dialogue, story, etc.)
    val audioUrl: String?,
    val transcript: String?,
    val estimatedMinutes: Int,
    val contentItems: List<EpisodeContentItemDto> = emptyList()
)

data class EpisodeContentItemDto(
    val id: Long,
    val orderIndex: Int,
    val contentType: String,
    val isRequired: Boolean,
    val exercise: ExerciseDto? = null,
    val grammarRule: GrammarRuleDto? = null
)

// ============================================================================
// GRAMMAR RULE DTOs
// ============================================================================

data class GrammarRuleDto(
    val id: Long,
    val name: String,
    val description: String,
    val examples: List<String> = emptyList(),
    val category: String?
)

// ============================================================================
// PROGRESS DTOs
// ============================================================================

data class CourseProgressDto(
    val courseId: Long,
    val userId: Long,
    val completionPercentage: Double,
    val currentModuleId: Long?,
    val currentEpisodeId: Long?,
    val totalTimeSpentSeconds: Int,
    val enrolledAt: String,
    val lastActivityAt: String
)

data class ModuleProgressDto(
    val moduleId: Long,
    val userId: Long,
    val episodesCompleted: Int,
    val totalEpisodes: Int,
    val completionPercentage: Double,
    val timeSpentSeconds: Int,
    val averageScore: Double?
)

data class EpisodeProgressDto(
    val episodeId: Long,
    val userId: Long,
    val hasReadContent: Boolean,
    val hasListenedAudio: Boolean,
    val completedExercises: List<Long> = emptyList(),
    val totalContentItems: Int,
    val requiredContentItemsCompleted: Int,
    val isCompleted: Boolean,
    val timeSpentSeconds: Int,
    val averageScore: Double?
)

// ============================================================================
// EXTENSION FUNCTIONS
// ============================================================================

fun Course.toSummaryDto(totalModules: Int = 0, totalEpisodes: Int = 0) = CourseSummaryDto(
    id = id!!,
    slug = slug,
    name = name,
    languageCode = languageCode,
    cefrLevel = cefrLevel,
    description = description,
    estimatedHours = estimatedHours,
    totalModules = totalModules,
    totalEpisodes = totalEpisodes
)

fun Course.toDto(modules: List<ModuleSummaryDto> = emptyList()) = CourseDto(
    id = id!!,
    slug = slug,
    name = name,
    languageCode = languageCode,
    cefrLevel = cefrLevel,
    description = description,
    objectives = objectives?.let { parseJsonArrayToStringList(it) } ?: emptyList(),
    estimatedHours = estimatedHours,
    modules = modules
)

fun Module.toSummaryDto(totalEpisodes: Int = 0) = ModuleSummaryDto(
    id = id!!,
    courseId = courseId,
    moduleNumber = moduleNumber,
    title = title,
    theme = theme,
    estimatedMinutes = estimatedMinutes,
    totalEpisodes = totalEpisodes
)

fun Module.toDto(episodes: List<EpisodeSummaryDto> = emptyList()) = ModuleDto(
    id = id!!,
    courseId = courseId,
    moduleNumber = moduleNumber,
    title = title,
    theme = theme,
    description = description,
    objectives = objectives?.let { parseJsonArrayToStringList(it) } ?: emptyList(),
    estimatedMinutes = estimatedMinutes,
    episodes = episodes
)

fun Episode.toSummaryDto(totalExercises: Int = 0) = EpisodeSummaryDto(
    id = id!!,
    moduleId = moduleId,
    episodeNumber = episodeNumber,
    type = episodeType.name,
    title = title,
    estimatedMinutes = estimatedMinutes,
    hasAudio = audioUrl != null,
    totalExercises = totalExercises
)

fun Episode.toDto(contentItems: List<EpisodeContentItemDto> = emptyList()) = EpisodeDto(
    id = id!!,
    moduleId = moduleId,
    episodeNumber = episodeNumber,
    type = episodeType.name,
    title = title,
    content = content,
    data = data,  // Include structured JSON data (dialogue, story, etc.)
    audioUrl = audioUrl,
    transcript = transcript,
    estimatedMinutes = estimatedMinutes,
    contentItems = contentItems
)

fun GrammarRule.toDto() = GrammarRuleDto(
    id = id!!,
    name = name,
    description = description,
    examples = examples?.let { parseJsonArrayToStringList(it) } ?: emptyList(),
    category = category
)

// Helper function to parse JSON array to string list
private fun parseJsonArrayToStringList(jsonNode: JsonNode): List<String> {
    return if (jsonNode.isArray) {
        jsonNode.map { it.asText() }
    } else {
        emptyList()
    }
}
