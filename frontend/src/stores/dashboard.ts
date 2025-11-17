import {defineStore} from 'pinia'
import {computed, ref} from 'vue'

export interface UserStats {
    streak: number
    wordsLearned: number
    timeThisWeek: string
}

export interface DailyTask {
    id: number
    icon: string
    title: string
    meta: string
    completed: boolean
    type?: 'vocabulary' | 'exercise' | 'listening' | 'grammar'
}

export interface RecommendedContent {
    id: number
    type: 'dialogue' | 'story' | 'grammar'
    icon: string
    title: string
    level?: string
    duration?: string
    topic?: string
    completed?: boolean
}

export const useDashboardStore = defineStore('dashboard', () => {
    // State
    const userStats = ref<UserStats>({
        streak: 7,
        wordsLearned: 142,
        timeThisWeek: '3h 24m',
    })

    const dailyTasks = ref<DailyTask[]>([
        {
            id: 1,
            icon: '✓',
            title: 'Vocabulary Review',
            meta: '12 words • 3 minutes',
            completed: true,
            type: 'vocabulary',
        },
        {
            id: 2,
            icon: '✓',
            title: 'Daily Exercises',
            meta: '3 exercises • 5 minutes',
            completed: true,
            type: 'exercise',
        },
        {
            id: 3,
            icon: '🎧',
            title: 'Quick Listen: At the Café',
            meta: 'Dialogue • 3 minutes',
            completed: false,
            type: 'listening',
        },
        {
            id: 4,
            icon: '📝',
            title: 'Grammar Tip: Present Tense',
            meta: 'Quick reference • 2 minutes',
            completed: false,
            type: 'grammar',
        },
    ])

    const recommendedContent = ref<RecommendedContent[]>([
        {
            id: 1,
            type: 'dialogue',
            icon: '🍽️',
            title: 'Ordering at a Restaurant',
            level: 'A1',
            duration: '4 min',
            topic: 'Food & Dining',
        },
        {
            id: 2,
            type: 'story',
            icon: '📚',
            title: 'Sophie Arrives in Paris',
            level: 'A1',
            duration: '5 min',
            topic: 'Travel',
        },
        {
            id: 3,
            type: 'dialogue',
            icon: '🏨',
            title: 'At the Hotel',
            level: 'A1',
            duration: '3 min',
            topic: 'Travel',
        },
    ])

    const loading = ref(false)
    const error = ref<string | null>(null)

    // Actions

    /**
     * TODO: Connect to backend API
     * Fetch user statistics from the backend
     * Endpoint: GET /api/user/stats
     * Response: { streak: number, wordsLearned: number, timeThisWeek: string }
     */
    async function fetchUserStats() {
        loading.value = true
        error.value = null

        try {
            // TODO: Replace with actual API call
            // const response = await fetch('/api/user/stats')
            // const data = await response.json()
            // userStats.value = data

            // Mock delay to simulate API call
            await new Promise((resolve) => setTimeout(resolve, 500))

            // Currently using mock data defined above
            console.log('TODO: Fetch user stats from backend')
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to fetch user stats'
            console.error('Error fetching user stats:', err)
        } finally {
            loading.value = false
        }
    }

    /**
     * TODO: Connect to backend API
     * Fetch daily tasks from the backend
     * Endpoint: GET /api/user/daily-tasks
     * Response: DailyTask[]
     */
    async function fetchDailyTasks() {
        loading.value = true
        error.value = null

        try {
            // TODO: Replace with actual API call
            // const response = await fetch('/api/user/daily-tasks')
            // const data = await response.json()
            // dailyTasks.value = data

            // Mock delay to simulate API call
            await new Promise((resolve) => setTimeout(resolve, 500))

            // Currently using mock data defined above
            console.log('TODO: Fetch daily tasks from backend')
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to fetch daily tasks'
            console.error('Error fetching daily tasks:', err)
        } finally {
            loading.value = false
        }
    }

    /**
     * TODO: Connect to backend API
     * Fetch recommended content from the backend
     * Endpoint: GET /api/user/recommended-content
     * Response: RecommendedContent[]
     */
    async function fetchRecommendedContent() {
        loading.value = true
        error.value = null

        try {
            // TODO: Replace with actual API call
            // const response = await fetch('/api/user/recommended-content')
            // const data = await response.json()
            // recommendedContent.value = data

            // Mock delay to simulate API call
            await new Promise((resolve) => setTimeout(resolve, 500))

            // Currently using mock data defined above
            console.log('TODO: Fetch recommended content from backend')
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to fetch recommended content'
            console.error('Error fetching recommended content:', err)
        } finally {
            loading.value = false
        }
    }

    /**
     * TODO: Connect to backend API
     * Mark a task as completed
     * Endpoint: POST /api/user/tasks/:taskId/complete
     */
    async function completeTask(taskId: number) {
        try {
            // TODO: Replace with actual API call
            // await fetch(`/api/user/tasks/${taskId}/complete`, { method: 'POST' })

            // Update local state optimistically
            const task = dailyTasks.value.find((t) => t.id === taskId)
            if (task) {
                task.completed = true
            }

            console.log('TODO: Mark task as completed in backend:', taskId)
        } catch (err) {
            error.value = err instanceof Error ? err.message : 'Failed to complete task'
            console.error('Error completing task:', err)
        }
    }

    /**
     * Fetch all dashboard data
     * Call this when the home view is loaded
     */
    async function loadDashboardData() {
        await Promise.all([fetchUserStats(), fetchDailyTasks(), fetchRecommendedContent()])
    }

    // Computed
    const completedTasksCount = computed(() => {
        return dailyTasks.value.filter((task) => task.completed).length
    })

    const totalTasksCount = computed(() => {
        return dailyTasks.value.length
    })

    return {
        // State
        userStats,
        dailyTasks,
        recommendedContent,
        loading,
        error,

        // Computed
        completedTasksCount,
        totalTasksCount,

        // Actions
        fetchUserStats,
        fetchDailyTasks,
        fetchRecommendedContent,
        completeTask,
        loadDashboardData,
    }
})
