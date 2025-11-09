<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useStudyStore } from '@/stores/study'
import FlashCard from '@/components/FlashCard.vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import ProgressBar from 'primevue/progressbar'
import Message from 'primevue/message'
import { useConfirm } from 'primevue/useconfirm'

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
watch(() => studyStore.sessionSummary, (summary) => {
  if (summary) {
    showSummaryDialog.value = true
  }
})

// Watch for new cards and reset flip state
watch(() => studyStore.currentCard, () => {
  if (flashCardRef.value) {
    flashCardRef.value.resetFlip()
  }
})

function handleFlip() {
  // Card was flipped - no action needed, just for tracking
}

async function handleAnswer(correct: boolean) {
  await studyStore.submitAnswer(correct)
}

function handleExitSession() {
  confirm.require({
    message: 'Are you sure you want to exit this study session? You can resume it later from where you left off.',
    header: 'Exit Session',
    icon: 'pi pi-sign-out',
    rejectLabel: 'Cancel',
    acceptLabel: 'Exit',
    accept: () => {
      router.push('/study')
    }
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

function formatInterval(interval: string): string {
  // Interval is like "20 hours", "3 days", etc.
  return interval
}
</script>

<template>
  <div class="study-session-container">
    <div class="study-session-content">
      <!-- Session Header -->
      <div v-if="studyStore.isSessionActive" class="session-header">
        <div class="session-info">
          <h2 class="session-title">Study Session</h2>
          <div class="session-meta">
            <span class="words-count">
              {{ studyStore.activeSession?.wordsCompleted }} / {{ studyStore.activeSession?.totalWords }} words
            </span>
            <span class="accuracy" :class="{
              'high-accuracy': (studyStore.activeSession?.stats.accuracy || 0) >= 80,
              'medium-accuracy': (studyStore.activeSession?.stats.accuracy || 0) >= 60 && (studyStore.activeSession?.stats.accuracy || 0) < 80,
              'low-accuracy': (studyStore.activeSession?.stats.accuracy || 0) < 60
            }">
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
      <Message v-if="studyStore.error" severity="error" :closable="false" class="error-message">
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
      <Card v-else-if="studyStore.loading" class="loading-card">
        <template #content>
          <div class="loading-content">
            <i class="pi pi-spin pi-spinner loading-icon"></i>
            <p>Loading next card...</p>
          </div>
        </template>
      </Card>

      <!-- No Cards State -->
      <Card v-else-if="!studyStore.currentCard && !studyStore.loading" class="no-cards">
        <template #content>
          <div class="no-cards-content">
            <i class="pi pi-check-circle success-icon"></i>
            <h3>Session Complete!</h3>
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
      <div v-if="studyStore.sessionSummary" class="summary-content">
        <div class="summary-header">
          <i class="pi pi-check-circle summary-icon"></i>
          <h2>Great job!</h2>
          <p>You completed your study session</p>
        </div>

        <div class="summary-stats">
          <div class="stat-card">
            <div class="stat-icon">
              <i class="pi pi-book"></i>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ studyStore.sessionSummary.stats.totalWords }}</span>
              <span class="stat-label">Words Studied</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon accuracy-icon">
              <i class="pi pi-chart-line"></i>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ Math.round(studyStore.sessionSummary.stats.accuracy) }}%</span>
              <span class="stat-label">Accuracy</span>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon time-icon">
              <i class="pi pi-clock"></i>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ formatDuration(studyStore.sessionSummary.duration) }}</span>
              <span class="stat-label">Duration</span>
            </div>
          </div>
        </div>

        <div class="summary-details">
          <div class="detail-row">
            <span class="detail-label">
              <i class="pi pi-check"></i>
              Correct Answers
            </span>
            <span class="detail-value success-text">
              {{ studyStore.sessionSummary.stats.correctAttempts }}
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">
              <i class="pi pi-times"></i>
              Incorrect Answers
            </span>
            <span class="detail-value danger-text">
              {{ studyStore.sessionSummary.stats.incorrectAttempts }}
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">
              <i class="pi pi-arrow-up"></i>
              Words Advanced
            </span>
            <span class="detail-value">
              {{ studyStore.sessionSummary.srsUpdates.wordsAdvanced }}
            </span>
          </div>
          <div v-if="studyStore.sessionSummary.srsUpdates.wordsReset > 0" class="detail-row">
            <span class="detail-label">
              <i class="pi pi-refresh"></i>
              Words Reset
            </span>
            <span class="detail-value">
              {{ studyStore.sessionSummary.srsUpdates.wordsReset }}
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">
              <i class="pi pi-calendar"></i>
              Next Due
            </span>
            <span class="detail-value">
              {{ studyStore.sessionSummary.srsUpdates.nextDueCount }} words
            </span>
          </div>
        </div>

        <div class="summary-actions">
          <Button
            label="Done"
            icon="pi pi-check"
            @click="handleFinishSummary"
            size="large"
            class="finish-button"
          />
        </div>
      </div>
    </Dialog>
  </div>
</template>

<style scoped>
.study-session-container {
  min-height: 100vh;
  background: var(--surface-ground);
  padding: 2rem 1rem;
}

.study-session-content {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Session Header */
.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-radius: var(--border-radius);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.session-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.session-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-color);
  margin: 0;
}

.session-meta {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.words-count {
  font-weight: 600;
  color: var(--text-color-secondary);
}

.accuracy {
  font-weight: 600;
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
}

.high-accuracy {
  background: var(--green-100);
  color: var(--green-700);
}

.medium-accuracy {
  background: var(--orange-100);
  color: var(--orange-700);
}

.low-accuracy {
  background: var(--red-100);
  color: var(--red-700);
}

.session-progress-bar {
  height: 0.75rem;
}

.error-message {
  margin-bottom: 1rem;
}

/* Loading & No Cards States */
.loading-card,
.no-cards {
  margin-top: 2rem;
}

.loading-content,
.no-cards-content {
  text-align: center;
  padding: 3rem 2rem;
}

.loading-icon {
  font-size: 3rem;
  color: var(--primary-color);
  margin-bottom: 1rem;
}

.success-icon {
  font-size: 4rem;
  color: var(--green-500);
  margin-bottom: 1rem;
}

.loading-content p,
.no-cards-content p {
  color: var(--text-color-secondary);
  margin: 0;
}

.no-cards-content h3 {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-color);
  margin: 0.5rem 0;
}

/* Summary Dialog */
.summary-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.summary-header {
  text-align: center;
}

.summary-icon {
  font-size: 4rem;
  color: var(--green-500);
  margin-bottom: 1rem;
}

.summary-header h2 {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-color);
  margin: 0 0 0.5rem 0;
}

.summary-header p {
  color: var(--text-color-secondary);
  margin: 0;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1.5rem;
  background: var(--surface-section);
  border-radius: var(--border-radius);
  gap: 1rem;
}

.stat-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  background: var(--primary-100);
  color: var(--primary-600);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

.accuracy-icon {
  background: var(--green-100);
  color: var(--green-600);
}

.time-icon {
  background: var(--orange-100);
  color: var(--orange-600);
}

.stat-info {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-color);
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.summary-details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  background: var(--surface-section);
  border-radius: var(--border-radius);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-color-secondary);
}

.detail-label i {
  font-size: 0.875rem;
}

.detail-value {
  font-weight: 600;
  color: var(--text-color);
}

.success-text {
  color: var(--green-600);
}

.danger-text {
  color: var(--red-600);
}

.summary-actions {
  display: flex;
  justify-content: center;
}

.finish-button {
  min-width: 200px;
}

/* Responsive */
@media (max-width: 768px) {
  .study-session-container {
    padding: 1rem;
  }

  .session-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .session-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .summary-stats {
    grid-template-columns: 1fr;
  }

  .summary-details {
    padding: 1rem;
  }

  .detail-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.25rem;
  }
}
</style>
