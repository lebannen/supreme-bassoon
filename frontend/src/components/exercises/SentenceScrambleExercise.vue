<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Chip from 'primevue/chip'

interface Props {
  content: {
    sentence: string
    translation?: string
    hint?: string
    explanation?: string
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
const autoAdvanceTimer = ref<any>(null)
const autoAdvanceSeconds = ref(5)

// Split sentence into words and cache the result
// Remove punctuation before splitting to match backend validation logic
const sentenceWords = computed(() => {
  const withoutPunctuation = props.content.sentence.replace(/[?.!,;:]/g, '')
  return withoutPunctuation.trim().split(/\s+/)
})
const totalWords = computed(() => sentenceWords.value.length)

onMounted(initializeWords)

function initializeWords() {
  // Split the sentence into words and shuffle them
  availableWords.value = [...sentenceWords.value].sort(() => Math.random() - 0.5)
}

function selectWord(word: string, index: number) {
  userOrderedWords.value.push(word)
  availableWords.value.splice(index, 1)
}

function removeWord(word: string, index: number) {
  if (showResult.value) return
  userOrderedWords.value.splice(index, 1)
  availableWords.value.push(word)
}

function submitAnswer() {
  if (userOrderedWords.value.length !== totalWords.value) return
  emit('submit', { orderedWords: userOrderedWords.value })
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

function setResult(result: { isCorrect: boolean; feedback: string; userResponse?: any }) {
  if (result.userResponse?.orderedWords) {
    userOrderedWords.value = result.userResponse.orderedWords
    availableWords.value = []
  }
  showResult.value = true
  isCorrect.value = result.isCorrect
  feedback.value = result.feedback
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

defineExpose({ setResult, reset })
</script>

<template>
  <div class="sentence-scramble-exercise">
    <div class="p-lg bg-surface-card rounded-lg">
      <h2 class="text-xl font-bold text-center mb-lg">Unscramble the sentence:</h2>

      <div class="answer-area" :class="{ 'correct': showResult && isCorrect, 'incorrect': showResult && !isCorrect }">
        <Chip
            v-for="(word, index) in userOrderedWords"
            :key="`answer-${index}`"
            :label="word"
            class="word-chip answer-chip"
            :removable="!showResult"
            @remove="removeWord(word, index)"
        />
        <span v-if="userOrderedWords.length === 0" class="text-secondary italic">
          Click the words below to form a sentence.
        </span>
      </div>

      <div v-if="!showResult" class="available-words-area">
        <Chip
            v-for="(word, index) in availableWords"
            :key="`available-${index}`"
            :label="word"
            class="word-chip available-chip"
            @click="selectWord(word, index)"
        />
      </div>

      <p v-if="content.translation && showResult" class="text-center text-secondary italic mt-md">{{
          content.translation
        }}</p>
    </div>

    <Message v-if="showHint" severity="secondary">{{ content.hint }}</Message>
    <Message v-if="showResult && feedback" :severity="isCorrect ? 'success' : 'warn'">{{ feedback }}</Message>

    <div class="actions-section">
      <Button v-if="!showResult && content.hint" label="Hint" icon="pi pi-lightbulb" text
              @click="showHint = !showHint"/>
      <Button v-if="!showResult" label="Clear" icon="pi pi-refresh" text @click="reset"
              :disabled="userOrderedWords.length === 0"/>
      <div class="flex-grow"></div>
      <Button v-if="!showResult" label="Submit" icon="pi pi-check" :disabled="userOrderedWords.length !== totalWords"
              @click="submitAnswer"/>
      <template v-else>
        <Button v-if="isCorrect" :label="`Next (${autoAdvanceSeconds}s)`" icon="pi pi-arrow-right" @click="handleNext"/>
        <Button v-else label="Try Again" icon="pi pi-refresh" @click="reset"/>
      </template>
    </div>
  </div>
</template>

<style scoped>
.sentence-scramble-exercise {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  max-width: 800px;
  margin: 0 auto;
}
.answer-area {
  min-height: 8rem;
  padding: 1rem;
  background-color: var(--surface-ground);
  border: 2px dashed var(--surface-border);
  border-radius: var(--radius-lg);
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  justify-content: center;
  transition: border-color 0.3s;
}
.answer-area.correct {
  border-color: var(--p-green-500);
}
.answer-area.incorrect {
  border-color: var(--p-red-500);
}

.available-words-area {
  padding-top: 1.5rem;
  margin-top: 1.5rem;
  border-top: 1px solid var(--surface-border);
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
}
.word-chip {
  font-size: 1.1rem;
  padding: 0.5rem 1rem;
}

.available-chip {
  cursor: pointer;
  background-color: var(--surface-card);
  border: 1px solid var(--surface-border);
}
.available-chip:hover {
  background-color: var(--surface-hover);
}
.answer-chip {
  background-color: var(--primary-color);
  color: var(--primary-contrast);
}
.actions-section {
  display: flex;
  gap: 0.5rem;
}
</style>
