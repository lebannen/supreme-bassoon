<script setup lang="ts">
import {onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import Divider from 'primevue/divider'
import {useCourseStore} from '@/stores/course'

const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()

// Destructure store state with refs
const {currentCourse: course, loading, error} = storeToRefs(courseStore)

function loadCourse() {
  const slug = route.params.slug as string
  if (slug) {
    courseStore.loadCourseBySlug(slug)
  }
}

function goToModule(moduleId: number) {
  router.push(`/course-modules/${moduleId}`)
}

function goBack() {
  router.push('/courses')
}

onMounted(() => {
  loadCourse()
})
</script>

<template>
  <div class="course-detail-view">
    <div class="detail-container">
      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner loading-spinner"></i>
        <p>Loading course...</p>
      </div>

      <!-- Error State -->
      <Message v-else-if="error" severity="error" :closable="false">
        {{ error }}
      </Message>

      <!-- Course Content -->
      <div v-else-if="course" class="content-area-lg">
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
              <Tag :value="course.languageCode.toUpperCase()" severity="info" />
              <Tag :value="course.cefrLevel" />
            </div>
            <h1>{{ course.name }}</h1>
            <p v-if="course.description" class="detail-description">
              {{ course.description }}
            </p>
            <div class="icon-label-group">
              <span><i class="pi pi-book"></i> {{ course.modules.length }} modules</span>
              <span><i class="pi pi-clock"></i> ~{{ course.estimatedHours }}h total</span>
            </div>
          </div>
        </div>

        <!-- Learning Objectives -->
        <Card v-if="course.objectives && course.objectives.length > 0">
          <template #title>
            <div class="card-title-icon">
              <i class="pi pi-list-check"></i>
              <span>What You'll Learn</span>
            </div>
          </template>
          <template #content>
            <ul class="checklist">
              <li v-for="(objective, index) in course.objectives" :key="index">
                <i class="pi pi-check-circle"></i>
                <span>{{ objective }}</span>
              </li>
            </ul>
          </template>
        </Card>

        <Divider />

        <!-- Modules -->
        <div class="section">
          <div class="section-header">
            <h2>Course Modules</h2>
          </div>

          <div class="task-list">
            <Card
              v-for="module in course.modules"
              :key="module.id"
              class="module-card"
              @click="goToModule(module.id)"
            >
              <template #title>
                <div class="flex gap-lg items-center">
                  <div class="number-badge">{{ module.moduleNumber }}</div>
                  <div class="flex-1">
                    <h3 class="text-xl font-semibold mb-xs">{{ module.title }}</h3>
                    <p v-if="module.theme" class="text-sm text-secondary">{{ module.theme }}</p>
                  </div>
                </div>
              </template>
              <template #content>
                <div class="icon-label-group">
                  <span>
                    <i class="pi pi-list"></i>
                    {{ module.totalEpisodes }} episodes
                  </span>
                  <span>
                    <i class="pi pi-clock"></i>
                    ~{{ module.estimatedMinutes }} min
                  </span>
                </div>
              </template>
              <template #footer>
                <Button
                  label="Start Module"
                  icon="pi pi-arrow-right"
                  iconPos="right"
                  @click="goToModule(module.id)"
                  outlined
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
.course-detail-view {
  min-height: 100vh;
  background: var(--bg-primary);
  padding: 2rem 1rem;
}

.module-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.module-card:hover {
  transform: translateX(4px);
  box-shadow: var(--shadow-md);
}

.loading-spinner {
  font-size: 3rem;
}

@media (max-width: 768px) {
  .course-detail-view {
    padding: 1rem;
  }
}
</style>
