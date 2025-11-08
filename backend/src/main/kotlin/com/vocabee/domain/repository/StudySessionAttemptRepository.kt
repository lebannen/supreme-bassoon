package com.vocabee.domain.repository

import com.vocabee.domain.model.StudySessionAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface StudySessionAttemptRepository : JpaRepository<StudySessionAttempt, Long> {

    // Find all attempts for a session item
    fun findBySessionItemIdOrderByAttemptedAtAsc(sessionItemId: Long): List<StudySessionAttempt>

    // Count attempts for a session item
    fun countBySessionItemId(sessionItemId: Long): Int

    // Get average response time for a session
    @Query("""
        SELECT AVG(a.responseTimeMs)
        FROM StudySessionAttempt a
        JOIN a.sessionItem i
        WHERE i.session.id = :sessionId
        AND a.responseTimeMs IS NOT NULL
    """)
    fun getAverageResponseTimeForSession(@Param("sessionId") sessionId: Long): Double?
}
