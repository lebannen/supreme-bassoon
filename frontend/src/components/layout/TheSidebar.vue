<script setup lang="ts">
  import {useRoute, useRouter} from 'vue-router'
  import {useAuthStore} from '@/stores/auth'
  import {useProgressStore} from '@/stores/progress'
  import {storeToRefs} from 'pinia'
  import ThemeToggle from './ThemeToggle.vue'
  import Button from 'primevue/button'

  const router = useRouter()
  const route = useRoute()
  const authStore = useAuthStore()
  const progressStore = useProgressStore()

  const {userStats, completedGoalsCount, dailyGoals} = storeToRefs(progressStore)

  const getUserInitials = (name: string | null) => {
    if (!name) return 'U'
    return name
        .split(' ')
        .map((n) => n[0])
        .join('')
        .toUpperCase()
        .substring(0, 2)
  }

  const navigateTo = (path: string) => {
    router.push(path)
  }

  const isActive = (path: string) => {
    if (path === '/') {
      return route.path === '/'
    }
    return route.path.startsWith(path)
  }

  const mainNav = [
    {icon: 'pi pi-home', label: 'Home', path: '/'},
    {icon: 'pi pi-sitemap', label: 'Courses', path: '/courses'},
    {icon: 'pi pi-book', label: 'Reading', path: '/reading'},
    {icon: 'pi pi-pencil', label: 'Exercises', path: '/exercises', auth: true},
  ]

  const studyNav = [
    {icon: 'pi pi-palette', label: 'Playground', path: '/playground'},
    {icon: 'pi pi-list', label: 'Vocabulary', path: '/vocabulary', auth: true},
    {icon: 'pi pi-bolt', label: 'Flashcards', path: '/study', auth: true},
    {icon: 'pi pi-clone', label: 'Word Sets', path: '/word-sets'},
    {icon: 'pi pi-search', label: 'Dictionary', path: '/search'},
  ]

  const adminNav = [
    {icon: 'pi pi-cog', label: 'Manage Courses', path: '/admin/courses', auth: true},
    {icon: 'pi pi-sitemap', label: 'Generation Pipeline', path: '/admin/pipeline', auth: true},
    {icon: 'pi pi-sparkles', label: 'Course Wizard', path: '/admin/wizard', auth: true},
    {icon: 'pi pi-book', label: 'Reading Admin', path: '/reading/admin', auth: true},
  ]
</script>

<template>
  <aside class="sidebar bg-surface-section border-r border-surface">
    <router-link to="/" class="logo text-primary">Vocabee</router-link>

    <ThemeToggle class="mb-4"/>

    <!-- Mini Stats (Authenticated Users) -->
    <div v-if="authStore.isAuthenticated" class="mini-stats mb-5">
      <div class="mini-stat">
        <i class="pi pi-bolt text-warning"></i>
        <span class="mini-stat-value">{{ userStats.streak }}</span>
        <span class="mini-stat-label">Streak</span>
      </div>
      <div class="mini-stat">
        <i class="pi pi-check-circle text-success"></i>
        <span class="mini-stat-value">{{ completedGoalsCount }}/{{ dailyGoals.length }}</span>
        <span class="mini-stat-label">Goals</span>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto">
      <!-- Main Navigation -->
      <nav class="nav-section">
        <div class="nav-section-title text-secondary">Main</div>
        <ul class="nav-items">
          <li v-for="item in mainNav.filter(i => !i.auth || authStore.isAuthenticated)" :key="item.path">
            <a :href="item.path" @click.prevent="navigateTo(item.path)"
               class="nav-link" :class="{ 'active-link': isActive(item.path) }">
              <i :class="item.icon"></i>
              <span>{{ item.label }}</span>
            </a>
          </li>
        </ul>
      </nav>

      <!-- Study Navigation -->
      <nav class="nav-section">
        <div class="nav-section-title text-secondary">Study</div>
        <ul class="nav-items">
          <li v-for="item in studyNav.filter(i => !i.auth || authStore.isAuthenticated)" :key="item.path">
            <a :href="item.path" @click.prevent="navigateTo(item.path)"
               class="nav-link" :class="{ 'active-link': isActive(item.path) }">
              <i :class="item.icon"></i>
              <span>{{ item.label }}</span>
            </a>
          </li>
        </ul>
      </nav>

      <!-- Admin Navigation -->
      <nav class="nav-section" v-if="authStore.isAuthenticated">
        <div class="nav-section-title text-secondary">Admin</div>
        <ul class="nav-items">
          <li v-for="item in adminNav" :key="item.path">
            <a :href="item.path" @click.prevent="navigateTo(item.path)"
               class="nav-link" :class="{ 'active-link': isActive(item.path) }">
              <i :class="item.icon"></i>
              <span>{{ item.label }}</span>
            </a>
          </li>
        </ul>
      </nav>
    </div>

    <!-- User Section -->
    <div v-if="authStore.isAuthenticated" class="mt-auto pt-md border-t border-surface">
      <div @click="navigateTo('/profile')"
           class="p-sm rounded-md flex align-items-center gap-2 cursor-pointer hover:bg-surface-hover">
        <div class="user-avatar text-white">
          {{ getUserInitials(authStore.user?.displayName || null) }}
        </div>
        <div class="flex-1 min-w-0">
          <div class="font-semibold text-primary truncate">{{ authStore.user?.displayName || 'User' }}</div>
          <div class="text-sm text-secondary">View Profile</div>
        </div>
        <i class="pi pi-chevron-right text-secondary"></i>
      </div>
    </div>

    <!-- Login/Register buttons -->
    <div v-else class="mt-auto pt-md border-t border-surface flex flex-column gap-2">
      <Button label="Login" @click="navigateTo('/login')" severity="secondary" outlined class="w-full"/>
      <Button label="Register" @click="navigateTo('/register')" class="w-full"/>
    </div>
  </aside>
</template>

<style scoped>
  .sidebar {
    width: var(--sidebar-width);
    padding: var(--spacing-xl);
    position: fixed;
    height: 100vh;
    display: flex;
    flex-direction: column;
    z-index: 1000;
  }

  .logo {
    font-size: 1.75rem;
    font-weight: 700;
    text-decoration: none;
    margin-bottom: var(--spacing-2xl);
  }

  .nav-section {
    margin-bottom: var(--spacing-xl);
  }

  .nav-section-title {
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    padding: 0 var(--spacing-sm) var(--spacing-xs);
  }

  .nav-items {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .nav-link {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
    text-decoration: none;
    font-weight: 500;
    padding: 10px var(--spacing-sm);
    border-radius: var(--radius-md);
    transition: all 0.2s;
    font-size: 0.9rem;
    color: var(--text-color-secondary);
  }

  .nav-link:hover {
    background-color: var(--surface-hover);
    color: var(--text-color);
  }

  .nav-link.active-link {
    background-color: var(--p-primary-50);
    color: var(--p-primary-600);
    font-weight: 600;
  }

  .dark-theme .nav-link.active-link {
    background-color: var(--p-primary-900);
    color: var(--p-primary-50);
  }

  .nav-link i {
    font-size: 1.1rem;
    width: 20px;
    text-align: center;
  }

  .user-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: var(--gradient-avatar);
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: 0.9rem;
    flex-shrink: 0;
  }

  /* Mini Stats */
  .mini-stats {
    display: flex;
    gap: var(--spacing-md);
    padding: var(--spacing-sm);
    background: var(--surface-ground);
    border-radius: var(--radius-md);
  }

  .mini-stat {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    text-align: center;
  }

  .mini-stat i {
    font-size: 1.25rem;
  }

  .mini-stat-value {
    font-size: 1rem;
    font-weight: 700;
    color: var(--text-color);
  }

  .mini-stat-label {
    font-size: 0.65rem;
    text-transform: uppercase;
    letter-spacing: 0.3px;
    color: var(--text-color-secondary);
  }
</style>
