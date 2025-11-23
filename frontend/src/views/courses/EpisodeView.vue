<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
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
import AudioPlayer from '@/components/audio/AudioPlayer.vue'
import {useCourseStore} from '@/stores/course'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()

const {currentEpisode: episode, loading, error} = storeToRefs(courseStore)
const hasReadContent = ref(false)
const completedExercises = ref<number[]>([])
const currentExerciseIndex = ref(0)
const exerciseComponentRef = ref<any>(null)

const episodeTypeIcon = computed(() => ({
  STORY: 'pi-book',
  DIALOGUE: 'pi-comments',
  ARTICLE: 'pi-file',
  AUDIO_LESSON: 'pi-volume-up'
}[episode.value?.type || ''] || 'pi-circle'))
const currentExercise = computed(() => episode.value?.contentItems[currentExerciseIndex.value])
const progress = computed(() => episode.value ? Math.round((completedExercises.value.length / episode.value.contentItems.length) * 100) : 0)
const isEpisodeCompleted = computed(() => episode.value && hasReadContent.value && completedExercises.value.length === episode.value.contentItems.length)

const dialogueData = computed(() => {
  if (!episode.value || episode.value.type !== 'DIALOGUE') return null
  try {
    const data = typeof episode.value.data === 'string'
        ? JSON.parse(episode.value.data)
        : episode.value.data
    return data?.dialogue
  } catch (e) {
    console.error('Failed to parse episode dialogue data:', e)
    return null
  }
})

const uniqueSpeakers = computed(() => {
  if (!dialogueData.value?.lines) return []
  const speakers = dialogueData.value.lines.map((line: any) => line.speaker)
  return [...new Set(speakers)]
})

function getSpeakerIndex(speaker: string): number {
  return uniqueSpeakers.value.indexOf(speaker)
}

const getAuthHeaders = () => ({
  Authorization: `Bearer ${localStorage.getItem('auth_token')}`,
  'Content-Type': 'application/json'
})

async function loadEpisode() {
  const episodeId = Number(route.params.id)
  if (episodeId) {
    await courseStore.loadEpisode(episodeId)
    if (localStorage.getItem('auth_token')) await loadProgress()
  }
}

async function loadProgress() {
  try {
    const response = await fetch(`${API_BASE}/api/episodes/${route.params.id}/progress`, {headers: getAuthHeaders()})
    if (response.ok) {
      const data = await response.json()
      hasReadContent.value = data.hasReadContent
      completedExercises.value = data.completedExercises || []
      if (episode.value) {
        const firstIncomplete = episode.value.contentItems.findIndex(item => item.exercise && !completedExercises.value.includes(item.exercise.id))
        currentExerciseIndex.value = firstIncomplete >= 0 ? firstIncomplete : 0
      }
    }
  } catch (err) {
    console.error('Error loading progress:', err)
  }
}

async function markContentAsRead() {
  if (hasReadContent.value) return
  try {
    const response = await fetch(`${API_BASE}/api/episodes/${route.params.id}/complete-content`, {
      method: 'POST',
      headers: getAuthHeaders()
    })
    if (response.ok) hasReadContent.value = true
  } catch (err) {
    console.error('Error marking content as read:', err)
  }
}

async function handleExerciseSubmit(response: any) {
  const exerciseId = currentExercise.value?.exercise?.id
  if (!exerciseId) return
  try {
    const validationResponse = await fetch(`${API_BASE}/api/exercises/${exerciseId}/attempt`, {
      method: 'POST', headers: getAuthHeaders(), body: JSON.stringify({userResponses: response}),
    })
    if (!validationResponse.ok) throw new Error('Failed to submit attempt')
    const result = await validationResponse.json()
    exerciseComponentRef.value?.setResult(result)
    if (result.isCorrect && !completedExercises.value.includes(exerciseId)) {
      completedExercises.value.push(exerciseId)
      await fetch(`${API_BASE}/api/episodes/${route.params.id}/complete-exercise/${exerciseId}`, {
        method: 'POST',
        headers: getAuthHeaders()
      })
    }
  } catch (err) {
    console.error('Error submitting exercise:', err)
  }
}

watch(currentExercise, () => exerciseComponentRef.value = null, {flush: 'pre'})
onMounted(loadEpisode)
</script>

<template>
  <div class="detail-container content-area-lg">
    <div v-if="loading" class="loading-state"><i class="pi pi-spin pi-spinner text-3xl"></i></div>
    <Message v-else-if="error" severity="error">{{ error }}</Message>
    <div v-else-if="episode" class="content-area-lg">
      <div class="detail-header">
        <Button icon="pi pi-arrow-left" text rounded @click="router.push(`/course-modules/${episode.moduleId}`)"
                class="detail-header-back-btn"/>
        <div class="detail-header-content">
          <div class="meta-badges">
            <Tag :value="episode.type" :icon="episodeTypeIcon"/>
            <span class="text-secondary">Episode {{ episode.episodeNumber }}</span></div>
          <h1>{{ episode.title }}</h1>
          <div class="icon-label-group"><span class="icon-label"><i class="pi pi-clock"></i>{{
              episode.estimatedMinutes
            }} min</span><span class="icon-label"><i class="pi pi-list"></i>{{ episode.contentItems.length }} exercises</span>
          </div>
        </div>
      </div>

      <Card>
        <template #content>
          <div class="content-area">
            <div class="flex justify-between items-center"><span class="font-semibold">Your Progress</span><span
                class="font-bold text-primary">{{ progress }}%</span></div>
            <ProgressBar :value="progress" style="height: 1rem"/>
            <div class="text-sm text-secondary flex flex-col gap-sm">
              <span class="flex items-center gap-sm"><i
                  :class="hasReadContent ? 'pi pi-check-circle text-success' : 'pi pi-circle'"></i> Content read</span>
              <span class="flex items-center gap-sm"><i
                  :class="completedExercises.length > 0 ? 'pi pi-check-circle text-success' : 'pi pi-circle'"></i> {{
                  completedExercises.length
                }} / {{ episode.contentItems.length }} exercises completed</span>
            </div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>
          <div class="card-title-icon"><i
              :class="episodeTypeIcon"></i><span>{{ episode.type === 'DIALOGUE' ? 'Dialogue' : 'Story' }}</span></div>
        </template>
        <template #content>
          <div class="content-area">
            <!-- Dialogue Display -->
            <div v-if="episode.type === 'DIALOGUE' && dialogueData?.lines" class="dialogue-container mb-lg">
              <div
                  v-for="(line, index) in dialogueData.lines"
                  :key="index"
                  class="dialogue-line"
                  :class="`speaker-${getSpeakerIndex(line.speaker)}`"
              >
                <div class="speaker-name">{{ line.speaker }}</div>
                <div class="dialogue-text">{{ line.text }}</div>
              </div>
            </div>

            <!-- Story or fallback display -->
            <div v-else class="prose" v-html="episode.content.replace(/\n\n/g, '<br/><br/>')"></div>

            <AudioPlayer v-if="episode.audioUrl" :audio-url="episode.audioUrl" class="mt-lg"/>
            <div v-if="!hasReadContent" class="text-center mt-lg">
              <Button label="I've read this" icon="pi pi-check" @click="markContentAsRead" outlined/>
            </div>
          </div>
        </template>
      </Card>

      <Divider/>

      <div class="content-area-lg">
        <h2 class="text-2xl font-bold">Practice Exercises</h2>
        <div class="flex gap-sm flex-wrap">
          <Button v-for="(item, index) in episode.contentItems" :key="item.id" :label="`${index + 1}`"
                  :severity="currentExerciseIndex === index ? 'primary' : 'secondary'"
                  :outlined="currentExerciseIndex !== index"
                  :icon="completedExercises.includes(item.exercise?.id || 0) ? 'pi pi-check' : ''"
                  @click="currentExerciseIndex = index" rounded/>
        </div>
        <Card v-if="currentExercise?.exercise">
          <template #title>
            <div class="exercise-header">
              <Tag :value="currentExercise.exercise.type"/>
              <span class="font-bold">{{ currentExercise.exercise.title }}</span></div>
          </template>
          <template #content>
            <p class="text-secondary mb-lg">{{ currentExercise.exercise.instructions }}</p>
            <component :is="{
              multiple_choice: MultipleChoiceExercise, fill_in_blank: FillInBlankExercise, sentence_scramble: SentenceScrambleExercise,
              matching: MatchingExercise, cloze_reading: ClozeReadingExercise, listening: ListeningExercise
            }[currentExercise.exercise.type]" :key="currentExercise.exercise.id" ref="exerciseComponentRef"
                       :content="currentExercise.exercise.content" @submit="handleExerciseSubmit"
                       @next="currentExerciseIndex < episode.contentItems.length - 1 && currentExerciseIndex++"/>
          </template>
        </Card>
      </div>

      <Card v-if="isEpisodeCompleted" class="bg-success-50 dark:bg-success-900 border-2 border-success">
        <template #content>
          <div class="text-center p-xl content-area">
            <i class="pi pi-check-circle text-4xl text-success"></i>
            <h3 class="text-2xl font-bold">Episode Complete! 🎉</h3>
            <p class="text-secondary">Great work! You've completed all exercises for this episode.</p>
            <Button label="Continue to Next Episode" icon="pi pi-arrow-right" iconPos="right"
                    @click="router.push(`/course-modules/${episode.moduleId}`)" size="large"/>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
.prose {
  line-height: 1.8;
  font-size: 1.1rem;
}

.exercise-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.dialogue-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.dialogue-line {
  padding: 1rem;
  border-radius: 0.5rem;
  border-left: 4px solid;
  transition: transform 0.2s;
}

.dialogue-line:hover {
  transform: translateX(4px);
}

/* Speaker 0 - Blue theme */
.dialogue-line.speaker-0 {
  background-color: rgba(59, 130, 246, 0.1);
  border-left-color: #3b82f6;
}

.dialogue-line.speaker-0 .speaker-name {
  color: #2563eb;
}

/* Speaker 1 - Purple theme */
.dialogue-line.speaker-1 {
  background-color: rgba(168, 85, 247, 0.1);
  border-left-color: #a855f7;
}

.dialogue-line.speaker-1 .speaker-name {
  color: #9333ea;
}

/* Speaker 2 - Green theme (if more than 2 speakers) */
.dialogue-line.speaker-2 {
  background-color: rgba(34, 197, 94, 0.1);
  border-left-color: #22c55e;
}

.dialogue-line.speaker-2 .speaker-name {
  color: #16a34a;
}

/* Speaker 3 - Orange theme (if more than 3 speakers) */
.dialogue-line.speaker-3 {
  background-color: rgba(249, 115, 22, 0.1);
  border-left-color: #f97316;
}

.dialogue-line.speaker-3 .speaker-name {
  color: #ea580c;
}

.speaker-name {
  font-size: 0.9rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 0.5rem;
}

.dialogue-text {
  font-size: 1.1rem;
  line-height: 1.7;
  color: var(--text-color);
}
</style>
