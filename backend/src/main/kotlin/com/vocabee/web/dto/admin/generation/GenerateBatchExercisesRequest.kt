package com.vocabee.web.dto.admin.generation

data class GenerateBatchExercisesRequest(
    val context: String,
    val targetLanguage: String,
    val level: String,
    val exerciseCounts: Map<String, Int> = mapOf(
        "MULTIPLE_CHOICE" to 4,
        "FILL_IN_THE_BLANK" to 4,
        "SENTENCE_SCRAMBLE" to 2,
        "CLOZE_READING" to 1,
        "MATCHING" to 2
    )
)
