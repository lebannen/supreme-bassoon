package com.vocabee.domain.repository

import com.vocabee.domain.model.UserVocabulary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserVocabularyRepository : JpaRepository<UserVocabulary, Long> {
    fun findByUserId(userId: Long): List<UserVocabulary>
    fun findByUserIdOrderByAddedAtDesc(userId: Long): List<UserVocabulary>
    fun findByUserIdAndWordId(userId: Long, wordId: Long): UserVocabulary?
    fun existsByUserIdAndWordId(userId: Long, wordId: Long): Boolean
    fun deleteByUserIdAndWordId(userId: Long, wordId: Long)
}
