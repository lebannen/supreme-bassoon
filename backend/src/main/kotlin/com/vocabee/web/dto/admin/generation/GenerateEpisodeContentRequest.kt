package com.vocabee.web.dto.admin.generation

data class GenerateEpisodeContentRequest(
    val targetLanguage: String,
    val level: String,
    val seriesContext: String?,
    val moduleTheme: String,
    val episodeTitle: String,
    val episodeType: String, // DIALOGUE | STORY
    val episodeSummary: String
)
