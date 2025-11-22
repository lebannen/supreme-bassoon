package com.vocabee.web.dto.admin.generation

data class GenerateModuleRequest(
    val theme: String,
    val level: String,
    val targetLanguage: String = "French",
    val contentType: String = "DIALOGUE", // DIALOGUE or STORY
    val focus: String? = null,
    val additionalInstructions: String? = null
)
