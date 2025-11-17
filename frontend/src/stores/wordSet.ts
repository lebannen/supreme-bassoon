import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import {wordSetAPI} from '@/api'
import type {ImportWordSetRequest, ImportWordSetResponse, WordSet, WordSetDetail,} from '@/types/wordSet'

export const useWordSetStore = defineStore('wordSet', () => {
    const wordSets = ref<WordSet[]>([])
    const currentWordSet = ref<WordSetDetail | null>(null)
    const lastImportResult = ref<ImportWordSetResponse | null>(null)

    const loading = ref(false)
    const error = ref<string | null>(null)

    const hasWordSets = computed(() => wordSets.value.length > 0)
    const currentWordSetId = computed(() => currentWordSet.value?.id ?? null)

    // Load all word sets
    async function loadAllWordSets() {
        loading.value = true
        error.value = null

        try {
            wordSets.value = await wordSetAPI.getAllWordSets()
            return true
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load word sets'
            return false
        } finally {
            loading.value = false
        }
    }

    // Load word sets for a specific language
    async function loadWordSetsByLanguage(languageCode: string) {
        loading.value = true
        error.value = null

        try {
            wordSets.value = await wordSetAPI.getWordSetsByLanguage(languageCode)
            return true
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load word sets'
            return false
        } finally {
            loading.value = false
        }
    }

    // Load a specific word set by ID
    async function loadWordSetById(id: number) {
        loading.value = true
        error.value = null

        try {
            const wordSet = await wordSetAPI.getWordSetById(id)
            currentWordSet.value = wordSet
            return wordSet
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load word set'
            currentWordSet.value = null
            return null
        } finally {
            loading.value = false
        }
    }

    // Import a word set to user's vocabulary
    async function importWordSet(id: number, request?: ImportWordSetRequest) {
        loading.value = true
        error.value = null

        try {
            const result = await wordSetAPI.importWordSet(id, request)
            lastImportResult.value = result

            // Refresh the current word set to update userVocabularyCount
            if (currentWordSet.value?.id === id) {
                await loadWordSetById(id)
            }

            return result
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to import word set'
            return null
        } finally {
            loading.value = false
        }
    }

    // Load word sets from a JSON file
    async function loadWordSetsFromJson(file: File) {
        loading.value = true
        error.value = null

        try {
            const result = await wordSetAPI.loadWordSetsFromJson(file)

            // Refresh the word sets list after loading
            await loadAllWordSets()

            return result
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load word sets from JSON'
            return null
        } finally {
            loading.value = false
        }
    }

    // Clear current word set data
    function clearCurrentWordSet() {
        currentWordSet.value = null
        lastImportResult.value = null
        error.value = null
    }

    // Clear last import result
    function clearLastImportResult() {
        lastImportResult.value = null
    }

    return {
        // State
        wordSets,
        currentWordSet,
        lastImportResult,
        loading,
        error,

        // Computed
        hasWordSets,
        currentWordSetId,

        // Actions
        loadAllWordSets,
        loadWordSetsByLanguage,
        loadWordSetById,
        importWordSet,
        loadWordSetsFromJson,
        clearCurrentWordSet,
        clearLastImportResult,
    }
})
