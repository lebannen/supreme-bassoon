package com.vocabee.domain.repository

import com.vocabee.domain.model.Course
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CourseRepository : JpaRepository<Course, Long> {
    fun findBySlug(slug: String): Course?
    fun findByLanguageCodeAndIsPublished(languageCode: String, isPublished: Boolean = true): List<Course>
    fun findByIsPublished(isPublished: Boolean = true): List<Course>
}
