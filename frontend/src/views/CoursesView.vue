<script setup lang="ts">
import {ref, onMounted, computed} from 'vue'
import { useRouter } from 'vue-router'
import Message from 'primevue/message'
import FilterBar from '@/components/ui/FilterBar.vue'
import CourseCard from '@/components/ui/CourseCard.vue'

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

// Filter state
const searchQuery = ref('')
const selectedLevel = ref('all')
const selectedLanguage = ref('all')

// Filter options
const levelOptions = [
  {label: 'All Levels', value: 'all'},
  {label: 'A1 - Beginner', value: 'A1'},
  {label: 'A2 - Elementary', value: 'A2'},
  {label: 'B1 - Intermediate', value: 'B1'},
  {label: 'B2 - Upper Intermediate', value: 'B2'},
  {label: 'C1 - Advanced', value: 'C1'},
  {label: 'C2 - Proficient', value: 'C2'}
]

const languageOptions = computed(() => {
  const uniqueLanguages = [...new Set(courses.value.map(c => c.languageCode))]
  return [
    {label: 'All Languages', value: 'all'},
    ...uniqueLanguages.map(lang => ({
      label: lang.toUpperCase(),
      value: lang
    }))
  ]
})

// Filtered courses
const filteredCourses = computed(() => {
  return courses.value.filter(course => {
    // Search filter
    const matchesSearch =
        !searchQuery.value ||
        course.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
        course.description.toLowerCase().includes(searchQuery.value.toLowerCase())

    // Level filter
    const matchesLevel = selectedLevel.value === 'all' || course.cefrLevel === selectedLevel.value

    // Language filter
    const matchesLanguage =
        selectedLanguage.value === 'all' || course.languageCode === selectedLanguage.value

    return matchesSearch && matchesLevel && matchesLanguage
  })
})

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

function handleSearchUpdate(value: string) {
  searchQuery.value = value
}

function handleFilterUpdate(filterIndex: number, value: string) {
  if (filterIndex === 0) {
    selectedLevel.value = value
  } else if (filterIndex === 1) {
    selectedLanguage.value = value
  }
}

onMounted(() => {
  loadCourses()
})
</script>

<template>
  <div class="courses-view">
    <!-- Page Header -->
    <div class="page-header">
      <h1>Explore Courses</h1>
      <p>Choose a course to start your learning journey</p>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <i class="pi pi-spin pi-spinner loading-spinner-primary"></i>
      <p>Loading courses...</p>
    </div>

    <!-- Error State -->
    <Message v-else-if="error" severity="error" :closable="false" class="error-message">
      {{ error }}
    </Message>

    <!-- Courses Content -->
    <template v-else>
      <!-- Filter Bar -->
      <FilterBar
          searchPlaceholder="Search courses..."
          :filters="[
          { label: 'Level', options: levelOptions, modelValue: selectedLevel },
          { label: 'Language', options: languageOptions, modelValue: selectedLanguage }
        ]"
          :resultsCount="filteredCourses.length"
          @update:search="handleSearchUpdate"
          @update:filter="handleFilterUpdate"
      />

      <!-- Empty State (no courses) -->
      <div v-if="courses.length === 0" class="empty-state card card-padding">
        <div class="empty-content">
          <span class="empty-icon">📚</span>
          <h3>No Courses Available</h3>
          <p>Check back soon for new learning content!</p>
        </div>
      </div>

      <!-- Empty State (no results) -->
      <div v-else-if="filteredCourses.length === 0" class="empty-state card card-padding">
        <div class="empty-content">
          <span class="empty-icon">🔍</span>
          <h3>No Courses Found</h3>
          <p>Try adjusting your search or filters</p>
        </div>
      </div>

      <!-- Courses Grid -->
      <div v-else class="content-grid">
        <CourseCard
            v-for="course in filteredCourses"
          :key="course.id"
            :name="course.name"
            :description="course.description"
            :languageCode="course.languageCode"
            :cefrLevel="course.cefrLevel"
            :totalModules="course.totalModules"
            :totalEpisodes="course.totalEpisodes"
            :estimatedHours="course.estimatedHours"
          @click="goToCourse(course.slug)"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.courses-view {
  max-width: 1400px;
  margin: 0 auto;
}

/* Error Message */
.error-message {
  margin-bottom: var(--spacing-3xl);
}

/* Empty State Content Wrapper */
.empty-content {
  max-width: 400px;
  margin: 0 auto;
}

/* Loading spinner with primary color */
.loading-spinner-primary {
  font-size: 3rem;
  color: var(--primary);
}
</style>
