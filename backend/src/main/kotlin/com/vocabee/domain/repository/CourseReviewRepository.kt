package com.vocabee.domain.repository

import com.vocabee.domain.model.CourseReview
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CourseReviewRepository : JpaRepository<CourseReview, Long> {
    fun findByCourseIdOrderByCreatedAtDesc(courseId: Long): List<CourseReview>
    fun findFirstByCourseIdOrderByCreatedAtDesc(courseId: Long): CourseReview?
}
