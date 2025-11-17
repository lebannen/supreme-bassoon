<script setup lang="ts">
import {RouterView} from 'vue-router'
import ConfirmDialog from 'primevue/confirmdialog'
import TheSidebar from './components/layout/TheSidebar.vue'
import {useAuthStore} from './stores/auth'

// Initialize auth store on app mount
const authStore = useAuthStore()
authStore.initAuth()
</script>

<template>
  <ConfirmDialog />
  <div class="app-container">
    <TheSidebar/>

    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  min-height: 100vh;
  background-color: var(--bg-primary);
}

.main-content {
  margin-left: var(--sidebar-width);
  flex: 1;
  padding: var(--spacing-3xl) var(--spacing-3xl) 80px;
  max-width: calc(100vw - var(--sidebar-width));
  background-color: var(--bg-primary);
  min-height: 100vh;
}

/* Responsive: Stack sidebar on small screens (optional - can add later) */
@media (max-width: 768px) {
  .main-content {
    margin-left: 0;
    max-width: 100vw;
    padding: var(--spacing-lg);
  }
}
</style>
