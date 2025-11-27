package com.vocabee.web.dto.admin.generation

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.vocabee.domain.model.*
import java.time.LocalDateTime
import java.util.*

// ============================================================================
// Request DTOs
// ============================================================================

data class StartGenerationRequest(
    val languageCode: String,
    val cefrLevel: String,
    val moduleCount: Int,
    val episodesPerModule: Int = 2,
    val themePreferences: String? = null,
    val autoMode: Boolean = false
)

data class RegenerateFeedback(
    val feedback: String? = null,
    val specificChanges: List<String>? = null
)

// ============================================================================
// Response DTOs
// ============================================================================

data class GenerationProgressResponse(
    val id: UUID,
    val languageCode: String,
    val cefrLevel: String,
    val moduleCount: Int,
    val episodesPerModule: Int,
    val themePreferences: String?,
    val currentStage: GenerationStage,
    val stageProgress: StageProgressDto,
    val blueprint: BlueprintDto?,
    val modulePlans: List<ModulePlanDto>?,
    val characters: List<CharacterDto>?,
    val canProceed: Boolean,
    val canPublish: Boolean,
    val errorMessage: String?,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime?
)

data class StageProgressDto(
    val completed: Int,
    val total: Int,
    val currentItem: String?
)

data class BlueprintDto(
    val id: UUID,
    val courseTitle: String?,
    val courseDescription: String?,
    val setting: String?,
    val premise: String?,
    val plotArc: List<PlotArcPointDto>?,
    val moduleTopics: List<ModuleTopicDto>?,
    val grammarDistribution: List<GrammarDistributionDto>?,
    val status: GenerationStepStatus
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlotArcPointDto(
    val module: Int = 0,
    val arcPoint: String = "",
    val moduleNumber: Int? = null,
    val moduleNum: Int? = null,
    val plotPoint: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ModuleTopicDto(
    val module: Int = 0,
    val topic: String = "",
    val moduleNumber: Int? = null,
    val moduleNum: Int? = null,
    val theme: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GrammarDistributionDto(
    val module: Int = 0,
    val rules: List<String> = emptyList(),
    val moduleNumber: Int? = null,
    val moduleNum: Int? = null,
    val grammarRules: List<String>? = null
)

data class CharacterDto(
    val id: UUID,
    val name: String,
    val role: String?,
    val initialDescription: String?,
    val ageRange: String?,
    val personalityTraits: List<String>?,
    val background: String?,
    val appearanceDescription: String?,
    val voiceId: String?,
    val referenceImageUrl: String?
)

data class ModulePlanDto(
    val id: UUID,
    val moduleNumber: Int,
    val title: String?,
    val theme: String?,
    val description: String?,
    val objectives: List<String>?,
    val plotSummary: String?,
    val episodes: List<EpisodePlanDto>,
    val status: GenerationStepStatus
)

data class EpisodePlanDto(
    val id: UUID,
    val episodeNumber: Int,
    val title: String?,
    val sceneDescription: String?,
    val episodeType: String,
    val vocabulary: List<String>?,
    val grammarRules: List<String>?,
    val characterNames: List<String>?,
    val plotPoints: String?,
    val content: EpisodeContentDto?,
    val exercises: EpisodeExercisesDto?,
    val status: GenerationStepStatus
)

data class EpisodeContentDto(
    val id: UUID,
    val contentType: String?,
    val content: String?,
    val summary: String?,
    val vocabularyUsed: List<String>?,
    val vocabularyMissing: List<String>?,
    val status: GenerationStepStatus
)

data class EpisodeExercisesDto(
    val id: UUID,
    val exerciseCount: Int?,
    val vocabularyCoverage: List<String>?,
    val grammarCoverage: List<String>?,
    val status: GenerationStepStatus
)

data class GenerationSummaryDto(
    val id: UUID,
    val languageCode: String,
    val cefrLevel: String,
    val moduleCount: Int,
    val currentStage: GenerationStage,
    val blueprintTitle: String?,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime?
)

// ============================================================================
// Internal Generation Schema DTOs (for AI responses)
// ============================================================================

@JsonIgnoreProperties(ignoreUnknown = true)
data class BlueprintGenerationResult(
    val courseTitle: String = "",
    val courseDescription: String = "",
    val setting: String = "",
    val premise: String = "",
    val plotArc: List<PlotArcPointDto> = emptyList(),
    val moduleTopics: List<ModuleTopicDto> = emptyList(),
    val characters: List<CharacterSeedDto> = emptyList(),
    val grammarTaxonomy: List<GeneratedGrammarRuleDto>? = null,
    val grammarDistribution: List<GrammarDistributionDto> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CharacterSeedDto(
    val name: String = "",
    val role: String = "",
    val gender: String = "",
    val age: String = "",
    val personality: List<String> = emptyList(),
    val background: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeneratedGrammarRuleDto(
    val slug: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val examples: List<GrammarExampleDto>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GrammarExampleDto(
    val target: String = "",
    val native: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ModulePlanGenerationResult(
    val title: String = "",
    val theme: String = "",
    val description: String = "",
    val objectives: List<String> = emptyList(),
    val plotSummary: String = "",
    val episodes: List<EpisodePlanGenerationResult> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodePlanGenerationResult(
    val episodeNumber: Int = 0,
    val title: String = "",
    val sceneDescription: String = "",
    val episodeType: String = "DIALOGUE",
    val vocabulary: List<String> = emptyList(),
    val grammarRules: List<String> = emptyList(),
    val characterNames: List<String> = emptyList(),
    val plotPoints: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodeContentGenerationResult(
    val dialogue: List<DialogueLineDto>? = null,
    val story: String? = null,
    val summary: String = "",
    val characterDevelopments: List<CharacterDevelopmentDto>? = null,
    val vocabularyUsed: List<String> = emptyList(),
    val vocabularyMissing: List<String> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DialogueLineDto(
    val speaker: String = "",
    val text: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CharacterDevelopmentDto(
    val characterName: String = "",
    val note: String = ""
)

// ============================================================================
// Debug/Raw Data DTOs - For viewing complete AI responses
// ============================================================================

data class GenerationDebugResponse(
    val id: UUID,
    val languageCode: String,
    val cefrLevel: String,
    val moduleCount: Int,
    val episodesPerModule: Int,
    val themePreferences: String?,
    val currentStage: GenerationStage,
    val autoMode: Boolean,
    val errorMessage: String?,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val blueprint: BlueprintDebugDto?,
    val modulePlans: List<ModulePlanDebugDto>,
    val characters: List<CharacterDebugDto>,
    val media: List<MediaDebugDto>
)

data class BlueprintDebugDto(
    val id: UUID,
    val courseTitle: String?,
    val courseDescription: String?,
    val setting: String?,
    val premise: String?,
    val plotArc: Any?,
    val moduleTopics: Any?,
    val grammarDistribution: Any?,
    val status: GenerationStepStatus,
    val rawResponse: String?,
    val createdAt: LocalDateTime?
)

data class ModulePlanDebugDto(
    val id: UUID,
    val moduleNumber: Int,
    val title: String?,
    val theme: String?,
    val description: String?,
    val objectives: Any?,
    val plotSummary: String?,
    val status: GenerationStepStatus,
    val rawResponse: String?,
    val createdAt: LocalDateTime?,
    val episodes: List<EpisodePlanDebugDto>
)

data class EpisodePlanDebugDto(
    val id: UUID,
    val episodeNumber: Int,
    val title: String?,
    val sceneDescription: String?,
    val episodeType: String,
    val vocabulary: Any?,
    val grammarRules: Any?,
    val characterIds: Any?,
    val plotPoints: String?,
    val status: GenerationStepStatus,
    val createdAt: LocalDateTime?,
    val content: EpisodeContentDebugDto?,
    val exercises: EpisodeExercisesDebugDto?
)

data class EpisodeContentDebugDto(
    val id: UUID,
    val contentType: String?,
    val content: String?,
    val contentStructured: Any?,
    val summary: String?,
    val characterDevelopments: Any?,
    val vocabularyUsed: Any?,
    val vocabularyMissing: Any?,
    val imagePrompts: Any?,
    val status: GenerationStepStatus,
    val rawResponse: String?,
    val createdAt: LocalDateTime?
)

data class EpisodeExercisesDebugDto(
    val id: UUID,
    val exercises: Any?,
    val exerciseCount: Int?,
    val vocabularyCoverage: Any?,
    val grammarCoverage: Any?,
    val status: GenerationStepStatus,
    val rawResponse: String?,
    val createdAt: LocalDateTime?
)

data class CharacterDebugDto(
    val id: UUID,
    val name: String,
    val role: String?,
    val gender: String?,
    val initialDescription: String?,
    val ageRange: String?,
    val personalityTraits: Any?,
    val background: String?,
    val appearanceDescription: String?,
    val voiceId: String?,
    val referenceImageUrl: String?,
    val developments: Any?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

data class MediaDebugDto(
    val id: UUID,
    val mediaType: String,
    val episodePlanId: UUID?,
    val characterId: UUID?,
    val url: String?,
    val metadata: Any?,
    val status: GenerationStepStatus,
    val errorMessage: String?,
    val createdAt: LocalDateTime?
)
