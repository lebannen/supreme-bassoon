import {BaseAPI} from './base'
import type {AddWordToVocabularyRequest, VocabularyWord} from '@/types/vocabulary'

/**
 * API client for user's personal vocabulary
 */
export class VocabularyAPI extends BaseAPI {
    /**
     * Get all words in user's vocabulary
     */
    async getUserVocabulary(): Promise<VocabularyWord[]> {
        return this.get<VocabularyWord[]>('/api/vocabulary')
    }

    /**
     * Add a word to user's vocabulary
     */
    async addWord(request: AddWordToVocabularyRequest): Promise<VocabularyWord> {
        return this.post<VocabularyWord>('/api/vocabulary', request)
    }

    /**
     * Remove a word from user's vocabulary
     */
    async removeWord(wordId: number): Promise<void> {
        return this.delete<void>(`/api/vocabulary/${wordId}`)
    }

    /**
     * Check if a word is in user's vocabulary
     */
    async checkIfWordInVocabulary(wordId: number): Promise<{ inVocabulary: boolean }> {
        return this.get<{ inVocabulary: boolean }>(`/api/vocabulary/check/${wordId}`)
    }
}
