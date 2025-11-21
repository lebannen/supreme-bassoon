package com.vocabee.domain.repository

import com.vocabee.domain.model.WordSetItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface WordSetItemRepository : JpaRepository<WordSetItem, Long> {
    fun findByWordSetIdOrderByDisplayOrderAsc(wordSetId: Long): List<WordSetItem>
    fun countByWordSetId(wordSetId: Long): Long

    @Query(
        """
        SELECT wsi.wordSet.id, COUNT(uv.id)
        FROM WordSetItem wsi
        JOIN UserVocabulary uv ON wsi.word.id = uv.word.id
        WHERE uv.user.id = :userId AND wsi.wordSet.id IN :wordSetIds
        GROUP BY wsi.wordSet.id
    """
    )
    fun countUserVocabularyInSets(
        @Param("userId") userId: Long,
        @Param("wordSetIds") wordSetIds: List<Long>
    ): List<Array<Any>>
}
