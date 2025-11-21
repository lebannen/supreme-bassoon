package com.vocabee.service

import com.vocabee.domain.model.UserVocabulary
import com.vocabee.domain.repository.UserRepository
import com.vocabee.domain.repository.UserVocabularyRepository
import com.vocabee.domain.repository.WordRepository
import com.vocabee.web.dto.VocabularyWordDto
import com.vocabee.web.dto.WordSummaryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserVocabularyService(
    private val userVocabularyRepository: UserVocabularyRepository,
    private val userRepository: UserRepository,
    private val wordRepository: WordRepository
) {

    @Transactional
    fun addWordToVocabulary(userId: Long, wordId: Long, notes: String?): VocabularyWordDto {
        val existing = userVocabularyRepository.findByUserIdAndWordId(userId, wordId)
        if (existing != null) {
            existing.notes = notes
            val updated = userVocabularyRepository.save(existing)
            return toVocabularyWordDto(updated)
        }

        val user = userRepository.getReferenceById(userId)
        val word = wordRepository.getReferenceById(wordId)

        val vocabularyEntry = UserVocabulary(
            user = user,
            word = word,
            notes = notes
        )

        val saved = userVocabularyRepository.save(vocabularyEntry)
        return toVocabularyWordDto(saved)
    }

    @Transactional(readOnly = true)
    fun getUserVocabulary(userId: Long): List<VocabularyWordDto> {
        val vocabularyEntries = userVocabularyRepository.findByUserIdWithWordOrderByAddedAtDesc(userId)
        return vocabularyEntries.map { toVocabularyWordDto(it) }
    }

    @Transactional
    fun removeWordFromVocabulary(userId: Long, wordId: Long): Boolean {
        return userVocabularyRepository.deleteByUserIdAndWordId(userId, wordId) > 0
    }

    fun isWordInVocabulary(userId: Long, wordId: Long): Boolean {
        return userVocabularyRepository.existsByUserIdAndWordId(userId, wordId)
    }

    private fun toVocabularyWordDto(userVocabulary: UserVocabulary): VocabularyWordDto {
        val word = userVocabulary.word
        return VocabularyWordDto(
            vocabularyId = userVocabulary.id!!,
            word = WordSummaryDto(
                id = word.id!!,
                lemma = word.lemma,
                partOfSpeech = word.partOfSpeech,
                frequencyRank = word.frequencyRank
            ),
            notes = userVocabulary.notes,
            addedAt = userVocabulary.addedAt.toString()
        )
    }
}
