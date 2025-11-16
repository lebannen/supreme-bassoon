package com.vocabee.domain.repository

import com.vocabee.domain.model.Episode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EpisodeRepository : JpaRepository<Episode, Long> {
    fun findByModuleIdOrderByEpisodeNumber(moduleId: Long): List<Episode>
    fun findByModuleIdAndEpisodeNumber(moduleId: Long, episodeNumber: Int): Episode?
    fun deleteByModuleId(moduleId: Long)
}
