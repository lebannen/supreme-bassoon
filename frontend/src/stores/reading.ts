import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import {readingAPI} from '@/api'
import type {ReadingProgress, ReadingText, TextFilters} from '@/types/reading'

export const useReadingStore = defineStore('reading', () => {
    const texts = ref<ReadingText[]>([])
    const currentText = ref<ReadingText | null>(null)
    const currentProgress = ref<ReadingProgress | null>(null)

    const loading = ref(false)
    const error = ref<string | null>(null)

    const hasTexts = computed(() => texts.value.length > 0)
    const currentTextId = computed(() => currentText.value?.id ?? null)
    const isCurrentTextCompleted = computed(() => currentProgress.value?.completed ?? false)
    const currentPageNumber = computed(() => currentProgress.value?.currentPage ?? 1)

    // Load reading texts with optional filters
    async function loadTexts(filters: TextFilters = {}) {
        loading.value = true
        error.value = null

        try {
            texts.value = await readingAPI.getTexts(filters)
            return true
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load reading texts'
            return false
        } finally {
            loading.value = false
        }
    }

    // Load a specific reading text
    async function loadTextById(textId: number) {
        loading.value = true
        error.value = null

        try {
            currentText.value = await readingAPI.getTextById(textId)

            // Also load progress for this text
            await loadProgress(textId)

            return currentText.value
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load reading text'
            currentText.value = null
            return null
        } finally {
            loading.value = false
        }
    }

    // Load reading progress for a specific text
    async function loadProgress(textId: number) {
        try {
            currentProgress.value = await readingAPI.getProgress(textId)
            return currentProgress.value
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load reading progress'
            currentProgress.value = null
            return null
        }
    }

    // Update reading progress
    async function updateProgress(textId: number, currentPage: number, totalPages: number) {
        loading.value = true
        error.value = null

        try {
            currentProgress.value = await readingAPI.updateProgress(textId, currentPage, totalPages)
            return currentProgress.value
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to update reading progress'
            return null
        } finally {
            loading.value = false
        }
    }

    // Mark a reading text as completed
    async function markCompleted(textId: number) {
        loading.value = true
        error.value = null

        try {
            currentProgress.value = await readingAPI.markCompleted(textId)
            return true
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to mark text as completed'
            return false
        } finally {
            loading.value = false
        }
    }

    // Clear current reading text data
    function clearCurrentText() {
        currentText.value = null
        currentProgress.value = null
        error.value = null
    }

    return {
        // State
        texts,
        currentText,
        currentProgress,
        loading,
        error,

        // Computed
        hasTexts,
        currentTextId,
        isCurrentTextCompleted,
        currentPageNumber,

        // Actions
        loadTexts,
        loadTextById,
        loadProgress,
        updateProgress,
        markCompleted,
        clearCurrentText,
    }
})
