package com.vocabee.domain.repository

import com.vocabee.domain.model.UserCourseEnrollment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserCourseEnrollmentRepository : JpaRepository<UserCourseEnrollment, Long> {
    fun findByUserId(userId: Long): List<UserCourseEnrollment>
    fun findByUserIdAndCourseId(userId: Long, courseId: Long): UserCourseEnrollment?
    fun existsByUserIdAndCourseId(userId: Long, courseId: Long): Boolean

    @Query(
        """
        SELECT e FROM UserCourseEnrollment e
        WHERE e.userId = :userId
        ORDER BY e.lastActivityAt DESC
    """
    )
    fun findByUserIdOrderByLastActivityDesc(userId: Long): List<UserCourseEnrollment>
}
