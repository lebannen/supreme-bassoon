package com.vocabee.web.dto.admin.generation

data class GenerateSyllabusRequest(
    val targetLanguage: String,
    val level: String,
    val seriesContext: String // The "Bible"
)
