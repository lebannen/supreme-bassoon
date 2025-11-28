package com.vocabee.domain.repository

import com.vocabee.domain.model.WordSet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WordSetRepository : JpaRepository<WordSet, Long> {
    fun findByLanguageCode(languageCode: String): List<WordSet>
    fun findByLanguageCodeAndLevel(languageCode: String, level: String): List<WordSet>
    fun findByLanguageCodeOrderByLevelAscThemeAsc(languageCode: String): List<WordSet>
    fun existsByNameAndLanguageCode(name: String, languageCode: String): Boolean

    // Module-linked word sets
    fun findByModuleId(moduleId: Long): WordSet?
    fun findByGenerationModulePlanId(generationModulePlanId: UUID): WordSet?
    fun existsByGenerationModulePlanId(generationModulePlanId: UUID): Boolean
    fun deleteByGenerationModulePlanId(generationModulePlanId: UUID)
}
