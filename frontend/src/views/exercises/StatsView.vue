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
  <div class="page-container-with-padding">
    <div class="view-container content-area-lg">
      <!-- Header -->
      <div class="page-header">
        <h1 class="flex align-items-center gap-3">
          <i class="pi pi-chart-bar text-3xl icon-primary"></i>
          Your Statistics
        </h1>
      </div>

      <Message v-if="error" severity="error" :closable="false" class="mb-3">
        {{ error }}
      </Message>

      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner text-3xl icon-primary"></i>
        <p class="text-lg text-secondary">Loading your statistics...</p>
      </div>

      <div v-else-if="stats" class="flex flex-column gap-4">
        <!-- Overview Stats -->
        <Card>
          <template #title>
            <div class="card-title-icon">
              <i class="pi pi-chart-line"></i>
              <span>Overview</span>
            </div>
          </template>
          <template #content>
            <div class="stats-grid mb-6">
              <div class="stat">
                <div class="stat-icon stat-icon-yellow">
                  <i class="pi pi-trophy"></i>
                </div>
                <div class="flex flex-column gap-1">
                  <span class="text-3xl font-bold text-primary">{{
                      stats.totalExercisesMastered
                    }}</span>
                  <span class="text-sm font-semibold text-primary text-uppercase">Mastered</span>
                  <span class="text-xs text-secondary"
                  >out of {{ stats.totalExercisesAvailable }} exercises</span
                  >
                </div>
              </div>

              <div class="stat">
                <div class="stat-icon stat-icon-purple">
                  <i class="pi pi-star"></i>
                </div>
                <div class="flex flex-column gap-1">
                  <span class="text-3xl font-bold text-primary">
                    {{ stats.overallAverageScore ? Math.round(stats.overallAverageScore) : 0 }}%
                  </span>
                  <span class="text-sm font-semibold text-primary text-uppercase">Average Score</span>
                  <span class="text-xs text-secondary"
                  >across {{ stats.totalAttempts }} attempts</span
                  >
                </div>
              </div>

              <div class="stat">
                <div class="stat-icon stat-icon-blue">
                  <i class="pi pi-clock"></i>
                </div>
                <div class="flex flex-column gap-1">
                  <span class="text-3xl font-bold text-primary">{{
                      formatTime(stats.totalTimeSpentSeconds)
                    }}</span>
                  <span class="text-sm font-semibold text-primary text-uppercase">Time Spent</span>
                  <span class="text-xs text-secondary">studying exercises</span>
                </div>
              </div>

              <div class="stat">
                <div class="stat-icon stat-icon-green">
                  <i class="pi pi-check-circle"></i>
                </div>
                <div class="flex flex-column gap-1">
                  <span class="text-3xl font-bold text-primary">{{
                      stats.totalExercisesCompleted
                    }}</span>
                  <span class="text-sm font-semibold text-primary text-uppercase">Completed</span>
                  <span class="text-xs text-secondary">unique exercises</span>
                </div>
              </div>
            </div>

            <div class="divider-top">
              <div class="flex justify-content-between align-items-center mb-2 font-semibold text-primary">
                <span>Overall Progress</span>
                <span class="text-xl text-primary"
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
            <div class="stats-grid">
              <div class="flex align-items-center gap-4 p-4 gradient-warning" style="border-radius: var(--radius-lg)">
                <i class="pi pi-calendar text-3xl"></i>
                <div class="flex flex-column gap-1">
                  <span class="text-4xl font-bold line-height-tight">{{
                      stats.currentStreak
                    }}</span>
                  <span class="text-base font-semibold">Current Streak</span>
                  <span class="text-sm">days in a row</span>
                </div>
              </div>
              <div class="flex align-items-center gap-4 p-4 gradient-purple" style="border-radius: var(--radius-lg)">
                <i class="pi pi-crown text-3xl"></i>
                <div class="flex flex-column gap-1">
                  <span class="text-4xl font-bold line-height-tight">{{
                      stats.longestStreak
                    }}</span>
                  <span class="text-base font-semibold">Longest Streak</span>
                  <span class="text-sm">personal record</span>
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
            <div v-else class="flex flex-column gap-2">
              <div
                v-for="activity in stats.recentActivity"
                :key="`${activity.exerciseId}-${activity.completedAt}`"
                class="list-item-interactive"
                @click="navigateToExercise(activity.exerciseId)"
              >
                <div class="flex justify-content-between align-items-center mb-1">
                  <Tag :value="activity.exerciseType" class="text-xs"/>
                  <span class="text-sm text-secondary">{{ formatDate(activity.completedAt) }}</span>
                </div>
                <div class="font-semibold text-primary mb-1">{{ activity.exerciseTitle }}</div>
                <div class="flex justify-content-between align-items-center">
                  <div
                      class="flex align-items-center gap-1 font-semibold"
                      :class="activity.isCorrect ? 'text-success' : 'text-error'"
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
              <div class="flex flex-column gap-1">
                <span class="text-sm text-secondary text-uppercase">Total Exercises</span>
                <span class="text-2xl font-bold text-primary">{{ lang.totalExercises }}</span>
              </div>
              <div class="flex flex-column gap-1">
                <span class="text-sm text-secondary text-uppercase">Completed</span>
                <span class="text-2xl font-bold text-primary">{{ lang.completedExercises }}</span>
              </div>
              <div class="flex flex-column gap-1">
                <span class="text-sm text-secondary text-uppercase">Mastered</span>
                <span class="text-2xl font-bold text-primary">{{ lang.masteredExercises }}</span>
              </div>
              <div class="flex flex-column gap-1">
                <span class="text-sm text-secondary text-uppercase">Average Score</span>
                <span class="text-2xl font-bold text-primary">{{
                    lang.averageScore ? Math.round(lang.averageScore) + '%' : 'N/A'
                  }}</span>
              </div>
            </div>

            <div v-if="Object.keys(lang.moduleProgress).length > 0" class="modules-section">
              <h4 class="mb-3 text-primary font-semibold">Modules</h4>
              <div class="content-grid">
                <div
                  v-for="module in Object.values(lang.moduleProgress)"
                  :key="module.moduleNumber"
                  class="module-card"
                >
                  <div class="flex justify-content-between align-items-center mb-2">
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
/* Custom progress bar heights */
.overall-progress-bar {
  height: 1.5rem;
  border-radius: var(--radius-md);
}

.module-progress {
  height: 0.5rem;
  margin-bottom: var(--spacing-xs);
}

/* Language progress grid */
.language-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-2xl);
  padding-bottom: var(--spacing-lg);
}

/* Module cards */
.module-card {
  padding: 1rem;
  border-radius: var(--radius-md);
}

/* Responsive */
@media (max-width: 768px) {
  .language-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
