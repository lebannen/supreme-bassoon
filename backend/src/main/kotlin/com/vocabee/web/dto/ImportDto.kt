package com.vocabee.web.dto

import com.vocabee.domain.model.ImportStatus
import java.time.LocalDateTime

data class ImportProgressDto(
    val importId: String,
    val languageCode: String,
    val languageName: String,
    val status: ImportStatus,
    val totalEntries: Long,
    val processedEntries: Long,
    val successfulEntries: Long,
    val failedEntries: Long,
    val progressPercentage: Int,
    val message: String,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val error: String?
)

data class ImportStartResponse(
    val importId: String,
    val message: String
)

data class WordEntryJson(
    val word: String?,  // Present for inflected forms
    val lemma: String?,  // Present for non-inflected forms
    val normalized: String?,
    val part_of_speech: String?,
    val etymology: String?,
    val usage_notes: String?,
    val is_inflected_form: Boolean?,
    val inflected_form_of: InflectedFormInfo?,
    val definitions: List<DefinitionJson>?,
    val pronunciations: List<PronunciationJson>?,
    val examples: List<ExampleJson>?,
    // Metadata fields (present only in first line of JSONL)
    val _metadata: Boolean?,
    val language: String?,
    val language_code: String?
) {
    // Get the actual word/lemma text
    fun getWordText(): String = word ?: lemma ?: throw IllegalStateException("Both word and lemma are null")

    // Check if this is a metadata entry
    val is_metadata: Boolean get() = _metadata == true
}

data class InflectedFormInfo(
    val lemma: String,
    val part_of_speech: String?,
    val person: String?,
    val number: String?,
    val tense: String?,
    val mood: String?,
    val gender: String?,
    val case: String?
)

data class DefinitionJson(
    val definition_number: Int,
    val text: String,
    val examples: List<ExampleJson>?
)

data class PronunciationJson(
    val ipa: String?,
    val audio_file: String?,
    val dialect: String?
)

data class ExampleJson(
    val text: String,
    val translation: String?
)
