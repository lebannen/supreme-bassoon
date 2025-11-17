<script setup lang="ts">
  import {useRoute, useRouter} from 'vue-router'
  import {useAuthStore} from '@/stores/auth'
  import ThemeToggle from './ThemeToggle.vue'

  const router = useRouter()
  const route = useRoute()
  const authStore = useAuthStore()

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
    return route.path === path
  }

  // Placeholder streak value - you can connect this to a store/API later
  const userStreak = 7
</script>

<template>
  <aside class="sidebar">
    <router-link to="/" class="logo">Vocabee</router-link>

    <ThemeToggle/>

    <div class="nav-content">
      <!-- Main Navigation -->
      <nav class="nav-section">
        <div class="nav-section-title">Main</div>
        <ul class="nav-items">
          <li class="nav-item">
            <a href="#" :class="{ active: isActive('/') }" @click.prevent="navigateTo('/')">
              <span class="nav-icon">🏠</span>
              <span>Home</span>
            </a>
          </li>
          <li class="nav-item">
            <a
                href="#"
                :class="{ active: isActive('/courses') }"
                @click.prevent="navigateTo('/courses')"
            >
              <span class="nav-icon">📚</span>
              <span>Courses</span>
            </a>
          </li>
          <li class="nav-item">
            <a
                href="#"
                :class="{ active: isActive('/reading') }"
                @click.prevent="navigateTo('/reading')"
            >
              <span class="nav-icon">📖</span>
              <span>Reading</span>
            </a>
          </li>
          <li class="nav-item" v-if="authStore.isAuthenticated">
            <a
                href="#"
                :class="{ active: isActive('/exercises') }"
                @click.prevent="navigateTo('/exercises')"
            >
              <span class="nav-icon">✏️</span>
              <span>Exercises</span>
            </a>
          </li>
        </ul>
      </nav>

      <!-- Study Navigation -->
      <nav class="nav-section">
        <div class="nav-section-title">Study</div>
        <ul class="nav-items">
          <li class="nav-item" v-if="authStore.isAuthenticated">
            <a
                href="#"
                :class="{ active: isActive('/vocabulary') }"
                @click.prevent="navigateTo('/vocabulary')"
            >
              <span class="nav-icon">📝</span>
              <span>Vocabulary</span>
            </a>
          </li>
          <li class="nav-item" v-if="authStore.isAuthenticated">
            <a
                href="#"
                :class="{ active: isActive('/study') }"
                @click.prevent="navigateTo('/study')"
            >
              <span class="nav-icon">🎯</span>
              <span>Flashcards</span>
            </a>
          </li>
          <li class="nav-item">
            <a
                href="#"
                :class="{ active: isActive('/word-sets') }"
                @click.prevent="navigateTo('/word-sets')"
            >
              <span class="nav-icon">📋</span>
              <span>Word Sets</span>
            </a>
          </li>
          <li class="nav-item">
            <a
                href="#"
                :class="{ active: isActive('/search') }"
                @click.prevent="navigateTo('/search')"
            >
              <span class="nav-icon">🔍</span>
              <span>Dictionary</span>
            </a>
          </li>
        </ul>
      </nav>

      <!-- Admin Navigation (only if authenticated) -->
      <nav class="nav-section" v-if="authStore.isAuthenticated">
        <div class="nav-section-title">Admin</div>
        <ul class="nav-items">
          <li class="nav-item">
            <a
                href="#"
                :class="{ active: isActive('/admin/courses') }"
                @click.prevent="navigateTo('/admin/courses')"
            >
              <span class="nav-icon">⚙️</span>
              <span>Manage Courses</span>
            </a>
          </li>
          <li class="nav-item">
            <a
                href="#"
                :class="{ active: isActive('/reading/import') }"
                @click.prevent="navigateTo('/reading/import')"
            >
              <span class="nav-icon">📥</span>
              <span>Import Content</span>
            </a>
          </li>
        </ul>
      </nav>
    </div>

    <!-- User Section -->
    <div class="user-section" v-if="authStore.isAuthenticated">
      <div class="user-card" @click="navigateTo('/profile')">
        <div class="user-avatar">
          {{ getUserInitials(authStore.user?.displayName || null) }}
        </div>
        <div class="user-info">
          <div class="user-name">
            {{ authStore.user?.displayName || authStore.user?.email?.split('@')[0] }}
          </div>
          <div class="user-level">Level A1</div>
        </div>
        <span class="user-streak" :title="`${userStreak} day streak`">🔥</span>
      </div>
    </div>

    <!-- Login/Register buttons for non-authenticated users -->
    <div class="auth-section" v-else>
      <button class="btn btn-secondary btn-full" @click="navigateTo('/login')">Login</button>
      <button class="btn btn-primary btn-full" @click="navigateTo('/register')">Register</button>
    </div>
  </aside>
</template>

<style scoped>
  .sidebar {
    width: var(--sidebar-width);
    background-color: var(--bg-secondary);
    border-right: 1px solid var(--border-medium);
    padding: var(--spacing-xl);
    position: fixed;
    height: 100vh;
    overflow-y: auto;
    z-index: 100;
    display: flex;
    flex-direction: column;
  }

  .logo {
    font-size: 24px;
    font-weight: 600;
    color: var(--primary);
    text-decoration: none;
    margin-bottom: var(--spacing-2xl);
    display: block;
  }

  .logo:hover {
    color: var(--primary-dark);
  }

  .nav-content {
    flex: 1;
    overflow-y: auto;
  }

  .nav-section {
    margin-bottom: var(--spacing-xl);
  }

  .nav-section-title {
    font-size: 11px;
    font-weight: 600;
    color: var(--text-tertiary);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: var(--spacing-xs);
    padding: 0 var(--spacing-sm);
  }

  .nav-items {
    list-style: none;
    padding: 0;
    margin: 0;
  }

  .nav-item {
    margin-bottom: 2px;
  }

  .nav-item a {
    display: flex;
    align-items: center;
    gap: 10px;
    text-decoration: none;
    color: var(--text-secondary);
    font-weight: 500;
    padding: 8px var(--spacing-sm);
    border-radius: var(--radius-sm);
    transition: all 0.15s;
    font-size: 14px;
  }

  .nav-item a:hover {
    color: var(--text-primary);
    background-color: var(--bg-tertiary);
  }

  .nav-item a.active {
    color: var(--primary);
    background-color: var(--primary-light);
  }

  .nav-icon {
    width: 18px;
    height: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
  }

  /* User Section */
  .user-section {
    border-top: 1px solid var(--border-light);
    padding-top: var(--spacing-md);
    margin-top: var(--spacing-md);
  }

  .user-card {
    padding: var(--spacing-sm);
    background: var(--bg-tertiary);
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
    transition: all 0.2s;
  }

  .user-card:hover {
    background: var(--bg-hover);
  }

  .user-avatar {
    width: 36px;
    height: 36px;
    border-radius: var(--radius-full);
    background: var(--gradient-avatar);
    color: var(--text-inverse);
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: 14px;
    flex-shrink: 0;
  }

  .user-info {
    flex: 1;
    min-width: 0;
  }

  .user-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .user-level {
    font-size: 12px;
    color: var(--text-tertiary);
  }

  .user-streak {
    font-size: 16px;
    flex-shrink: 0;
  }

  /* Auth Section */
  .auth-section {
    padding-top: var(--spacing-md);
    margin-top: var(--spacing-md);
    border-top: 1px solid var(--border-light);
    display: flex;
    flex-direction: column;
    gap: var(--spacing-sm);
  }

  /* Button Styles */
  .btn {
    padding: 8px 20px;
    border-radius: var(--radius-md);
    border: none;
    font-weight: 600;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
  }

  .btn-primary {
    background-color: var(--primary);
    color: var(--text-inverse);
  }

  .btn-primary:hover {
    background-color: var(--primary-dark);
    transform: translateY(-1px);
  }

  .btn-secondary {
    background-color: var(--bg-tertiary);
    color: var(--text-secondary);
  }

  .btn-secondary:hover {
    background-color: var(--bg-hover);
  }

  .btn-full {
    width: 100%;
  }
</style>
