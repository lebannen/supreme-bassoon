package com.vocabee.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExerciseValidationServiceTest {

    private lateinit var validationService: ExerciseValidationService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        validationService = ExerciseValidationService()
        objectMapper = ObjectMapper()
    }

    @Nested
    @DisplayName("Multiple Choice Validation")
    inner class MultipleChoiceValidation {

        @Test
        @DisplayName("Should validate correct answer")
        fun shouldValidateCorrectAnswer() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "question": {
                        "type": "text",
                        "content": "parler"
                    },
                    "options": [
                        { "id": "a", "text": "to speak", "isCorrect": true },
                        { "id": "b", "text": "to eat", "isCorrect": false },
                        { "id": "c", "text": "to sleep", "isCorrect": false },
                        { "id": "d", "text": "to work", "isCorrect": false }
                    ],
                    "explanation": "Parler means 'to speak' in French."
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "selectedOption": "a"
                }
            """)

            // When
            val result = validationService.validate("multiple_choice", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
            assertTrue(result.feedback.contains("Correct"))
        }

        @Test
        @DisplayName("Should validate incorrect answer")
        fun shouldValidateIncorrectAnswer() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "question": {
                        "type": "text",
                        "content": "parler"
                    },
                    "options": [
                        { "id": "a", "text": "to speak", "isCorrect": true },
                        { "id": "b", "text": "to eat", "isCorrect": false },
                        { "id": "c", "text": "to sleep", "isCorrect": false },
                        { "id": "d", "text": "to work", "isCorrect": false }
                    ],
                    "explanation": "Parler means 'to speak' in French."
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "selectedOption": "b"
                }
            """)

            // When
            val result = validationService.validate("multiple_choice", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertTrue(result.feedback.contains("Incorrect"))
        }

        @Test
        @DisplayName("Should handle missing selection")
        fun shouldHandleMissingSelection() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "question": {
                        "type": "text",
                        "content": "parler"
                    },
                    "options": [
                        { "id": "a", "text": "to speak", "isCorrect": true }
                    ],
                    "explanation": "Parler means 'to speak' in French."
                }
            """)

            val userResponses = objectMapper.readTree("{}")

            // When
            val result = validationService.validate("multiple_choice", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
        }
    }

    @Nested
    @DisplayName("Fill in the Blank Validation")
    inner class FillInBlankValidation {

        @Test
        @DisplayName("Should validate correct answer with exact match")
        fun shouldValidateCorrectAnswerExactMatch() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "sentence": "Je ____ français.",
                    "blankIndex": 1,
                    "verb": "parler",
                    "subject": "je",
                    "tense": "present",
                    "correctAnswer": "parle",
                    "options": ["parle", "parles", "parlons", "parlent"],
                    "translation": "I speak French.",
                    "grammarExplanation": "With 'je', use the first person singular: parle"
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answer": "parle"
                }
            """)

            // When
            val result = validationService.validate("fill_in_blank", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
            assertTrue(result.feedback.contains("Correct"))
            assertTrue(result.feedback.contains("Translation: I speak French."))
        }

        @Test
        @DisplayName("Should validate correct answer with case insensitive match")
        fun shouldValidateCaseInsensitiveAnswer() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "sentence": "Je ____ français.",
                    "correctAnswer": "parle",
                    "grammarExplanation": "With 'je', use the first person singular: parle",
                    "translation": "I speak French."
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answer": "PARLE"
                }
            """)

            // When
            val result = validationService.validate("fill_in_blank", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
        }

        @Test
        @DisplayName("Should trim whitespace from answer")
        fun shouldTrimWhitespaceFromAnswer() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "sentence": "Je ____ français.",
                    "correctAnswer": "parle",
                    "grammarExplanation": "With 'je', use the first person singular: parle"
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answer": "  parle  "
                }
            """)

            // When
            val result = validationService.validate("fill_in_blank", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
        }

        @Test
        @DisplayName("Should validate incorrect answer")
        fun shouldValidateIncorrectAnswer() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "sentence": "Je ____ français.",
                    "correctAnswer": "parle",
                    "grammarExplanation": "With 'je', use the first person singular: parle"
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answer": "parles"
                }
            """)

            // When
            val result = validationService.validate("fill_in_blank", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertTrue(result.feedback.contains("Incorrect"))
            assertTrue(result.feedback.contains("The correct answer is 'parle'"))
            assertTrue(result.feedback.contains("first person singular"))
        }

        @Test
        @DisplayName("Should handle missing answer")
        fun shouldHandleMissingAnswer() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "sentence": "Je ____ français.",
                    "correctAnswer": "parle"
                }
            """)

            val userResponses = objectMapper.readTree("{}")

            // When
            val result = validationService.validate("fill_in_blank", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertTrue(result.feedback.contains("No answer provided"))
        }

        @Test
        @DisplayName("Should return correct answer in result")
        fun shouldReturnCorrectAnswerInResult() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "sentence": "Je ____ français.",
                    "correctAnswer": "parle"
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answer": "wrong"
                }
            """)

            // When
            val result = validationService.validate("fill_in_blank", content, userResponses)

            // Then
            assertNotNull(result.correctAnswers)
            assertEquals("parle", result.correctAnswers?.get("correctAnswer")?.asText())
        }

        @Test
        @DisplayName("Should throw exception when correct answer is missing from content")
        fun shouldThrowExceptionWhenCorrectAnswerMissing() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "sentence": "Je ____ français."
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answer": "parle"
                }
            """)

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                validationService.validate("fill_in_blank", content, userResponses)
            }
        }
    }

    @Nested
    @DisplayName("Sentence Scramble Validation")
    inner class SentenceScrambleValidation {

        @Test
        @DisplayName("Should validate correct word order")
        fun shouldValidateCorrectWordOrder() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "words": ["Je", "parle", "français"],
                    "sentence": "Je parle français",
                    "translation": "I speak French",
                    "grammarExplanation": "Basic subject-verb-object structure."
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "orderedWords": ["Je", "parle", "français"]
                }
            """)

            // When
            val result = validationService.validate("sentence_scramble", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
            assertTrue(result.feedback.contains("Correct"))
            assertTrue(result.feedback.contains("Je parle français"))
        }

        @Test
        @DisplayName("Should validate incorrect word order")
        fun shouldValidateIncorrectWordOrder() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "words": ["Je", "parle", "français"],
                    "sentence": "Je parle français",
                    "grammarExplanation": "Basic subject-verb-object structure."
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "orderedWords": ["parle", "Je", "français"]
                }
            """)

            // When
            val result = validationService.validate("sentence_scramble", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertTrue(result.feedback.contains("Incorrect"))
            assertTrue(result.feedback.contains("Je parle français"))
        }

        @Test
        @DisplayName("Should handle missing user response")
        fun shouldHandleMissingUserResponse() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "words": ["Je", "parle", "français"],
                    "sentence": "Je parle français"
                }
            """)

            val userResponses = objectMapper.readTree("{}")

            // When
            val result = validationService.validate("sentence_scramble", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertTrue(result.feedback.contains("No answer provided"))
        }

        @Test
        @DisplayName("Should return correct answers in result")
        fun shouldReturnCorrectAnswersInResult() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "words": ["Je", "parle", "français"],
                    "sentence": "Je parle français"
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "orderedWords": ["parle", "français", "Je"]
                }
            """)

            // When
            val result = validationService.validate("sentence_scramble", content, userResponses)

            // Then
            assertNotNull(result.correctAnswers)
            assertEquals("Je parle français", result.correctAnswers?.get("correctSentence")?.asText())
            assertNotNull(result.correctAnswers?.get("correctWords"))
        }

        @Test
        @DisplayName("Should throw exception when correct answer is missing")
        fun shouldThrowExceptionWhenCorrectAnswerIsMissing() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "words": ["Je", "parle", "français"]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "orderedWords": ["Je", "parle", "français"]
                }
            """)

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                validationService.validate("sentence_scramble", content, userResponses)
            }
        }
    }

    @Nested
    @DisplayName("Matching Validation")
    inner class MatchingValidation {

        @Test
        @DisplayName("Should validate all correct matches")
        fun shouldValidateAllCorrectMatches() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "pairs": [
                        {"left": "le chat", "right": "the cat"},
                        {"left": "le chien", "right": "the dog"},
                        {"left": "la maison", "right": "the house"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "matches": {
                        "le chat": "the cat",
                        "le chien": "the dog",
                        "la maison": "the house"
                    }
                }
            """)

            // When
            val result = validationService.validate("matching", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
            assertTrue(result.feedback.contains("Perfect!"))
        }

        @Test
        @DisplayName("Should validate partial correct matches")
        fun shouldValidatePartialCorrectMatches() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "pairs": [
                        {"left": "le chat", "right": "the cat"},
                        {"left": "le chien", "right": "the dog"},
                        {"left": "la maison", "right": "the house"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "matches": {
                        "le chat": "the cat",
                        "le chien": "the house",
                        "la maison": "the dog"
                    }
                }
            """)

            // When
            val result = validationService.validate("matching", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(33.33, result.score, 0.1)
            assertTrue(result.feedback.contains("1 out of 3 correct"))
        }

        @Test
        @DisplayName("Should validate all incorrect matches")
        fun shouldValidateAllIncorrectMatches() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "pairs": [
                        {"left": "le chat", "right": "the cat"},
                        {"left": "le chien", "right": "the dog"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "matches": {
                        "le chat": "the dog",
                        "le chien": "the cat"
                    }
                }
            """)

            // When
            val result = validationService.validate("matching", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertTrue(result.feedback.contains("0 out of 2 correct"))
        }

        @Test
        @DisplayName("Should handle missing matches")
        fun shouldHandleMissingMatches() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "pairs": [
                        {"left": "le chat", "right": "the cat"},
                        {"left": "le chien", "right": "the dog"},
                        {"left": "la maison", "right": "the house"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "matches": {
                        "le chat": "the cat"
                    }
                }
            """)

            // When
            val result = validationService.validate("matching", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(33.33, result.score, 0.1)
            assertTrue(result.feedback.contains("Missing matches for"))
        }

        @Test
        @DisplayName("Should handle empty matches")
        fun shouldHandleEmptyMatches() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "pairs": [
                        {"left": "le chat", "right": "the cat"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "matches": {}
                }
            """)

            // When
            val result = validationService.validate("matching", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertTrue(result.feedback.contains("0 out of 1 correct"))
        }

        @Test
        @DisplayName("Should handle no matches provided")
        fun shouldHandleNoMatchesProvided() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "pairs": [
                        {"left": "le chat", "right": "the cat"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {}
            """)

            // When
            val result = validationService.validate("matching", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertEquals("No matches provided", result.feedback)
        }

        @Test
        @DisplayName("Should throw exception when pairs is missing")
        fun shouldThrowExceptionWhenPairsIsMissing() {
            // Given
            val content = objectMapper.readTree("""
                {}
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "matches": {}
                }
            """)

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                validationService.validate("matching", content, userResponses)
            }
        }
    }

    @Nested
    @DisplayName("Listening Validation")
    inner class ListeningValidation {

        @Test
        @DisplayName("Should validate correct multiple choice answer")
        fun shouldValidateCorrectMultipleChoiceAnswer() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "audioUrl": "/audio/bonjour.mp3",
                    "question": "What greeting did you hear?",
                    "questionType": "multiple_choice",
                    "options": [
                        {"id": "a", "text": "Bonjour"},
                        {"id": "b", "text": "Bonsoir"},
                        {"id": "c", "text": "Au revoir"},
                        {"id": "d", "text": "Salut"}
                    ],
                    "correctAnswer": "a",
                    "transcript": "Bonjour",
                    "explanation": "The speaker said 'Bonjour', which means 'Hello' or 'Good day'."
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "selectedOption": "a"
                }
            """)

            // When
            val result = validationService.validate("listening", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
            assertTrue(result.feedback.contains("Correct"))
            assertTrue(result.feedback.contains("Transcript"))
        }

        @Test
        @DisplayName("Should validate incorrect multiple choice answer")
        fun shouldValidateIncorrectMultipleChoiceAnswer() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "audioUrl": "/audio/bonjour.mp3",
                    "question": "What greeting did you hear?",
                    "questionType": "multiple_choice",
                    "options": [
                        {"id": "a", "text": "Bonjour"},
                        {"id": "b", "text": "Bonsoir"},
                        {"id": "c", "text": "Au revoir"}
                    ],
                    "correctAnswer": "a",
                    "transcript": "Bonjour",
                    "explanation": "The speaker said 'Bonjour', not 'Bonsoir'."
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "selectedOption": "b"
                }
            """)

            // When
            val result = validationService.validate("listening", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertTrue(result.feedback.contains("Incorrect"))
        }

        @Test
        @DisplayName("Should validate correct text input answer")
        fun shouldValidateCorrectTextInputAnswer() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "audioUrl": "/audio/word.mp3",
                    "question": "Type what you heard:",
                    "questionType": "text_input",
                    "correctAnswer": "bonjour",
                    "transcript": "Bonjour",
                    "explanation": "The word is 'bonjour', meaning 'hello'."
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answer": "bonjour"
                }
            """)

            // When
            val result = validationService.validate("listening", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
            assertTrue(result.feedback.contains("Correct"))
        }

        @Test
        @DisplayName("Should validate text input case-insensitively")
        fun shouldValidateTextInputCaseInsensitively() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "audioUrl": "/audio/word.mp3",
                    "question": "Type what you heard:",
                    "questionType": "text_input",
                    "correctAnswer": "bonjour",
                    "transcript": "Bonjour"
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answer": "BONJOUR"
                }
            """)

            // When
            val result = validationService.validate("listening", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
        }

        @Test
        @DisplayName("Should validate incorrect text input answer")
        fun shouldValidateIncorrectTextInputAnswer() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "audioUrl": "/audio/word.mp3",
                    "question": "Type what you heard:",
                    "questionType": "text_input",
                    "correctAnswer": "bonjour",
                    "transcript": "Bonjour",
                    "explanation": "The correct word is 'bonjour'."
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answer": "bonsoir"
                }
            """)

            // When
            val result = validationService.validate("listening", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertTrue(result.feedback.contains("Incorrect"))
            assertTrue(result.feedback.contains("bonjour"))
        }

        @Test
        @DisplayName("Should handle multiple acceptable text answers")
        fun shouldHandleMultipleAcceptableTextAnswers() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "audioUrl": "/audio/word.mp3",
                    "question": "Type what you heard:",
                    "questionType": "text_input",
                    "correctAnswer": ["hi", "hello", "hey"],
                    "transcript": "Hi"
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answer": "hello"
                }
            """)

            // When
            val result = validationService.validate("listening", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
        }

        @Test
        @DisplayName("Should handle missing selection for multiple choice")
        fun shouldHandleMissingSelectionForMultipleChoice() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "audioUrl": "/audio/word.mp3",
                    "question": "What did you hear?",
                    "questionType": "multiple_choice",
                    "correctAnswer": "a"
                }
            """)

            val userResponses = objectMapper.readTree("""
                {}
            """)

            // When
            val result = validationService.validate("listening", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertEquals("No option selected", result.feedback)
        }

        @Test
        @DisplayName("Should handle missing answer for text input")
        fun shouldHandleMissingAnswerForTextInput() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "audioUrl": "/audio/word.mp3",
                    "question": "Type what you heard:",
                    "questionType": "text_input",
                    "correctAnswer": "bonjour"
                }
            """)

            val userResponses = objectMapper.readTree("""
                {}
            """)

            // When
            val result = validationService.validate("listening", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertEquals("No answer provided", result.feedback)
        }

        @Test
        @DisplayName("Should default to multiple choice when questionType is missing")
        fun shouldDefaultToMultipleChoiceWhenQuestionTypeIsMissing() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "audioUrl": "/audio/word.mp3",
                    "question": "What did you hear?",
                    "correctAnswer": "a"
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "selectedOption": "a"
                }
            """)

            // When
            val result = validationService.validate("listening", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
        }
    }

    @Nested
    @DisplayName("Cloze Reading Validation")
    inner class ClozeReadingValidation {

        @Test
        @DisplayName("Should validate all correct answers")
        fun shouldValidateAllCorrectAnswers() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "text": "Je ___1___ au marché. J'___2___ des pommes et du pain. C'___3___ délicieux.",
                    "blanks": [
                        {"id": "1", "correctAnswer": "vais"},
                        {"id": "2", "correctAnswer": "achète"},
                        {"id": "3", "correctAnswer": "est"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answers": {
                        "1": "vais",
                        "2": "achète",
                        "3": "est"
                    }
                }
            """)

            // When
            val result = validationService.validate("cloze_reading", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
            assertTrue(result.feedback.contains("Perfect!"))
        }

        @Test
        @DisplayName("Should validate with case-insensitive matching")
        fun shouldValidateWithCaseInsensitiveMatching() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "text": "Bonjour, je m'appelle ___1___.",
                    "blanks": [
                        {"id": "1", "correctAnswer": "Marie"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answers": {
                        "1": "marie"
                    }
                }
            """)

            // When
            val result = validationService.validate("cloze_reading", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
        }

        @Test
        @DisplayName("Should validate partial correct answers")
        fun shouldValidatePartialCorrectAnswers() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "text": "Il ___1___ un livre. Le livre ___2___ intéressant.",
                    "blanks": [
                        {"id": "1", "correctAnswer": "lit"},
                        {"id": "2", "correctAnswer": "est"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answers": {
                        "1": "lit",
                        "2": "était"
                    }
                }
            """)

            // When
            val result = validationService.validate("cloze_reading", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(50.0, result.score)
            assertTrue(result.feedback.contains("1 out of 2 correct"))
        }

        @Test
        @DisplayName("Should handle multiple acceptable answers")
        fun shouldHandleMultipleAcceptableAnswers() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "text": "Je ___1___ content.",
                    "blanks": [
                        {"id": "1", "correctAnswer": ["suis", "me sens"]}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answers": {
                        "1": "me sens"
                    }
                }
            """)

            // When
            val result = validationService.validate("cloze_reading", content, userResponses)

            // Then
            assertTrue(result.isCorrect)
            assertEquals(100.0, result.score)
        }

        @Test
        @DisplayName("Should handle missing answers")
        fun shouldHandleMissingAnswers() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "text": "Je ___1___ français. Tu ___2___ anglais.",
                    "blanks": [
                        {"id": "1", "correctAnswer": "parle"},
                        {"id": "2", "correctAnswer": "parles"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answers": {
                        "1": "parle"
                    }
                }
            """)

            // When
            val result = validationService.validate("cloze_reading", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(50.0, result.score)
            assertTrue(result.feedback.contains("not answered"))
        }

        @Test
        @DisplayName("Should handle all incorrect answers")
        fun shouldHandleAllIncorrectAnswers() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "text": "Elle ___1___ à Paris. Nous ___2___ à Lyon.",
                    "blanks": [
                        {"id": "1", "correctAnswer": "habite"},
                        {"id": "2", "correctAnswer": "habitons"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answers": {
                        "1": "travaille",
                        "2": "travaillons"
                    }
                }
            """)

            // When
            val result = validationService.validate("cloze_reading", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertTrue(result.feedback.contains("0 out of 2 correct"))
        }

        @Test
        @DisplayName("Should handle empty answers object")
        fun shouldHandleEmptyAnswersObject() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "text": "Je ___1___ français.",
                    "blanks": [
                        {"id": "1", "correctAnswer": "parle"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answers": {}
                }
            """)

            // When
            val result = validationService.validate("cloze_reading", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertTrue(result.feedback.contains("not answered"))
        }

        @Test
        @DisplayName("Should handle no answers provided")
        fun shouldHandleNoAnswersProvided() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "text": "Je ___1___ français.",
                    "blanks": [
                        {"id": "1", "correctAnswer": "parle"}
                    ]
                }
            """)

            val userResponses = objectMapper.readTree("""
                {}
            """)

            // When
            val result = validationService.validate("cloze_reading", content, userResponses)

            // Then
            assertFalse(result.isCorrect)
            assertEquals(0.0, result.score)
            assertEquals("No answers provided", result.feedback)
        }

        @Test
        @DisplayName("Should throw exception when blanks is missing")
        fun shouldThrowExceptionWhenBlanksIsMissing() {
            // Given
            val content = objectMapper.readTree("""
                {
                    "text": "Some text"
                }
            """)

            val userResponses = objectMapper.readTree("""
                {
                    "answers": {}
                }
            """)

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                validationService.validate("cloze_reading", content, userResponses)
            }
        }
    }

    @Nested
    @DisplayName("Unsupported Exercise Types")
    inner class UnsupportedTypes {

        @Test
        @DisplayName("Should throw exception for unsupported exercise type")
        fun shouldThrowExceptionForUnsupportedType() {
            // Given
            val content = objectMapper.readTree("{}")
            val userResponses = objectMapper.readTree("{}")

            // When & Then
            assertThrows(UnsupportedExerciseTypeException::class.java) {
                validationService.validate("unknown_type", content, userResponses)
            }
        }
    }
}
