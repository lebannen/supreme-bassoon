<template>
  <div class="view-container p-2xl">
    <div class="page-header text-center">
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
    <div v-if="selectedType !== null" class="flex justify-center mb-2xl">
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
    <div v-if="selectedType !== null" class="section">
      <div class="section-header">
        <h2>
          {{ exerciseTypes.find(t => t.value === selectedType)?.label || 'All' }} Exercises
        </h2>
      </div>

      <div v-if="loading" class="loading-state">
        <ProgressSpinner />
      </div>

      <div v-else-if="exercises.length === 0" class="empty-state">
        <i class="pi pi-inbox empty-icon text-secondary"></i>
        <p>No exercises available for this type yet.</p>
      </div>

      <div v-else class="content-grid">
        <Card
          v-for="exercise in exercises"
          :key="exercise.id"
          class="exercise-card"
          @click="goToExercise(exercise.id)"
        >
          <template #title>{{ exercise.title }}</template>
          <template #subtitle>{{ formatExerciseType(exercise.type) }}</template>
          <template #content>
            <div class="meta-badges">
              <Tag :value="exercise.moduleNumber ? `Module ${exercise.moduleNumber}` : 'General'" />
              <span class="flex items-center gap-xs text-sm text-secondary">
                <i class="pi pi-clock text-xs"></i>
                {{ exercise.estimatedDurationSeconds }}s
              </span>
              <span class="flex items-center gap-xs text-sm text-secondary">
                <i class="pi pi-star text-xs"></i>
                {{ exercise.pointsValue }} pts
              </span>
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
/* Exercise Type Grid */
.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-3xl);
}

.type-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border: 3px solid transparent;
}

.type-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
  border-color: var(--primary);
}

.type-card.selected {
  border-color: var(--primary);
  background: linear-gradient(135deg, var(--primary-gradient-start) 0%, var(--primary-gradient-end) 100%);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

.type-card.selected .type-card-content {
  color: var(--text-inverse);
}

.type-card.selected .type-icon {
  color: var(--text-inverse);
  background: rgba(255, 255, 255, 0.2);
}

.type-card-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: var(--spacing-md);
  gap: var(--spacing-sm);
}

.type-icon {
  font-size: 2.5rem;
  color: var(--primary);
  background: var(--primary-light);
  padding: var(--spacing-md);
  border-radius: var(--radius-full);
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
  color: var(--text-primary);
}

.type-card.selected .type-name {
  color: var(--text-inverse);
}

.type-count {
  color: var(--text-secondary);
  margin: 0;
}

.type-card.selected .type-count {
  color: rgba(255, 255, 255, 0.9);
}

.module-select {
  min-width: 250px;
}

.exercise-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.exercise-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

/* Responsive */
@media (max-width: 768px) {
  .type-grid {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: var(--spacing-md);
  }

  .type-icon {
    width: 60px;
    height: 60px;
    font-size: 2rem;
  }

  .type-name {
    font-size: 1rem;
  }
}
</style>
