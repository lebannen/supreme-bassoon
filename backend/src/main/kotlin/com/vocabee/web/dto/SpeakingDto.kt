package com.vocabee.web.dto

/**
 * Result of a speaking exercise attempt, returned after transcription and validation
 */
data class SpeakingAttemptResult(
    val transcription: String,
    val accuracy: Int,
    val isCorrect: Boolean,
    val feedback: String
)

/**
 * Internal representation of speaking exercise content extracted from exercise JSON
 */
data class SpeakingExerciseContent(
    val mode: String,           // "repeat", "read_aloud", "respond"
    val targetText: String,
    val targetLanguage: String,
    val nativeAudioUrl: String? = null,
    val prompt: String? = null,
    val promptAudioUrl: String? = null,
    val hint: String? = null,
    val acceptableVariations: List<String> = emptyList()
)
