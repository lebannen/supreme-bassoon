<template>
  <div class="multiple-choice-exercise">
    <div class="question-section">
      <h2 class="question-text">{{ questionText }}</h2>
    </div>

    <div class="options-section">
      <div
        v-for="option in options"
        :key="option.id"
        class="option-card"
        :class="{
          'selected': selectedOption === option.id,
          'correct': showResult && option.isCorrect,
          'incorrect': showResult && selectedOption === option.id && !option.isCorrect,
          'disabled': showResult
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
        <Button
          v-else
          label="Try Again"
          icon="pi pi-refresh"
          @click="reset"
        />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
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
  padding: 2rem;
  background: var(--surface-card);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.question-text {
  font-size: 2rem;
  font-weight: 600;
  color: var(--text-color);
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
  background: #e3f2fd;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.4);
}

.option-card.correct {
  border-color: #22c55e;
  border-width: 3px;
  background: rgba(34, 197, 94, 0.15);
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.3);
}

.option-card.incorrect {
  border-color: #ef4444;
  border-width: 3px;
  background: rgba(239, 68, 68, 0.15);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
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
  color: var(--text-color);
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.option-card.selected:not(.disabled) .option-label {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
  transform: scale(1.1);
}

.option-card.correct .option-label {
  background: #22c55e;
  border-color: #22c55e;
  color: white;
  transform: scale(1.1);
}

.option-card.incorrect .option-label {
  background: #ef4444;
  border-color: #ef4444;
  color: white;
  transform: scale(1.1);
}

.option-text {
  flex: 1;
  font-size: 1.125rem;
  font-weight: 500;
  color: var(--text-color);
}

.result-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.correct-icon {
  color: #22c55e;
}

.incorrect-icon {
  color: #ef4444;
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

/* Dark mode adjustments */
@media (prefers-color-scheme: dark) {
  .option-card {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.15);
  }

  .option-card:hover:not(.disabled) {
    background: rgba(255, 255, 255, 0.08);
    border-color: var(--primary-color);
  }

  .option-card.selected:not(.disabled) {
    background: rgba(33, 150, 243, 0.25);
  }
}
</style>
