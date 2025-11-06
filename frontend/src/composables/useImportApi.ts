import { ref, type Ref } from 'vue'

export interface ImportProgress {
  importId: string
  languageCode: string
  languageName: string
  status: 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  totalEntries: number
  processedEntries: number
  successfulEntries: number
  failedEntries: number
  progressPercentage: number
  message: string
  startedAt: string
  completedAt: string | null
  error: string | null
}

export interface ImportStartResponse {
  importId: string
  message: string
}

export function useImportApi() {
  const API_BASE = 'http://localhost:8080/api'
  const isUploading = ref(false)
  const uploadError = ref<string | null>(null)

  async function uploadFile(
    file: File,
    languageCode: string
  ): Promise<ImportStartResponse | null> {
    isUploading.value = true
    uploadError.value = null

    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('languageCode', languageCode)

      const response = await fetch(`${API_BASE}/import/upload`, {
        method: 'POST',
        body: formData
      })

      if (!response.ok) {
        throw new Error(`Upload failed: ${response.statusText}`)
      }

      const data: ImportStartResponse = await response.json()
      return data
    } catch (error) {
      uploadError.value = error instanceof Error ? error.message : 'Unknown error'
      return null
    } finally {
      isUploading.value = false
    }
  }

  function connectToProgressStream(
    importId: string,
    onProgress: (progress: ImportProgress) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): () => void {
    const eventSource = new EventSource(`${API_BASE}/import/progress/${importId}`)

    eventSource.addEventListener('progress', (event: MessageEvent) => {
      try {
        const progress: ImportProgress = JSON.parse(event.data)
        onProgress(progress)
      } catch (error) {
        console.error('Failed to parse progress data:', error)
      }
    })

    eventSource.addEventListener('complete', () => {
      eventSource.close()
      onComplete()
    })

    eventSource.addEventListener('not_found', () => {
      eventSource.close()
      onError(new Error('Import not found'))
    })

    eventSource.onerror = (error) => {
      eventSource.close()
      onError(new Error('Connection error'))
    }

    // Return cleanup function
    return () => {
      eventSource.close()
    }
  }

  async function getImportStatus(importId: string): Promise<ImportProgress | null> {
    try {
      const response = await fetch(`${API_BASE}/import/progress/${importId}/status`)

      if (!response.ok) {
        return null
      }

      return await response.json()
    } catch (error) {
      console.error('Failed to fetch import status:', error)
      return null
    }
  }

  async function getAllImports(): Promise<ImportProgress[]> {
    try {
      const response = await fetch(`${API_BASE}/import/progress`)

      if (!response.ok) {
        return []
      }

      return await response.json()
    } catch (error) {
      console.error('Failed to fetch imports:', error)
      return []
    }
  }

  async function clearDatabase(): Promise<{ message: string; deletedCount: number } | null> {
    try {
      const response = await fetch(`${API_BASE}/import/clear`, {
        method: 'DELETE'
      })

      if (!response.ok) {
        throw new Error(`Clear database failed: ${response.statusText}`)
      }

      return await response.json()
    } catch (error) {
      console.error('Failed to clear database:', error)
      return null
    }
  }

  return {
    isUploading,
    uploadError,
    uploadFile,
    connectToProgressStream,
    getImportStatus,
    getAllImports,
    clearDatabase
  }
}
