<script setup lang="ts">
import {onMounted, ref, watch} from 'vue'
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

const exerciseComponent = ref<any>(null)
const startTime = ref(Date.now())

const fetchExercise = async () => {
  await exerciseStore.loadExercise(Number(route.params.id))
  startTime.value = Date.now()
}

const handleSubmit = async (response: any) => {
  if (!exercise.value) return
  const durationSeconds = Math.floor((Date.now() - startTime.value) / 1000)
  const result = await exerciseStore.submitAttempt(exercise.value.id, {
    userResponses: response,
    durationSeconds,
    hintsUsed: 0,
  })
  if (result && exerciseComponent.value) {
    exerciseComponent.value.setResult({...result, userResponse: response})
  }
}

const handleNext = () => {
  // This would typically go to the next exercise in a sequence
  router.push('/exercises')
}

watch(exercise, () => exerciseComponent.value = null, {flush: 'pre'})
onMounted(fetchExercise)
</script>

<template>
  <div class="view-container content-area-lg">
    <div v-if="loading" class="loading-state">
      <ProgressSpinner/>
    </div>
    <div v-else-if="exercise">
      <div class="page-header">
        <Button icon="pi pi-arrow-left" text @click="router.push('/exercises')" label="Back to Exercises"/>
        <h1 class="mt-md">{{ exercise.title }}</h1>
        <p class="text-secondary">{{ exercise.instructions }}</p>
        <div class="meta-badges">
          <Tag :value="exercise.type"/>
          <Tag v-if="exercise.moduleNumber" :value="`Module ${exercise.moduleNumber}`" severity="secondary"/>
          <Tag v-if="exercise.cefrLevel" :value="exercise.cefrLevel" severity="success"/>
        </div>
      </div>
      <Card>
        <template #content>
          <component :is="{
              multiple_choice: MultipleChoiceExercise, fill_in_blank: FillInBlankExercise, sentence_scramble: SentenceScrambleExercise,
              matching: MatchingExercise, listening: ListeningExercise, cloze_reading: ClozeReadingExercise
            }[exercise.type]" :key="exercise.id" ref="exerciseComponent" :content="exercise.content"
                     @submit="handleSubmit" @next="handleNext"/>
        </template>
      </Card>
    </div>
    <Message v-else severity="error">Exercise not found or failed to load.</Message>
  </div>
</template>
