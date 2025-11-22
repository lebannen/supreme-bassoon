package com.vocabee.web.dto.admin.generation

import com.vocabee.domain.generation.GeneratedModule

data class ModuleGenerationResponse(
    val module: GeneratedModule,
    val validationErrors: List<String> = emptyList()
)
