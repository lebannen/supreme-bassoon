<template>
  <div class="listening-exercise">
    <div class="audio-section">
      <div class="audio-player-container">
        <audio
          ref="audioPlayer"
          :src="audioUrl"
          controls
          preload="auto"
          class="audio-player"
        >
          Your browser does not support the audio element.
        </audio>
      </div>
      <div class="audio-hint">
        <i class="pi pi-volume-up"></i>
        <span>Listen to the audio and answer the question below</span>
      </div>
    </div>

    <div class="question-section">
      <h2 class="question-text">{{ content.question }}</h2>
    </div>

    <!-- Multiple Choice Questions -->
    <div v-if="questionType === 'multiple_choice'" class="options-section">
      <div
        v-for="option in content.options"
        :key="option.id"
        class="option-card"
        :class="{
          'selected': selectedOption === option.id,
          'correct': showResult && option.id === correctAnswer,
          'incorrect': showResult && selectedOption === option.id && option.id !== correctAnswer,
          'disabled': showResult
        }"
        @click="selectOption(option.id)"
      >
        <div class="option-label">{{ option.id.toUpperCase() }}</div>
        <div class="option-text">{{ option.text }}</div>
        <i
          v-if="showResult && option.id === correctAnswer"
          class="pi pi-check-circle result-icon correct-icon"
        />
        <i
          v-if="showResult && selectedOption === option.id && option.id !== correctAnswer"
          class="pi pi-times-circle result-icon incorrect-icon"
        />
      </div>
    </div>

    <!-- Text Input Questions -->
    <div v-if="questionType === 'text_input'" class="input-section">
      <InputText
        v-model="userAnswer"
        placeholder="Type your answer here..."
        :disabled="showResult"
        class="answer-input"
        @keyup.enter="!showResult && submitAnswer()"
      />
    </div>

    <div v-if="showHint && !showResult" class="hint-section">
      <Message severity="info">
        <div class="hint-content">
          <strong>Hint:</strong> {{ hint }}
        </div>
      </Message>
    </div>

    <div v-if="showResult && feedback" class="feedback-section">
      <Message :severity="isCorrect ? 'success' : 'warn'">
        <div class="feedback-content">
          {{ feedback }}
        </div>
      </Message>
    </div>

    <div class="actions-section">
      <Button
        v-if="!showResult && hint"
        label="Show Hint"
        icon="pi pi-lightbulb"
        text
        @click="toggleHint"
      />
      <Button
        v-if="!showResult"
        label="Submit Answer"
        icon="pi pi-check"
        :disabled="!canSubmit"
        @click="submitAnswer"
      />
      <Button
        v-else
        label="Try Again"
        icon="pi pi-refresh"
        @click="resetExercise"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import Button from 'primevue/button'
import Message from 'primevue/message'
import InputText from 'primevue/inputtext'

interface ListeningOption {
  id: string
  text: string
}

interface ListeningContent {
  audioUrl: string
  question: string
  questionType: 'multiple_choice' | 'text_input'
  options?: ListeningOption[]
  hint?: string
}

interface Props {
  content: ListeningContent
}

const props = defineProps<Props>()
const emit = defineEmits<{
  submit: [response: { selectedOption?: string; answer?: string }]
}>()

const audioPlayer = ref<HTMLAudioElement | null>(null)
const selectedOption = ref<string | null>(null)
const userAnswer = ref('')
const showResult = ref(false)
const isCorrect = ref(false)
const feedback = ref('')
const showHint = ref(false)
const correctAnswer = ref<string>('')

const questionType = computed(() => props.content.questionType || 'multiple_choice')
const audioUrl = computed(() => props.content.audioUrl)
const hint = computed(() => props.content.hint || '')

const canSubmit = computed(() => {
  if (questionType.value === 'multiple_choice') {
    return selectedOption.value !== null
  } else {
    return userAnswer.value.trim().length > 0
  }
})

function selectOption(optionId: string) {
  if (showResult.value) return
  selectedOption.value = optionId
}

function toggleHint() {
  showHint.value = !showHint.value
}

function submitAnswer() {
  if (questionType.value === 'multiple_choice') {
    emit('submit', { selectedOption: selectedOption.value || '' })
  } else {
    emit('submit', { answer: userAnswer.value })
  }
}

function resetExercise() {
  selectedOption.value = null
  userAnswer.value = ''
  showResult.value = false
  isCorrect.value = false
  feedback.value = ''
  showHint.value = false
  correctAnswer.value = ''

  // Reset audio player
  if (audioPlayer.value) {
    audioPlayer.value.currentTime = 0
    audioPlayer.value.pause()
  }
}

function setResult(result: { isCorrect: boolean; feedback: string; userResponse?: any; correctAnswers?: any }) {
  showResult.value = true
  isCorrect.value = result.isCorrect
  feedback.value = result.feedback

  if (result.correctAnswers?.correctAnswer) {
    correctAnswer.value = result.correctAnswers.correctAnswer
  }

  if (result.userResponse) {
    if (result.userResponse.selectedOption) {
      selectedOption.value = result.userResponse.selectedOption
    } else if (result.userResponse.answer) {
      userAnswer.value = result.userResponse.answer
    }
  }
}

defineExpose({
  setResult
})
</script>

<style scoped>
.listening-exercise {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.audio-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  background: var(--surface-ground);
  border-radius: 8px;
}

.audio-player-container {
  display: flex;
  justify-content: center;
}

.audio-player {
  width: 100%;
  max-width: 500px;
}

.audio-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  color: var(--text-color-secondary);
  font-size: 0.95rem;
}

.audio-hint i {
  color: var(--primary-color);
  font-size: 1.2rem;
}

.question-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.question-text {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
  margin: 0;
}

.options-section {
  display: grid;
  gap: 1rem;
}

.option-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: var(--surface-card);
  border: 2px solid var(--surface-border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.option-card:hover:not(.disabled) {
  background: var(--surface-hover);
  border-color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.option-card.selected {
  background: #3b82f6 !important;
  color: white !important;
  border-color: #2563eb !important;
  border-width: 3px;
  transform: scale(1.02);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.3);
}

.option-card.correct {
  background: var(--green-50);
  border-color: var(--green-500);
  border-width: 3px;
}

.option-card.incorrect {
  background: var(--red-50);
  border-color: var(--red-500);
  border-width: 3px;
}

.option-card.disabled {
  cursor: not-allowed;
}

.option-label {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: var(--primary-color);
  color: white;
  border-radius: 50%;
  font-weight: 600;
  font-size: 0.875rem;
}

.option-card.selected .option-label {
  background: white;
  color: #3b82f6;
}

.option-card.correct .option-label {
  background: var(--green-500);
}

.option-card.incorrect .option-label {
  background: var(--red-500);
}

.option-text {
  flex: 1;
  font-size: 1rem;
  font-weight: 500;
}

.option-card.selected .option-text {
  color: white !important;
}

.result-icon {
  font-size: 1.5rem;
  margin-left: auto;
}

.correct-icon {
  color: var(--green-500);
}

.incorrect-icon {
  color: var(--red-500);
}

.input-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.answer-input {
  width: 100%;
  font-size: 1rem;
  padding: 0.75rem;
}

.hint-section,
.feedback-section {
  margin-top: -0.5rem;
}

.hint-content,
.feedback-content {
  font-size: 1rem;
  line-height: 1.6;
}

.actions-section {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .audio-player {
    width: 100%;
  }

  .actions-section {
    justify-content: stretch;
  }

  .actions-section button {
    flex: 1;
  }
}
</style>
