<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const router = useRouter()

interface Course {
  id: number
  slug: string
  name: string
  languageCode: string
  cefrLevel: string
  description: string
  estimatedHours: number
  totalModules: number
  totalEpisodes: number
}

const courses = ref<Course[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

async function loadCourses() {
  try {
    loading.value = true
    error.value = null

    const response = await fetch(`${API_BASE}/api/courses`)

    if (!response.ok) {
      throw new Error('Failed to load courses')
    }

    courses.value = await response.json()
  } catch (err) {
    console.error('Error loading courses:', err)
    error.value = err instanceof Error ? err.message : 'Failed to load courses'
  } finally {
    loading.value = false
  }
}

function goToCourse(slug: string) {
  router.push(`/courses/${slug}`)
}

onMounted(() => {
  loadCourses()
})
</script>

<template>
  <div class="courses-view">
    <div class="courses-container">
      <div class="header">
        <h1>
          <i class="pi pi-book"></i>
          Available Courses
        </h1>
        <p class="subtitle">Choose a course to start your learning journey</p>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner" style="font-size: 3rem"></i>
        <p>Loading courses...</p>
      </div>

      <!-- Error State -->
      <Message v-else-if="error" severity="error" :closable="false">
        {{ error }}
      </Message>

      <!-- Empty State -->
      <Card v-else-if="courses.length === 0" class="empty-state">
        <template #content>
          <div class="empty-content">
            <i class="pi pi-inbox"></i>
            <h3>No Courses Available</h3>
            <p>Check back soon for new learning content!</p>
          </div>
        </template>
      </Card>

      <!-- Courses Grid -->
      <div v-else class="courses-grid">
        <Card
          v-for="course in courses"
          :key="course.id"
          class="course-card"
          @click="goToCourse(course.slug)"
        >
          <template #header>
            <div class="course-card-header">
              <Tag :value="course.languageCode.toUpperCase()" severity="info" />
              <Tag :value="course.cefrLevel" />
            </div>
          </template>
          <template #title>
            {{ course.name }}
          </template>
          <template #content>
            <p class="course-description">{{ course.description }}</p>

            <div class="course-stats">
              <div class="stat">
                <i class="pi pi-book"></i>
                <span>{{ course.totalModules }} modules</span>
              </div>
              <div class="stat">
                <i class="pi pi-list"></i>
                <span>{{ course.totalEpisodes }} episodes</span>
              </div>
              <div class="stat">
                <i class="pi pi-clock"></i>
                <span>~{{ course.estimatedHours }}h</span>
              </div>
            </div>
          </template>
          <template #footer>
            <Button
              label="Start Learning"
              icon="pi pi-arrow-right"
              iconPos="right"
              @click="goToCourse(course.slug)"
              class="start-button"
            />
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.courses-view {
  min-height: 100vh;
  background: var(--surface-ground);
  padding: 2rem 1rem;
}

.courses-container {
  max-width: 1400px;
  margin: 0 auto;
}

.header {
  margin-bottom: 2rem;
  text-align: center;
}

.header h1 {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--text-color);
  margin: 0 0 0.5rem 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
}

.header h1 i {
  color: var(--primary-color);
}

.subtitle {
  font-size: 1.125rem;
  color: var(--text-color-secondary);
  margin: 0;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem;
  gap: 1rem;
}

.empty-content {
  text-align: center;
  padding: 3rem 2rem;
}

.empty-content i {
  font-size: 4rem;
  color: var(--text-color-secondary);
  opacity: 0.5;
  margin-bottom: 1rem;
}

.empty-content h3 {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-color);
  margin: 0 0 0.5rem 0;
}

.empty-content p {
  color: var(--text-color-secondary);
  margin: 0;
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 2rem;
}

.course-card {
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.course-card-header {
  display: flex;
  gap: 0.5rem;
  padding: 1rem;
  background: var(--surface-50);
}

.course-description {
  color: var(--text-color-secondary);
  line-height: 1.6;
  margin-bottom: 1.5rem;
  min-height: 3rem;
}

.course-stats {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: auto;
}

.stat {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--text-color);
  font-size: 0.875rem;
}

.stat i {
  color: var(--primary-color);
  font-size: 1rem;
}

.start-button {
  width: 100%;
}

@media (max-width: 768px) {
  .courses-view {
    padding: 1rem;
  }

  .header h1 {
    font-size: 2rem;
  }

  .courses-grid {
    grid-template-columns: 1fr;
  }
}
</style>
