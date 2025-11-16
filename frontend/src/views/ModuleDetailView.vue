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
    <div class="module-container">
      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner" style="font-size: 3rem"></i>
        <p>Loading module...</p>
      </div>

      <!-- Error State -->
      <Message v-else-if="error" severity="error" :closable="false">
        {{ error }}
      </Message>

      <!-- Module Content -->
      <div v-else-if="module" class="module-content">
        <!-- Header -->
        <div class="module-header">
          <Button
            icon="pi pi-arrow-left"
            text
            rounded
            @click="goBack"
            class="back-button"
          />
          <div class="header-info">
            <div class="module-meta">
              <span class="module-number-badge">Module {{ module.moduleNumber }}</span>
              <Tag v-if="module.theme" :value="module.theme" />
            </div>
            <h1>{{ module.title }}</h1>
            <p v-if="module.description" class="module-description">
              {{ module.description }}
            </p>
            <div class="module-stats">
              <span><i class="pi pi-list"></i> {{ module.episodes.length }} episodes</span>
              <span><i class="pi pi-clock"></i> ~{{ module.estimatedMinutes }} min</span>
            </div>
          </div>
        </div>

        <!-- Learning Objectives -->
        <Card v-if="module.objectives && module.objectives.length > 0" class="objectives-card">
          <template #title>
            <div class="card-title">
              <i class="pi pi-target"></i>
              <span>Module Objectives</span>
            </div>
          </template>
          <template #content>
            <ul class="objectives-list">
              <li v-for="(objective, index) in module.objectives" :key="index">
                <i class="pi pi-check"></i>
                <span>{{ objective }}</span>
              </li>
            </ul>
          </template>
        </Card>

        <!-- Episodes -->
        <div class="episodes-section">
          <h2>Episodes</h2>

          <div class="episodes-list">
            <Card
              v-for="episode in module.episodes"
              :key="episode.id"
              class="episode-card"
              @click="goToEpisode(episode.id)"
            >
              <template #header>
                <div class="episode-card-header">
                  <div class="episode-number">{{ episode.episodeNumber }}</div>
                  <div class="episode-badges">
                    <Tag :value="episode.type" :icon="episodeTypeIcon(episode.type)" />
                    <Tag v-if="episode.hasAudio" value="Audio" severity="success" icon="pi pi-volume-up" />
                  </div>
                </div>
              </template>
              <template #title>
                {{ episode.title }}
              </template>
              <template #content>
                <div class="episode-stats">
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
  background: var(--surface-ground);
  padding: 2rem 1rem;
}

.module-container {
  max-width: 1000px;
  margin: 0 auto;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem;
  gap: 1rem;
}

.module-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

/* Header */
.module-header {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.back-button {
  margin-top: 0.5rem;
}

.header-info {
  flex: 1;
}

.module-meta {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
  align-items: center;
}

.module-number-badge {
  background: var(--primary-100);
  color: var(--primary-700);
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 600;
  text-transform: uppercase;
}

.module-header h1 {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 0 0 1rem 0;
  color: var(--text-color);
}

.module-description {
  font-size: 1.125rem;
  color: var(--text-color-secondary);
  line-height: 1.6;
  margin: 0 0 1.5rem 0;
}

.module-stats {
  display: flex;
  gap: 2rem;
  color: var(--text-color-secondary);
}

.module-stats span {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.module-stats i {
  color: var(--primary-color);
}

/* Objectives */
.card-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--text-color);
}

.card-title i {
  color: var(--primary-color);
}

.objectives-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.objectives-list li {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.objectives-list i {
  color: var(--green-500);
  margin-top: 0.25rem;
  flex-shrink: 0;
}

/* Episodes */
.episodes-section h2 {
  margin-bottom: 1.5rem;
  color: var(--text-color);
}

.episodes-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
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
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.episode-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: var(--surface-50);
}

.episode-number {
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-600));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  font-weight: 700;
}

.episode-badges {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.episode-stats {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.episode-stats span {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.episode-stats i {
  color: var(--primary-color);
}

@media (max-width: 768px) {
  .module-detail-view {
    padding: 1rem;
  }

  .module-header {
    flex-direction: column;
  }

  .module-header h1 {
    font-size: 2rem;
  }

  .episodes-list {
    grid-template-columns: 1fr;
  }

  .objectives-list {
    grid-template-columns: 1fr;
  }
}
</style>
