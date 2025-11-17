import {BaseAPI} from './base'
import type {ReadingProgress, ReadingText, TextFilters} from '@/types/reading'

/**
 * API client for reading texts and progress tracking
 */
export class ReadingAPI extends BaseAPI {
    /**
     * Get all reading texts with optional filters
     */
    async getTexts(filters: TextFilters = {}): Promise<ReadingText[]> {
        const params = new URLSearchParams()
        if (filters.languageCode) params.append('languageCode', filters.languageCode)
        if (filters.level) params.append('level', filters.level)
        if (filters.topic) params.append('topic', filters.topic)

        const query = params.toString()
        return this.get<ReadingText[]>(`/api/reading/texts${query ? '?' + query : ''}`)
    }

    /**
     * Get a specific reading text by ID
     */
    async getTextById(textId: number): Promise<ReadingText> {
        return this.get<ReadingText>(`/api/reading/texts/${textId}`)
    }

    /**
     * Get reading progress for a specific text
     */
    async getProgress(textId: number): Promise<ReadingProgress | null> {
        try {
            return await this.get<ReadingProgress>(`/api/reading/texts/${textId}/progress`)
        } catch {
            // Return null if no progress found
            return null
        }
    }

    /**
     * Update reading progress
     */
    async updateProgress(
        textId: number,
        currentPage: number,
        totalPages: number
    ): Promise<ReadingProgress> {
        return this.post<ReadingProgress>(`/api/reading/texts/${textId}/progress`, {
            currentPage,
            totalPages,
        })
    }

    /**
     * Mark a reading text as completed
     */
    async markCompleted(textId: number): Promise<ReadingProgress> {
        return this.post<ReadingProgress>(`/api/reading/texts/${textId}/complete`)
    }
}
