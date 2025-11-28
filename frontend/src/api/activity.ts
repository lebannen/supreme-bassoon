import {BaseAPI} from './base'

export interface DailyActivityDto {
    date: string
    wordsReviewed: number
    exercisesCompleted: number
    episodesCompleted: number
    studyTimeSeconds: number
    goalsCompleted: number
}

export interface DailyGoalDto {
    id: string
    title: string
    description: string
    icon: string
    target: number
    current: number
    completed: boolean
}

export interface DailyGoalsResponse {
    goals: DailyGoalDto[]
    todayActivity: DailyActivityDto
    streak: number
}

export interface UserActivitySummaryDto {
    streak: number
    totalWordsReviewed: number
    totalExercisesCompleted: number
    totalEpisodesCompleted: number
    totalStudyTimeSeconds: number
    activeDays: number
    recentActivity: DailyActivityDto[]
}

export type ActivityType = 'word' | 'exercise' | 'episode'

/**
 * API client for user activity tracking
 */
export class ActivityAPI extends BaseAPI {
    /**
     * Record a user activity
     */
    async recordActivity(type: ActivityType, count: number = 1): Promise<DailyActivityDto> {
        return this.post<DailyActivityDto>('/api/activity/record', {type, count})
    }

    /**
     * Get today's activity
     */
    async getTodayActivity(): Promise<DailyActivityDto> {
        return this.get<DailyActivityDto>('/api/activity/today')
    }

    /**
     * Get daily goals with today's progress
     */
    async getDailyGoals(): Promise<DailyGoalsResponse> {
        return this.get<DailyGoalsResponse>('/api/activity/goals')
    }

    /**
     * Get activity summary for the last N days
     */
    async getActivitySummary(days: number = 30): Promise<UserActivitySummaryDto> {
        return this.get<UserActivitySummaryDto>(`/api/activity/summary?days=${days}`)
    }

    /**
     * Get current streak
     */
    async getStreak(): Promise<number> {
        const response = await this.get<{ streak: number }>('/api/activity/streak')
        return response.streak
    }
}

export const activityAPI = new ActivityAPI()
