package com.vocabee.domain.repository

import com.vocabee.domain.model.UserEpisodeProgress
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserEpisodeProgressRepository : JpaRepository<UserEpisodeProgress, Long> {
    fun findByUserIdAndEpisodeId(userId: Long, episodeId: Long): UserEpisodeProgress?
    fun findByUserId(userId: Long): List<UserEpisodeProgress>

    @Query(
        """
        SELECT COUNT(uep) FROM UserEpisodeProgress uep
        JOIN Episode e ON e.id = uep.episodeId
        JOIN Module m ON m.id = e.moduleId
        WHERE uep.userId = :userId AND m.courseId = :courseId AND uep.isCompleted = true
    """
    )
    fun countCompletedEpisodesByCourse(userId: Long, courseId: Long): Int

    @Query(
        """
        SELECT uep FROM UserEpisodeProgress uep
        JOIN Episode e ON e.id = uep.episodeId
        JOIN Module m ON m.id = e.moduleId
        WHERE uep.userId = :userId AND m.courseId = :courseId
    """
    )
    fun findByUserIdAndCourseId(userId: Long, courseId: Long): List<UserEpisodeProgress>
}
