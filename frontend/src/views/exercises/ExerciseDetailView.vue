<template>
  <div class="page-container-with-padding">
    <div class="view-container content-area-lg">
      <div v-if="loading" class="loading-state">
        <ProgressSpinner/>
        <p>Loading exercise...</p>
      </div>

      <div v-else-if="exercise">
      <div class="exercise-header">
        <Button icon="pi pi-arrow-left" text @click="goBack" label="Back to Exercises"/>
        <h1>{{ exercise.title }}</h1>
        <p class="instructions">{{ exercise.instructions }}</p>
        <div class="meta">
          <Tag :value="exercise.type" severity="info" />
          <Tag
              :value="`Module ${exercise.moduleNumber}`"
              severity="secondary"
              v-if="exercise.moduleNumber"
          />
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

      <div v-else class="empty-state">
        <Message severity="error"> Exercise not found or failed to load.</Message>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import MultipleChoiceExercise from '@/components/exercises/MultipleChoiceExercise.vue'
import FillInBlankExercise from '@/components/exercises/FillInBlankExercise.vue'
import SentenceScrambleExercise from '@/components/exercises/SentenceScrambleExercise.vue'
import MatchingExercise from '@/components/exercises/MatchingExercise.vue'
import ListeningExercise from '@/components/exercises/ListeningExercise.vue'
import ClozeReadingExercise from '@/components/exercises/ClozeReadingExercise.vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import ProgressSpinner from 'primevue/progressspinner'
import Message from 'primevue/message'
import {useExerciseStore} from '@/stores/exercise'

const router = useRouter()
const route = useRoute()
const exerciseStore = useExerciseStore()
const {currentExercise: exercise, loading} = storeToRefs(exerciseStore)

const exerciseComponent = ref<InstanceType<
    | typeof MultipleChoiceExercise
    | typeof FillInBlankExercise
    | typeof SentenceScrambleExercise
    | typeof MatchingExercise
    | typeof ListeningExercise
    | typeof ClozeReadingExercise
> | null>(null)
const startTime = ref<number>(Date.now())
const lastUserResponse = ref<any>(null)

async function fetchExercise() {
  const id = Number(route.params.id)
  await exerciseStore.loadExercise(id)
  startTime.value = Date.now()
}

async function handleSubmit(response: any) {
  if (!exercise.value) return

  // Save the user's response before API call
  lastUserResponse.value = response

  const durationSeconds = Math.floor((Date.now() - startTime.value) / 1000)

  const result = await exerciseStore.submitAttempt(exercise.value.id, {
    userResponses: response,
    durationSeconds,
    hintsUsed: 0,
  })

  if (result && exerciseComponent.value) {
    exerciseComponent.value.setResult({
      isCorrect: result.isCorrect,
      feedback: result.feedback,
      userResponse: lastUserResponse.value,
    })
  }
}

function goBack() {
  router.push('/exercises')
}

onMounted(fetchExercise)
</script>

<style scoped>
/* Exercise header */
.exercise-header {
  margin-bottom: 2rem;
}

.exercise-header h1 {
  font-size: 2rem;
  margin: 1rem 0;
}

.instructions {
  font-size: 1.125rem;
  color: var(--text-secondary);
  margin-bottom: 1rem;
}

.meta {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* Placeholder for unimplemented exercise types */
.placeholder {
  padding: 1rem 0;
}
</style>
