<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import Divider from 'primevue/divider'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const route = useRoute()
const router = useRouter()

interface ModuleSummary {
  id: number
  moduleNumber: number
  title: string
  theme: string | null
  estimatedMinutes: number
  totalEpisodes: number
}

interface Course {
  id: number
  slug: string
  name: string
  languageCode: string
  cefrLevel: string
  description: string | null
  objectives: string[]
  estimatedHours: number
  modules: ModuleSummary[]
}

const course = ref<Course | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

async function loadCourse() {
  const slug = route.params.slug as string
  if (!slug) {
    error.value = 'No course slug provided'
    loading.value = false
    return
  }

  try {
    loading.value = true
    error.value = null

    const response = await fetch(`${API_BASE}/api/courses/${slug}`)

    if (!response.ok) {
      throw new Error('Failed to load course')
    }

    course.value = await response.json()
  } catch (err) {
    console.error('Error loading course:', err)
    error.value = err instanceof Error ? err.message : 'Failed to load course'
  } finally {
    loading.value = false
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
    <div class="course-container">
      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner" style="font-size: 3rem"></i>
        <p>Loading course...</p>
      </div>

      <!-- Error State -->
      <Message v-else-if="error" severity="error" :closable="false">
        {{ error }}
      </Message>

      <!-- Course Content -->
      <div v-else-if="course" class="course-content">
        <!-- Header -->
        <div class="course-header">
          <Button
            icon="pi pi-arrow-left"
            text
            rounded
            @click="goBack"
            class="back-button"
          />
          <div class="header-info">
            <div class="course-meta">
              <Tag :value="course.languageCode.toUpperCase()" severity="info" />
              <Tag :value="course.cefrLevel" />
            </div>
            <h1>{{ course.name }}</h1>
            <p v-if="course.description" class="course-description">
              {{ course.description }}
            </p>
            <div class="course-stats">
              <span><i class="pi pi-book"></i> {{ course.modules.length }} modules</span>
              <span><i class="pi pi-clock"></i> ~{{ course.estimatedHours }}h total</span>
            </div>
          </div>
        </div>

        <!-- Learning Objectives -->
        <Card v-if="course.objectives && course.objectives.length > 0" class="objectives-card">
          <template #title>
            <div class="card-title">
              <i class="pi pi-list-check"></i>
              <span>What You'll Learn</span>
            </div>
          </template>
          <template #content>
            <ul class="objectives-list">
              <li v-for="(objective, index) in course.objectives" :key="index">
                <i class="pi pi-check-circle"></i>
                <span>{{ objective }}</span>
              </li>
            </ul>
          </template>
        </Card>

        <Divider />

        <!-- Modules -->
        <div class="modules-section">
          <h2>Course Modules</h2>

          <div class="modules-list">
            <Card
              v-for="module in course.modules"
              :key="module.id"
              class="module-card"
              @click="goToModule(module.id)"
            >
              <template #title>
                <div class="module-title">
                  <div class="module-number">{{ module.moduleNumber }}</div>
                  <div class="module-info">
                    <h3>{{ module.title }}</h3>
                    <p v-if="module.theme" class="module-theme">{{ module.theme }}</p>
                  </div>
                </div>
              </template>
              <template #content>
                <div class="module-stats">
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
  background: var(--surface-ground);
  padding: 2rem 1rem;
}

.course-container {
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

.course-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

/* Header */
.course-header {
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

.course-meta {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.course-header h1 {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 0 0 1rem 0;
  color: var(--text-color);
}

.course-description {
  font-size: 1.125rem;
  color: var(--text-color-secondary);
  line-height: 1.6;
  margin: 0 0 1.5rem 0;
}

.course-stats {
  display: flex;
  gap: 2rem;
  color: var(--text-color-secondary);
}

.course-stats span {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.course-stats i {
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
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.objectives-list li {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  line-height: 1.6;
}

.objectives-list i {
  color: var(--green-500);
  font-size: 1.25rem;
  margin-top: 0.125rem;
  flex-shrink: 0;
}

/* Modules */
.modules-section h2 {
  margin-bottom: 1.5rem;
  color: var(--text-color);
}

.modules-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.module-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.module-card:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.module-title {
  display: flex;
  gap: 1.5rem;
  align-items: center;
}

.module-number {
  width: 4rem;
  height: 4rem;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-600));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
  flex-shrink: 0;
}

.module-info {
  flex: 1;
}

.module-info h3 {
  margin: 0 0 0.25rem 0;
  font-size: 1.25rem;
  color: var(--text-color);
}

.module-theme {
  margin: 0;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  font-weight: normal;
}

.module-stats {
  display: flex;
  gap: 2rem;
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.module-stats span {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.module-stats i {
  color: var(--primary-color);
}

@media (max-width: 768px) {
  .course-detail-view {
    padding: 1rem;
  }

  .course-header {
    flex-direction: column;
  }

  .course-header h1 {
    font-size: 2rem;
  }

  .module-title {
    gap: 1rem;
  }

  .module-number {
    width: 3rem;
    height: 3rem;
    font-size: 1.25rem;
  }

  .module-stats {
    flex-direction: column;
    gap: 0.5rem;
  }
}
</style>
