import type {ImportWordSetRequest, ImportWordSetResponse, WordSet, WordSetDetail,} from '@/types/wordSet'

const API_BASE = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`

export function useWordSetApi() {
  function getAuthHeaders(): HeadersInit {
    const token = localStorage.getItem('auth_token')
    const headers: HeadersInit = {
        'Content-Type': 'application/json',
    }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    return headers
  }

  async function getAllWordSets(): Promise<WordSet[]> {
    try {
      const response = await fetch(`${API_BASE}/word-sets`, {
          headers: getAuthHeaders(),
      })

      if (!response.ok) {
        throw new Error('Failed to fetch word sets')
      }

      return await response.json()
    } catch (error) {
      console.error('Failed to fetch word sets:', error)
      return []
    }
  }

  async function getWordSetsByLanguage(languageCode: string): Promise<WordSet[]> {
    try {
      const response = await fetch(`${API_BASE}/word-sets/language/${languageCode}`, {
          headers: getAuthHeaders(),
      })

      if (!response.ok) {
        throw new Error('Failed to fetch word sets')
      }

      return await response.json()
    } catch (error) {
      console.error('Failed to fetch word sets:', error)
      return []
    }
  }

  async function getWordSetById(id: number): Promise<WordSetDetail | null> {
    try {
      const response = await fetch(`${API_BASE}/word-sets/${id}`, {
          headers: getAuthHeaders(),
      })

      if (!response.ok) {
        return null
      }

      return await response.json()
    } catch (error) {
      console.error('Failed to fetch word set:', error)
      return null
    }
  }

  async function importWordSet(
    id: number,
    request?: ImportWordSetRequest
  ): Promise<ImportWordSetResponse | null> {
    try {
      const response = await fetch(`${API_BASE}/word-sets/${id}/import`, {
        method: 'POST',
        headers: getAuthHeaders(),
          body: request ? JSON.stringify(request) : undefined,
      })

      if (!response.ok) {
        throw new Error('Failed to import word set')
      }

      return await response.json()
    } catch (error) {
      console.error('Failed to import word set:', error)
      return null
    }
  }

  async function loadWordSetsFromJson(
    file: File
  ): Promise<{ message: string; count: number } | null> {
    try {
      const text = await file.text()
      const jsonData = JSON.parse(text)

      const response = await fetch(`${API_BASE}/word-sets/load-from-json-content`, {
        method: 'POST',
        headers: {
          ...getAuthHeaders(),
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            jsonContent: jsonData,
        }),
      })

      if (!response.ok) {
        const error = await response.json()
        throw new Error(error.error || 'Failed to load word sets from JSON')
      }

      return await response.json()
    } catch (error) {
      console.error('Failed to load word sets from JSON:', error)
      throw error
    }
  }

  return {
    getAllWordSets,
    getWordSetsByLanguage,
    getWordSetById,
    importWordSet,
      loadWordSetsFromJson,
  }
}
