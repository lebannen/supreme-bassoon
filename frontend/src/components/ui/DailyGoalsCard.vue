<script setup lang="ts">
import Card from 'primevue/card'
import ProgressBar from 'primevue/progressbar'
import {computed} from 'vue'
import type {DailyGoal} from '@/types/progress'

export interface DailyGoalsCardProps {
  goals: DailyGoal[]
}

const props = defineProps<DailyGoalsCardProps>()

const completedCount = computed(() => props.goals.filter(g => g.completed).length)

const tierMedals: Record<number, string> = {
  1: '🥉',
  2: '🥈',
  3: '🥇'
}

function getProgressPercent(goal: DailyGoal): number {
  return Math.min(100, Math.round((goal.current / goal.target) * 100))
}
</script>

<template>
  <Card class="daily-goals-card">
    <template #title>
      <div class="flex align-items-center justify-content-between">
        <span>Today's Goals</span>
        <span class="goals-summary text-secondary text-sm font-normal">
                    {{ completedCount }}/{{ goals.length }} completed
                </span>
      </div>
    </template>
    <template #content>
      <div class="goals-list">
        <div
            v-for="goal in goals"
            :key="goal.id"
            class="goal-item"
            :class="{ 'goal-completed': goal.completed }"
        >
          <div class="goal-header">
            <div class="goal-info">
              <span class="goal-medal">{{ tierMedals[goal.tier] }}</span>
              <span class="goal-title">{{ goal.title }}</span>
            </div>
            <span class="goal-progress-text">
                            {{ goal.current }}/{{ goal.target }}
                        </span>
          </div>
          <ProgressBar
              :value="getProgressPercent(goal)"
              :showValue="false"
              class="goal-progress"
              :class="{ 'progress-complete': goal.completed }"
          />
          <p class="goal-description text-sm text-secondary m-0">
            {{ goal.description }}
          </p>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>
.daily-goals-card {
  height: 100%;
}

.goals-list {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.goal-item {
  padding: 1rem;
  border-radius: var(--radius-md);
  background: var(--surface-ground);
  transition: all 0.2s;
}

.goal-completed {
  background: var(--green-50);
}

:root.p-dark .goal-completed {
  background: color-mix(in srgb, var(--green-500) 15%, transparent);
}

.goal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.goal-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.goal-medal {
  font-size: 1.25rem;
}

.goal-title {
  font-weight: 600;
}

.goal-progress-text {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-color-secondary);
}

.goal-progress {
  height: 0.5rem;
  margin-bottom: 0.5rem;
}

.goal-progress.progress-complete :deep(.p-progressbar-value) {
  background: var(--green-500);
}

.goal-description {
  line-height: 1.4;
}
</style>
