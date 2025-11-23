package com.vocabee.service

import com.vocabee.domain.generation.GeneratedContentItem
import com.vocabee.domain.generation.GeneratedDialogue
import com.vocabee.domain.generation.GeneratedEpisodeContent
import com.vocabee.domain.validation.EpisodeValidationResult
import com.vocabee.domain.validation.ErrorSeverity
import com.vocabee.domain.validation.ValidationIssue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EpisodeValidationService {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun validateEpisodeContent(
        content: GeneratedEpisodeContent,
        type: String
    ): EpisodeValidationResult {
        val issues = mutableListOf<ValidationIssue>()

        try {
            // Validate based on type
            when (type.uppercase()) {
                "DIALOGUE" -> validateDialogue(content.dialogue, issues)
                "STORY" -> validateStory(content.story, issues)
            }

            // Validate exercises (common for both types)
            validateExercises(content.exercises, issues)

        } catch (e: Exception) {
            logger.error("Validation failed with exception", e)
            issues.add(
                ValidationIssue(
                    field = "general",
                    message = "Validation failed: ${e.message}",
                    severity = ErrorSeverity.CRITICAL
                )
            )
        }

        val isValid = issues.none { it.severity == ErrorSeverity.CRITICAL || it.severity == ErrorSeverity.ERROR }

        return EpisodeValidationResult(
            isValid = isValid,
            issues = issues
        )
    }

    private fun validateDialogue(
        dialogue: GeneratedDialogue?,
        issues: MutableList<ValidationIssue>
    ) {
        if (dialogue == null) {
            issues.add(
                ValidationIssue(
                    field = "dialogue",
                    message = "Dialogue content is missing",
                    severity = ErrorSeverity.CRITICAL
                )
            )
            return
        }

        if (dialogue.lines.isEmpty()) {
            issues.add(
                ValidationIssue(
                    field = "dialogue.lines",
                    message = "Dialogue has no lines",
                    severity = ErrorSeverity.CRITICAL
                )
            )
            return
        }

        // Check speaker count (max 2 for audio generation)
        val speakers = dialogue.lines.map { it.speaker }.distinct()
        if (speakers.isEmpty()) {
            issues.add(
                ValidationIssue(
                    field = "dialogue.speakers",
                    message = "No speakers found in dialogue",
                    severity = ErrorSeverity.CRITICAL
                )
            )
        } else if (speakers.size > 2) {
            issues.add(
                ValidationIssue(
                    field = "dialogue.speakers",
                    message = "Found ${speakers.size} speakers (${speakers.joinToString(", ")}) - maximum is 2 for audio generation",
                    severity = ErrorSeverity.ERROR
                )
            )
        }

        // Check for "Narrator" speaker
        speakers.forEach { speaker ->
            if (speaker.equals("Narrator", ignoreCase = true)) {
                issues.add(
                    ValidationIssue(
                        field = "dialogue.speakers",
                        message = "Speaker 'Narrator' found - not allowed in dialogues (use 2 character names instead)",
                        severity = ErrorSeverity.ERROR
                    )
                )
            }
        }

        // Check for potential speaker name inconsistencies
        detectSpeakerNameChanges(dialogue.lines, issues)

        // Check minimum lines
        if (dialogue.lines.size < 5) {
            issues.add(
                ValidationIssue(
                    field = "dialogue.lines",
                    message = "Only ${dialogue.lines.size} dialogue lines - recommended minimum is 5",
                    severity = ErrorSeverity.WARNING
                )
            )
        }

        // Validate each line has content
        dialogue.lines.forEachIndexed { index, line ->
            if (line.speaker.isBlank()) {
                issues.add(
                    ValidationIssue(
                        field = "dialogue.lines[$index].speaker",
                        message = "Line $index has empty speaker name",
                        severity = ErrorSeverity.ERROR
                    )
                )
            }
            if (line.text.isBlank()) {
                issues.add(
                    ValidationIssue(
                        field = "dialogue.lines[$index].text",
                        message = "Line $index has empty text",
                        severity = ErrorSeverity.ERROR
                    )
                )
            }
        }
    }

    private fun detectSpeakerNameChanges(
        lines: List<com.vocabee.domain.generation.GeneratedDialogueLine>,
        issues: MutableList<ValidationIssue>
    ) {
        // Look for similar speaker names that might indicate inconsistency
        val speakers = lines.map { it.speaker }.distinct()

        for (i in speakers.indices) {
            for (j in i + 1 until speakers.size) {
                val speaker1 = speakers[i]
                val speaker2 = speakers[j]

                // Check for similar names (simple substring check)
                if (speaker1.contains(speaker2, ignoreCase = true) ||
                    speaker2.contains(speaker1, ignoreCase = true)
                ) {
                    issues.add(
                        ValidationIssue(
                            field = "dialogue.speakers",
                            message = "Possible speaker name inconsistency: '$speaker1' and '$speaker2' appear to be related",
                            severity = ErrorSeverity.WARNING
                        )
                    )
                }
            }
        }
    }

    private fun validateStory(
        story: String?,
        issues: MutableList<ValidationIssue>
    ) {
        if (story == null) {
            issues.add(
                ValidationIssue(
                    field = "story",
                    message = "Story content is missing",
                    severity = ErrorSeverity.CRITICAL
                )
            )
            return
        }

        if (story.isBlank()) {
            issues.add(
                ValidationIssue(
                    field = "story",
                    message = "Story content is empty",
                    severity = ErrorSeverity.CRITICAL
                )
            )
            return
        }

        // Check minimum length
        if (story.length < 100) {
            issues.add(
                ValidationIssue(
                    field = "story",
                    message = "Story is very short (${story.length} characters) - recommended minimum is 100",
                    severity = ErrorSeverity.WARNING
                )
            )
        }
    }

    private fun validateExercises(
        exercises: List<GeneratedContentItem>?,
        issues: MutableList<ValidationIssue>
    ) {
        if (exercises == null || exercises.isEmpty()) {
            issues.add(
                ValidationIssue(
                    field = "exercises",
                    message = "No exercises generated",
                    severity = ErrorSeverity.ERROR
                )
            )
            return
        }

        // Check expected count (13 total)
        if (exercises.size < 10) {
            issues.add(
                ValidationIssue(
                    field = "exercises",
                    message = "Generated ${exercises.size} exercises - recommended is 13",
                    severity = ErrorSeverity.WARNING
                )
            )
        } else if (exercises.size != 13) {
            issues.add(
                ValidationIssue(
                    field = "exercises",
                    message = "Generated ${exercises.size} exercises, expected 13",
                    severity = ErrorSeverity.WARNING
                )
            )
        }

        // Validate each exercise
        exercises.forEachIndexed { index, item ->
            validateExercise(item, index, issues)
        }
    }

    private fun validateExercise(
        item: GeneratedContentItem,
        index: Int,
        issues: MutableList<ValidationIssue>
    ) {
        if (item.type != "EXERCISE") {
            issues.add(
                ValidationIssue(
                    field = "exercises[$index].type",
                    message = "Item type is '${item.type}', expected 'EXERCISE'",
                    severity = ErrorSeverity.ERROR
                )
            )
            return
        }

        val exercise = item.exercise
        val content = exercise.content

        when (exercise.type.uppercase().replace("-", "_")) {
            "FILL_IN_THE_BLANK", "FILL_IN_BLANK" -> {
                if (!content.containsKey("text")) {
                    issues.add(
                        ValidationIssue(
                            field = "exercises[$index].content.text",
                            message = "Fill-in-blank exercise missing required field: text",
                            severity = ErrorSeverity.ERROR
                        )
                    )
                }
                val blanks = content["blanks"]
                if (blanks == null || blanks !is List<*>) {
                    issues.add(
                        ValidationIssue(
                            field = "exercises[$index].content.blanks",
                            message = "Fill-in-blank exercise missing or invalid field: blanks (must be array)",
                            severity = ErrorSeverity.ERROR
                        )
                    )
                } else {
                    // Validate each blank has correctAnswer
                    blanks.forEachIndexed { blankIndex, blank ->
                        if (blank is Map<*, *>) {
                            if (!blank.containsKey("correctAnswer")) {
                                issues.add(
                                    ValidationIssue(
                                        field = "exercises[$index].content.blanks[$blankIndex]",
                                        message = "Blank missing required field: correctAnswer",
                                        severity = ErrorSeverity.ERROR
                                    )
                                )
                            }
                        }
                    }
                }
            }

            "SENTENCE_SCRAMBLE" -> {
                val words = content["words"]
                if (words == null || words !is List<*>) {
                    issues.add(
                        ValidationIssue(
                            field = "exercises[$index].content.words",
                            message = "Sentence scramble missing or invalid field: words",
                            severity = ErrorSeverity.ERROR
                        )
                    )
                }
                val correctOrder = content["correctOrder"]
                if (correctOrder == null || correctOrder !is List<*>) {
                    issues.add(
                        ValidationIssue(
                            field = "exercises[$index].content.correctOrder",
                            message = "Sentence scramble missing or invalid field: correctOrder",
                            severity = ErrorSeverity.ERROR
                        )
                    )
                }
                // Validate words and correctOrder match
                if (words is List<*> && correctOrder is List<*>) {
                    if (words.size != correctOrder.size) {
                        issues.add(
                            ValidationIssue(
                                field = "exercises[$index].content",
                                message = "Sentence scramble words array (${words.size}) and correctOrder array (${correctOrder.size}) have different lengths",
                                severity = ErrorSeverity.ERROR
                            )
                        )
                    }
                }
            }

            "MULTIPLE_CHOICE" -> {
                if (!content.containsKey("question")) {
                    issues.add(
                        ValidationIssue(
                            field = "exercises[$index].content.question",
                            message = "Multiple choice missing required field: question",
                            severity = ErrorSeverity.ERROR
                        )
                    )
                }
                val options = content["options"]
                if (options == null || options !is List<*>) {
                    issues.add(
                        ValidationIssue(
                            field = "exercises[$index].content.options",
                            message = "Multiple choice missing or invalid field: options",
                            severity = ErrorSeverity.ERROR
                        )
                    )
                } else {
                    if (options.size < 2) {
                        issues.add(
                            ValidationIssue(
                                field = "exercises[$index].content.options",
                                message = "Multiple choice has ${options.size} options - minimum is 2",
                                severity = ErrorSeverity.ERROR
                            )
                        )
                    }
                    // Check that at least one option is correct
                    val hasCorrect = options.any { option ->
                        option is Map<*, *> && option["isCorrect"] == true
                    }
                    if (!hasCorrect) {
                        issues.add(
                            ValidationIssue(
                                field = "exercises[$index].content.options",
                                message = "Multiple choice has no correct answer",
                                severity = ErrorSeverity.ERROR
                            )
                        )
                    }
                }
            }

            "MATCHING" -> {
                val pairs = content["pairs"]
                if (pairs == null || pairs !is List<*>) {
                    issues.add(
                        ValidationIssue(
                            field = "exercises[$index].content.pairs",
                            message = "Matching exercise missing or invalid field: pairs",
                            severity = ErrorSeverity.ERROR
                        )
                    )
                } else {
                    if (pairs.size < 2) {
                        issues.add(
                            ValidationIssue(
                                field = "exercises[$index].content.pairs",
                                message = "Matching exercise has ${pairs.size} pairs - minimum is 2",
                                severity = ErrorSeverity.ERROR
                            )
                        )
                    }
                    // Validate each pair has left and right
                    pairs.forEachIndexed { pairIndex, pair ->
                        if (pair is Map<*, *>) {
                            if (!pair.containsKey("left") || !pair.containsKey("right")) {
                                issues.add(
                                    ValidationIssue(
                                        field = "exercises[$index].content.pairs[$pairIndex]",
                                        message = "Pair missing 'left' or 'right' field",
                                        severity = ErrorSeverity.ERROR
                                    )
                                )
                            }
                        }
                    }
                }
            }

            "CLOZE_READING" -> {
                if (!content.containsKey("text")) {
                    issues.add(
                        ValidationIssue(
                            field = "exercises[$index].content.text",
                            message = "Cloze reading missing required field: text",
                            severity = ErrorSeverity.ERROR
                        )
                    )
                }
                val blanks = content["blanks"]
                if (blanks == null || blanks !is List<*>) {
                    issues.add(
                        ValidationIssue(
                            field = "exercises[$index].content.blanks",
                            message = "Cloze reading missing or invalid field: blanks",
                            severity = ErrorSeverity.ERROR
                        )
                    )
                } else {
                    // Validate each blank has id and correctAnswer
                    blanks.forEachIndexed { blankIndex, blank ->
                        if (blank is Map<*, *>) {
                            if (!blank.containsKey("id")) {
                                issues.add(
                                    ValidationIssue(
                                        field = "exercises[$index].content.blanks[$blankIndex]",
                                        message = "Blank missing required field: id",
                                        severity = ErrorSeverity.ERROR
                                    )
                                )
                            }
                            if (!blank.containsKey("correctAnswer")) {
                                issues.add(
                                    ValidationIssue(
                                        field = "exercises[$index].content.blanks[$blankIndex]",
                                        message = "Blank missing required field: correctAnswer",
                                        severity = ErrorSeverity.ERROR
                                    )
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                issues.add(
                    ValidationIssue(
                        field = "exercises[$index].type",
                        message = "Unknown exercise type: ${exercise.type}",
                        severity = ErrorSeverity.WARNING
                    )
                )
            }
        }
    }
}
