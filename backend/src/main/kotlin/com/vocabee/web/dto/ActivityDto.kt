package com.vocabee.web.dto

// ============================================================================
// ACTIVITY DTOs
// ============================================================================

data class RecordActivityRequest(
    val type: String,  // "word", "exercise", "episode"
    val count: Int = 1
)

data class DailyActivityDto(
    val date: String,
    val wordsReviewed: Int,
    val exercisesCompleted: Int,
    val episodesCompleted: Int,
    val studyTimeSeconds: Int = 0,
    val goalsCompleted: Int
)

data class DailyGoalDto(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val target: Int,
    val current: Int,
    val completed: Boolean
)

data class UserActivitySummaryDto(
    val streak: Int,
    val totalWordsReviewed: Int,
    val totalExercisesCompleted: Int,
    val totalEpisodesCompleted: Int,
    val totalStudyTimeSeconds: Int,
    val activeDays: Int,
    val recentActivity: List<DailyActivityDto>
)

data class DailyGoalsResponse(
    val goals: List<DailyGoalDto>,
    val todayActivity: DailyActivityDto,
    val streak: Int
)
