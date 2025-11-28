import {BaseAPI} from './base'
import type {ImportWordSetRequest, ImportWordSetResponse, WordSet, WordSetDetail,} from '@/types/wordSet'

/**
 * API client for word sets
 */
export class WordSetAPI extends BaseAPI {
    /**
     * Get all word sets
     */
    async getAllWordSets(): Promise<WordSet[]> {
        return this.get<WordSet[]>('/api/word-sets')
    }

    /**
     * Get word sets filtered by language
     */
    async getWordSetsByLanguage(languageCode: string): Promise<WordSet[]> {
        return this.get<WordSet[]>(`/api/word-sets/language/${languageCode}`)
    }

    /**
     * Get a specific word set by ID with all words
     */
    async getWordSetById(id: number): Promise<WordSetDetail | null> {
        try {
            return await this.get<WordSetDetail>(`/api/word-sets/${id}`)
        } catch (error) {
            // Return null if word set not found (404)
            if (error instanceof Error && error.message.includes('404')) {
                return null
            }
            throw error
        }
    }

    /**
     * Get word set for a published course module
     */
    async getWordSetByModuleId(moduleId: number): Promise<WordSetDetail | null> {
        try {
            return await this.get<WordSetDetail>(`/api/word-sets/module/${moduleId}`)
        } catch (error) {
            if (error instanceof Error && error.message.includes('404')) {
                return null
            }
            throw error
        }
    }

    /**
     * Get word set for a generation module plan (pipeline preview)
     */
    async getWordSetByGenerationModuleId(modulePlanId: string): Promise<WordSetDetail | null> {
        try {
            return await this.get<WordSetDetail>(`/api/word-sets/generation-module/${modulePlanId}`)
        } catch (error) {
            if (error instanceof Error && error.message.includes('404')) {
                return null
            }
            throw error
        }
    }

    /**
     * Import a word set to user's vocabulary
     */
    async importWordSet(
        id: number,
        request?: ImportWordSetRequest
    ): Promise<ImportWordSetResponse> {
        return this.post<ImportWordSetResponse>(`/api/word-sets/${id}/import`, request)
    }

    /**
     * Load word sets from JSON content
     */
    async loadWordSetsFromJson(file: File): Promise<{ message: string; count: number }> {
        const text = await file.text()
        const jsonData = JSON.parse(text)

        return this.post<{ message: string; count: number }>(
            '/api/word-sets/load-from-json-content',
            {jsonContent: jsonData}
        )
    }
}
