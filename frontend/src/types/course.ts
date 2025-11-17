/**
 * Course, Module, and Episode types
 */

export interface Course {
    id: number
    slug: string
    name: string
    languageCode: string
    cefrLevel: string
    description: string | null
    objectives: string[]
    estimatedHours: number
    totalModules: number
    totalEpisodes: number
    isPublished?: boolean
}

export interface CourseSummary {
    id: number
    slug: string
    name: string
    languageCode: string
    cefrLevel: string
    description: string
    estimatedHours: number
    totalModules: number
    totalEpisodes: number
}

export interface ModuleSummary {
    id: number
    moduleNumber: number
    title: string
    theme: string | null
    estimatedMinutes: number
    totalEpisodes: number
}

export interface CourseDetail extends Course {
    modules: ModuleSummary[]
}

export interface EpisodeSummary {
    id: number
    episodeNumber: number
    type: string
    title: string
    estimatedMinutes: number
    hasAudio: boolean
    totalExercises: number
}

export interface Module {
    id: number
    courseId: number
    moduleNumber: number
    title: string
    theme: string | null
    description: string | null
    objectives: string[]
    estimatedMinutes: number
    episodes: EpisodeSummary[]
}

export interface ContentItem {
    id: number
    itemType: string
    contentData: unknown
    sequenceNumber: number
    isRequired?: boolean
    exercise?: {
        id: number
        type: string
        title: string
        instructions: string
        content: any
        estimatedDurationSeconds: number
        pointsValue: number
    }
}

export interface Episode {
    id: number
    moduleId: number
    episodeNumber: number
    type: string
    title: string
    content: string
    audioUrl: string | null
    transcript: string | null
    estimatedMinutes: number
    contentItems: ContentItem[]
}

export interface ExerciseProgress {
    exerciseId: number
    exerciseTitle: string
    exerciseType: string
    isCompleted: boolean
    isMastered: boolean
    bestScore: number | null
    attemptCount: number
    lastAttemptAt: string | null
    // Exercise metadata
    estimatedDurationSeconds?: number
    difficultyRating?: number
    pointsValue?: number
    topic?: string | null
}

export interface ModuleProgress {
    moduleNumber: number
    languageCode: string
    totalExercises: number
    completedExercises: number
    masteredExercises: number
    averageScore: number | null
    totalTimeSpentSeconds: number
    completionPercentage: number
    exercises: ExerciseProgress[]
}
