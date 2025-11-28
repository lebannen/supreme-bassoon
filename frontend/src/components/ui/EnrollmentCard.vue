<script setup lang="ts">
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import ProgressBar from 'primevue/progressbar'
import {useRouter} from 'vue-router'
import {computed} from 'vue'
import type {Enrollment} from '@/types/progress'

export interface EnrollmentCardProps {
  enrollment: Enrollment
}

const props = defineProps<EnrollmentCardProps>()
const router = useRouter()

const lastAccessedFormatted = computed(() => {
  const date = new Date(props.enrollment.lastAccessedAt)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffDays === 0) return 'Today'
  if (diffDays === 1) return 'Yesterday'
  if (diffDays < 7) return `${diffDays} days ago`
  if (diffDays < 30) return `${Math.floor(diffDays / 7)} weeks ago`
  return date.toLocaleDateString()
})

function handleClick() {
  router.push({
    name: 'course-detail',
    params: {slug: props.enrollment.courseSlug}
  })
}
</script>

<template>
  <Card class="enrollment-card card-interactive" @click="handleClick">
    <template #content>
      <div class="enrollment-header">
        <div class="meta-badges">
          <Tag :value="enrollment.languageCode.toUpperCase()" severity="info"/>
          <Tag :value="enrollment.cefrLevel" severity="secondary"/>
        </div>
        <span class="last-accessed text-sm text-secondary">{{ lastAccessedFormatted }}</span>
      </div>

      <h3 class="enrollment-title">{{ enrollment.courseName }}</h3>

      <p v-if="enrollment.currentEpisodeName" class="enrollment-current text-sm text-secondary">
        <i class="pi pi-bookmark mr-2"></i>
        {{ enrollment.currentModuleName }} &bull; {{ enrollment.currentEpisodeName }}
      </p>

      <div class="enrollment-progress">
        <div class="progress-header">
                    <span class="episodes-count text-sm">
                        {{ enrollment.completedEpisodes }}/{{ enrollment.totalEpisodes }} episodes
                    </span>
          <span class="progress-percent text-sm font-semibold">{{ enrollment.progress }}%</span>
        </div>
        <ProgressBar :value="enrollment.progress" :showValue="false"/>
      </div>
    </template>
  </Card>
</template>

<style scoped>
.enrollment-card {
  height: 100%;
}

.enrollment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}

.enrollment-title {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0 0 0.5rem 0;
  line-height: 1.3;
}

.enrollment-current {
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
}

.enrollment-progress {
  margin-top: auto;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.episodes-count {
  color: var(--text-color-secondary);
}

.progress-percent {
  color: var(--primary-color);
}
</style>
