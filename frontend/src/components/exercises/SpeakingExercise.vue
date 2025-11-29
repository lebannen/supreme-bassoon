<script setup lang="ts">
import {computed, ref} from 'vue'
import Button from 'primevue/button'
import Message from 'primevue/message'
import ProgressBar from 'primevue/progressbar'
import AudioPlayer from '@/components/audio/AudioPlayer.vue'
import AudioRecorder from '@/components/audio/AudioRecorder.vue'

interface SpeakingContent {
  mode: 'repeat' | 'read_aloud' | 'respond'
  targetText: string
  targetLanguage: string
  nativeAudioUrl?: string
  prompt?: string
  promptAudioUrl?: string
  hint?: string
}

interface SpeakingResult {
  transcription: string
  accuracy: number
  isCorrect: boolean
  feedback: string
}

interface Props {
  content: SpeakingContent
}

const props = defineProps<Props>()
const emit = defineEmits<{
  submit: [audio: Blob]
  next: []
}>()

const recorderRef = ref<InstanceType<typeof AudioRecorder> | null>(null)
const showResult = ref(false)
const showHint = ref(false)
const isSubmitting = ref(false)
const result = ref<SpeakingResult | null>(null)
const errorMessage = ref<string | null>(null)

const autoAdvanceTimer = ref<number | null>(null)
const autoAdvanceSeconds = ref(5)

const modeLabel = computed(() => {
  switch (props.content.mode) {
    case 'repeat':
      return 'Listen and repeat:'
    case 'read_aloud':
      return 'Say this phrase:'
    case 'respond':
      return 'Respond to:'
    default:
      return 'Say:'
  }
})

function handleRecorded(blob: Blob) {
  emit('submit', blob)
  isSubmitting.value = true
  errorMessage.value = null
}

function handleRecordingError(message: string) {
  errorMessage.value = message
}

function setResult(data: SpeakingResult) {
  result.value = data
  showResult.value = true
  isSubmitting.value = false
  // Auto-advance disabled - let user read feedback at their own pace
}

function setError(message: string) {
  errorMessage.value = message
  isSubmitting.value = false
}

function startAutoAdvance() {
  autoAdvanceSeconds.value = 5
  autoAdvanceTimer.value = window.setInterval(() => {
    autoAdvanceSeconds.value--
    if (autoAdvanceSeconds.value <= 0) handleNext()
  }, 1000)
}

function stopAutoAdvance() {
  if (autoAdvanceTimer.value) {
    clearInterval(autoAdvanceTimer.value)
    autoAdvanceTimer.value = null
  }
}

function handleNext() {
  stopAutoAdvance()
  emit('next')
}

function tryAgain() {
  showResult.value = false
  result.value = null
  errorMessage.value = null
  recorderRef.value?.clearRecording()
}

function resetExercise() {
  stopAutoAdvance()
  showResult.value = false
  showHint.value = false
  result.value = null
  isSubmitting.value = false
  errorMessage.value = null
  recorderRef.value?.clearRecording()
}

defineExpose({setResult, setError, resetExercise})
</script>

<template>
  <div class="speaking-exercise">
    <!-- Prompt section -->
    <div class="prompt-section">
      <p class="mode-label">{{ modeLabel }}</p>

      <!-- For 'respond' mode, show the prompt -->
      <div v-if="content.mode === 'respond' && content.prompt" class="prompt-text">
        {{ content.prompt }}
      </div>

      <!-- Prompt audio for respond mode -->
      <div v-if="content.mode === 'respond' && content.promptAudioUrl" class="prompt-audio">
        <AudioPlayer :audio-url="content.promptAudioUrl"/>
      </div>

      <!-- Target text to say -->
      <div class="target-text">"{{ content.targetText }}"</div>

      <!-- Native audio example (for repeat and read_aloud modes) -->
      <div v-if="content.nativeAudioUrl && content.mode !== 'respond'" class="native-audio">
        <p class="audio-label">Listen to example:</p>
        <AudioPlayer :audio-url="content.nativeAudioUrl"/>
      </div>
    </div>

    <!-- Error message -->
    <Message v-if="errorMessage" severity="error" :closable="false">
      {{ errorMessage }}
    </Message>

    <!-- Recording section (hidden after result) -->
    <div v-if="!showResult" class="recording-section">
      <AudioRecorder
          ref="recorderRef"
          @recorded="handleRecorded"
          @error="handleRecordingError"
      />

      <div v-if="isSubmitting" class="submitting-indicator">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Checking your pronunciation...</span>
      </div>
    </div>

    <!-- Result section -->
    <div v-if="showResult && result" class="result-section">
      <Message :severity="result.isCorrect ? 'success' : 'warn'" :closable="false">
        {{ result.isCorrect ? 'Great job!' : 'Almost there!' }}
      </Message>

      <div class="comparison">
        <div class="comparison-row">
          <span class="label">Expected:</span>
          <span class="text">"{{ content.targetText }}"</span>
        </div>
        <div class="comparison-row">
          <span class="label">You said:</span>
          <span class="text">"{{ result.transcription }}"</span>
        </div>
      </div>

      <div class="accuracy-bar">
        <span>Accuracy: {{ result.accuracy }}%</span>
        <ProgressBar :value="result.accuracy" :showValue="false"/>
      </div>

      <p v-if="result.feedback" class="feedback">
        {{ result.feedback }}
      </p>
    </div>

    <!-- Hint section -->
    <Message v-if="showHint && content.hint" severity="secondary" :closable="false">
      {{ content.hint }}
    </Message>

    <!-- Actions -->
    <div class="actions-section">
      <template v-if="!showResult">
        <Button
            v-if="content.hint && !showHint"
            label="Hint"
            icon="pi pi-lightbulb"
            text
            @click="showHint = true"
        />
        <div class="flex-grow"></div>
      </template>
      <template v-else>
        <Button
            v-if="!result?.isCorrect"
            label="Try Again"
            icon="pi pi-refresh"
            @click="tryAgain"
        />
        <div class="flex-grow"></div>
        <Button
            label="Next"
            icon="pi pi-arrow-right"
            :severity="result?.isCorrect ? 'primary' : 'secondary'"
            :text="!result?.isCorrect"
            @click="handleNext"
        />
      </template>
    </div>
  </div>
</template>

<style scoped>
.speaking-exercise {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.prompt-section {
  text-align: center;
}

.mode-label {
  color: var(--text-color-secondary);
  margin-bottom: 0.5rem;
}

.prompt-text {
  font-size: 1.1rem;
  margin-bottom: 1rem;
  color: var(--text-color-secondary);
}

.target-text {
  font-size: 1.5rem;
  font-weight: 600;
  padding: 1.5rem;
  background: var(--surface-card);
  border-radius: var(--border-radius);
  border: 2px solid var(--surface-border);
  margin-bottom: 1rem;
}

.native-audio {
  margin-top: 1rem;
}

.audio-label {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  margin-bottom: 0.5rem;
}

.prompt-audio {
  margin-bottom: 1rem;
}

.recording-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.submitting-indicator {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-color-secondary);
}

.result-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.comparison {
  background: var(--surface-card);
  padding: 1rem;
  border-radius: var(--border-radius);
}

.comparison-row {
  display: flex;
  gap: 0.5rem;
  padding: 0.5rem 0;
}

.comparison-row .label {
  color: var(--text-color-secondary);
  min-width: 80px;
  flex-shrink: 0;
}

.comparison-row .text {
  font-weight: 500;
}

.accuracy-bar {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.feedback {
  color: var(--text-color-secondary);
  font-style: italic;
}

.actions-section {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.flex-grow {
  flex-grow: 1;
}
</style>
