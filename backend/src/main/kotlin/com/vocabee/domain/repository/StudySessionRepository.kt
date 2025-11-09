package com.vocabee.domain.repository

import com.vocabee.domain.model.SessionStatus
import com.vocabee.domain.model.StudySession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface StudySessionRepository : JpaRepository<StudySession, Long> {

    // Find active session for a user
    fun findByUserIdAndStatus(userId: Long, status: SessionStatus): StudySession?

    // Find user's session history
    fun findByUserIdOrderByStartedAtDesc(userId: Long): List<StudySession>

    // Find user's completed sessions
    fun findByUserIdAndStatusOrderByCompletedAtDesc(userId: Long, status: SessionStatus): List<StudySession>

    // Check if user has an active session
    fun existsByUserIdAndStatus(userId: Long, status: SessionStatus): Boolean

    // Find sessions within a date range
    @Query("SELECT s FROM StudySession s WHERE s.user.id = :userId AND s.startedAt BETWEEN :startDate AND :endDate")
    fun findByUserIdAndDateRange(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): List<StudySession>
}
