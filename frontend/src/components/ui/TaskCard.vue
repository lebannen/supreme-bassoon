<script setup lang="ts">
import Card from 'primevue/card'
import Button from 'primevue/button'

export interface TaskCardProps {
  /**
   * Icon class to display (e.g., 'pi pi-book')
   */
  icon: string

  /**
   * Task title
   */
  title: string

  /**
   * Task metadata (e.g., "12 words • 3 minutes")
   */
  meta: string

  /**
   * Whether the task is completed
   */
  completed?: boolean

  /**
   * Action button label
   */
  actionLabel?: string
}

const props = withDefaults(defineProps<TaskCardProps>(), {
  completed: false,
  actionLabel: 'Start',
})

const emit = defineEmits<{
  action: []
}>()

const handleAction = () => {
  if (!props.completed) {
    emit('action')
  }
}
</script>

<template>
  <Card class="p-4">
    <template #content>
      <div class="flex align-items-center gap-3">
        <div
            class="number-badge-sm"
            :class="completed ? 'bg-success' : 'bg-surface'"
        >
          <i v-if="!completed" :class="icon"></i>
        </div>

        <div class="flex-1 min-w-0">
          <div class="font-semibold text-primary mb-1">{{ title }}</div>
          <div class="text-sm text-secondary">{{ meta }}</div>
        </div>

        <Button
            :label="completed ? 'Done' : actionLabel"
            :disabled="completed"
            :severity="completed ? 'success' : 'primary'"
            @click="handleAction"
        />
      </div>
    </template>
  </Card>
</template>
