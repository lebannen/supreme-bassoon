<template>
  <div class="flex items-center justify-center auth-container">
    <Card class="auth-card">
      <template #header>
        <div class="text-center p-xl">
          <h1 class="text-3xl font-bold mb-xs text-primary">Vocabee</h1>
          <p class="text-secondary">Sign in to your account</p>
        </div>
      </template>

      <template #content>
        <form @submit.prevent="handleLogin" class="content-area">
          <Message v-if="route.query.expired === 'true'" severity="warn" :closable="true">
            Your session has expired. Please log in again.
          </Message>

          <Message v-if="authStore.error" severity="error" :closable="false">
            {{ authStore.error }}
          </Message>

          <div class="flex flex-col gap-xs">
            <label for="email" class="font-semibold text-primary">Email</label>
            <InputText
              id="email"
              v-model="email"
              type="email"
              placeholder="Enter your email"
              :invalid="!!emailError"
              required
              autocomplete="email"
            />
            <small v-if="emailError" class="text-error">{{ emailError }}</small>
          </div>

          <div class="flex flex-col gap-xs">
            <label for="password" class="font-semibold text-primary">Password</label>
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
            <small v-if="passwordError" class="text-error">{{ passwordError }}</small>
          </div>

          <Button
            type="submit"
            label="Sign In"
            :loading="authStore.loading"
            class="full-width"
            severity="primary"
          />

          <div class="flex items-center gap-md">
            <Divider class="flex-1" />
            <span class="text-secondary text-sm">OR</span>
            <Divider class="flex-1" />
          </div>

          <Button
            type="button"
            label="Sign in with Google"
            icon="pi pi-google"
            @click="handleGoogleLogin"
            class="full-width"
            severity="secondary"
            outlined
          />

          <div class="text-center text-sm text-secondary">
            Don't have an account?
            <router-link to="/register" class="link-primary font-semibold"> Sign up</router-link>
          </div>
        </form>
      </template>
    </Card>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
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
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  window.location.href = `${baseUrl}/oauth2/authorization/google`
}
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  background: var(--bg-tertiary);
}

.auth-card {
  width: 100%;
  max-width: 28rem;
  box-shadow: var(--shadow-lg);
}

.full-width {
  width: 100%;
}

.text-primary {
  color: var(--text-primary);
}

.text-error {
  color: var(--error);
}

.link-primary {
  color: var(--primary);
  text-decoration: none;
}

.link-primary:hover {
  text-decoration: underline;
}
</style>
