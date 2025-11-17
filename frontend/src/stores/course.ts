import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import {courseAPI} from '@/api'
import type {CourseDetail, CourseSummary, Episode, Module, ModuleProgress,} from '@/types/course'

export const useCourseStore = defineStore('course', () => {
    const courses = ref<CourseSummary[]>([])
    const currentCourse = ref<CourseDetail | null>(null)
    const currentModule = ref<Module | null>(null)
    const currentEpisode = ref<Episode | null>(null)
    const moduleProgress = ref<ModuleProgress | null>(null)

    const loading = ref(false)
    const error = ref<string | null>(null)

    const hasCourses = computed(() => courses.value.length > 0)
    const currentCourseSlug = computed(() => currentCourse.value?.slug ?? null)

    // Load all courses
    async function loadCourses() {
        loading.value = true
        error.value = null

        try {
            courses.value = await courseAPI.getCourses()
            return true
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load courses'
            return false
        } finally {
            loading.value = false
        }
    }

    // Load a specific course by slug
    async function loadCourseBySlug(slug: string) {
        loading.value = true
        error.value = null

        try {
            currentCourse.value = await courseAPI.getCourseBySlug(slug)
            return currentCourse.value
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load course'
            currentCourse.value = null
            return null
        } finally {
            loading.value = false
        }
    }

    // Load a specific course by ID
    async function loadCourseById(id: number) {
        loading.value = true
        error.value = null

        try {
            currentCourse.value = await courseAPI.getCourseById(id)
            return currentCourse.value
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load course'
            currentCourse.value = null
            return null
        } finally {
            loading.value = false
        }
    }

    // Load a specific module
    async function loadModule(moduleId: number) {
        loading.value = true
        error.value = null

        try {
            currentModule.value = await courseAPI.getModule(moduleId)
            return currentModule.value
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load module'
            currentModule.value = null
            return null
        } finally {
            loading.value = false
        }
    }

    // Load a specific episode
    async function loadEpisode(episodeId: number) {
        loading.value = true
        error.value = null

        try {
            currentEpisode.value = await courseAPI.getEpisode(episodeId)
            return currentEpisode.value
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load episode'
            currentEpisode.value = null
            return null
        } finally {
            loading.value = false
        }
    }

    // Load module progress
    async function loadModuleProgress(languageCode: string, moduleNumber: number) {
        loading.value = true
        error.value = null

        try {
            moduleProgress.value = await courseAPI.getModuleProgress(languageCode, moduleNumber)
            return moduleProgress.value
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to load module progress'
            moduleProgress.value = null
            return null
        } finally {
            loading.value = false
        }
    }

    // Complete an episode
    async function completeEpisode(episodeId: number) {
        loading.value = true
        error.value = null

        try {
            await courseAPI.completeEpisode(episodeId)
            return true
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to complete episode'
            return false
        } finally {
            loading.value = false
        }
    }

    // Clear current course data
    function clearCurrentCourse() {
        currentCourse.value = null
        currentModule.value = null
        currentEpisode.value = null
        moduleProgress.value = null
        error.value = null
    }

    return {
        // State
        courses,
        currentCourse,
        currentModule,
        currentEpisode,
        moduleProgress,
        loading,
        error,

        // Computed
        hasCourses,
        currentCourseSlug,

        // Actions
        loadCourses,
        loadCourseBySlug,
        loadCourseById,
        loadModule,
        loadEpisode,
        loadModuleProgress,
        completeEpisode,
        clearCurrentCourse,
    }
})
