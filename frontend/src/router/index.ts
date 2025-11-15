import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
    },
    {
      path: '/auth/callback',
      name: 'auth-callback',
      component: () => import('../views/AuthCallbackView.vue'),
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/vocabulary',
      name: 'vocabulary',
      component: () => import('../views/VocabularyView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/search',
      name: 'search',
      component: () => import('../views/SearchView.vue'),
    },
    {
      path: '/import',
      name: 'import',
      component: () => import('../views/ImportView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/word-sets',
      name: 'word-sets',
      component: () => import('../views/WordSetsView.vue'),
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/book',
      name: 'book',
      component: () => import('../views/BookView.vue'),
    },
    {
      path: '/reading',
      name: 'reading-library',
      component: () => import('../views/ReadingLibraryView.vue'),
    },
    {
      path: '/reading/:id',
      name: 'ReadingText',
      component: () => import('../views/BookView.vue'),
      props: true
    },
    {
      path: '/reading/import',
      name: 'reading-import',
      component: () => import('../views/ReadingImportView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/reading/admin',
      name: 'reading-admin',
      component: () => import('../views/ReadingAdminView.vue'),
    },
    {
      path: '/study',
      name: 'study',
      component: () => import('../views/StudyHomeView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/study/session',
      name: 'study-session',
      component: () => import('../views/StudySessionView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/exercises',
      name: 'exercises',
      component: () => import('../views/ExercisesView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/modules',
      name: 'modules',
      component: () => import('../views/ModuleView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/exercises/:id',
      name: 'exercise-detail',
      component: () => import('../views/ExerciseDetailView.vue'),
      props: true,
      meta: { requiresAuth: true }
    },
    {
      path: '/admin/audio-test',
      name: 'audio-test',
      component: () => import('../views/AudioTestView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin/exercise-import',
      name: 'exercise-import',
      component: () => import('../views/ExerciseImportView.vue'),
      meta: { requiresAuth: true }
    },
  ],
})

// Navigation guard for authentication
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // Check if the route requires authentication
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    // Redirect to login page
    next({
      name: 'login',
      query: { redirect: to.fullPath } // Save the intended destination
    })
  } else if ((to.name === 'login' || to.name === 'register') && authStore.isAuthenticated) {
    // If already logged in, redirect to home
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
