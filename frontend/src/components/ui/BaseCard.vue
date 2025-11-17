<script setup lang="ts">
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
  hoverableLift: false
})

const cardClasses = computed(() => {
  return {
    'card': true,
    'card-padding': props.padding === 'default',
    'card-padding-lg': props.padding === 'lg',
    'card-padding-xl': props.padding === 'xl',
    'card-hoverable': props.hoverable,
    'card-hoverable-lift': props.hoverableLift
  }
})
</script>

<script lang="ts">
import {computed} from 'vue'
</script>

<template>
  <div :class="cardClasses">
    <slot></slot>
  </div>
</template>

<style scoped>
.card {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  transition: all 0.2s;
}

.card-padding {
  padding: var(--spacing-lg);
}

.card-padding-lg {
  padding: var(--spacing-xl);
}

.card-padding-xl {
  padding: var(--spacing-4xl);
}

.card-hoverable:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
  cursor: pointer;
}

.card-hoverable-lift:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
  cursor: pointer;
}
</style>
