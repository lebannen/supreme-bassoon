<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

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
  <div class="callback-container">
    <div class="loading">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem"></i>
      <p>Completing authentication...</p>
    </div>
  </div>
</template>

<style scoped>
.callback-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
}

.loading {
  text-align: center;
}

.loading p {
  margin-top: 1rem;
  color: var(--text-color-secondary);
}
</style>
