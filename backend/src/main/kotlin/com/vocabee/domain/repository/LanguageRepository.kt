package com.vocabee.domain.repository

import com.vocabee.domain.model.Language
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LanguageRepository : JpaRepository<Language, Int> {

    fun findByCode(code: String): Language?

    fun findAllByOrderByNameAsc(): List<Language>
}
