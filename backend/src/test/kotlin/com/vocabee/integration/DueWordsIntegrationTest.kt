package com.vocabee.integration

import com.vocabee.web.dto.DueWordsList
import com.vocabee.web.dto.DueWordsResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("Due Words Integration Tests")
class DueWordsIntegrationTest : BaseIntegrationTest() {

    private lateinit var testWords: List<com.vocabee.domain.model.Word>

    @BeforeEach
    fun setup() {
        createTestLanguage("fr", "French")
        testWords = listOf(
            createTestWord("parler", "fr", "verb"),
            createTestWord("manger", "fr", "verb"),
            createTestWord("dormir", "fr", "verb"),
            createTestWord("boire", "fr", "verb"),
            createTestWord("lire", "fr", "verb"),
            createTestWord("écrire", "fr", "verb")
        )
    }

    @Test
    @DisplayName("Should correctly count due words")
    fun `should return correct counts for due words`() {
        // ARRANGE: Create vocabulary with different due dates
        val now = Instant.now()

        // Overdue (1 day ago)
        createTestUserVocabulary(testUser, testWords[0], nextReviewAt = now.minus(1, ChronoUnit.DAYS))
        createTestUserVocabulary(testUser, testWords[1], nextReviewAt = now.minus(1, ChronoUnit.DAYS))

        // Due today (in 2 hours)
        createTestUserVocabulary(testUser, testWords[2], nextReviewAt = now.plus(2, ChronoUnit.HOURS))

        // Due soon (in 2 days)
        createTestUserVocabulary(testUser, testWords[3], nextReviewAt = now.plus(2, ChronoUnit.DAYS))

        // Not due yet (in 5 days)
        createTestUserVocabulary(testUser, testWords[4], nextReviewAt = now.plus(5, ChronoUnit.DAYS))

        // Never reviewed (null nextReviewAt) - should count as due
        createTestUserVocabulary(testUser, testWords[5], nextReviewAt = null)

        // ACT
        val response = authenticatedGet(
            "${baseUrl()}/api/study/due-words",
            DueWordsResponse::class.java
        )

        // ASSERT
        assertEquals(HttpStatus.OK, response.statusCode)
        val dueWords = response.body!!

        println("=== Due Words Count ===")
        println("Total due: ${dueWords.totalDue}")
        println("Overdue: ${dueWords.overdue}")
        println("Due today: ${dueWords.dueToday}")
        println("Due soon: ${dueWords.dueSoon}")

        assertEquals(4, dueWords.totalDue, "Should have 4 words due (2 overdue + 1 today + 1 never reviewed)")
        assertEquals(2, dueWords.overdue, "Should have 2 overdue words")
        assertTrue(dueWords.dueToday >= 1, "Should have at least 1 word due today")
        assertTrue(dueWords.dueSoon >= 1, "Should have at least 1 word due soon")
    }

    @Test
    @DisplayName("Should return list of due words with details")
    fun `should return detailed list of due words`() {
        // ARRANGE
        val now = Instant.now()

        // Create some due words
        createTestUserVocabulary(
            testUser,
            testWords[0],
            nextReviewAt = now.minus(2, ChronoUnit.DAYS),
            consecutiveSuccesses = 2
        ).apply {
            currentIntervalHours = 80
            reviewCount = 3
            userVocabularyRepository.save(this)
        }

        createTestUserVocabulary(
            testUser,
            testWords[1],
            nextReviewAt = now.minus(1, ChronoUnit.HOURS),
            consecutiveSuccesses = 1
        ).apply {
            currentIntervalHours = 40
            reviewCount = 2
            userVocabularyRepository.save(this)
        }

        // ACT
        val response = authenticatedGet(
            "${baseUrl()}/api/study/due-words/list?limit=10",
            DueWordsList::class.java
        )

        // ASSERT
        assertEquals(HttpStatus.OK, response.statusCode)
        val dueWordsList = response.body!!

        assertEquals(2, dueWordsList.totalCount)
        assertEquals(2, dueWordsList.words.size)

        // Verify details of first word (most overdue)
        val firstWord = dueWordsList.words[0]
        assertEquals("parler", firstWord.lemma)
        assertEquals("fr", firstWord.languageCode)
        assertEquals("verb", firstWord.partOfSpeech)
        assertTrue(firstWord.daysOverdue >= 1, "Should be at least 1 day overdue")
        assertEquals(3, firstWord.reviewCount)
        assertEquals("3 days", firstWord.currentInterval)

        println("=== Due Words List ===")
        dueWordsList.words.forEach { word ->
            println("${word.lemma}: ${word.daysOverdue} days overdue, " +
                    "reviewed ${word.reviewCount} times, " +
                    "interval: ${word.currentInterval}")
        }
    }

    @Test
    @DisplayName("Should respect limit parameter")
    fun `should limit number of returned due words`() {
        // ARRANGE: Create 10 due words
        val now = Instant.now()
        testWords.forEachIndexed { index, word ->
            createTestUserVocabulary(
                testUser,
                word,
                nextReviewAt = now.minus(index.toLong(), ChronoUnit.HOURS)
            )
        }

        // ACT: Request only 3 words
        val response = authenticatedGet(
            "${baseUrl()}/api/study/due-words/list?limit=3",
            DueWordsList::class.java
        )

        // ASSERT
        assertEquals(HttpStatus.OK, response.statusCode)
        val dueWordsList = response.body!!

        assertEquals(3, dueWordsList.words.size, "Should return only 3 words")
        assertTrue(dueWordsList.totalCount >= 3, "Total count should be at least 3")
    }

    @Test
    @DisplayName("Should return empty list when no words are due")
    fun `should return empty list when no words are due`() {
        // ARRANGE: Create vocabulary with all words due in the future
        val now = Instant.now()
        testWords.forEach { word ->
            createTestUserVocabulary(
                testUser,
                word,
                nextReviewAt = now.plus(7, ChronoUnit.DAYS)
            )
        }

        // ACT
        val countResponse = authenticatedGet(
            "${baseUrl()}/api/study/due-words",
            DueWordsResponse::class.java
        )

        val listResponse = authenticatedGet(
            "${baseUrl()}/api/study/due-words/list",
            DueWordsList::class.java
        )

        // ASSERT
        assertEquals(HttpStatus.OK, countResponse.statusCode)
        assertEquals(0, countResponse.body!!.totalDue)
        assertEquals(0, countResponse.body!!.overdue)

        assertEquals(HttpStatus.OK, listResponse.statusCode)
        assertEquals(0, listResponse.body!!.totalCount)
        assertTrue(listResponse.body!!.words.isEmpty())
    }

    @Test
    @DisplayName("Should prioritize most overdue words")
    fun `should return most overdue words first`() {
        // ARRANGE: Create words with different overdue amounts
        val now = Instant.now()

        val word1 = createTestUserVocabulary(
            testUser,
            testWords[0],
            nextReviewAt = now.minus(5, ChronoUnit.DAYS)  // 5 days overdue
        )

        val word2 = createTestUserVocabulary(
            testUser,
            testWords[1],
            nextReviewAt = now.minus(1, ChronoUnit.DAYS)  // 1 day overdue
        )

        val word3 = createTestUserVocabulary(
            testUser,
            testWords[2],
            nextReviewAt = now.minus(10, ChronoUnit.DAYS)  // 10 days overdue (most)
        )

        // ACT
        val response = authenticatedGet(
            "${baseUrl()}/api/study/due-words/list",
            DueWordsList::class.java
        )

        // ASSERT
        val dueWords = response.body!!.words

        // First word should be most overdue
        assertEquals("dormir", dueWords[0].lemma, "Most overdue word should be first")
        assertTrue(dueWords[0].daysOverdue >= 9, "Should be ~10 days overdue")

        // Second should be 5 days overdue
        assertEquals("parler", dueWords[1].lemma)
        assertTrue(dueWords[1].daysOverdue >= 4, "Should be ~5 days overdue")

        // Third should be 1 day overdue
        assertEquals("manger", dueWords[2].lemma)
        assertTrue(dueWords[2].daysOverdue >= 0, "Should be ~1 day overdue")
    }

    @Test
    @DisplayName("Should update due count after completing session")
    fun `should reflect updated due count after session completion`() {
        // ARRANGE: Create due words and verify initial count
        val now = Instant.now()
        testWords.take(3).forEach { word ->
            createTestUserVocabulary(testUser, word, nextReviewAt = now.minus(1, ChronoUnit.HOURS))
        }

        val wordSet = createTestWordSet("Test Set", "fr", testWords.take(3))

        // Check initial due count
        val initialResponse = authenticatedGet(
            "${baseUrl()}/api/study/due-words",
            DueWordsResponse::class.java
        )
        assertEquals(3, initialResponse.body!!.totalDue)

        // ACT: Complete a study session
        val startRequest = com.vocabee.web.dto.StartSessionRequest(
            source = com.vocabee.web.dto.SessionSourceType.WORD_SET,
            wordSetId = wordSet.id,
            sessionSize = 2,
            includeNewWords = true
        )

        val startResponse = authenticatedPost(
            "${baseUrl()}/api/study/sessions/start",
            startRequest,
            com.vocabee.web.dto.SessionResponse::class.java
        )

        val sessionId = startResponse.body!!.sessionId

        // Complete the session by answering correctly
        var attempts = 0
        while (attempts < 20) {
            attempts++

            val cardResponse = authenticatedGet(
                "${baseUrl()}/api/study/sessions/$sessionId/next-card",
                com.vocabee.web.dto.NextCardResponse::class.java
            )

            if (cardResponse.statusCode == HttpStatus.NO_CONTENT) break

            val card = cardResponse.body!!

            val answerResponse = authenticatedPost(
                "${baseUrl()}/api/study/sessions/$sessionId/answer",
                com.vocabee.web.dto.AnswerRequest(cardId = card.cardId, correct = true),
                com.vocabee.web.dto.AnswerResponse::class.java
            )

            if (answerResponse.body!!.sessionCompleted) break
        }

        // Complete session
        authenticatedPost(
            "${baseUrl()}/api/study/sessions/$sessionId/complete",
            null,
            com.vocabee.web.dto.SessionSummaryResponse::class.java
        )

        // ASSERT: Due count should decrease
        val finalResponse = authenticatedGet(
            "${baseUrl()}/api/study/due-words",
            DueWordsResponse::class.java
        )

        assertTrue(
            finalResponse.body!!.totalDue < initialResponse.body!!.totalDue,
            "Due count should decrease after session (initial: ${initialResponse.body!!.totalDue}, " +
                    "final: ${finalResponse.body!!.totalDue})"
        )
    }
}
