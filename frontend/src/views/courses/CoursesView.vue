<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import FilterBar from '@/components/ui/FilterBar.vue'
import CourseCard from '@/components/ui/CourseCard.vue'
import {useCourseStore} from '@/stores/course'

const router = useRouter()
const courseStore = useCourseStore()
const {courses, loading, error} = storeToRefs(courseStore)

const searchQuery = ref('')
const selectedLevel = ref('all')
const selectedLanguage = ref('all')

const levelOptions = [
  {label: 'All Levels', value: 'all'}, {label: 'A1', value: 'A1'}, {label: 'A2', value: 'A2'},
  {label: 'B1', value: 'B1'}, {label: 'B2', value: 'B2'}, {label: 'C1', value: 'C1'}, {label: 'C2', value: 'C2'},
]
const languageOptions = computed(() => [
  {label: 'All Languages', value: 'all'},
  ...[...new Set(courses.value.map(c => c.languageCode))].map(lang => ({label: lang.toUpperCase(), value: lang})),
])

const filteredCourses = computed(() => {
  return courses.value.filter(course => {
    const search = searchQuery.value.toLowerCase()
    return (!search || course.name.toLowerCase().includes(search) || course.description.toLowerCase().includes(search)) &&
        (selectedLevel.value === 'all' || course.cefrLevel === selectedLevel.value) &&
        (selectedLanguage.value === 'all' || course.languageCode === selectedLanguage.value)
  })
})

onMounted(() => courseStore.loadCourses())
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <h1>Explore Courses</h1>
      <p class="text-secondary">Choose a course to start your learning journey.</p>
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner/>
    </div>
    <Message v-else-if="error" severity="error">{{ error }}</Message>

    <template v-else>
      <FilterBar
          search-placeholder="Search courses..."
          :filters="[
          { label: 'Level', options: levelOptions, modelValue: selectedLevel },
          { label: 'Language', options: languageOptions, modelValue: selectedLanguage },
        ]"
          :results-count="filteredCourses.length"
          @update:search="searchQuery = $event"
          @update:filter="(idx, val) => idx === 0 ? selectedLevel = val : selectedLanguage = val"
      />

      <div v-if="courses.length === 0" class="empty-state">
        <i class="pi pi-book empty-icon"></i>
        <h3>No Courses Available</h3>
        <p class="text-secondary">Check back soon for new learning content!</p>
      </div>
      <div v-else-if="filteredCourses.length === 0" class="empty-state">
        <i class="pi pi-search empty-icon"></i>
        <h3>No Courses Found</h3>
        <p class="text-secondary">Try adjusting your search or filters.</p>
      </div>
      <div v-else class="content-grid">
        <CourseCard
            v-for="course in filteredCourses"
          :key="course.id"
            :name="course.name"
            :description="course.description"
            :language-code="course.languageCode"
            :cefr-level="course.cefrLevel"
            :total-modules="course.totalModules"
            :total-episodes="course.totalEpisodes"
            @click="router.push(`/courses/${course.slug}`)"
        />
      </div>
    </template>
  </div>
</template>
