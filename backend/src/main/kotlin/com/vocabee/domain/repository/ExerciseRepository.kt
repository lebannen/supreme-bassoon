package com.vocabee.domain.repository

import com.vocabee.domain.model.Exercise
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ExerciseRepository : JpaRepository<Exercise, Long> {
    fun findByLanguageCodeAndIsPublished(
        languageCode: String,
        isPublished: Boolean = true
    ): List<Exercise>

    fun findByExerciseType_TypeKey(typeKey: String): List<Exercise>

    fun findByTitleAndLanguageCode(
        title: String,
        languageCode: String
    ): Exercise?

    @Query("""
        SELECT e FROM Exercise e
        WHERE e.languageCode = :languageCode
        AND (:typeKey IS NULL OR e.exerciseType.typeKey = :typeKey)
        AND e.isPublished = true
        AND (e.exerciseType.typeKey != 'listening' OR CAST(FUNCTION('jsonb_extract_path_text', e.content, 'audioUrl') AS string) IS NOT NULL)
        ORDER BY e.id
    """)
    fun findByFilters(
        @Param("languageCode") languageCode: String,
        @Param("typeKey") typeKey: String?
    ): List<Exercise>

    @Query("""
        SELECT e FROM Exercise e
        WHERE e.isPublished = true
        AND (e.exerciseType.typeKey != 'listening' OR CAST(FUNCTION('jsonb_extract_path_text', e.content, 'audioUrl') AS string) IS NOT NULL)
    """)
    fun findAllPublishedAndValid(): List<Exercise>

    @Query(
        """
        SELECT COUNT(e) FROM Exercise e
        WHERE e.isPublished = true
        AND (e.exerciseType.typeKey != 'listening' OR CAST(FUNCTION('jsonb_extract_path_text', e.content, 'audioUrl') AS string) IS NOT NULL)
    """
    )
    fun countPublishedAndValid(): Long

    @Query(
        """
        SELECT e.languageCode, COUNT(e)
        FROM Exercise e
        WHERE e.isPublished = true
        AND (e.exerciseType.typeKey != 'listening' OR CAST(FUNCTION('jsonb_extract_path_text', e.content, 'audioUrl') AS string) IS NOT NULL)
        GROUP BY e.languageCode
    """
    )
    fun countPublishedAndValidByLanguage(): List<Array<Any>>
}
