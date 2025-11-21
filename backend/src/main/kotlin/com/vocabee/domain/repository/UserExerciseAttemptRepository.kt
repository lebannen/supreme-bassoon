package com.vocabee.domain.repository

import com.vocabee.domain.model.UserExerciseAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

// Data classes for custom query results
data class UserStatsSummary(
    val totalAttempts: Long,
    val totalTimeSpent: Long,
    val averageScore: Double?
)

data class RecentActivityItem(
    val exerciseId: Long,
    val exerciseTitle: String,
    val exerciseType: String,
    val score: Double?,
    val isCorrect: Boolean?,
    val completedAt: LocalDateTime
)

data class LanguageStats(
    val completed: Long,
    val mastered: Long,
    val averageScore: Double?
)

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

    @Query("""
        SELECT new com.vocabee.domain.repository.UserStatsSummary(
            COUNT(a),
            COALESCE(SUM(a.durationSeconds), 0),
            AVG(a.score)
        )
        FROM UserExerciseAttempt a
        WHERE a.userId = :userId
    """
    )
    fun getUserStatsSummary(@Param("userId") userId: Long): UserStatsSummary

    @Query(
        """
        SELECT new com.vocabee.domain.repository.RecentActivityItem(
            a.exerciseId,
            e.title,
            e.exerciseType.typeKey,
            a.score,
            a.isCorrect,
            a.completedAt
        )
        FROM UserExerciseAttempt a JOIN Exercise e ON a.exerciseId = e.id
        WHERE a.userId = :userId AND a.completedAt IS NOT NULL
        ORDER BY a.completedAt DESC
    """
    )
    fun findRecentActivity(@Param("userId") userId: Long): List<RecentActivityItem>

    @Query(
        """
        SELECT DISTINCT CAST(a.completedAt AS LocalDate)
        FROM UserExerciseAttempt a
        WHERE a.userId = :userId AND a.completedAt IS NOT NULL
        ORDER BY CAST(a.completedAt AS LocalDate) DESC
    """)
    fun findActiveDays(@Param("userId") userId: Long): List<LocalDate>

    @Query(
        """
        SELECT e.languageCode as language,
               new com.vocabee.domain.repository.LanguageStats(
                   COUNT(DISTINCT a.exerciseId),
                   SUM(CASE WHEN a.score >= 80.0 THEN 1 ELSE 0 END),
                   AVG(a.score)
               )
        FROM UserExerciseAttempt a JOIN Exercise e ON a.exerciseId = e.id
        WHERE a.userId = :userId
        GROUP BY e.languageCode
    """
    )
    fun getStatsByLanguage(@Param("userId") userId: Long): List<Map<String, Any>>

    @Query(
        """
        SELECT a.exerciseId, MAX(a.score)
        FROM UserExerciseAttempt a
        WHERE a.userId = :userId AND a.completedAt IS NOT NULL
        GROUP BY a.exerciseId
    """
    )
    fun findBestScoresByExercise(@Param("userId") userId: Long): List<Array<Any>>
}
