<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import ProgressBar from 'primevue/progressbar'
import {useCourseStore} from '@/stores/course'
import type {ExerciseProgress} from '@/types/course'

const router = useRouter()
const courseStore = useCourseStore()

const selectedLanguage = ref('fr')
const selectedModule = ref(1)
const {moduleProgress: moduleStats, loading, error} = storeToRefs(courseStore)
const exerciseProgress = computed(() => moduleStats.value?.exercises || [])

const languages = [
  {label: 'French', value: 'fr'}, {label: 'English', value: 'en'}, {label: 'German', value: 'de'},
]
const modules = [
  {label: 'Module 1', value: 1}, {label: 'Module 2', value: 2}, {label: 'Module 3', value: 3},
  {label: 'Module 4', value: 4}, {label: 'Module 5', value: 5},
]

const moduleTitle = computed(() => `${languages.find(l => l.value === selectedLanguage.value)?.label || ''} - Module ${selectedModule.value}`)
const uniqueTypes = computed(() => [...new Set(exerciseProgress.value.map(ep => ep.exerciseType))])
const totalDurationMinutes = computed(() => Math.ceil(exerciseProgress.value.reduce((sum, ep) => sum + (ep.estimatedDurationSeconds || 0), 0) / 60))

const getExerciseStatus = (p: ExerciseProgress) => p.isMastered ? 'MASTERED' : p.isCompleted ? 'IN_PROGRESS' : 'NOT_STARTED'
const loadExercises = () => courseStore.loadModuleProgress(selectedLanguage.value, selectedModule.value)
const getTypeSeverity = (type: string) => ({
  multiple_choice: 'info',
  fill_in_blank: 'success',
  sentence_scramble: 'warn',
  matching: 'secondary',
  listening: 'contrast',
  cloze_reading: 'primary'
}[type] || 'info')
const formatTime = (seconds: number) => seconds < 60 ? `${seconds}s` : `${Math.floor(seconds / 60)}m`

onMounted(loadExercises)
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <h1>{{ moduleTitle }}</h1>
      <div class="flex gap-3">
        <Select v-model="selectedLanguage" :options="languages" optionLabel="label" optionValue="value"
                @change="loadExercises"/>
        <Select v-model="selectedModule" :options="modules" optionLabel="label" optionValue="value"
                @change="loadExercises"/>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner/>
    </div>
    <Message v-else-if="error" severity="error">{{ error }}</Message>
    <div v-else-if="exerciseProgress.length === 0" class="empty-state">
      <i class="pi pi-inbox empty-icon"></i>
      <h3>No exercises found</h3>
    </div>

    <div v-else class="content-area-lg">
      <Card v-if="moduleStats">
        <template #title>
          <div class="card-title-icon"><i class="pi pi-chart-line"></i><span>Your Progress</span></div>
        </template>
        <template #content>
          <div class="summary-stats mb-5">
            <div class="stat">
              <div class="stat-icon stat-icon-success"><i class="pi pi-check-circle"></i></div>
              <div><span class="stat-value">{{ Math.round(moduleStats.completionPercentage) }}%</span><span
                  class="stat-label">Completion</span></div>
            </div>
            <div class="stat">
              <div class="stat-icon stat-icon-yellow"><i class="pi pi-star"></i></div>
              <div><span class="stat-value">{{
                  moduleStats.averageScore !== null ? `${Math.round(moduleStats.averageScore)}%` : 'N/A'
                }}</span><span class="stat-label">Avg. Score</span></div>
            </div>
            <div class="stat">
              <div class="stat-icon stat-icon-blue"><i class="pi pi-clock"></i></div>
              <div><span class="stat-value">{{ formatTime(moduleStats.totalTimeSpentSeconds) }}</span><span
                  class="stat-label">Time Spent</span></div>
            </div>
            <div class="stat">
              <div class="stat-icon stat-icon-purple"><i class="pi pi-book"></i></div>
              <div><span class="stat-value">{{ moduleStats.totalExercises }}</span><span
                  class="stat-label">Exercises</span></div>
            </div>
          </div>
          <ProgressBar :value="moduleStats.completionPercentage" style="height: 1rem"/>
        </template>
      </Card>

      <div class="content-grid">
        <Card v-for="progress in exerciseProgress" :key="progress.exerciseId" class="card-interactive"
              :class="{ 'border-2 border-green-500': getExerciseStatus(progress) === 'MASTERED' }"
              @click="router.push(`/exercises/${progress.exerciseId}`)">
          <template #header>
            <div class="p-md flex justify-content-between align-items-center bg-surface-section">
              <Tag :value="progress.exerciseType" :severity="getTypeSeverity(progress.exerciseType)"/>
              <div class="flex align-items-center gap-2">
                <Tag v-if="progress.pointsValue" :value="`${progress.pointsValue} pts`" severity="contrast"/>
                <i v-if="getExerciseStatus(progress) === 'MASTERED'" class="pi pi-check-circle text-xl text-success"
                   v-tooltip.top="'Mastered'"></i>
                <i v-else-if="getExerciseStatus(progress) === 'IN_PROGRESS'" class="pi pi-clock text-xl text-warning"
                   v-tooltip.top="'In Progress'"></i>
              </div>
            </div>
          </template>
          <template #title>
            <div class="flex justify-content-between align-items-start">
              <span>{{ progress.exerciseTitle }}</span>
              <Tag v-if="progress.bestScore !== null" :value="`${Math.round(progress.bestScore)}%`" severity="success"
                   outlined/>
            </div>
          </template>
          <template #content>
            <div class="icon-label-group vertical compact">
              <span v-if="progress.estimatedDurationSeconds" class="icon-label"><i
                  class="pi pi-clock"></i>{{ formatTime(progress.estimatedDurationSeconds) }}</span>
              <span v-if="progress.topic" class="icon-label"><i class="pi pi-tag"></i>{{ progress.topic }}</span>
              <span v-if="progress.attemptCount > 0" class="icon-label"><i
                  class="pi pi-replay"></i>{{ progress.attemptCount }} attempts</span>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>
