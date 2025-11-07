package com.vocabee.domain.repository

import com.vocabee.domain.model.Word
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface WordRepository : JpaRepository<Word, Long> {

    fun findByLanguageCodeAndLemma(languageCode: String, lemma: String): Word?

    fun findByLanguageCode(languageCode: String): List<Word>

    @Query("""
        SELECT w FROM Word w
        WHERE w.languageCode = :languageCode
        AND LOWER(w.normalized) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY
          CASE
            WHEN LOWER(w.lemma) = LOWER(:query) THEN 0
            WHEN LOWER(w.lemma) LIKE LOWER(CONCAT(:query, '%')) THEN 1
            WHEN LOWER(w.normalized) = LOWER(:query) THEN 2
            WHEN LOWER(w.normalized) LIKE LOWER(CONCAT(:query, '%')) THEN 3
            ELSE 4
          END,
          w.frequencyRank NULLS LAST,
          w.lemma
        LIMIT 2000
    """)
    fun searchByLemma(
        @Param("languageCode") languageCode: String,
        @Param("query") query: String
    ): List<Word>

    @Query("""
        SELECT DISTINCT w FROM Word w
        LEFT JOIN FETCH w.definitions
        WHERE w.languageCode = :languageCode
        AND w.lemma = :lemma
    """)
    fun findAllWithDefinitions(
        @Param("languageCode") languageCode: String,
        @Param("lemma") lemma: String
    ): List<Word>

    fun findByIsInflectedFormTrueAndLemmaId(lemmaId: Long): List<Word>

    @Query("""
        SELECT w FROM Word w
        WHERE w.languageCode = :languageCode
        AND w.isInflectedForm = false
        ORDER BY w.frequencyRank NULLS LAST
        LIMIT :limit
    """)
    fun findLemmasByLanguage(
        @Param("languageCode") languageCode: String,
        @Param("limit") limit: Int
    ): List<Word>
}
