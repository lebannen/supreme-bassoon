<script setup lang="ts">
import {computed, ref} from 'vue'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Message from 'primevue/message'

interface BlankConfig {
  correctAnswer: string
  acceptableAnswers?: string[]
  options?: string[]
}

interface Props {
  content: {
    text: string
    blanks: BlankConfig[]
    translation?: string
    hint?: string
  }
}

const props = defineProps<Props>()
const emit = defineEmits<{
  submit: [response: { answers: string[] }]
  next: []
}>()

const selectedAnswers = ref<(string | null)[]>([])
const showResult = ref(false)
const feedback = ref<string | null>(null)
const isCorrect = ref(false)
const blankResults = ref<('correct' | 'incorrect' | null)[]>([])
const showHint = ref(false)
const autoAdvanceTimer = ref<any>(null)
const autoAdvanceSeconds = ref(5)

const sentenceParts = computed(() => {
  const parts: Array<{ type: 'text' | 'blank'; content: string; blankIndex?: number }> = []
  let blankIndex = 0
  props.content.text.split(/(___)/).forEach(segment => {
    if (segment === '___') {
      parts.push({type: 'blank', content: '', blankIndex: blankIndex++})
    } else if (segment) {
      parts.push({type: 'text', content: segment})
    }
  })
  if (selectedAnswers.value.length !== blankIndex) {
    selectedAnswers.value = new Array(blankIndex).fill(null)
  }
  return parts
})

const allAnswersFilled = computed(() => selectedAnswers.value.every(a => a !== null))

function submitAnswer() {
  if (!allAnswersFilled.value) return
  emit('submit', {answers: selectedAnswers.value.filter((a): a is string => a !== null)})
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

function setResult(result: { isCorrect: boolean; feedback: string; userResponse?: any; blankResults?: boolean[] }) {
  showResult.value = true
  isCorrect.value = result.isCorrect
  feedback.value = result.feedback
  if (result.userResponse?.answers) {
    selectedAnswers.value = result.userResponse.answers
  }
  if (result.blankResults) {
    blankResults.value = result.blankResults.map(r => r ? 'correct' : 'incorrect')
  }
  if (result.isCorrect) {
    startAutoAdvance()
  }
}

function reset() {
  stopAutoAdvance()
  selectedAnswers.value = new Array(props.content.blanks.length).fill(null)
  showHint.value = false
  showResult.value = false
  feedback.value = null
  isCorrect.value = false
  blankResults.value = []
}

defineExpose({ setResult, reset })
</script>

<template>
  <div class="fill-in-blank-exercise">
    <div class="p-4 bg-surface-card rounded-lg text-center">
      <div class="sentence-display">
        <span v-for="(part, index) in sentenceParts" :key="index">
          <span v-if="part.type === 'text'" class="sentence-text">{{ part.content }}</span>
          <span v-else-if="part.type === 'blank' && part.blankIndex !== undefined" class="inline-block">
            <Select
                v-if="!showResult"
                v-model="selectedAnswers[part.blankIndex]"
                :options="content.blanks[part.blankIndex].options || []"
                placeholder="Select"
                class="blank-select"
            />
            <span
                v-else
                class="blank-result"
                :class="blankResults[part.blankIndex]"
            >
              {{ selectedAnswers[part.blankIndex] || '___' }}
            </span>
          </span>
        </span>
      </div>
      <p v-if="content.translation" class="text-secondary mt-md italic">{{ content.translation }}</p>
    </div>

    <Message v-if="showHint" severity="secondary">{{ content.hint }}</Message>
    <Message v-if="showResult && feedback" :severity="isCorrect ? 'success' : 'warn'">{{ feedback }}</Message>

    <div class="actions-section">
      <Button v-if="!showResult && content.hint" label="Hint" icon="pi pi-lightbulb" text
              @click="showHint = !showHint"/>
      <div class="flex-grow"></div>
      <Button v-if="!showResult" label="Submit" icon="pi pi-check" :disabled="!allAnswersFilled" @click="submitAnswer"/>
      <template v-else>
        <Button v-if="isCorrect" :label="`Next (${autoAdvanceSeconds}s)`" icon="pi pi-arrow-right" @click="handleNext"/>
        <Button v-else label="Try Again" icon="pi pi-refresh" @click="reset"/>
      </template>
    </div>
  </div>
</template>

<style scoped>
.fill-in-blank-exercise {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  max-width: 900px;
  margin: 0 auto;
}

.sentence-display {
  font-size: 1.5rem;
  font-weight: 500;
  line-height: 2.5;
  display: inline;
}

.sentence-text {
  white-space: pre-wrap;
}

.blank-select {
  min-width: 160px;
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0 0.25rem;
  vertical-align: middle;
}

.blank-result {
  display: inline-block;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  font-size: 1.25rem;
  font-weight: 600;
  min-width: 160px;
  text-align: center;
  margin: 0 0.25rem;
  border: 2px solid transparent;
  vertical-align: middle;
}

.blank-result.correct {
  border-color: var(--p-green-500);
  background-color: var(--p-green-50);
  color: var(--p-green-700);
}

.dark-theme .blank-result.correct {
  background-color: var(--p-green-900);
  color: var(--p-green-300);
}

.blank-result.incorrect {
  border-color: var(--p-red-500);
  background-color: var(--p-red-50);
  color: var(--p-red-700);
}

.dark-theme .blank-result.incorrect {
  background-color: var(--p-red-900);
  color: var(--p-red-300);
}

.actions-section {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .sentence-display {
    font-size: 1.25rem;
  }
}
</style>
