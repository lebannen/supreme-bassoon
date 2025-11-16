package com.vocabee.domain.repository

import com.vocabee.domain.model.EpisodeContentItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EpisodeContentItemRepository : JpaRepository<EpisodeContentItem, Long> {
    fun findByEpisodeIdOrderByOrderIndex(episodeId: Long): List<EpisodeContentItem>
    fun deleteByEpisodeId(episodeId: Long)
}
