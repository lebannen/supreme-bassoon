import { ref } from 'vue'
import type { Exercise, ExerciseSummary, SubmitAttemptRequest, AttemptResult, UserProgress } from '@/types/exercise'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export function useExerciseApi() {
  const loading = ref(false)
  const error = ref<string | null>(null)

  function getAuthHeaders(): HeadersInit {
    const token = localStorage.getItem('auth_token')
    const headers: HeadersInit = {
      'Content-Type': 'application/json'
    }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    return headers
  }

  async function getExercises(
    languageCode: string,
    filters?: {
      module?: number
      topic?: string
      type?: string
    }
  ): Promise<ExerciseSummary[]> {
    loading.value = true
    error.value = null

    try {
      const params = new URLSearchParams({ languageCode })
      if (filters?.module) params.append('module', filters.module.toString())
      if (filters?.topic) params.append('topic', filters.topic)
      if (filters?.type) params.append('type', filters.type)

      const response = await fetch(`${API_BASE}/api/exercises?${params}`)
      if (!response.ok) throw new Error('Failed to fetch exercises')
      return await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      return []
    } finally {
      loading.value = false
    }
  }

  async function getExercise(id: number): Promise<Exercise | null> {
    loading.value = true
    error.value = null

    try {
      const response = await fetch(`${API_BASE}/api/exercises/${id}`)
      if (!response.ok) throw new Error('Failed to fetch exercise')
      return await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      return null
    } finally {
      loading.value = false
    }
  }

  async function submitAttempt(
    exerciseId: number,
    request: SubmitAttemptRequest
  ): Promise<AttemptResult | null> {
    loading.value = true
    error.value = null

    try {
      const response = await fetch(`${API_BASE}/api/exercises/${exerciseId}/attempt`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(request)
      })

      if (!response.ok) {
        throw new Error(`Failed to submit attempt: ${response.status} ${response.statusText}`)
      }

      return await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      return null
    } finally {
      loading.value = false
    }
  }

  async function getProgress(exerciseId: number): Promise<UserProgress | null> {
    try {
      const response = await fetch(`${API_BASE}/api/exercises/${exerciseId}/progress`, {
        headers: getAuthHeaders()
      })

      if (!response.ok) throw new Error('Failed to fetch progress')
      return await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      return null
    }
  }

  return {
    loading,
    error,
    getExercises,
    getExercise,
    submitAttempt,
    getProgress
  }
}
