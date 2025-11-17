<template>
  <div class="page-container">
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

    <div class="view-container content-area-lg">
      <Card>
        <template #content>
          <div class="empty-state">
            <i class="pi pi-book text-6xl text-secondary"></i>
            <h2>Reading Library</h2>
            <p class="text-secondary">The reading library feature is currently being developed.</p>
            <p class="text-secondary">Check back soon for curated reading materials!</p>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import {useRouter} from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import {useAuthStore} from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
</script>
