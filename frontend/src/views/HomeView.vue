<script setup lang="ts">
import {useRouter} from 'vue-router'
import {computed, onMounted} from 'vue'
import Button from 'primevue/button'
import Card from 'primevue/card'
import {useAuthStore} from '@/stores/auth'
import {useDashboardStore} from '@/stores/dashboard'
import StatCard from '@/components/ui/StatCard.vue'
import DailyGoalsCard from '@/components/ui/DailyGoalsCard.vue'
import ContinueLearningCard from '@/components/ui/ContinueLearningCard.vue'
import EnrollmentCard from '@/components/ui/EnrollmentCard.vue'

const router = useRouter()
const authStore = useAuthStore()
const dashboardStore = useDashboardStore()

const greetingName = computed(() => authStore.user?.displayName || authStore.user?.email?.split('@')[0] || 'there')
const timeBasedGreeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
})

const streakMessage = computed(() => {
  const streak = dashboardStore.userStats.streak
  if (streak === 0) return 'Start your streak today!'
  if (streak === 1) return "You're on a 1-day streak! Keep going!"
  return `You're on a ${streak}-day streak! Keep it up!`
})

const features = [
  {icon: 'pi pi-sitemap', title: 'Structured Courses', description: 'Learn with step-by-step courses for all levels.'},
  {
    icon: 'pi pi-megaphone',
    title: 'Authentic Dialogues',
    description: 'Practice listening with native speaker dialogues.'
  },
  {icon: 'pi pi-star', title: 'Vocabulary Mastery', description: 'Review words with spaced repetition.'},
  {icon: 'pi pi-pencil', title: 'Interactive Exercises', description: 'Reinforce learning with varied exercises.'},
]

onMounted(() => {
  if (authStore.isAuthenticated) {
    dashboardStore.loadDashboardData()
  }
})
</script>

<template>
  <div class="home-view">
    <!-- Authenticated Dashboard -->
    <div v-if="authStore.isAuthenticated" class="view-container content-area-lg">
      <div class="page-header">
        <h1>{{ timeBasedGreeting }}, {{ greetingName }}!</h1>
        <p class="text-secondary">{{ streakMessage }}</p>
      </div>

      <!-- Continue Learning (if enrolled in courses) -->
      <ContinueLearningCard
          v-if="dashboardStore.continueLearning"
          :data="dashboardStore.continueLearning"
      />

      <!-- Stats Grid -->
      <div class="stats-grid">
        <StatCard
            icon="pi pi-bolt"
            label="Study Streak"
            :value="`${dashboardStore.userStats.streak} days`"
            variant="warning"
        />
        <StatCard
            icon="pi pi-book"
            label="Words Learned"
            :value="dashboardStore.userStats.wordsLearned"
            variant="purple"
        />
        <StatCard
            icon="pi pi-clock"
            label="Time This Week"
            :value="dashboardStore.userStats.timeThisWeek"
            variant="blue"
        />
      </div>

      <!-- Daily Goals -->
      <section class="section">
        <DailyGoalsCard :goals="dashboardStore.dailyGoals"/>
      </section>

      <!-- My Courses -->
      <section v-if="dashboardStore.hasActiveEnrollments" class="section">
        <div class="section-header">
          <h2>My Courses</h2>
          <Button
              label="Browse More"
              icon="pi pi-plus"
              text
              @click="router.push('/courses')"
          />
        </div>
        <div class="content-grid">
          <EnrollmentCard
              v-for="enrollment in dashboardStore.enrollments"
              :key="enrollment.id"
              :enrollment="enrollment"
          />
        </div>
      </section>

      <!-- Empty State - No Enrollments -->
      <section v-else class="section">
        <Card class="empty-courses-card">
          <template #content>
            <div class="empty-state">
              <i class="pi pi-book empty-icon text-primary"></i>
              <h3>Start Your Learning Journey</h3>
              <p class="text-secondary mb-4">
                Enroll in a course to begin learning and track your progress.
              </p>
              <Button
                  label="Browse Courses"
                  icon="pi pi-arrow-right"
                  iconPos="right"
                  @click="router.push('/courses')"
              />
            </div>
          </template>
        </Card>
      </section>
    </div>

    <!-- Public Landing Page -->
    <div v-else class="landing-page">
      <section class="hero-section text-center">
        <div class="view-container">
          <h1 class="text-4xl font-bold text-primary mb-3">Welcome to Vocabee</h1>
          <p class="text-xl text-secondary mb-6">Master French through engaging dialogues, stories, and exercises.</p>
          <div class="flex justify-content-center gap-3">
            <Button label="Start Learning" size="large" @click="router.push('/register')"/>
            <Button label="Browse Courses" size="large" severity="secondary" @click="router.push('/courses')"/>
          </div>
        </div>
      </section>

      <section class="p-8">
        <div class="view-container">
          <h2 class="text-3xl font-bold text-center mb-7">Why Choose Vocabee?</h2>
          <div class="content-grid">
            <Card v-for="feature in features" :key="feature.title" class="text-center p-4 card-interactive">
              <template #content>
                <i :class="feature.icon" class="text-4xl text-primary mb-4"></i>
                <h3 class="text-xl font-bold mb-2">{{ feature.title }}</h3>
                <p class="text-secondary m-0">{{ feature.description }}</p>
              </template>
            </Card>
          </div>
        </div>
      </section>

      <section class="bg-primary text-center p-8">
        <div class="view-container">
          <h2 class="text-3xl font-bold mb-3">Ready to Start Your Journey?</h2>
          <p class="text-xl opacity-80 mb-6">Join thousands of learners mastering French with Vocabee.</p>
          <Button label="Get Started for Free" size="large" severity="contrast" @click="router.push('/register')"/>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.hero-section {
  padding: 6rem 1.5rem;
  background-color: var(--surface-section);
}

.empty-courses-card {
  text-align: center;
}
</style>
