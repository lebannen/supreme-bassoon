<script setup lang="ts">
import Card from 'primevue/card'
import {computed} from 'vue'

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
  hoverable: true,
})

const cardClasses = computed(() => {
  return {
    'stat-card': true,
    'card-hoverable': props.hoverable,
  }
})
</script>

<template>
  <Card :class="cardClasses">
    <template #content>
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value">
        <span class="stat-icon">{{ icon }}</span>
        <span>{{ value }}</span>
      </div>
    </template>
  </Card>
</template>

<style scoped>
.stat-card {
  transition: all 0.2s;
}

.stat-card :deep(.p-card-body),
.stat-card :deep(.p-card-content) {
  padding: var(--spacing-lg);
}

.card-hoverable:hover {
  transform: translateY(-1px);
  cursor: pointer;
}

.stat-label {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: var(--spacing-xs);
  font-weight: 600;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  display: flex;
  align-items: baseline;
  gap: var(--spacing-xs);
}

.stat-icon {
  font-size: 20px;
}
</style>
