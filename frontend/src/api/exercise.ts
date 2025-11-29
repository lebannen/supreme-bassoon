import {BaseAPI} from './base'
import type {
    AttemptResult,
    Exercise,
    ExerciseSummary,
    SpeakingAttemptResult,
    SubmitAttemptRequest,
    UserProgress,
} from '@/types/exercise'
import type {UserStats} from '@/types/stats'

export interface ExerciseFilters {
    module?: number
    topic?: string
    type?: string
}

/**
 * API client for exercises and exercise attempts
 */
export class ExerciseAPI extends BaseAPI {
    /**
     * Get exercises with optional filters
     */
    async getExercises(
        languageCode: string,
        filters?: ExerciseFilters
    ): Promise<ExerciseSummary[]> {
        const params = new URLSearchParams({languageCode})
        if (filters?.module) params.append('module', filters.module.toString())
        if (filters?.topic) params.append('topic', filters.topic)
        if (filters?.type) params.append('type', filters.type)

        return this.get<ExerciseSummary[]>(`/api/exercises?${params}`)
    }

    /**
     * Get a specific exercise by ID
     */
    async getExercise(id: number): Promise<Exercise> {
        return this.get<Exercise>(`/api/exercises/${id}`)
    }

    /**
     * Submit an exercise attempt
     */
    async submitAttempt(
        exerciseId: number,
        request: SubmitAttemptRequest
    ): Promise<AttemptResult> {
        return this.post<AttemptResult>(`/api/exercises/${exerciseId}/attempt`, request)
    }

    /**
     * Get user's progress for a specific exercise
     */
    async getProgress(exerciseId: number): Promise<UserProgress | null> {
        try {
            return await this.get<UserProgress>(`/api/exercises/${exerciseId}/progress`)
        } catch (error) {
            // Return null if no progress found (404)
            if (error instanceof Error && error.message.includes('404')) {
                return null
            }
            throw error
        }
    }

    /**
     * Get user's exercise statistics
     */
    async getStats(): Promise<UserStats> {
        return this.get<UserStats>('/api/exercises/stats')
    }

    /**
     * Submit a speaking exercise attempt
     */
    async submitSpeakingAttempt(
        exerciseId: number,
        audioBlob: Blob
    ): Promise<SpeakingAttemptResult> {
        const formData = new FormData()
        formData.append('audio', audioBlob, 'recording.webm')

        return this.postFormData<SpeakingAttemptResult>(
            `/api/exercises/${exerciseId}/speaking`,
            formData
        )
    }

    /**
     * Validate speaking directly (for vocabulary practice, not tied to an exercise)
     */
    async validateSpeaking(
        audioBlob: Blob,
        expectedText: string,
        targetLanguage: string,
        acceptableVariations?: string[]
    ): Promise<SpeakingAttemptResult> {
        const formData = new FormData()
        formData.append('audio', audioBlob, 'recording.webm')
        formData.append('expectedText', expectedText)
        formData.append('targetLanguage', targetLanguage)
        if (acceptableVariations) {
            acceptableVariations.forEach((v) => formData.append('acceptableVariations', v))
        }

        return this.postFormData<SpeakingAttemptResult>('/api/exercises/speaking/validate', formData)
    }
}
