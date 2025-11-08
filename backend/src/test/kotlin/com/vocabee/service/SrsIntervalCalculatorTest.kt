package com.vocabee.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.time.Duration
import java.time.Instant

class SrsIntervalCalculatorTest {

    private lateinit var srsCalculator: SrsIntervalCalculator

    @BeforeEach
    fun setup() {
        srsCalculator = SrsIntervalCalculator()
    }

    @Nested
    @DisplayName("Interval Calculation Tests")
    inner class IntervalCalculationTests {

        @Test
        fun `should return initial interval for first success`() {
            val interval = srsCalculator.calculateNextInterval(
                consecutiveSuccesses = 0,
                wasCorrect = true
            )

            assertEquals(20, interval, "First success should use initial 20-hour interval")
        }

        @Test
        fun `should reset to initial interval on failure`() {
            val interval = srsCalculator.calculateNextInterval(
                consecutiveSuccesses = 5,  // Had a good streak
                wasCorrect = false,         // But failed this time
                currentIntervalHours = 320
            )

            assertEquals(20, interval, "Failure should reset to 20-hour interval")
        }

        @Test
        fun `should double interval for second success`() {
            val interval = srsCalculator.calculateNextInterval(
                consecutiveSuccesses = 1,
                wasCorrect = true
            )

            assertEquals(40, interval, "Second success should be 40 hours")
        }

        @Test
        fun `should follow progression for consecutive successes`() {
            // Test the full progression
            val expected = mapOf(
                0 to 20,    // 20 hours
                1 to 40,    // 40 hours (~1.7 days)
                2 to 80,    // 80 hours (~3.3 days)
                3 to 160,   // 160 hours (~6.7 days)
                4 to 320,   // 320 hours (~13.3 days)
                5 to 640,   // 640 hours (~26.7 days)
                6 to 720,   // Cap at 720 hours (30 days)
                7 to 720,   // Still capped
                10 to 720   // Still capped
            )

            expected.forEach { (successes, expectedInterval) ->
                val interval = srsCalculator.calculateNextInterval(
                    consecutiveSuccesses = successes,
                    wasCorrect = true
                )
                assertEquals(
                    expectedInterval,
                    interval,
                    "After $successes successes, interval should be $expectedInterval hours"
                )
            }
        }

        @Test
        fun `should respect maximum interval cap`() {
            val interval = srsCalculator.calculateNextInterval(
                consecutiveSuccesses = 100,
                wasCorrect = true
            )

            assertTrue(interval <= 720, "Interval should never exceed 720 hours (30 days)")
        }

        @Test
        fun `should apply ease factor`() {
            val normalInterval = srsCalculator.calculateNextInterval(
                consecutiveSuccesses = 2,
                wasCorrect = true,
                easeFactor = 1.0  // Normal (no adjustment)
            )

            val easierInterval = srsCalculator.calculateNextInterval(
                consecutiveSuccesses = 2,
                wasCorrect = true,
                easeFactor = 1.5  // Higher ease = longer intervals
            )

            val harderInterval = srsCalculator.calculateNextInterval(
                consecutiveSuccesses = 2,
                wasCorrect = true,
                easeFactor = 0.8  // Lower ease = shorter intervals
            )

            assertEquals(80, normalInterval, "Normal ease factor should give base interval")
            assertTrue(
                easierInterval > normalInterval,
                "Higher ease factor should result in longer intervals"
            )
            assertTrue(
                harderInterval < normalInterval,
                "Lower ease factor should result in shorter intervals"
            )
        }

        @Test
        fun `should handle edge case of zero consecutive successes and failure`() {
            val interval = srsCalculator.calculateNextInterval(
                consecutiveSuccesses = 0,
                wasCorrect = false
            )

            assertEquals(20, interval, "Zero successes + failure should give initial interval")
        }
    }

    @Nested
    @DisplayName("Next Review Date Tests")
    inner class NextReviewDateTests {

        @Test
        fun `should calculate next review date correctly`() {
            val now = Instant.parse("2025-01-01T12:00:00Z")
            val nextReview = srsCalculator.calculateNextReviewDate(
                intervalHours = 24,
                fromDate = now
            )

            val expected = Instant.parse("2025-01-02T12:00:00Z")
            assertEquals(expected, nextReview)
        }

        @Test
        fun `should calculate next review from current time by default`() {
            val before = Instant.now()
            val nextReview = srsCalculator.calculateNextReviewDate(intervalHours = 1)
            val after = Instant.now().plus(Duration.ofHours(1))

            assertTrue(nextReview.isAfter(before))
            assertTrue(nextReview.isBefore(after) || nextReview == after)
        }

        @Test
        fun `should handle large intervals`() {
            val now = Instant.parse("2025-01-01T12:00:00Z")
            val nextReview = srsCalculator.calculateNextReviewDate(
                intervalHours = 720,  // 30 days
                fromDate = now
            )

            val expected = Instant.parse("2025-01-31T12:00:00Z")
            assertEquals(expected, nextReview)
        }
    }

    @Nested
    @DisplayName("Ease Factor Tests")
    inner class EaseFactorTests {

        @Test
        fun `should not change ease factor on correct answer`() {
            val newEaseFactor = srsCalculator.updateEaseFactor(
                currentEaseFactor = 2.0,
                wasCorrect = true
            )

            assertEquals(2.0, newEaseFactor)
        }

        @Test
        fun `should decrease ease factor on incorrect answer`() {
            val newEaseFactor = srsCalculator.updateEaseFactor(
                currentEaseFactor = 2.0,
                wasCorrect = false
            )

            assertEquals(1.8, newEaseFactor, 0.01)
        }

        @Test
        fun `should not drop ease factor below minimum`() {
            val newEaseFactor = srsCalculator.updateEaseFactor(
                currentEaseFactor = 1.3,  // Already at minimum
                wasCorrect = false
            )

            assertTrue(newEaseFactor >= 1.3, "Ease factor should not go below 1.3")
        }

        @Test
        fun `should not increase ease factor above maximum`() {
            val newEaseFactor = srsCalculator.updateEaseFactor(
                currentEaseFactor = 2.5,  // Already at maximum
                wasCorrect = true
            )

            assertTrue(newEaseFactor <= 2.5, "Ease factor should not exceed 2.5")
        }

        @Test
        fun `should recover ease factor after multiple failures`() {
            var easeFactor = 2.0

            // Simulate 3 failures
            easeFactor = srsCalculator.updateEaseFactor(easeFactor, false)
            easeFactor = srsCalculator.updateEaseFactor(easeFactor, false)
            easeFactor = srsCalculator.updateEaseFactor(easeFactor, false)

            assertTrue(easeFactor < 2.0, "Ease factor should decrease after failures")
            assertTrue(easeFactor >= 1.3, "Ease factor should stay above minimum")
        }
    }

    @Nested
    @DisplayName("Due Date Tests")
    inner class DueDateTests {

        @Test
        fun `should identify word as due when next review is in the past`() {
            val pastDate = Instant.now().minus(Duration.ofHours(1))

            assertTrue(srsCalculator.isDue(pastDate))
        }

        @Test
        fun `should identify word as due when next review is now`() {
            val now = Instant.now()

            assertTrue(srsCalculator.isDue(now, now))
        }

        @Test
        fun `should identify word as not due when next review is in the future`() {
            val futureDate = Instant.now().plus(Duration.ofHours(1))

            assertFalse(srsCalculator.isDue(futureDate))
        }

        @Test
        fun `should identify word as due when next review is null`() {
            assertTrue(srsCalculator.isDue(null), "Null next review means word has never been reviewed")
        }

        @Test
        fun `should handle custom current time`() {
            val reviewDate = Instant.parse("2025-01-01T12:00:00Z")
            val checkTime = Instant.parse("2025-01-01T13:00:00Z")

            assertTrue(
                srsCalculator.isDue(reviewDate, checkTime),
                "Word should be due when check time is after review date"
            )
        }
    }

    @Nested
    @DisplayName("Interval Formatting Tests")
    inner class IntervalFormattingTests {

        @Test
        fun `should format hours correctly`() {
            assertEquals("20 hours", srsCalculator.formatInterval(20))
            assertEquals("12 hours", srsCalculator.formatInterval(12))
            assertEquals("1 hours", srsCalculator.formatInterval(1))
        }

        @Test
        fun `should format days correctly`() {
            assertEquals("1 days", srsCalculator.formatInterval(24))
            assertEquals("3 days", srsCalculator.formatInterval(80))
            assertEquals("6 days", srsCalculator.formatInterval(160))
        }

        @Test
        fun `should format weeks correctly`() {
            assertEquals("1 weeks", srsCalculator.formatInterval(168))
            assertEquals("1 weeks", srsCalculator.formatInterval(320))  // 320/168 = 1.9, rounds to 1
            assertEquals("3 weeks", srsCalculator.formatInterval(640))  // 640/168 = 3.8, rounds to 3
            assertEquals("4 weeks", srsCalculator.formatInterval(720))  // 720/168 = 4.3, rounds to 4
        }

        @Test
        fun `should handle edge cases in formatting`() {
            assertEquals("0 hours", srsCalculator.formatInterval(0))
            assertEquals("23 hours", srsCalculator.formatInterval(23))
            assertEquals("1 days", srsCalculator.formatInterval(25))
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    inner class IntegrationTests {

        @Test
        fun `should simulate realistic learning progression`() {
            var consecutiveSuccesses = 0
            var easeFactor = 2.0
            var currentInterval = 20

            // Simulate successful reviews
            for (i in 1..7) {
                val newInterval = srsCalculator.calculateNextInterval(
                    consecutiveSuccesses = consecutiveSuccesses,
                    wasCorrect = true,
                    currentIntervalHours = currentInterval,
                    easeFactor = easeFactor
                )

                println("Review $i: $consecutiveSuccesses successes -> $newInterval hours (${srsCalculator.formatInterval(newInterval)})")

                assertTrue(newInterval >= currentInterval, "Interval should increase or stay same on success")
                consecutiveSuccesses++
                currentInterval = newInterval
            }

            // Should have reached or be near the cap
            assertTrue(currentInterval >= 640, "After 7 successes, should be at or near 720-hour cap")
        }

        @Test
        fun `should simulate learning with failures`() {
            var consecutiveSuccesses = 0
            var currentInterval = 20

            // Success, success, fail, success, success
            val results = listOf(true, true, false, true, true)
            val expectedSuccesses = listOf(1, 2, 0, 1, 2)

            results.forEachIndexed { index, wasCorrect ->
                val newInterval = srsCalculator.calculateNextInterval(
                    consecutiveSuccesses = consecutiveSuccesses,
                    wasCorrect = wasCorrect,
                    currentIntervalHours = currentInterval
                )

                println("Attempt ${index + 1}: correct=$wasCorrect, successes before=$consecutiveSuccesses, new interval=$newInterval")

                if (wasCorrect) {
                    consecutiveSuccesses++
                } else {
                    consecutiveSuccesses = 0
                    assertEquals(20, newInterval, "Failure should reset interval to 20 hours")
                }

                currentInterval = newInterval
                assertEquals(expectedSuccesses[index], consecutiveSuccesses)
            }
        }
    }
}
