package com.vocabee.web.dto

import java.time.Instant

/**
 * Request to start a new study session
 */
data class StartSessionRequest(
    val source: SessionSourceType,
    val wordSetId: Long? = null,  // Required if source=WORD_SET
    val sessionSize: Int,  // Number of words, or 0 for all available
    val includeNewWords: Boolean = true  // Include words never reviewed
)

enum class SessionSourceType {
    WORD_SET,
    VOCABULARY,
    DUE_REVIEW
}

/**
 * Response containing session information and progress
 */
data class SessionResponse(
    val sessionId: Long,
    val status: String,  // ACTIVE, COMPLETED, ABANDONED
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val totalWords: Int,
    val wordsCompleted: Int,
    val progress: SessionProgressDto,
    val stats: SessionStatsDto
)

data class SessionProgressDto(
    val completed: Int,
    val learning: Int,
    val new: Int
)

data class SessionStatsDto(
    val totalAttempts: Int,
    val correctAttempts: Int,
    val incorrectAttempts: Int,
    val accuracy: Double
)

/**
 * Response containing the next card to study
 */
data class NextCardResponse(
    val cardId: Long,  // StudySessionItem ID
    val word: WordCardDto,
    val progress: CardProgressDto,
    val srsInfo: SrsInfoDto
)

data class WordCardDto(
    val id: Long,
    val lemma: String,
    val partOfSpeech: String?,
    val languageCode: String,
    val definitions: List<DefinitionCardDto>
)

data class DefinitionCardDto(
    val id: Long,
    val definitionNumber: Int,
    val definitionText: String,
    val examples: List<ExampleCardDto>
)

data class ExampleCardDto(
    val id: Long,
    val sentenceText: String,
    val translationText: String?
)

data class CardProgressDto(
    val position: Int,  // Current position in session
    val total: Int,  // Total words in session
    val currentStreak: Int,  // Consecutive correct for this word
    val needsStreak: Int  // How many consecutive correct needed (usually 2)
)

data class SrsInfoDto(
    val reviewCount: Int,
    val currentInterval: String,  // Formatted interval (e.g., "3 days")
    val nextReview: Instant?
)

/**
 * Request to submit an answer for the current card
 */
data class AnswerRequest(
    val cardId: Long,  // StudySessionItem ID
    val correct: Boolean,
    val responseTimeMs: Int? = null
)

/**
 * Response after submitting an answer
 */
data class AnswerResponse(
    val success: Boolean,
    val itemCompleted: Boolean,  // Did this word reach 2 consecutive correct?
    val sessionCompleted: Boolean,  // Is the entire session done?
    val consecutiveCorrect: Int,  // Current streak for this word
    val message: String?
)

/**
 * Summary shown at the end of a session
 */
data class SessionSummaryResponse(
    val sessionId: Long,
    val completedAt: Instant,
    val duration: String,  // Human-readable (e.g., "30 minutes")
    val stats: SessionSummaryStatsDto,
    val srsUpdates: SrsUpdatesSummaryDto
)

data class SessionSummaryStatsDto(
    val totalWords: Int,
    val newWords: Int,
    val reviewWords: Int,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val incorrectAttempts: Int,
    val accuracy: Double,
    val averageResponseTime: String?  // e.g., "3.2 seconds"
)

data class SrsUpdatesSummaryDto(
    val wordsAdvanced: Int,  // Words that moved to longer intervals
    val wordsReset: Int,  // Words that were reset to 20 hours
    val nextDueCount: Int  // How many words are due after this session
)

/**
 * Response for due words count
 */
data class DueWordsResponse(
    val totalDue: Int,
    val overdue: Int,  // Due before now
    val dueToday: Int,  // Due within next 24 hours
    val dueSoon: Int  // Due within next 3 days
)

/**
 * Response for list of due words
 */
data class DueWordsList(
    val words: List<DueWordDto>,
    val totalCount: Int
)

data class DueWordDto(
    val wordId: Long,
    val lemma: String,
    val partOfSpeech: String?,
    val languageCode: String,
    val nextReviewAt: Instant,
    val daysOverdue: Int,  // Negative if not yet due
    val reviewCount: Int,
    val currentInterval: String
)

/**
 * Response for session history
 */
data class SessionHistoryResponse(
    val sessions: List<SessionHistoryItemDto>,
    val totalSessions: Int,
    val totalWordsStudied: Int
)

data class SessionHistoryItemDto(
    val sessionId: Long,
    val startedAt: Instant,
    val completedAt: Instant?,
    val status: String,
    val sessionType: String?,
    val totalWords: Int,
    val wordsCompleted: Int,
    val accuracy: Double
)
