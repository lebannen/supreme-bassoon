<template>
  <div class="reading-library-view">
    <div class="header">
      <div class="header-content">
        <div class="header-text">
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
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import TextLibrary from '@/components/reading/TextLibrary.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
</script>

<style scoped>
.reading-library-view {
  min-height: 100vh;
  background: var(--surface-ground);
}

.header {
  background: var(--surface-card);
  border-bottom: 1px solid var(--surface-border);
  padding: 2rem;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.header-text {
  flex: 1;
  text-align: center;
}

.header-text h1 {
  margin: 0 0 0.5rem 0;
  color: var(--text-color);
  font-size: 2rem;
  font-weight: 600;
}

.header-text p {
  margin: 0;
  color: var(--text-color-secondary);
  font-size: 1.125rem;
}

@media (max-width: 768px) {
  .header {
    padding: 1.5rem 1rem;
  }

  .header-content {
    flex-direction: column;
    gap: 1rem;
  }

  .header-text h1 {
    font-size: 1.5rem;
  }

  .header-text p {
    font-size: 1rem;
  }
}
</style>
