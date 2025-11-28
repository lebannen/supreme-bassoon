package com.vocabee.service

import com.vocabee.domain.model.ItemStatus
import com.vocabee.domain.model.StudySessionItem
import org.springframework.stereotype.Service

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
        private const val SIMILARITY_THRESHOLD = 60 // Points within which scores are considered similar
        private const val MAX_ATTEMPTS_BONUS = 20   // Cap the attempts bonus to prevent infinite priority
    }

    /**
     * Select the next word to show in a study session.
     *
     * Priority scoring (lower score = higher priority):
     * - LEARNING words: base 0, NEW words: base 20
     * - More attempts: -8 per attempt (capped at -20)
     * - Recently failed: -10 bonus
     * - Consecutive correct streak: +25 per correct (defer words doing well)
     *
     * When multiple candidates have similar scores (within threshold of 60),
     * one is randomly selected to avoid predictable patterns and ensure variety.
     * This ensures NEW words still appear even when some words need practice.
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
     * The algorithm balances:
     * - Prioritizing words that need practice (LEARNING status)
     * - Still introducing NEW words to prevent getting stuck on a few words
     * - Avoiding words that are going well (consecutive correct streak)
     *
     * @param item The session item
     * @return Priority score
     */
    private fun calculatePriorityScore(item: StudySessionItem): Int {
        // Base priority: LEARNING words get slight priority, but NEW words aren't far behind
        val basePriority = when (item.status) {
            ItemStatus.LEARNING -> 0    // Highest priority
            ItemStatus.NEW -> 20        // Small penalty - new words should still appear regularly
            ItemStatus.COMPLETED -> 1000  // Should never be selected
        }

        // Words with more attempts get priority, but capped to prevent monopolizing
        val attemptsBonus = -minOf(item.attemptsCount * 8, MAX_ATTEMPTS_BONUS)

        // Recently failed words get a small boost (but not overwhelming)
        val recentFailBonus = if (item.incorrectCount > item.correctCount && item.attemptsCount > 0) {
            -10
        } else {
            0
        }

        // Words with consecutive correct answers get lower priority (higher score)
        // This ensures words close to completion don't keep appearing
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
