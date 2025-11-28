import {defineStore, storeToRefs} from 'pinia'
import {computed} from 'vue'
import {useProgressStore} from './progress'
import type {ContinueLearning, DailyGoal, Enrollment, UserStats} from '@/types/progress'

// Re-export types for backward compatibility
export type {UserStats, DailyGoal, ContinueLearning, Enrollment}

export const useDashboardStore = defineStore('dashboard', () => {
    const progressStore = useProgressStore()

    // Re-export from progress store
    const {
        userStats,
        dailyGoals,
        continueLearning,
        enrollments,
        completedGoalsCount,
        dailyActivity,
        loading,
        error
    } = storeToRefs(progressStore)

    // Computed for backward compatibility
    const completedTasksCount = computed(() => completedGoalsCount.value)
    const totalTasksCount = computed(() => dailyGoals.value.length)
    const hasActiveEnrollments = computed(() => enrollments.value.length > 0)

    // Actions
    function loadDashboardData() {
        progressStore.loadFromStorage()
    }

    function enrollInCourse(course: Parameters<typeof progressStore.enrollInCourse>[0]) {
        return progressStore.enrollInCourse(course)
    }

    function isEnrolled(courseId: number) {
        return progressStore.isEnrolled(courseId)
    }

    function getEnrollment(courseId: number) {
        return progressStore.getEnrollment(courseId)
    }

    function clearDashboard() {
        progressStore.clearProgress()
    }

    return {
        // State (from progress store)
        userStats,
        dailyGoals,
        continueLearning,
        enrollments,
        dailyActivity,
        completedGoalsCount,
        loading,
        error,
        // Computed
        completedTasksCount,
        totalTasksCount,
        hasActiveEnrollments,
        // Actions
        loadDashboardData,
        enrollInCourse,
        isEnrolled,
        getEnrollment,
        clearDashboard
    }
})
