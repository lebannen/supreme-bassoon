<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const route = useRoute()
const router = useRouter()

interface EpisodeSummary {
  id: number
  episodeNumber: number
  type: string
  title: string
  estimatedMinutes: number
  hasAudio: boolean
  totalExercises: number
}

interface Module {
  id: number
  courseId: number
  moduleNumber: number
  title: string
  theme: string | null
  description: string | null
  objectives: string[]
  estimatedMinutes: number
  episodes: EpisodeSummary[]
}

const module = ref<Module | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

const episodeTypeIcon = (type: string) => {
  switch (type) {
    case 'STORY': return 'pi-book'
    case 'DIALOGUE': return 'pi-comments'
    case 'ARTICLE': return 'pi-file'
    case 'AUDIO_LESSON': return 'pi-volume-up'
    default: return 'pi-circle'
  }
}

async function loadModule() {
  const moduleId = route.params.id
  if (!moduleId) {
    error.value = 'No module ID provided'
    loading.value = false
    return
  }

  try {
    loading.value = true
    error.value = null

    const response = await fetch(`${API_BASE}/api/modules/${moduleId}`)

    if (!response.ok) {
      throw new Error('Failed to load module')
    }

    module.value = await response.json()
  } catch (err) {
    console.error('Error loading module:', err)
    error.value = err instanceof Error ? err.message : 'Failed to load module'
  } finally {
    loading.value = false
  }
}

function goToEpisode(episodeId: number) {
  router.push(`/episodes/${episodeId}`)
}

function goBack() {
  if (module.value) {
    router.push(`/courses/${module.value.courseId}`)
  } else {
    router.push('/courses')
  }
}

onMounted(() => {
  loadModule()
})
</script>

<template>
  <div class="module-detail-view">
    <div class="detail-container">
      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner loading-spinner"></i>
        <p>Loading module...</p>
      </div>

      <!-- Error State -->
      <Message v-else-if="error" severity="error" :closable="false">
        {{ error }}
      </Message>

      <!-- Module Content -->
      <div v-else-if="module" class="content-area-lg">
        <!-- Header -->
        <div class="detail-header">
          <Button
            icon="pi pi-arrow-left"
            text
            rounded
            @click="goBack"
            class="detail-header-back-btn"
          />
          <div class="detail-header-content">
            <div class="meta-badges">
              <span class="badge badge-info">Module {{ module.moduleNumber }}</span>
              <Tag v-if="module.theme" :value="module.theme" />
            </div>
            <h1>{{ module.title }}</h1>
            <p v-if="module.description" class="detail-description">
              {{ module.description }}
            </p>
            <div class="icon-label-group">
              <span><i class="pi pi-list"></i> {{ module.episodes.length }} episodes</span>
              <span><i class="pi pi-clock"></i> ~{{ module.estimatedMinutes }} min</span>
            </div>
          </div>
        </div>

        <!-- Learning Objectives -->
        <Card v-if="module.objectives && module.objectives.length > 0">
          <template #title>
            <div class="card-title-icon">
              <i class="pi pi-target"></i>
              <span>Module Objectives</span>
            </div>
          </template>
          <template #content>
            <ul class="objectives-grid">
              <li v-for="(objective, index) in module.objectives" :key="index">
                <i class="pi pi-check"></i>
                <span>{{ objective }}</span>
              </li>
            </ul>
          </template>
        </Card>

        <!-- Episodes -->
        <div class="section">
          <div class="section-header">
            <h2>Episodes</h2>
          </div>

          <div class="content-grid">
            <Card
              v-for="episode in module.episodes"
              :key="episode.id"
              class="episode-card"
              @click="goToEpisode(episode.id)"
            >
              <template #header>
                <div class="episode-card-header">
                  <div class="number-badge-sm">{{ episode.episodeNumber }}</div>
                  <div class="flex gap-sm flex-wrap">
                    <Tag :value="episode.type" :icon="episodeTypeIcon(episode.type)" />
                    <Tag v-if="episode.hasAudio" value="Audio" severity="success" icon="pi pi-volume-up" />
                  </div>
                </div>
              </template>
              <template #title>
                {{ episode.title }}
              </template>
              <template #content>
                <div class="icon-label-group episode-stats">
                  <span>
                    <i class="pi pi-list"></i>
                    {{ episode.totalExercises }} exercises
                  </span>
                  <span>
                    <i class="pi pi-clock"></i>
                    ~{{ episode.estimatedMinutes }} min
                  </span>
                </div>
              </template>
              <template #footer>
                <Button
                  label="Start Episode"
                  icon="pi pi-play"
                  @click="goToEpisode(episode.id)"
                />
              </template>
            </Card>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.module-detail-view {
  min-height: 100vh;
  background: var(--bg-primary);
  padding: 2rem 1rem;
}

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

.episode-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
  background: var(--bg-tertiary);
}

/* Objectives Grid (similar to checklist but in grid layout) */
.objectives-grid {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.objectives-grid li {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.objectives-grid i {
  color: var(--success);
  margin-top: 0.25rem;
  flex-shrink: 0;
}

.episode-card {
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.episode-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

@media (max-width: 768px) {
  .module-detail-view {
    padding: 1rem;
  }

  .objectives-grid {
    grid-template-columns: 1fr;
  }
}
</style>
