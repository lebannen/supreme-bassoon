<script setup lang="ts">
import Card from 'primevue/card'

export interface StatCardProps {
  /**
   * Icon to display (e.g., 'pi pi-star')
   */
  icon: string

  /**
   * Label text (e.g., "Study Streak")
   */
  label: string

  /**
   * Value to display (e.g., "7 days", "142")
   */
  value: string | number

  /**
   * Color variant for the icon
   */
  variant?: 'primary' | 'success' | 'warning' | 'error' | 'info' | 'purple' | 'blue' | 'yellow' | 'green'
}

withDefaults(defineProps<StatCardProps>(), {
  variant: 'primary',
})
</script>

<template>
  <Card>
    <template #content>
      <div class="flex align-items-center gap-4">
        <div class="stat-icon" :class="`stat-icon-${variant}`">
          <i :class="icon"></i>
        </div>
        <div class="flex flex-column">
          <span class="text-sm font-semibold text-secondary text-uppercase">{{ label }}</span>
          <span class="text-3xl font-bold text-primary">{{ value }}</span>
        </div>
      </div>
    </template>
  </Card>
</template>
