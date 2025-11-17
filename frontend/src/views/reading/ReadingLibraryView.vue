<template>
  <div class="reading-library-view">
    <div class="view-header">
      <div class="view-header-content">
        <div class="view-header-text">
          <h1>Reading Texts</h1>
          <p>Browse and select texts to practice reading in your target language</p>
        </div>
        <Button
          v-if="authStore.isAuthenticated"
          label="Import Texts"
          icon="pi pi-upload"
          @click="router.push('/reading/import')"
          outlined
        />
      </div>
    </div>

    <TextLibrary />
  </div>
</template>

<script setup lang="ts">
import {useRouter} from 'vue-router'
import Button from 'primevue/button'
import TextLibrary from '@/components/reading/TextLibrary.vue'
import {useAuthStore} from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
</script>

<style scoped>
.reading-library-view {
  min-height: 100vh;
  background: var(--surface-ground);
}
</style>
