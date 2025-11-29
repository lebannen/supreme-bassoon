package com.vocabee.domain.repository

import com.vocabee.domain.model.ReviewStatus
import com.vocabee.domain.model.VocabularyCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VocabularyCardRepository : JpaRepository<VocabularyCard, Long> {

    fun findByLanguageCodeAndLemmaAndPartOfSpeech(
        languageCode: String,
        lemma: String,
        partOfSpeech: String?
    ): VocabularyCard?

    fun findByLanguageCodeAndLemma(languageCode: String, lemma: String): List<VocabularyCard>

    fun findByWordSetId(wordSetId: Long): List<VocabularyCard>

    fun findByLanguageCodeAndReviewStatus(
        languageCode: String,
        reviewStatus: ReviewStatus
    ): List<VocabularyCard>

    @Query(
        """
        SELECT vc FROM VocabularyCard vc
        WHERE vc.languageCode = :languageCode
        AND LOWER(vc.lemma) LIKE LOWER(CONCAT(:query, '%'))
        ORDER BY vc.frequencyRank ASC NULLS LAST, vc.lemma ASC
    """
    )
    fun searchByLemmaPrefix(languageCode: String, query: String): List<VocabularyCard>

    fun existsByLanguageCodeAndLemmaAndPartOfSpeech(
        languageCode: String,
        lemma: String,
        partOfSpeech: String?
    ): Boolean

    fun countByWordSetId(wordSetId: Long): Long

    fun countByLanguageCode(languageCode: String): Long
}
