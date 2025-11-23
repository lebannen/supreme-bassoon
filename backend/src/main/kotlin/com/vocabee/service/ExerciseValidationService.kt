package com.vocabee.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.web.dto.ValidationResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExerciseValidationService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()

    fun validate(
        exerciseType: String,
        content: JsonNode,
        userResponses: JsonNode
    ): ValidationResult {
        return when (exerciseType) {
            "multiple_choice" -> validateMultipleChoice(content, userResponses)
            "fill_in_blank" -> validateFillInBlank(content, userResponses)
            "sentence_scramble" -> validateSentenceScramble(content, userResponses)
            "matching" -> validateMatching(content, userResponses)
            "listening" -> validateListening(content, userResponses)
            "cloze_reading" -> validateClozeReading(content, userResponses)
            else -> throw UnsupportedExerciseTypeException("Unsupported exercise type: $exerciseType")
        }
    }

    // Multiple Choice validation
    private fun validateMultipleChoice(content: JsonNode, userResponses: JsonNode): ValidationResult {
        // Extract user's selected option
        val selectedOption = userResponses.get("selectedOption")?.asText()
            ?: return ValidationResult(
                isCorrect = false,
                score = 0.0,
                feedback = "No option selected",
                correctAnswers = null
            )

        // Find the correct option
        val options = content.get("options")
        var correctOptionId: String? = null

        options.forEach { option ->
            if (option.get("isCorrect")?.asBoolean() == true) {
                correctOptionId = option.get("id")?.asText()
            }
        }

        if (correctOptionId == null) {
            throw IllegalStateException("No correct answer defined in exercise content")
        }

        // Check if user's answer is correct
        val isCorrect = selectedOption == correctOptionId
        val explanation = content.get("explanation")?.asText() ?: "Check the correct answer above."

        return ValidationResult(
            isCorrect = isCorrect,
            score = if (isCorrect) 100.0 else 0.0,
            feedback = if (isCorrect) {
                "Correct! $explanation"
            } else {
                "Incorrect. $explanation"
            },
            correctAnswers = objectMapper.createObjectNode().apply {
                put("correctOption", correctOptionId)
            }
        )
    }

    // Fill in the Blank validation
    private fun validateFillInBlank(content: JsonNode, userResponses: JsonNode): ValidationResult {
        // Check if this is multi-blank or single-blank format
        val userAnswersArray = userResponses.get("answers")

        return if (userAnswersArray != null && userAnswersArray.isArray) {
            // Multi-blank format
            validateMultiBlankFillIn(content, userAnswersArray)
        } else {
            // Single-blank format (backward compatibility)
            validateSingleBlankFillIn(content, userResponses)
        }
    }

    private fun validateSingleBlankFillIn(content: JsonNode, userResponses: JsonNode): ValidationResult {
        // Extract user's answer
        val userAnswer = userResponses.get("answer")?.asText()?.trim()
            ?: return ValidationResult(
                isCorrect = false,
                score = 0.0,
                feedback = "No answer provided",
                correctAnswers = null
            )

        // Extract correct answer
        val correctAnswer = content.get("correctAnswer")?.asText()
            ?: throw IllegalStateException("No correct answer defined in exercise content")

        // Check if user's answer is correct (case-insensitive comparison)
        val isCorrect = userAnswer.equals(correctAnswer, ignoreCase = true)

        // Get additional content for feedback
        val grammarExplanation = content.get("grammarExplanation")?.asText() ?: ""
        val translation = content.get("translation")?.asText()

        // Build feedback message
        val feedback = if (isCorrect) {
            buildString {
                append("Correct! ")
                if (grammarExplanation.isNotEmpty()) {
                    append(grammarExplanation)
                }
                if (translation != null) {
                    append(" Translation: $translation")
                }
            }
        } else {
            buildString {
                append("Incorrect. The correct answer is '$correctAnswer'. ")
                if (grammarExplanation.isNotEmpty()) {
                    append(grammarExplanation)
                }
            }
        }

        return ValidationResult(
            isCorrect = isCorrect,
            score = if (isCorrect) 100.0 else 0.0,
            feedback = feedback,
            correctAnswers = objectMapper.createObjectNode().apply {
                put("correctAnswer", correctAnswer)
            }
        )
    }

    private fun validateMultiBlankFillIn(content: JsonNode, userAnswersArray: JsonNode): ValidationResult {
        // Extract blanks configuration
        val blanks = content.get("blanks")
            ?: throw IllegalStateException("No blanks defined in multi-blank exercise content")

        if (!blanks.isArray) {
            throw IllegalStateException("Blanks must be an array")
        }

        // Convert user answers to list
        val userAnswers = mutableListOf<String>()
        userAnswersArray.forEach { userAnswers.add(it.asText().trim()) }

        // Validate each blank
        var correctCount = 0
        val blankResults = mutableListOf<Boolean>()
        val incorrectBlanks = mutableListOf<String>()

        blanks.forEachIndexed { index, blank ->
            val correctAnswer = blank.get("correctAnswer")?.asText()
                ?: throw IllegalStateException("No correct answer for blank $index")

            // Get acceptable answers (if any)
            val acceptableAnswersNode = blank.get("acceptableAnswers")
            val acceptableAnswers = if (acceptableAnswersNode != null && acceptableAnswersNode.isArray) {
                val answers = mutableListOf<String>()
                acceptableAnswersNode.forEach { answers.add(it.asText()) }
                answers
            } else {
                listOf(correctAnswer)
            }

            // Check user's answer for this blank
            val userAnswer = if (index < userAnswers.size) userAnswers[index] else ""
            val isCorrect = acceptableAnswers.any { it.equals(userAnswer, ignoreCase = true) }

            blankResults.add(isCorrect)
            if (isCorrect) {
                correctCount++
            } else {
                incorrectBlanks.add("Blank ${index + 1}: '$userAnswer' (should be '$correctAnswer')")
            }
        }

        val totalBlanks = blanks.size()
        val isAllCorrect = correctCount == totalBlanks

        // Calculate partial score
        val score = if (totalBlanks == 0) 0.0 else (correctCount.toDouble() / totalBlanks) * 100.0

        // Get additional content for feedback
        val grammarExplanation = content.get("grammarExplanation")?.asText() ?: ""
        val translation = content.get("translation")?.asText()

        // Build feedback message
        val feedback = if (isAllCorrect) {
            buildString {
                append("Perfect! All blanks are correct. ")
                if (grammarExplanation.isNotEmpty()) {
                    append(grammarExplanation)
                    append(" ")
                }
                if (translation != null) {
                    append("Translation: $translation")
                }
            }
        } else {
            buildString {
                append("You got $correctCount out of $totalBlanks correct. ")
                if (incorrectBlanks.isNotEmpty()) {
                    append("Incorrect: ${incorrectBlanks.joinToString(", ")}.")
                }
            }
        }

        return ValidationResult(
            isCorrect = isAllCorrect,
            score = score,
            feedback = feedback,
            correctAnswers = objectMapper.createObjectNode().apply {
                val correctAnswersArray = objectMapper.createArrayNode()
                blanks.forEach { blank ->
                    correctAnswersArray.add(blank.get("correctAnswer")?.asText() ?: "")
                }
                set<JsonNode>("correctAnswers", correctAnswersArray)

                // Add blankResults for frontend to show which blanks were correct
                val blankResultsArray = objectMapper.createArrayNode()
                blankResults.forEach { blankResultsArray.add(it) }
                set<JsonNode>("blankResults", blankResultsArray)
            }
        )
    }

    // Sentence Scramble validation
    private fun validateSentenceScramble(content: JsonNode, userResponses: JsonNode): ValidationResult {
        // Extract user's ordered words
        val userOrderedWords = userResponses.get("orderedWords")
        if (userOrderedWords == null || !userOrderedWords.isArray) {
            return ValidationResult(
                isCorrect = false,
                score = 0.0,
                feedback = "No answer provided",
                correctAnswers = null
            )
        }

        // Get the correct order of words
        // Check for "correctOrder" array first (new format), fall back to "sentence" (old format)
        val correctWordsList = if (content.has("correctOrder") && content.get("correctOrder").isArray) {
            content.get("correctOrder").map { it.asText() }
        } else if (content.has("sentence")) {
            // Old format: parse sentence string
            val correctSentence = content.get("sentence").asText()
            correctSentence
                .replace(Regex("[?.!,;:]"), "")  // Remove punctuation
                .trim()
                .split(Regex("\\s+"))  // Split by whitespace
                .filter { it.isNotEmpty() }
        } else {
            throw IllegalStateException("No correct sentence or correctOrder defined in exercise content")
        }

        // Convert user's ordered words to list
        val userWordsList = mutableListOf<String>()
        userOrderedWords.forEach { userWordsList.add(it.asText()) }

        // Compare the arrays (case-insensitive to handle capitalization differences)
        val isCorrect = userWordsList.map { it.lowercase() } == correctWordsList.map { it.lowercase() }

        // Reconstruct the correct sentence for display
        val correctSentenceDisplay = correctWordsList.joinToString(" ")

        // Get additional content for feedback
        val translation = content.get("translation")?.asText()
        val grammarExplanation = content.get("grammarExplanation")?.asText() ?: ""

        // Build feedback message
        val feedback = if (isCorrect) {
            buildString {
                append("Correct! The sentence is: \"$correctSentenceDisplay\"")
                if (translation != null) {
                    append(" Translation: $translation")
                }
                if (grammarExplanation.isNotEmpty()) {
                    append(" ")
                    append(grammarExplanation)
                }
            }
        } else {
            buildString {
                append("Incorrect. The correct order is: \"$correctSentenceDisplay\"")
                if (grammarExplanation.isNotEmpty()) {
                    append(" ")
                    append(grammarExplanation)
                }
            }
        }

        return ValidationResult(
            isCorrect = isCorrect,
            score = if (isCorrect) 100.0 else 0.0,
            feedback = feedback,
            correctAnswers = objectMapper.createObjectNode().apply {
                val correctWordsArray = objectMapper.createArrayNode()
                correctWordsList.forEach { correctWordsArray.add(it) }
                set<JsonNode>("correctWords", correctWordsArray)
                put("correctSentence", correctSentenceDisplay)
            }
        )
    }

    // Matching validation
    private fun validateMatching(content: JsonNode, userResponses: JsonNode): ValidationResult {
        // Extract user's matches
        val userMatches = userResponses.get("matches")
        if (userMatches == null || !userMatches.isObject) {
            return ValidationResult(
                isCorrect = false,
                score = 0.0,
                feedback = "No matches provided",
                correctAnswers = null
            )
        }

        // Extract correct pairs
        val pairs = content.get("pairs")
            ?: throw IllegalStateException("No pairs defined in exercise content")

        if (!pairs.isArray) {
            throw IllegalStateException("Pairs must be an array")
        }

        // Build map of correct matches
        val correctMatches = mutableMapOf<String, String>()
        pairs.forEach { pair ->
            val left = pair.get("left")?.asText()
            val right = pair.get("right")?.asText()
            if (left != null && right != null) {
                correctMatches[left] = right
            }
        }

        // Count correct and incorrect matches
        var correctCount = 0
        var incorrectCount = 0
        val incorrectPairs = mutableListOf<String>()

        userMatches.fields().forEach { (left, right) ->
            val userRight = right.asText()
            val correctRight = correctMatches[left]

            if (correctRight != null && correctRight == userRight) {
                correctCount++
            } else {
                incorrectCount++
                incorrectPairs.add("$left → $userRight (should be $correctRight)")
            }
        }

        // Check if all matches are provided
        val missingMatches = correctMatches.keys.filter { left ->
            !userMatches.has(left)
        }

        val isCorrect = correctCount == correctMatches.size && incorrectCount == 0 && missingMatches.isEmpty()

        // Calculate partial score
        val score = if (correctMatches.isEmpty()) 0.0 else (correctCount.toDouble() / correctMatches.size) * 100.0

        // Build feedback message
        val feedback = if (isCorrect) {
            "Perfect! All matches are correct."
        } else {
            buildString {
                append("You got $correctCount out of ${correctMatches.size} correct. ")
                if (incorrectPairs.isNotEmpty()) {
                    append("Incorrect matches: ${incorrectPairs.joinToString(", ")}. ")
                }
                if (missingMatches.isNotEmpty()) {
                    append("Missing matches for: ${missingMatches.joinToString(", ")}.")
                }
            }
        }

        return ValidationResult(
            isCorrect = isCorrect,
            score = score,
            feedback = feedback,
            correctAnswers = objectMapper.createObjectNode().apply {
                val correctMatchesNode = objectMapper.createObjectNode()
                correctMatches.forEach { (left, right) ->
                    correctMatchesNode.put(left, right)
                }
                set<JsonNode>("correctMatches", correctMatchesNode)
            }
        )
    }

    private fun validateListening(content: JsonNode, userResponses: JsonNode): ValidationResult {
        // Get question type (multiple_choice or text_input)
        val questionType = content.get("questionType")?.asText() ?: "multiple_choice"

        return when (questionType) {
            "multiple_choice" -> validateListeningMultipleChoice(content, userResponses)
            "text_input" -> validateListeningTextInput(content, userResponses)
            else -> throw IllegalStateException("Invalid question type: $questionType")
        }
    }

    private fun validateListeningMultipleChoice(content: JsonNode, userResponses: JsonNode): ValidationResult {
        // Extract user's selected option
        val selectedOption = userResponses.get("selectedOption")?.asText()
            ?: return ValidationResult(
                isCorrect = false,
                score = 0.0,
                feedback = "No option selected",
                correctAnswers = null
            )

        // Get the correct answer - can be specified directly or via isCorrect in options
        var correctOptionId: String? = content.get("correctAnswer")?.asText()

        // If not found via correctAnswer field, check for isCorrect in options
        if (correctOptionId == null) {
            val options = content.get("options")
            if (options == null) {
                logger.error("No options found in listening exercise content")
                throw IllegalStateException("No options defined in exercise content")
            }

            options.forEach { option ->
                if (option.get("isCorrect")?.asBoolean() == true) {
                    correctOptionId = option.get("id")?.asText()
                }
            }
        }

        if (correctOptionId == null) {
            logger.error("No correct answer found in content: ${content.toString()}")
            throw IllegalStateException("No correct answer defined in exercise content")
        }

        logger.debug("Found correct option: $correctOptionId")

        // Check if user's answer is correct
        val isCorrect = selectedOption == correctOptionId

        // Get additional content for feedback
        val explanation = content.get("explanation")?.asText() ?: ""
        val transcript = content.get("transcript")?.asText()

        // Build feedback message
        val feedback = if (isCorrect) {
            buildString {
                append("Correct! ")
                if (explanation.isNotEmpty()) {
                    append(explanation)
                }
                if (transcript != null) {
                    append(" Transcript: \"$transcript\"")
                }
            }
        } else {
            buildString {
                append("Incorrect. ")
                if (explanation.isNotEmpty()) {
                    append(explanation)
                    append(" ")
                }
                if (transcript != null) {
                    append("Transcript: \"$transcript\"")
                }
            }
        }

        return ValidationResult(
            isCorrect = isCorrect,
            score = if (isCorrect) 100.0 else 0.0,
            feedback = feedback,
            correctAnswers = objectMapper.createObjectNode().apply {
                put("correctAnswer", correctOptionId)
                if (transcript != null) {
                    put("transcript", transcript)
                }
            }
        )
    }

    private fun validateListeningTextInput(content: JsonNode, userResponses: JsonNode): ValidationResult {
        // Extract user's answer
        val userAnswer = userResponses.get("answer")?.asText()?.trim()
            ?: return ValidationResult(
                isCorrect = false,
                score = 0.0,
                feedback = "No answer provided",
                correctAnswers = null
            )

        // Get correct answer(s) - could be a single string or array of acceptable answers
        val correctAnswerNode = content.get("correctAnswer")
            ?: throw IllegalStateException("No correct answer defined in exercise content")

        val acceptableAnswers = if (correctAnswerNode.isArray) {
            val answers = mutableListOf<String>()
            correctAnswerNode.forEach { answers.add(it.asText()) }
            answers
        } else {
            listOf(correctAnswerNode.asText())
        }

        // Check if user's answer matches any acceptable answer (case-insensitive)
        val isCorrect = acceptableAnswers.any { it.equals(userAnswer, ignoreCase = true) }

        // Get additional content for feedback
        val explanation = content.get("explanation")?.asText() ?: ""
        val transcript = content.get("transcript")?.asText()

        // Build feedback message
        val feedback = if (isCorrect) {
            buildString {
                append("Correct! ")
                if (explanation.isNotEmpty()) {
                    append(explanation)
                }
                if (transcript != null) {
                    append(" Transcript: \"$transcript\"")
                }
            }
        } else {
            buildString {
                append("Incorrect. The correct answer is '${acceptableAnswers.first()}'. ")
                if (explanation.isNotEmpty()) {
                    append(explanation)
                    append(" ")
                }
                if (transcript != null) {
                    append("Transcript: \"$transcript\"")
                }
            }
        }

        return ValidationResult(
            isCorrect = isCorrect,
            score = if (isCorrect) 100.0 else 0.0,
            feedback = feedback,
            correctAnswers = objectMapper.createObjectNode().apply {
                put("correctAnswer", acceptableAnswers.first())
                if (transcript != null) {
                    put("transcript", transcript)
                }
            }
        )
    }

    private fun validateClozeReading(content: JsonNode, userResponses: JsonNode): ValidationResult {
        // Extract user's answers
        val userAnswers = userResponses.get("answers")
        if (userAnswers == null || !userAnswers.isObject) {
            return ValidationResult(
                isCorrect = false,
                score = 0.0,
                feedback = "No answers provided",
                correctAnswers = null
            )
        }

        // Extract blanks definitions
        val blanks = content.get("blanks")
            ?: throw IllegalStateException("No blanks defined in exercise content")

        if (!blanks.isArray) {
            throw IllegalStateException("Blanks must be an array")
        }

        // Build map of correct answers for each blank
        val correctAnswers = mutableMapOf<String, List<String>>()
        blanks.forEach { blank ->
            val blankId = blank.get("id")?.asText()
            val correctAnswerNode = blank.get("correctAnswer")

            if (blankId != null && correctAnswerNode != null) {
                val acceptableAnswers = if (correctAnswerNode.isArray) {
                    val answers = mutableListOf<String>()
                    correctAnswerNode.forEach { answers.add(it.asText()) }
                    answers
                } else {
                    listOf(correctAnswerNode.asText())
                }
                correctAnswers[blankId] = acceptableAnswers
            }
        }

        // Count correct and incorrect answers
        var correctCount = 0
        var incorrectCount = 0
        val incorrectBlanks = mutableListOf<String>()

        correctAnswers.forEach { (blankId, acceptableAnswers) ->
            val userAnswer = userAnswers.get(blankId)?.asText()?.trim()

            if (userAnswer != null) {
                val isCorrect = acceptableAnswers.any { it.equals(userAnswer, ignoreCase = true) }
                if (isCorrect) {
                    correctCount++
                } else {
                    incorrectCount++
                    incorrectBlanks.add("Blank $blankId: '$userAnswer' (should be '${acceptableAnswers.first()}')")
                }
            } else {
                incorrectCount++
                incorrectBlanks.add("Blank $blankId: not answered")
            }
        }

        val totalBlanks = correctAnswers.size
        val isCorrect = correctCount == totalBlanks && incorrectCount == 0

        // Calculate partial score
        val score = if (totalBlanks == 0) 0.0 else (correctCount.toDouble() / totalBlanks) * 100.0

        // Build feedback message
        val feedback = if (isCorrect) {
            "Perfect! All blanks are correct."
        } else {
            buildString {
                append("You got $correctCount out of $totalBlanks correct. ")
                if (incorrectBlanks.isNotEmpty()) {
                    append("Incorrect: ${incorrectBlanks.joinToString(", ")}.")
                }
            }
        }

        return ValidationResult(
            isCorrect = isCorrect,
            score = score,
            feedback = feedback,
            correctAnswers = objectMapper.createObjectNode().apply {
                val correctAnswersNode = objectMapper.createObjectNode()
                correctAnswers.forEach { (blankId, acceptableAnswers) ->
                    correctAnswersNode.put(blankId, acceptableAnswers.first())
                }
                set<JsonNode>("correctAnswers", correctAnswersNode)
            }
        )
    }
}

class UnsupportedExerciseTypeException(message: String) : RuntimeException(message)
