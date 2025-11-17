<script setup lang="ts">
import {computed, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import ProgressBar from 'primevue/progressbar'
import {useExerciseStore} from '@/stores/exercise'

const router = useRouter()
const exerciseStore = useExerciseStore()

// Destructure store state with refs
const {stats, loading, error} = storeToRefs(exerciseStore)

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

const completionPercentage = computed(() => {
  if (!stats.value || stats.value.totalExercisesAvailable === 0) return 0
  return (stats.value.totalExercisesMastered / stats.value.totalExercisesAvailable) * 100
})

function navigateToExercise(exerciseId: number) {
  router.push(`/exercises/${exerciseId}`)
}

onMounted(() => {
  exerciseStore.loadStats()
})
</script>

<template>
  <div class="p-2xl stats-page-container">
    <div class="view-container content-area-lg">
      <!-- Header -->
      <div class="page-header">
        <h1 class="flex items-center gap-md">
          <i class="pi pi-chart-bar text-3xl icon-primary"></i>
          Your Statistics
        </h1>
      </div>

      <Message v-if="error" severity="error" :closable="false" class="mb-md">
        {{ error }}
      </Message>

      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner text-3xl icon-primary"></i>
        <p class="text-lg text-secondary">Loading your statistics...</p>
      </div>

      <div v-else-if="stats" class="flex flex-col gap-lg">
        <!-- Overview Stats -->
        <Card>
          <template #title>
            <div class="card-title-icon">
              <i class="pi pi-chart-line"></i>
              <span>Overview</span>
            </div>
          </template>
          <template #content>
            <div class="stats-grid mb-2xl">
              <div class="overview-stat mastered">
                <div class="stat-icon-wrapper">
                  <i class="pi pi-trophy"></i>
                </div>
                <div class="flex flex-col gap-xs">
                  <span class="text-3xl font-bold text-primary">{{
                      stats.totalExercisesMastered
                    }}</span>
                  <span class="text-sm font-semibold text-primary stat-label">Mastered</span>
                  <span class="text-xs text-secondary"
                  >out of {{ stats.totalExercisesAvailable }} exercises</span
                  >
                </div>
              </div>

              <div class="overview-stat score">
                <div class="stat-icon-wrapper">
                  <i class="pi pi-star"></i>
                </div>
                <div class="flex flex-col gap-xs">
                  <span class="text-3xl font-bold text-primary">
                    {{ stats.overallAverageScore ? Math.round(stats.overallAverageScore) : 0 }}%
                  </span>
                  <span class="text-sm font-semibold text-primary stat-label">Average Score</span>
                  <span class="text-xs text-secondary"
                  >across {{ stats.totalAttempts }} attempts</span
                  >
                </div>
              </div>

              <div class="overview-stat time">
                <div class="stat-icon-wrapper">
                  <i class="pi pi-clock"></i>
                </div>
                <div class="flex flex-col gap-xs">
                  <span class="text-3xl font-bold text-primary">{{
                      formatTime(stats.totalTimeSpentSeconds)
                    }}</span>
                  <span class="text-sm font-semibold text-primary stat-label">Time Spent</span>
                  <span class="text-xs text-secondary">studying exercises</span>
                </div>
              </div>

              <div class="overview-stat completed">
                <div class="stat-icon-wrapper">
                  <i class="pi pi-check-circle"></i>
                </div>
                <div class="flex flex-col gap-xs">
                  <span class="text-3xl font-bold text-primary">{{
                      stats.totalExercisesCompleted
                    }}</span>
                  <span class="text-sm font-semibold text-primary stat-label">Completed</span>
                  <span class="text-xs text-secondary">unique exercises</span>
                </div>
              </div>
            </div>

            <div class="progress-section">
              <div class="flex justify-between items-center mb-sm font-semibold text-primary">
                <span>Overall Progress</span>
                <span class="text-xl progress-percentage"
                >{{ Math.round(completionPercentage) }}%</span
                >
              </div>
              <ProgressBar :value="completionPercentage" class="overall-progress-bar" />
            </div>
          </template>
        </Card>

        <!-- Streaks -->
        <Card>
          <template #title>
            <div class="card-title-icon">
              <i class="pi pi-bolt"></i>
              <span>Streaks</span>
            </div>
          </template>
          <template #content>
            <div class="streaks-grid">
              <div class="streak-stat current">
                <i class="pi pi-calendar text-3xl streak-icon-warning"></i>
                <div class="flex flex-col gap-xs">
                  <span class="text-4xl font-bold text-primary streak-number">{{
                      stats.currentStreak
                    }}</span>
                  <span class="text-base font-semibold text-primary">Current Streak</span>
                  <span class="text-sm text-secondary">days in a row</span>
                </div>
              </div>
              <div class="streak-stat longest">
                <i class="pi pi-crown text-3xl streak-icon-purple"></i>
                <div class="flex flex-col gap-xs">
                  <span class="text-4xl font-bold text-primary streak-number">{{
                      stats.longestStreak
                    }}</span>
                  <span class="text-base font-semibold text-primary">Longest Streak</span>
                  <span class="text-sm text-secondary">personal record</span>
                </div>
              </div>
            </div>
          </template>
        </Card>

        <!-- Recent Activity -->
        <Card>
          <template #title>
            <div class="card-title-icon">
              <i class="pi pi-history"></i>
              <span>Recent Activity</span>
            </div>
          </template>
          <template #content>
            <div v-if="stats.recentActivity.length === 0" class="empty-state">
              <i class="pi pi-inbox empty-icon"></i>
              <p>No activity yet. Start practicing to see your progress here!</p>
            </div>
            <div v-else class="flex flex-col gap-sm">
              <div
                v-for="activity in stats.recentActivity"
                :key="`${activity.exerciseId}-${activity.completedAt}`"
                class="activity-item"
                @click="navigateToExercise(activity.exerciseId)"
              >
                <div class="flex justify-between items-center mb-xs">
                  <Tag :value="activity.exerciseType" class="text-xs"/>
                  <span class="text-sm text-secondary">{{ formatDate(activity.completedAt) }}</span>
                </div>
                <div class="font-semibold text-primary mb-xs">{{ activity.exerciseTitle }}</div>
                <div class="flex justify-between items-center">
                  <div
                      class="flex items-center gap-xs font-semibold"
                      :class="{ correct: activity.isCorrect }"
                  >
                    <i
                        :class="activity.isCorrect ? 'pi pi-check-circle' : 'pi pi-times-circle'"
                    ></i>
                    <span>{{ Math.round(activity.score) }}%</span>
                  </div>
                  <i class="pi pi-arrow-right text-secondary"></i>
                </div>
              </div>
            </div>
          </template>
        </Card>

        <!-- Language Progress -->
        <Card v-for="lang in languageProgressList" :key="lang.languageCode">
          <template #title>
            <div class="card-title-icon">
              <i class="pi pi-globe"></i>
              <span>{{ lang.languageCode.toUpperCase() }} Progress</span>
            </div>
          </template>
          <template #content>
            <div class="language-stats">
              <div class="flex flex-col gap-xs">
                <span class="text-sm text-secondary stat-label">Total Exercises</span>
                <span class="text-2xl font-bold text-primary">{{ lang.totalExercises }}</span>
              </div>
              <div class="flex flex-col gap-xs">
                <span class="text-sm text-secondary stat-label">Completed</span>
                <span class="text-2xl font-bold text-primary">{{ lang.completedExercises }}</span>
              </div>
              <div class="flex flex-col gap-xs">
                <span class="text-sm text-secondary stat-label">Mastered</span>
                <span class="text-2xl font-bold text-primary">{{ lang.masteredExercises }}</span>
              </div>
              <div class="flex flex-col gap-xs">
                <span class="text-sm text-secondary stat-label">Average Score</span>
                <span class="text-2xl font-bold text-primary">{{
                    lang.averageScore ? Math.round(lang.averageScore) + '%' : 'N/A'
                  }}</span>
              </div>
            </div>

            <div v-if="Object.keys(lang.moduleProgress).length > 0" class="modules-section">
              <h4 class="mb-md text-primary font-semibold modules-heading">Modules</h4>
              <div class="modules-grid">
                <div
                  v-for="module in Object.values(lang.moduleProgress)"
                  :key="module.moduleNumber"
                  class="module-card"
                >
                  <div class="flex justify-between items-center mb-sm">
                    <span class="font-semibold text-primary">Module {{ module.moduleNumber }}</span>
                    <span class="font-bold module-percentage"
                    >{{ Math.round(module.completionPercentage) }}%</span
                    >
                  </div>
                  <ProgressBar :value="module.completionPercentage" class="module-progress" />
                  <div class="text-sm text-secondary">
                    <span
                    >{{ module.masteredExercises }} / {{ module.totalExercises }} mastered</span
                    >
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
/* Page container */
.stats-page-container {
  min-height: 100vh;
  background: var(--bg-primary);
}

.icon-primary {
  color: var(--primary);
}

.stat-label {
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.progress-percentage {
  color: var(--primary);
}

.streak-icon-warning {
  color: var(--warning);
}

.streak-icon-purple {
  color: #8b5cf6;
}

.streak-number {
  line-height: 1;
}

.modules-heading {
  margin-top: 0;
}

.module-percentage {
  color: var(--primary);
}

/* Overview stat icons with colored backgrounds */
.stat-icon-wrapper {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  flex-shrink: 0;
}

.overview-stat.mastered .stat-icon-wrapper {
  background: #fef3c7;
  color: #d97706;
}

.overview-stat.score .stat-icon-wrapper {
  background: #f3e8ff;
  color: #8b5cf6;
}

.overview-stat.time .stat-icon-wrapper {
  background: #dbeafe;
  color: #3b82f6;
}

.overview-stat.completed .stat-icon-wrapper {
  background: #d1fae5;
  color: #10b981;
}

.overview-stat {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem;
  border-radius: var(--radius-lg);
  background: var(--bg-tertiary);
}

/* Progress section */
.progress-section {
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--border-medium);
}

.overall-progress-bar {
  height: 1.5rem;
  border-radius: var(--radius-md);
}

/* Streaks */
.streaks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-lg);
}

.streak-stat {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
}

.streak-stat.current {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
}

.streak-stat.longest {
  background: linear-gradient(135deg, #f3e8ff 0%, #e9d5ff 100%);
}

/* Activity items */
.activity-item {
  padding: 1rem;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  background: var(--bg-secondary);
}

.activity-item:hover {
  border-color: var(--primary);
  background: var(--primary-light);
  transform: translateX(4px);
}

.activity-score.correct {
  color: var(--success);
}

.activity-score:not(.correct) {
  color: var(--error);
}

/* Language progress */
.language-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-2xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-medium);
}

/* Module cards */
.modules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1rem;
}

.module-card {
  padding: 1rem;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
  background: var(--bg-tertiary);
}

.module-progress {
  height: 0.5rem;
  margin-bottom: var(--spacing-xs);
}

/* Responsive */
@media (max-width: 768px) {
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
