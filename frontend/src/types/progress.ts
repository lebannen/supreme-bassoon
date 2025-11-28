export interface UserStats {
    streak: number
    wordsLearned: number
    timeThisWeek: string
    coursesInProgress: number
}

export interface DailyGoal {
    id: string
    tier: 1 | 2 | 3
    title: string
    description: string
    target: number
    current: number
    completed: boolean
    icon: string
}

export interface ContinueLearning {
    courseId: number
    courseName: string
    courseSlug: string
    moduleId: number | null
    moduleName: string | null
    episodeId: number | null
    episodeName: string | null
    progress: number // 0-100
}

export interface Enrollment {
    id: number
    courseId: number
    courseName: string
    courseSlug: string
    languageCode: string
    cefrLevel: string
    startedAt: string
    lastAccessedAt: string
    progress: number
    currentModuleId: number | null
    currentModuleName: string | null
    currentEpisodeId: number | null
    currentEpisodeName: string | null
    completedEpisodes: number
    totalEpisodes: number
}

export interface DailyActivity {
    date: string
    wordsReviewed: number
    exercisesCompleted: number
    episodesCompleted: number
    goalsCompleted: number // 0-3
}

export interface EnrollCourseParams {
    id: number
    name: string
    slug: string
    languageCode: string
    cefrLevel: string
    totalEpisodes: number
    firstModuleId?: number
    firstModuleName?: string
    firstEpisodeId?: number
    firstEpisodeName?: string
}
