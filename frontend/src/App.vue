<script setup lang="ts">
import { RouterView, useRouter } from 'vue-router'
import { ref, computed } from 'vue'
import Menubar from 'primevue/menubar'
import ConfirmDialog from 'primevue/confirmdialog'
import Button from 'primevue/button'
import Avatar from 'primevue/avatar'
import Menu from 'primevue/menu'
import { useAuthStore } from './stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const userMenu = ref()

const items = computed(() => {
  const baseItems = [
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
      label: 'Word Sets',
      icon: 'pi pi-list',
      command: () => router.push('/word-sets')
    }
  ]

  // Only show Vocabulary when authenticated
  if (authStore.isAuthenticated) {
    baseItems.push({
      label: 'Vocabulary',
      icon: 'pi pi-bookmark',
      command: () => router.push('/vocabulary')
    })
  }

  baseItems.push({
    label: 'Book',
    icon: 'pi pi-book',
    command: () => router.push('/book')
  })

  // Only show Import when authenticated
  if (authStore.isAuthenticated) {
    baseItems.push({
      label: 'Import',
      icon: 'pi pi-upload',
      command: () => router.push('/import')
    })
  }

  baseItems.push({
    label: 'About',
    icon: 'pi pi-info-circle',
    command: () => router.push('/about')
  })

  return baseItems
})

const userMenuItems = ref([
  {
    label: 'Profile',
    icon: 'pi pi-user',
    command: () => {
      router.push('/profile')
    }
  },
  {
    label: 'Settings',
    icon: 'pi pi-cog',
    command: () => {
      // TODO: Navigate to settings page when implemented
    }
  },
  {
    separator: true
  },
  {
    label: 'Logout',
    icon: 'pi pi-sign-out',
    command: () => {
      authStore.logout()
      router.push('/')
    }
  }
])

const toggleUserMenu = (event: Event) => {
  userMenu.value.toggle(event)
}

const getUserInitials = (name: string | null) => {
  if (!name) return 'U'
  return name
    .split(' ')
    .map(n => n[0])
    .join('')
    .toUpperCase()
    .substring(0, 2)
}
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
        <template #end>
          <div class="auth-section">
            <!-- Not authenticated: Show login/register buttons -->
            <div v-if="!authStore.isAuthenticated" class="auth-buttons">
              <Button
                label="Login"
                icon="pi pi-sign-in"
                outlined
                size="small"
                @click="router.push('/login')"
              />
              <Button
                label="Register"
                icon="pi pi-user-plus"
                size="small"
                @click="router.push('/register')"
              />
            </div>

            <!-- Authenticated: Show user info and menu -->
            <div v-else class="user-section">
              <span class="user-greeting">
                Hello, {{ authStore.user?.displayName || authStore.user?.email?.split('@')[0] }}!
              </span>
              <Button
                type="button"
                :label="getUserInitials(authStore.user?.displayName || null)"
                rounded
                severity="info"
                @click="toggleUserMenu"
                aria-haspopup="true"
                aria-controls="user_menu"
              />
              <Menu id="user_menu" ref="userMenu" :model="userMenuItems" :popup="true" />
            </div>
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

.auth-section {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.auth-buttons {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.user-section {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-greeting {
  color: var(--text-color);
  font-weight: 500;
  font-size: 0.95rem;
}

@media (max-width: 768px) {
  .user-greeting {
    display: none;
  }

  .auth-buttons {
    gap: 0.25rem;
  }
}
</style>
