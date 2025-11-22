package com.vocabee.web.dto.admin.generation

data class GenerateStructureRequest(
    val script: String,
    val theme: String,
    val level: String,
    val targetLanguage: String
)
