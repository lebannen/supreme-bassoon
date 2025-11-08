package com.vocabee.domain.repository

import com.vocabee.domain.model.UserImportedWordSet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserImportedWordSetRepository : JpaRepository<UserImportedWordSet, Long> {
    fun findByUserId(userId: Long): List<UserImportedWordSet>
    fun existsByUserIdAndWordSetId(userId: Long, wordSetId: Long): Boolean

    @Query("SELECT uiws.wordSet.id FROM UserImportedWordSet uiws WHERE uiws.user.id = :userId")
    fun findImportedWordSetIdsByUserId(userId: Long): List<Long>
}
