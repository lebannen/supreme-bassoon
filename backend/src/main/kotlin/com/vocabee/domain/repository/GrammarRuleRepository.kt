package com.vocabee.domain.repository

import com.vocabee.domain.model.GrammarRule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GrammarRuleRepository : JpaRepository<GrammarRule, Long> {
    fun findByLanguageCodeAndCefrLevel(languageCode: String, cefrLevel: String): List<GrammarRule>
}
