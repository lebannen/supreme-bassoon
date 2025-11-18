<template>
  <div class="multiple-choice-exercise">
    <Card class="question-section">
      <template #content>
        <h2 class="question-text">{{ questionText }}</h2>
      </template>
    </Card>

    <div class="options-section">
      <div
        v-for="option in options"
        :key="option.id"
        class="option-card"
        :class="{
          selected: selectedOption === option.id,
          correct: showResult && option.isCorrect,
          incorrect: showResult && selectedOption === option.id && !option.isCorrect,
          disabled: showResult,
        }"
        @click="selectOption(option.id)"
      >
        <div class="option-label">{{ option.id.toUpperCase() }}</div>
        <div class="option-text">{{ option.text }}</div>
        <i
          v-if="showResult && option.isCorrect"
          class="pi pi-check-circle result-icon correct-icon"
        />
        <i
          v-if="showResult && selectedOption === option.id && !option.isCorrect"
          class="pi pi-times-circle result-icon incorrect-icon"
        />
      </div>
    </div>

    <div v-if="showHint && !showResult" class="hint-section">
      <Message severity="info">
        <div class="hint-content"><strong>Hint:</strong> {{ hint }}</div>
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
        :disabled="!selectedOption"
        @click="submitAnswer"
      />
      <template v-else>
        <Button
          v-if="isCorrect"
          :label="`Next (${autoAdvanceSeconds}s)`"
          icon="pi pi-arrow-right"
          @click="handleNext"
        />
        <Button v-else label="Try Again" icon="pi pi-refresh" @click="reset"/>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue'
import Button from 'primevue/button'
import Card from 'primevue/card'
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
const autoAdvanceTimer = ref<number | null>(null)
const autoAdvanceSeconds = ref(3)

const questionText = computed(() => props.content.question.content)
const options = computed(() => props.content.options)
const hint = computed(() => props.content.hint || '')

function selectOption(optionId: string) {
  if (!showResult.value) {
    selectedOption.value = optionId
  }
}

function toggleHint() {
  showHint.value = !showHint.value
}

function submitAnswer() {
  if (!selectedOption.value) return

  emit('submit', { selectedOption: selectedOption.value })
}

function setResult(result: { isCorrect: boolean; feedback: string; userResponse?: any }) {
  // Restore selectedOption from parent if component was recreated
  if (result.userResponse && result.userResponse.selectedOption && !selectedOption.value) {
    selectedOption.value = result.userResponse.selectedOption
  }

  showResult.value = true
  isCorrect.value = result.isCorrect
  feedback.value = result.feedback

  // Start auto-advance countdown for correct answers
  if (result.isCorrect) {
    startAutoAdvance()
  }
}

function startAutoAdvance() {
  autoAdvanceSeconds.value = 3

  const countdown = setInterval(() => {
    autoAdvanceSeconds.value--
    if (autoAdvanceSeconds.value <= 0) {
      clearInterval(countdown)
      handleNext()
    }
  }, 1000)

  autoAdvanceTimer.value = countdown
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

<style scoped>
.multiple-choice-exercise {
  max-width: 800px;
  margin: 0 auto;
}

.question-section {
  text-align: center;
  margin-bottom: 2rem;
}

.question-text {
  font-size: 2rem;
  font-weight: 600;
  margin: 0;
}

.options-section {
  display: grid;
  gap: 1rem;
  margin-bottom: 2rem;
}

.option-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem;
  background: var(--surface-section);
  border: 3px solid var(--surface-border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.option-card:hover:not(.disabled) {
  border-color: var(--primary-color);
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background: var(--surface-card);
}

.option-card.selected:not(.disabled) {
  border-color: var(--primary-color);
  border-width: 3px;
  box-shadow: 0 4px 12px rgba(var(--primary-color-rgb), 0.4);
}

.option-card.correct {
  border-width: 3px;
  box-shadow: 0 4px 12px rgba(var(--green-500-rgb), 0.3);
}

.option-card.incorrect {
  border-width: 3px;
  box-shadow: 0 4px 12px rgba(var(--red-500-rgb), 0.3);
}

.option-card.disabled {
  cursor: not-allowed;
  opacity: 0.95;
}

.option-label {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: var(--surface-ground);
  border: 2px solid var(--surface-border);
  border-radius: 50%;
  font-weight: 700;
  font-size: 1.125rem;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.option-card.selected:not(.disabled) .option-label {
  background: var(--primary-color);
  border-color: var(--primary-color);
  transform: scale(1.1);
}

.option-card.correct .option-label {
  transform: scale(1.1);
}

.option-card.incorrect .option-label {
  transform: scale(1.1);
}

.option-text {
  flex: 1;
  font-size: 1.125rem;
  font-weight: 500;
}

.result-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.hint-section,
.feedback-section {
  margin-bottom: 2rem;
}

.hint-content,
.feedback-content {
  line-height: 1.6;
}

.actions-section {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  align-items: center;
}
</style>
