import { useAuthStore } from '@/stores/auth'
import router from '@/router'

export function setupApiInterceptor() {
  // Intercept fetch requests globally
  const originalFetch = window.fetch

  window.fetch = async function (...args) {
    const response = await originalFetch(...args)

    // If we get a 401 Unauthorized, automatically log out
    if (response.status === 401) {
      const authStore = useAuthStore()

      // Only log out if we're currently authenticated
      // (to avoid logging out repeatedly)
      if (authStore.isAuthenticated) {
        console.warn('Received 401 Unauthorized - token is invalid or expired, logging out')
        authStore.logout()

        // Redirect to login page
        router.push({
          name: 'login',
          query: {
            redirect: router.currentRoute.value.fullPath,
            expired: 'true'
          }
        })
      }
    }

    return response
  }
}
