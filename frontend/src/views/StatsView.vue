<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import ProgressBar from 'primevue/progressbar'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const router = useRouter()

interface RecentActivity {
  exerciseId: number
  exerciseTitle: string
  exerciseType: string
  score: number
  isCorrect: boolean
  completedAt: string
}

interface ModuleProgressSummary {
  moduleNumber: number
  totalExercises: number
  masteredExercises: number
  completionPercentage: number
}

interface LanguageProgress {
  languageCode: string
  totalExercises: number
  completedExercises: number
  masteredExercises: number
  averageScore: number | null
  moduleProgress: Record<number, ModuleProgressSummary>
}

interface UserStats {
  totalExercisesAvailable: number
  totalExercisesCompleted: number
  totalExercisesMastered: number
  overallAverageScore: number | null
  totalTimeSpentSeconds: number
  totalAttempts: number
  currentStreak: number
  longestStreak: number
  recentActivity: RecentActivity[]
  progressByLanguage: Record<string, LanguageProgress>
}

const stats = ref<UserStats | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

function getAuthHeaders() {
  const token = localStorage.getItem('auth_token')
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
}

function formatTime(seconds: number): string {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)

  if (hours > 0) {
    return `${hours}h ${minutes}m`
  }
  return `${minutes}m`
}

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffHours / 24)

  if (diffHours < 1) {
    return 'Just now'
  } else if (diffHours < 24) {
    return `${diffHours}h ago`
  } else if (diffDays === 1) {
    return 'Yesterday'
  } else if (diffDays < 7) {
    return `${diffDays} days ago`
  } else {
    return date.toLocaleDateString()
  }
}

const languageProgressList = computed(() => {
  if (!stats.value) return []
  return Object.values(stats.value.progressByLanguage)
})

const moduleProgressFlat = computed(() => {
  const modules: Array<{
    languageCode: string
    moduleNumber: number
    totalExercises: number
    masteredExercises: number
    completionPercentage: number
  }> = []

  languageProgressList.value.forEach(lang => {
    Object.values(lang.moduleProgress).forEach(module => {
      modules.push({
        languageCode: lang.languageCode,
        ...module
      })
    })
  })

  return modules.sort((a, b) => {
    if (a.languageCode !== b.languageCode) {
      return a.languageCode.localeCompare(b.languageCode)
    }
    return a.moduleNumber - b.moduleNumber
  })
})

const completionPercentage = computed(() => {
  if (!stats.value || stats.value.totalExercisesAvailable === 0) return 0
  return (stats.value.totalExercisesMastered / stats.value.totalExercisesAvailable) * 100
})

async function loadStats() {
  try {
    loading.value = true
    error.value = null

    const response = await fetch(`${API_BASE}/api/exercises/stats`, {
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      if (response.status === 401) {
        router.push('/login')
        return
      }
      throw new Error('Failed to load statistics')
    }

    stats.value = await response.json()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load statistics'
    console.error('Error loading stats:', err)
  } finally {
    loading.value = false
  }
}

function navigateToExercise(exerciseId: number) {
  router.push(`/exercises/${exerciseId}`)
}

onMounted(() => {
  loadStats()
})
</script>

<template>
  <div class="stats-container">
    <div class="stats-content">
      <!-- Header -->
      <div class="header">
        <h1 class="stats-title">
          <i class="pi pi-chart-bar"></i>
          Your Statistics
        </h1>
      </div>

      <Message v-if="error" severity="error" :closable="false" class="error-message">
        {{ error }}
      </Message>

      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner" style="font-size: 3rem; color: var(--primary-color)"></i>
        <p>Loading your statistics...</p>
      </div>

      <div v-else-if="stats" class="stats-grid">
        <!-- Overview Stats -->
        <Card class="overview-card">
          <template #title>
            <div class="card-title">
              <i class="pi pi-chart-line"></i>
              <span>Overview</span>
            </div>
          </template>
          <template #content>
            <div class="overview-stats-grid">
              <div class="overview-stat mastered">
                <div class="stat-icon-wrapper">
                  <i class="pi pi-trophy"></i>
                </div>
                <div class="stat-details">
                  <span class="stat-value-large">{{ stats.totalExercisesMastered }}</span>
                  <span class="stat-label">Mastered</span>
                  <span class="stat-sublabel">out of {{ stats.totalExercisesAvailable }} exercises</span>
                </div>
              </div>

              <div class="overview-stat score">
                <div class="stat-icon-wrapper">
                  <i class="pi pi-star"></i>
                </div>
                <div class="stat-details">
                  <span class="stat-value-large">
                    {{ stats.overallAverageScore ? Math.round(stats.overallAverageScore) : 0 }}%
                  </span>
                  <span class="stat-label">Average Score</span>
                  <span class="stat-sublabel">across {{ stats.totalAttempts }} attempts</span>
                </div>
              </div>

              <div class="overview-stat time">
                <div class="stat-icon-wrapper">
                  <i class="pi pi-clock"></i>
                </div>
                <div class="stat-details">
                  <span class="stat-value-large">{{ formatTime(stats.totalTimeSpentSeconds) }}</span>
                  <span class="stat-label">Time Spent</span>
                  <span class="stat-sublabel">studying exercises</span>
                </div>
              </div>

              <div class="overview-stat completed">
                <div class="stat-icon-wrapper">
                  <i class="pi pi-check-circle"></i>
                </div>
                <div class="stat-details">
                  <span class="stat-value-large">{{ stats.totalExercisesCompleted }}</span>
                  <span class="stat-label">Completed</span>
                  <span class="stat-sublabel">unique exercises</span>
                </div>
              </div>
            </div>

            <div class="progress-section">
              <div class="progress-header">
                <span>Overall Progress</span>
                <span class="progress-percentage">{{ Math.round(completionPercentage) }}%</span>
              </div>
              <ProgressBar :value="completionPercentage" class="overall-progress-bar" />
            </div>
          </template>
        </Card>

        <!-- Streaks -->
        <Card class="streaks-card">
          <template #title>
            <div class="card-title">
              <i class="pi pi-bolt"></i>
              <span>Streaks</span>
            </div>
          </template>
          <template #content>
            <div class="streaks-grid">
              <div class="streak-stat current">
                <i class="pi pi-calendar"></i>
                <div class="streak-info">
                  <span class="streak-value">{{ stats.currentStreak }}</span>
                  <span class="streak-label">Current Streak</span>
                  <span class="streak-sublabel">days in a row</span>
                </div>
              </div>
              <div class="streak-stat longest">
                <i class="pi pi-crown"></i>
                <div class="streak-info">
                  <span class="streak-value">{{ stats.longestStreak }}</span>
                  <span class="streak-label">Longest Streak</span>
                  <span class="streak-sublabel">personal record</span>
                </div>
              </div>
            </div>
          </template>
        </Card>

        <!-- Recent Activity -->
        <Card class="recent-activity-card">
          <template #title>
            <div class="card-title">
              <i class="pi pi-history"></i>
              <span>Recent Activity</span>
            </div>
          </template>
          <template #content>
            <div v-if="stats.recentActivity.length === 0" class="empty-state">
              <i class="pi pi-inbox"></i>
              <p>No activity yet. Start practicing to see your progress here!</p>
            </div>
            <div v-else class="activity-list">
              <div
                v-for="activity in stats.recentActivity"
                :key="`${activity.exerciseId}-${activity.completedAt}`"
                class="activity-item"
                @click="navigateToExercise(activity.exerciseId)"
              >
                <div class="activity-header">
                  <Tag :value="activity.exerciseType" class="type-tag" />
                  <span class="activity-time">{{ formatDate(activity.completedAt) }}</span>
                </div>
                <div class="activity-title">{{ activity.exerciseTitle }}</div>
                <div class="activity-footer">
                  <div class="activity-score" :class="{ correct: activity.isCorrect }">
                    <i :class="activity.isCorrect ? 'pi pi-check-circle' : 'pi pi-times-circle'"></i>
                    <span>{{ Math.round(activity.score) }}%</span>
                  </div>
                  <i class="pi pi-arrow-right"></i>
                </div>
              </div>
            </div>
          </template>
        </Card>

        <!-- Language Progress -->
        <Card class="language-progress-card" v-for="lang in languageProgressList" :key="lang.languageCode">
          <template #title>
            <div class="card-title">
              <i class="pi pi-globe"></i>
              <span>{{ lang.languageCode.toUpperCase() }} Progress</span>
            </div>
          </template>
          <template #content>
            <div class="language-stats">
              <div class="language-stat">
                <span class="stat-label">Total Exercises</span>
                <span class="stat-value">{{ lang.totalExercises }}</span>
              </div>
              <div class="language-stat">
                <span class="stat-label">Completed</span>
                <span class="stat-value">{{ lang.completedExercises }}</span>
              </div>
              <div class="language-stat">
                <span class="stat-label">Mastered</span>
                <span class="stat-value">{{ lang.masteredExercises }}</span>
              </div>
              <div class="language-stat">
                <span class="stat-label">Average Score</span>
                <span class="stat-value">{{ lang.averageScore ? Math.round(lang.averageScore) + '%' : 'N/A' }}</span>
              </div>
            </div>

            <div v-if="Object.keys(lang.moduleProgress).length > 0" class="modules-section">
              <h4>Modules</h4>
              <div class="modules-grid">
                <div
                  v-for="module in Object.values(lang.moduleProgress)"
                  :key="module.moduleNumber"
                  class="module-card"
                >
                  <div class="module-header">
                    <span class="module-number">Module {{ module.moduleNumber }}</span>
                    <span class="module-percentage">{{ Math.round(module.completionPercentage) }}%</span>
                  </div>
                  <ProgressBar :value="module.completionPercentage" class="module-progress" />
                  <div class="module-stats">
                    <span>{{ module.masteredExercises }} / {{ module.totalExercises }} mastered</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stats-container {
  min-height: 100vh;
  background: var(--surface-ground);
  padding: 2rem 1rem;
}

.stats-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.header {
  margin-bottom: 1rem;
}

.stats-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--text-color);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stats-title i {
  color: var(--primary-color);
  font-size: 2rem;
}

.error-message {
  margin-bottom: 1rem;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  gap: 1rem;
}

.loading-state p {
  color: var(--text-color-secondary);
  font-size: 1.125rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--text-color);
}

.card-title i {
  color: var(--primary-color);
}

/* Overview Card */
.overview-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.overview-stat {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem;
  border-radius: 12px;
  background: var(--surface-50);
}

.stat-icon-wrapper {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  flex-shrink: 0;
}

.overview-stat.mastered .stat-icon-wrapper {
  background: var(--yellow-50);
  color: var(--yellow-600);
}

.overview-stat.score .stat-icon-wrapper {
  background: var(--purple-50);
  color: var(--purple-600);
}

.overview-stat.time .stat-icon-wrapper {
  background: var(--blue-50);
  color: var(--blue-600);
}

.overview-stat.completed .stat-icon-wrapper {
  background: var(--green-50);
  color: var(--green-600);
}

.stat-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-value-large {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-color);
  line-height: 1;
}

.stat-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-color);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-sublabel {
  font-size: 0.75rem;
  color: var(--text-color-secondary);
}

.progress-section {
  padding-top: 1.5rem;
  border-top: 1px solid var(--surface-border);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  font-weight: 600;
  color: var(--text-color);
}

.progress-percentage {
  font-size: 1.25rem;
  color: var(--primary-color);
}

.overall-progress-bar {
  height: 1.5rem;
  border-radius: 8px;
}

/* Streaks Card */
.streaks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
}

.streak-stat {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 1.5rem;
  border-radius: 12px;
}

.streak-stat.current {
  background: linear-gradient(135deg, var(--orange-50) 0%, var(--orange-100) 100%);
}

.streak-stat.longest {
  background: linear-gradient(135deg, var(--purple-50) 0%, var(--purple-100) 100%);
}

.streak-stat > i {
  font-size: 3rem;
}

.streak-stat.current > i {
  color: var(--orange-500);
}

.streak-stat.longest > i {
  color: var(--purple-500);
}

.streak-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.streak-value {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--text-color);
  line-height: 1;
}

.streak-label {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-color);
}

.streak-sublabel {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

/* Recent Activity */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.activity-item {
  padding: 1rem;
  border: 1px solid var(--surface-border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--surface-card);
}

.activity-item:hover {
  border-color: var(--primary-color);
  background: var(--primary-50);
  transform: translateX(4px);
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.type-tag {
  font-size: 0.75rem;
}

.activity-time {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.activity-title {
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 0.5rem;
}

.activity-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.activity-score {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
}

.activity-score.correct {
  color: var(--green-600);
}

.activity-score:not(.correct) {
  color: var(--red-600);
}

.activity-footer > i {
  color: var(--text-color-secondary);
}

/* Language Progress */
.language-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--surface-border);
}

.language-stat {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.language-stat .stat-label {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.language-stat .stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-color);
}

.modules-section h4 {
  margin: 0 0 1rem 0;
  color: var(--text-color);
  font-weight: 600;
}

.modules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1rem;
}

.module-card {
  padding: 1rem;
  border: 1px solid var(--surface-border);
  border-radius: 8px;
  background: var(--surface-50);
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.module-number {
  font-weight: 600;
  color: var(--text-color);
}

.module-percentage {
  font-weight: 700;
  color: var(--primary-color);
}

.module-progress {
  height: 0.5rem;
  margin-bottom: 0.5rem;
}

.module-stats {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.empty-state {
  text-align: center;
  padding: 3rem 2rem;
  color: var(--text-color-secondary);
}

.empty-state i {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

/* Responsive */
@media (max-width: 768px) {
  .stats-container {
    padding: 1rem;
  }

  .stats-title {
    font-size: 2rem;
  }

  .overview-stats-grid {
    grid-template-columns: 1fr;
  }

  .streaks-grid {
    grid-template-columns: 1fr;
  }

  .language-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .modules-grid {
    grid-template-columns: 1fr;
  }
}
</style>
