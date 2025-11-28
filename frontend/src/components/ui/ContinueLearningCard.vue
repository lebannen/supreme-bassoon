<script setup lang="ts">
import Card from 'primevue/card'
import Button from 'primevue/button'
import ProgressBar from 'primevue/progressbar'
import {useRouter} from 'vue-router'
import type {ContinueLearning} from '@/types/progress'

export interface ContinueLearningCardProps {
  data: ContinueLearning
}

const props = defineProps<ContinueLearningCardProps>()
const router = useRouter()

function handleContinue() {
  if (props.data.episodeId) {
    router.push({
      name: 'episode',
      params: {id: props.data.episodeId}
    })
  } else {
    router.push({
      name: 'course-detail',
      params: {slug: props.data.courseSlug}
    })
  }
}
</script>

<template>
  <Card class="continue-learning-card card-interactive" @click="handleContinue">
    <template #content>
      <div class="continue-content">
        <div class="continue-icon">
          <i class="pi pi-play-circle"></i>
        </div>
        <div class="continue-info">
          <span class="continue-label">Continue Learning</span>
          <h3 class="continue-course">{{ data.courseName }}</h3>
          <p v-if="data.episodeName" class="continue-episode">
            {{ data.moduleName }} &bull; {{ data.episodeName }}
          </p>
          <div class="continue-progress">
            <ProgressBar :value="data.progress" :showValue="false" class="progress-bar"/>
            <span class="progress-text">{{ data.progress }}% complete</span>
          </div>
        </div>
        <Button
            icon="pi pi-arrow-right"
            rounded
            class="continue-button"
            @click.stop="handleContinue"
        />
      </div>
    </template>
  </Card>
</template>

<style scoped>
.continue-learning-card {
  background: var(--gradient-dialogue);
}

.continue-content {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.continue-icon {
  width: 4rem;
  height: 4rem;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.continue-icon i {
  font-size: 2rem;
  color: white;
}

.continue-info {
  flex: 1;
  min-width: 0;
}

.continue-label {
  display: block;
  margin-bottom: 0.25rem;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.85);
}

.continue-course {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0 0 0.25rem 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: white;
}

.continue-episode {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.continue-progress {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.progress-bar {
  flex: 1;
  height: 0.375rem;
  background: rgba(255, 255, 255, 0.3);
}

.progress-bar :deep(.p-progressbar-value) {
  background: white;
}

.progress-text {
  flex-shrink: 0;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.85);
}

.continue-button {
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .continue-content {
    flex-wrap: wrap;
  }

  .continue-info {
    flex: 1 1 calc(100% - 5.25rem);
  }

  .continue-button {
    margin-left: auto;
  }
}
</style>
