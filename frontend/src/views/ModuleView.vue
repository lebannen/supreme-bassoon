<template>
  <div class="module-view">
    <div class="container">
      <div class="header">
        <h1>{{ moduleTitle }}</h1>
        <div class="header-actions">
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

      <div v-if="loading" class="loading">
        <ProgressSpinner />
      </div>

      <Message v-else-if="error" severity="error">
        {{ error }}
      </Message>

      <div v-else-if="exerciseProgress.length === 0" class="empty-state">
        <i class="pi pi-inbox" style="font-size: 3rem; color: var(--text-color-secondary)"></i>
        <p>No exercises found for this module.</p>
        <p class="hint">Try importing exercises from the admin panel.</p>
      </div>

      <div v-else class="exercises-container">
        <Card class="progress-summary-card" v-if="moduleStats">
          <template #title>
            <div class="summary-title">
              <i class="pi pi-chart-line"></i>
              <span>Your Progress</span>
            </div>
          </template>
          <template #content>
            <div class="progress-stats-grid">
              <div class="progress-stat">
                <div class="stat-icon-wrapper completion">
                  <i class="pi pi-check-circle"></i>
                </div>
                <div class="stat-details">
                  <span class="stat-value-large">{{ Math.round(moduleStats.completionPercentage) }}%</span>
                  <span class="stat-label">Completion</span>
                  <span class="stat-sublabel">{{ moduleStats.masteredExercises }} of {{ moduleStats.totalExercises }} mastered</span>
                </div>
              </div>

              <div class="progress-stat">
                <div class="stat-icon-wrapper score">
                  <i class="pi pi-star"></i>
                </div>
                <div class="stat-details">
                  <span class="stat-value-large">
                    {{ moduleStats.averageScore !== null ? Math.round(moduleStats.averageScore) + '%' : 'N/A' }}
                  </span>
                  <span class="stat-label">Average Score</span>
                  <span class="stat-sublabel">Across {{ moduleStats.completedExercises }} attempts</span>
                </div>
              </div>

              <div class="progress-stat">
                <div class="stat-icon-wrapper time">
                  <i class="pi pi-clock"></i>
                </div>
                <div class="stat-details">
                  <span class="stat-value-large">{{ formatTimeSpent(moduleStats.totalTimeSpentSeconds) }}</span>
                  <span class="stat-label">Time Spent</span>
                  <span class="stat-sublabel">{{ totalDurationMinutes }} min estimated</span>
                </div>
              </div>

              <div class="progress-stat">
                <div class="stat-icon-wrapper exercises">
                  <i class="pi pi-book"></i>
                </div>
                <div class="stat-details">
                  <span class="stat-value-large">{{ moduleStats.totalExercises }}</span>
                  <span class="stat-label">Total Exercises</span>
                  <span class="stat-sublabel">{{ uniqueTypes.length }} different types</span>
                </div>
              </div>
            </div>

            <div class="progress-bar-section">
              <div class="progress-bar-labels">
                <span>Progress</span>
                <span class="progress-percentage">{{ moduleStats.masteredExercises }}/{{ moduleStats.totalExercises }}</span>
              </div>
              <div class="progress-bar-track">
                <div
                  class="progress-bar-fill"
                  :style="{ width: moduleStats.completionPercentage + '%' }"
                ></div>
              </div>
            </div>
          </template>
        </Card>

        <div class="exercises-grid">
          <Card
            v-for="progress in exerciseProgress"
            :key="progress.exercise.id"
            class="exercise-card"
            :class="{ 'mastered': progress.status === 'MASTERED' }"
            @click="goToExercise(progress.exercise.id)"
          >
            <template #header>
              <div class="exercise-card-header">
                <Tag :value="progress.exercise.type" :severity="getTypeSeverity(progress.exercise.type)" />
                <div class="header-right">
                  <span class="exercise-points">{{ progress.exercise.pointsValue }} pts</span>
                  <i v-if="progress.status === 'MASTERED'" class="pi pi-check-circle status-icon mastered" title="Mastered"></i>
                  <i v-else-if="progress.status === 'IN_PROGRESS'" class="pi pi-clock status-icon in-progress" title="In Progress"></i>
                </div>
              </div>
            </template>
            <template #title>
              <div class="exercise-title-wrapper">
                {{ progress.exercise.title }}
                <span v-if="progress.bestScore !== null" class="best-score">
                  {{ Math.round(progress.bestScore) }}%
                </span>
              </div>
            </template>
            <template #content>
              <div class="exercise-meta">
                <div class="meta-item">
                  <i class="pi pi-clock"></i>
                  <span>{{ formatDuration(progress.exercise.estimatedDurationSeconds) }}</span>
                </div>
                <div class="meta-item">
                  <i class="pi pi-chart-bar"></i>
                  <span>Difficulty: {{ progress.exercise.difficultyRating.toFixed(1) }}</span>
                </div>
                <div v-if="progress.exercise.topic" class="meta-item">
                  <i class="pi pi-tag"></i>
                  <span>{{ progress.exercise.topic }}</span>
                </div>
                <div v-if="progress.attemptsCount > 0" class="meta-item">
                  <i class="pi pi-replay"></i>
                  <span>{{ progress.attemptsCount }} attempt{{ progress.attemptsCount > 1 ? 's' : '' }}</span>
                </div>
              </div>
            </template>
            <template #footer>
              <Button
                :label="progress.status === 'NOT_STARTED' ? 'Start Exercise' : 'Continue'"
                :icon="progress.status === 'NOT_STARTED' ? 'pi pi-play' : 'pi pi-refresh'"
                @click.stop="goToExercise(progress.exercise.id)"
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import Select from 'primevue/select'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const router = useRouter()

interface Exercise {
  id: number
  type: string
  title: string
  languageCode: string
  moduleNumber: number
  topic: string | null
  difficultyRating: number
  estimatedDurationSeconds: number
  pointsValue: number
}

interface ExerciseProgress {
  exercise: Exercise
  bestScore: number | null
  attemptsCount: number
  lastAttemptAt: string | null
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'MASTERED'
}

interface ModuleProgress {
  moduleNumber: number
  languageCode: string
  totalExercises: number
  completedExercises: number
  masteredExercises: number
  averageScore: number | null
  totalTimeSpentSeconds: number
  completionPercentage: number
  exercises: ExerciseProgress[]
}

const selectedLanguage = ref('fr')
const selectedModule = ref(1)
const exerciseProgress = ref<ExerciseProgress[]>([])
const moduleStats = ref<ModuleProgress | null>(null)
const loading = ref(false)
const error = ref('')

const languages = [
  { label: 'French (Français)', value: 'fr' },
  { label: 'English', value: 'en' },
  { label: 'German (Deutsch)', value: 'de' }
]

const modules = [
  { label: 'Module 1', value: 1 },
  { label: 'Module 2', value: 2 },
  { label: 'Module 3', value: 3 },
  { label: 'Module 4', value: 4 },
  { label: 'Module 5', value: 5 }
]

const moduleTitle = computed(() => {
  const lang = languages.find(l => l.value === selectedLanguage.value)
  return `${lang?.label || 'Language'} - Module ${selectedModule.value}`
})

const uniqueTypes = computed(() => {
  return [...new Set(exerciseProgress.value.map(ep => ep.exercise.type))]
})

const totalDurationMinutes = computed(() => {
  const totalSeconds = exerciseProgress.value.reduce((sum, ep) => sum + ep.exercise.estimatedDurationSeconds, 0)
  return Math.ceil(totalSeconds / 60)
})

function getAuthHeaders(): HeadersInit {
  const token = localStorage.getItem('auth_token')
  const headers: HeadersInit = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

async function loadExercises() {
  loading.value = true
  error.value = ''

  try {
    const response = await fetch(
      `${API_BASE}/api/exercises/module-progress?languageCode=${selectedLanguage.value}&moduleNumber=${selectedModule.value}`,
      {
        headers: getAuthHeaders()
      }
    )

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const progress: ModuleProgress = await response.json()
    moduleStats.value = progress
    exerciseProgress.value = progress.exercises
  } catch (e: any) {
    console.error('Failed to load module progress:', e)
    error.value = e.message || 'Failed to load module progress'
  } finally {
    loading.value = false
  }
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
    cloze_reading: 'primary'
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
.module-view {
  padding: 2rem;
  min-height: 100vh;
  background: var(--surface-ground);
}

.container {
  max-width: 1400px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.header h1 {
  margin: 0;
  color: var(--text-color);
}

.header-actions {
  display: flex;
  gap: 1rem;
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  color: var(--text-color-secondary);
}

.empty-state p {
  margin: 1rem 0;
}

.empty-state .hint {
  font-size: 0.875rem;
}

.progress-summary-card {
  margin-bottom: 1.5rem;
}

.summary-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--primary-color);
  font-size: 1.25rem;
}

.progress-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.progress-stat {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.stat-icon-wrapper {
  width: 3rem;
  height: 3rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  flex-shrink: 0;
}

.stat-icon-wrapper.completion {
  background: var(--green-50);
  color: var(--green-600);
}

.stat-icon-wrapper.score {
  background: var(--yellow-50);
  color: var(--yellow-600);
}

.stat-icon-wrapper.time {
  background: var(--blue-50);
  color: var(--blue-600);
}

.stat-icon-wrapper.exercises {
  background: var(--purple-50);
  color: var(--purple-600);
}

.stat-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-value-large {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-color);
  line-height: 1;
}

.stat-sublabel {
  font-size: 0.75rem;
  color: var(--text-color-secondary);
}

.progress-bar-section {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--surface-border);
}

.progress-bar-labels {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  font-weight: 600;
}

.progress-percentage {
  color: var(--primary-color);
}

.progress-bar-track {
  height: 0.75rem;
  background: var(--surface-100);
  border-radius: 999px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-color), var(--primary-600));
  border-radius: 999px;
  transition: width 0.5s ease;
}

.module-stats {
  display: flex;
  gap: 2rem;
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: var(--surface-card);
  border-radius: 6px;
  border: 1px solid var(--surface-border);
  flex-wrap: wrap;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary-color);
}

.exercises-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.exercise-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  height: 100%;
}

.exercise-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.exercise-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: var(--surface-50);
  border-bottom: 1px solid var(--surface-border);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.exercise-points {
  font-weight: 600;
  color: var(--primary-color);
}

.status-icon {
  font-size: 1.25rem;
}

.status-icon.mastered {
  color: var(--green-500);
}

.status-icon.in-progress {
  color: var(--orange-500);
}

.exercise-card.mastered {
  border: 2px solid var(--green-200);
  background: linear-gradient(135deg, var(--surface-card) 0%, var(--green-50) 100%);
}

.exercise-title-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.best-score {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--green-600);
  background: var(--green-50);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  white-space: nowrap;
}

.exercise-meta {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.meta-item i {
  color: var(--primary-color);
}

@media (max-width: 768px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
  }

  .progress-stats-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .module-stats {
    flex-direction: column;
    gap: 1rem;
  }

  .exercises-grid {
    grid-template-columns: 1fr;
  }
}
</style>
