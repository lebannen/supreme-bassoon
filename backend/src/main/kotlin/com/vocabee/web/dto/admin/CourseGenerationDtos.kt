package com.vocabee.web.dto.admin

import com.vocabee.domain.generation.CharacterProfile
import com.vocabee.domain.generation.GeneratedEpisodeContent
import com.vocabee.domain.validation.EpisodeValidationResult
import com.vocabee.domain.validation.ValidationIssue

// Batch generation request
data class GenerateCourseContentRequest(
    val courseId: Long
)

// Episode generation result
data class EpisodeGenerationResult(
    val episodeNumber: Int,
    val title: String,
    val type: String,
    val content: GeneratedEpisodeContent?,
    val validation: EpisodeValidationResult
)

// Module generation result
data class ModuleGenerationResult(
    val moduleId: Long,
    val moduleNumber: Int,
    val title: String,
    val episodes: List<EpisodeGenerationResult>
) {
    val validCount: Int
        get() = episodes.count { it.validation.isValid }

    val totalCount: Int
        get() = episodes.size
}

// Character analysis
data class CharacterInfo(
    val name: String,
    val appearances: Int,  // Total dialogue lines
    val episodes: List<EpisodeReference>
)

data class EpisodeReference(
    val moduleNumber: Int,
    val episodeNumber: Int,
    val lineCount: Int
)

data class CharacterAnalysis(
    val characters: List<CharacterInfo>,
    val totalDialogues: Int
)

// Validation summary
data class ValidationSummary(
    val totalEpisodes: Int,
    val validEpisodes: Int,
    val episodesWithWarnings: Int,
    val episodesWithErrors: Int
)

// Complete batch generation response
data class GenerateCourseContentResponse(
    val modules: List<ModuleGenerationResult>,
    val characterAnalysis: CharacterAnalysis,
    val validationSummary: ValidationSummary,
    val characterProfiles: List<CharacterProfile> = emptyList()
)

// Voice assignment
data class VoiceAssignment(
    val characterName: String,
    val voiceName: String,
    val gender: String? = null
)

// Save course content request (after voice assignment)
data class SaveCourseContentRequest(
    val courseId: Long,
    val modules: List<SaveModuleContentRequest>,
    val voiceAssignments: List<VoiceAssignment>,
    val characterProfiles: List<CharacterProfile> = emptyList(),
    val generateAudio: Boolean = true,
    val generateImages: Boolean = true
)

data class SaveModuleContentRequest(
    val moduleId: Long,
    val episodes: List<SaveEpisodeContentRequest>
)

data class SaveEpisodeContentRequest(
    val episodeNumber: Int,
    val title: String,
    val type: String,
    val summary: String,
    val content: GeneratedEpisodeContent
)

// DTO for validation issues (for JSON serialization)
data class ValidationIssueDto(
    val field: String,
    val message: String,
    val severity: String
)

fun ValidationIssue.toDto() = ValidationIssueDto(
    field = this.field,
    message = this.message,
    severity = this.severity.name
)

data class EpisodeValidationResultDto(
    val isValid: Boolean,
    val issues: List<ValidationIssueDto>,
    val hasErrors: Boolean,
    val hasWarnings: Boolean
)

fun EpisodeValidationResult.toDto() = EpisodeValidationResultDto(
    isValid = this.isValid,
    issues = this.issues.map { it.toDto() },
    hasErrors = this.hasErrors,
    hasWarnings = this.hasWarnings
)
