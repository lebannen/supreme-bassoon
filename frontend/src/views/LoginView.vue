<template>
  <div class="flex items-center justify-center min-h-screen bg-surface-50 dark:bg-surface-950">
    <Card class="w-full max-w-md shadow-lg">
      <template #header>
        <div class="text-center pt-6">
          <h1 class="text-3xl font-bold text-primary mb-2">Vocabee</h1>
          <p class="text-surface-600 dark:text-surface-400">Sign in to your account</p>
        </div>
      </template>

      <template #content>
        <form @submit.prevent="handleLogin" class="flex flex-col gap-6">
          <Message v-if="authStore.error" severity="error" :closable="false">
            {{ authStore.error }}
          </Message>

          <div class="flex flex-col gap-2">
            <label for="email" class="font-semibold">Email</label>
            <InputText
              id="email"
              v-model="email"
              type="email"
              placeholder="Enter your email"
              :invalid="!!emailError"
              required
              autocomplete="email"
            />
            <small v-if="emailError" class="text-red-500">{{ emailError }}</small>
          </div>

          <div class="flex flex-col gap-2">
            <label for="password" class="font-semibold">Password</label>
            <Password
              id="password"
              v-model="password"
              placeholder="Enter your password"
              :feedback="false"
              :invalid="!!passwordError"
              required
              toggleMask
              autocomplete="current-password"
            />
            <small v-if="passwordError" class="text-red-500">{{ passwordError }}</small>
          </div>

          <Button
            type="submit"
            label="Sign In"
            :loading="authStore.loading"
            class="w-full"
            severity="primary"
          />

          <div class="flex items-center gap-4">
            <Divider class="flex-1" />
            <span class="text-surface-500 text-sm">OR</span>
            <Divider class="flex-1" />
          </div>

          <Button
            type="button"
            label="Sign in with Google"
            icon="pi pi-google"
            @click="handleGoogleLogin"
            class="w-full"
            severity="secondary"
            outlined
          />

          <div class="text-center text-sm text-surface-600 dark:text-surface-400">
            Don't have an account?
            <router-link to="/register" class="text-primary hover:underline font-semibold">
              Sign up
            </router-link>
          </div>
        </form>
      </template>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Divider from 'primevue/divider'

const router = useRouter()
const authStore = useAuthStore()

// Get redirect URL from query params
const route = useRoute()
const redirectPath = (route.query.redirect as string) || '/'

const email = ref('')
const password = ref('')
const emailError = ref('')
const passwordError = ref('')

const validateForm = () => {
  emailError.value = ''
  passwordError.value = ''

  if (!email.value) {
    emailError.value = 'Email is required'
    return false
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    emailError.value = 'Please enter a valid email'
    return false
  }

  if (!password.value) {
    passwordError.value = 'Password is required'
    return false
  }

  if (password.value.length < 6) {
    passwordError.value = 'Password must be at least 6 characters'
    return false
  }

  return true
}

const handleLogin = async () => {
  if (!validateForm()) return

  const success = await authStore.login({
    email: email.value,
    password: password.value,
  })

  if (success) {
    router.push(redirectPath)
  }
}

const handleGoogleLogin = () => {
  // Redirect to backend OAuth2 endpoint
  window.location.href = 'http://localhost:8080/oauth2/authorization/google'
}
</script>

<style scoped>
.flex {
  display: flex;
}

.flex-col {
  flex-direction: column;
}

.items-center {
  align-items: center;
}

.justify-center {
  justify-content: center;
}

.min-h-screen {
  min-height: 100vh;
}

.w-full {
  width: 100%;
}

.max-w-md {
  max-width: 28rem;
}

.gap-2 {
  gap: 0.5rem;
}

.gap-4 {
  gap: 1rem;
}

.gap-6 {
  gap: 1.5rem;
}

.shadow-lg {
  box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1);
}

.text-center {
  text-align: center;
}

.text-3xl {
  font-size: 1.875rem;
  line-height: 2.25rem;
}

.text-sm {
  font-size: 0.875rem;
  line-height: 1.25rem;
}

.font-bold {
  font-weight: 700;
}

.font-semibold {
  font-weight: 600;
}

.mb-2 {
  margin-bottom: 0.5rem;
}

.pt-6 {
  padding-top: 1.5rem;
}

.flex-1 {
  flex: 1 1 0%;
}
</style>
