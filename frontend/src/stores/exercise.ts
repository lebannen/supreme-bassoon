import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import {exerciseAPI} from '@/api'
import type {ExerciseFilters} from '@/api/exercise'
import type {AttemptResult, Exercise, ExerciseSummary, SubmitAttemptRequest, UserProgress,} from '@/types/exercise'
import type {UserStats} from '@/types/stats'

export const useExerciseStore = defineStore('exercise', () => {
    const exercises = ref<ExerciseSummary[]>([])
    const currentExercise = ref<Exercise | null>(null)
    const currentProgress = ref<UserProgress | null>(null)
    const lastAttemptResult = ref<AttemptResult | null>(null)
    const stats = ref<UserStats | null>(null)

    const loading = ref(false)
    const error = ref<string | null>(null)

    const hasExercises = computed(() => exercises.value.length > 0)
    const currentExerciseId = computed(() => currentExercise.value?.id ?? null)
    const isCurrentExerciseCompleted = computed(() => currentProgress.value?.isCompleted ?? false)

    // Load exercises with optional filters
    async function loadExercises(languageCode: string, filters?: ExerciseFilters) {
        loading.value = true
        error.value = null

        try {
            exercises.value = await exerciseAPI.getExercises(languageCode, filters)
            return true
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load exercises'
            return false
        } finally {
            loading.value = false
        }
    }

    // Load a specific exercise
    async function loadExercise(id: number) {
        loading.value = true
        error.value = null

        try {
            currentExercise.value = await exerciseAPI.getExercise(id)

            // Also load progress for this exercise
            await loadProgress(id)

            return currentExercise.value
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load exercise'
            currentExercise.value = null
            return null
        } finally {
            loading.value = false
        }
    }

    // Load user's progress for an exercise
    async function loadProgress(exerciseId: number) {
        try {
            currentProgress.value = await exerciseAPI.getProgress(exerciseId)
            return currentProgress.value
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load progress'
            currentProgress.value = null
            return null
        }
    }

    // Submit an exercise attempt
    async function submitAttempt(exerciseId: number, request: SubmitAttemptRequest) {
        loading.value = true
        error.value = null

        try {
            const result = await exerciseAPI.submitAttempt(exerciseId, request)
            lastAttemptResult.value = result

            // Refresh progress after submitting
            await loadProgress(exerciseId)

            return result
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to submit attempt'
            return null
        } finally {
            loading.value = false
        }
    }

    // Clear current exercise data
    function clearCurrentExercise() {
        currentExercise.value = null
        currentProgress.value = null
        lastAttemptResult.value = null
        error.value = null
    }

    // Clear last attempt result
    function clearLastAttempt() {
        lastAttemptResult.value = null
    }

    // Load user statistics
    async function loadStats() {
        loading.value = true
        error.value = null

        try {
            stats.value = await exerciseAPI.getStats()
            return stats.value
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load statistics'
            stats.value = null
            return null
        } finally {
            loading.value = false
        }
    }

    return {
        // State
        exercises,
        currentExercise,
        currentProgress,
        lastAttemptResult,
        stats,
        loading,
        error,

        // Computed
        hasExercises,
        currentExerciseId,
        isCurrentExerciseCompleted,

        // Actions
        loadExercises,
        loadExercise,
        loadProgress,
        submitAttempt,
        loadStats,
        clearCurrentExercise,
        clearLastAttempt,
    }
})
