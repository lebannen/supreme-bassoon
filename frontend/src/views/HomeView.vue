<script setup lang="ts">
import {useRouter} from 'vue-router'
import {computed, onMounted} from 'vue'
import Button from 'primevue/button'
import Card from 'primevue/card'
import {useAuthStore} from '@/stores/auth'
import {useDashboardStore} from '@/stores/dashboard'
import StatCard from '@/components/ui/StatCard.vue'
import TaskCard from '@/components/ui/TaskCard.vue'
import ContentCard from '@/components/ui/ContentCard.vue'

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

const handleTaskAction = (taskId: number) => {
  dashboardStore.completeTask(taskId)
}
</script>

<template>
  <div class="home-view">
    <!-- Authenticated Dashboard -->
    <div v-if="authStore.isAuthenticated" class="view-container content-area-lg">
      <div class="page-header">
        <h1>{{ timeBasedGreeting }}, {{ greetingName }}!</h1>
        <p class="text-secondary">You're on a {{ dashboardStore.userStats.streak }}-day streak 🔥 Keep it up!</p>
      </div>

      <div class="stats-grid">
        <StatCard icon="pi pi-bolt" label="Study Streak" :value="`${dashboardStore.userStats.streak} days`"
                  variant="warning"/>
        <StatCard icon="pi pi-book" label="Words Learned" :value="dashboardStore.userStats.wordsLearned"
                  variant="purple"/>
        <StatCard icon="pi pi-clock" label="Time This Week" :value="dashboardStore.userStats.timeThisWeek"
                  variant="blue"/>
      </div>

      <section class="section">
        <div class="section-header">
          <h2>Today's Tasks</h2>
        </div>
        <div class="task-list">
          <TaskCard v-for="task in dashboardStore.dailyTasks" :key="task.id" :icon="task.icon" :title="task.title"
                    :meta="task.meta" :completed="task.completed" @action="handleTaskAction(task.id)"/>
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h2>Recommended for You</h2>
        </div>
        <div class="content-grid">
          <ContentCard v-for="content in dashboardStore.recommendedContent" :key="content.id" :type="content.type"
                       :icon="content.icon" :title="content.title" :level="content.level"
                       @click="router.push('/courses')"/>
        </div>
      </section>
    </div>

    <!-- Public Landing Page -->
    <div v-else class="landing-page">
      <section class="hero-section text-center">
        <div class="view-container">
          <h1 class="text-4xl font-bold text-primary mb-md">Welcome to Vocabee</h1>
          <p class="text-xl text-secondary mb-2xl">Master French through engaging dialogues, stories, and exercises.</p>
          <div class="flex justify-center gap-md">
            <Button label="Start Learning" size="large" @click="router.push('/register')"/>
            <Button label="Browse Courses" size="large" severity="secondary" @click="router.push('/courses')"/>
          </div>
        </div>
      </section>

      <section class="p-4xl">
        <div class="view-container">
          <h2 class="text-3xl font-bold text-center mb-3xl">Why Choose Vocabee?</h2>
          <div class="content-grid">
            <Card v-for="feature in features" :key="feature.title" class="text-center p-lg card-interactive">
              <template #content>
                <i :class="feature.icon" class="text-4xl text-primary mb-lg"></i>
                <h3 class="text-xl font-bold mb-sm">{{ feature.title }}</h3>
                <p class="text-secondary m-0">{{ feature.description }}</p>
              </template>
            </Card>
          </div>
        </div>
      </section>

      <section class="bg-primary text-center p-4xl">
        <div class="view-container">
          <h2 class="text-3xl font-bold mb-md">Ready to Start Your Journey?</h2>
          <p class="text-xl opacity-80 mb-2xl">Join thousands of learners mastering French with Vocabee.</p>
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
</style>
