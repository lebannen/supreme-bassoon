<template>
  <div class="matching-exercise">
    <div class="question-section">
      <h2 class="instructions-text">Match the pairs:</h2>

      <div class="matching-container">
        <!-- Left column (items to match) -->
        <div class="left-column">
          <div
            v-for="(pair, index) in leftItems"
            :key="`left-${index}`"
            class="match-item left-item"
            :class="{
              selected: selectedLeft === index,
              matched: userMatches[pair.left] !== undefined,
              correct: showResult && isMatchCorrect(pair.left),
              incorrect:
                showResult && !isMatchCorrect(pair.left) && userMatches[pair.left] !== undefined,
            }"
            @click="!showResult && selectLeft(index)"
          >
            <div class="item-content">{{ pair.left }}</div>
            <div v-if="userMatches[pair.left]" class="matched-with">
              <i class="pi pi-link"></i>
              <span class="matched-text">{{ userMatches[pair.left] }}</span>
            </div>
          </div>
        </div>

        <!-- Right column (items to select) -->
        <div class="right-column">
          <div
            v-for="(option, index) in rightOptions"
            :key="`right-${index}`"
            class="match-item right-item"
            :class="{
              matched: isRightMatched(option),
              available: !isRightMatched(option) && selectedLeft !== null,
              correct: showResult && isRightCorrect(option),
              incorrect: showResult && isRightIncorrect(option),
            }"
            @click="!showResult && selectRight(option)"
          >
            <div class="item-content">{{ option }}</div>
          </div>
        </div>
      </div>

      <div v-if="selectedLeft !== null && !showResult" class="selection-hint">
        <Message severity="info">
          <i class="pi pi-hand-pointer"></i> Click an item on the right to create a match
        </Message>
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
        label="Clear All"
        icon="pi pi-refresh"
        text
        :disabled="Object.keys(userMatches).length === 0"
        @click="clearAllMatches"
      />
      <Button
        v-if="!showResult"
        label="Submit Answer"
        icon="pi pi-check"
        :disabled="Object.keys(userMatches).length !== leftItems.length"
        @click="submitAnswer"
      />
      <template v-else>
        <Button
          v-if="isCorrect"
          :label="`Next (${autoAdvanceSeconds}s)`"
          icon="pi pi-arrow-right"
          @click="handleNext"
        />
        <Button v-else label="Try Again" icon="pi pi-refresh" @click="resetExercise"/>
      </template>
    </div>
  </div>
</template>

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
const autoAdvanceTimer = ref<number | null>(null)
const autoAdvanceSeconds = ref(3)

onMounted(() => {
  initializeExercise()
})

function initializeExercise() {
  leftItems.value = [...props.content.pairs]

  // Shuffle right items using Fisher-Yates algorithm
  const rights = props.content.pairs.map((p) => p.right)
  for (let i = rights.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[rights[i], rights[j]] = [rights[j], rights[i]]
  }
  rightOptions.value = rights

  // Store correct matches for validation
  props.content.pairs.forEach((pair) => {
    correctMatches.value[pair.left] = pair.right
  })
}

function selectLeft(index: number) {
  selectedLeft.value = index
}

function selectRight(rightValue: string) {
  if (selectedLeft.value === null) return

  // Remove existing match for this right value (if any)
  const existingLeft = Object.keys(userMatches.value).find(
      (key) => userMatches.value[key] === rightValue
  )
  if (existingLeft) {
    delete userMatches.value[existingLeft]
  }

  // Create new match
  const leftValue = leftItems.value[selectedLeft.value].left
  userMatches.value[leftValue] = rightValue

  // Clear selection
  selectedLeft.value = null
}

function isRightMatched(option: string): boolean {
  return Object.values(userMatches.value).includes(option)
}

function isMatchCorrect(leftValue: string): boolean {
  return userMatches.value[leftValue] === correctMatches.value[leftValue]
}

function isRightCorrect(rightValue: string): boolean {
  // Find if this right value is correctly matched
  const leftValue = Object.keys(userMatches.value).find(
      (key) => userMatches.value[key] === rightValue
  )
  if (!leftValue) return false
  return correctMatches.value[leftValue] === rightValue
}

function isRightIncorrect(rightValue: string): boolean {
  // Find if this right value is incorrectly matched
  const leftValue = Object.keys(userMatches.value).find(
      (key) => userMatches.value[key] === rightValue
  )
  if (!leftValue) return false
  return correctMatches.value[leftValue] !== rightValue
}

function clearAllMatches() {
  userMatches.value = {}
  selectedLeft.value = null
}

function toggleHint() {
  showHint.value = !showHint.value
}

function submitAnswer() {
  emit('submit', { matches: userMatches.value })
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

function setResult(result: { isCorrect: boolean; feedback: string; userResponse?: any }) {
  if (result.userResponse && result.userResponse.matches) {
    userMatches.value = result.userResponse.matches
  }
  showResult.value = true
  isCorrect.value = result.isCorrect
  feedback.value = result.feedback

  // Start auto-advance countdown for correct answers
  if (result.isCorrect) {
    startAutoAdvance()
  }
}

defineExpose({
  setResult,
})
</script>

<style scoped>
.matching-exercise {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.question-section {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.instructions-text {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
  margin: 0;
}

.matching-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
  padding: 1rem;
  background: var(--surface-ground);
  border-radius: 8px;
}

.left-column,
.right-column {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.match-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  background: var(--surface-card);
  border: 2px solid var(--surface-border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 60px;
}

.match-item:hover:not(.matched):not(.selected) {
  background: var(--surface-hover);
  border-color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.match-item.selected {
  background: #3b82f6 !important;
  color: white !important;
  border-color: #2563eb !important;
  border-width: 3px;
  transform: scale(1.02);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.3);
}

.match-item.matched {
  background: #e0e7ff !important;
  border-color: #818cf8 !important;
  border-width: 2px;
  cursor: not-allowed;
}

.match-item.matched .item-content {
  color: #4338ca !important;
  font-weight: 600;
}

.match-item.available {
  border-color: var(--green-400);
  border-width: 3px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    border-color: var(--green-400);
  }
  50% {
    border-color: var(--green-600);
  }
}

.match-item.correct {
  background: var(--green-50);
  border-color: var(--green-500);
  border-width: 3px;
}

.match-item.incorrect {
  background: var(--red-50);
  border-color: var(--red-500);
  border-width: 3px;
}

.item-content {
  font-size: 1rem;
  font-weight: 500;
  color: var(--text-color);
  flex: 1;
}

.match-item.selected .item-content {
  color: white !important;
}

.matched-with {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--blue-100);
  border-radius: 6px;
  font-size: 0.9rem;
  color: var(--blue-700);
  margin-left: 0.75rem;
}

.match-item.correct .matched-with {
  background: var(--green-100);
  color: var(--green-700);
}

.match-item.incorrect .matched-with {
  background: var(--red-100);
  color: var(--red-700);
}

.matched-with i {
  font-size: 0.875rem;
}

.matched-text {
  font-weight: 500;
}

.selection-hint {
  margin-top: -0.5rem;
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
  .matching-container {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }

  .actions-section {
    justify-content: stretch;
  }

  .actions-section button {
    flex: 1;
  }

  .matched-with {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.25rem;
  }
}
</style>
