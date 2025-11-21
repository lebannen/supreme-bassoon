package com.vocabee.service

import com.vocabee.domain.model.SessionStatus
import com.vocabee.domain.repository.StudySessionAttemptRepository
import com.vocabee.domain.repository.StudySessionRepository
import com.vocabee.domain.repository.UserVocabularyRepository
import com.vocabee.web.dto.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class StudySessionService(
    private val sessionManagerService: SessionManagerService,
    private val sessionOrchestratorService: SessionOrchestratorService,
    private val studySessionRepository: StudySessionRepository,
    private val userVocabularyRepository: UserVocabularyRepository,
    private val studySessionAttemptRepository: StudySessionAttemptRepository,
    private val sessionDtoMapper: SessionDtoMapper
) {

    @Transactional
    fun startSession(userId: Long, request: StartSessionRequest): SessionResponse {
        val session = sessionManagerService.startSession(userId, request)
        return sessionDtoMapper.toSessionResponse(session)
    }

    @Transactional(readOnly = true)
    fun getActiveSession(userId: Long): SessionResponse? {
        val session = studySessionRepository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE)
        return session?.let { sessionDtoMapper.toSessionResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getSession(sessionId: Long, userId: Long): SessionResponse {
        val session = sessionManagerService.findSessionForUser(sessionId, userId)
        return sessionDtoMapper.toSessionResponse(session)
    }

    @Transactional
    fun getNextCard(sessionId: Long, userId: Long): NextCardResponse? {
        return sessionOrchestratorService.getNextCard(sessionId, userId)
    }

    @Transactional
    fun submitAnswer(sessionId: Long, userId: Long, request: AnswerRequest): AnswerResponse {
        return sessionOrchestratorService.submitAnswer(sessionId, userId, request)
    }

    @Transactional
    fun completeSession(sessionId: Long, userId: Long): SessionSummaryResponse {
        val (session, srsResult) = sessionManagerService.completeSession(sessionId, userId)

        val nextDueCount = userVocabularyRepository.countDueWords(userId, Instant.now())
        val avgResponseTime = studySessionAttemptRepository.getAverageResponseTimeForSession(session.id!!)

        return sessionDtoMapper.toSessionSummaryResponse(
            session = session,
            wordsAdvanced = srsResult.wordsAdvanced,
            wordsReset = srsResult.wordsReset,
            nextDueCount = nextDueCount,
            avgResponseTime = avgResponseTime
        )
    }

    @Transactional
    fun abandonSession(sessionId: Long, userId: Long) {
        sessionManagerService.abandonSession(sessionId, userId)
    }

    @Transactional(readOnly = true)
    fun getSessionHistory(userId: Long, limit: Int = 20): SessionHistoryResponse {
        val sessions = studySessionRepository.findByUserIdOrderByStartedAtDesc(userId).take(limit)
        val totalWordsStudied = sessions
            .filter { it.status == SessionStatus.COMPLETED }
            .sumOf { it.wordsCompleted }

        return SessionHistoryResponse(
            sessions = sessions.map { sessionDtoMapper.toSessionHistoryItem(it) },
            totalSessions = sessions.size,
            totalWordsStudied = totalWordsStudied
        )
    }
}
