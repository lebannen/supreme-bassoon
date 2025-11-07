<script setup lang="ts">
import { RouterView, useRouter } from 'vue-router'
import { ref } from 'vue'
import Menubar from 'primevue/menubar'
import ConfirmDialog from 'primevue/confirmdialog'

const router = useRouter()

const items = ref([
  {
    label: 'Home',
    icon: 'pi pi-home',
    command: () => router.push('/')
  },
  {
    label: 'Search',
    icon: 'pi pi-search',
    command: () => router.push('/search')
  },
  {
    label: 'Book',
    icon: 'pi pi-book',
    command: () => router.push('/book')
  },
  {
    label: 'Import',
    icon: 'pi pi-upload',
    command: () => router.push('/import')
  },
  {
    label: 'About',
    icon: 'pi pi-info-circle',
    command: () => router.push('/about')
  }
])
</script>

<template>
  <ConfirmDialog />
  <div class="app-container">
    <header class="app-header">
      <Menubar :model="items">
        <template #start>
          <div class="app-logo">
            <i class="pi pi-book" style="font-size: 1.5rem"></i>
            <span class="app-title">Vocabee</span>
          </div>
        </template>
      </Menubar>
    </header>

    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--surface-ground);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: var(--surface-card);
  border-bottom: 1px solid var(--surface-border);
}

.app-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-weight: 700;
  font-size: 1.25rem;
  color: var(--primary-color);
  padding-left: 0.5rem;
}

.app-title {
  color: var(--primary-color);
  font-weight: 700;
}

.app-main {
  flex: 1;
  background: var(--surface-ground);
}
</style>
