<template>
  <div class="flex items-center justify-center auth-container">
    <Card class="auth-card">
      <template #header>
        <div class="text-center p-xl">
          <h1 class="text-3xl font-bold mb-xs text-primary">Vocabee</h1>
          <p class="text-secondary">Create your account</p>
        </div>
      </template>

      <template #content>
        <form @submit.prevent="handleRegister" class="content-area">
          <Message v-if="authStore.error" severity="error" :closable="false">
            {{ authStore.error }}
          </Message>

          <div class="flex flex-col gap-xs">
            <label for="displayName" class="font-semibold text-primary">
              Display Name (Optional)
            </label>
            <InputText
              id="displayName"
              v-model="displayName"
              type="text"
              placeholder="Enter your display name"
              autocomplete="name"
            />
          </div>

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
              :invalid="!!passwordError"
              required
              toggleMask
              autocomplete="new-password"
            >
              <template #footer>
                <Divider />
                <ul class="requirements-list">
                  <li class="text-sm">At least 6 characters</li>
                </ul>
              </template>
            </Password>
            <small v-if="passwordError" class="text-error">{{ passwordError }}</small>
          </div>

          <div class="flex flex-col gap-xs">
            <label for="confirmPassword" class="font-semibold text-primary">
              Confirm Password
            </label>
            <Password
              id="confirmPassword"
              v-model="confirmPassword"
              placeholder="Confirm your password"
              :feedback="false"
              :invalid="!!confirmPasswordError"
              required
              toggleMask
              autocomplete="new-password"
            />
            <small v-if="confirmPasswordError" class="text-error">{{
              confirmPasswordError
            }}</small>
          </div>

          <Button
            type="submit"
            label="Sign Up"
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
            label="Sign up with Google"
            icon="pi pi-google"
            @click="handleGoogleLogin"
            class="full-width"
            severity="secondary"
            outlined
          />

          <div class="text-center text-sm text-secondary">
            Already have an account?
            <router-link to="/login" class="link-primary font-semibold">
              Sign in
            </router-link>
          </div>
        </form>
      </template>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Divider from 'primevue/divider'

const router = useRouter()
const authStore = useAuthStore()

const displayName = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const emailError = ref('')
const passwordError = ref('')
const confirmPasswordError = ref('')

const validateForm = () => {
  emailError.value = ''
  passwordError.value = ''
  confirmPasswordError.value = ''

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

  if (!confirmPassword.value) {
    confirmPasswordError.value = 'Please confirm your password'
    return false
  }

  if (password.value !== confirmPassword.value) {
    confirmPasswordError.value = 'Passwords do not match'
    return false
  }

  return true
}

const handleRegister = async () => {
  if (!validateForm()) return

  const success = await authStore.register({
    email: email.value,
    password: password.value,
    displayName: displayName.value || undefined,
  })

  if (success) {
    router.push('/')
  }
}

const handleGoogleLogin = () => {
  // Redirect to backend OAuth2 endpoint
  window.location.href = 'http://localhost:8080/oauth2/authorization/google'
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

.requirements-list {
  padding-left: var(--spacing-xs);
  margin-left: var(--spacing-xs);
  margin: 0;
  line-height: 1.5;
}
</style>
