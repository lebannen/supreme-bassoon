<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStudyStore } from '@/stores/study'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import RadioButton from 'primevue/radiobutton'
import InputNumber from 'primevue/inputnumber'
import Checkbox from 'primevue/checkbox'
import ProgressBar from 'primevue/progressbar'
import { useConfirm } from 'primevue/useconfirm'
import type { SessionSourceType } from '@/types/study'

const router = useRouter()
const studyStore = useStudyStore()
const confirm = useConfirm()

const loading = ref(true)
const startingSession = ref(false)

// Form state
const selectedSource = ref<SessionSourceType>('DUE_REVIEW')
const sessionSize = ref(20)
const includeNewWords = ref(true)

const sourceOptions = [
  {
    value: 'DUE_REVIEW' as SessionSourceType,
    label: 'Due for Review',
    description: 'Study words that are due for review based on the spaced repetition schedule',
    icon: 'pi-clock'
  },
  {
    value: 'VOCABULARY' as SessionSourceType,
    label: 'All Vocabulary',
    description: 'Study words from your entire vocabulary collection',
    icon: 'pi-book'
  }
]

const canStartSession = computed(() => {
  if (selectedSource.value === 'DUE_REVIEW') {
    return studyStore.dueWords && studyStore.dueWords.totalDue > 0
  }
  return true
})

const maxSessionSize = computed(() => {
  if (selectedSource.value === 'DUE_REVIEW' && studyStore.dueWords) {
    return Math.min(50, studyStore.dueWords.totalDue)
  }
  return 50
})

onMounted(async () => {
  await Promise.all([
    studyStore.loadActiveSession(),
    studyStore.loadDueWords()
  ])
  loading.value = false
})

async function handleStartSession() {
  startingSession.value = true

  const success = await studyStore.startSession({
    source: selectedSource.value,
    sessionSize: sessionSize.value,
    includeNewWords: includeNewWords.value
  })

  startingSession.value = false

  if (success) {
    router.push('/study/session')
  }
}

function handleResumeSession() {
  router.push('/study/session')
}

function handleAbandonSession() {
  confirm.require({
    message: 'Are you sure you want to abandon the current study session? Your progress will be lost.',
    header: 'Abandon Session',
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Cancel',
    acceptLabel: 'Abandon',
    accept: async () => {
      await studyStore.abandonSession()
      await studyStore.loadDueWords()
    }
  })
}

function formatDueInfo() {
  if (!studyStore.dueWords) return ''
  const { overdue, dueToday, dueSoon } = studyStore.dueWords
  const parts = []
  if (overdue > 0) parts.push(`${overdue} overdue`)
  if (dueToday > 0) parts.push(`${dueToday} today`)
  if (dueSoon > 0) parts.push(`${dueSoon} soon`)
  return parts.join(', ')
}
</script>

<template>
  <div class="study-home-container">
    <div class="study-home-content">
      <div class="header">
        <h1 class="study-title">
          <i class="pi pi-graduation-cap"></i>
          Study Mode
        </h1>
      </div>

      <Message v-if="studyStore.error" severity="error" :closable="false" class="error-message">
        {{ studyStore.error }}
      </Message>

      <!-- Active Session Card -->
      <Card v-if="studyStore.isSessionActive" class="active-session-card">
        <template #title>
          <div class="active-session-title">
            <i class="pi pi-play-circle"></i>
            Active Session in Progress
          </div>
        </template>
        <template #content>
          <div class="active-session-content">
            <div class="session-stats">
              <div class="stat-item">
                <span class="stat-label">Progress</span>
                <span class="stat-value">
                  {{ studyStore.activeSession?.wordsCompleted }} / {{ studyStore.activeSession?.totalWords }}
                </span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Accuracy</span>
                <span class="stat-value">{{ Math.round(studyStore.activeSession?.stats.accuracy || 0) }}%</span>
              </div>
            </div>

            <ProgressBar
              :value="studyStore.progressPercentage"
              class="session-progress"
            />

            <div class="session-actions">
              <Button
                label="Resume Session"
                icon="pi pi-play"
                @click="handleResumeSession"
                size="large"
              />
              <Button
                label="Abandon Session"
                icon="pi pi-times"
                severity="danger"
                outlined
                @click="handleAbandonSession"
              />
            </div>
          </div>
        </template>
      </Card>

      <!-- Due Words Summary -->
      <div v-if="!loading && studyStore.dueWords" class="due-words-summary">
        <Card class="due-card">
          <template #content>
            <div class="due-content">
              <div class="due-icon-wrapper">
                <i class="pi pi-clock"></i>
              </div>
              <div class="due-info">
                <h3>{{ studyStore.dueWords.totalDue }}</h3>
                <p>Words Due for Review</p>
                <span v-if="studyStore.dueWords.totalDue > 0" class="due-breakdown">
                  {{ formatDueInfo() }}
                </span>
              </div>
            </div>
          </template>
        </Card>
      </div>

      <!-- New Session Form -->
      <Card v-if="!studyStore.isSessionActive && !loading" class="new-session-card">
        <template #title>Start New Study Session</template>
        <template #content>
          <div class="session-form">
            <!-- Source Selection -->
            <div class="form-section">
              <label class="form-label">Study Source</label>
              <div class="source-options">
                <div
                  v-for="option in sourceOptions"
                  :key="option.value"
                  class="source-option"
                  :class="{ selected: selectedSource === option.value }"
                  @click="selectedSource = option.value"
                >
                  <RadioButton
                    v-model="selectedSource"
                    :value="option.value"
                    :input-id="option.value"
                  />
                  <div class="option-content">
                    <div class="option-header">
                      <i class="pi" :class="option.icon"></i>
                      <label :for="option.value" class="option-label">{{ option.label }}</label>
                    </div>
                    <p class="option-description">{{ option.description }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Session Size -->
            <div class="form-section">
              <label class="form-label">
                Session Size: {{ sessionSize }} words
              </label>
              <InputNumber
                v-model="sessionSize"
                :min="5"
                :max="maxSessionSize"
                :step="5"
                show-buttons
                button-layout="horizontal"
                class="session-size-input"
              >
                <template #incrementbuttonicon>
                  <span class="pi pi-plus"></span>
                </template>
                <template #decrementbuttonicon>
                  <span class="pi pi-minus"></span>
                </template>
              </InputNumber>
              <small class="form-help">
                Choose how many words to study in this session (5-{{ maxSessionSize }})
              </small>
            </div>

            <!-- Include New Words -->
            <div class="form-section">
              <div class="checkbox-field">
                <Checkbox
                  v-model="includeNewWords"
                  :binary="true"
                  input-id="includeNew"
                />
                <label for="includeNew" class="checkbox-label">
                  Include new words (not yet studied)
                </label>
              </div>
              <small class="form-help">
                Mix in new vocabulary words along with review words
              </small>
            </div>

            <!-- Start Button -->
            <div class="form-actions">
              <Button
                label="Start Session"
                icon="pi pi-play"
                @click="handleStartSession"
                :loading="startingSession"
                :disabled="!canStartSession"
                size="large"
                class="start-button"
              />
              <Message
                v-if="!canStartSession && selectedSource === 'DUE_REVIEW'"
                severity="info"
                :closable="false"
                class="no-due-message"
              >
                No words are due for review right now. Try studying from all vocabulary or check back later!
              </Message>
            </div>
          </div>
        </template>
      </Card>

      <!-- Empty State -->
      <Card v-if="!loading && !studyStore.dueWords?.totalDue && !studyStore.isSessionActive" class="empty-state">
        <template #content>
          <div class="empty-content">
            <i class="pi pi-check-circle empty-icon"></i>
            <h3>All Caught Up!</h3>
            <p>You have no words due for review right now. Keep up the great work!</p>
            <Button
              label="Study Anyway"
              icon="pi pi-book"
              @click="selectedSource = 'VOCABULARY'"
              outlined
            />
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
.study-home-container {
  min-height: 100vh;
  background: var(--surface-ground);
  padding: 2rem 1rem;
}

.study-home-content {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.header {
  margin-bottom: 1rem;
}

.study-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--text-color);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.study-title i {
  color: var(--primary-color);
  font-size: 2rem;
}

.error-message {
  margin-bottom: 1rem;
}

/* Active Session Card */
.active-session-card {
  border: 2px solid var(--primary-color);
  background: var(--primary-50);
}

.active-session-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--primary-color);
}

.active-session-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.session-stats {
  display: flex;
  gap: 2rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-color);
}

.session-progress {
  height: 1rem;
}

.session-actions {
  display: flex;
  gap: 1rem;
}

/* Due Words Summary */
.due-words-summary {
  margin-bottom: 0.5rem;
}

.due-card {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-600) 100%);
  color: white;
}

.due-content {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.due-icon-wrapper {
  width: 4rem;
  height: 4rem;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.due-icon-wrapper i {
  font-size: 2rem;
  color: white;
}

.due-info h3 {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 0 0 0.25rem 0;
  color: white;
}

.due-info p {
  font-size: 1.125rem;
  margin: 0 0 0.5rem 0;
  color: rgba(255, 255, 255, 0.95);
}

.due-breakdown {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
}

/* New Session Form */
.session-form {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-label {
  font-weight: 600;
  font-size: 1.125rem;
  color: var(--text-color);
}

.source-options {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.source-option {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.25rem;
  border: 2px solid var(--surface-border);
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: all 0.2s;
}

.source-option:hover {
  border-color: var(--primary-color);
  background: var(--primary-50);
}

.source-option.selected {
  border-color: var(--primary-color);
  background: var(--primary-50);
}

.option-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.option-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.option-header i {
  color: var(--primary-color);
  font-size: 1.25rem;
}

.option-label {
  font-weight: 600;
  font-size: 1.125rem;
  color: var(--text-color);
  cursor: pointer;
}

.option-description {
  margin: 0;
  color: var(--text-color-secondary);
  line-height: 1.5;
}

.session-size-input {
  width: 100%;
}

.form-help {
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.checkbox-field {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.checkbox-label {
  font-weight: 500;
  color: var(--text-color);
  cursor: pointer;
}

.form-actions {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.start-button {
  width: 100%;
}

.no-due-message {
  width: 100%;
}

/* Empty State */
.empty-state {
  margin-top: 2rem;
}

.empty-content {
  text-align: center;
  padding: 3rem 2rem;
}

.empty-icon {
  font-size: 4rem;
  color: var(--green-500);
  margin-bottom: 1rem;
}

.empty-content h3 {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 0.5rem;
}

.empty-content p {
  color: var(--text-color-secondary);
  margin-bottom: 2rem;
  font-size: 1rem;
}

/* Responsive */
@media (max-width: 768px) {
  .study-home-container {
    padding: 1rem;
  }

  .study-title {
    font-size: 2rem;
  }

  .session-stats {
    flex-direction: column;
    gap: 1rem;
  }

  .session-actions {
    flex-direction: column;
  }

  .due-content {
    flex-direction: column;
    text-align: center;
  }

  .source-option {
    padding: 1rem;
  }
}
</style>
