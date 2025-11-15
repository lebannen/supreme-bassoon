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

      <div v-else-if="exercises.length === 0" class="empty-state">
        <i class="pi pi-inbox" style="font-size: 3rem; color: var(--text-color-secondary)"></i>
        <p>No exercises found for this module.</p>
        <p class="hint">Try importing exercises from the admin panel.</p>
      </div>

      <div v-else class="exercises-container">
        <div class="module-stats">
          <div class="stat">
            <span class="stat-label">Total Exercises:</span>
            <span class="stat-value">{{ exercises.length }}</span>
          </div>
          <div class="stat">
            <span class="stat-label">Exercise Types:</span>
            <span class="stat-value">{{ uniqueTypes.length }}</span>
          </div>
          <div class="stat">
            <span class="stat-label">Estimated Time:</span>
            <span class="stat-value">{{ totalDurationMinutes }} min</span>
          </div>
        </div>

        <div class="exercises-grid">
          <Card
            v-for="exercise in exercises"
            :key="exercise.id"
            class="exercise-card"
            @click="goToExercise(exercise.id)"
          >
            <template #header>
              <div class="exercise-card-header">
                <Tag :value="exercise.type" :severity="getTypeSeverity(exercise.type)" />
                <span class="exercise-points">{{ exercise.pointsValue }} pts</span>
              </div>
            </template>
            <template #title>
              {{ exercise.title }}
            </template>
            <template #content>
              <div class="exercise-meta">
                <div class="meta-item">
                  <i class="pi pi-clock"></i>
                  <span>{{ formatDuration(exercise.estimatedDurationSeconds) }}</span>
                </div>
                <div class="meta-item">
                  <i class="pi pi-chart-bar"></i>
                  <span>Difficulty: {{ exercise.difficultyRating.toFixed(1) }}</span>
                </div>
                <div v-if="exercise.topic" class="meta-item">
                  <i class="pi pi-tag"></i>
                  <span>{{ exercise.topic }}</span>
                </div>
              </div>
            </template>
            <template #footer>
              <Button
                label="Start Exercise"
                icon="pi pi-play"
                @click.stop="goToExercise(exercise.id)"
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

const selectedLanguage = ref('fr')
const selectedModule = ref(1)
const exercises = ref<Exercise[]>([])
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
  return [...new Set(exercises.value.map(e => e.type))]
})

const totalDurationMinutes = computed(() => {
  const totalSeconds = exercises.value.reduce((sum, e) => sum + e.estimatedDurationSeconds, 0)
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
      `${API_BASE}/api/exercises?languageCode=${selectedLanguage.value}&module=${selectedModule.value}`,
      {
        headers: getAuthHeaders()
      }
    )

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    exercises.value = await response.json()
  } catch (e: any) {
    console.error('Failed to load exercises:', e)
    error.value = e.message || 'Failed to load exercises'
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

.exercise-points {
  font-weight: 600;
  color: var(--primary-color);
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

  .module-stats {
    flex-direction: column;
    gap: 1rem;
  }

  .exercises-grid {
    grid-template-columns: 1fr;
  }
}
</style>
