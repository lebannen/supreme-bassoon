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
  if (!studyStore.isSessionActive) {
    await studyStore.loadActiveSession()
    if (!studyStore.isSessionActive) {
      router.push('/study')
      return
    }
  }
  if (!studyStore.currentCard) {
    await studyStore.fetchNextCard()
  }
})

watch(() => studyStore.sessionSummary, summary => {
  if (summary) showSummaryDialog.value = true
})
watch(() => studyStore.currentCard, () => flashCardRef.value?.resetFlip())

const handleAnswer = async (correct: boolean) => await studyStore.submitAnswer(correct)

function handleExitSession() {
  confirm.require({
    message: 'Are you sure you want to exit? You can resume this session later.',
    header: 'Exit Session',
    icon: 'pi pi-sign-out',
    accept: () => router.push('/study'),
  })
}

function handleFinishSummary() {
  studyStore.clearSummary()
  router.push('/study')
}

const formatDuration = (duration: string) => {
  const match = duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/)
  if (!match) return duration
  const [, h, m, s] = match.map(v => parseInt(v || '0'))
  return [h > 0 && `${h}h`, m > 0 && `${m}m`, (s > 0 || (!h && !m)) && `${s}s`].filter(Boolean).join(' ')
}
</script>

<template>
  <div class="view-container content-area-lg">
    <div v-if="studyStore.isSessionActive" class="flex justify-between items-center p-lg bg-surface-card rounded-lg">
      <div class="content-area">
        <h2 class="text-2xl font-bold m-0">Study Session</h2>
        <div class="flex items-center gap-lg">
          <span class="font-semibold text-secondary">{{
              studyStore.activeSession?.wordsCompleted
            }} / {{ studyStore.activeSession?.totalWords }} words</span>
          <Tag :value="`${Math.round(studyStore.activeSession?.stats.accuracy || 0)}% accuracy`" severity="contrast"/>
        </div>
      </div>
      <Button label="Exit" icon="pi pi-sign-out" severity="secondary" @click="handleExitSession"/>
    </div>

    <ProgressBar v-if="studyStore.isSessionActive" :value="studyStore.progressPercentage" style="height: 0.75rem"/>
    <Message v-if="studyStore.error" severity="error">{{ studyStore.error }}</Message>

    <FlashCard v-if="studyStore.currentCard" ref="flashCardRef" :word="studyStore.currentCard.word"
               :progress="studyStore.currentCard.progress" :srs-info="studyStore.currentCard.srsInfo"
               :loading="studyStore.loading" @answer="handleAnswer"/>
    <Card v-else-if="studyStore.loading" class="mt-xl">
      <template #content>
        <div class="loading-state"><i class="pi pi-spin pi-spinner text-3xl text-primary"></i>
          <p>Loading next card...</p></div>
      </template>
    </Card>
    <Card v-else class="mt-xl">
      <template #content>
        <div class="loading-state"><i class="pi pi-check-circle text-4xl text-success"></i>
          <h3 class="text-2xl font-semibold">Session Complete!</h3>
          <p>Preparing your summary...</p></div>
      </template>
    </Card>

    <Dialog v-model:visible="showSummaryDialog" header="Session Complete!" :modal="true" :closable="false"
            style="width: 600px" :breakpoints="{ '960px': '75vw', '640px': '90vw' }">
      <div v-if="studyStore.sessionSummary" class="content-area-lg">
        <div class="text-center"><i class="pi pi-check-circle text-4xl mb-md text-success"></i>
          <h2 class="text-3xl font-bold">Great job!</h2>
          <p class="text-secondary">You completed your study session.</p></div>
        <div class="summary-stats">
          <div class="stat">
            <div class="stat-icon-lg stat-icon-blue"><i class="pi pi-book"></i></div>
            <div class="text-center"><span class="stat-value">{{
                studyStore.sessionSummary.stats.totalWords
              }}</span><span class="stat-label">Words</span></div>
          </div>
          <div class="stat">
            <div class="stat-icon-lg stat-icon-purple"><i class="pi pi-chart-line"></i></div>
            <div class="text-center"><span class="stat-value">{{
                Math.round(studyStore.sessionSummary.stats.accuracy)
              }}%</span><span class="stat-label">Accuracy</span></div>
          </div>
          <div class="stat">
            <div class="stat-icon-lg stat-icon-yellow"><i class="pi pi-clock"></i></div>
            <div class="text-center"><span class="stat-value">{{
                formatDuration(studyStore.sessionSummary.duration)
              }}</span><span class="stat-label">Duration</span></div>
          </div>
        </div>
        <div class="p-lg bg-surface-section rounded-lg content-area">
          <div class="flex justify-between items-center"><span class="flex items-center gap-sm text-secondary"><i
              class="pi pi-check"></i>Correct</span><span
              class="font-semibold text-success">{{ studyStore.sessionSummary.stats.correctAttempts }}</span></div>
          <div class="flex justify-between items-center"><span class="flex items-center gap-sm text-secondary"><i
              class="pi pi-times"></i>Incorrect</span><span
              class="font-semibold text-error">{{ studyStore.sessionSummary.stats.incorrectAttempts }}</span></div>
          <div class="flex justify-between items-center"><span class="flex items-center gap-sm text-secondary"><i
              class="pi pi-arrow-up"></i>Advanced</span><span
              class="font-semibold">{{ studyStore.sessionSummary.srsUpdates.wordsAdvanced }}</span></div>
          <div v-if="studyStore.sessionSummary.srsUpdates.wordsReset > 0" class="flex justify-between items-center">
            <span class="flex items-center gap-sm text-secondary"><i class="pi pi-refresh"></i>Reset</span><span
              class="font-semibold">{{ studyStore.sessionSummary.srsUpdates.wordsReset }}</span></div>
        </div>
        <div class="text-center">
          <Button label="Done" icon="pi pi-check" @click="handleFinishSummary" size="large"/>
        </div>
      </div>
    </Dialog>
  </div>
</template>
