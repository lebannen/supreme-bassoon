import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiService } from '@/services/api'
import type { VocabularyWord, AddWordToVocabularyRequest } from '@/types/vocabulary'

export const useVocabularyStore = defineStore('vocabulary', () => {
  const words = ref<VocabularyWord[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const wordCount = computed(() => words.value.length)

  const isWordInVocabulary = computed(() => (wordId: number) => {
    return words.value.some(w => w.word.id === wordId)
  })

  async function fetchVocabulary() {
    loading.value = true
    error.value = null

    try {
      const vocabularyWords = await apiService.getUserVocabulary()
      words.value = vocabularyWords
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch vocabulary'
      return false
    } finally {
      loading.value = false
    }
  }

  async function addWord(request: AddWordToVocabularyRequest) {
    loading.value = true
    error.value = null

    try {
      const vocabularyWord = await apiService.addWordToVocabulary(request)

      // Check if word already exists (update scenario)
      const existingIndex = words.value.findIndex(w => w.word.id === request.wordId)
      if (existingIndex !== -1) {
        // Update existing word
        words.value[existingIndex] = vocabularyWord
      } else {
        // Add new word to the beginning of the list
        words.value.unshift(vocabularyWord)
      }

      return vocabularyWord
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to add word to vocabulary'
      return null
    } finally {
      loading.value = false
    }
  }

  async function removeWord(wordId: number) {
    loading.value = true
    error.value = null

    try {
      await apiService.removeWordFromVocabulary(wordId)

      // Remove from local state
      words.value = words.value.filter(w => w.word.id !== wordId)

      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to remove word from vocabulary'
      return false
    } finally {
      loading.value = false
    }
  }

  async function checkIfWordInVocabulary(wordId: number) {
    try {
      const result = await apiService.checkIfWordInVocabulary(wordId)
      return result.inVocabulary
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to check word status'
      return false
    }
  }

  function clearVocabulary() {
    words.value = []
    error.value = null
  }

  return {
    words,
    loading,
    error,
    wordCount,
    isWordInVocabulary,
    fetchVocabulary,
    addWord,
    removeWord,
    checkIfWordInVocabulary,
    clearVocabulary,
  }
})
