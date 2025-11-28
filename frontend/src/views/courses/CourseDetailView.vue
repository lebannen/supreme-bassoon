<script setup lang="ts">
import {computed, onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import Divider from 'primevue/divider'
import ProgressBar from 'primevue/progressbar'
import ProgressSpinner from 'primevue/progressspinner'
import {useCourseStore} from '@/stores/course'
import {useProgressStore} from '@/stores/progress'
import {useAuthStore} from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()
const progressStore = useProgressStore()
const authStore = useAuthStore()

const {currentCourse: course, loading, error} = storeToRefs(courseStore)

const enrollment = computed(() => {
  if (!course.value) return null
  return progressStore.getEnrollment(course.value.id)
})

const isEnrolled = computed(() => !!enrollment.value)

const firstModuleWithEpisode = computed(() => {
  if (!course.value?.modules?.length) return null
  return course.value.modules[0]
})

function handleStartCourse() {
  if (!course.value) return

  const firstModule = firstModuleWithEpisode.value

  progressStore.enrollInCourse({
    id: course.value.id,
    name: course.value.name,
    slug: course.value.slug,
    languageCode: course.value.languageCode,
    cefrLevel: course.value.cefrLevel,
    totalEpisodes: course.value.totalEpisodes,
    firstModuleId: firstModule?.id,
    firstModuleName: firstModule?.title
  })

  // Navigate to first module
  if (firstModule) {
    router.push(`/course-modules/${firstModule.id}`)
  }
}

function handleContinueLearning() {
  if (!enrollment.value) return

  if (enrollment.value.currentModuleId) {
    router.push(`/course-modules/${enrollment.value.currentModuleId}`)
  } else if (firstModuleWithEpisode.value) {
    router.push(`/course-modules/${firstModuleWithEpisode.value.id}`)
  }
}

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
        <Button icon="pi pi-arrow-left" text rounded @click="router.push('/courses')"
                class="detail-header-back-btn"/>
        <div class="detail-header-content">
          <div class="meta-badges">
            <Tag :value="course.languageCode.toUpperCase()"/>
            <Tag :value="course.cefrLevel" severity="secondary"/>
            <Tag v-if="isEnrolled" value="Enrolled" severity="success" icon="pi pi-check"/>
          </div>
          <h1>{{ course.name }}</h1>
          <p v-if="course.description" class="detail-description">{{ course.description }}</p>
          <div class="icon-label-group">
            <span class="icon-label"><i class="pi pi-book"></i>{{ course.modules.length }} modules</span>
            <span class="icon-label"><i class="pi pi-video"></i>{{ course.totalEpisodes }} episodes</span>
            <span class="icon-label"><i class="pi pi-clock"></i>~{{ course.estimatedHours }}h total</span>
          </div>
        </div>
      </div>

      <!-- Enrollment Progress Card (if enrolled) -->
      <Card v-if="isEnrolled && enrollment" class="enrollment-progress-card">
        <template #content>
          <div class="enrollment-progress-content">
            <div class="progress-info">
              <div class="progress-header">
                <span class="font-semibold">Your Progress</span>
                <span class="font-bold text-primary">{{ enrollment.progress }}%</span>
              </div>
              <ProgressBar :value="enrollment.progress" style="height: 0.75rem"/>
              <div class="progress-meta text-sm text-secondary">
                {{ enrollment.completedEpisodes }} / {{ enrollment.totalEpisodes }} episodes completed
              </div>
            </div>
            <div class="progress-actions">
              <Button
                  label="Continue Learning"
                  icon="pi pi-play"
                  @click="handleContinueLearning"
              />
            </div>
          </div>
        </template>
      </Card>

      <!-- Start Course CTA (if not enrolled) -->
      <Card v-else-if="authStore.isAuthenticated" class="start-course-card">
        <template #content>
          <div class="start-course-content">
            <div class="start-course-info">
              <i class="pi pi-play-circle text-4xl text-primary"></i>
              <div>
                <h3 class="m-0 mb-1">Ready to start learning?</h3>
                <p class="m-0 text-secondary">
                  Begin your journey with {{ course.modules.length }} modules and {{
                    course.totalEpisodes
                  }} episodes.
                </p>
              </div>
            </div>
            <Button
                label="Start Course"
                icon="pi pi-play"
                size="large"
                @click="handleStartCourse"
            />
          </div>
        </template>
      </Card>

      <!-- Login prompt for non-authenticated users -->
      <Card v-else class="login-prompt-card">
        <template #content>
          <div class="login-prompt-content">
            <i class="pi pi-lock text-3xl text-secondary"></i>
            <div>
              <h3 class="m-0 mb-1">Sign in to start learning</h3>
              <p class="m-0 text-secondary">Create a free account to track your progress.</p>
            </div>
            <div class="login-prompt-actions">
              <Button
                  label="Sign In"
                  @click="router.push({ name: 'login', query: { redirect: route.fullPath } })"
              />
              <Button
                  label="Register"
                  severity="secondary"
                  outlined
                  @click="router.push({ name: 'register', query: { redirect: route.fullPath } })"
              />
            </div>
          </div>
        </template>
      </Card>

      <Card v-if="course.objectives?.length">
        <template #title>
          <div class="card-title-icon"><i class="pi pi-list-check"></i><span>What You'll Learn</span></div>
        </template>
        <template #content>
          <ul class="checklist">
            <li v-for="(objective, index) in course.objectives" :key="index">
              <i class="pi pi-check-circle text-primary"></i>
              <span>{{ objective }}</span>
            </li>
          </ul>
        </template>
      </Card>

      <Divider/>

      <div class="section">
        <div class="section-header">
          <h2>Course Modules</h2>
        </div>
        <div class="task-list">
          <Card
              v-for="module in course.modules"
              :key="module.id"
              :class="isEnrolled ? 'card-interactive' : 'module-locked'"
              @click="isEnrolled && router.push(`/course-modules/${module.id}`)"
          >
            <template #content>
              <div class="flex align-items-center gap-4">
                <div
                    class="number-badge"
                    :class="isEnrolled ? 'bg-primary text-primary-contrast' : 'bg-surface-200'"
                >
                  {{ module.moduleNumber }}
                </div>
                <div class="flex-1">
                  <h3 class="text-xl font-bold mb-1">{{ module.title }}</h3>
                  <p v-if="module.theme" class="text-sm text-secondary m-0">{{ module.theme }}</p>
                </div>
                <div class="module-meta text-sm text-secondary">
                  {{ module.totalEpisodes }} episodes
                </div>
                <i
                    :class="isEnrolled ? 'pi pi-chevron-right' : 'pi pi-lock'"
                    class="text-secondary text-xl"
                ></i>
              </div>
            </template>
          </Card>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.enrollment-progress-card,
.start-course-card,
.login-prompt-card {
  background: var(--surface-section);
}

.enrollment-progress-content {
  display: flex;
  align-items: center;
  gap: 2rem;
}

.progress-info {
  flex: 1;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.progress-meta {
  margin-top: 0.5rem;
}

.start-course-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
}

.start-course-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.login-prompt-content {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.login-prompt-actions {
  display: flex;
  gap: 0.75rem;
  margin-left: auto;
}

.module-meta {
  white-space: nowrap;
}

.module-locked {
  opacity: 0.7;
  cursor: default;
}

@media (max-width: 768px) {
  .enrollment-progress-content,
  .start-course-content,
  .login-prompt-content {
    flex-direction: column;
    align-items: stretch;
    text-align: center;
  }

  .start-course-info {
    flex-direction: column;
  }

  .login-prompt-actions {
    margin-left: 0;
    justify-content: center;
  }

  .module-meta {
    display: none;
  }
}
</style>
