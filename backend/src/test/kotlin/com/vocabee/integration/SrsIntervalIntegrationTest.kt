package com.vocabee.integration

import com.vocabee.web.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.http.HttpStatus
import java.time.Duration
import java.time.Instant

@DisplayName("SRS Interval Calculation Integration Tests")
class SrsIntervalIntegrationTest : BaseIntegrationTest() {

    private lateinit var testWords: List<com.vocabee.domain.model.Word>

    @BeforeEach
    fun setup() {
        createTestLanguage("fr", "French")
        testWords = listOf(
            createTestWord("parler", "fr", "verb"),
            createTestWord("manger", "fr", "verb"),
            createTestWord("dormir", "fr", "verb")
        )
    }

    @Test
    @DisplayName("Should advance interval on successful review")
    fun `should increase interval when word is answered correctly`() {
        // ARRANGE: Create word set and start session
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

        // ACT: Answer correctly twice to complete the word
        repeat(2) {
            val cardResponse = authenticatedGet(
                "${baseUrl()}/api/study/sessions/$sessionId/next-card",
                NextCardResponse::class.java
            )

            val card = cardResponse.body!!

            authenticatedPost(
                "${baseUrl()}/api/study/sessions/$sessionId/answer",
                AnswerRequest(cardId = card.cardId, correct = true),
                AnswerResponse::class.java
            )
        }

        // Complete session
        val summaryResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/$sessionId/complete",
            null,
            SessionSummaryResponse::class.java
        )

        val summary = summaryResponse.body!!

        // ASSERT: Verify interval was advanced
        assertEquals(1, summary.srsUpdates.wordsAdvanced)
        assertEquals(0, summary.srsUpdates.wordsReset)

        // Verify in database
        val vocab = userVocabularyRepository.findByUserId(testUser.id!!).first()
        assertEquals(1, vocab.consecutiveSuccesses)
        assertEquals(40, vocab.currentIntervalHours, "First success should give 40-hour interval")

        assertNotNull(vocab.nextReviewAt)
        val expectedNextReview = Instant.now().plus(Duration.ofHours(40))
        assertTrue(
            Duration.between(vocab.nextReviewAt, expectedNextReview).abs().toMinutes() < 1,
            "Next review should be ~40 hours from now"
        )
    }

    @Test
    @DisplayName("Should reset interval on failed review")
    fun `should reset interval when word is answered incorrectly`() {
        // ARRANGE: Create vocabulary with advanced interval
        val word = testWords[0]
        val vocab = createTestUserVocabulary(
            user = testUser,
            word = word,
            consecutiveSuccesses = 3,
            nextReviewAt = Instant.now().minusSeconds(3600)  // Due now
        )
        vocab.currentIntervalHours = 160  // Advanced interval
        userVocabularyRepository.save(vocab)

        val wordSet = createTestWordSet("Test Set", "fr", listOf(word))

        val startRequest = StartSessionRequest(
            source = SessionSourceType.WORD_SET,
            wordSetId = wordSet.id,
            sessionSize = 1,
            includeNewWords = false
        )

        val startResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/start",
            startRequest,
            SessionResponse::class.java
        )

        val sessionId = startResponse.body!!.sessionId

        // ACT: Answer incorrectly, then correctly twice
        val cardResponse1 = authenticatedGet(
            "${baseUrl()}/api/study/sessions/$sessionId/next-card",
            NextCardResponse::class.java
        )

        // First answer: incorrect
        authenticatedPost(
            "${baseUrl()}/api/study/sessions/$sessionId/answer",
            AnswerRequest(cardId = cardResponse1.body!!.cardId, correct = false),
            AnswerResponse::class.java
        )

        // Then answer correctly twice to complete
        repeat(2) {
            val cardResponse = authenticatedGet(
                "${baseUrl()}/api/study/sessions/$sessionId/next-card",
                NextCardResponse::class.java
            )

            authenticatedPost(
                "${baseUrl()}/api/study/sessions/$sessionId/answer",
                AnswerRequest(cardId = cardResponse.body!!.cardId, correct = true),
                AnswerResponse::class.java
            )
        }

        // Complete session
        val summaryResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/$sessionId/complete",
            null,
            SessionSummaryResponse::class.java
        )

        val summary = summaryResponse.body!!

        // ASSERT: Verify word was counted as advanced (2 correct > 1 incorrect)
        // Even though there was a failure, the word completed successfully overall
        assertEquals(1, summary.srsUpdates.wordsAdvanced, "Should advance (2 correct > 1 incorrect)")
        assertEquals(0, summary.srsUpdates.wordsReset)

        // Verify in database - word advanced from consecutiveSuccesses 3 to 4
        val updatedVocab = userVocabularyRepository.findByUserIdAndWordId(testUser.id!!, word.id!!)!!
        assertEquals(4, updatedVocab.consecutiveSuccesses, "Consecutive successes should increment")
        assertTrue(updatedVocab.currentIntervalHours > 160, "Interval should advance from 160")
    }

    @Test
    @DisplayName("Should progress through interval stages correctly")
    fun `should advance through SRS intervals on repeated success`() {
        // ARRANGE
        val word = testWords[0]
        val wordSet = createTestWordSet("Test Set", "fr", listOf(word))

        // Expected intervals AFTER each session: 40, 80, 160, 320, 640, 720, 720 (capped)
        // (First session moves from default 20 to 40, then doubles each time until cap)
        val expectedIntervals = listOf(40, 80, 160, 320, 640, 720, 720)
        val actualIntervals = mutableListOf<Int>()

        // ACT: Complete multiple sessions with correct answers
        for (sessionNum in 0..6) {
            // Start session
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

            // Answer correctly twice
            repeat(2) {
                val cardResponse = authenticatedGet(
                    "${baseUrl()}/api/study/sessions/$sessionId/next-card",
                    NextCardResponse::class.java
                )

                authenticatedPost(
                    "${baseUrl()}/api/study/sessions/$sessionId/answer",
                    AnswerRequest(cardId = cardResponse.body!!.cardId, correct = true),
                    AnswerResponse::class.java
                )
            }

            // Complete session
            authenticatedPost(
                "${baseUrl()}/api/study/sessions/$sessionId/complete",
                null,
                SessionSummaryResponse::class.java
            )

            // Check interval
            val vocab = userVocabularyRepository.findByUserIdAndWordId(testUser.id!!, word.id!!)!!
            actualIntervals.add(vocab.currentIntervalHours)

            println("Session ${sessionNum + 1}: " +
                    "Consecutive successes: ${vocab.consecutiveSuccesses}, " +
                    "Interval: ${vocab.currentIntervalHours} hours")
        }

        // ASSERT: Verify interval progression
        assertEquals(expectedIntervals.size, actualIntervals.size)
        expectedIntervals.forEachIndexed { index, expected ->
            assertEquals(
                expected,
                actualIntervals[index],
                "Session ${index + 1} should have interval of $expected hours"
            )
        }

        // Verify cap at 720 hours
        val finalVocab = userVocabularyRepository.findByUserIdAndWordId(testUser.id!!, word.id!!)!!
        assertEquals(720, finalVocab.currentIntervalHours, "Should cap at 720 hours (30 days)")
    }

    @Test
    @DisplayName("Should track review count correctly")
    fun `should increment review count on each session`() {
        // ARRANGE
        val word = testWords[0]
        val wordSet = createTestWordSet("Test Set", "fr", listOf(word))

        // ACT: Complete 3 study sessions
        repeat(3) { sessionNum ->
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

            // Answer correctly twice
            repeat(2) {
                val cardResponse = authenticatedGet(
                    "${baseUrl()}/api/study/sessions/$sessionId/next-card",
                    NextCardResponse::class.java
                )

                authenticatedPost(
                    "${baseUrl()}/api/study/sessions/$sessionId/answer",
                    AnswerRequest(cardId = cardResponse.body!!.cardId, correct = true),
                    AnswerResponse::class.java
                )
            }

            // Complete session
            authenticatedPost(
                "${baseUrl()}/api/study/sessions/$sessionId/complete",
                null,
                SessionSummaryResponse::class.java
            )

            // ASSERT: Verify review count
            val vocab = userVocabularyRepository.findByUserIdAndWordId(testUser.id!!, word.id!!)!!
            assertEquals(sessionNum + 1, vocab.reviewCount, "Review count should be ${sessionNum + 1}")
        }
    }

    @Test
    @DisplayName("Should set next review date based on interval")
    fun `should schedule next review based on calculated interval`() {
        // ARRANGE
        val word = testWords[0]
        val wordSet = createTestWordSet("Test Set", "fr", listOf(word))

        val startRequest = StartSessionRequest(
            source = SessionSourceType.WORD_SET,
            wordSetId = wordSet.id,
            sessionSize = 1,
            includeNewWords = true
        )

        val beforeSession = Instant.now()

        // ACT
        val startResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/start",
            startRequest,
            SessionResponse::class.java
        )

        val sessionId = startResponse.body!!.sessionId

        // Answer correctly twice
        repeat(2) {
            val cardResponse = authenticatedGet(
                "${baseUrl()}/api/study/sessions/$sessionId/next-card",
                NextCardResponse::class.java
            )

            authenticatedPost(
                "${baseUrl()}/api/study/sessions/$sessionId/answer",
                AnswerRequest(cardId = cardResponse.body!!.cardId, correct = true),
                AnswerResponse::class.java
            )
        }

        // Complete session
        authenticatedPost(
            "${baseUrl()}/api/study/sessions/$sessionId/complete",
            null,
            SessionSummaryResponse::class.java
        )

        // ASSERT
        val vocab = userVocabularyRepository.findByUserIdAndWordId(testUser.id!!, word.id!!)!!

        assertNotNull(vocab.nextReviewAt)
        assertNotNull(vocab.lastReviewedAt)

        // First success should give 40-hour interval
        val expectedNextReview = beforeSession.plus(Duration.ofHours(40))

        // Allow some tolerance for test execution time (5 minutes)
        val actualDuration = Duration.between(beforeSession, vocab.nextReviewAt).toHours()
        assertTrue(
            actualDuration >= 39 && actualDuration <= 41,
            "Next review should be approximately 40 hours from now, got $actualDuration hours"
        )
    }
}
