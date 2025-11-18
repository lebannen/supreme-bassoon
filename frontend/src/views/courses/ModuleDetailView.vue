<script setup lang="ts">
import {onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import {useCourseStore} from '@/stores/course'

const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()

// Destructure store state with refs
const {currentModule: module, loading, error} = storeToRefs(courseStore)

const episodeTypeIcon = (type: string) => {
  switch (type) {
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
}

function loadModule() {
  const moduleId = route.params.id
  if (moduleId) {
    courseStore.loadModule(Number(moduleId))
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
  <div class="page-container-with-padding">
    <div class="view-container">
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
              class="card-interactive"
              @click="goToEpisode(episode.id)"
            >
              <template #header>
                <div class="episode-card-header">
                  <div class="number-badge-sm">{{ episode.episodeNumber }}</div>
                  <div class="flex gap-sm flex-wrap">
                    <Tag :value="episode.type" :icon="episodeTypeIcon(episode.type)" />
                    <Tag
                        v-if="episode.hasAudio"
                        value="Audio"
                        severity="success"
                        icon="pi pi-volume-up"
                    />
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
                <Button label="Start Episode" icon="pi pi-play" @click="goToEpisode(episode.id)"/>
              </template>
            </Card>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Custom loading spinner */
.loading-spinner {
  font-size: 3rem;
}

/* Episode card header */
.episode-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
}

/* Objectives Grid */
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
  margin-top: 0.25rem;
  flex-shrink: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .objectives-grid {
    grid-template-columns: 1fr;
  }
}
</style>
