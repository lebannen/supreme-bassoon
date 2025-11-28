<script setup lang="ts">
import {onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import Divider from 'primevue/divider'
import ProgressSpinner from 'primevue/progressspinner'
import {useCourseStore} from '@/stores/course'

const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()
const {currentCourse: course, loading, error} = storeToRefs(courseStore)

onMounted(() => {
  const slug = route.params.slug as string
  if (slug) courseStore.loadCourseBySlug(slug)
})
</script>

<template>
  <div class="detail-container content-area-lg">
    <div v-if="loading" class="loading-state">
      <ProgressSpinner/>
    </div>
    <Message v-else-if="error" severity="error">{{ error }}</Message>

    <div v-else-if="course" class="content-area-lg">
      <div class="detail-header">
        <Button icon="pi pi-arrow-left" text rounded @click="router.push('/courses')" class="detail-header-back-btn"/>
        <div class="detail-header-content">
          <div class="meta-badges">
            <Tag :value="course.languageCode.toUpperCase()"/>
            <Tag :value="course.cefrLevel" severity="secondary"/>
          </div>
          <h1>{{ course.name }}</h1>
          <p v-if="course.description" class="detail-description">{{ course.description }}</p>
          <div class="icon-label-group">
            <span class="icon-label"><i class="pi pi-book"></i>{{ course.modules.length }} modules</span>
            <span class="icon-label"><i class="pi pi-clock"></i>~{{ course.estimatedHours }}h total</span>
          </div>
        </div>
      </div>

      <Card v-if="course.objectives?.length">
        <template #title>
          <div class="card-title-icon"><i class="pi pi-list-check"></i><span>What You'll Learn</span></div>
        </template>
        <template #content>
          <ul class="checklist">
            <li v-for="(objective, index) in course.objectives" :key="index">
              <i class="pi pi-check-circle text-primary"></i><span>{{ objective }}</span>
            </li>
          </ul>
        </template>
      </Card>

      <Divider/>

      <div class="section">
        <div class="section-header"><h2>Course Modules</h2></div>
        <div class="task-list">
          <Card v-for="module in course.modules" :key="module.id" class="card-interactive"
                @click="router.push(`/course-modules/${module.id}`)">
            <template #content>
              <div class="flex align-items-center gap-4">
                <div class="number-badge bg-primary text-primary-contrast">{{ module.moduleNumber }}</div>
                <div class="flex-1">
                  <h3 class="text-xl font-bold mb-1">{{ module.title }}</h3>
                  <p v-if="module.theme" class="text-sm text-secondary m-0">{{ module.theme }}</p>
                </div>
                <i class="pi pi-chevron-right text-secondary text-xl"></i>
              </div>
            </template>
          </Card>
        </div>
      </div>
    </div>
  </div>
</template>
