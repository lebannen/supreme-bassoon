<script setup lang="ts">
export interface StatCardProps {
  /**
   * Icon to display (emoji or text)
   */
  icon: string

  /**
   * Label text (e.g., "Study Streak")
   */
  label: string

  /**
   * Value to display (e.g., "7 days", "142")
   */
  value: string

  /**
   * Whether the card should have hover effect
   */
  hoverable?: boolean
}

const props = withDefaults(defineProps<StatCardProps>(), {
  hoverable: true
})
</script>

<script lang="ts">
import {defineProps, withDefaults} from 'vue'
</script>

<template>
  <div :class="['stat-card', 'card', 'card-padding', { 'card-hoverable': hoverable }]">
    <div class="stat-label">{{ label }}</div>
    <div class="stat-value">
      <span class="stat-icon">{{ icon }}</span>
      <span>{{ value }}</span>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  /* Inherits base card styles from theme.css */
}

.stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: var(--spacing-xs);
  font-weight: 600;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: baseline;
  gap: var(--spacing-xs);
}

.stat-icon {
  font-size: 20px;
}
</style>
