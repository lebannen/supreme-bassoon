<script setup lang="ts">
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
  actionLabel: 'Start'
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

<script lang="ts">
import {defineProps, withDefaults, defineEmits} from 'vue'
</script>

<template>
  <div :class="['task-card', 'card', 'card-padding', { completed: completed }]">
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

<style scoped>
.task-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  transition: all 0.2s;
}

.task-card:hover:not(.completed) {
  box-shadow: var(--shadow-md);
  border-color: var(--border-medium);
}

.task-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  transition: background 0.2s;
}

.task-icon.completed {
  background: var(--success-light);
}

.task-info {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.task-card.completed .task-title {
  color: var(--text-tertiary);
}

.task-meta {
  font-size: 13px;
  color: var(--text-tertiary);
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

.btn-primary {
  background-color: var(--primary);
  color: var(--text-inverse);
}

.btn-primary:hover {
  background-color: var(--primary-dark);
  transform: translateY(-1px);
}

.task-action-completed {
  background-color: transparent;
  color: var(--success);
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
