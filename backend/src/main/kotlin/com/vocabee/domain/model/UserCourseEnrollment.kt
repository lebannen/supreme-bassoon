package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_course_enrollments",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "course_id"])]
)
data class UserCourseEnrollment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val courseId: Long,

    val currentModuleId: Long? = null,
    val currentEpisodeId: Long? = null,

    val totalTimeSpentSeconds: Int = 0,
    val completionPercentage: Double = 0.0,

    val enrolledAt: LocalDateTime = LocalDateTime.now(),
    val lastActivityAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null
)
