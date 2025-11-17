/**
 * User statistics types
 */

export interface RecentActivity {
    exerciseId: number
    exerciseTitle: string
    exerciseType: string
    score: number
    isCorrect: boolean
    completedAt: string
}

export interface ModuleProgressSummary {
    moduleNumber: number
    totalExercises: number
    masteredExercises: number
    completionPercentage: number
}

export interface LanguageProgress {
    languageCode: string
    totalExercises: number
    completedExercises: number
    masteredExercises: number
    averageScore: number | null
    moduleProgress: Record<number, ModuleProgressSummary>
}

export interface UserStats {
    totalExercisesAvailable: number
    totalExercisesCompleted: number
    totalExercisesMastered: number
    overallAverageScore: number | null
    totalTimeSpentSeconds: number
    totalAttempts: number
    currentStreak: number
    longestStreak: number
    recentActivity: RecentActivity[]
    progressByLanguage: Record<string, LanguageProgress>
}
