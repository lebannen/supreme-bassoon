package com.vocabee.web.dto.admin

import com.vocabee.domain.generation.GeneratedEpisodeContent

data class SaveModuleRequest(
    val episodes: List<SaveEpisodeRequest>,
    val generateAudio: Boolean = false
)

data class SaveEpisodeRequest(
    val episodeNumber: Int,
    val title: String,
    val type: String, // DIALOGUE | STORY
    val summary: String,
    val content: GeneratedEpisodeContent
)
