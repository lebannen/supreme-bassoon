package com.vocabee.web.dto

data class WordDto(
    val id: Long,
    val languageCode: String,
    val lemma: String,
    val partOfSpeech: String?,
    val frequencyRank: Int?,
    val definitions: List<DefinitionDto> = emptyList(),
    val pronunciations: List<PronunciationDto> = emptyList(),
    val wordForms: List<WordFormDto> = emptyList()
)

data class DefinitionDto(
    val id: Long,
    val definitionNumber: Int,
    val definitionText: String,
    val etymology: String?,
    val examples: List<ExampleDto> = emptyList()
)

data class PronunciationDto(
    val id: Long,
    val ipa: String?,
    val audioUrl: String?,
    val dialect: String?
)

data class WordFormDto(
    val id: Long,
    val form: String,
    val formType: String?,
    val metadata: Map<String, Any>?
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
