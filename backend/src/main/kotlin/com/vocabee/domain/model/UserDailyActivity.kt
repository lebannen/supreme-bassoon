package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_daily_activity",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "activity_date"])]
)
data class UserDailyActivity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "activity_date", nullable = false)
    val activityDate: LocalDate,

    @Column(name = "words_reviewed", nullable = false)
    var wordsReviewed: Int = 0,

    @Column(name = "exercises_completed", nullable = false)
    var exercisesCompleted: Int = 0,

    @Column(name = "episodes_completed", nullable = false)
    var episodesCompleted: Int = 0,

    @Column(name = "study_time_seconds", nullable = false)
    var studyTimeSeconds: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun incrementWords(count: Int = 1) {
        wordsReviewed += count
        updatedAt = LocalDateTime.now()
    }

    fun incrementExercises(count: Int = 1) {
        exercisesCompleted += count
        updatedAt = LocalDateTime.now()
    }

    fun incrementEpisodes(count: Int = 1) {
        episodesCompleted += count
        updatedAt = LocalDateTime.now()
    }

    fun addStudyTime(seconds: Int) {
        studyTimeSeconds += seconds
        updatedAt = LocalDateTime.now()
    }
}
