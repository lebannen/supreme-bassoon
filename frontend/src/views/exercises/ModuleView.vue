<template>
  <div class="module-view-container">
    <div class="view-container">
      <div class="flex justify-between items-center mb-xl flex-wrap gap-md">
        <h1 class="module-title">{{ moduleTitle }}</h1>
        <div class="flex gap-md">
          <Select
            v-model="selectedLanguage"
            :options="languages"
            optionLabel="label"
            optionValue="value"
            @change="loadExercises"
          />
          <Select
            v-model="selectedModule"
            :options="modules"
            optionLabel="label"
            optionValue="value"
            @change="loadExercises"
          />
        </div>
      </div>

      <div v-if="loading" class="loading-state">
        <ProgressSpinner />
      </div>

      <Message v-else-if="error" severity="error">
        {{ error }}
      </Message>

      <div v-else-if="exerciseProgress.length === 0" class="empty-state">
        <i class="pi pi-inbox empty-icon"></i>
        <p class="empty-message">No exercises found for this module.</p>
        <p class="empty-hint">Try importing exercises from the admin panel.</p>
      </div>

      <div v-else class="content-area-lg">
        <Card v-if="moduleStats">
          <template #title>
            <div class="card-title-icon text-xl">
              <i class="pi pi-chart-line"></i>
              <span>Your Progress</span>
            </div>
          </template>
          <template #content>
            <div class="stats-grid mb-xl">
              <div class="flex gap-md items-start">
                <div class="stat-icon completion">
                  <i class="pi pi-check-circle"></i>
                </div>
                <div class="flex flex-col gap-xs">
                  <span class="stat-value-large"
                  >{{ Math.round(moduleStats.completionPercentage) }}%</span
                  >
                  <span class="stat-label">Completion</span>
                  <span class="text-xs text-secondary"
                  >{{ moduleStats.masteredExercises }} of
                    {{ moduleStats.totalExercises }} mastered</span
                  >
                </div>
              </div>

              <div class="flex gap-md items-start">
                <div class="stat-icon score">
                  <i class="pi pi-star"></i>
                </div>
                <div class="flex flex-col gap-xs">
                  <span class="stat-value-large">
                    {{
                      moduleStats.averageScore !== null
                          ? Math.round(moduleStats.averageScore) + '%'
                          : 'N/A'
                    }}
                  </span>
                  <span class="stat-label">Average Score</span>
                  <span class="text-xs text-secondary"
                  >Across {{ moduleStats.completedExercises }} attempts</span
                  >
                </div>
              </div>

              <div class="flex gap-md items-start">
                <div class="stat-icon time">
                  <i class="pi pi-clock"></i>
                </div>
                <div class="flex flex-col gap-xs">
                  <span class="stat-value-large">{{
                      formatTimeSpent(moduleStats.totalTimeSpentSeconds)
                    }}</span>
                  <span class="stat-label">Time Spent</span>
                  <span class="text-xs text-secondary"
                  >{{ totalDurationMinutes }} min estimated</span
                  >
                </div>
              </div>

              <div class="flex gap-md items-start">
                <div class="stat-icon exercises">
                  <i class="pi pi-book"></i>
                </div>
                <div class="flex flex-col gap-xs">
                  <span class="stat-value-large">{{ moduleStats.totalExercises }}</span>
                  <span class="stat-label">Total Exercises</span>
                  <span class="text-xs text-secondary"
                  >{{ uniqueTypes.length }} different types</span
                  >
                </div>
              </div>
            </div>

            <div class="progress-section">
              <div class="flex justify-between items-center mb-sm text-sm font-semibold">
                <span>Progress</span>
                <span class="progress-count"
                >{{ moduleStats.masteredExercises }}/{{ moduleStats.totalExercises }}</span
                >
              </div>
              <div class="progress-track">
                <div
                    class="progress-fill"
                    :style="{ width: moduleStats.completionPercentage + '%' }"
                ></div>
              </div>
            </div>
          </template>
        </Card>

        <div class="content-grid">
          <Card
            v-for="progress in exerciseProgress"
            :key="progress.exerciseId"
            class="exercise-card"
            :class="{ mastered: getExerciseStatus(progress) === 'MASTERED' }"
            @click="goToExercise(progress.exerciseId)"
          >
            <template #header>
              <div class="exercise-header">
                <Tag
                    :value="progress.exerciseType"
                    :severity="getTypeSeverity(progress.exerciseType)"
                />
                <div class="flex items-center gap-sm">
                  <span class="points-value">{{ progress.pointsValue || 0 }} pts</span>
                  <i
                      v-if="getExerciseStatus(progress) === 'MASTERED'"
                      class="pi pi-check-circle text-xl status-icon-mastered"
                      title="Mastered"
                  ></i>
                  <i
                      v-else-if="getExerciseStatus(progress) === 'IN_PROGRESS'"
                      class="pi pi-clock text-xl status-icon-in-progress"
                      title="In Progress"
                  ></i>
                </div>
              </div>
            </template>
            <template #title>
              <div class="flex justify-between items-center gap-sm">
                {{ progress.exerciseTitle }}
                <span v-if="progress.bestScore !== null" class="score-badge">
                  {{ Math.round(progress.bestScore) }}%
                </span>
              </div>
            </template>
            <template #content>
              <div class="exercise-meta">
                <div v-if="progress.estimatedDurationSeconds" class="meta-item">
                  <i class="pi pi-clock"></i>
                  <span>{{ formatDuration(progress.estimatedDurationSeconds) }}</span>
                </div>
                <div v-if="progress.difficultyRating" class="meta-item">
                  <i class="pi pi-chart-bar"></i>
                  <span>Difficulty: {{ progress.difficultyRating.toFixed(1) }}</span>
                </div>
                <div v-if="progress.topic" class="meta-item">
                  <i class="pi pi-tag"></i>
                  <span>{{ progress.topic }}</span>
                </div>
                <div v-if="progress.attemptCount > 0" class="meta-item">
                  <i class="pi pi-replay"></i>
                  <span
                  >{{ progress.attemptCount }} attempt{{
                      progress.attemptCount > 1 ? 's' : ''
                    }}</span
                  >
                </div>
              </div>
            </template>
            <template #footer>
              <Button
                  :label="getExerciseStatus(progress) === 'NOT_STARTED' ? 'Start Exercise' : 'Continue'"
                  :icon="getExerciseStatus(progress) === 'NOT_STARTED' ? 'pi pi-play' : 'pi pi-refresh'"
                  @click.stop="goToExercise(progress.exerciseId)"
                size="small"
              />
            </template>
          </Card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Select from 'primevue/select'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import {useCourseStore} from '@/stores/course'
import type {ExerciseProgress} from '@/types/course'

const router = useRouter()
const courseStore = useCourseStore()

const selectedLanguage = ref('fr')
const selectedModule = ref(1)

// Destructure store state with refs
const {moduleProgress: moduleStats, loading, error} = storeToRefs(courseStore)

// Extract exercises from module progress
const exerciseProgress = computed(() => moduleStats.value?.exercises || [])

const languages = [
  { label: 'French (Français)', value: 'fr' },
  { label: 'English', value: 'en' },
  {label: 'German (Deutsch)', value: 'de'},
]

const modules = [
  { label: 'Module 1', value: 1 },
  { label: 'Module 2', value: 2 },
  { label: 'Module 3', value: 3 },
  { label: 'Module 4', value: 4 },
  {label: 'Module 5', value: 5},
]

const moduleTitle = computed(() => {
  const lang = languages.find((l) => l.value === selectedLanguage.value)
  return `${lang?.label || 'Language'} - Module ${selectedModule.value}`
})

const uniqueTypes = computed(() => {
  return [...new Set(exerciseProgress.value.map((ep) => ep.exerciseType))]
})

const totalDurationMinutes = computed(() => {
  const totalSeconds = exerciseProgress.value.reduce(
      (sum, ep) => sum + (ep.estimatedDurationSeconds || 0),
      0
  )
  return Math.ceil(totalSeconds / 60)
})

// Compute status from isMastered and isCompleted flags
function getExerciseStatus(progress: ExerciseProgress): 'NOT_STARTED' | 'IN_PROGRESS' | 'MASTERED' {
  if (progress.isMastered) return 'MASTERED'
  if (progress.isCompleted) return 'IN_PROGRESS'
  return 'NOT_STARTED'
}

function loadExercises() {
  courseStore.loadModuleProgress(selectedLanguage.value, selectedModule.value)
}

function goToExercise(id: number) {
  router.push({ name: 'exercise-detail', params: { id } })
}

function getTypeSeverity(type: string): string {
  const severityMap: Record<string, string> = {
    multiple_choice: 'info',
    fill_in_blank: 'success',
    sentence_scramble: 'warn',
    matching: 'secondary',
    listening: 'contrast',
    cloze_reading: 'primary',
  }
  return severityMap[type] || 'info'
}

function formatDuration(seconds: number): string {
  if (seconds < 60) {
    return `${seconds}s`
  }
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return remainingSeconds > 0 ? `${minutes}m ${remainingSeconds}s` : `${minutes}m`
}

function formatTimeSpent(seconds: number): string {
  if (seconds === 0) {
    return '0m'
  }
  if (seconds < 60) {
    return `${seconds}s`
  }
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) {
    return `${minutes}m`
  }
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  return remainingMinutes > 0 ? `${hours}h ${remainingMinutes}m` : `${hours}h`
}

onMounted(() => {
  loadExercises()
})
</script>

<style scoped>
.module-view-container {
  padding: var(--spacing-2xl);
  min-height: 100vh;
  background: var(--surface-ground);
}

.module-title {
  margin: 0;
  font-size: 1.875rem;
  font-weight: 700;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-4xl);
  min-height: 400px;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: var(--spacing-md);
}

.empty-message {
  font-size: 1rem;
  margin: var(--spacing-md) 0;
}

.empty-hint {
  font-size: 0.875rem;
  margin: 0;
}

.progress-count {
  color: var(--primary-color);
}

.points-value {
  font-weight: 600;
  color: var(--primary-color);
}

.status-icon-mastered {
  color: var(--green-500);
}

.status-icon-in-progress {
  color: var(--orange-500);
}

.exercise-meta {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-sm);
}

.stat-icon {
  width: 3rem;
  height: 3rem;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  flex-shrink: 0;
}

.stat-icon.completion {
  background: var(--green-50);
  color: var(--green-600);
}

.stat-icon.score {
  background: var(--yellow-50);
  color: var(--yellow-600);
}

.stat-icon.time {
  background: var(--blue-50);
  color: var(--blue-600);
}

.stat-icon.exercises {
  background: var(--purple-50);
  color: var(--purple-600);
}

.stat-value-large {
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1;
}

.stat-label {
}

.progress-section {
  margin-top: var(--spacing-md);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--surface-border);
}

.progress-track {
  height: 0.75rem;
  background: var(--surface-100);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-color), var(--primary-600));
  border-radius: var(--radius-full);
  transition: width 0.5s ease;
}

.exercise-card {
  cursor: pointer;
  transition: all 0.2s;
  height: 100%;
}

.exercise-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.exercise-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md);
  background: var(--surface-50);
  border-bottom: 1px solid var(--surface-border);
}

.exercise-card.mastered {
  border: 2px solid var(--green-200);
  background: linear-gradient(135deg, var(--surface-card) 0%, var(--green-50) 100%);
}

.score-badge {
  font-weight: 700;
  color: var(--green-600);
  background: var(--green-50);
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.meta-item i {
  color: var(--primary-color);
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
