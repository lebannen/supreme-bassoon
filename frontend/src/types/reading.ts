/**
 * Reading texts and progress types
 */

export interface ReadingText {
    id: number
    title: string
    content: string
    languageCode: string
    level: string | null
    topic: string | null
    wordCount: number | null
    description: string | null
    estimatedMinutes: number | null
    difficulty: string | null
    isPublished: boolean
    createdAt: string
    updatedAt: string
    author: string | null
    source: string | null
    audioUrl: string | null
}

export interface ReadingProgress {
    id: number | null
    userId: number
    textId: number
    currentPage: number
    totalPages: number
    completed: boolean
    startedAt: string
    completedAt: string | null
    lastReadAt: string
}

export interface TextFilters {
    languageCode?: string
    level?: string
    topic?: string
}
