<script setup lang="ts">
import {computed, ref} from 'vue'
import Button from 'primevue/button'
import Message from 'primevue/message'
import InputText from 'primevue/inputtext'
import AudioPlayer from '@/components/audio/AudioPlayer.vue'

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
  next: []
}>()

const selectedOption = ref<string | null>(null)
const userAnswer = ref('')
const showResult = ref(false)
const isCorrect = ref(false)
const feedback = ref('')
const showHint = ref(false)
const correctAnswer = ref<string>('')
const autoAdvanceTimer = ref<any>(null)
const autoAdvanceSeconds = ref(5)

const questionType = computed(() => props.content.questionType || 'multiple_choice')
const canSubmit = computed(() => {
  return questionType.value === 'multiple_choice'
      ? selectedOption.value !== null
      : userAnswer.value.trim().length > 0
})

function selectOption(optionId: string) {
  if (showResult.value) return
  selectedOption.value = optionId
}

function submitAnswer() {
  const response = questionType.value === 'multiple_choice'
      ? {selectedOption: selectedOption.value || ''}
      : {answer: userAnswer.value}
  emit('submit', response)
}

function startAutoAdvance() {
  autoAdvanceSeconds.value = 5
  autoAdvanceTimer.value = setInterval(() => {
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

function resetExercise() {
  stopAutoAdvance()
  selectedOption.value = null
  userAnswer.value = ''
  showResult.value = false
  isCorrect.value = false
  feedback.value = ''
  showHint.value = false
  correctAnswer.value = ''
}

function setResult(result: { isCorrect: boolean; feedback: string; userResponse?: any; correctAnswers?: any }) {
  showResult.value = true
  isCorrect.value = result.isCorrect
  feedback.value = result.feedback
  if (result.correctAnswers?.correctAnswer) {
    correctAnswer.value = result.correctAnswers.correctAnswer
  }
  if (result.userResponse) {
    selectedOption.value = result.userResponse.selectedOption || null
    userAnswer.value = result.userResponse.answer || ''
  }
  if (result.isCorrect) {
    startAutoAdvance()
  }
}

defineExpose({setResult})
</script>

<template>
  <div class="listening-exercise">
    <AudioPlayer :audio-url="content.audioUrl"/>

    <div class="p-4 bg-surface-card rounded-lg">
      <h2 class="text-lg font-bold">{{ content.question }}</h2>
    </div>

    <!-- Multiple Choice -->
    <div v-if="questionType === 'multiple_choice'" class="options-grid">
      <div
          v-for="option in content.options"
          :key="option.id"
          class="option-card"
          :class="{
          'selected': selectedOption === option.id,
          'correct': showResult && option.id === correctAnswer,
          'incorrect': showResult && selectedOption === option.id && option.id !== correctAnswer,
          'disabled': showResult,
        }"
          @click="selectOption(option.id)"
      >
        <div class="option-label" :class="{'bg-primary': selectedOption !== option.id}">{{
            option.id.toUpperCase()
          }}
        </div>
        <span class="font-medium">{{ option.text }}</span>
        <i v-if="showResult && option.id === correctAnswer" class="pi pi-check-circle text-success ml-auto text-xl"></i>
        <i v-if="showResult && selectedOption === option.id && option.id !== correctAnswer"
           class="pi pi-times-circle text-error ml-auto text-xl"></i>
      </div>
    </div>

    <!-- Text Input -->
    <div v-if="questionType === 'text_input'">
      <InputText v-model="userAnswer" placeholder="Type your answer..." :disabled="showResult" class="w-full p-4"
                 @keyup.enter="!showResult && submitAnswer()"/>
    </div>

    <Message v-if="showHint" severity="secondary">{{ content.hint }}</Message>
    <Message v-if="showResult && feedback" :severity="isCorrect ? 'success' : 'warn'">{{ feedback }}</Message>

    <div class="actions-section">
      <Button v-if="!showResult && content.hint" label="Hint" icon="pi pi-lightbulb" text
              @click="showHint = !showHint"/>
      <div class="flex-grow"></div>
      <Button v-if="!showResult" label="Submit" icon="pi pi-check" :disabled="!canSubmit" @click="submitAnswer"/>
      <template v-else>
        <Button v-if="isCorrect" :label="`Next (${autoAdvanceSeconds}s)`" icon="pi pi-arrow-right" @click="handleNext"/>
        <Button v-else label="Try Again" icon="pi pi-refresh" @click="resetExercise"/>
      </template>
    </div>
  </div>
</template>

<style scoped>
.listening-exercise {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.options-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}

.option-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 2px solid var(--surface-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.2s;
  background-color: var(--surface-card);
}

.option-card:hover:not(.disabled) {
  border-color: var(--primary-color);
  background-color: var(--surface-hover);
}

.option-card.selected {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px var(--primary-color);
}

.option-card.correct {
  border-color: var(--p-green-500);
}

.option-card.incorrect {
  border-color: var(--p-red-500);
}

.option-card.disabled {
  cursor: not-allowed;
  opacity: 0.8;
}

.option-label {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: var(--p-primary-contrast);
  flex-shrink: 0;
}

.option-card.selected .option-label {
  background-color: var(--primary-color);
}

.option-card.correct .option-label {
  background-color: var(--p-green-500);
}

.option-card.incorrect .option-label {
  background-color: var(--p-red-500);
}

.actions-section {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}
</style>
