package com.vocabee.domain.repository

import com.vocabee.domain.model.ReadingText
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReadingTextRepository : JpaRepository<ReadingText, Long> {
    fun findByLanguageCode(languageCode: String): List<ReadingText>
    fun findByLanguageCodeAndLevel(languageCode: String, level: String): List<ReadingText>
    fun findByLanguageCodeAndTopic(languageCode: String, topic: String): List<ReadingText>
    fun findByLanguageCodeAndLevelAndTopic(languageCode: String, level: String, topic: String): List<ReadingText>
    fun findByIsPublished(isPublished: Boolean): List<ReadingText>
    fun findByLanguageCodeAndIsPublished(languageCode: String, isPublished: Boolean): List<ReadingText>
}
