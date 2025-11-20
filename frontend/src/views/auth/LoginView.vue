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
const route = useRoute()
const redirectPath = (route.query.redirect as string) || '/'

const email = ref('')
const password = ref('')

const handleLogin = async () => {
  const success = await authStore.login({email: email.value, password: password.value})
  if (success) {
    router.push(redirectPath)
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
          <p class="text-secondary">Sign in to your account</p>
        </div>
      </template>
      <template #content>
        <form @submit.prevent="handleLogin" class="content-area">
          <Message v-if="route.query.expired === 'true'" severity="warn">
            Your session has expired. Please log in again.
          </Message>
          <Message v-if="authStore.error" severity="error">{{ authStore.error }}</Message>

          <div class="flex flex-col gap-sm">
            <label for="email" class="font-semibold">Email</label>
            <InputText id="email" v-model="email" type="email" placeholder="Enter your email" required
                       autocomplete="email"/>
          </div>

          <div class="flex flex-col gap-sm">
            <label for="password" class="font-semibold">Password</label>
            <Password id="password" v-model="password" placeholder="Enter your password" :feedback="false" required
                      toggleMask autocomplete="current-password"/>
          </div>

          <Button type="submit" label="Sign In" :loading="authStore.loading" class="w-full"/>

          <div class="flex items-center gap-md">
            <Divider class="flex-1"/>
            <span class="text-secondary text-sm">OR</span>
            <Divider class="flex-1"/>
          </div>

          <Button type="button" label="Sign in with Google" icon="pi pi-google" @click="handleGoogleLogin"
                  class="w-full" severity="secondary" outlined/>

          <div class="text-center text-sm text-secondary">
            Don't have an account?
            <router-link to="/register" class="font-semibold text-primary hover:underline">Sign up</router-link>
          </div>
        </form>
      </template>
    </Card>
  </div>
</template>
