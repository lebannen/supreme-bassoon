<script setup lang="ts">
export interface ContentCardProps {
  /**
   * Content type: 'dialogue', 'story', or 'grammar'
   */
  type: 'dialogue' | 'story' | 'grammar'

  /**
   * Icon/emoji to display
   */
  icon: string

  /**
   * Content title
   */
  title: string

  /**
   * Level badge (e.g., "A1", "B2")
   */
  level?: string

  /**
   * Content description
   */
  description?: string

  /**
   * Duration (e.g., "4 min")
   */
  duration?: string

  /**
   * Topic/category
   */
  topic?: string

  /**
   * Whether content is completed
   */
  completed?: boolean
}

const props = withDefaults(defineProps<ContentCardProps>(), {
  completed: false
})

const emit = defineEmits<{
  click: []
}>()

const gradientClass = computed(() => {
  return `content-image-${props.type}`
})
</script>

<script lang="ts">
import {defineProps, withDefaults, defineEmits, computed} from 'vue'
</script>

<template>
  <div class="content-card card card-hoverable-lift" @click="emit('click')">
    <div :class="['content-image', gradientClass]">
      <span class="content-icon">{{ icon }}</span>
      <span v-if="completed" class="completed-badge">✓ Completed</span>
      <span v-if="duration" class="audio-indicator">🎧 {{ duration }}</span>
    </div>
    <div class="content-body">
      <div class="content-header">
        <span class="badge">{{ type.charAt(0).toUpperCase() + type.slice(1) }}</span>
        <span v-if="level" class="badge badge-level">{{ level }}</span>
      </div>
      <div class="content-title">{{ title }}</div>
      <div v-if="description" class="content-description">{{ description }}</div>
      <div v-if="topic || duration" class="content-meta">
        <span v-if="duration" class="meta-item">
          <span>🎧</span>
          <span>{{ duration }}</span>
        </span>
        <span v-if="topic && duration" class="meta-separator">•</span>
        <span v-if="topic" class="meta-item">{{ topic }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.content-card {
  overflow: hidden;
  cursor: pointer;
}

.content-image {
  width: 100%;
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 48px;
  position: relative;
}

.content-image-dialogue {
  background: var(--gradient-dialogue);
}

.content-image-story {
  background: var(--gradient-story);
}

.content-image-grammar {
  background: var(--gradient-grammar);
}

.content-icon {
  font-size: 48px;
}

.completed-badge {
  position: absolute;
  top: var(--spacing-sm);
  left: var(--spacing-sm);
  background: rgba(16, 185, 129, 0.95);
  color: white;
  padding: 5px var(--spacing-sm);
  border-radius: var(--radius-xl);
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

.audio-indicator {
  position: absolute;
  top: var(--spacing-sm);
  right: var(--spacing-sm);
  background: rgba(255, 255, 255, 0.95);
  padding: 5px var(--spacing-sm);
  border-radius: var(--radius-xl);
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.content-body {
  padding: var(--spacing-md);
}

.content-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.badge {
  background-color: var(--primary-light);
  color: var(--primary);
  font-size: 10px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 10px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.badge-level {
  background-color: var(--warning-light);
  color: var(--warning);
}

.content-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
  line-height: 1.4;
}

.content-description {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 10px;
  line-height: 1.5;
}

.content-meta {
  font-size: 12px;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-separator {
  color: var(--border-dark);
}
</style>
