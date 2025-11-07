import { ref } from 'vue'

export interface Language {
  code: string
  name: string
  entryCount: number
}

export interface WordSummary {
  id: number
  lemma: string
  partOfSpeech: string | null
  frequencyRank: number | null
}

export interface SearchResult {
  words: WordSummary[]
  total: number
}

export interface Example {
  id: number
  sentenceText: string
  translation: string | null
}

export interface Definition {
  id: number
  definitionNumber: number
  definitionText: string
  examples: Example[]
}

export interface Pronunciation {
  id: number
  ipa: string | null
  audioUrl: string | null
  dialect: string | null
}

export interface InflectedForm {
  id: number
  form: string
  partOfSpeech: string | null
  grammaticalFeatures: Record<string, any> | null
}

export interface BaseForm {
  id: number
  lemma: string
  partOfSpeech: string | null
}

export interface Word {
  id: number
  languageCode: string
  lemma: string
  partOfSpeech: string | null
  etymology: string | null
  usageNotes: string | null
  frequencyRank: number | null
  isInflectedForm: boolean
  lemmaId: number | null
  grammaticalFeatures: Record<string, any> | null
  baseForm: BaseForm | null
  definitions: Definition[]
  pronunciations: Pronunciation[]
  inflectedForms: InflectedForm[]
}

export function useVocabularyApi() {
  const API_BASE = 'http://localhost:8080/api/v1'
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  async function getLanguages(): Promise<Language[]> {
    isLoading.value = true
    error.value = null

    try {
      const response = await fetch(`${API_BASE}/languages`, {
        credentials: 'include'
      })

      if (!response.ok) {
        throw new Error(`Failed to fetch languages: ${response.statusText}`)
      }

      return await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      return []
    } finally {
      isLoading.value = false
    }
  }

  async function searchWords(
    languageCode: string,
    query: string
  ): Promise<SearchResult | null> {
    if (!query.trim()) {
      return { words: [], total: 0 }
    }

    isLoading.value = true
    error.value = null

    try {
      const url = new URL(`${API_BASE}/search`)
      url.searchParams.append('lang', languageCode)
      url.searchParams.append('q', query)

      const response = await fetch(url.toString(), {
        credentials: 'include'
      })

      if (!response.ok) {
        throw new Error(`Search failed: ${response.statusText}`)
      }

      return await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function getWord(
    languageCode: string,
    lemma: string
  ): Promise<Word | null> {
    isLoading.value = true
    error.value = null

    try {
      const response = await fetch(`${API_BASE}/words/${languageCode}/${lemma}`, {
        credentials: 'include'
      })

      if (!response.ok) {
        if (response.status === 404) {
          error.value = 'Word not found'
          return null
        }
        throw new Error(`Failed to fetch word: ${response.statusText}`)
      }

      return await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      return null
    } finally {
      isLoading.value = false
    }
  }

  return {
    isLoading,
    error,
    getLanguages,
    searchWords,
    getWord
  }
}
