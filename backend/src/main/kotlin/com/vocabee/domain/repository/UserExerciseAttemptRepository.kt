package com.vocabee.domain.repository

import com.vocabee.domain.model.UserExerciseAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserExerciseAttemptRepository : JpaRepository<UserExerciseAttempt, Long> {
    fun findByUserId(userId: Long): List<UserExerciseAttempt>

    fun findByUserIdAndExerciseId(userId: Long, exerciseId: Long): List<UserExerciseAttempt>

    fun findByUserIdOrderByStartedAtDesc(userId: Long): List<UserExerciseAttempt>

    @Query("""
        SELECT COUNT(DISTINCT a.exerciseId)
        FROM UserExerciseAttempt a
        WHERE a.userId = :userId AND a.isCorrect = true
    """)
    fun countCompletedExercisesByUser(@Param("userId") userId: Long): Long
}
