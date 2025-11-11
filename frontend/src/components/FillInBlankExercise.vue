<template>
  <div class="fill-in-blank-exercise">
    <div class="question-section">
      <div class="sentence-display">
        <span v-for="(part, index) in sentenceParts" :key="index">
          <span v-if="part.type === 'text'" class="sentence-text">{{ part.content }}</span>
          <span v-else-if="part.type === 'blank'" class="blank-wrapper">
            <Select
              v-if="!showResult"
              v-model="selectedAnswer"
              :options="options"
              placeholder="Choose..."
              class="blank-select"
              :class="{ 'has-value': selectedAnswer }"
            />
            <span v-else class="blank-result" :class="resultClass">
              {{ userAnswer || '___' }}
            </span>
          </span>
        </span>
      </div>
      <p v-if="translation" class="translation">{{ translation }}</p>
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
        :disabled="!selectedAnswer"
        @click="submitAnswer"
      />
      <Button
        v-else
        label="Try Again"
        icon="pi pi-refresh"
        @click="reset"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Message from 'primevue/message'

interface Props {
  content: {
    sentence: string
    blankIndex?: number
    correctAnswer: string
    options: string[]
    translation?: string
    grammarExplanation?: string
    hint?: string
  }
}

const props = defineProps<Props>()
const emit = defineEmits<{
  submit: [response: { answer: string }]
}>()

const selectedAnswer = ref<string | null>(null)
const userAnswer = ref<string | null>(null) // Preserve the user's answer for display
const showHint = ref(false)
const showResult = ref(false)
const feedback = ref<string | null>(null)
const isCorrect = ref(false)

// Parse the sentence and split it into parts (text and blank)
const sentenceParts = computed(() => {
  const sentence = props.content.sentence
  const parts: Array<{ type: 'text' | 'blank'; content: string }> = []

  // Split by blank marker (e.g., "Je ____ français." or "Je ___ français.")
  const blankPattern = /_{2,}/
  const segments = sentence.split(blankPattern)

  if (segments.length === 1) {
    // No blank found, return entire sentence as text
    parts.push({ type: 'text', content: sentence })
  } else {
    // Add parts with blank in between
    segments.forEach((segment, index) => {
      if (segment) {
        parts.push({ type: 'text', content: segment })
      }
      if (index < segments.length - 1) {
        parts.push({ type: 'blank', content: '' })
      }
    })
  }

  return parts
})

const options = computed(() => props.content.options || [])
const translation = computed(() => props.content.translation)
const hint = computed(() => props.content.hint || props.content.grammarExplanation || '')

const resultClass = computed(() => {
  if (!showResult.value) return ''
  return isCorrect.value ? 'correct' : 'incorrect'
})

function toggleHint() {
  showHint.value = !showHint.value
}

function submitAnswer() {
  if (!selectedAnswer.value) return

  // Preserve the user's answer before submission
  userAnswer.value = selectedAnswer.value
  emit('submit', { answer: selectedAnswer.value })
}

function setResult(result: { isCorrect: boolean; feedback: string; userResponse?: any }) {
  // Use the userResponse passed from parent if available, otherwise fall back to preserved state
  if (result.userResponse && result.userResponse.answer) {
    userAnswer.value = result.userResponse.answer
  }

  showResult.value = true
  isCorrect.value = result.isCorrect
  feedback.value = result.feedback
}

function reset() {
  selectedAnswer.value = null
  userAnswer.value = null
  showHint.value = false
  showResult.value = false
  feedback.value = null
  isCorrect.value = false
}

defineExpose({ setResult, reset })
</script>

<style scoped>
.fill-in-blank-exercise {
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

.sentence-display {
  font-size: 1.75rem;
  font-weight: 500;
  line-height: 2.5;
  color: var(--text-color);
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 0.25rem;
}

.sentence-text {
  white-space: pre-wrap;
}

.blank-wrapper {
  display: inline-flex;
  align-items: center;
  margin: 0 0.25rem;
}

.blank-select {
  min-width: 150px;
  font-size: 1.5rem;
  font-weight: 600;
}

.blank-select.has-value {
  border-color: var(--primary-color);
  border-width: 2px;
}

.blank-result {
  display: inline-block;
  padding: 0.5rem 1.25rem;
  border-radius: 8px;
  font-size: 1.5rem;
  font-weight: 700;
  min-width: 150px;
  text-align: center;
}

.blank-result.correct {
  background: rgba(34, 197, 94, 0.2);
  color: #16a34a;
  border: 3px solid #22c55e;
}

.blank-result.incorrect {
  background: rgba(239, 68, 68, 0.2);
  color: #dc2626;
  border: 3px solid #ef4444;
}

.translation {
  margin-top: 1.5rem;
  font-size: 1rem;
  color: var(--text-color-secondary);
  font-style: italic;
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
  .blank-result.correct {
    background: rgba(34, 197, 94, 0.25);
    color: #4ade80;
  }

  .blank-result.incorrect {
    background: rgba(239, 68, 68, 0.25);
    color: #f87171;
  }
}

/* Mobile responsive */
@media (max-width: 768px) {
  .sentence-display {
    font-size: 1.5rem;
    line-height: 2.25;
  }

  .blank-select,
  .blank-result {
    font-size: 1.25rem;
    min-width: 120px;
  }
}
</style>
