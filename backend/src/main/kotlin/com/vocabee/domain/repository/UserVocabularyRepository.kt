package com.vocabee.domain.repository

import com.vocabee.domain.model.UserVocabulary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface UserVocabularyRepository : JpaRepository<UserVocabulary, Long> {
    fun findByUserId(userId: Long): List<UserVocabulary>

    @Query("SELECT uv FROM UserVocabulary uv JOIN FETCH uv.word WHERE uv.user.id = :userId ORDER BY uv.addedAt DESC")
    fun findByUserIdWithWordOrderByAddedAtDesc(@Param("userId") userId: Long): List<UserVocabulary>

    fun findByUserIdOrderByAddedAtDesc(userId: Long): List<UserVocabulary>
    fun findByUserIdAndWordId(userId: Long, wordId: Long): UserVocabulary?
    fun existsByUserIdAndWordId(userId: Long, wordId: Long): Boolean
    fun deleteByUserIdAndWordId(userId: Long, wordId: Long): Long

    @Query("SELECT uv FROM UserVocabulary uv WHERE uv.user.id = :userId AND uv.word.id IN :wordIds")
    fun findByUserIdAndWordIdIn(@Param("userId") userId: Long, @Param("wordIds") wordIds: List<Long>): List<UserVocabulary>

    @Query("""
        SELECT uv FROM UserVocabulary uv
        WHERE uv.user.id = :userId
        AND (uv.nextReviewAt IS NULL OR uv.nextReviewAt <= :cutoffTime)
        ORDER BY uv.nextReviewAt ASC NULLS FIRST
    """)
    fun findDueWords(@Param("userId") userId: Long, @Param("cutoffTime") cutoffTime: Instant): List<UserVocabulary>

    @Query("""
        SELECT COUNT(uv) FROM UserVocabulary uv
        WHERE uv.user.id = :userId
        AND (uv.nextReviewAt IS NULL OR uv.nextReviewAt <= :cutoffTime)
    """)
    fun countDueWords(@Param("userId") userId: Long, @Param("cutoffTime") cutoffTime: Instant): Int

    @Query("""
        SELECT COUNT(uv) FROM UserVocabulary uv
        WHERE uv.user.id = :userId
        AND uv.nextReviewAt IS NOT NULL
        AND uv.nextReviewAt < :currentTime
    """)
    fun countOverdueWords(@Param("userId") userId: Long, @Param("currentTime") currentTime: Instant): Int

    @Query("""
        SELECT uv FROM UserVocabulary uv
        WHERE uv.user.id = :userId
        AND uv.nextReviewAt IS NOT NULL
        AND uv.nextReviewAt BETWEEN :startTime AND :endTime
        ORDER BY uv.nextReviewAt ASC
    """)
    fun findWordsDueInWindow(
        @Param("userId") userId: Long,
        @Param("startTime") startTime: Instant,
        @Param("endTime") endTime: Instant
    ): List<UserVocabulary>
}
