<script setup lang="ts">
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
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

const handleRegister = async () => {
  if (password.value !== confirmPassword.value) {
    authStore.setError('Passwords do not match.')
    return
  }
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
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  window.location.href = `${baseUrl}/oauth2/authorization/google`
}
</script>

<template>
  <div class="flex items-center justify-center page-container-with-padding">
    <Card class="w-full max-w-md shadow-lg">
      <template #header>
        <div class="text-center p-xl">
          <h1 class="text-3xl font-bold text-primary mb-xs">Vocabee</h1>
          <p class="text-secondary">Create your account</p>
        </div>
      </template>
      <template #content>
        <form @submit.prevent="handleRegister" class="content-area">
          <Message v-if="authStore.error" severity="error">{{ authStore.error }}</Message>

          <div class="flex flex-col gap-sm">
            <label for="displayName" class="font-semibold">Display Name (Optional)</label>
            <InputText id="displayName" v-model="displayName" type="text" placeholder="Enter your display name"
                       autocomplete="name"/>
          </div>

          <div class="flex flex-col gap-sm">
            <label for="email" class="font-semibold">Email</label>
            <InputText id="email" v-model="email" type="email" placeholder="Enter your email" required
                       autocomplete="email"/>
          </div>

          <div class="flex flex-col gap-sm">
            <label for="password" class="font-semibold">Password</label>
            <Password id="password" v-model="password" placeholder="Enter your password" required toggleMask
                      autocomplete="new-password">
              <template #footer>
                <Divider />
                <p class="mt-sm text-secondary text-sm">Password must be at least 6 characters.</p>
              </template>
            </Password>
          </div>

          <div class="flex flex-col gap-sm">
            <label for="confirmPassword" class="font-semibold">Confirm Password</label>
            <Password id="confirmPassword" v-model="confirmPassword" placeholder="Confirm your password"
                      :feedback="false" required toggleMask autocomplete="new-password"/>
          </div>

          <Button type="submit" label="Sign Up" :loading="authStore.loading" class="w-full"/>

          <div class="flex items-center gap-md">
            <Divider class="flex-1"/>
            <span class="text-secondary text-sm">OR</span>
            <Divider class="flex-1"/>
          </div>

          <Button type="button" label="Sign up with Google" icon="pi pi-google" @click="handleGoogleLogin"
                  class="w-full" severity="secondary" outlined/>

          <div class="text-center text-sm text-secondary">
            Already have an account?
            <router-link to="/login" class="font-semibold text-primary hover:underline">Sign in</router-link>
          </div>
        </form>
      </template>
    </Card>
  </div>
</template>
