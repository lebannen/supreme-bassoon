package com.vocabee.service

import com.vocabee.domain.model.UserDailyActivity
import com.vocabee.domain.repository.UserDailyActivityRepository
import com.vocabee.web.dto.DailyActivityDto
import com.vocabee.web.dto.DailyGoalDto
import com.vocabee.web.dto.RecordActivityRequest
import com.vocabee.web.dto.UserActivitySummaryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ActivityService(
    private val activityRepository: UserDailyActivityRepository
) {
    companion object {
        // Daily goal targets
        const val WORDS_TARGET = 10
        const val EXERCISES_TARGET = 5
        const val EPISODES_TARGET = 1
    }

    @Transactional
    fun recordActivity(userId: Long, request: RecordActivityRequest): DailyActivityDto {
        val today = LocalDate.now()
        val activity = getOrCreateTodayActivity(userId, today)

        when (request.type) {
            "word" -> activity.incrementWords(request.count)
            "exercise" -> activity.incrementExercises(request.count)
            "episode" -> activity.incrementEpisodes(request.count)
        }

        val saved = activityRepository.save(activity)
        return toDto(saved)
    }

    @Transactional(readOnly = true)
    fun getTodayActivity(userId: Long): DailyActivityDto {
        val today = LocalDate.now()
        val activity = activityRepository.findByUserIdAndActivityDate(userId, today)
        return activity?.let { toDto(it) } ?: createEmptyDto(today)
    }

    @Transactional(readOnly = true)
    fun getDailyGoals(userId: Long): List<DailyGoalDto> {
        val activity = getTodayActivity(userId)
        return listOf(
            DailyGoalDto(
                id = "words",
                title = "Word Review",
                description = "Review $WORDS_TARGET vocabulary words",
                icon = "📚",
                target = WORDS_TARGET,
                current = activity.wordsReviewed,
                completed = activity.wordsReviewed >= WORDS_TARGET
            ),
            DailyGoalDto(
                id = "exercises",
                title = "Exercise Practice",
                description = "Complete $EXERCISES_TARGET exercises",
                icon = "✏️",
                target = EXERCISES_TARGET,
                current = activity.exercisesCompleted,
                completed = activity.exercisesCompleted >= EXERCISES_TARGET
            ),
            DailyGoalDto(
                id = "episode",
                title = "Episode Completion",
                description = "Complete an episode",
                icon = "🎯",
                target = EPISODES_TARGET,
                current = activity.episodesCompleted,
                completed = activity.episodesCompleted >= EPISODES_TARGET
            )
        )
    }

    @Transactional(readOnly = true)
    fun getActivitySummary(userId: Long, days: Int = 30): UserActivitySummaryDto {
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong())
        val activities = activityRepository.findByUserIdAndActivityDateBetweenOrderByActivityDateDesc(
            userId, startDate, today
        )

        val streak = calculateStreak(userId)
        val totalWords = activities.sumOf { it.wordsReviewed }
        val totalExercises = activities.sumOf { it.exercisesCompleted }
        val totalEpisodes = activities.sumOf { it.episodesCompleted }
        val totalStudyTime = activities.sumOf { it.studyTimeSeconds }

        // Calculate days with completed goals
        val daysWithGoals = activities.count { activity ->
            activity.wordsReviewed >= WORDS_TARGET ||
                    activity.exercisesCompleted >= EXERCISES_TARGET ||
                    activity.episodesCompleted >= EPISODES_TARGET
        }

        return UserActivitySummaryDto(
            streak = streak,
            totalWordsReviewed = totalWords,
            totalExercisesCompleted = totalExercises,
            totalEpisodesCompleted = totalEpisodes,
            totalStudyTimeSeconds = totalStudyTime,
            activeDays = daysWithGoals,
            recentActivity = activities.take(7).map { toDto(it) }
        )
    }

    @Transactional(readOnly = true)
    fun calculateStreak(userId: Long): Int {
        val today = LocalDate.now()
        val recentActivities = activityRepository.findRecentActivity(userId, today.minusDays(90))

        if (recentActivities.isEmpty()) return 0

        var streak = 0
        var expectedDate = today

        // Check if today has any activity that counts toward a goal
        val todayActivity = recentActivities.find { it.activityDate == today }
        val todayHasGoalProgress = todayActivity?.let { hasGoalProgress(it) } ?: false

        if (!todayHasGoalProgress) {
            // If no activity today, check if yesterday had activity
            expectedDate = today.minusDays(1)
            val yesterdayActivity = recentActivities.find { it.activityDate == expectedDate }
            if (yesterdayActivity == null || !hasGoalProgress(yesterdayActivity)) {
                return 0
            }
        }

        // Count consecutive days
        for (activity in recentActivities.sortedByDescending { it.activityDate }) {
            if (activity.activityDate == expectedDate && hasGoalProgress(activity)) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else if (activity.activityDate < expectedDate) {
                break
            }
        }

        return streak
    }

    private fun hasGoalProgress(activity: UserDailyActivity): Boolean {
        return activity.wordsReviewed > 0 ||
                activity.exercisesCompleted > 0 ||
                activity.episodesCompleted > 0
    }

    private fun getOrCreateTodayActivity(userId: Long, date: LocalDate): UserDailyActivity {
        return activityRepository.findByUserIdAndActivityDate(userId, date)
            ?: UserDailyActivity(userId = userId, activityDate = date)
    }

    private fun toDto(activity: UserDailyActivity): DailyActivityDto {
        val goalsCompleted = listOf(
            activity.wordsReviewed >= WORDS_TARGET,
            activity.exercisesCompleted >= EXERCISES_TARGET,
            activity.episodesCompleted >= EPISODES_TARGET
        ).count { it }

        return DailyActivityDto(
            date = activity.activityDate.toString(),
            wordsReviewed = activity.wordsReviewed,
            exercisesCompleted = activity.exercisesCompleted,
            episodesCompleted = activity.episodesCompleted,
            studyTimeSeconds = activity.studyTimeSeconds,
            goalsCompleted = goalsCompleted
        )
    }

    private fun createEmptyDto(date: LocalDate): DailyActivityDto {
        return DailyActivityDto(
            date = date.toString(),
            wordsReviewed = 0,
            exercisesCompleted = 0,
            episodesCompleted = 0,
            studyTimeSeconds = 0,
            goalsCompleted = 0
        )
    }
}
