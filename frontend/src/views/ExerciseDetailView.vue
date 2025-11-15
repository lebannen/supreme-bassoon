<template>
  <div class="exercise-detail-container">
    <div v-if="loading" class="loading">
      <ProgressSpinner />
    </div>

    <div v-else-if="exercise" class="exercise-content">
      <div class="exercise-header">
        <Button
          icon="pi pi-arrow-left"
          text
          @click="goBack"
          label="Back to Exercises"
        />
        <h1>{{ exercise.title }}</h1>
        <p class="instructions">{{ exercise.instructions }}</p>
        <div class="meta">
          <Tag :value="exercise.type" severity="info" />
          <Tag :value="`Module ${exercise.moduleNumber}`" severity="secondary" v-if="exercise.moduleNumber" />
          <Tag :value="exercise.cefrLevel" severity="success" />
        </div>
      </div>

      <Card class="exercise-card">
        <template #content>
          <!-- Render exercise based on type -->
          <MultipleChoiceExercise
            v-if="exercise.type === 'multiple_choice'"
            :key="`mc-${exercise.id}`"
            ref="exerciseComponent"
            :content="exercise.content"
            @submit="handleSubmit"
          />

          <FillInBlankExercise
            v-else-if="exercise.type === 'fill_in_blank'"
            :key="`fib-${exercise.id}`"
            ref="exerciseComponent"
            :content="exercise.content"
            @submit="handleSubmit"
          />

          <SentenceScrambleExercise
            v-else-if="exercise.type === 'sentence_scramble'"
            :key="`ss-${exercise.id}`"
            ref="exerciseComponent"
            :content="exercise.content"
            @submit="handleSubmit"
          />

          <MatchingExercise
            v-else-if="exercise.type === 'matching'"
            :key="`m-${exercise.id}`"
            ref="exerciseComponent"
            :content="exercise.content"
            @submit="handleSubmit"
          />

          <ListeningExercise
            v-else-if="exercise.type === 'listening'"
            :key="`l-${exercise.id}`"
            ref="exerciseComponent"
            :content="exercise.content"
            @submit="handleSubmit"
          />

          <ClozeReadingExercise
            v-else-if="exercise.type === 'cloze_reading'"
            :key="`cr-${exercise.id}`"
            ref="exerciseComponent"
            :content="exercise.content"
            @submit="handleSubmit"
          />

          <!-- Placeholder for other types -->
          <div v-else class="placeholder">
            <Message severity="info">
              <p><strong>Exercise Type:</strong> {{ exercise.type }}</p>
              <p>This exercise type is not yet implemented.</p>
            </Message>
          </div>
        </template>
      </Card>
    </div>

    <div v-else class="error">
      <Message severity="error">
        Exercise not found or failed to load.
      </Message>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useExerciseApi } from '@/composables/useExerciseApi'
import type { Exercise } from '@/types/exercise'
import MultipleChoiceExercise from '@/components/MultipleChoiceExercise.vue'
import FillInBlankExercise from '@/components/FillInBlankExercise.vue'
import SentenceScrambleExercise from '@/components/SentenceScrambleExercise.vue'
import MatchingExercise from '@/components/MatchingExercise.vue'
import ListeningExercise from '@/components/ListeningExercise.vue'
import ClozeReadingExercise from '@/components/ClozeReadingExercise.vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import ProgressSpinner from 'primevue/progressspinner'
import Message from 'primevue/message'

const router = useRouter()
const route = useRoute()
const { getExercise, submitAttempt, loading } = useExerciseApi()

const exercise = ref<Exercise | null>(null)
const exerciseComponent = ref<InstanceType<typeof MultipleChoiceExercise | typeof FillInBlankExercise | typeof SentenceScrambleExercise | typeof MatchingExercise | typeof ListeningExercise | typeof ClozeReadingExercise> | null>(null)
const startTime = ref<number>(Date.now())
const lastUserResponse = ref<any>(null)

async function fetchExercise() {
  const id = Number(route.params.id)
  exercise.value = await getExercise(id)
  startTime.value = Date.now()
}

async function handleSubmit(response: any) {
  if (!exercise.value) return

  // Save the user's response before API call
  lastUserResponse.value = response

  const durationSeconds = Math.floor((Date.now() - startTime.value) / 1000)

  const result = await submitAttempt(exercise.value.id, {
    userResponses: response,
    durationSeconds,
    hintsUsed: 0
  })

  if (result && exerciseComponent.value) {
    exerciseComponent.value.setResult({
      isCorrect: result.isCorrect,
      feedback: result.feedback,
      userResponse: lastUserResponse.value
    })
  }
}

function goBack() {
  router.push('/exercises')
}

onMounted(fetchExercise)
</script>

<style scoped>
.exercise-detail-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 2rem;
}

.loading,
.error {
  display: flex;
  justify-content: center;
  padding: 4rem;
}

.exercise-header {
  margin-bottom: 2rem;
}

.exercise-header h1 {
  font-size: 2rem;
  margin: 1rem 0;
}

.instructions {
  font-size: 1.125rem;
  color: var(--text-color-secondary);
  margin-bottom: 1rem;
}

.meta {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.placeholder {
  padding: 1rem 0;
}

.exercise-data {
  margin-top: 2rem;
  padding: 1rem;
  background: var(--surface-ground);
  border-radius: 8px;
}

.exercise-data h3 {
  margin-top: 0;
  margin-bottom: 1rem;
}

.exercise-data pre {
  background: var(--surface-card);
  padding: 1rem;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 0.875rem;
}
</style>
