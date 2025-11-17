<script setup lang="ts">
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

<script lang="ts">
import {defineProps, withDefaults, defineEmits, computed} from 'vue'
</script>

<template>
  <div class="course-card card card-hoverable-lift" @click="emit('click')">
    <!-- Course Header with gradient -->
    <div class="course-header">
      <span class="course-icon">{{ languageEmoji }}</span>
      <div class="course-badges">
        <span class="badge">{{ languageCode.toUpperCase() }}</span>
        <span class="badge badge-level">{{ cefrLevel }}</span>
      </div>
    </div>

    <!-- Course Body -->
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
  </div>
</template>

<style scoped>
.course-card {
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
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
  background-color: rgba(255, 255, 255, 0.25);
  color: white;
  font-size: 10px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 10px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  backdrop-filter: blur(10px);
}

.badge-level {
  background-color: rgba(255, 255, 255, 0.35);
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
  color: var(--text-primary);
  margin: 0 0 var(--spacing-sm) 0;
  line-height: 1.4;
}

.course-description {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 var(--spacing-lg) 0;
  line-height: 1.6;
  flex: 1;
}

.course-stats {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--border-light);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 13px;
  color: var(--text-secondary);
}

.stat-icon {
  font-size: 14px;
}
</style>
