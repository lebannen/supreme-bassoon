<script setup lang="ts">
import Card from 'primevue/card'
import {computed} from 'vue'

export interface TaskCardProps {
  /**
   * Icon to display (emoji or text)
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

const cardClasses = computed(() => {
  return {
    'task-card': true,
    'completed': props.completed,
  }
})
</script>

<template>
  <Card :class="cardClasses">
    <template #content>
      <div class="task-content">
        <div :class="['task-icon', { completed: completed }]">{{ completed ? '✓' : icon }}</div>
        <div class="task-info">
          <div class="task-title">{{ title }}</div>
          <div class="task-meta">{{ meta }}</div>
        </div>
        <button
            :class="['btn', completed ? 'task-action-completed' : 'btn-primary']"
            @click="handleAction"
            :disabled="completed"
        >
          {{ completed ? '✓ Done' : actionLabel }}
        </button>
      </div>
    </template>
  </Card>
</template>

<style scoped>
.task-card {
  transition: all 0.2s;
}

.task-card :deep(.p-card-body),
.task-card :deep(.p-card-content) {
  padding: var(--spacing-lg);
}

.task-card:hover:not(.completed) {
  transform: translateY(-1px);
}

.task-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.task-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  transition: all 0.2s;
}

.task-info {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.task-meta {
  font-size: 13px;
}

/* Button styles */
.btn {
  padding: 8px 20px;
  border-radius: var(--radius-md);
  border: none;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-primary:hover {
  transform: translateY(-1px);
}

.task-action-completed {
  cursor: default;
  padding: 8px var(--spacing-sm);
}

.task-action-completed:hover {
  transform: none;
}

button:disabled {
  opacity: 1;
}
</style>
