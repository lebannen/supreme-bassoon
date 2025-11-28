package com.vocabee.domain.repository

import com.vocabee.domain.model.ModuleVocabularyMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ModuleVocabularyMappingRepository : JpaRepository<ModuleVocabularyMapping, Long> {

    fun findByModulePlanId(modulePlanId: UUID): List<ModuleVocabularyMapping>

    fun findByModulePlanIdAndMatched(modulePlanId: UUID, matched: Boolean): List<ModuleVocabularyMapping>

    fun deleteByModulePlanId(modulePlanId: UUID)

    fun countByModulePlanId(modulePlanId: UUID): Long

    fun countByModulePlanIdAndMatched(modulePlanId: UUID, matched: Boolean): Long
}
