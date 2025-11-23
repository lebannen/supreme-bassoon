package com.vocabee.web.dto.admin

import com.vocabee.domain.generation.GeneratedEpisodeSummary
import com.vocabee.domain.generation.GeneratedSyllabus

data class CreateCourseRequest(
    val name: String,
    val targetLanguage: String,
    val level: String,
    val seriesContext: String,
    val syllabus: GeneratedSyllabus,
    val enrichedModules: List<EnrichedModuleDto>? = null
)

data class EnrichedModuleDto(
    val moduleNumber: Int,
    val title: String,
    val theme: String,
    val description: String,
    val detailedDescription: String? = null,
    val objectives: List<String>? = null,
    val vocabularyFocus: List<String>? = null,
    val grammarFocus: List<String>? = null,
    val episodeOutline: List<GeneratedEpisodeSummary>? = null
)
