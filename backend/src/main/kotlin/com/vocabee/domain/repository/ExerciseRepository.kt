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

    fun findByLanguageCodeAndModuleNumber(
        languageCode: String,
        moduleNumber: Int
    ): List<Exercise>

    fun findByExerciseType_TypeKey(typeKey: String): List<Exercise>

    @Query("""
        SELECT e FROM Exercise e
        WHERE e.languageCode = :languageCode
        AND (:moduleNumber IS NULL OR e.moduleNumber = :moduleNumber)
        AND (:topic IS NULL OR e.topic = :topic)
        AND (:typeKey IS NULL OR e.exerciseType.typeKey = :typeKey)
        AND e.isPublished = true
        ORDER BY e.moduleNumber, e.topic, e.id
    """)
    fun findByFilters(
        @Param("languageCode") languageCode: String,
        @Param("moduleNumber") moduleNumber: Int?,
        @Param("topic") topic: String?,
        @Param("typeKey") typeKey: String?
    ): List<Exercise>
}
