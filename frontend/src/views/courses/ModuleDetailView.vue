<script setup lang="ts">
import {onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import ProgressSpinner from 'primevue/progressspinner'
import {useCourseStore} from '@/stores/course'

const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()
const {currentModule: module, loading, error} = storeToRefs(courseStore)

const episodeTypeIcon = (type: string) => ({
  STORY: 'pi-book',
  DIALOGUE: 'pi-comments',
  ARTICLE: 'pi-file',
  AUDIO_LESSON: 'pi-volume-up'
}[type] || 'pi-circle')

onMounted(() => {
  const moduleId = Number(route.params.id)
  if (moduleId) courseStore.loadModule(moduleId)
})
</script>

<template>
  <div class="detail-container content-area-lg">
    <div v-if="loading" class="loading-state">
      <ProgressSpinner/>
    </div>
    <Message v-else-if="error" severity="error">{{ error }}</Message>

    <div v-else-if="module" class="content-area-lg">
      <div class="detail-header">
        <Button icon="pi pi-arrow-left" text rounded @click="router.push(`/courses/${module.courseId}`)"
                class="detail-header-back-btn"/>
        <div class="detail-header-content">
          <div class="meta-badges">
            <Tag severity="contrast">Module {{ module.moduleNumber }}</Tag>
            <Tag v-if="module.theme" :value="module.theme"/>
          </div>
          <h1>{{ module.title }}</h1>
          <p v-if="module.description" class="detail-description">{{ module.description }}</p>
          <div class="icon-label-group">
            <span class="icon-label"><i class="pi pi-list"></i>{{ module.episodes.length }} episodes</span>
            <span class="icon-label"><i class="pi pi-clock"></i>~{{ module.estimatedMinutes }} min</span>
          </div>
        </div>
      </div>

      <Card v-if="module.objectives?.length">
        <template #title>
          <div class="card-title-icon"><i class="pi pi-target"></i><span>Module Objectives</span></div>
        </template>
        <template #content>
          <ul class="checklist md:grid md:grid-cols-2">
            <li v-for="(objective, index) in module.objectives" :key="index">
              <i class="pi pi-check-circle text-primary"></i><span>{{ objective }}</span>
            </li>
          </ul>
        </template>
      </Card>

      <div class="section">
        <div class="section-header"><h2>Episodes</h2></div>
        <div class="content-grid">
          <Card v-for="episode in module.episodes" :key="episode.id" class="card-interactive"
                @click="router.push(`/episodes/${episode.id}`)">
            <template #header>
              <div class="p-md flex justify-content-between align-items-center bg-surface-section">
                <div class="number-badge-sm bg-primary text-primary-contrast">{{ episode.episodeNumber }}</div>
                <div class="flex gap-2 flex-wrap">
                  <Tag :value="episode.type" :icon="episodeTypeIcon(episode.type)"/>
                  <Tag v-if="episode.hasAudio" value="Audio" severity="success" icon="pi pi-volume-up"/>
                </div>
              </div>
            </template>
            <template #title>{{ episode.title }}</template>
            <template #footer>
              <div class="icon-label-group compact">
                <span class="icon-label"><i class="pi pi-list"></i>{{ episode.totalExercises }} exercises</span>
                <span class="icon-label"><i class="pi pi-clock"></i>~{{ episode.estimatedMinutes }} min</span>
              </div>
            </template>
          </Card>
        </div>
      </div>
    </div>
  </div>
</template>
