package com.vocabee.domain.generation

data class GeneratedEpisodeContent(
    val dialogue: GeneratedDialogue? = null,
    val story: String? = null,
    val exercises: List<GeneratedContentItem> = emptyList()
)
