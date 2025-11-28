<script setup lang="ts">
import {ref} from 'vue'
import Button from 'primevue/button'
import Message from 'primevue/message'

interface Option {
  id: string
  text: string
  isCorrect: boolean
}

interface Props {
  content: {
    question: {
      type: string
      content: string
    }
    options: Option[]
    explanation?: string
    hint?: string
  }
}

const props = defineProps<Props>()
const emit = defineEmits<{
  submit: [response: { selectedOption: string }]
  next: []
}>()

const selectedOption = ref<string | null>(null)
const showHint = ref(false)
const showResult = ref(false)
const feedback = ref<string | null>(null)
const isCorrect = ref(false)
const autoAdvanceTimer = ref<any>(null)
const autoAdvanceSeconds = ref(5)

function selectOption(optionId: string) {
  if (!showResult.value) {
    selectedOption.value = optionId
  }
}

function submitAnswer() {
  if (!selectedOption.value) return
  emit('submit', { selectedOption: selectedOption.value })
}

function setResult(result: { isCorrect: boolean; feedback: string; userResponse?: any }) {
  if (result.userResponse?.selectedOption) {
    selectedOption.value = result.userResponse.selectedOption
  }
  showResult.value = true
  isCorrect.value = result.isCorrect
  feedback.value = result.feedback
  if (result.isCorrect) {
    startAutoAdvance()
  }
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

function reset() {
  stopAutoAdvance()
  selectedOption.value = null
  showHint.value = false
  showResult.value = false
  feedback.value = null
  isCorrect.value = false
}

defineExpose({ setResult, reset })
</script>

<template>
  <div class="multiple-choice-exercise">
    <div class="p-4 bg-surface-card rounded-lg text-center">
      <h2 class="text-2xl font-bold">{{ content.question.content }}</h2>
    </div>

    <div class="options-grid">
      <div
          v-for="option in content.options"
          :key="option.id"
          class="option-card"
          :class="{
          'selected': selectedOption === option.id,
          'correct': showResult && option.isCorrect,
          'incorrect': showResult && selectedOption === option.id && !option.isCorrect,
          'disabled': showResult,
        }"
          @click="selectOption(option.id)"
      >
        <div class="option-label" :class="{'bg-primary': selectedOption !== option.id}">{{
            option.id.toUpperCase()
          }}
        </div>
        <span class="font-medium text-lg">{{ option.text }}</span>
        <i v-if="showResult && option.isCorrect" class="pi pi-check-circle text-success ml-auto text-2xl"></i>
        <i v-if="showResult && selectedOption === option.id && !option.isCorrect"
           class="pi pi-times-circle text-error ml-auto text-2xl"></i>
      </div>
    </div>

    <Message v-if="showHint" severity="secondary">{{ content.hint }}</Message>
    <Message v-if="showResult && feedback" :severity="isCorrect ? 'success' : 'warn'">{{ feedback }}</Message>

    <div class="actions-section">
      <Button v-if="!showResult && content.hint" label="Hint" icon="pi pi-lightbulb" text
              @click="showHint = !showHint"/>
      <div class="flex-grow"></div>
      <Button v-if="!showResult" label="Submit" icon="pi pi-check" :disabled="!selectedOption" @click="submitAnswer"/>
      <template v-else>
        <Button v-if="isCorrect" :label="`Next (${autoAdvanceSeconds}s)`" icon="pi pi-arrow-right" @click="handleNext"/>
        <Button v-else label="Try Again" icon="pi pi-refresh" @click="reset"/>
      </template>
    </div>
  </div>
</template>

<style scoped>
.multiple-choice-exercise {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  max-width: 800px;
  margin: 0 auto;
}

.options-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}
.option-card {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 1.25rem;
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
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: var(--p-primary-contrast);
  flex-shrink: 0;
  transition: transform 0.2s;
}

.option-card.selected .option-label {
  background-color: var(--primary-color);
  transform: scale(1.1);
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
}
</style>
