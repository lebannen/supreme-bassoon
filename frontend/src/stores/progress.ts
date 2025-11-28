import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import type {EnrollmentResponse} from '@/api'
import {activityAPI, enrollmentAPI} from '@/api'
import type {
    ContinueLearning,
    DailyActivity,
    DailyGoal,
    EnrollCourseParams,
    Enrollment,
    UserStats
} from '@/types/progress'

// Helper to convert API response to local Enrollment type
function mapApiEnrollment(apiEnrollment: EnrollmentResponse): Enrollment {
    return {
        id: apiEnrollment.id,
        courseId: apiEnrollment.courseId,
        courseName: apiEnrollment.courseName,
        courseSlug: apiEnrollment.courseSlug,
        languageCode: apiEnrollment.languageCode,
        cefrLevel: apiEnrollment.cefrLevel,
        startedAt: apiEnrollment.enrolledAt,
        lastAccessedAt: apiEnrollment.lastActivityAt,
        progress: apiEnrollment.progress,
        currentModuleId: apiEnrollment.currentModuleId,
        currentModuleName: apiEnrollment.currentModuleName,
        currentEpisodeId: apiEnrollment.currentEpisodeId,
        currentEpisodeName: apiEnrollment.currentEpisodeName,
        completedEpisodes: apiEnrollment.completedEpisodes,
        totalEpisodes: apiEnrollment.totalEpisodes
    }
}

const STORAGE_KEY = 'vocabee_progress'
const isAuthenticated = () => !!localStorage.getItem('auth_token')

function getTodayDate(): string {
    return new Date().toISOString().split('T')[0] as string
}

export const useProgressStore = defineStore('progress', () => {
    // State (persisted to localStorage)
    const enrollments = ref<Enrollment[]>([])
    const dailyActivity = ref<DailyActivity>({
        date: getTodayDate(),
        wordsReviewed: 0,
        exercisesCompleted: 0,
        episodesCompleted: 0,
        goalsCompleted: 0
    })
    const activityHistory = ref<DailyActivity[]>([])
    const currentStreak = ref(0)
    const loading = ref(false)
    const error = ref<string | null>(null)

    // Daily Goals Configuration
    const DAILY_GOALS: Omit<DailyGoal, 'current' | 'completed'>[] = [
        {id: 'words', tier: 1, title: 'Word Review', description: 'Review 10 vocabulary words', target: 10, icon: '📚'},
        {
            id: 'exercises',
            tier: 2,
            title: 'Exercise Practice',
            description: 'Complete 5 exercises',
            target: 5,
            icon: '✏️'
        },
        {id: 'episode', tier: 3, title: 'Episode Completion', description: 'Complete an episode', target: 1, icon: '🎯'}
    ]

    // Computed
    const dailyGoals = computed((): DailyGoal[] => {
        checkAndResetDailyActivity()
        return DAILY_GOALS.map(goal => {
            let current = 0
            switch (goal.id) {
                case 'words':
                    current = dailyActivity.value.wordsReviewed
                    break
                case 'exercises':
                    current = dailyActivity.value.exercisesCompleted
                    break
                case 'episode':
                    current = dailyActivity.value.episodesCompleted
                    break
            }
            return {
                ...goal,
                current,
                completed: current >= goal.target
            }
        })
    })

    const completedGoalsCount = computed(() =>
        dailyGoals.value.filter(g => g.completed).length
    )

    const streak = computed(() => {
        // Use API streak if available
        if (currentStreak.value > 0) {
            return currentStreak.value
        }

        // Fallback: Calculate streak from activity history (for non-authenticated users)
        let count = 0
        const today = getTodayDate()
        const sortedHistory = [...activityHistory.value].sort((a, b) =>
            b.date.localeCompare(a.date)
        )

        // Check if today has activity
        if (dailyActivity.value.goalsCompleted > 0) {
            count = 1
        } else if (sortedHistory.length === 0) {
            return 0
        } else {
            // No activity today - check if yesterday was active
            const yesterday = new Date(Date.now() - 86400000).toISOString().split('T')[0] as string
            if (sortedHistory[0]?.date !== yesterday) return 0
        }

        // Count consecutive days from history
        const expectedDate = new Date(today)
        expectedDate.setDate(expectedDate.getDate() - (count > 0 ? 1 : 0))

        for (const activity of sortedHistory) {
            const activityDate = activity.date
            const expected = expectedDate.toISOString().split('T')[0] as string

            if (activityDate === expected && activity.goalsCompleted > 0) {
                count++
                expectedDate.setDate(expectedDate.getDate() - 1)
            } else if (activityDate < expected) {
                break
            }
        }

        return count
    })

    const userStats = computed((): UserStats => ({
        streak: streak.value,
        wordsLearned: enrollments.value.reduce((sum, e) => sum + e.completedEpisodes * 10, 0),
        timeThisWeek: formatTimeThisWeek(),
        coursesInProgress: enrollments.value.filter(e => e.progress < 100).length
    }))

    const continueLearning = computed((): ContinueLearning | null => {
        const recent = enrollments.value
            .filter(e => e.progress < 100)
            .sort((a, b) => new Date(b.lastAccessedAt).getTime() - new Date(a.lastAccessedAt).getTime())[0]

        if (!recent) return null

        return {
            courseId: recent.courseId,
            courseName: recent.courseName,
            courseSlug: recent.courseSlug,
            moduleId: recent.currentModuleId,
            moduleName: recent.currentModuleName,
            episodeId: recent.currentEpisodeId,
            episodeName: recent.currentEpisodeName,
            progress: recent.progress
        }
    })

    // Helper to check and reset daily activity if it's a new day
    function checkAndResetDailyActivity() {
        const today = getTodayDate()
        if (dailyActivity.value.date !== today) {
            // Archive yesterday's activity if it had goals completed
            if (dailyActivity.value.goalsCompleted > 0) {
                activityHistory.value.push({...dailyActivity.value})
                // Keep only last 90 days
                if (activityHistory.value.length > 90) {
                    activityHistory.value = activityHistory.value.slice(-90)
                }
            }
            // Reset for new day
            dailyActivity.value = {
                date: today,
                wordsReviewed: 0,
                exercisesCompleted: 0,
                episodesCompleted: 0,
                goalsCompleted: 0
            }
            saveToStorage()
        }
    }

    // Actions
    async function enrollInCourse(course: EnrollCourseParams): Promise<Enrollment> {
        const existing = enrollments.value.find(e => e.courseId === course.id)
        if (existing) {
            existing.lastAccessedAt = new Date().toISOString()
            saveToStorage()
            return existing
        }

        // Use API if authenticated
        if (isAuthenticated()) {
            try {
                const apiEnrollment = await enrollmentAPI.enrollInCourse(course.id)
                const enrollment = mapApiEnrollment(apiEnrollment)
                enrollments.value.push(enrollment)
                saveToStorage()
                return enrollment
            } catch (err) {
                console.warn('Failed to enroll via API, falling back to local:', err)
            }
        }

        // Fallback to local enrollment for unauthenticated users
        const enrollment: Enrollment = {
            id: Date.now(),
            courseId: course.id,
            courseName: course.name,
            courseSlug: course.slug,
            languageCode: course.languageCode,
            cefrLevel: course.cefrLevel,
            startedAt: new Date().toISOString(),
            lastAccessedAt: new Date().toISOString(),
            progress: 0,
            currentModuleId: course.firstModuleId || null,
            currentModuleName: course.firstModuleName || null,
            currentEpisodeId: course.firstEpisodeId || null,
            currentEpisodeName: course.firstEpisodeName || null,
            completedEpisodes: 0,
            totalEpisodes: course.totalEpisodes
        }

        enrollments.value.push(enrollment)
        saveToStorage()
        return enrollment
    }

    function isEnrolled(courseId: number): boolean {
        return enrollments.value.some(e => e.courseId === courseId)
    }

    function getEnrollment(courseId: number): Enrollment | undefined {
        return enrollments.value.find(e => e.courseId === courseId)
    }

    async function updateProgress(courseId: number, episodeId: number, episodeName: string, moduleId: number, moduleName: string) {
        const enrollment = enrollments.value.find(e => e.courseId === courseId)
        if (!enrollment) return

        enrollment.lastAccessedAt = new Date().toISOString()
        enrollment.currentEpisodeId = episodeId
        enrollment.currentEpisodeName = episodeName
        enrollment.currentModuleId = moduleId
        enrollment.currentModuleName = moduleName
        saveToStorage()

        // Sync with backend if authenticated
        if (isAuthenticated()) {
            try {
                await enrollmentAPI.updatePosition(courseId, moduleId, episodeId)
            } catch (err) {
                console.warn('Failed to update position via API:', err)
            }
        }
    }

    async function completeEpisode(courseId: number, episodeId: number, nextEpisodeId?: number, nextEpisodeName?: string, nextModuleId?: number, nextModuleName?: string) {
        const enrollment = enrollments.value.find(e => e.courseId === courseId)
        if (!enrollment) return

        // Call API to mark episode completed if authenticated
        if (isAuthenticated()) {
            try {
                await enrollmentAPI.completeEpisode(episodeId)
                // Refresh enrollment data from API to get accurate counts
                const apiEnrollment = await enrollmentAPI.getEnrollment(courseId)
                if (apiEnrollment) {
                    const updated = mapApiEnrollment(apiEnrollment)
                    Object.assign(enrollment, updated)
                }
            } catch (err) {
                console.warn('Failed to complete episode via API:', err)
                // Fallback to local update
                enrollment.completedEpisodes++
                enrollment.progress = Math.min(100, Math.round((enrollment.completedEpisodes / enrollment.totalEpisodes) * 100))
            }
        } else {
            // Local update for unauthenticated users
            enrollment.completedEpisodes++
            enrollment.progress = Math.min(100, Math.round((enrollment.completedEpisodes / enrollment.totalEpisodes) * 100))
        }

        enrollment.lastAccessedAt = new Date().toISOString()

        // Update current position to next episode if provided
        if (nextEpisodeId !== undefined) {
            enrollment.currentEpisodeId = nextEpisodeId
            enrollment.currentEpisodeName = nextEpisodeName || null
        }
        if (nextModuleId !== undefined) {
            enrollment.currentModuleId = nextModuleId
            enrollment.currentModuleName = nextModuleName || null
        }

        // Update daily activity
        recordActivity('episode')
        saveToStorage()
    }

    async function recordActivity(type: 'word' | 'exercise' | 'episode', count: number = 1) {
        checkAndResetDailyActivity()

        // Update local state immediately for responsive UI
        switch (type) {
            case 'word':
                dailyActivity.value.wordsReviewed += count
                break
            case 'exercise':
                dailyActivity.value.exercisesCompleted += count
                break
            case 'episode':
                dailyActivity.value.episodesCompleted += count
                break
        }

        // Recalculate completed goals
        const goalsCompleted = dailyGoals.value.filter(g => g.completed).length
        dailyActivity.value.goalsCompleted = goalsCompleted
        saveToStorage()

        // Sync with backend if authenticated
        if (isAuthenticated()) {
            try {
                const response = await activityAPI.recordActivity(type, count)
                // Update from server response
                dailyActivity.value.wordsReviewed = response.wordsReviewed
                dailyActivity.value.exercisesCompleted = response.exercisesCompleted
                dailyActivity.value.episodesCompleted = response.episodesCompleted
                dailyActivity.value.goalsCompleted = response.goalsCompleted
                saveToStorage()
            } catch (err) {
                console.warn('Failed to sync activity with backend:', err)
                // Local state is already updated, so user experience is preserved
            }
        }
    }

    function saveToStorage() {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify({
                enrollments: enrollments.value,
                dailyActivity: dailyActivity.value,
                activityHistory: activityHistory.value
            }))
        } catch (e) {
            console.error('Failed to save progress to storage:', e)
        }
    }

    function loadFromStorage() {
        const stored = localStorage.getItem(STORAGE_KEY)
        if (stored) {
            try {
                const data = JSON.parse(stored)
                // Migrate old enrollment data that might be missing fields
                const migratedEnrollments = (data.enrollments || []).map((e: any) => ({
                    ...e,
                    completedEpisodes: e.completedEpisodes ?? 0,
                    totalEpisodes: e.totalEpisodes ?? 1,
                    progress: e.progress ?? 0
                }))
                enrollments.value = migratedEnrollments
                dailyActivity.value = data.dailyActivity || dailyActivity.value
                activityHistory.value = data.activityHistory || []

                // Save migrated data back to storage
                saveToStorage()

                // Check if we need to reset for a new day
                checkAndResetDailyActivity()
            } catch (e) {
                console.error('Failed to load progress from storage:', e)
            }
        }

        // Also load from API if authenticated
        if (isAuthenticated()) {
            loadFromAPI()
        }
    }

    async function loadEnrollmentsFromAPI() {
        try {
            const apiEnrollments = await enrollmentAPI.getMyEnrollments()

            if (apiEnrollments.length > 0) {
                // Use API data as source of truth
                enrollments.value = apiEnrollments.map(mapApiEnrollment)
                saveToStorage()
            } else if (enrollments.value.length > 0) {
                // Migrate localStorage enrollments to the database
                console.log('Migrating localStorage enrollments to database...')
                for (const localEnrollment of enrollments.value) {
                    try {
                        await enrollmentAPI.enrollInCourse(localEnrollment.courseId)
                    } catch (err) {
                        console.warn(`Failed to migrate enrollment for course ${localEnrollment.courseId}:`, err)
                    }
                }
                // Reload from API after migration
                const migratedEnrollments = await enrollmentAPI.getMyEnrollments()
                if (migratedEnrollments.length > 0) {
                    enrollments.value = migratedEnrollments.map(mapApiEnrollment)
                    saveToStorage()
                }
            }
        } catch (err) {
            console.warn('Failed to load enrollments from API:', err)
            // Use localStorage data as fallback
        }
    }

    async function loadFromAPI() {
        try {
            // Load enrollments from API
            await loadEnrollmentsFromAPI()

            // Load daily activity from API
            const response = await activityAPI.getDailyGoals()

            // Update daily activity from API
            dailyActivity.value = {
                date: response.todayActivity.date,
                wordsReviewed: response.todayActivity.wordsReviewed,
                exercisesCompleted: response.todayActivity.exercisesCompleted,
                episodesCompleted: response.todayActivity.episodesCompleted,
                goalsCompleted: response.todayActivity.goalsCompleted
            }

            // Update streak from API
            currentStreak.value = response.streak

            saveToStorage()
        } catch (err) {
            console.warn('Failed to load activity from API:', err)
            // Use localStorage data as fallback
        }
    }

    function clearProgress() {
        enrollments.value = []
        dailyActivity.value = {
            date: getTodayDate(),
            wordsReviewed: 0,
            exercisesCompleted: 0,
            episodesCompleted: 0,
            goalsCompleted: 0
        }
        activityHistory.value = []
        localStorage.removeItem(STORAGE_KEY)
    }

    function formatTimeThisWeek(): string {
        const weekAgo = new Date(Date.now() - 7 * 86400000).toISOString().split('T')[0] as string
        const weekActivities = activityHistory.value.filter(a => a.date >= weekAgo)

        // Include today's activity
        const today = dailyActivity.value
        const totalMinutes = weekActivities.reduce((sum, a) =>
                sum + (a.wordsReviewed * 0.5) + (a.exercisesCompleted * 2) + (a.episodesCompleted * 10), 0)
            + (today.wordsReviewed * 0.5) + (today.exercisesCompleted * 2) + (today.episodesCompleted * 10)

        const hours = Math.floor(totalMinutes / 60)
        const minutes = Math.round(totalMinutes % 60)
        return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`
    }

    // Initialize on store creation
    loadFromStorage()

    return {
        // State
        enrollments,
        dailyActivity,
        activityHistory,
        loading,
        error,
        // Computed
        dailyGoals,
        completedGoalsCount,
        streak,
        userStats,
        continueLearning,
        // Actions
        enrollInCourse,
        isEnrolled,
        getEnrollment,
        updateProgress,
        completeEpisode,
        recordActivity,
        loadFromStorage,
        loadFromAPI,
        loadEnrollmentsFromAPI,
        saveToStorage,
        clearProgress
    }
})
