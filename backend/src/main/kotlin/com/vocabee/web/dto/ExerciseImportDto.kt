package com.vocabee.web.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

/**
 * Module exercise data structure matching JSON format from course-content files
 */
data class ModuleExerciseData(
    val module: Int,
    val title: String,
    @JsonProperty("cefr_level")
    val cefrLevel: String,
    @JsonProperty("language_code")
    val languageCode: String = "fr",
    val exercises: List<ExerciseImportData>
)

/**
 * Single exercise data for import
 */
data class ExerciseImportData(
    val type: String,
    val title: String,
    val instructions: String,
    val content: JsonNode
)

/**
 * Request for importing exercises
 */
data class ImportExercisesRequest(
    val content: ModuleExerciseData,
    val generateAudio: Boolean = true,
    val overwriteExisting: Boolean = false
)

/**
 * Result of exercise import operation
 */
data class ExerciseImportResult(
    val imported: Int,
    val skipped: Int,
    val audioGenerated: Int,
    val audioFailed: Int,
    val errors: List<String>,
    val exercises: List<ImportedExerciseDto>
)

/**
 * Details of a single imported exercise
 */
data class ImportedExerciseDto(
    val id: Long?,
    val title: String,
    val type: String,
    val audioUrl: String? = null,
    val status: ExerciseImportStatus
)

enum class ExerciseImportStatus {
    IMPORTED,
    SKIPPED,
    FAILED
}
