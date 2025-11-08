package com.vocabee.web.dto

data class WordSetDto(
    val id: Long,
    val name: String,
    val description: String?,
    val languageCode: String,
    val languageName: String,
    val level: String?,
    val theme: String?,
    val wordCount: Int,
    val isImported: Boolean = false,
    val userVocabularyCount: Int = 0  // How many words from this set user already has
)

data class WordSetDetailDto(
    val id: Long,
    val name: String,
    val description: String?,
    val languageCode: String,
    val languageName: String,
    val level: String?,
    val theme: String?,
    val wordCount: Int,
    val isImported: Boolean = false,
    val words: List<WordSummaryDto> = emptyList()
)

data class ImportWordSetRequest(
    val wordSetId: Long,
    val addNotes: Boolean = false,  // Add set name as notes
    val notes: String? = null  // Custom notes for all words
)

data class ImportWordSetResponse(
    val wordSetId: Long,
    val totalWords: Int,
    val addedWords: Int,
    val alreadyInVocabulary: Int,
    val message: String
)
