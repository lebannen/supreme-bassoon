<script setup lang="ts">
import {ref, onMounted, computed} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import Tag from 'primevue/tag'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'

const route = useRoute()
const router = useRouter()
const episodeId = Number(route.params.episodeId)
const courseId = Number(route.params.courseId)

const episode = ref<any>(null)
const loading = ref(true)
const error = ref('')

const dialogueData = computed(() => {
  if (!episode.value || episode.value.type !== 'DIALOGUE') return null

  try {
    const data = typeof episode.value.data === 'string'
        ? JSON.parse(episode.value.data)
        : episode.value.data

    return data?.dialogue
  } catch (e) {
    console.error('Failed to parse episode data:', e)
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

onMounted(async () => {
  await loadEpisode()
})

async function loadEpisode() {
  loading.value = true
  try {
    const response = await fetch(`/api/episodes/${episodeId}`)
    if (!response.ok) throw new Error('Failed to load episode')
    episode.value = await response.json()

    console.log('=== EPISODE LOADED ===')
    console.log('Episode ID:', episode.value.id)
    console.log('Episode Type:', episode.value.type)
    console.log('Has data field?', 'data' in episode.value)
    console.log('Data value:', episode.value.data)
  } catch (e: any) {
    error.value = e.message || 'Failed to load episode'
  } finally {
    loading.value = false
  }
}

function goBack() {
  if (courseId) {
    router.push({name: 'course-dashboard', params: {id: courseId}})
  } else {
    router.back()
  }
}
</script>

<template>
  <div class="episode-viewer p-xl max-w-6xl mx-auto">
    <div class="mb-xl">
      <div class="flex items-center justify-between mb-md">
        <h1 class="text-3xl font-bold">Episode Content</h1>
        <Button label="Back to Dashboard" icon="pi pi-arrow-left" text @click="goBack"/>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center p-xl">
      <ProgressSpinner/>
    </div>

    <div v-else-if="error">
      <Message severity="error">{{ error }}</Message>
      <Button label="Retry" icon="pi pi-refresh" @click="loadEpisode" class="mt-md"/>
    </div>

    <div v-else-if="episode">
      <!-- Episode Header -->
      <Card class="mb-lg">
        <template #title>
          <div class="flex items-center justify-between">
            <div>Episode {{ episode.episodeNumber }}: {{ episode.title }}</div>
            <Tag :value="episode.type" severity="info"/>
          </div>
        </template>
        <template #content>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-lg">
            <div>
              <label class="block font-bold text-sm mb-xs text-secondary">Type</label>
              <p class="m-0">{{ episode.type }}</p>
            </div>
            <div>
              <label class="block font-bold text-sm mb-xs text-secondary">Status</label>
              <Tag :value="episode.status || 'DRAFT'"/>
            </div>
            <div class="col-span-2" v-if="episode.summary">
              <label class="block font-bold text-sm mb-xs text-secondary">Summary</label>
              <p class="m-0 text-sm">{{ episode.summary }}</p>
            </div>
            <div class="col-span-2" v-if="episode.audioUrl">
              <label class="block font-bold text-sm mb-xs text-secondary">Audio</label>
              <audio controls class="w-full">
                <source :src="episode.audioUrl" type="audio/mpeg"/>
                Your browser does not support the audio element.
              </audio>
            </div>
          </div>
        </template>
      </Card>

      <!-- Content -->
      <Card class="mb-lg">
        <template #title>{{ episode.type === 'DIALOGUE' ? 'Dialogue' : 'Story' }}</template>
        <template #content>
          <div class="content-area">
            <!-- Dialogue Display -->
            <div v-if="episode.type === 'DIALOGUE' && dialogueData?.lines" class="dialogue-container">
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

            <!-- Fallback to plain text if dialogue parsing fails -->
            <pre v-else class="whitespace-pre-wrap font-sans">{{ episode.content }}</pre>
          </div>
        </template>
      </Card>

      <!-- Exercises -->
      <Card v-if="episode.contentItems && episode.contentItems.length > 0">
        <template #title>Exercises ({{ episode.contentItems.length }})</template>
        <template #content>
          <Accordion :multiple="true">
            <AccordionTab
                v-for="(item, index) in episode.contentItems"
                :key="item.id"
                :header="`Exercise ${index + 1}: ${item.exercise?.type || 'Unknown'}`"
            >
              <div class="space-y-md">
                <div>
                  <label class="block font-bold text-sm mb-xs text-secondary">Type</label>
                  <Tag :value="item.exercise?.type"/>
                </div>
                <div>
                  <label class="block font-bold text-sm mb-xs text-secondary">Instructions</label>
                  <p class="m-0">{{ item.exercise?.instructions || 'No instructions' }}</p>
                </div>
                <div>
                  <label class="block font-bold text-sm mb-xs text-secondary">Content</label>
                  <pre class="bg-surface-ground p-md rounded text-sm whitespace-pre-wrap">{{
                      JSON.stringify(item.exercise?.content, null, 2)
                    }}</pre>
                </div>
              </div>
            </AccordionTab>
          </Accordion>
        </template>
      </Card>

      <div v-else>
        <Message severity="info">No exercises found for this episode.</Message>
      </div>
    </div>
  </div>
</template>

<style scoped>
.space-y-md > * + * {
  margin-top: 1rem;
}

.content-area {
  max-height: 600px;
  overflow-y: auto;
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
  font-size: 0.95rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 0.5rem;
}

.dialogue-text {
  font-size: 1.05rem;
  line-height: 1.6;
  color: var(--text-color);
}
</style>
