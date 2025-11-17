import {BaseAPI} from './base'
import type {Language, SearchResult, Word} from '@/types/dictionary'

/**
 * API client for dictionary word lookup and search
 */
export class DictionaryAPI extends BaseAPI {
    /**
     * Get all available languages in the dictionary
     */
    async getLanguages(): Promise<Language[]> {
        return this.get<Language[]>('/api/v1/languages')
    }

    /**
     * Search for words in a specific language
     */
    async searchWords(languageCode: string, query: string): Promise<SearchResult> {
        if (!query.trim()) {
            return {words: [], total: 0}
        }

        const url = `/api/v1/search?lang=${encodeURIComponent(languageCode)}&q=${encodeURIComponent(query)}`
        return this.get<SearchResult>(url)
    }

    /**
     * Get detailed information about a specific word
     */
    async getWord(languageCode: string, lemma: string): Promise<Word | null> {
        try {
            return await this.get<Word>(`/api/v1/words/${languageCode}/${encodeURIComponent(lemma)}`)
        } catch (error) {
            // Return null if word not found (404)
            if (error instanceof Error && error.message.includes('404')) {
                return null
            }
            throw error
        }
    }
}
