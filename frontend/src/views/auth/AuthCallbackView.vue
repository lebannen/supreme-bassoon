<script setup lang="ts">
import {onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

onMounted(async () => {
  const token = route.query.token as string

  if (token) {
    // Store the token
    localStorage.setItem('auth_token', token)
    authStore.token = token

    // Fetch user data with the token
    const success = await authStore.fetchCurrentUser()

    if (success) {
      // Redirect to home page
      router.push('/')
    } else {
      // If fetching user fails, redirect to login
      router.push('/login')
    }
  } else {
    // No token provided, redirect to login
    router.push('/login')
  }
})
</script>

<template>
  <div class="page-container">
    <div class="loading-state">
      <i class="pi pi-spin pi-spinner text-2xl text-secondary"></i>
      <p class="text-base text-secondary">Completing authentication...</p>
    </div>
  </div>
</template>
