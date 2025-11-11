<template>
  <div class="exercises-container">
    <div class="header">
      <h1>Exercises</h1>
      <p>Choose an exercise type to get started</p>
    </div>

    <!-- Exercise Type Grid -->
    <div class="type-grid">
      <Card
        v-for="type in exerciseTypes"
        :key="type.value"
        class="type-card"
        :class="{ 'selected': selectedType === type.value }"
        @click="selectType(type.value)"
      >
        <template #content>
          <div class="type-card-content">
            <i :class="type.icon" class="type-icon"></i>
            <h3 class="type-name">{{ type.label }}</h3>
            <p class="type-count">{{ getExerciseCount(type.value) }} exercises</p>
          </div>
        </template>
      </Card>
    </div>

    <!-- Module Filter (secondary) -->
    <div v-if="selectedType !== null" class="secondary-filters">
      <Select
        v-model="selectedModule"
        :options="modules"
        optionLabel="label"
        optionValue="value"
        placeholder="All Modules"
        class="module-select"
      />
    </div>

    <!-- Exercises List -->
    <div v-if="selectedType !== null" class="exercises-section">
      <h2 class="section-title">
        {{ exerciseTypes.find(t => t.value === selectedType)?.label || 'All' }} Exercises
      </h2>

      <div v-if="loading" class="loading">
        <ProgressSpinner />
      </div>

      <div v-else-if="exercises.length === 0" class="no-exercises">
        <i class="pi pi-inbox" style="font-size: 3rem; color: var(--text-color-secondary)"></i>
        <p>No exercises available for this type yet.</p>
      </div>

      <div v-else class="exercises-grid">
        <Card
          v-for="exercise in exercises"
          :key="exercise.id"
          class="exercise-card"
          @click="goToExercise(exercise.id)"
        >
          <template #title>{{ exercise.title }}</template>
          <template #subtitle>{{ formatExerciseType(exercise.type) }}</template>
          <template #content>
            <div class="meta">
              <Tag :value="exercise.moduleNumber ? `Module ${exercise.moduleNumber}` : 'General'" />
              <span><i class="pi pi-clock"></i> {{ exercise.estimatedDurationSeconds }}s</span>
              <span><i class="pi pi-star"></i> {{ exercise.pointsValue }} pts</span>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useExerciseApi } from '@/composables/useExerciseApi'
import Card from 'primevue/card'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import ProgressSpinner from 'primevue/progressspinner'

const router = useRouter()
const { getExercises, loading } = useExerciseApi()

const exercises = ref([])
const allExercises = ref([])
const selectedModule = ref(null)
const selectedType = ref<string | null>(null)

const modules = [
  { label: 'All Modules', value: null },
  ...Array.from({ length: 10 }, (_, i) => ({ label: `Module ${i + 1}`, value: i + 1 }))
]

const exerciseTypes = [
  {
    label: 'Multiple Choice',
    value: 'multiple_choice',
    icon: 'pi pi-list'
  },
  {
    label: 'Fill in the Blank',
    value: 'fill_in_blank',
    icon: 'pi pi-pencil'
  },
  {
    label: 'Sentence Scramble',
    value: 'sentence_scramble',
    icon: 'pi pi-sort-alt'
  },
  {
    label: 'Matching',
    value: 'matching',
    icon: 'pi pi-link'
  },
  {
    label: 'Listening',
    value: 'listening',
    icon: 'pi pi-volume-up'
  },
  {
    label: 'Cloze Reading',
    value: 'cloze_reading',
    icon: 'pi pi-file-edit'
  }
]

async function fetchAllExercises() {
  allExercises.value = await getExercises('fr', {})
}

async function fetchExercises() {
  if (selectedType.value === null) {
    exercises.value = []
    return
  }

  exercises.value = await getExercises('fr', {
    module: selectedModule.value,
    type: selectedType.value
  })
}

function selectType(typeValue: string | null) {
  selectedType.value = typeValue
  selectedModule.value = null
  fetchExercises()
}

function getExerciseCount(typeValue: string | null): number {
  if (typeValue === null) {
    return allExercises.value.length
  }
  return allExercises.value.filter(ex => ex.type === typeValue).length
}

function formatExerciseType(type: string): string {
  const typeObj = exerciseTypes.find(t => t.value === type)
  return typeObj?.label || type
}

function goToExercise(id: number) {
  router.push(`/exercises/${id}`)
}

watch(selectedModule, fetchExercises)

onMounted(async () => {
  await fetchAllExercises()
})
</script>

<style scoped>
.exercises-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

.header {
  margin-bottom: 3rem;
  text-align: center;
}

.header h1 {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
  color: var(--primary-color);
}

.header p {
  color: var(--text-color-secondary);
  font-size: 1.125rem;
}

/* Exercise Type Grid */
.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
  margin-bottom: 3rem;
}

.type-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border: 3px solid transparent;
}

.type-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
  border-color: var(--primary-color);
}

.type-card.selected {
  border-color: var(--primary-color);
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-color-dark) 100%);
  box-shadow: 0 6px 20px rgba(var(--primary-color-rgb), 0.4);
}

.type-card.selected .type-card-content {
  color: white;
}

.type-card.selected .type-icon {
  color: white;
  background: rgba(255, 255, 255, 0.2);
}

.type-card-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 1rem;
  gap: 0.75rem;
}

.type-icon {
  font-size: 2.5rem;
  color: var(--primary-color);
  background: var(--primary-color-light);
  padding: 1rem;
  border-radius: 50%;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.type-name {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0;
  color: var(--text-color);
}

.type-card.selected .type-name {
  color: white;
}

.type-count {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  margin: 0;
}

.type-card.selected .type-count {
  color: rgba(255, 255, 255, 0.9);
}

/* Secondary Filters */
.secondary-filters {
  display: flex;
  justify-content: center;
  margin-bottom: 2rem;
}

.module-select {
  min-width: 250px;
}

/* Exercises Section */
.exercises-section {
  margin-top: 2rem;
}

.section-title {
  font-size: 1.75rem;
  margin-bottom: 1.5rem;
  color: var(--text-color);
  padding-bottom: 0.75rem;
  border-bottom: 2px solid var(--surface-border);
}

.loading {
  display: flex;
  justify-content: center;
  padding: 4rem;
}

.no-exercises {
  text-align: center;
  padding: 4rem 2rem;
  color: var(--text-color-secondary);
}

.no-exercises p {
  margin-top: 1rem;
  font-size: 1.125rem;
}

.exercises-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.exercise-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.exercise-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.meta {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.meta span {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.meta i {
  font-size: 0.75rem;
}

/* Responsive */
@media (max-width: 768px) {
  .type-grid {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 1rem;
  }

  .type-icon {
    width: 60px;
    height: 60px;
    font-size: 2rem;
  }

  .type-name {
    font-size: 1rem;
  }

  .exercises-grid {
    grid-template-columns: 1fr;
  }
}
</style>
