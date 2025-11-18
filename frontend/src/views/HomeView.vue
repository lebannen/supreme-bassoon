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

const greetingName = computed(() => {
  if (!authStore.user) return null
  return authStore.user.displayName || authStore.user.email?.split('@')[0] || 'there'
})

const timeBasedGreeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
})

// Landing page features for non-authenticated users
const features = [
  {
    icon: 'pi pi-book',
    title: 'Structured Courses',
    description: 'Learn French with step-by-step courses designed for all levels',
  },
  {
    icon: 'pi pi-headphones',
    title: 'Authentic Dialogues',
    description: 'Practice listening with native speaker dialogues and real-life scenarios',
  },
  {
    icon: 'pi pi-bookmark',
    title: 'Vocabulary Tracking',
    description: 'Save and review words with spaced repetition for effective learning',
  },
  {
    icon: 'pi pi-pencil',
    title: 'Interactive Exercises',
    description: 'Reinforce your learning with multiple choice, fill-in-blank, and more',
  },
]

// Load dashboard data when component mounts (for authenticated users)
onMounted(() => {
  if (authStore.isAuthenticated) {
    dashboardStore.loadDashboardData()
  }
})

const handleTaskAction = (taskId: number) => {
  console.log('Starting task:', taskId)
  // TODO: Navigate to appropriate view based on task type
  // You can use the task type to determine where to navigate
  dashboardStore.completeTask(taskId)
}

const handleContentClick = (contentId: number) => {
  console.log('Opening content:', contentId)
  // TODO: Navigate to specific content page
  router.push('/courses')
}
</script>

<template>
  <div class="home-container">
    <!-- ========== AUTHENTICATED USER: DASHBOARD ========== -->
    <div v-if="authStore.isAuthenticated" class="dashboard">
      <!-- Page Header -->
      <div class="page-header">
        <h1>{{ timeBasedGreeting }}, {{ greetingName }}!</h1>
        <p>
          Ready for your daily practice? You're on a {{ dashboardStore.userStats.streak }}-day
          streak 🔥
        </p>
      </div>

      <!-- Stats Bar -->
      <div class="stats-grid">
        <StatCard
            icon="🔥"
            label="Study Streak"
            :value="`${dashboardStore.userStats.streak} days`"
        />
        <StatCard
            icon="📚"
            label="Words Learned"
            :value="dashboardStore.userStats.wordsLearned.toString()"
        />
        <StatCard icon="⏱️" label="This Week" :value="dashboardStore.userStats.timeThisWeek"/>
      </div>

      <!-- Today's Tasks -->
      <section class="tasks-section">
        <div class="section-header">
          <h2>Today's Tasks</h2>
          <span class="badge badge-info">
            {{ dashboardStore.completedTasksCount }} of
            {{ dashboardStore.totalTasksCount }} completed
          </span>
        </div>
        <div class="task-list">
          <TaskCard
              v-for="task in dashboardStore.dailyTasks"
              :key="task.id"
              :icon="task.icon"
              :title="task.title"
              :meta="task.meta"
              :completed="task.completed"
              @action="handleTaskAction(task.id)"
          />
        </div>
      </section>

      <!-- Recommended for You -->
      <section class="recommendations-section">
        <div class="section-header">
          <h2>Recommended for You</h2>
        </div>
        <div class="content-grid">
          <ContentCard
              v-for="content in dashboardStore.recommendedContent"
              :key="content.id"
              :type="content.type"
              :icon="content.icon"
              :title="content.title"
              :level="content.level"
              :duration="content.duration"
              :topic="content.topic"
              @click="handleContentClick(content.id)"
          />
        </div>
      </section>
    </div>

    <!-- ========== NON-AUTHENTICATED USER: LANDING PAGE ========== -->
    <div v-else class="landing">
      <section class="hero-section">
        <div class="hero-content">
          <h1 class="hero-title">
            <i class="pi pi-book"></i>
            Vocabee
          </h1>
          <p class="hero-subtitle">Master French with Interactive Courses</p>
          <p class="hero-description">
            Learn French through engaging dialogues, stories, and exercises. Track your progress and
            build your vocabulary with spaced repetition.
          </p>

          <div class="hero-actions">
            <Button
                label="Start Learning"
                icon="pi pi-play"
                size="large"
                @click="router.push('/register')"
            />
            <Button
                label="Browse Courses"
                icon="pi pi-book"
                size="large"
                severity="secondary"
                outlined
                @click="router.push('/courses')"
            />
          </div>
        </div>
      </section>

      <section class="features-section">
        <div class="features-container">
          <h2 class="features-title">Why Choose Vocabee?</h2>
          <div class="features-grid">
            <Card v-for="feature in features" :key="feature.title" class="feature-card">
              <template #content>
                <div class="feature-icon">
                  <i :class="feature.icon"></i>
                </div>
                <h3 class="feature-title">{{ feature.title }}</h3>
                <p class="feature-description">{{ feature.description }}</p>
              </template>
            </Card>
          </div>
        </div>
      </section>

      <section class="cta-section">
        <div class="cta-content">
          <h2>Ready to Start Your Journey?</h2>
          <p>Join thousands of learners mastering French with Vocabee</p>
          <Button
              label="Get Started Free"
              icon="pi pi-user-plus"
              size="large"
              @click="router.push('/register')"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.home-container {
  min-height: 100%;
}

/* ========== DASHBOARD (AUTHENTICATED) ========== */
.dashboard {
  max-width: 1400px;
  margin: 0 auto;
}

.stats-grid {
  margin-bottom: var(--spacing-3xl);
}

.tasks-section {
  margin-bottom: var(--spacing-4xl);
}

.recommendations-section {
  margin-bottom: var(--spacing-4xl);
}

/* ========== LANDING PAGE (NON-AUTHENTICATED) ========== */
.landing {
  min-height: 100%;
}

.hero-section {
  background: var(--bg-secondary);
  padding: 5rem 1.5rem;
  text-align: center;
  border-bottom: 1px solid var(--border-light);
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 800;
  margin-bottom: 1.5rem;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
}

.hero-title i {
  font-size: 3rem;
  color: var(--p-primary-color);
}

.hero-subtitle {
  font-size: 1.5rem;
  color: var(--text-primary);
  margin-bottom: 1rem;
  font-weight: 600;
}

.hero-description {
  font-size: 1.1rem;
  color: var(--text-secondary);
  margin-bottom: 2.5rem;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.features-section {
  padding: 5rem 1.5rem;
  background: var(--bg-primary);
}

.features-container {
  max-width: 1200px;
  margin: 0 auto;
}

.features-title {
  text-align: center;
  font-size: 2.5rem;
  margin-bottom: 3rem;
  color: var(--text-primary);
  font-weight: 700;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
}

.feature-card {
  text-align: center;
  transition: transform 0.2s,
  box-shadow 0.2s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.feature-icon {
  font-size: 3rem;
  color: var(--p-primary-color);
  margin-bottom: 1rem;
}

.feature-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
  color: var(--text-primary);
}

.feature-description {
  color: var(--text-secondary);
  line-height: 1.6;
}

.cta-section {
  background: var(--p-primary-color);
  color: var(--p-primary-contrast-color);
  padding: 4rem 1.5rem;
  text-align: center;
}

.cta-content h2 {
  font-size: 2.5rem;
  margin-bottom: 1rem;
  font-weight: 700;
}

.cta-content p {
  font-size: 1.1rem;
  margin-bottom: 2rem;
  opacity: 0.95;
}

@media (max-width: 768px) {
  .hero-section {
    padding: 3rem 1rem;
  }

  .hero-title {
    font-size: 2.5rem;
    flex-direction: column;
    gap: 0.5rem;
  }

  .hero-subtitle {
    font-size: 1.25rem;
  }

  .features-section {
    padding: 3rem 1rem;
  }

  .features-title {
    font-size: 2rem;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .cta-section {
    padding: 3rem 1rem;
  }

  .cta-content h2 {
    font-size: 2rem;
  }
}
</style>
