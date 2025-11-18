<script setup lang="ts">
import Card from 'primevue/card'
import {computed} from 'vue'

export interface BaseCardProps {
  /**
   * Padding variant
   * - 'default': 20px padding
   * - 'lg': 24px padding
   * - 'xl': 48px padding
   * - 'none': No padding
   */
  padding?: 'default' | 'lg' | 'xl' | 'none'

  /**
   * Whether the card should have a hover effect
   */
  hoverable?: boolean

  /**
   * Whether the card should have a stronger lift on hover
   */
  hoverableLift?: boolean
}

const props = withDefaults(defineProps<BaseCardProps>(), {
  padding: 'default',
  hoverable: false,
  hoverableLift: false,
})

const cardClasses = computed(() => {
  return {
    'base-card': true,
    'card-padding': props.padding === 'default',
    'card-padding-lg': props.padding === 'lg',
    'card-padding-xl': props.padding === 'xl',
    'card-hoverable': props.hoverable,
    'card-hoverable-lift': props.hoverableLift,
  }
})
</script>

<template>
  <Card :class="cardClasses">
    <template #content>
      <slot></slot>
    </template>
  </Card>
</template>

<style scoped>
.base-card {
  transition: all 0.2s;
}

.card-padding :deep(.p-card-body),
.card-padding :deep(.p-card-content) {
  padding: var(--spacing-lg);
}

.card-padding-lg :deep(.p-card-body),
.card-padding-lg :deep(.p-card-content) {
  padding: var(--spacing-xl);
}

.card-padding-xl :deep(.p-card-body),
.card-padding-xl :deep(.p-card-content) {
  padding: var(--spacing-4xl);
}

.card-hoverable:hover {
  transform: translateY(-1px);
  cursor: pointer;
}

.card-hoverable-lift:hover {
  transform: translateY(-2px);
  cursor: pointer;
}
</style>
