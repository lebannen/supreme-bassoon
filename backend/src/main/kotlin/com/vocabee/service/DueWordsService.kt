package com.vocabee.service

import com.vocabee.domain.repository.UserVocabularyRepository
import com.vocabee.web.dto.DueWordDto
import com.vocabee.web.dto.DueWordsList
import com.vocabee.web.dto.DueWordsResponse
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class DueWordsService(
    private val userVocabularyRepository: UserVocabularyRepository,
    private val srsIntervalCalculator: SrsIntervalCalculator
) {

    /**
     * Get count of due words broken down by urgency
     */
    fun getDueWordsCount(userId: Long): DueWordsResponse {
        val now = Instant.now()
        val tomorrow = now.plus(Duration.ofHours(24))

        // Count all words due within next 24 hours (includes overdue and never reviewed)
        val totalDue = userVocabularyRepository.countDueWords(userId, tomorrow)
        val overdue = userVocabularyRepository.countOverdueWords(userId, now)

        val dueToday = userVocabularyRepository.findWordsDueInWindow(userId, now, tomorrow).size

        val threeDaysLater = now.plus(Duration.ofDays(3))
        val dueSoon = userVocabularyRepository.findWordsDueInWindow(userId, tomorrow, threeDaysLater).size

        return DueWordsResponse(
            totalDue = totalDue,
            overdue = overdue,
            dueToday = dueToday,
            dueSoon = dueSoon
        )
    }

    /**
     * Get list of due words with details
     */
    fun getDueWordsList(userId: Long, limit: Int = 50): DueWordsList {
        val now = Instant.now()
        val tomorrow = now.plus(Duration.ofHours(24))
        val dueWords = userVocabularyRepository.findDueWords(userId, tomorrow).take(limit)

        val dueWordDtos = dueWords.map { vocab ->
            val daysOverdue = if (vocab.nextReviewAt != null) {
                Duration.between(vocab.nextReviewAt, now).toDays().toInt()
            } else {
                0
            }

            DueWordDto(
                wordId = vocab.word.id!!,
                lemma = vocab.word.lemma,
                partOfSpeech = vocab.word.partOfSpeech,
                languageCode = vocab.word.languageCode,
                nextReviewAt = vocab.nextReviewAt ?: Instant.EPOCH,
                daysOverdue = daysOverdue,
                reviewCount = vocab.reviewCount,
                currentInterval = srsIntervalCalculator.formatInterval(vocab.currentIntervalHours)
            )
        }

        return DueWordsList(
            words = dueWordDtos,
            totalCount = dueWords.size
        )
    }
}
