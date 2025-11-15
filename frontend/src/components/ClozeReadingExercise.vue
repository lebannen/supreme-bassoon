<template>
  <div class="cloze-reading-exercise">
    <div class="passage-section">
      <div class="passage-content">
        <component
          v-for="(segment, index) in textSegments"
          :key="index"
          :is="segment.type === 'text' ? 'span' : 'span'"
          :class="segment.type === 'blank' ? 'blank-wrapper' : ''"
        >
          <template v-if="segment.type === 'text'">
            {{ segment.content }}
          </template>
          <template v-else-if="segment.type === 'blank'">
            <InputText
              v-model="userAnswers[segment.blankId]"
              :disabled="showResult"
              :class="{
                'blank-input': true,
                'correct': showResult && isBlankCorrect(segment.blankId),
                'incorrect': showResult && !isBlankCorrect(segment.blankId) && userAnswers[segment.blankId]
              }"
              :placeholder="`${segment.blankId}`"
              @keyup.enter="handleEnterKey"
            />
            <span v-if="showResult && !isBlankCorrect(segment.blankId)" class="correct-answer-hint">
              ({{ getCorrectAnswer(segment.blankId) }})
            </span>
          </template>
        </component>
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
        label="Clear All"
        icon="pi pi-refresh"
        text
        :disabled="!hasAnyAnswers"
        @click="clearAllAnswers"
      />
      <Button
        v-if="!showResult"
        label="Submit Answer"
        icon="pi pi-check"
        :disabled="!allBlanksAnswered"
        @click="submitAnswer"
      />
      <Button
        v-else
        label="Try Again"
        icon="pi pi-refresh"
        @click="resetExercise"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import Button from 'primevue/button'
import Message from 'primevue/message'
import InputText from 'primevue/inputtext'

interface ClozeBlank {
  id: string
  correctAnswer: string | string[]
}

interface ClozeContent {
  text: string
  blanks: ClozeBlank[]
  hint?: string
}

interface TextSegment {
  type: 'text' | 'blank'
  content?: string
  blankId?: string
}

interface Props {
  content: ClozeContent
}

const props = defineProps<Props>()
const emit = defineEmits<{
  submit: [response: { answers: Record<string, string> }]
}>()

const userAnswers = ref<Record<string, string>>({})
const showResult = ref(false)
const isCorrect = ref(false)
const feedback = ref('')
const showHint = ref(false)
const correctAnswersMap = ref<Record<string, string>>({})

const hint = computed(() => props.content.hint || '')
const textSegments = ref<TextSegment[]>([])

const hasAnyAnswers = computed(() => {
  return Object.values(userAnswers.value).some(answer => answer && answer.trim().length > 0)
})

const allBlanksAnswered = computed(() => {
  return props.content.blanks.every(blank => {
    const answer = userAnswers.value[blank.id]
    return answer && answer.trim().length > 0
  })
})

onMounted(() => {
  initializeExercise()
})

function initializeExercise() {
  // Parse the text and create segments
  parseTextIntoSegments()

  // Initialize user answers
  props.content.blanks.forEach(blank => {
    userAnswers.value[blank.id] = ''
  })
}

function parseTextIntoSegments() {
  const segments: TextSegment[] = []
  const regex = /___(\d+)___/g
  let lastIndex = 0
  let match

  while ((match = regex.exec(props.content.text)) !== null) {
    // Add text before the blank
    if (match.index > lastIndex) {
      segments.push({
        type: 'text',
        content: props.content.text.substring(lastIndex, match.index)
      })
    }

    // Add the blank
    segments.push({
      type: 'blank',
      blankId: match[1]
    })

    lastIndex = regex.lastIndex
  }

  // Add remaining text
  if (lastIndex < props.content.text.length) {
    segments.push({
      type: 'text',
      content: props.content.text.substring(lastIndex)
    })
  }

  textSegments.value = segments
}

function isBlankCorrect(blankId: string): boolean {
  const correctAnswer = correctAnswersMap.value[blankId]
  if (!correctAnswer) return false

  const userAnswer = userAnswers.value[blankId]
  if (!userAnswer) return false

  return userAnswer.trim().toLowerCase() === correctAnswer.toLowerCase()
}

function getCorrectAnswer(blankId: string): string {
  return correctAnswersMap.value[blankId] || ''
}

function toggleHint() {
  showHint.value = !showHint.value
}

function clearAllAnswers() {
  props.content.blanks.forEach(blank => {
    userAnswers.value[blank.id] = ''
  })
}

function handleEnterKey() {
  if (allBlanksAnswered.value && !showResult.value) {
    submitAnswer()
  }
}

function submitAnswer() {
  emit('submit', { answers: userAnswers.value })
}

function resetExercise() {
  clearAllAnswers()
  showResult.value = false
  isCorrect.value = false
  feedback.value = ''
  showHint.value = false
  correctAnswersMap.value = {}
}

function setResult(result: { isCorrect: boolean; feedback: string; userResponse?: any; correctAnswers?: any }) {
  showResult.value = true
  isCorrect.value = result.isCorrect
  feedback.value = result.feedback

  if (result.correctAnswers?.correctAnswers) {
    correctAnswersMap.value = result.correctAnswers.correctAnswers
  }

  if (result.userResponse?.answers) {
    userAnswers.value = result.userResponse.answers
  }
}

defineExpose({
  setResult
})
</script>

<style scoped>
.cloze-reading-exercise {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.passage-section {
  padding: 2rem;
  background: var(--surface-ground);
  border-radius: 8px;
  border: 2px solid var(--surface-border);
}

.passage-content {
  font-size: 1.125rem;
  line-height: 2;
  color: var(--text-color);
  font-family: Georgia, 'Times New Roman', serif;
}

.blank-wrapper {
  display: inline-block;
  margin: 0 0.25rem;
}

.blank-input {
  display: inline-block;
  width: auto;
  min-width: 80px;
  max-width: 200px;
  padding: 0.25rem 0.5rem;
  font-size: 1rem;
  border: 2px solid var(--primary-color);
  border-radius: 4px;
  text-align: center;
  font-family: inherit;
  transition: all 0.2s ease;
}

.blank-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.blank-input.correct {
  border-color: var(--green-500);
  background: var(--green-50);
  color: var(--green-700);
}

.blank-input.incorrect {
  border-color: var(--red-500);
  background: var(--red-50);
  color: var(--red-700);
}

.blank-input:disabled {
  opacity: 1;
  cursor: default;
}

.correct-answer-hint {
  display: inline-block;
  margin-left: 0.5rem;
  color: var(--green-600);
  font-weight: 500;
  font-size: 0.95rem;
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
  .passage-section {
    padding: 1.5rem;
  }

  .passage-content {
    font-size: 1rem;
    line-height: 1.8;
  }

  .blank-input {
    min-width: 60px;
    max-width: 150px;
  }

  .actions-section {
    justify-content: stretch;
  }

  .actions-section button {
    flex: 1;
  }
}
</style>
