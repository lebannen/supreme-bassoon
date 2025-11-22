package com.vocabee.web.dto.admin.generation

data class GenerateExerciseRequest(
    val context: String,
    val type: String, // multiple_choice, fill_in_blank, etc.
    val targetLanguage: String,
    val level: String,
    val instructions: String? = null
)
