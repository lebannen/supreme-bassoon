package com.vocabee.service

import com.vocabee.domain.model.StudySession
import com.vocabee.domain.model.StudySessionItem
import com.vocabee.web.dto.*
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class SessionDtoMapper(
    private val cardSelectionService: CardSelectionService,
    private val srsIntervalCalculator: SrsIntervalCalculator
) {

    companion object {
        private const val REQUIRED_CONSECUTIVE_CORRECT = 2
    }

    fun toSessionResponse(session: StudySession): SessionResponse {
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

    fun toNextCardResponse(item: StudySessionItem, session: StudySession): NextCardResponse {
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
                currentInterval = srsIntervalCalculator.formatInterval(userVocab?.currentIntervalHours ?: 20),
                nextReview = userVocab?.nextReviewAt
            )
        )
    }

    fun toSessionSummaryResponse(
        session: StudySession,
        wordsAdvanced: Int,
        wordsReset: Int,
        nextDueCount: Int,
        avgResponseTime: Double?
    ): SessionSummaryResponse {
        val duration = Duration.between(session.startedAt, session.completedAt ?: session.startedAt)
        val minutes = duration.toMinutes()
        val newWords = session.items.count { it.userVocabulary == null || it.userVocabulary!!.reviewCount == 1 }
        val reviewWords = session.items.size - newWords

        return SessionSummaryResponse(
            sessionId = session.id!!,
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

    fun toSessionHistoryItem(session: StudySession): SessionHistoryItemDto {
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
