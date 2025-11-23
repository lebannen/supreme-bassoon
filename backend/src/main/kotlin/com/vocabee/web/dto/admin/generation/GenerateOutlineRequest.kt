package com.vocabee.web.dto.admin.generation

data class GenerateOutlineRequest(
    val targetLanguage: String,
    val level: String,
    val moduleTheme: String,
    val moduleDescription: String,
    val seriesContext: String? = null
)
