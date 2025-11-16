package com.vocabee.domain.repository

import com.vocabee.domain.model.UserEpisodeProgress
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserEpisodeProgressRepository : JpaRepository<UserEpisodeProgress, Long> {
    fun findByUserIdAndEpisodeId(userId: Long, episodeId: Long): UserEpisodeProgress?
    fun findByUserId(userId: Long): List<UserEpisodeProgress>
}
