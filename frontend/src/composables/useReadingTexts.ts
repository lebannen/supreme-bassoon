import {ref} from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

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

export function useReadingTexts() {
  const texts = ref<ReadingText[]>([])
  const currentText = ref<ReadingText | null>(null)
  const progress = ref<ReadingProgress | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

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

  async function fetchTexts(filters: TextFilters = {}): Promise<ReadingText[]> {
    loading.value = true
    error.value = null

    try {
      const params = new URLSearchParams()
      if (filters.languageCode) params.append('languageCode', filters.languageCode)
      if (filters.level) params.append('level', filters.level)
      if (filters.topic) params.append('topic', filters.topic)

      const url = `${API_BASE}/api/reading/texts${params.toString() ? '?' + params.toString() : ''}`
      const response = await fetch(url)

      if (!response.ok) {
        throw new Error('Failed to fetch texts')
      }

      const data = await response.json()
      texts.value = data
      return data
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch texts'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchTextById(id: number): Promise<ReadingText> {
    loading.value = true
    error.value = null

    try {
      const response = await fetch(`${API_BASE}/api/reading/texts/${id}`)

      if (!response.ok) {
        throw new Error('Failed to fetch text')
      }

      const data = await response.json()
      currentText.value = data
      return data
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch text'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchProgress(textId: number): Promise<ReadingProgress | null> {
    try {
        const response = await fetch(`${API_BASE}/api/reading/texts/${textId}/progress`, {
            headers: getAuthHeaders(),
        })

      if (response.status === 404) {
        // User hasn't started this text yet
        progress.value = null
        return null
      }

      if (!response.ok) {
        throw new Error('Failed to fetch progress')
      }

      const data = await response.json()
      progress.value = data
      return data
    } catch {
      // Silently fail for progress fetching
      return null
    }
  }

  async function updateProgress(
    textId: number,
    currentPage: number,
    totalPages: number
  ): Promise<ReadingProgress> {
    try {
        const response = await fetch(`${API_BASE}/api/reading/texts/${textId}/progress`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({currentPage, totalPages}),
        })

      if (!response.ok) {
        throw new Error('Failed to update progress')
      }

      const data = await response.json()
      progress.value = data
      return data
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to update progress'
      throw err
    }
  }

  async function markCompleted(textId: number): Promise<ReadingProgress> {
    try {
        const response = await fetch(`${API_BASE}/api/reading/texts/${textId}/complete`, {
            method: 'POST',
            headers: getAuthHeaders(),
        })

      if (!response.ok) {
        throw new Error('Failed to mark as completed')
      }

      const data = await response.json()
      progress.value = data
      return data
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to mark as completed'
      throw err
    }
  }

  return {
    texts,
    currentText,
    progress,
    loading,
    error,
    fetchTexts,
    fetchTextById,
    fetchProgress,
    updateProgress,
      markCompleted,
  }
}
