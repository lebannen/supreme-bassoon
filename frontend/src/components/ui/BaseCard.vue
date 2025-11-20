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
    'p-lg': props.padding === 'default',
    'p-xl': props.padding === 'lg',
    'p-4xl': props.padding === 'xl',
    'hover-lift': props.hoverable,
    'card-interactive': props.hoverable || props.hoverableLift,
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
