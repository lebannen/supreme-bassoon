import {BaseAPI} from '@/api/base'

export interface VocabularyCard {
    id: number
    languageCode: string
    lemma: string
    partOfSpeech: string | null
    ipa: string | null
    audioUrl: string | null
    gender: string | null
    pluralForm: string | null
    verbGroup: string | null
    grammarNotes: string | null
    definitions: string[]
    examples: { sentence: string; translation: string }[] | null
    translations: Record<string, string[]>
    inflections: Record<string, any> | null
    cefrLevel: string | null
    frequencyRank: number | null
    tags: string[] | null
    wordSetId: number | null
    sourceWordId: number | null
    modelUsed: string | null
    generatedAt: string
    reviewedAt: string | null
    reviewStatus: 'PENDING' | 'APPROVED' | 'REJECTED'
    createdAt: string
    updatedAt: string
}

export interface GenerationResult {
    wordSetId: number | null
    wordSetName: string | null
    totalWords: number
    generated: number
    skipped: number
    failed: number
    errors: string[]
}

export interface VocabularyCardStats {
    total: number
}

class VocabularyCardAPI extends BaseAPI {
    /**
     * Generate vocabulary cards for all words in a word set
     */
    async generateForWordSet(wordSetId: number, cefrLevel?: string): Promise<GenerationResult> {
        const params = cefrLevel ? `?cefrLevel=${cefrLevel}` : ''
        return this.post<GenerationResult>(`/api/admin/vocabulary-cards/generate/word-set/${wordSetId}${params}`)
    }

    /**
     * Generate a single vocabulary card
     */
    async generateSingleCard(request: {
        lemma: string
        languageCode: string
        partOfSpeech?: string
        cefrLevel?: string
    }): Promise<VocabularyCard> {
        return this.post<VocabularyCard>('/api/admin/vocabulary-cards/generate/single', request)
    }

    /**
     * Generate vocabulary cards for a batch of words
     */
    async generateBatch(request: {
        words: string[]
        languageCode: string
        cefrLevel?: string
    }): Promise<GenerationResult> {
        return this.post<GenerationResult>('/api/admin/vocabulary-cards/generate/batch', request)
    }

    /**
     * Get all vocabulary cards for a word set
     */
    async getCardsByWordSet(wordSetId: number): Promise<VocabularyCard[]> {
        return this.get<VocabularyCard[]>(`/api/admin/vocabulary-cards/word-set/${wordSetId}`)
    }

    /**
     * Get vocabulary cards by language and optional status
     */
    async getCardsByLanguage(languageCode: string, status?: string): Promise<VocabularyCard[]> {
        const params = status ? `?status=${status}` : ''
        return this.get<VocabularyCard[]>(`/api/admin/vocabulary-cards/language/${languageCode}${params}`)
    }

    /**
     * Search vocabulary cards by lemma prefix
     */
    async searchCards(languageCode: string, query: string): Promise<VocabularyCard[]> {
        return this.get<VocabularyCard[]>(`/api/admin/vocabulary-cards/search?languageCode=${languageCode}&query=${encodeURIComponent(query)}`)
    }

    /**
     * Get a single vocabulary card by ID
     */
    async getCard(id: number): Promise<VocabularyCard> {
        return this.get<VocabularyCard>(`/api/admin/vocabulary-cards/${id}`)
    }

    /**
     * Update the review status of a vocabulary card
     */
    async updateCardStatus(id: number, status: 'PENDING' | 'APPROVED' | 'REJECTED'): Promise<VocabularyCard> {
        return this.put<VocabularyCard>(`/api/admin/vocabulary-cards/${id}/status`, {status})
    }

    /**
     * Delete a vocabulary card
     */
    async deleteCard(id: number): Promise<void> {
        return this.delete<void>(`/api/admin/vocabulary-cards/${id}`)
    }

    /**
     * Get statistics for vocabulary cards
     */
    async getStats(params?: { languageCode?: string; wordSetId?: number }): Promise<VocabularyCardStats> {
        const queryParams = new URLSearchParams()
        if (params?.languageCode) queryParams.append('languageCode', params.languageCode)
        if (params?.wordSetId) queryParams.append('wordSetId', params.wordSetId.toString())
        const query = queryParams.toString() ? `?${queryParams.toString()}` : ''
        return this.get<VocabularyCardStats>(`/api/admin/vocabulary-cards/stats${query}`)
    }
}

export const VocabularyCardService = new VocabularyCardAPI()
