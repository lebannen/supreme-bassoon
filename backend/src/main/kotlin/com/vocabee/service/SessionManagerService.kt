package com.vocabee.service

import com.vocabee.domain.model.*
import com.vocabee.domain.repository.*
import com.vocabee.web.dto.SessionSourceType
import com.vocabee.web.dto.StartSessionRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SessionManagerService(
    private val studySessionRepository: StudySessionRepository,
    private val userRepository: UserRepository,
    private val wordRepository: WordRepository,
    private val wordSetRepository: WordSetRepository,
    private val userVocabularyRepository: UserVocabularyRepository,
    private val srsUpdateService: SrsUpdateService
) {
    private val logger = LoggerFactory.getLogger(SessionManagerService::class.java)

    @Transactional
    fun startSession(userId: Long, request: StartSessionRequest): StudySession {
        if (studySessionRepository.existsByUserIdAndStatus(userId, SessionStatus.ACTIVE)) {
            throw IllegalStateException("User already has an active session.")
        }

        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found: $userId") }
        val words = selectWordsForSession(userId, request)

        if (words.isEmpty()) {
            throw IllegalArgumentException("No words available for study for the selected source.")
        }

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

        words.forEachIndexed { index, (word, userVocab) ->
            session.items.add(
                StudySessionItem(
                    session = session,
                    word = word,
                    userVocabulary = userVocab,
                    displayOrder = index
                )
            )
        }

        val savedSession = studySessionRepository.save(session)
        logger.info("Started study session ${savedSession.id} for user $userId with ${words.size} words")
        return savedSession
    }

    @Transactional
    fun completeSession(sessionId: Long, userId: Long): Pair<StudySession, SrsUpdateService.SrsUpdateResult> {
        val session = findSessionForUser(sessionId, userId)
        if (session.status != SessionStatus.ACTIVE) {
            throw IllegalStateException("Session is not active.")
        }

        session.complete()
        val result = srsUpdateService.updateUserVocabulary(session)
        logger.info("Completed session $sessionId: ${result.wordsAdvanced} words advanced, ${result.wordsReset} words reset.")
        val savedSession = studySessionRepository.save(session)
        return Pair(savedSession, result)
    }

    @Transactional
    fun abandonSession(sessionId: Long, userId: Long) {
        val session = findSessionForUser(sessionId, userId)
        if (session.status != SessionStatus.ACTIVE) {
            throw IllegalStateException("Session is not active.")
        }
        session.abandon()
        studySessionRepository.save(session)
        logger.info("Abandoned session $sessionId for user $userId")
    }

    fun findSessionForUser(sessionId: Long, userId: Long): StudySession {
        val session = studySessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found: $sessionId") }
        if (session.user.id != userId) {
            throw SecurityException("User $userId is not authorized to access session $sessionId")
        }
        return session
    }

    private fun selectWordsForSession(userId: Long, request: StartSessionRequest): List<Pair<Word, UserVocabulary?>> {
        return when (request.source) {
            SessionSourceType.WORD_SET -> {
                val wordSetId =
                    request.wordSetId ?: throw IllegalArgumentException("wordSetId is required for WORD_SET source")
                val wordSet = wordSetRepository.findById(wordSetId)
                    .orElseThrow { IllegalArgumentException("Word set not found: $wordSetId") }
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
                if (request.sessionSize > 0) pairs.take(request.sessionSize) else pairs
            }
        }
    }

    private fun selectAndLimitWords(
        words: List<Word>,
        userId: Long,
        sessionSize: Int,
        prioritizeDue: Boolean = false
    ): List<Pair<Word, UserVocabulary?>> {
        val userVocabMap = userVocabularyRepository.findByUserIdAndWordIdIn(userId, words.mapNotNull { it.id })
            .associateBy { it.word.id }
        val pairs = words.map { word -> word to userVocabMap[word.id] }
        val sorted = if (prioritizeDue) pairs.sortedBy { (_, vocab) -> vocab?.nextReviewAt ?: Instant.EPOCH } else pairs
        return if (sessionSize > 0) sorted.take(sessionSize) else sorted
    }
}
