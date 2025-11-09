package com.vocabee.service

import com.vocabee.domain.model.*
import com.vocabee.domain.repository.*
import com.vocabee.web.dto.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class StudySessionService(
    private val studySessionRepository: StudySessionRepository,
    private val studySessionItemRepository: StudySessionItemRepository,
    private val studySessionAttemptRepository: StudySessionAttemptRepository,
    private val userVocabularyRepository: UserVocabularyRepository,
    private val userRepository: UserRepository,
    private val wordRepository: WordRepository,
    private val wordSetRepository: WordSetRepository,
    private val cardSelectionService: CardSelectionService,
    private val srsIntervalCalculator: SrsIntervalCalculator
) {
    private val logger = LoggerFactory.getLogger(StudySessionService::class.java)

    companion object {
        private const val REQUIRED_CONSECUTIVE_CORRECT = 2
    }

    /**
     * Start a new study session
     */
    @Transactional
    fun startSession(userId: Long, request: StartSessionRequest): SessionResponse {
        // Validate no active session exists
        val existingSession = studySessionRepository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE)
        if (existingSession != null) {
            throw IllegalStateException("User already has an active session (ID: ${existingSession.id})")
        }

        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found: $userId")
        }

        // Select words for the session based on source
        val words = selectWordsForSession(userId, request)

        if (words.isEmpty()) {
            throw IllegalArgumentException("No words available for study")
        }

        // Create session
        val wordSet = if (request.source == SessionSourceType.WORD_SET && request.wordSetId != null) {
            wordSetRepository.findById(request.wordSetId).orElse(null)
        } else null

        val session = StudySession(
            user = user,
            wordSet = wordSet,
            sessionSize = request.sessionSize,
            totalWords = words.size,
            sessionType = SessionType.valueOf(request.source.name)
        )

        val savedSession = studySessionRepository.save(session)

        // Create session items
        words.forEachIndexed { index, (word, userVocab) ->
            val item = StudySessionItem(
                session = savedSession,
                word = word,
                userVocabulary = userVocab,
                displayOrder = index
            )
            savedSession.items.add(item)
        }

        studySessionRepository.save(savedSession)

        logger.info("Started study session ${savedSession.id} for user $userId with ${words.size} words")

        return toSessionResponse(savedSession)
    }

    /**
     * Get the active session for a user
     */
    fun getActiveSession(userId: Long): SessionResponse? {
        val session = studySessionRepository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE)
            ?: return null
        return toSessionResponse(session)
    }

    /**
     * Get session details by ID
     */
    fun getSession(sessionId: Long, userId: Long): SessionResponse {
        val session = studySessionRepository.findById(sessionId).orElseThrow {
            IllegalArgumentException("Session not found: $sessionId")
        }

        if (session.user.id != userId) {
            throw IllegalArgumentException("Session does not belong to user")
        }

        return toSessionResponse(session)
    }

    /**
     * Get the next card to study in the session
     */
    @Transactional
    fun getNextCard(sessionId: Long, userId: Long): NextCardResponse? {
        val session = studySessionRepository.findById(sessionId).orElseThrow {
            IllegalArgumentException("Session not found: $sessionId")
        }

        if (session.user.id != userId) {
            throw IllegalArgumentException("Session does not belong to user")
        }

        if (session.status != SessionStatus.ACTIVE) {
            throw IllegalStateException("Session is not active")
        }

        // Use card selection service to get next card
        val nextItem = cardSelectionService.selectNextCard(
            session.items,
            session.lastShownWordId
        ) ?: return null  // Session complete

        // Update last shown word
        session.lastShownWordId = nextItem.word.id
        studySessionRepository.save(session)

        return toNextCardResponse(nextItem, session)
    }

    /**
     * Submit an answer for a card
     */
    @Transactional
    fun submitAnswer(sessionId: Long, userId: Long, request: AnswerRequest): AnswerResponse {
        val session = studySessionRepository.findById(sessionId).orElseThrow {
            IllegalArgumentException("Session not found: $sessionId")
        }

        if (session.user.id != userId) {
            throw IllegalArgumentException("Session does not belong to user")
        }

        if (session.status != SessionStatus.ACTIVE) {
            throw IllegalStateException("Session is not active")
        }

        val item = session.items.find { it.id == request.cardId }
            ?: throw IllegalArgumentException("Card not found in session: ${request.cardId}")

        // Record the attempt
        val attempt = StudySessionAttempt(
            sessionItem = item,
            wasCorrect = request.correct,
            responseTimeMs = request.responseTimeMs
        )
        item.attemptsList.add(attempt)

        // Update item stats
        item.recordAnswer(request.correct)

        // Update session stats
        session.recordAnswer(request.correct)

        // Check if item is now completed
        val itemCompleted = item.isCompleted
        if (itemCompleted) {
            session.completeWord()
        }

        // Check if session is complete
        val sessionCompleted = cardSelectionService.isSessionComplete(session.items)

        studySessionRepository.save(session)

        val message = when {
            itemCompleted -> "Great! You've mastered this word."
            request.correct -> "Correct! One more time to master it."
            else -> "Keep practicing!"
        }

        return AnswerResponse(
            success = true,
            itemCompleted = itemCompleted,
            sessionCompleted = sessionCompleted,
            consecutiveCorrect = item.consecutiveCorrect,
            message = message
        )
    }

    /**
     * Complete the session and update SRS data
     */
    @Transactional
    fun completeSession(sessionId: Long, userId: Long): SessionSummaryResponse {
        val session = studySessionRepository.findById(sessionId).orElseThrow {
            IllegalArgumentException("Session not found: $sessionId")
        }

        if (session.user.id != userId) {
            throw IllegalArgumentException("Session does not belong to user")
        }

        if (session.status != SessionStatus.ACTIVE) {
            throw IllegalStateException("Session is already completed or abandoned")
        }

        // Mark session as completed
        session.complete()

        // Update SRS data for all items
        var wordsAdvanced = 0
        var wordsReset = 0

        session.items.forEach { item ->
            val userVocab = item.userVocabulary
                ?: findOrCreateUserVocabulary(userId, item.word.id!!)

            val wasSuccessful = item.correctCount > item.incorrectCount

            // Update review stats
            userVocab.reviewCount++
            userVocab.lastReviewedAt = Instant.now()

            if (wasSuccessful) {
                userVocab.consecutiveSuccesses++
                wordsAdvanced++
            } else {
                userVocab.consecutiveSuccesses = 0
                wordsReset++
            }

            // Calculate new interval
            val newInterval = srsIntervalCalculator.calculateNextInterval(
                consecutiveSuccesses = userVocab.consecutiveSuccesses,
                wasCorrect = wasSuccessful,
                currentIntervalHours = userVocab.currentIntervalHours,
                easeFactor = userVocab.easeFactor
            )

            userVocab.currentIntervalHours = newInterval
            userVocab.nextReviewAt = srsIntervalCalculator.calculateNextReviewDate(newInterval)

            // Update ease factor
            userVocab.easeFactor = srsIntervalCalculator.updateEaseFactor(
                userVocab.easeFactor,
                wasSuccessful
            )

            userVocabularyRepository.save(userVocab)
        }

        studySessionRepository.save(session)

        // Get count of due words after this session
        val nextDueCount = userVocabularyRepository.countDueWords(userId, Instant.now())

        logger.info("Completed session $sessionId: $wordsAdvanced advanced, $wordsReset reset")

        return toSessionSummaryResponse(session, wordsAdvanced, wordsReset, nextDueCount)
    }

    /**
     * Abandon an active session
     */
    @Transactional
    fun abandonSession(sessionId: Long, userId: Long) {
        val session = studySessionRepository.findById(sessionId).orElseThrow {
            IllegalArgumentException("Session not found: $sessionId")
        }

        if (session.user.id != userId) {
            throw IllegalArgumentException("Session does not belong to user")
        }

        if (session.status != SessionStatus.ACTIVE) {
            throw IllegalStateException("Session is not active")
        }

        session.abandon()
        studySessionRepository.save(session)

        logger.info("Abandoned session $sessionId for user $userId")
    }

    /**
     * Get session history for a user
     */
    fun getSessionHistory(userId: Long, limit: Int = 20): SessionHistoryResponse {
        val sessions = studySessionRepository.findByUserIdOrderByStartedAtDesc(userId)
            .take(limit)

        val totalWordsStudied = sessions
            .filter { it.status == SessionStatus.COMPLETED }
            .sumOf { it.wordsCompleted }

        return SessionHistoryResponse(
            sessions = sessions.map { toSessionHistoryItem(it) },
            totalSessions = sessions.size,
            totalWordsStudied = totalWordsStudied
        )
    }

    // Helper methods

    private fun selectWordsForSession(
        userId: Long,
        request: StartSessionRequest
    ): List<Pair<Word, UserVocabulary?>> {
        return when (request.source) {
            SessionSourceType.WORD_SET -> {
                val wordSetId = request.wordSetId
                    ?: throw IllegalArgumentException("wordSetId required for WORD_SET source")

                val wordSet = wordSetRepository.findById(wordSetId).orElseThrow {
                    IllegalArgumentException("Word set not found: $wordSetId")
                }

                val words = wordSet.items.map { it.word }
                selectAndLimitWords(words, userId, request.sessionSize)
            }

            SessionSourceType.VOCABULARY -> {
                val userVocab = userVocabularyRepository.findByUserIdOrderByAddedAtDesc(userId)
                val words = userVocab.map { it.word }
                selectAndLimitWords(words, userId, request.sessionSize, prioritizeDue = true)
            }

            SessionSourceType.DUE_REVIEW -> {
                val dueWords = userVocabularyRepository.findDueWords(userId, Instant.now())
                val pairs = dueWords.map { it.word to it }
                if (request.sessionSize > 0) {
                    pairs.take(request.sessionSize)
                } else {
                    pairs
                }
            }
        }
    }

    private fun selectAndLimitWords(
        words: List<Word>,
        userId: Long,
        sessionSize: Int,
        prioritizeDue: Boolean = false
    ): List<Pair<Word, UserVocabulary?>> {
        val userVocabMap = userVocabularyRepository.findByUserIdAndWordIdIn(
            userId,
            words.mapNotNull { it.id }
        ).associateBy { it.word.id }

        val pairs = words.map { word ->
            word to userVocabMap[word.id]
        }

        // Sort by due date if prioritizing
        val sorted = if (prioritizeDue) {
            pairs.sortedBy { (_, vocab) ->
                vocab?.nextReviewAt ?: Instant.EPOCH
            }
        } else {
            pairs
        }

        return if (sessionSize > 0) {
            sorted.take(sessionSize)
        } else {
            sorted
        }
    }

    private fun findOrCreateUserVocabulary(userId: Long, wordId: Long): UserVocabulary {
        return userVocabularyRepository.findByUserIdAndWordId(userId, wordId)
            ?: run {
                val user = userRepository.findById(userId).orElseThrow()
                val word = wordRepository.findById(wordId).orElseThrow()
                UserVocabulary(user = user, word = word)
            }
    }

    // DTO conversion methods

    private fun toSessionResponse(session: StudySession): SessionResponse {
        val progress = cardSelectionService.getSessionProgress(session.items)

        return SessionResponse(
            sessionId = session.id!!,
            status = session.status.name,
            startedAt = session.startedAt,
            completedAt = session.completedAt,
            totalWords = session.totalWords,
            wordsCompleted = session.wordsCompleted,
            progress = SessionProgressDto(
                completed = progress["completed"] ?: 0,
                learning = progress["learning"] ?: 0,
                new = progress["new"] ?: 0
            ),
            stats = SessionStatsDto(
                totalAttempts = session.totalAttempts,
                correctAttempts = session.correctAttempts,
                incorrectAttempts = session.incorrectAttempts,
                accuracy = session.accuracy
            )
        )
    }

    private fun toNextCardResponse(item: StudySessionItem, session: StudySession): NextCardResponse {
        val word = item.word
        val userVocab = item.userVocabulary

        return NextCardResponse(
            cardId = item.id!!,
            word = WordCardDto(
                id = word.id!!,
                lemma = word.lemma,
                partOfSpeech = word.partOfSpeech,
                languageCode = word.languageCode,
                definitions = word.definitions.map { def ->
                    DefinitionCardDto(
                        id = def.id!!,
                        definitionNumber = def.definitionNumber,
                        definitionText = def.definitionText,
                        examples = def.examples.map { ex ->
                            ExampleCardDto(
                                id = ex.id!!,
                                sentenceText = ex.sentenceText,
                                translationText = ex.translation
                            )
                        }
                    )
                }
            ),
            progress = CardProgressDto(
                position = session.wordsCompleted + 1,
                total = session.totalWords,
                currentStreak = item.consecutiveCorrect,
                needsStreak = REQUIRED_CONSECUTIVE_CORRECT
            ),
            srsInfo = SrsInfoDto(
                reviewCount = userVocab?.reviewCount ?: 0,
                currentInterval = srsIntervalCalculator.formatInterval(
                    userVocab?.currentIntervalHours ?: 20
                ),
                nextReview = userVocab?.nextReviewAt
            )
        )
    }

    private fun toSessionSummaryResponse(
        session: StudySession,
        wordsAdvanced: Int,
        wordsReset: Int,
        nextDueCount: Int
    ): SessionSummaryResponse {
        val duration = Duration.between(session.startedAt, session.completedAt ?: Instant.now())
        val minutes = duration.toMinutes()

        val avgResponseTime = studySessionAttemptRepository.getAverageResponseTimeForSession(session.id!!)

        val newWords = session.items.count { it.userVocabulary == null || it.userVocabulary!!.reviewCount == 1 }
        val reviewWords = session.items.size - newWords

        return SessionSummaryResponse(
            sessionId = session.id,
            completedAt = session.completedAt!!,
            duration = "$minutes minutes",
            stats = SessionSummaryStatsDto(
                totalWords = session.totalWords,
                newWords = newWords,
                reviewWords = reviewWords,
                totalAttempts = session.totalAttempts,
                correctAttempts = session.correctAttempts,
                incorrectAttempts = session.incorrectAttempts,
                accuracy = session.accuracy,
                averageResponseTime = avgResponseTime?.let { String.format("%.1f seconds", it / 1000.0) }
            ),
            srsUpdates = SrsUpdatesSummaryDto(
                wordsAdvanced = wordsAdvanced,
                wordsReset = wordsReset,
                nextDueCount = nextDueCount
            )
        )
    }

    private fun toSessionHistoryItem(session: StudySession): SessionHistoryItemDto {
        return SessionHistoryItemDto(
            sessionId = session.id!!,
            startedAt = session.startedAt,
            completedAt = session.completedAt,
            status = session.status.name,
            sessionType = session.sessionType?.name,
            totalWords = session.totalWords,
            wordsCompleted = session.wordsCompleted,
            accuracy = session.accuracy
        )
    }
}
