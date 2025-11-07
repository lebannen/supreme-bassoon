package com.vocabee.web.dto

data class WordDto(
    val id: Long,
    val languageCode: String,
    val lemma: String,
    val partOfSpeech: String?,
    val etymology: String?,
    val usageNotes: String?,
    val frequencyRank: Int?,
    val isInflectedForm: Boolean = false,
    val lemmaId: Long? = null,
    val grammaticalFeatures: Map<String, Any>? = null,
    val baseForm: BaseFormDto? = null,
    val definitions: List<DefinitionDto> = emptyList(),
    val pronunciations: List<PronunciationDto> = emptyList(),
    val inflectedForms: List<InflectedFormDto> = emptyList()
)

data class DefinitionDto(
    val id: Long,
    val definitionNumber: Int,
    val definitionText: String,
    val examples: List<ExampleDto> = emptyList()
)

data class PronunciationDto(
    val id: Long,
    val ipa: String?,
    val audioUrl: String?,
    val dialect: String?
)

data class InflectedFormDto(
    val id: Long,
    val form: String,
    val partOfSpeech: String?,
    val grammaticalFeatures: Map<String, Any>?
)

data class BaseFormDto(
    val id: Long,
    val lemma: String,
    val partOfSpeech: String?
)

data class ExampleDto(
    val id: Long,
    val sentenceText: String,
    val translation: String?
)

data class SearchResultDto(
    val words: List<WordSummaryDto>,
    val total: Int
)

data class WordSummaryDto(
    val id: Long,
    val lemma: String,
    val partOfSpeech: String?,
    val frequencyRank: Int?
)

data class LanguageDto(
    val code: String,
    val name: String,
    val entryCount: Int
)
