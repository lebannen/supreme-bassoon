package com.vocabee.service

import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import kotlin.math.min

/**
 * Service for calculating Spaced Repetition System (SRS) intervals.
 *
 * The SRS algorithm determines when a word should be reviewed next based on:
 * - How many times it has been reviewed successfully
 * - Whether the last review was correct or incorrect
 * - The current interval
 * - The ease factor (difficulty)
 */
@Service
class SrsIntervalCalculator {

    companion object {
        private const val INITIAL_INTERVAL_HOURS = 20
        private const val MAX_INTERVAL_HOURS = 720 // 30 days
        private const val BASE_MULTIPLIER = 2.0
    }

    /**
     * Calculate the next review interval based on the user's performance.
     *
     * Interval progression (without ease factor):
     * - 1st success: 20 hours
     * - 2nd success: 40 hours (~1.7 days)
     * - 3rd success: 80 hours (~3.3 days)
     * - 4th success: 160 hours (~6.7 days)
     * - 5th success: 320 hours (~13.3 days)
     * - 6th success: 640 hours (~26.7 days)
     * - 7th+ success: Cap at 720 hours (30 days)
     * - Failure: Reset to 20 hours
     *
     * @param consecutiveSuccesses Number of consecutive successful reviews
     * @param wasCorrect Whether the most recent review was correct
     * @param currentIntervalHours Current interval in hours (not currently used)
     * @param easeFactor Difficulty modifier (1.0 = normal/default, higher = easier, lower = harder)
     * @return The new interval in hours
     */
    fun calculateNextInterval(
        consecutiveSuccesses: Int,
        wasCorrect: Boolean,
        currentIntervalHours: Int = INITIAL_INTERVAL_HOURS,
        easeFactor: Double = 1.0
    ): Int {
        // If the answer was incorrect, reset to minimum interval
        if (!wasCorrect) {
            return INITIAL_INTERVAL_HOURS
        }

        // Calculate new interval based on consecutive successes
        val newInterval = when (consecutiveSuccesses) {
            0 -> INITIAL_INTERVAL_HOURS
            1 -> INITIAL_INTERVAL_HOURS * 2      // 40 hours
            2 -> INITIAL_INTERVAL_HOURS * 4      // 80 hours
            3 -> INITIAL_INTERVAL_HOURS * 8      // 160 hours
            4 -> INITIAL_INTERVAL_HOURS * 16     // 320 hours
            5 -> INITIAL_INTERVAL_HOURS * 32     // 640 hours
            else -> MAX_INTERVAL_HOURS            // Cap at 720 hours (30 days)
        }

        // Apply ease factor if different from default
        val adjustedInterval = if (easeFactor != 1.0) {
            (newInterval * easeFactor).toInt()
        } else {
            newInterval
        }

        // Cap at maximum interval
        return min(adjustedInterval, MAX_INTERVAL_HOURS)
    }

    /**
     * Calculate the next review date based on an interval in hours.
     *
     * @param intervalHours The interval in hours
     * @param fromDate The date to calculate from (defaults to now)
     * @return The calculated next review date
     */
    fun calculateNextReviewDate(
        intervalHours: Int,
        fromDate: Instant = Instant.now()
    ): Instant {
        return fromDate.plus(Duration.ofHours(intervalHours.toLong()))
    }

    /**
     * Update the ease factor based on performance.
     * Ease factor affects how quickly intervals increase.
     *
     * This is a simplified implementation. Future versions could use
     * more sophisticated algorithms like SM-2 or SM-2+.
     *
     * @param currentEaseFactor Current ease factor
     * @param wasCorrect Whether the review was correct
     * @return Updated ease factor
     */
    fun updateEaseFactor(currentEaseFactor: Double, wasCorrect: Boolean): Double {
        val adjustment = if (wasCorrect) 0.0 else -0.2

        // Ease factor should stay between 1.0 and 2.5
        val newEaseFactor = currentEaseFactor + adjustment
        return newEaseFactor.coerceIn(1.0, 2.5)
    }

    /**
     * Determine if a word is due for review.
     *
     * @param nextReviewAt The scheduled next review date
     * @param currentTime The current time (defaults to now)
     * @return True if the word is due for review (includes exact match)
     */
    fun isDue(nextReviewAt: Instant?, currentTime: Instant = Instant.now()): Boolean {
        return nextReviewAt == null || !nextReviewAt.isAfter(currentTime)
    }

    /**
     * Get a human-readable representation of an interval.
     *
     * @param intervalHours Interval in hours
     * @return Human-readable string (e.g., "3 days", "20 hours")
     */
    fun formatInterval(intervalHours: Int): String {
        return when {
            intervalHours < 24 -> "$intervalHours hours"
            intervalHours < 168 -> "${intervalHours / 24} days"
            else -> "${intervalHours / 168} weeks"
        }
    }
}
