<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import Button from 'primevue/button'
import Message from 'primevue/message'

interface MatchingContent {
  pairs: Array<{ left: string; right: string }>
  hint?: string
}

interface Props {
  content: MatchingContent
}

const props = defineProps<Props>()
const emit = defineEmits<{
  submit: [response: { matches: Record<string, string> }]
  next: []
}>()

const leftItems = ref<Array<{ left: string; right: string }>>([])
const rightOptions = ref<string[]>([])
const userMatches = ref<Record<string, string>>({})
const selectedLeft = ref<number | null>(null)
const showResult = ref(false)
const isCorrect = ref(false)
const feedback = ref('')
const showHint = ref(false)
const hint = computed(() => props.content.hint || '')
const correctMatches = ref<Record<string, string>>({})
const autoAdvanceTimer = ref<any>(null)
const autoAdvanceSeconds = ref(5)

onMounted(() => {
  initializeExercise()
})

function initializeExercise() {
  leftItems.value = [...props.content.pairs]
  rightOptions.value = [...props.content.pairs.map(p => p.right)].sort(() => Math.random() - 0.5)
  props.content.pairs.forEach(pair => {
    correctMatches.value[pair.left] = pair.right
  })
}

function selectLeft(index: number) {
  if (showResult.value) return
  selectedLeft.value = index
}

function selectRight(rightValue: string) {
  if (selectedLeft.value === null || showResult.value) return

  const existingLeft = Object.keys(userMatches.value).find(key => userMatches.value[key] === rightValue)
  if (existingLeft) {
    delete userMatches.value[existingLeft]
  }

  const leftValue = leftItems.value[selectedLeft.value].left
  userMatches.value[leftValue] = rightValue
  selectedLeft.value = null
}

const isRightMatched = (option: string) => Object.values(userMatches.value).includes(option)
const isMatchCorrect = (leftValue: string) => userMatches.value[leftValue] === correctMatches.value[leftValue]

function getRightItemState(option: string) {
  if (!showResult.value) {
    return {
      'p-highlight': isRightMatched(option),
      'p-disabled': isRightMatched(option),
      'available-target': selectedLeft.value !== null && !isRightMatched(option)
    }
  }
  const isMatched = isRightMatched(option)
  if (!isMatched) return {'p-disabled': true}

  const leftVal = Object.keys(userMatches.value).find(k => userMatches.value[k] === option)
  return {
    'correct-match': leftVal && correctMatches.value[leftVal] === option,
    'incorrect-match': leftVal && correctMatches.value[leftVal] !== option,
  }
}

function clearAllMatches() {
  userMatches.value = {}
  selectedLeft.value = null
}

function submitAnswer() {
  emit('submit', {matches: userMatches.value})
}

function resetExercise() {
  stopAutoAdvance()
  userMatches.value = {}
  selectedLeft.value = null
  showResult.value = false
  isCorrect.value = false
  feedback.value = ''
  showHint.value = false
  initializeExercise()
}

function startAutoAdvance() {
  autoAdvanceSeconds.value = 5
  autoAdvanceTimer.value = setInterval(() => {
    autoAdvanceSeconds.value--
    if (autoAdvanceSeconds.value <= 0) {
      handleNext()
    }
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

function setResult(result: { isCorrect: boolean; feedback: string; userResponse?: any }) {
  if (result.userResponse && result.userResponse.matches) {
    userMatches.value = result.userResponse.matches
  }
  showResult.value = true
  isCorrect.value = result.isCorrect
  feedback.value = result.feedback
  if (result.isCorrect) {
    startAutoAdvance()
  }
}

defineExpose({setResult})
</script>

<template>
  <div class="matching-exercise">
    <h2 class="text-xl font-bold mb-4">Match the pairs:</h2>

    <div class="matching-container">
      <!-- Left Column -->
      <div class="column">
        <div
            v-for="(pair, index) in leftItems"
            :key="`left-${index}`"
            class="match-item"
            :class="{
            'selected': selectedLeft === index,
            'p-highlight': userMatches[pair.left],
            'correct-match': showResult && isMatchCorrect(pair.left),
            'incorrect-match': showResult && !isMatchCorrect(pair.left) && userMatches[pair.left],
          }"
            @click="selectLeft(index)"
        >
          {{ pair.left }}
        </div>
      </div>

      <!-- Right Column -->
      <div class="column">
        <div
            v-for="(option, index) in rightOptions"
            :key="`right-${index}`"
            class="match-item"
            :class="getRightItemState(option)"
            @click="selectRight(option)"
        >
          {{ option }}
        </div>
      </div>
    </div>

    <Message v-if="selectedLeft !== null && !showResult" severity="info">
      Select the matching item on the right.
    </Message>

    <Message v-if="showHint" severity="secondary">{{ hint }}</Message>
    <Message v-if="showResult && feedback" :severity="isCorrect ? 'success' : 'warn'">
      {{ feedback }}
    </Message>

    <div class="actions-section">
      <Button v-if="!showResult && hint" label="Hint" icon="pi pi-lightbulb" text @click="showHint = !showHint"/>
      <Button v-if="!showResult" label="Clear" icon="pi pi-refresh" text @click="clearAllMatches"
              :disabled="Object.keys(userMatches).length === 0"/>
      <div class="flex-grow"></div>
      <Button v-if="!showResult" label="Submit" icon="pi pi-check"
              :disabled="Object.keys(userMatches).length !== leftItems.length" @click="submitAnswer"/>
      <template v-else>
        <Button v-if="isCorrect" :label="`Next (${autoAdvanceSeconds}s)`" icon="pi pi-arrow-right" @click="handleNext"/>
        <Button v-else label="Try Again" icon="pi pi-refresh" @click="resetExercise"/>
      </template>
    </div>
  </div>
</template>

<style scoped>
.matching-exercise {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.matching-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.column {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.match-item {
  padding: 1rem;
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  background-color: var(--surface-card);
  font-weight: 500;
}

.match-item:hover {
  background-color: var(--surface-hover);
}

.match-item.selected {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px var(--primary-color);
  transform: scale(1.02);
}

.match-item.p-highlight {
  background-color: var(--highlight-bg);
  color: var(--highlight-text-color);
}

.match-item.p-disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.match-item.available-target {
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% {
    border-color: var(--p-green-500);
    box-shadow: 0 0 0 2px transparent;
  }
  50% {
    border-color: var(--p-green-600);
    box-shadow: 0 0 0 2px var(--p-green-300);
  }
}

.dark-theme .available-target {
  @keyframes pulse {
    0%, 100% {
      border-color: var(--p-green-400);
      box-shadow: 0 0 0 2px transparent;
    }
    50% {
      border-color: var(--p-green-300);
      box-shadow: 0 0 0 2px var(--p-green-600);
    }
  }
}

.match-item.correct-match {
  border-color: var(--p-green-500);
  background-color: var(--p-green-50);
}

.dark-theme .match-item.correct-match {
  background-color: var(--p-green-900);
}

.match-item.incorrect-match {
  border-color: var(--p-red-500);
  background-color: var(--p-red-50);
}

.dark-theme .match-item.incorrect-match {
  background-color: var(--p-red-900);
}

.actions-section {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  flex-wrap: wrap;
}
</style>
