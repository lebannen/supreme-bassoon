package com.vocabee.domain.repository

import com.vocabee.domain.model.WordSetItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WordSetItemRepository : JpaRepository<WordSetItem, Long> {
    fun findByWordSetIdOrderByDisplayOrderAsc(wordSetId: Long): List<WordSetItem>
    fun countByWordSetId(wordSetId: Long): Long
}
