package com.vocabee.web.dto.admin.generation

data class GenerateModulePlanRequest(
    val targetLanguage: String,
    val level: String,
    val seriesContext: String,
    val moduleNumber: Int,
    val moduleTitle: String,
    val moduleTheme: String,
    val moduleDescription: String
)
