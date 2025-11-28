<script setup lang="ts">
import {onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import ProgressSpinner from 'primevue/progressspinner'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

onMounted(async () => {
  const token = route.query.token as string
  if (token) {
    localStorage.setItem('auth_token', token)
    authStore.token = token
    const success = await authStore.fetchCurrentUser()
    router.push(success ? '/' : '/login')
  } else {
    router.push('/login')
  }
})
</script>

<template>
  <div class="page-container flex align-items-center justify-content-center">
    <div class="loading-state">
      <ProgressSpinner/>
      <p class="text-secondary mt-md">Completing authentication...</p>
    </div>
  </div>
</template>
