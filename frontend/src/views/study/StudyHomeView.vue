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
const selectedSource = ref<SessionSourceType>('DUE_REVIEW')
const sessionSize = ref(20)
const includeNewWords = ref(true)

const sourceOptions = [
  {
    value: 'DUE_REVIEW' as SessionSourceType,
    label: 'Due for Review',
    description: 'Study words from your spaced repetition schedule.',
    icon: 'pi-clock'
  },
  {
    value: 'VOCABULARY' as SessionSourceType,
    label: 'All Vocabulary',
    description: 'Study words from your entire collection.',
    icon: 'pi-book'
  },
]

const canStartSession = computed(() => selectedSource.value !== 'DUE_REVIEW' || (studyStore.dueWords?.totalDue ?? 0) > 0)
const maxSessionSize = computed(() => selectedSource.value === 'DUE_REVIEW' ? Math.min(50, studyStore.dueWords?.totalDue ?? 50) : 50)

onMounted(async () => {
  await Promise.all([studyStore.loadActiveSession(), studyStore.loadDueWords()])
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
  if (success) router.push('/study/session')
}

function handleAbandonSession() {
  confirm.require({
    message: 'Are you sure you want to abandon this session? Your progress will be lost.',
    header: 'Abandon Session',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      await studyStore.abandonSession()
      await studyStore.loadDueWords()
    },
  })
}

const formatDueInfo = () => {
  if (!studyStore.dueWords) return ''
  const { overdue, dueToday, dueSoon } = studyStore.dueWords
  return [overdue > 0 && `${overdue} overdue`, dueToday > 0 && `${dueToday} today`, dueSoon > 0 && `${dueSoon} soon`].filter(Boolean).join(', ')
}
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header"><h1 class="flex items-center gap-md"><i
        class="pi pi-graduation-cap text-3xl icon-primary"></i> Study Mode</h1></div>
    <Message v-if="studyStore.error" severity="error">{{ studyStore.error }}</Message>

    <Card v-if="studyStore.isSessionActive" class="border-2 border-primary bg-primary-50 dark:bg-primary-900">
      <template #title>
        <div class="flex items-center gap-sm"><i class="pi pi-play-circle"></i> Active Session</div>
      </template>
      <template #content>
        <div class="content-area-lg">
          <div class="flex gap-xl">
            <div class="flex flex-col gap-xs"><span class="text-sm text-secondary uppercase">Progress</span><span
                class="text-2xl font-bold">{{
                studyStore.activeSession?.wordsCompleted
              }} / {{ studyStore.activeSession?.totalWords }}</span></div>
            <div class="flex flex-col gap-xs"><span class="text-sm text-secondary uppercase">Accuracy</span><span
                class="text-2xl font-bold">{{ Math.round(studyStore.activeSession?.stats.accuracy || 0) }}%</span></div>
          </div>
          <ProgressBar :value="studyStore.progressPercentage" style="height: 1rem"/>
          <div class="flex gap-md">
            <Button label="Resume Session" icon="pi pi-play" @click="router.push('/study/session')" size="large"/>
            <Button label="Abandon" icon="pi pi-times" severity="danger" @click="handleAbandonSession" outlined/>
          </div>
        </div>
      </template>
    </Card>

    <Card v-if="!loading && studyStore.dueWords" class="bg-primary text-primary-contrast">
      <template #content>
        <div class="flex items-center gap-lg">
          <div class="stat-icon-lg bg-primary-400 dark:bg-primary-600"><i class="pi pi-clock"></i></div>
          <div>
            <h3 class="text-4xl font-bold m-0">{{ studyStore.dueWords.totalDue }}</h3>
            <p class="text-lg m-0 opacity-90">Words Due for Review</p>
            <span v-if="studyStore.dueWords.totalDue > 0" class="text-sm opacity-80">{{ formatDueInfo() }}</span>
          </div>
        </div>
      </template>
    </Card>

    <Card v-if="!studyStore.isSessionActive && !loading">
      <template #title>Start New Study Session</template>
      <template #content>
        <div class="content-area-lg">
          <div class="content-area">
            <label class="font-semibold">Study Source</label>
            <div v-for="option in sourceOptions" :key="option.value" class="source-option"
                 :class="{ 'selected': selectedSource === option.value }" @click="selectedSource = option.value">
              <RadioButton v-model="selectedSource" :value="option.value" :input-id="option.value"/>
              <div class="flex-1"><label :for="option.value" class="font-semibold cursor-pointer">{{
                  option.label
                }}</label>
                <p class="text-secondary m-0">{{ option.description }}</p></div>
            </div>
          </div>
          <div class="content-area">
            <label class="font-semibold">Session Size: {{ sessionSize }} words</label>
            <InputNumber v-model="sessionSize" :min="5" :max="maxSessionSize" :step="5" show-buttons
                         button-layout="horizontal" class="w-full"/>
          </div>
          <div class="flex items-center gap-sm">
            <Checkbox v-model="includeNewWords" :binary="true" input-id="includeNew"/>
            <label for="includeNew" class="font-medium cursor-pointer">Include new words</label>
          </div>
          <Button label="Start Session" icon="pi pi-play" @click="handleStartSession" :loading="startingSession"
                  :disabled="!canStartSession" size="large" class="w-full"/>
          <Message v-if="!canStartSession && selectedSource === 'DUE_REVIEW'" severity="info">No words are due for
            review right now.
          </Message>
        </div>
      </template>
    </Card>

    <Card v-if="!loading && !studyStore.dueWords?.totalDue && !studyStore.isSessionActive">
      <template #content>
        <div class="empty-state">
          <i class="pi pi-check-circle text-6xl text-success"></i>
          <h3>All Caught Up!</h3>
          <p class="text-secondary">You have no words due for review.</p>
          <Button label="Study Anyway" icon="pi pi-book" @click="selectedSource = 'VOCABULARY'" outlined/>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.source-option {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.source-option:hover, .source-option.selected {
  border-color: var(--primary-color);
  background-color: var(--surface-hover);
}

.dark-theme .source-option.selected {
  background-color: var(--p-primary-900);
}
</style>
