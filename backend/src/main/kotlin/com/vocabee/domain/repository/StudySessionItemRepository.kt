package com.vocabee.domain.repository

import com.vocabee.domain.model.ItemStatus
import com.vocabee.domain.model.StudySessionItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface StudySessionItemRepository : JpaRepository<StudySessionItem, Long> {

    // Find all items for a session
    fun findBySessionId(sessionId: Long): List<StudySessionItem>

    // Find items by status
    fun findBySessionIdAndStatus(sessionId: Long, status: ItemStatus): List<StudySessionItem>

    // Find incomplete items (NEW or LEARNING)
    @Query("SELECT i FROM StudySessionItem i WHERE i.session.id = :sessionId AND i.status IN ('NEW', 'LEARNING')")
    fun findIncompleteBySessionId(@Param("sessionId") sessionId: Long): List<StudySessionItem>

    // Count items by status
    fun countBySessionIdAndStatus(sessionId: Long, status: ItemStatus): Int

    // Find items excluding a specific word (to avoid showing the same word twice in a row)
    @Query("""
        SELECT i FROM StudySessionItem i
        WHERE i.session.id = :sessionId
        AND i.status IN ('NEW', 'LEARNING')
        AND i.word.id != :excludeWordId
    """)
    fun findIncompleteExcludingWord(
        @Param("sessionId") sessionId: Long,
        @Param("excludeWordId") excludeWordId: Long
    ): List<StudySessionItem>
}
