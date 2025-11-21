package com.vocabee.service

import com.vocabee.domain.model.StudySessionAttempt
import com.vocabee.domain.repository.StudySessionRepository
import com.vocabee.web.dto.AnswerRequest
import com.vocabee.web.dto.AnswerResponse
import com.vocabee.web.dto.NextCardResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SessionOrchestratorService(
    private val studySessionRepository: StudySessionRepository,
    private val cardSelectionService: CardSelectionService,
    private val sessionDtoMapper: SessionDtoMapper,
    private val sessionManagerService: SessionManagerService
) {

    @Transactional
    fun getNextCard(sessionId: Long, userId: Long): NextCardResponse? {
        val session = sessionManagerService.findSessionForUser(sessionId, userId)
        if (session.status != com.vocabee.domain.model.SessionStatus.ACTIVE) {
            throw IllegalStateException("Session is not active")
        }

        val nextItem = cardSelectionService.selectNextCard(session.items, session.lastShownWordId) ?: return null

        session.lastShownWordId = nextItem.word.id
        studySessionRepository.save(session)

        return sessionDtoMapper.toNextCardResponse(nextItem, session)
    }

    @Transactional
    fun submitAnswer(sessionId: Long, userId: Long, request: AnswerRequest): AnswerResponse {
        val session = sessionManagerService.findSessionForUser(sessionId, userId)
        if (session.status != com.vocabee.domain.model.SessionStatus.ACTIVE) {
            throw IllegalStateException("Session is not active")
        }

        val item = session.items.find { it.id == request.cardId }
            ?: throw IllegalArgumentException("Card not found in session: ${request.cardId}")

        item.attemptsList.add(
            StudySessionAttempt(
                sessionItem = item,
                wasCorrect = request.correct,
                responseTimeMs = request.responseTimeMs
            )
        )
        item.recordAnswer(request.correct)
        session.recordAnswer(request.correct)

        val itemCompleted = item.isCompleted
        if (itemCompleted) {
            session.completeWord()
        }

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
}
