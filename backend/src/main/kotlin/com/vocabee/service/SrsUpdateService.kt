package com.vocabee.service

import com.vocabee.domain.model.StudySession
import com.vocabee.domain.model.UserVocabulary
import com.vocabee.domain.repository.UserRepository
import com.vocabee.domain.repository.UserVocabularyRepository
import com.vocabee.domain.repository.WordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SrsUpdateService(
    private val userVocabularyRepository: UserVocabularyRepository,
    private val userRepository: UserRepository,
    private val wordRepository: WordRepository,
    private val srsIntervalCalculator: SrsIntervalCalculator
) {

    data class SrsUpdateResult(val wordsAdvanced: Int, val wordsReset: Int)

    @Transactional
    fun updateUserVocabulary(session: StudySession): SrsUpdateResult {
        var wordsAdvanced = 0
        var wordsReset = 0
        val userVocabToUpdate = mutableListOf<UserVocabulary>()

        session.items.forEach { item ->
            val userVocab = item.userVocabulary ?: findOrCreateUserVocabulary(session.user.id!!, item.word.id!!)
            val wasSuccessful = item.correctCount > item.incorrectCount

            userVocab.reviewCount++
            userVocab.lastReviewedAt = Instant.now()

            if (wasSuccessful) {
                userVocab.consecutiveSuccesses++
                wordsAdvanced++
            } else {
                userVocab.consecutiveSuccesses = 0
                wordsReset++
            }

            val newInterval = srsIntervalCalculator.calculateNextInterval(
                consecutiveSuccesses = userVocab.consecutiveSuccesses,
                wasCorrect = wasSuccessful,
                easeFactor = userVocab.easeFactor
            )

            userVocab.currentIntervalHours = newInterval
            userVocab.nextReviewAt = srsIntervalCalculator.calculateNextReviewDate(newInterval)
            userVocab.easeFactor = srsIntervalCalculator.updateEaseFactor(userVocab.easeFactor, wasSuccessful)

            userVocabToUpdate.add(userVocab)
        }

        if (userVocabToUpdate.isNotEmpty()) {
            userVocabularyRepository.saveAll(userVocabToUpdate)
        }

        return SrsUpdateResult(wordsAdvanced, wordsReset)
    }

    private fun findOrCreateUserVocabulary(userId: Long, wordId: Long): UserVocabulary {
        return userVocabularyRepository.findByUserIdAndWordId(userId, wordId)
            ?: run {
                val user = userRepository.getReferenceById(userId)
                val word = wordRepository.getReferenceById(wordId)
                UserVocabulary(user = user, word = word)
            }
    }
}
