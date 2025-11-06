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
        AND LOWER(w.lemma) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY w.frequencyRank NULLS LAST, w.lemma
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
    fun findWithDefinitions(
        @Param("languageCode") languageCode: String,
        @Param("lemma") lemma: String
    ): Word?

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
