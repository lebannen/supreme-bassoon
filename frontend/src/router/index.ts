import {createRouter, createWebHistory} from 'vue-router'
import HomeView from '../views/HomeView.vue'
import {useAuthStore} from '@/stores/auth'

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
      component: () => import('../views/auth/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/auth/RegisterView.vue'),
    },
    {
      path: '/auth/callback',
      name: 'auth-callback',
      component: () => import('../views/auth/AuthCallbackView.vue'),
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/auth/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/vocabulary',
      name: 'vocabulary',
      component: () => import('../views/vocabulary/VocabularyView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/search',
      name: 'search',
      component: () => import('../views/vocabulary/SearchView.vue'),
    },
    {
      path: '/import',
      name: 'import',
      component: () => import('../views/admin/ImportView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/word-sets',
      name: 'word-sets',
      component: () => import('../views/vocabulary/WordSetsView.vue'),
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
      path: '/playground',
      name: 'playground',
      component: () => import('../views/PlaygroundView.vue'),
    },
    {
      path: '/book',
      name: 'book',
      component: () => import('../views/reading/BookView.vue'),
    },
    {
      path: '/reading',
      name: 'reading-library',
      component: () => import('../views/reading/ReadingLibraryView.vue'),
    },
    {
      path: '/reading/:id',
      name: 'ReadingText',
      component: () => import('../views/reading/BookView.vue'),
      props: true,
    },
    {
      path: '/reading/import',
      name: 'reading-import',
      component: () => import('../views/admin/ReadingImportView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/reading/admin',
      name: 'reading-admin',
      component: () => import('../views/admin/ReadingAdminView.vue'),
    },
    {
      path: '/study',
      name: 'study',
      component: () => import('../views/study/StudyHomeView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/study/session',
      name: 'study-session',
      component: () => import('../views/study/StudySessionView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/exercises',
      name: 'exercises',
      component: () => import('../views/exercises/ExercisesView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/modules',
      name: 'modules',
      component: () => import('../views/exercises/ModuleView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/exercises/:id',
      name: 'exercise-detail',
      component: () => import('../views/exercises/ExerciseDetailView.vue'),
      props: true,
      meta: { requiresAuth: true },
    },
    {
      path: '/stats',
      name: 'stats',
      component: () => import('../views/exercises/StatsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/audio-test',
      name: 'audio-test',
      component: () => import('../views/admin/AudioTestView.vue'),
      meta: { requiresAuth: true },
    },
    {
        path: '/admin/speaking-test',
        name: 'speaking-test',
        component: () => import('../views/admin/SpeakingTestView.vue'),
        meta: {requiresAuth: true},
    },
      {
      path: '/admin/exercise-import',
      name: 'exercise-import',
      component: () => import('../views/admin/ExerciseImportView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/courses/:id',
      name: 'course-dashboard',
      component: () => import('../views/admin/CourseDashboardView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/courses',
      name: 'course-admin',
      component: () => import('../views/admin/CourseAdminView.vue'),
      meta: { requiresAuth: true }, // TODO: Add role-based access control later
    },
    {
      path: '/admin/wizard',
      name: 'course-wizard',
      component: () => import('../views/admin/creator/CourseWizardView.vue'),
      meta: { requiresAuth: true },
    },
    {
        path: '/admin/courses/:courseId/modules/:moduleId/episodes/generate',
        name: 'episode-wizard',
        component: () => import('../views/admin/creator/EpisodeWizardView.vue'),
      meta: { requiresAuth: true },
    },
      {
          path: '/admin/courses/:courseId/episodes/:episodeId/view',
          name: 'episode-viewer',
          component: () => import('../views/admin/creator/EpisodeViewerView.vue'),
          meta: {requiresAuth: true},
      },
      {
          path: '/admin/courses/:courseId/import',
          name: 'course-import',
          component: () => import('../views/admin/creator/CourseImportView.vue'),
          meta: {requiresAuth: true},
      },
      {
          path: '/admin/courses/:courseId/voices',
          name: 'voice-assignment',
          component: () => import('../views/admin/creator/VoiceAssignmentView.vue'),
          meta: {requiresAuth: true}
      },
      // Pipeline Routes
      {
          path: '/admin/pipeline',
          name: 'pipeline-list',
          component: () => import('../views/admin/pipeline/PipelineListView.vue'),
          meta: {requiresAuth: true}
      },
      {
          path: '/admin/pipeline/:id',
          name: 'pipeline-detail',
          component: () => import('../views/admin/pipeline/PipelineDetailView.vue'),
          meta: {requiresAuth: true}
      },
      {
          path: '/admin/pipeline/:id/debug',
          name: 'pipeline-debug',
          component: () => import('../views/admin/pipeline/PipelineDebugView.vue'),
          meta: {requiresAuth: true}
      },
    // Course Structure Routes
    {
      path: '/courses',
      name: 'courses',
      component: () => import('../views/courses/CoursesView.vue'),
    },
    {
      path: '/courses/:slug',
      name: 'course-detail',
      component: () => import('../views/courses/CourseDetailView.vue'),
    },
    {
      path: '/course-modules/:id',
      name: 'module-detail',
      component: () => import('../views/courses/ModuleDetailView.vue'),
    },
    {
      path: '/episodes/:id',
      name: 'episode',
      component: () => import('../views/courses/EpisodeView.vue'),
      meta: { requiresAuth: true },
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
      query: { redirect: to.fullPath }, // Save the intended destination
    })
  } else if ((to.name === 'login' || to.name === 'register') && authStore.isAuthenticated) {
    // If already logged in, redirect to home
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
