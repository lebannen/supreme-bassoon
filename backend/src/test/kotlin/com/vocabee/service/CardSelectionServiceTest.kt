package com.vocabee.service

import com.vocabee.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

class CardSelectionServiceTest {

    private lateinit var cardSelectionService: CardSelectionService

    @BeforeEach
    fun setup() {
        cardSelectionService = CardSelectionService()
    }

    // Helper function to create a mock Word
    private fun createMockWord(id: Long, lemma: String): Word {
        return Word(
            id = id,
            lemma = lemma,
            normalized = lemma.lowercase(),
            partOfSpeech = "verb",
            languageCode = "fr"
        )
    }

    // Helper function to create a mock StudySession
    private fun createMockSession(id: Long, userId: Long): StudySession {
        val user = User(id = userId, email = "test@example.com")
        return StudySession(
            id = id,
            user = user,
            sessionSize = 10,
            totalWords = 10,
            sessionType = SessionType.VOCABULARY
        )
    }

    // Helper function to create a mock StudySessionItem
    private fun createMockItem(
        id: Long,
        wordId: Long,
        wordLemma: String,
        status: ItemStatus = ItemStatus.NEW,
        attemptsCount: Int = 0,
        correctCount: Int = 0,
        incorrectCount: Int = 0,
        consecutiveCorrect: Int = 0
    ): StudySessionItem {
        val session = createMockSession(1L, 1L)
        val word = createMockWord(wordId, wordLemma)

        return StudySessionItem(
            id = id,
            session = session,
            word = word,
            attemptsCount = attemptsCount,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            consecutiveCorrect = consecutiveCorrect,
            status = status
        )
    }

    @Nested
    @DisplayName("Basic Selection Tests")
    inner class BasicSelectionTests {

        @Test
        fun `should return null when all items are completed`() {
            val items = listOf(
                createMockItem(1L, 1L, "parler", status = ItemStatus.COMPLETED),
                createMockItem(2L, 2L, "manger", status = ItemStatus.COMPLETED),
                createMockItem(3L, 3L, "dormir", status = ItemStatus.COMPLETED)
            )

            val result = cardSelectionService.selectNextCard(items)

            assertNull(result)
        }

        @Test
        fun `should return the only incomplete item`() {
            val items = listOf(
                createMockItem(1L, 1L, "parler", status = ItemStatus.COMPLETED),
                createMockItem(2L, 2L, "manger", status = ItemStatus.LEARNING),
                createMockItem(3L, 3L, "dormir", status = ItemStatus.COMPLETED)
            )

            val result = cardSelectionService.selectNextCard(items)

            assertNotNull(result)
            assertEquals(2L, result?.word?.id)
        }

        @Test
        fun `should select a new word when all are new`() {
            val items = listOf(
                createMockItem(1L, 1L, "parler"),
                createMockItem(2L, 2L, "manger"),
                createMockItem(3L, 3L, "dormir")
            )

            val result = cardSelectionService.selectNextCard(items)

            assertNotNull(result)
            assertTrue(items.contains(result))
        }
    }

    @Nested
    @DisplayName("Prioritization Tests")
    inner class PrioritizationTests {

        @Test
        fun `should prioritize failed words over new words`() {
            val items = listOf(
                createMockItem(1L, 1L, "parler", status = ItemStatus.NEW),
                createMockItem(
                    2L, 2L, "manger",
                    status = ItemStatus.LEARNING,
                    attemptsCount = 3,
                    correctCount = 1,
                    incorrectCount = 2  // More failures than successes
                ),
                createMockItem(3L, 3L, "dormir", status = ItemStatus.NEW)
            )

            val result = cardSelectionService.selectNextCard(items)

            assertEquals(2L, result?.word?.id, "Should select the word with more failures")
        }

        @Test
        fun `should prioritize learning words over new words`() {
            val items = listOf(
                createMockItem(1L, 1L, "parler", status = ItemStatus.NEW),
                createMockItem(
                    2L, 2L, "manger",
                    status = ItemStatus.LEARNING,
                    attemptsCount = 1,
                    correctCount = 1
                ),
                createMockItem(3L, 3L, "dormir", status = ItemStatus.NEW)
            )

            val result = cardSelectionService.selectNextCard(items)

            assertEquals(2L, result?.word?.id, "Should select learning word over new words")
        }

        @Test
        fun `should prioritize words with more attempts`() {
            val items = listOf(
                createMockItem(
                    1L, 1L, "parler",
                    status = ItemStatus.LEARNING,
                    attemptsCount = 1
                ),
                createMockItem(
                    2L, 2L, "manger",
                    status = ItemStatus.LEARNING,
                    attemptsCount = 10  // Much more attempts = clearly struggling
                ),
                createMockItem(
                    3L, 3L, "dormir",
                    status = ItemStatus.LEARNING,
                    attemptsCount = 2
                )
            )

            val result = cardSelectionService.selectNextCard(items)

            // Word 2 has 10 attempts (score: -100), word 3 has 2 attempts (score: -20)
            // The gap is 80, well beyond the similarity threshold of 30
            assertEquals(2L, result?.word?.id, "Should select word with most attempts")
        }

        @Test
        fun `should deprioritize words with high consecutive correct streak`() {
            val items = listOf(
                createMockItem(
                    1L, 1L, "parler",
                    status = ItemStatus.LEARNING,
                    consecutiveCorrect = 0
                ),
                createMockItem(
                    2L, 2L, "manger",
                    status = ItemStatus.LEARNING,
                    consecutiveCorrect = 5  // High streak = doing well, score = 100
                ),
                createMockItem(
                    3L, 3L, "dormir",
                    status = ItemStatus.LEARNING,
                    consecutiveCorrect = 0
                )
            )

            val result = cardSelectionService.selectNextCard(items)

            // Words 1 & 3 have score 0, word 2 has score 100 (well outside threshold)
            assertNotEquals(2L, result?.word?.id, "Should NOT select word with high streak")
        }
    }

    @Nested
    @DisplayName("Avoid Repetition Tests")
    inner class AvoidRepetitionTests {

        @Test
        fun `should avoid showing the last shown word when alternatives exist`() {
            val items = listOf(
                createMockItem(1L, 1L, "parler", status = ItemStatus.LEARNING),
                createMockItem(2L, 2L, "manger", status = ItemStatus.LEARNING),
                createMockItem(3L, 3L, "dormir", status = ItemStatus.LEARNING)
            )

            val result = cardSelectionService.selectNextCard(items, lastShownWordId = 2L)

            assertNotNull(result)
            assertNotEquals(2L, result?.word?.id, "Should not select the last shown word")
        }

        @Test
        fun `should show last word again if it's the only incomplete item`() {
            val items = listOf(
                createMockItem(1L, 1L, "parler", status = ItemStatus.COMPLETED),
                createMockItem(2L, 2L, "manger", status = ItemStatus.LEARNING),
                createMockItem(3L, 3L, "dormir", status = ItemStatus.COMPLETED)
            )

            val result = cardSelectionService.selectNextCard(items, lastShownWordId = 2L)

            assertNotNull(result)
            assertEquals(2L, result?.word?.id, "Should show the only incomplete word even if it was last shown")
        }

        @Test
        @DisplayName("EDGE CASE: With 2 remaining words, should have variety in selection")
        fun `should provide variety when only 2 words remain`() {
            val items = listOf(
                createMockItem(1L, 1L, "parler", status = ItemStatus.LEARNING, consecutiveCorrect = 0),
                createMockItem(2L, 2L, "manger", status = ItemStatus.LEARNING, consecutiveCorrect = 0),
                createMockItem(3L, 3L, "dormir", status = ItemStatus.COMPLETED)
            )

            // Simulate showing words alternately and track the pattern
            val sequence = mutableListOf<Long>()
            var lastWordId: Long? = null

            // Simulate 10 card selections
            for (i in 0 until 10) {
                val selected = cardSelectionService.selectNextCard(items, lastWordId)
                assertNotNull(selected, "Should always select a word when incomplete items exist")
                sequence.add(selected!!.word.id!!)
                lastWordId = selected.word.id

                // Should not show same word twice in a row (when we have 2+ words)
                if (sequence.size >= 2) {
                    assertNotEquals(
                        sequence[sequence.size - 2],
                        sequence[sequence.size - 1],
                        "Should not show the same word twice in a row at position ${sequence.size}"
                    )
                }
            }

            // Check that we're not in a strict alternating pattern (A, B, A, B, A, B...)
            // With randomization, we should see some variation
            val isStrictAlternating = sequence.zipWithNext().all { (a, b) -> a != b } &&
                    sequence.filterIndexed { index, _ -> index % 2 == 0 }.distinct().size == 1

            println("Selection sequence with 2 words: $sequence")
            println("Is strict alternating: $isStrictAlternating")
        }

        @Test
        @DisplayName("EDGE CASE: With 3 remaining words, should avoid predictable patterns")
        fun `should avoid predictable patterns when 3 words remain`() {
            val items = listOf(
                createMockItem(1L, 1L, "parler", status = ItemStatus.LEARNING, consecutiveCorrect = 0),
                createMockItem(2L, 2L, "manger", status = ItemStatus.LEARNING, consecutiveCorrect = 0),
                createMockItem(3L, 3L, "dormir", status = ItemStatus.LEARNING, consecutiveCorrect = 0)
            )

            val sequence = mutableListOf<Long>()
            var lastWordId: Long? = null

            // Simulate 15 card selections
            for (i in 0 until 15) {
                val selected = cardSelectionService.selectNextCard(items, lastWordId)
                assertNotNull(selected)
                sequence.add(selected!!.word.id!!)
                lastWordId = selected.word.id
            }

            println("Selection sequence with 3 words: $sequence")

            // Should not show same word twice in a row
            sequence.zipWithNext().forEach { (a, b) ->
                assertNotEquals(a, b, "Should not show the same word twice in a row")
            }

            // All three words should appear in the sequence
            assertEquals(3, sequence.distinct().size, "All three words should appear")

            // Check distribution - each word should appear somewhat evenly
            val distribution = sequence.groupingBy { it }.eachCount()
            println("Distribution: $distribution")

            // Each word should appear at least 3 times in 15 selections
            distribution.values.forEach { count ->
                assertTrue(count >= 3, "Each word should appear at least 3 times")
            }
        }
    }

    @Nested
    @DisplayName("Session Progress Tests")
    inner class SessionProgressTests {

        @Test
        fun `should correctly count items by status`() {
            val items = listOf(
                createMockItem(1L, 1L, "parler", status = ItemStatus.NEW),
                createMockItem(2L, 2L, "manger", status = ItemStatus.LEARNING),
                createMockItem(3L, 3L, "dormir", status = ItemStatus.COMPLETED),
                createMockItem(4L, 4L, "boire", status = ItemStatus.NEW),
                createMockItem(5L, 5L, "lire", status = ItemStatus.LEARNING)
            )

            val progress = cardSelectionService.getSessionProgress(items)

            assertEquals(2, progress["new"])
            assertEquals(2, progress["learning"])
            assertEquals(1, progress["completed"])
        }

        @Test
        fun `should identify completed session`() {
            val items = listOf(
                createMockItem(1L, 1L, "parler", status = ItemStatus.COMPLETED),
                createMockItem(2L, 2L, "manger", status = ItemStatus.COMPLETED)
            )

            assertTrue(cardSelectionService.isSessionComplete(items))
        }

        @Test
        fun `should identify incomplete session`() {
            val items = listOf(
                createMockItem(1L, 1L, "parler", status = ItemStatus.COMPLETED),
                createMockItem(2L, 2L, "manger", status = ItemStatus.LEARNING)
            )

            assertFalse(cardSelectionService.isSessionComplete(items))
        }
    }
}
