<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Divider from 'primevue/divider'
import ProgressBar from 'primevue/progressbar'
import Tag from 'primevue/tag'
import MultipleChoiceExercise from '@/components/exercises/MultipleChoiceExercise.vue'
import FillInBlankExercise from '@/components/exercises/FillInBlankExercise.vue'
import SentenceScrambleExercise from '@/components/exercises/SentenceScrambleExercise.vue'
import MatchingExercise from '@/components/exercises/MatchingExercise.vue'
import ClozeReadingExercise from '@/components/exercises/ClozeReadingExercise.vue'
import ListeningExercise from '@/components/exercises/ListeningExercise.vue'
import {useCourseStore} from '@/stores/course'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()

// Destructure store state with refs
const {currentEpisode: episode, loading, error} = storeToRefs(courseStore)
const hasReadContent = ref(false)
const completedExercises = ref<number[]>([])
const currentExerciseIndex = ref(0)
const exerciseComponentRef = ref<any>(null)

const episodeTypeIcon = computed(() => {
  switch (episode.value?.type) {
    case 'STORY':
      return 'pi-book'
    case 'DIALOGUE':
      return 'pi-comments'
    case 'ARTICLE':
      return 'pi-file'
    case 'AUDIO_LESSON':
      return 'pi-volume-up'
    default:
      return 'pi-circle'
  }
})

const currentExercise = computed(() => {
  if (!episode.value || currentExerciseIndex.value >= episode.value.contentItems.length) {
    return null
  }
  return episode.value.contentItems[currentExerciseIndex.value]
})

const progress = computed(() => {
  if (!episode.value || episode.value.contentItems.length === 0) return 0
  const completed = completedExercises.value.length
  const total = episode.value.contentItems.length
  return Math.round((completed / total) * 100)
})

const isEpisodeCompleted = computed(() => {
  if (!episode.value) return false
  return (
      hasReadContent.value && completedExercises.value.length === episode.value.contentItems.length
  )
})

function getAuthHeaders() {
  const token = localStorage.getItem('auth_token')
  return {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  }
}

async function loadEpisode() {
  const episodeId = route.params.id
  if (episodeId) {
    await courseStore.loadEpisode(Number(episodeId))

    // Load progress if authenticated
    if (localStorage.getItem('auth_token')) {
      await loadProgress()
    }
  }
}

async function loadProgress() {
  const episodeId = route.params.id
  try {
    const response = await fetch(`${API_BASE}/api/episodes/${episodeId}/progress`, {
      headers: getAuthHeaders(),
    })

    if (response.ok) {
      const progressData = await response.json()
      hasReadContent.value = progressData.hasReadContent
      completedExercises.value = progressData.completedExercises || []

      // Set currentExerciseIndex to first incomplete exercise
      if (episode.value && episode.value.contentItems.length > 0) {
        const firstIncompleteIndex = episode.value.contentItems.findIndex((item) => {
          return item.exercise && !completedExercises.value.includes(item.exercise.id)
        })

        // If found an incomplete exercise, start there. Otherwise start at beginning
        currentExerciseIndex.value = firstIncompleteIndex >= 0 ? firstIncompleteIndex : 0
      }
    }
  } catch (err) {
    console.error('Error loading progress:', err)
  }
}

async function markContentAsRead() {
  if (hasReadContent.value) return

  const episodeId = route.params.id
  try {
    const response = await fetch(`${API_BASE}/api/episodes/${episodeId}/complete-content`, {
      method: 'POST',
      headers: getAuthHeaders(),
    })

    if (response.ok) {
      hasReadContent.value = true
    }
  } catch (err) {
    console.error('Error marking content as read:', err)
  }
}

async function handleExerciseSubmit(response: any) {
  if (!currentExercise.value?.exercise) return

  const exerciseId = currentExercise.value.exercise.id

  // Format request according to backend SubmitAttemptRequest DTO
  const attemptRequest = {
    userResponses: response,
    durationSeconds: null, // TODO: Track time spent on exercise
    hintsUsed: 0,
  }

  try {
    const validationResponse = await fetch(`${API_BASE}/api/exercises/${exerciseId}/attempt`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(attemptRequest),
    })

    if (!validationResponse.ok) {
      throw new Error('Failed to submit exercise attempt')
    }

    const result = await validationResponse.json()

    // Call setResult on the exercise component
    if (exerciseComponentRef.value && exerciseComponentRef.value.setResult) {
      exerciseComponentRef.value.setResult(result)
    }

    // If correct, mark as complete
    if (result.isCorrect && !completedExercises.value.includes(exerciseId)) {
      completedExercises.value.push(exerciseId)

      // Save progress to backend
      const episodeId = route.params.id
      try {
        await fetch(`${API_BASE}/api/episodes/${episodeId}/complete-exercise/${exerciseId}`, {
          method: 'POST',
          headers: getAuthHeaders(),
        })
      } catch (err) {
        console.error('Error saving exercise progress:', err)
      }

      // Note: Auto-advance is now handled by the exercise component with countdown
    }
  } catch (err) {
    console.error('Error submitting exercise:', err)
  }
}

function goToExercise(index: number) {
  currentExerciseIndex.value = index
}

function handleNext() {
  if (currentExerciseIndex.value < (episode.value?.contentItems.length || 0) - 1) {
    currentExerciseIndex.value++
  }
}

function goToNextEpisode() {
  // TODO: Navigate to next episode
  router.push(`/course-modules/${episode.value?.moduleId}`)
}

function goBackToModule() {
  router.push(`/course-modules/${episode.value?.moduleId}`)
}

onMounted(() => {
  loadEpisode()
})
</script>

<template>
  <div class="episode-view">
    <div class="episode-container">
      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner loading-spinner"></i>
        <p>Loading episode...</p>
      </div>

      <!-- Error State -->
      <Message v-else-if="error" severity="error" :closable="false">
        {{ error }}
      </Message>

      <!-- Episode Content -->
      <div v-else-if="episode" class="episode-content">
        <!-- Header -->
        <div class="episode-header">
          <Button
            icon="pi pi-arrow-left"
            text
            rounded
            @click="goBackToModule"
            class="back-button"
          />
          <div class="header-info">
            <div class="episode-meta">
              <Tag :value="episode.type" :icon="episodeTypeIcon" />
              <span class="episode-number">Episode {{ episode.episodeNumber }}</span>
            </div>
            <h1>{{ episode.title }}</h1>
            <div class="episode-stats">
              <span><i class="pi pi-clock"></i> {{ episode.estimatedMinutes }} min</span>
              <span><i class="pi pi-list"></i> {{ episode.contentItems.length }} exercises</span>
            </div>
          </div>
        </div>

        <!-- Progress -->
        <Card class="progress-card">
          <template #content>
            <div class="progress-section">
              <div class="progress-header">
                <span>Your Progress</span>
                <span class="progress-percentage">{{ Math.round(progress) }}%</span>
              </div>
              <ProgressBar :value="progress" />
              <div class="progress-details">
                <span v-if="!hasReadContent" class="pending-item">
                  <i class="pi pi-circle"></i> Read episode content
                </span>
                <span v-else class="completed-item">
                  <i class="pi pi-check-circle"></i> Content read
                </span>
                <span>
                  <i
                      :class="completedExercises.length > 0 ? 'pi pi-check-circle' : 'pi pi-circle'"
                  ></i>
                  {{ completedExercises.length }} / {{ episode.contentItems.length }} exercises
                  completed
                </span>
              </div>
            </div>
          </template>
        </Card>

        <!-- Episode Content -->
        <Card class="content-card">
          <template #title>
            <div class="content-title">
              <i :class="episodeTypeIcon"></i>
              <span>{{ episode.type === 'DIALOGUE' ? 'Dialogue' : 'Story' }}</span>
            </div>
          </template>
          <template #content>
            <div class="episode-text">
              <p v-for="(paragraph, index) in episode.content.split('\n\n')" :key="index">
                {{ paragraph }}
              </p>
            </div>

            <!-- Audio Player -->
            <div v-if="episode.audioUrl" class="audio-player">
              <Divider />
              <div class="audio-controls">
                <i class="pi pi-volume-up"></i>
                <audio controls :src="episode.audioUrl" class="audio-element">
                  Your browser does not support the audio element.
                </audio>
              </div>
            </div>

            <!-- Mark as Read Button -->
            <div v-if="!hasReadContent" class="content-actions">
              <Button
                label="I've read this"
                icon="pi pi-check"
                @click="markContentAsRead"
                outlined
              />
            </div>
          </template>
        </Card>

        <Divider />

        <!-- Exercises Section -->
        <div class="exercises-section">
          <h2>Practice Exercises</h2>

          <!-- Exercise Navigation -->
          <div class="exercise-nav">
            <Button
              v-for="(item, index) in episode.contentItems"
              :key="item.id"
              :label="`${index + 1}`"
              :class="{
                'exercise-nav-btn': true,
                active: currentExerciseIndex === index,
                completed: completedExercises.includes(item.exercise?.id || 0),
              }"
              :icon="completedExercises.includes(item.exercise?.id || 0) ? 'pi pi-check' : ''"
              @click="goToExercise(index)"
              rounded
            />
          </div>

          <!-- Current Exercise -->
          <Card v-if="currentExercise?.exercise" class="exercise-card">
            <template #title>
              <div class="exercise-header">
                <Tag :value="currentExercise.exercise.type" />
                <span class="exercise-title">{{ currentExercise.exercise.title }}</span>
                <span v-if="currentExercise.isRequired" class="required-badge">Required</span>
              </div>
            </template>
            <template #content>
              <p class="exercise-instructions">{{ currentExercise.exercise.instructions }}</p>

              <!-- Exercise Component -->
              <MultipleChoiceExercise
                v-if="currentExercise.exercise.type === 'multiple_choice'"
                :key="currentExercise.exercise.id"
                ref="exerciseComponentRef"
                :content="currentExercise.exercise.content"
                @submit="handleExerciseSubmit"
                @next="handleNext"
              />
              <FillInBlankExercise
                v-else-if="currentExercise.exercise.type === 'fill_in_blank'"
                :key="currentExercise.exercise.id"
                ref="exerciseComponentRef"
                :content="currentExercise.exercise.content"
                @submit="handleExerciseSubmit"
                @next="handleNext"
              />
              <SentenceScrambleExercise
                v-else-if="currentExercise.exercise.type === 'sentence_scramble'"
                :key="currentExercise.exercise.id"
                ref="exerciseComponentRef"
                :content="currentExercise.exercise.content"
                @submit="handleExerciseSubmit"
                @next="handleNext"
              />
              <MatchingExercise
                v-else-if="currentExercise.exercise.type === 'matching'"
                :key="currentExercise.exercise.id"
                ref="exerciseComponentRef"
                :content="currentExercise.exercise.content"
                @submit="handleExerciseSubmit"
                @next="handleNext"
              />
              <ClozeReadingExercise
                v-else-if="currentExercise.exercise.type === 'cloze_reading'"
                :key="currentExercise.exercise.id"
                ref="exerciseComponentRef"
                :content="currentExercise.exercise.content"
                @submit="handleExerciseSubmit"
                @next="handleNext"
              />
              <ListeningExercise
                v-else-if="currentExercise.exercise.type === 'listening'"
                :key="currentExercise.exercise.id"
                ref="exerciseComponentRef"
                :content="currentExercise.exercise.content"
                @submit="handleExerciseSubmit"
                @next="handleNext"
              />
              <Message v-else severity="warn">
                Exercise type "{{ currentExercise.exercise.type }}" not supported yet.
              </Message>
            </template>
          </Card>
        </div>

        <!-- Completion Message -->
        <Card v-if="isEpisodeCompleted" class="completion-card">
          <template #content>
            <div class="completion-message">
              <i class="pi pi-check-circle"></i>
              <h3>Episode Complete! 🎉</h3>
              <p>Great work! You've completed all exercises for this episode.</p>
              <Button
                label="Continue to Next Episode"
                icon="pi pi-arrow-right"
                iconPos="right"
                @click="goToNextEpisode"
                size="large"
              />
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.episode-view {
  min-height: 100vh;
  padding: 2rem 1rem;
}

.episode-container {
  max-width: 900px;
  margin: 0 auto;
}

.episode-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Header */
.episode-header {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.back-button {
  margin-top: 0.5rem;
}

.header-info {
  flex: 1;
}

.episode-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.episode-number {
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.episode-header h1 {
  font-size: 2rem;
  font-weight: 700;
  margin: 0 0 0.75rem 0;
}

.episode-stats {
  display: flex;
  gap: 1.5rem;
}

.episode-stats span {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* Progress */
.progress-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.progress-percentage {
  font-size: 1.25rem;
}

.progress-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.progress-details span {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.completed-item {
}

.completed-item i {
}

.pending-item {
}

/* Content */
.content-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.episode-text {
  line-height: 1.8;
  font-size: 1.125rem;
}

.episode-text p {
  margin-bottom: 1.5rem;
}

.audio-player {
  margin-top: 1rem;
}

.audio-controls {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.audio-controls i {
  font-size: 1.5rem;
}

.audio-element {
  flex: 1;
  width: 100%;
}

.content-actions {
  margin-top: 1.5rem;
  display: flex;
  justify-content: center;
}

/* Exercises */
.exercises-section {
  margin-top: 2rem;
}

.exercises-section h2 {
  margin-bottom: 1.5rem;
}

.exercise-nav {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.exercise-nav-btn {
  width: 3rem;
  height: 3rem;
}

.exercise-nav-btn.active {
}

.exercise-nav-btn.completed {
}

.exercise-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.exercise-title {
  flex: 1;
  font-size: 1.25rem;
}

.required-badge {
  padding: 0.25rem 0.75rem;
  border-radius: var(--radius-lg);
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.exercise-instructions {
  font-size: 1rem;
  margin-bottom: 1.5rem;
}

/* Completion */
.completion-card {
}

.completion-message {
  text-align: center;
  padding: 2rem;
}

.completion-message i {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.completion-message h3 {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 1rem 0;
}

.completion-message p {
  margin-bottom: 2rem;
}

/* Loading */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  min-height: 400px;
}

.loading-spinner {
  font-size: 3rem;
}

/* Responsive */
@media (max-width: 768px) {
  .episode-view {
    padding: 1rem;
  }

  .episode-header {
    flex-direction: column;
  }

  .episode-header h1 {
    font-size: 1.5rem;
  }

  .episode-text {
    font-size: 1rem;
  }

  .exercise-nav {
    justify-content: center;
  }
}
</style>
