<script setup lang="ts">
  import {onMounted, ref} from 'vue'
  import Button from 'primevue/button'

  const isDark = ref(false)

  onMounted(() => {
    const savedTheme = localStorage.getItem('theme')
    if (savedTheme === 'dark') {
      isDark.value = true
      document.documentElement.classList.add('dark-theme')
    } else {
      isDark.value = false
      document.documentElement.classList.remove('dark-theme')
    }
  })

  const toggleTheme = () => {
    isDark.value = !isDark.value
    if (isDark.value) {
      document.documentElement.classList.add('dark-theme')
      localStorage.setItem('theme', 'dark')
    } else {
      document.documentElement.classList.remove('dark-theme')
      localStorage.setItem('theme', 'light')
    }
  }
</script>

<template>
  <Button
      :icon="isDark ? 'pi pi-sun' : 'pi pi-moon'"
      @click="toggleTheme"
      :aria-label="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
      :title="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
      severity="secondary"
      text
      rounded
  />
</template>
