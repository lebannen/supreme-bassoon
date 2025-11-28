<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import ProgressSpinner from 'primevue/progressspinner'
import {useExerciseStore} from '@/stores/exercise'
import type {ExerciseSummary} from '@/types/exercise'

const router = useRouter()
const exerciseStore = useExerciseStore()
const {loading} = storeToRefs(exerciseStore)

const allExercises = ref<ExerciseSummary[]>([])
const selectedModule = ref<number | null>(null)
const selectedType = ref<string | null>(null)

const exercises = computed(() => {
  if (!selectedType.value) return []
  return allExercises.value.filter(ex =>
      ex.type === selectedType.value && (selectedModule.value === null || ex.moduleNumber === selectedModule.value)
  )
})

const modules = [{label: 'All Modules', value: null}, ...Array.from({length: 10}, (_, i) => ({
  label: `Module ${i + 1}`,
  value: i + 1
}))]
const exerciseTypes = [
  {label: 'Multiple Choice', value: 'multiple_choice', icon: 'pi pi-list'},
  {label: 'Fill in the Blank', value: 'fill_in_blank', icon: 'pi pi-pencil'},
  {label: 'Sentence Scramble', value: 'sentence_scramble', icon: 'pi pi-sort-alt'},
  {label: 'Matching', value: 'matching', icon: 'pi pi-link'},
  {label: 'Listening', value: 'listening', icon: 'pi pi-volume-up'},
  {label: 'Cloze Reading', value: 'cloze_reading', icon: 'pi pi-file-edit'},
]

const fetchAllExercises = async () => {
  await exerciseStore.loadExercises('fr', {})
  allExercises.value = exerciseStore.exercises
}

const getExerciseCount = (type: string | null) => type ? allExercises.value.filter(ex => ex.type === type).length : allExercises.value.length

onMounted(fetchAllExercises)
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header text-center">
      <h1>Exercises</h1>
      <p class="text-secondary">Choose an exercise type to get started.</p>
    </div>

    <div class="type-grid">
      <Card v-for="type in exerciseTypes" :key="type.value" class="type-card"
            :class="{ 'selected': selectedType === type.value }"
            @click="selectedType = type.value; selectedModule = null">
        <template #content>
          <div class="type-card-content">
            <i :class="type.icon" class="type-icon"></i>
            <h3 class="type-name">{{ type.label }}</h3>
            <p class="type-count">{{ getExerciseCount(type.value) }} exercises</p>
          </div>
        </template>
      </Card>
    </div>

    <div v-if="selectedType" class="flex justify-content-center mb-6">
      <Select v-model="selectedModule" :options="modules" optionLabel="label" optionValue="value"
              placeholder="Filter by Module" class="w-full max-w-xs"/>
    </div>

    <div v-if="selectedType" class="section">
      <div class="section-header">
        <h2>{{ exerciseTypes.find(t => t.value === selectedType)?.label }} Exercises</h2>
      </div>
      <div v-if="loading" class="loading-state">
        <ProgressSpinner/>
      </div>
      <div v-else-if="exercises.length === 0" class="empty-state">
        <i class="pi pi-inbox empty-icon"></i>
        <p>No exercises available for this type yet.</p>
      </div>
      <div v-else class="content-grid">
        <Card v-for="exercise in exercises" :key="exercise.id" class="card-interactive"
              @click="router.push(`/exercises/${exercise.id}`)">
          <template #title>{{ exercise.title }}</template>
          <template #content>
            <div class="meta-badges">
              <Tag :value="exercise.moduleNumber ? `Module ${exercise.moduleNumber}` : 'General'"/>
              <span class="icon-label"><i class="pi pi-clock"></i>{{ exercise.estimatedDurationSeconds }}s</span>
              <span class="icon-label"><i class="pi pi-star"></i>{{ exercise.pointsValue }} pts</span>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}
.type-card {
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}
.type-card:hover {
  transform: translateY(-4px);
  border-color: var(--primary-color);
}
.type-card.selected {
  border-color: var(--primary-color);
  background-color: var(--surface-hover);
}

.dark-theme .type-card.selected {
  background-color: var(--p-primary-900);
}
.type-card-content {
  text-align: center;
  padding: 1rem;
}
.type-icon {
  font-size: 2.5rem;
  margin-bottom: 1rem;
  color: var(--primary-color);
}
.type-name {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0;
}
.type-count {
  margin: 0.25rem 0 0;
  color: var(--text-color-secondary);
}
</style>
