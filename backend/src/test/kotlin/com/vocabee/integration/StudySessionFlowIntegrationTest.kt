package com.vocabee.integration

import com.vocabee.web.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.http.HttpStatus
import java.time.Instant

@DisplayName("Study Session Flow Integration Tests")
class StudySessionFlowIntegrationTest : BaseIntegrationTest() {

    private lateinit var testWords: List<com.vocabee.domain.model.Word>

    @BeforeEach
    fun setup() {
        // Create test language
        createTestLanguage("fr", "French")

        // Create test words
        testWords = listOf(
            createTestWord("parler", "fr", "verb"),
            createTestWord("manger", "fr", "verb"),
            createTestWord("dormir", "fr", "verb"),
            createTestWord("boire", "fr", "verb"),
            createTestWord("lire", "fr", "verb")
        )
    }

    @Test
    @DisplayName("Complete study session flow - from start to finish")
    fun `should complete full study session successfully`() {
        // ARRANGE: Create a word set with test words
        val wordSet = createTestWordSet("Test Set A1", "fr", testWords)

        // ACT 1: Start a study session
        val startRequest = StartSessionRequest(
            source = SessionSourceType.WORD_SET,
            wordSetId = wordSet.id,
            sessionSize = 3,  // Study 3 words
            includeNewWords = true
        )

        val startResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/start",
            startRequest,
            SessionResponse::class.java
        )

        // ASSERT: Session created successfully
        assertEquals(HttpStatus.CREATED, startResponse.statusCode)
        assertNotNull(startResponse.body)
        val session = startResponse.body!!
        assertEquals(3, session.totalWords)
        assertEquals(0, session.wordsCompleted)
        assertEquals("ACTIVE", session.status)

        val sessionId = session.sessionId

        // ACT 2: Get the first card
        var cardResponse = authenticatedGet(
            "${baseUrl()}/api/study/sessions/$sessionId/next-card",
            NextCardResponse::class.java
        )

        assertEquals(HttpStatus.OK, cardResponse.statusCode)
        assertNotNull(cardResponse.body)
        var currentCard = cardResponse.body!!

        // Track which words we've seen
        val seenWords = mutableSetOf<Long>()
        val cardAttempts = mutableMapOf<Long, Int>()

        // ACT 3: Study all cards until session complete
        var attempts = 0
        val maxAttempts = 50  // Safety limit

        while (attempts < maxAttempts) {
            attempts++

            // Record which card we're seeing
            val cardId = currentCard.cardId
            val wordId = currentCard.word.id
            seenWords.add(wordId)
            cardAttempts[wordId] = (cardAttempts[wordId] ?: 0) + 1

            // Simulate user studying the card
            // For testing, we'll answer correctly on first 2 attempts, incorrectly on 3rd
            val shouldAnswerCorrectly = cardAttempts[wordId]!! != 3

            val answerRequest = AnswerRequest(
                cardId = cardId,
                correct = shouldAnswerCorrectly,
                responseTimeMs = 2000
            )

            val answerResponse = authenticatedPost(
                "${baseUrl()}/api/study/sessions/$sessionId/answer",
                answerRequest,
                AnswerResponse::class.java
            )

            assertEquals(HttpStatus.OK, answerResponse.statusCode)
            val answer = answerResponse.body!!

            println("Attempt $attempts: Word ${currentCard.word.lemma}, " +
                    "Correct: $shouldAnswerCorrectly, " +
                    "Streak: ${answer.consecutiveCorrect}, " +
                    "Item Complete: ${answer.itemCompleted}, " +
                    "Session Complete: ${answer.sessionCompleted}")

            // Check if session is complete
            if (answer.sessionCompleted) {
                println("Session completed after $attempts attempts!")
                break
            }

            // Get next card
            cardResponse = authenticatedGet(
                "${baseUrl()}/api/study/sessions/$sessionId/next-card",
                NextCardResponse::class.java
            )

            if (cardResponse.statusCode == HttpStatus.NO_CONTENT) {
                // No more cards
                break
            }

            assertEquals(HttpStatus.OK, cardResponse.statusCode)
            currentCard = cardResponse.body!!
        }

        assertTrue(attempts < maxAttempts, "Session should complete within $maxAttempts attempts")

        // ACT 4: Complete the session
        val completeResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/$sessionId/complete",
            null,
            SessionSummaryResponse::class.java
        )

        // ASSERT: Session completed successfully
        assertEquals(HttpStatus.OK, completeResponse.statusCode)
        val summary = completeResponse.body!!

        println("\n=== Session Summary ===")
        println("Total words: ${summary.stats.totalWords}")
        println("New words: ${summary.stats.newWords}")
        println("Total attempts: ${summary.stats.totalAttempts}")
        println("Correct: ${summary.stats.correctAttempts}")
        println("Incorrect: ${summary.stats.incorrectAttempts}")
        println("Accuracy: ${summary.stats.accuracy}%")
        println("Words advanced: ${summary.srsUpdates.wordsAdvanced}")
        println("Words reset: ${summary.srsUpdates.wordsReset}")

        // Verify summary stats
        assertEquals(3, summary.stats.totalWords)
        assertTrue(summary.stats.totalAttempts > 0)
        assertTrue(summary.stats.accuracy > 0)

        // Verify SRS updates were applied
        val totalUpdates = summary.srsUpdates.wordsAdvanced + summary.srsUpdates.wordsReset
        assertEquals(3, totalUpdates, "All 3 words should have SRS updates")

        // ASSERT: Verify SRS intervals were updated in database
        val userVocabs = userVocabularyRepository.findByUserId(testUser.id!!)
        assertEquals(3, userVocabs.size, "Should have 3 words in vocabulary")

        userVocabs.forEach { vocab ->
            assertNotNull(vocab.nextReviewAt, "Next review should be set")
            assertTrue(vocab.reviewCount > 0, "Review count should be incremented")
            assertTrue(vocab.currentIntervalHours >= 20, "Interval should be at least 20 hours")
            println("Word ${vocab.word.lemma}: " +
                    "Next review in ${vocab.currentIntervalHours}h, " +
                    "Consecutive successes: ${vocab.consecutiveSuccesses}, " +
                    "Review count: ${vocab.reviewCount}")
        }
    }

    @Test
    @DisplayName("Should enforce 2 consecutive correct answers requirement")
    fun `should require 2 consecutive correct answers to complete a word`() {
        // ARRANGE
        val wordSet = createTestWordSet("Test Set", "fr", testWords.take(1))

        val startRequest = StartSessionRequest(
            source = SessionSourceType.WORD_SET,
            wordSetId = wordSet.id,
            sessionSize = 1,
            includeNewWords = true
        )

        val startResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/start",
            startRequest,
            SessionResponse::class.java
        )

        val sessionId = startResponse.body!!.sessionId

        // ACT & ASSERT: Test the 2-consecutive-correct requirement
        var cardResponse = authenticatedGet(
            "${baseUrl()}/api/study/sessions/$sessionId/next-card",
            NextCardResponse::class.java
        )
        var card = cardResponse.body!!

        // First attempt: Correct
        var answerResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/$sessionId/answer",
            AnswerRequest(cardId = card.cardId, correct = true),
            AnswerResponse::class.java
        )
        var answer = answerResponse.body!!

        assertEquals(1, answer.consecutiveCorrect)
        assertFalse(answer.itemCompleted, "Should NOT be complete after 1 correct")
        assertFalse(answer.sessionCompleted)

        // Get next card (should be the same word)
        cardResponse = authenticatedGet(
            "${baseUrl()}/api/study/sessions/$sessionId/next-card",
            NextCardResponse::class.java
        )
        card = cardResponse.body!!

        // Second attempt: Incorrect (resets streak)
        answerResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/$sessionId/answer",
            AnswerRequest(cardId = card.cardId, correct = false),
            AnswerResponse::class.java
        )
        answer = answerResponse.body!!

        assertEquals(0, answer.consecutiveCorrect, "Streak should reset to 0")
        assertFalse(answer.itemCompleted)

        // Get next card again
        cardResponse = authenticatedGet(
            "${baseUrl()}/api/study/sessions/$sessionId/next-card",
            NextCardResponse::class.java
        )
        card = cardResponse.body!!

        // Third attempt: Correct
        answerResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/$sessionId/answer",
            AnswerRequest(cardId = card.cardId, correct = true),
            AnswerResponse::class.java
        )
        answer = answerResponse.body!!

        assertEquals(1, answer.consecutiveCorrect)
        assertFalse(answer.itemCompleted)

        // Get next card again
        cardResponse = authenticatedGet(
            "${baseUrl()}/api/study/sessions/$sessionId/next-card",
            NextCardResponse::class.java
        )
        card = cardResponse.body!!

        // Fourth attempt: Correct (should complete now)
        answerResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/$sessionId/answer",
            AnswerRequest(cardId = card.cardId, correct = true),
            AnswerResponse::class.java
        )
        answer = answerResponse.body!!

        assertEquals(2, answer.consecutiveCorrect)
        assertTrue(answer.itemCompleted, "Should be complete after 2 consecutive correct")
        assertTrue(answer.sessionCompleted, "Session should be complete (only 1 word)")
    }

    @Test
    @DisplayName("Should prevent starting multiple active sessions")
    fun `should not allow multiple active sessions`() {
        // ARRANGE
        val wordSet = createTestWordSet("Test Set", "fr", testWords)

        val startRequest = StartSessionRequest(
            source = SessionSourceType.WORD_SET,
            wordSetId = wordSet.id,
            sessionSize = 3,
            includeNewWords = true
        )

        // ACT 1: Start first session
        val firstResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/start",
            startRequest,
            SessionResponse::class.java
        )

        assertEquals(HttpStatus.CREATED, firstResponse.statusCode)

        // ACT 2: Try to start second session
        val secondResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/start",
            startRequest,
            String::class.java
        )

        // ASSERT: Should fail with CONFLICT
        assertEquals(HttpStatus.CONFLICT, secondResponse.statusCode)
    }

    @Test
    @DisplayName("Should allow starting new session after abandoning")
    fun `should allow new session after abandoning previous`() {
        // ARRANGE
        val wordSet = createTestWordSet("Test Set", "fr", testWords)

        val startRequest = StartSessionRequest(
            source = SessionSourceType.WORD_SET,
            wordSetId = wordSet.id,
            sessionSize = 3,
            includeNewWords = true
        )

        // ACT 1: Start and then abandon session
        val firstResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/start",
            startRequest,
            SessionResponse::class.java
        )

        val sessionId = firstResponse.body!!.sessionId

        val abandonResponse = authenticatedDelete(
            "${baseUrl()}/api/study/sessions/$sessionId",
            Void::class.java
        )

        assertEquals(HttpStatus.NO_CONTENT, abandonResponse.statusCode)

        // ACT 2: Start new session
        val secondResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/start",
            startRequest,
            SessionResponse::class.java
        )

        // ASSERT: Should succeed
        assertEquals(HttpStatus.CREATED, secondResponse.statusCode)
        assertNotNull(secondResponse.body)
        assertNotEquals(sessionId, secondResponse.body!!.sessionId)
    }

    @Test
    @DisplayName("Should study from user vocabulary")
    fun `should start session from user vocabulary`() {
        // ARRANGE: Add words to user's vocabulary
        testWords.take(3).forEach { word ->
            createTestUserVocabulary(testUser, word)
        }

        val startRequest = StartSessionRequest(
            source = SessionSourceType.VOCABULARY,
            wordSetId = null,
            sessionSize = 2,
            includeNewWords = true
        )

        // ACT
        val response = authenticatedPost(
            "${baseUrl()}/api/study/sessions/start",
            startRequest,
            SessionResponse::class.java
        )

        // ASSERT
        assertEquals(HttpStatus.CREATED, response.statusCode)
        val session = response.body!!
        assertEquals(2, session.totalWords)
        // Verify progress breakdown adds up to totalWords
        assertEquals(2, session.progress.new + session.progress.learning + session.progress.completed)
    }

    @Test
    @DisplayName("Should study due words only")
    fun `should start session with only due words`() {
        // ARRANGE: Create vocabulary with some due and some not due
        val now = Instant.now()
        val past = now.minusSeconds(3600)  // 1 hour ago
        val future = now.plusSeconds(7200)  // 2 hours from now

        createTestUserVocabulary(testUser, testWords[0], nextReviewAt = past)  // Due
        createTestUserVocabulary(testUser, testWords[1], nextReviewAt = past)  // Due
        createTestUserVocabulary(testUser, testWords[2], nextReviewAt = future)  // Not due yet

        val startRequest = StartSessionRequest(
            source = SessionSourceType.DUE_REVIEW,
            wordSetId = null,
            sessionSize = 0,  // All due words
            includeNewWords = false
        )

        // ACT
        val response = authenticatedPost(
            "${baseUrl()}/api/study/sessions/start",
            startRequest,
            SessionResponse::class.java
        )

        // ASSERT
        assertEquals(HttpStatus.CREATED, response.statusCode)
        val session = response.body!!
        assertEquals(2, session.totalWords, "Should only include 2 due words")
    }
}
