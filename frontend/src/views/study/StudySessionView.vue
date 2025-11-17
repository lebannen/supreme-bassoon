<script setup lang="ts">
import {onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useStudyStore} from '@/stores/study'
import FlashCard from '@/components/vocabulary/FlashCard.vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import ProgressBar from 'primevue/progressbar'
import Message from 'primevue/message'
import {useConfirm} from 'primevue/useconfirm'

const router = useRouter()
const studyStore = useStudyStore()
const confirm = useConfirm()

const flashCardRef = ref<InstanceType<typeof FlashCard> | null>(null)
const showSummaryDialog = ref(false)

onMounted(async () => {
  // If no active session, redirect to study home
  if (!studyStore.isSessionActive) {
    await studyStore.loadActiveSession()

    if (!studyStore.isSessionActive) {
      router.push('/study')
      return
    }
  }

  // Load first card if we don't have one yet
  if (!studyStore.currentCard) {
    await studyStore.fetchNextCard()
  }
})

// Watch for session completion
watch(
    () => studyStore.sessionSummary,
    (summary) => {
      if (summary) {
        showSummaryDialog.value = true
      }
    }
)

// Watch for new cards and reset flip state
watch(
    () => studyStore.currentCard,
    () => {
      if (flashCardRef.value) {
        flashCardRef.value.resetFlip()
      }
    }
)

function handleFlip() {
  // Card was flipped - no action needed, just for tracking
}

async function handleAnswer(correct: boolean) {
  await studyStore.submitAnswer(correct)
}

function handleExitSession() {
  confirm.require({
    message:
        'Are you sure you want to exit this study session? You can resume it later from where you left off.',
    header: 'Exit Session',
    icon: 'pi pi-sign-out',
    rejectLabel: 'Cancel',
    acceptLabel: 'Exit',
    accept: () => {
      router.push('/study')
    },
  })
}

function handleFinishSummary() {
  studyStore.clearSummary()
  router.push('/study')
}

function formatDuration(duration: string): string {
  // Duration is in format like "PT5M30S" (ISO 8601)
  const match = duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/)
  if (!match) return duration

  const hours = parseInt(match[1] || '0')
  const minutes = parseInt(match[2] || '0')
  const seconds = parseInt(match[3] || '0')

  const parts = []
  if (hours > 0) parts.push(`${hours}h`)
  if (minutes > 0) parts.push(`${minutes}m`)
  if (seconds > 0 || parts.length === 0) parts.push(`${seconds}s`)

  return parts.join(' ')
}
</script>

<template>
  <div class="page-container">
    <div class="content-area">
      <!-- Session Header -->
      <div v-if="studyStore.isSessionActive" class="session-header">
        <div class="flex flex-col gap-sm">
          <h2 class="text-2xl font-bold text-primary session-title">Study Session</h2>
          <div class="flex items-center gap-lg">
            <span class="font-semibold text-secondary">
              {{ studyStore.activeSession?.wordsCompleted }} /
              {{ studyStore.activeSession?.totalWords }} words
            </span>
            <span
                class="accuracy-badge"
                :class="{
                'high-accuracy': (studyStore.activeSession?.stats.accuracy || 0) >= 80,
                'medium-accuracy':
                  (studyStore.activeSession?.stats.accuracy || 0) >= 60 &&
                  (studyStore.activeSession?.stats.accuracy || 0) < 80,
                'low-accuracy': (studyStore.activeSession?.stats.accuracy || 0) < 60,
              }"
            >
              {{ Math.round(studyStore.activeSession?.stats.accuracy || 0) }}% accuracy
            </span>
          </div>
        </div>
        <Button
          label="Exit"
          icon="pi pi-sign-out"
          severity="secondary"
          outlined
          @click="handleExitSession"
        />
      </div>

      <!-- Progress Bar -->
      <ProgressBar
        v-if="studyStore.isSessionActive"
        :value="studyStore.progressPercentage"
        class="session-progress-bar"
      />

      <!-- Error Message -->
      <Message v-if="studyStore.error" severity="error" :closable="false" class="mb-md">
        {{ studyStore.error }}
      </Message>

      <!-- Flash Card -->
      <FlashCard
        v-if="studyStore.currentCard"
        ref="flashCardRef"
        :word="studyStore.currentCard.word"
        :progress="studyStore.currentCard.progress"
        :srs-info="studyStore.currentCard.srsInfo"
        :loading="studyStore.loading"
        @flip="handleFlip"
        @answer="handleAnswer"
      />

      <!-- Loading State -->
      <Card v-else-if="studyStore.loading" class="mt-xl">
        <template #content>
          <div class="loading-state">
            <i class="pi pi-spin pi-spinner text-3xl icon-primary"></i>
            <p>Loading next card...</p>
          </div>
        </template>
      </Card>

      <!-- No Cards State -->
      <Card v-else-if="!studyStore.currentCard && !studyStore.loading" class="mt-xl">
        <template #content>
          <div class="loading-state">
            <i class="pi pi-check-circle text-4xl icon-success"></i>
            <h3 class="text-2xl font-semibold text-primary complete-heading">Session Complete!</h3>
            <p>Preparing your summary...</p>
          </div>
        </template>
      </Card>
    </div>

    <!-- Session Summary Dialog -->
    <Dialog
      v-model:visible="showSummaryDialog"
      header="Session Complete!"
      :modal="true"
      :closable="false"
      :style="{ width: '600px' }"
      :breakpoints="{ '960px': '75vw', '640px': '90vw' }"
    >
      <div v-if="studyStore.sessionSummary" class="flex flex-col gap-xl">
        <div class="text-center">
          <i class="pi pi-check-circle text-4xl mb-md icon-success"></i>
          <h2 class="text-3xl font-bold text-primary summary-heading">Great job!</h2>
          <p class="text-secondary summary-subheading">You completed your study session</p>
        </div>

        <div class="summary-stats">
          <div class="stat-card">
            <div class="stat-icon">
              <i class="pi pi-book"></i>
            </div>
            <div class="text-center flex flex-col gap-xs">
              <span class="text-2xl font-bold text-primary">{{
                  studyStore.sessionSummary.stats.totalWords
                }}</span>
              <span class="text-sm text-secondary">Words Studied</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon accuracy-icon">
              <i class="pi pi-chart-line"></i>
            </div>
            <div class="text-center flex flex-col gap-xs">
              <span class="text-2xl font-bold text-primary"
              >{{ Math.round(studyStore.sessionSummary.stats.accuracy) }}%</span
              >
              <span class="text-sm text-secondary">Accuracy</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon time-icon">
              <i class="pi pi-clock"></i>
            </div>
            <div class="text-center flex flex-col gap-xs">
              <span class="text-2xl font-bold text-primary">{{
                  formatDuration(studyStore.sessionSummary.duration)
                }}</span>
              <span class="text-sm text-secondary">Duration</span>
            </div>
          </div>
        </div>

        <div class="summary-details">
          <div class="flex justify-between items-center">
            <span class="flex items-center gap-sm text-secondary">
              <i class="pi pi-check text-sm"></i>
              Correct Answers
            </span>
            <span class="font-semibold detail-success">
              {{ studyStore.sessionSummary.stats.correctAttempts }}
            </span>
          </div>
          <div class="flex justify-between items-center">
            <span class="flex items-center gap-sm text-secondary">
              <i class="pi pi-times text-sm"></i>
              Incorrect Answers
            </span>
            <span class="font-semibold detail-error">
              {{ studyStore.sessionSummary.stats.incorrectAttempts }}
            </span>
          </div>
          <div class="flex justify-between items-center">
            <span class="flex items-center gap-sm text-secondary">
              <i class="pi pi-arrow-up text-sm"></i>
              Words Advanced
            </span>
            <span class="font-semibold text-primary">
              {{ studyStore.sessionSummary.srsUpdates.wordsAdvanced }}
            </span>
          </div>
          <div
              v-if="studyStore.sessionSummary.srsUpdates.wordsReset > 0"
              class="flex justify-between items-center"
          >
            <span class="flex items-center gap-sm text-secondary">
              <i class="pi pi-refresh text-sm"></i>
              Words Reset
            </span>
            <span class="font-semibold text-primary">
              {{ studyStore.sessionSummary.srsUpdates.wordsReset }}
            </span>
          </div>
          <div class="flex justify-between items-center">
            <span class="flex items-center gap-sm text-secondary">
              <i class="pi pi-calendar text-sm"></i>
              Next Due
            </span>
            <span class="font-semibold text-primary">
              {{ studyStore.sessionSummary.srsUpdates.nextDueCount }} words
            </span>
          </div>
        </div>

        <div class="flex justify-center">
          <Button
            label="Done"
            icon="pi pi-check"
            @click="handleFinishSummary"
            size="large"
            class="done-button"
          />
        </div>
      </div>
    </Dialog>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  background: var(--bg-primary);
  padding: var(--spacing-xl) var(--spacing-md);
}

.content-area {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.mt-xl {
  margin-top: var(--spacing-xl);
}

.icon-primary {
  color: var(--primary);
}

.icon-success {
  color: var(--success);
}

.session-title {
  margin: 0;
}

.session-progress-bar {
  height: 0.75rem;
}

.complete-heading {
  margin: 0.5rem 0;
}

.summary-heading {
  margin: 0 0 0.5rem 0;
}

.summary-subheading {
  margin: 0;
}

.detail-success {
  color: var(--success);
}

.detail-error {
  color: var(--error);
}

.done-button {
  min-width: 200px;
}

/* Session Header */
.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
  background: var(--bg-secondary);
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.accuracy-badge {
  font-weight: 600;
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
}

.high-accuracy {
  background: var(--success-light);
  color: var(--success);
}

.medium-accuracy {
  background: var(--warning-light);
  color: var(--warning);
}

.low-accuracy {
  background: var(--error-light);
  color: var(--error);
}

/* Summary Dialog */
.summary-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-md);
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-lg);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  gap: var(--spacing-md);
}

.stat-icon {
  width: 3rem;
  height: 3rem;
  border-radius: var(--radius-full);
  background: var(--primary-light);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

.accuracy-icon {
  background: var(--success-light);
  color: var(--success);
}

.time-icon {
  background: var(--warning-light);
  color: var(--warning);
}

.summary-details {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

/* Responsive */
@media (max-width: 768px) {
  .page-container {
    padding: var(--spacing-md);
  }

  .session-header {
    flex-direction: column;
    gap: var(--spacing-md);
    align-items: flex-start;
  }

  .summary-stats {
    grid-template-columns: 1fr;
  }

  .summary-details {
    padding: var(--spacing-md);
  }
}
</style>
