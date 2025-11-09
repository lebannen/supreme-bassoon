package com.vocabee.service

import com.vocabee.domain.model.ItemStatus
import com.vocabee.domain.model.StudySessionItem
import org.springframework.stereotype.Service
import kotlin.random.Random

/**
 * Service for selecting the next card to show in a study session.
 *
 * The selection algorithm prioritizes:
 * 1. Words that were recently answered incorrectly (need more practice)
 * 2. Words that have been attempted more times (getting stuck on)
 * 3. Words in LEARNING status over NEW words
 * 4. Avoids showing the same word twice in a row
 * 5. Adds randomization to prevent monotonous patterns with small word sets
 */
@Service
class CardSelectionService {

    companion object {
        private const val SIMILARITY_THRESHOLD = 30 // Points within which scores are considered similar
    }

    /**
     * Select the next word to show in a study session.
     *
     * Priority scoring (lower score = higher priority):
     * - Recently failed words: -1000 penalty (shown first)
     * - More attempts: -10 per attempt (struggling words get priority)
     * - Higher consecutive correct streak: +20 per streak (defer words doing well)
     * - New words: +100 penalty (show after learning words)
     *
     * When multiple candidates have similar scores, one is randomly selected
     * to avoid predictable patterns.
     *
     * @param items All items in the session
     * @param lastShownWordId ID of the last shown word (to avoid immediate repetition)
     * @return The next item to show, or null if all items are completed
     */
    fun selectNextCard(
        items: List<StudySessionItem>,
        lastShownWordId: Long? = null
    ): StudySessionItem? {
        // Filter to incomplete items only
        val incomplete = items.filter { it.status != ItemStatus.COMPLETED }

        if (incomplete.isEmpty()) {
            return null
        }

        // Try to find candidates excluding the last shown word
        var candidates = if (lastShownWordId != null) {
            incomplete.filter { it.word.id != lastShownWordId }
        } else {
            incomplete
        }

        // If only one word left and it was the last shown, we must show it again
        if (candidates.isEmpty() && incomplete.size == 1) {
            return incomplete.first()
        }

        // If no candidates, fall back to all incomplete items
        if (candidates.isEmpty()) {
            candidates = incomplete
        }

        // Calculate priority scores for all candidates
        val candidatesWithScores = candidates.map { item ->
            item to calculatePriorityScore(item)
        }

        // Find the minimum score
        val minScore = candidatesWithScores.minOf { it.second }

        // Find all candidates within the similarity threshold of the minimum score
        val topCandidates = candidatesWithScores
            .filter { it.second <= minScore + SIMILARITY_THRESHOLD }
            .map { it.first }

        // If we have multiple similarly-scored candidates, choose randomly to add variety
        return if (topCandidates.size > 1) {
            topCandidates.random()
        } else {
            topCandidates.firstOrNull()
        }
    }

    /**
     * Calculate priority score for an item.
     * Lower score = higher priority.
     *
     * @param item The session item
     * @return Priority score
     */
    private fun calculatePriorityScore(item: StudySessionItem): Int {
        // Base priority: show NEW words first (lower score)
        val basePriority = when (item.status) {
            ItemStatus.NEW -> 0
            ItemStatus.LEARNING -> 100
            ItemStatus.COMPLETED -> 1000  // Should never be selected
        }

        // Words with more attempts get higher priority (lower score) - they're struggling
        val attemptsBonus = -(item.attemptsCount * 15)

        // Recently failed words get a moderate boost (not too extreme)
        val recentFailBonus = if (item.incorrectCount > item.correctCount && item.attemptsCount > 0) {
            -30
        } else {
            0
        }

        // Words with consecutive correct answers get lower priority (higher score)
        val streakPenalty = item.consecutiveCorrect * 25

        return basePriority + attemptsBonus + recentFailBonus + streakPenalty
    }

    /**
     * Get statistics about remaining items in the session.
     *
     * @param items All items in the session
     * @return Map with counts: "new", "learning", "completed"
     */
    fun getSessionProgress(items: List<StudySessionItem>): Map<String, Int> {
        val newCount = items.count { it.status == ItemStatus.NEW }
        val learningCount = items.count { it.status == ItemStatus.LEARNING }
        val completedCount = items.count { it.status == ItemStatus.COMPLETED }

        return mapOf(
            "new" to newCount,
            "learning" to learningCount,
            "completed" to completedCount
        )
    }

    /**
     * Check if the session is complete (all items are COMPLETED).
     *
     * @param items All items in the session
     * @return True if all items are completed
     */
    fun isSessionComplete(items: List<StudySessionItem>): Boolean {
        return items.all { it.status == ItemStatus.COMPLETED }
    }
}
