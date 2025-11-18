<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useStudyStore} from '@/stores/study'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import RadioButton from 'primevue/radiobutton'
import InputNumber from 'primevue/inputnumber'
import Checkbox from 'primevue/checkbox'
import ProgressBar from 'primevue/progressbar'
import {useConfirm} from 'primevue/useconfirm'
import type {SessionSourceType} from '@/types/study'

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
    icon: 'pi-clock',
  },
  {
    value: 'VOCABULARY' as SessionSourceType,
    label: 'All Vocabulary',
    description: 'Study words from your entire vocabulary collection',
    icon: 'pi-book',
  },
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
  await Promise.all([studyStore.loadActiveSession(), studyStore.loadDueWords()])
  loading.value = false
})

async function handleStartSession() {
  startingSession.value = true

  const success = await studyStore.startSession({
    source: selectedSource.value,
    sessionSize: sessionSize.value,
    includeNewWords: includeNewWords.value,
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
    message:
        'Are you sure you want to abandon the current study session? Your progress will be lost.',
    header: 'Abandon Session',
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Cancel',
    acceptLabel: 'Abandon',
    accept: async () => {
      await studyStore.abandonSession()
      await studyStore.loadDueWords()
    },
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
  <div class="page-container-with-padding">
    <div class="view-container content-area-lg flex flex-col gap-lg">
      <div class="page-header">
        <h1 class="flex items-center gap-md">
          <i class="pi pi-graduation-cap text-3xl icon-primary"></i>
          Study Mode
        </h1>
      </div>

      <Message v-if="studyStore.error" severity="error" :closable="false" class="mb-lg">
        {{ studyStore.error }}
      </Message>

      <!-- Active Session Card -->
      <Card v-if="studyStore.isSessionActive" class="active-session-card">
        <template #title>
          <div class="flex items-center gap-sm text-primary">
            <i class="pi pi-play-circle"></i>
            Active Session in Progress
          </div>
        </template>
        <template #content>
          <div class="flex flex-col gap-lg">
            <div class="flex gap-xl">
              <div class="flex flex-col gap-xs">
                <span class="text-sm text-secondary text-uppercase">Progress</span>
                <span class="text-2xl font-bold text-primary">
                  {{ studyStore.activeSession?.wordsCompleted }} /
                  {{ studyStore.activeSession?.totalWords }}
                </span>
              </div>
              <div class="flex flex-col gap-xs">
                <span class="text-sm text-secondary text-uppercase">Accuracy</span>
                <span class="text-2xl font-bold text-primary"
                >{{ Math.round(studyStore.activeSession?.stats.accuracy || 0) }}%</span
                >
              </div>
            </div>

            <ProgressBar :value="studyStore.progressPercentage" class="session-progress-bar"/>

            <div class="flex gap-md">
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
      <div v-if="!loading && studyStore.dueWords" class="mb-sm">
        <Card class="due-card">
          <template #content>
            <div class="flex items-center gap-lg">
              <div class="due-icon-wrapper">
                <i class="pi pi-clock"></i>
              </div>
              <div>
                <h3 class="text-4xl font-bold due-count">{{ studyStore.dueWords.totalDue }}</h3>
                <p class="text-lg due-label">Words Due for Review</p>
                <span v-if="studyStore.dueWords.totalDue > 0" class="text-sm due-info">
                  {{ formatDueInfo() }}
                </span>
              </div>
            </div>
          </template>
        </Card>
      </div>

      <!-- New Session Form -->
      <Card v-if="!studyStore.isSessionActive && !loading">
        <template #title>Start New Study Session</template>
        <template #content>
          <div class="flex flex-col gap-xl">
            <!-- Source Selection -->
            <div class="flex flex-col gap-md">
              <label class="text-lg font-semibold text-primary">Study Source</label>
              <div class="flex flex-col gap-md">
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
                  <div class="flex-1 flex flex-col gap-sm">
                    <div class="flex items-center gap-sm">
                      <i class="pi text-xl icon-primary" :class="option.icon"></i>
                      <label
                          :for="option.value"
                          class="text-lg font-semibold text-primary cursor-pointer"
                      >{{ option.label }}</label
                      >
                    </div>
                    <p class="text-secondary leading-normal m-0">
                      {{ option.description }}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Session Size -->
            <div class="flex flex-col gap-md">
              <label class="text-lg font-semibold text-primary">
                Session Size: {{ sessionSize }} words
              </label>
              <InputNumber
                v-model="sessionSize"
                :min="5"
                :max="maxSessionSize"
                :step="5"
                show-buttons
                button-layout="horizontal"
                class="w-full"
              >
                <template #incrementbuttonicon>
                  <span class="pi pi-plus"></span>
                </template>
                <template #decrementbuttonicon>
                  <span class="pi pi-minus"></span>
                </template>
              </InputNumber>
              <small class="text-sm text-secondary">
                Choose how many words to study in this session (5-{{ maxSessionSize }})
              </small>
            </div>

            <!-- Include New Words -->
            <div class="flex flex-col gap-md">
              <div class="flex items-center gap-sm">
                <Checkbox v-model="includeNewWords" :binary="true" input-id="includeNew"/>
                <label for="includeNew" class="font-medium text-primary cursor-pointer">
                  Include new words (not yet studied)
                </label>
              </div>
              <small class="text-sm text-secondary">
                Mix in new vocabulary words along with review words
              </small>
            </div>

            <!-- Start Button -->
            <div class="flex flex-col gap-md">
              <Button
                label="Start Session"
                icon="pi pi-play"
                @click="handleStartSession"
                :loading="startingSession"
                :disabled="!canStartSession"
                size="large"
                class="w-full"
              />
              <Message
                v-if="!canStartSession && selectedSource === 'DUE_REVIEW'"
                severity="info"
                :closable="false"
              >
                No words are due for review right now. Try studying from all vocabulary or check
                back later!
              </Message>
            </div>
          </div>
        </template>
      </Card>

      <!-- Empty State -->
      <Card
          v-if="!loading && !studyStore.dueWords?.totalDue && !studyStore.isSessionActive"
          class="mt-xl"
      >
        <template #content>
          <div class="empty-state">
            <i class="pi pi-check-circle text-6xl icon-success"></i>
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
/* Progress bar */
.session-progress-bar {
  height: 1rem;
}

/* Active Session Card */
.active-session-card {
  border: 2px solid var(--p-primary-color);
  background: var(--p-primary-50);
}

/* Due Words Card */
.due-card {
  background: linear-gradient(135deg, var(--p-primary-color) 0%, var(--p-primary-600) 100%);
  color: var(--p-primary-contrast-color);
}

.due-count {
  margin: 0 0 0.25rem 0;
  color: var(--p-primary-contrast-color);
}

.due-label {
  margin: 0 0 0.5rem 0;
  color: var(--p-primary-contrast-color);
  opacity: 0.95;
}

.due-info {
  color: var(--p-primary-contrast-color);
  opacity: 0.8;
}

.due-icon-wrapper {
  width: 4rem;
  height: 4rem;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.due-icon-wrapper i {
  font-size: 2rem;
  color: var(--p-primary-contrast-color);
}

/* Source Option */
.source-option {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
  padding: var(--spacing-xl);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.source-option:hover,
.source-option.selected {
  border-color: var(--p-primary-color);
  background: var(--p-primary-50);
}

/* Responsive */
@media (max-width: 768px) {
  .source-option {
    padding: var(--spacing-md);
  }
}
</style>
