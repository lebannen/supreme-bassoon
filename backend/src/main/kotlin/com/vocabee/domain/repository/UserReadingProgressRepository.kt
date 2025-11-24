package com.vocabee.domain.repository

import com.vocabee.domain.model.UserReadingProgress
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserReadingProgressRepository : JpaRepository<UserReadingProgress, Long> {
    fun findByUserIdAndTextId(userId: Long, textId: Long): Optional<UserReadingProgress>
    fun findByUserId(userId: Long): List<UserReadingProgress>
    fun findByUserIdAndCompleted(userId: Long, completed: Boolean): List<UserReadingProgress>
    fun deleteByTextId(textId: Long)
}
