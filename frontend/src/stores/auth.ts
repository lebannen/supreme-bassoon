import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiService } from '@/services/api'
import type { User, LoginRequest, RegisterRequest } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!token.value && !!user.value)

  // Initialize from localStorage and validate token
  async function initAuth() {
    const storedToken = localStorage.getItem('auth_token')
    const storedUser = localStorage.getItem('auth_user')

    if (storedToken && storedUser) {
      token.value = storedToken
      user.value = JSON.parse(storedUser)

      // Validate token by fetching current user
      const isValid = await fetchCurrentUser()
      if (!isValid) {
        console.warn('Stored token is invalid, logging out')
        logout()
      }
    }
  }

  async function login(credentials: LoginRequest) {
    loading.value = true
    error.value = null

    try {
      const response = await apiService.login(credentials)

      token.value = response.token
      user.value = response.user

      // Store in localStorage
      localStorage.setItem('auth_token', response.token)
      localStorage.setItem('auth_user', JSON.stringify(response.user))

      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Login failed'
      return false
    } finally {
      loading.value = false
    }
  }

  async function register(data: RegisterRequest) {
    loading.value = true
    error.value = null

    try {
      const response = await apiService.register(data)

      token.value = response.token
      user.value = response.user

      // Store in localStorage
      localStorage.setItem('auth_token', response.token)
      localStorage.setItem('auth_user', JSON.stringify(response.user))

      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Registration failed'
      return false
    } finally {
      loading.value = false
    }
  }

  async function fetchCurrentUser() {
    if (!token.value) return false

    loading.value = true
    error.value = null

    try {
      const userData = await apiService.getCurrentUser()
      user.value = userData
      localStorage.setItem('auth_user', JSON.stringify(userData))
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch user'
      logout()
      return false
    } finally {
      loading.value = false
    }
  }

  function logout() {
    token.value = null
    user.value = null
    error.value = null

    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
  }

  async function updateProfile(data: {
    displayName?: string
    nativeLanguage?: string
    learningLanguages?: string[]
  }) {
    loading.value = true
    error.value = null

    try {
      const updatedUser = await apiService.updateProfile(data)
      user.value = updatedUser
      localStorage.setItem('auth_user', JSON.stringify(updatedUser))
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to update profile'
      return false
    } finally {
      loading.value = false
    }
  }

  return {
    user,
    token,
    loading,
    error,
    isAuthenticated,
    initAuth,
    login,
    register,
    fetchCurrentUser,
    logout,
    updateProfile,
  }
})
