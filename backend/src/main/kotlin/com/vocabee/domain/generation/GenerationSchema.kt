package com.vocabee.domain.generation

data class GeneratedModule(
    val title: String,
    val theme: String,
    val description: String,
    val objectives: List<String>,
    val episodes: List<GeneratedEpisode>
)

data class GeneratedEpisode(
    val title: String,
    val type: String, // DIALOGUE, STORY
    val dialogue: GeneratedDialogue?,
    val contentItems: List<GeneratedContentItem>
)

data class GeneratedDialogue(
    val lines: List<GeneratedDialogueLine>,
    val speakers: Map<String, GeneratedSpeaker>
)

data class GeneratedDialogueLine(
    val speaker: String,
    val text: String
)

data class GeneratedSpeaker(
    val voice: String,
    val description: String
)

data class GeneratedContentItem(
    val type: String, // EXERCISE
    val exercise: GeneratedExercise
)

data class GeneratedExercise(
    val type: String, // multiple_choice, etc.
    val meta: Map<String, Any>? = null,
    val content: Map<String, Any>
)
