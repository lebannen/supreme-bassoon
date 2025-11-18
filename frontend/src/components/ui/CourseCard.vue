<script setup lang="ts">

import {computed, defineEmits, defineProps, withDefaults} from 'vue'
import Card from 'primevue/card'

export interface CourseCardProps {
  /**
   * Course name
   */
  name: string

  /**
   * Course description
   */
  description: string

  /**
   * Language code (e.g., "FR", "ES")
   */
  languageCode: string

  /**
   * CEFR level (e.g., "A1", "B2")
   */
  cefrLevel: string

  /**
   * Total number of modules
   */
  totalModules: number

  /**
   * Total number of episodes
   */
  totalEpisodes: number

  /**
   * Estimated hours to complete
   */
  estimatedHours: number

  /**
   * Icon/emoji to display
   */
  icon?: string
}

const props = withDefaults(defineProps<CourseCardProps>(), {
  icon: '📚',
})

const emit = defineEmits<{
  click: []
}>()

// Map language codes to flags/emojis
const languageEmoji = computed(() => {
  const emojiMap: Record<string, string> = {
    fr: '🇫🇷',
    es: '🇪🇸',
    de: '🇩🇪',
    it: '🇮🇹',
    pt: '🇵🇹',
    ja: '🇯🇵',
    zh: '🇨🇳',
    ko: '🇰🇷',
    ru: '🇷🇺',
    ar: '🇸🇦',
  }
  return emojiMap[props.languageCode.toLowerCase()] || '🌐'
})
</script>

<template>
  <Card class="course-card" @click="emit('click')">
    <template #header>
      <div class="course-header">
        <span class="course-icon">{{ languageEmoji }}</span>
        <div class="course-badges">
          <span class="badge">{{ languageCode.toUpperCase() }}</span>
          <span class="badge badge-level">{{ cefrLevel }}</span>
        </div>
      </div>
    </template>

    <template #content>
      <div class="course-body">
        <h3 class="course-title">{{ name }}</h3>
        <p class="course-description">{{ description }}</p>

        <!-- Course Stats -->
        <div class="course-stats">
          <div class="stat-item">
            <span class="stat-icon">📖</span>
            <span>{{ totalModules }} modules</span>
          </div>
          <div class="stat-item">
            <span class="stat-icon">📝</span>
            <span>{{ totalEpisodes }} episodes</span>
          </div>
          <div class="stat-item">
            <span class="stat-icon">⏱️</span>
            <span>~{{ estimatedHours }}h</span>
          </div>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>
.course-card {
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.course-card:hover {
  transform: translateY(-2px);
}

.course-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: var(--spacing-xl);
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 120px;
  position: relative;
}

.course-icon {
  font-size: 48px;
}

.course-badges {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-end;
}

.badge {
  font-size: 10px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 10px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  backdrop-filter: blur(10px);
}

.course-body {
  padding: var(--spacing-lg);
  flex: 1;
  display: flex;
  flex-direction: column;
}

.course-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 var(--spacing-sm) 0;
  line-height: 1.4;
}

.course-description {
  font-size: 14px;
  margin: 0 0 var(--spacing-lg) 0;
  line-height: 1.6;
  flex: 1;
}

.course-stats {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  padding-top: var(--spacing-md);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 13px;
}

.stat-icon {
  font-size: 14px;
}
</style>
