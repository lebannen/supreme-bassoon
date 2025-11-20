<script setup lang="ts">
import {useRouter} from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import {useAuthStore} from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <h1>Reading Library</h1>
      <p class="text-secondary">Browse and select texts to practice reading.</p>
    </div>

    <Card>
      <template #content>
        <div class="empty-state">
          <i class="pi pi-book empty-icon"></i>
          <h3>Coming Soon</h3>
          <p class="text-secondary">The reading library is currently under development. Check back soon!</p>
          <Button v-if="authStore.isAuthenticated" label="Import Texts" icon="pi pi-upload"
                  @click="router.push('/reading/import')" class="mt-lg"/>
        </div>
      </template>
    </Card>
  </div>
</template>
