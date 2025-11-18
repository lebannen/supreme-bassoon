<script setup lang="ts">
  import {onMounted, ref} from 'vue'

  const isDark = ref(false)

  // Check for saved theme preference or default to light mode
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
  <button
      class="theme-toggle"
      @click="toggleTheme"
      :aria-label="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
      :title="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
  >
    <span class="theme-icon">{{ isDark ? '☀️' : '🌙' }}</span>
  </button>
</template>

<style scoped>
  .theme-toggle {
    width: 40px;
    height: 40px;
    border-radius: var(--radius-md);
    background: var(--bg-tertiary);
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    transition: all 0.2s;
    margin-bottom: var(--spacing-md);
  }

  .theme-toggle:hover {
    background: var(--bg-hover);
    transform: scale(1.05);
  }

  .theme-icon {
    line-height: 1;
  }
</style>
