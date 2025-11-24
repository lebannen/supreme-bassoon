package com.vocabee.domain.generation

data class ImagePrompt(
    val description: String,
    val sceneContext: String
)

data class GeneratedEpisodeContent(
    val dialogue: GeneratedDialogue? = null,
    val story: String? = null,
    val exercises: List<GeneratedContentItem> = emptyList(),
    val imagePrompts: List<ImagePrompt> = emptyList()
)
