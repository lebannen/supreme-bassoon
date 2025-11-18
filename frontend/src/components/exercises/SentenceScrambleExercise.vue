<template>
  <div class="sentence-scramble-exercise">
    <Card class="question-section">
      <template #content>
        <h2 class="instructions-text">Arrange the words in the correct order:</h2>

        <!-- User's answer area -->
        <div class="answer-area">
          <div
              v-for="(word, index) in userOrderedWords"
              :key="`answer-${index}`"
              class="word-chip answer-chip"
              :class="{ correct: showResult && isCorrect, incorrect: showResult && !isCorrect }"
              @click="removeWord(index)"
          >
            {{ word }}
            <i v-if="!showResult" class="pi pi-times remove-icon"></i>
          </div>
          <div v-if="userOrderedWords.length === 0 && !showResult" class="placeholder-text">
            Click words below to build your sentence
          </div>
        </div>

        <!-- Available words to choose from -->
        <div v-if="!showResult" class="available-words">
          <div
              v-for="(word, index) in availableWords"
              :key="`available-${index}`"
              class="word-chip available-chip"
              @click="selectWord(index)"
          >
            {{ word }}
          </div>
        </div>

        <p v-if="translation && showResult" class="translation">{{ translation }}</p>
      </template>
    </Card>

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
        label="Clear"
        icon="pi pi-refresh"
        text
        :disabled="userOrderedWords.length === 0"
        @click="clearAnswer"
      />
      <Button
        v-if="!showResult"
        label="Submit Answer"
        icon="pi pi-check"
        :disabled="userOrderedWords.length !== totalWords"
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

interface Props {
  content: {
    words: string[]
    sentence: string
    translation?: string
    grammarExplanation?: string
    hint?: string
  }
}

const props = defineProps<Props>()
const emit = defineEmits<{
  submit: [response: { orderedWords: string[] }]
  next: []
}>()

const userOrderedWords = ref<string[]>([])
const availableWords = ref<string[]>([])
const showHint = ref(false)
const showResult = ref(false)
const feedback = ref<string | null>(null)
const isCorrect = ref(false)
const autoAdvanceTimer = ref<number | null>(null)
const autoAdvanceSeconds = ref(3)

const totalWords = computed(() => props.content.words.length)
const translation = computed(() => props.content.translation)
const hint = computed(() => props.content.hint || props.content.grammarExplanation || '')

// Initialize with scrambled words
function initializeWords() {
  // Create a scrambled copy of the words
  const scrambled = [...props.content.words]

  // Fisher-Yates shuffle algorithm
  for (let i = scrambled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[scrambled[i], scrambled[j]] = [scrambled[j], scrambled[i]]
  }

  availableWords.value = scrambled
}

function selectWord(index: number) {
  const word = availableWords.value[index]
  userOrderedWords.value.push(word)
  availableWords.value.splice(index, 1)
}

function removeWord(index: number) {
  if (showResult.value) return

  const word = userOrderedWords.value[index]
  userOrderedWords.value.splice(index, 1)
  availableWords.value.push(word)
}

function clearAnswer() {
  availableWords.value = [...availableWords.value, ...userOrderedWords.value]
  userOrderedWords.value = []
}

function toggleHint() {
  showHint.value = !showHint.value
}

function submitAnswer() {
  if (userOrderedWords.value.length !== totalWords.value) return

  emit('submit', { orderedWords: userOrderedWords.value })
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
  // Restore user's answer from parent if component was recreated
  if (result.userResponse && result.userResponse.orderedWords) {
    userOrderedWords.value = result.userResponse.orderedWords
    availableWords.value = []
  }

  showResult.value = true
  isCorrect.value = result.isCorrect
  feedback.value = result.feedback

  // Start auto-advance countdown for correct answers
  if (result.isCorrect) {
    startAutoAdvance()
  }
}

function reset() {
  stopAutoAdvance()
  userOrderedWords.value = []
  showHint.value = false
  showResult.value = false
  feedback.value = null
  isCorrect.value = false
  initializeWords()
}

// Initialize on mount
initializeWords()

defineExpose({ setResult, reset })
</script>

<style scoped>
.sentence-scramble-exercise {
  max-width: 900px;
  margin: 0 auto;
}

.question-section {
  margin-bottom: 2rem;
}

.instructions-text {
  text-align: center;
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 2rem;
}

.answer-area {
  min-height: 100px;
  padding: 1.5rem;
  background: var(--surface-ground);
  border: 3px dashed var(--surface-border);
  border-radius: 12px;
  margin-bottom: 2rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.answer-area.correct {
  border-color: var(--green-500);
}

.answer-area.incorrect {
  border-color: var(--red-500);
}

.placeholder-text {
  font-style: italic;
  text-align: center;
  width: 100%;
}

.available-words {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
  padding: 1rem;
}

.word-chip {
  padding: 0.75rem 1.25rem;
  border-radius: 8px;
  font-size: 1.125rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.available-chip {
  background: var(--primary-color);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.available-chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

.available-chip:active {
  transform: translateY(0);
}

.answer-chip {
  background: var(--surface-section);
  border: 2px solid var(--surface-border);
}

.answer-chip:not(.correct):not(.incorrect):hover {
  border-color: var(--primary-color);
  transform: translateY(-2px);
}

.answer-chip.correct {
  border-color: var(--green-500);
  cursor: default;
}

.answer-chip.incorrect {
  border-color: var(--red-500);
  cursor: default;
}

.remove-icon {
  font-size: 0.875rem;
  opacity: 0.6;
}

.translation {
  margin-top: 1.5rem;
  text-align: center;
  font-size: 1rem;
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
  flex-wrap: wrap;
}

/* Mobile responsive */
@media (max-width: 768px) {
  .instructions-text {
    font-size: 1.25rem;
  }

  .word-chip {
    font-size: 1rem;
    padding: 0.6rem 1rem;
  }

  .actions-section {
    justify-content: center;
  }
}
</style>
