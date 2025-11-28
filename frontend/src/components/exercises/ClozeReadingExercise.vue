<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
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
  next: []
}>()

const userAnswers = ref<Record<string, string>>({})
const showResult = ref(false)
const isCorrect = ref(false)
const feedback = ref('')
const showHint = ref(false)
const correctAnswersMap = ref<Record<string, string[]>>({})
const autoAdvanceTimer = ref<any>(null)
const autoAdvanceSeconds = ref(5)

const textSegments = ref<TextSegment[]>([])
const hasAnyAnswers = computed(() => Object.values(userAnswers.value).some(a => a && a.trim().length > 0))
const allBlanksAnswered = computed(() => props.content.blanks.every(b => userAnswers.value[b.id]?.trim()))

onMounted(initializeExercise)

function initializeExercise() {
  parseTextIntoSegments()
  props.content.blanks.forEach(blank => {
    userAnswers.value[blank.id] = ''
    correctAnswersMap.value[blank.id] = Array.isArray(blank.correctAnswer) ? blank.correctAnswer : [blank.correctAnswer]
  })
}

function parseTextIntoSegments() {
  const segments: TextSegment[] = []
  const regex = /\{blank(\d+)\}/g
  let lastIndex = 0
  let match

  while ((match = regex.exec(props.content.text)) !== null) {
    if (match.index > lastIndex) {
      segments.push({type: 'text', content: props.content.text.substring(lastIndex, match.index)})
    }
    segments.push({type: 'blank', blankId: `blank${match[1]}`})
    lastIndex = regex.lastIndex
  }

  if (lastIndex < props.content.text.length) {
    segments.push({type: 'text', content: props.content.text.substring(lastIndex)})
  }
  textSegments.value = segments
}

const isBlankCorrect = (blankId: string) => {
  const correct = correctAnswersMap.value[blankId] || []
  const user = userAnswers.value[blankId]?.trim().toLowerCase()
  return correct.some(c => c.toLowerCase() === user)
}

const getCorrectAnswer = (blankId: string) => (correctAnswersMap.value[blankId] || [])[0] || ''

function submitAnswer() {
  if (!allBlanksAnswered.value) return
  emit('submit', { answers: userAnswers.value })
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

function resetExercise() {
  stopAutoAdvance()
  Object.keys(userAnswers.value).forEach(key => userAnswers.value[key] = '')
  showResult.value = false
  isCorrect.value = false
  feedback.value = ''
  showHint.value = false
}

function setResult(result: { isCorrect: boolean; feedback: string; userResponse?: any; correctAnswers?: any }) {
  showResult.value = true
  isCorrect.value = result.isCorrect
  feedback.value = result.feedback
  if (result.userResponse?.answers) {
    userAnswers.value = {...userAnswers.value, ...result.userResponse.answers}
  }
  if (result.isCorrect) {
    startAutoAdvance()
  }
}

defineExpose({setResult})
</script>

<template>
  <div class="cloze-exercise">
    <div class="p-4 bg-surface-card rounded-lg">
      <div class="passage-text">
        <template v-for="(segment, index) in textSegments" :key="index">
          <span v-if="segment.type === 'text'">{{ segment.content }}</span>
          <span v-else-if="segment.type === 'blank' && segment.blankId" class="blank-wrapper">
            <InputText
                v-model="userAnswers[segment.blankId]"
                :disabled="showResult"
                class="blank-input"
                :class="{
                'correct': showResult && isBlankCorrect(segment.blankId),
                'incorrect': showResult && !isBlankCorrect(segment.blankId)
              }"
                :placeholder="segment.blankId.replace('blank', '')"
                @keyup.enter="allBlanksAnswered && !showResult && submitAnswer()"
            />
            <span v-if="showResult && !isBlankCorrect(segment.blankId)" class="correct-answer-hint">
              ({{ getCorrectAnswer(segment.blankId) }})
            </span>
          </span>
        </template>
      </div>
    </div>

    <Message v-if="showHint" severity="secondary">{{ content.hint }}</Message>
    <Message v-if="showResult && feedback" :severity="isCorrect ? 'success' : 'warn'">{{ feedback }}</Message>

    <div class="actions-section">
      <Button v-if="!showResult && content.hint" label="Hint" icon="pi pi-lightbulb" text
              @click="showHint = !showHint"/>
      <Button v-if="!showResult" label="Clear" icon="pi pi-refresh" text @click="resetExercise"
              :disabled="!hasAnyAnswers"/>
      <div class="flex-grow"></div>
      <Button v-if="!showResult" label="Submit" icon="pi pi-check" :disabled="!allBlanksAnswered"
              @click="submitAnswer"/>
      <template v-else>
        <Button v-if="isCorrect" :label="`Next (${autoAdvanceSeconds}s)`" icon="pi pi-arrow-right" @click="handleNext"/>
        <Button v-else label="Try Again" icon="pi pi-refresh" @click="resetExercise"/>
      </template>
    </div>
  </div>
</template>

<style scoped>
.cloze-exercise {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.passage-text {
  font-size: 1.25rem;
  line-height: 2.5;
  font-family: 'Georgia', serif;
}
.blank-wrapper {
  display: inline-flex;
  flex-direction: column;
  vertical-align: bottom;
  margin: 0 0.2rem;
}
.blank-input {
  width: 120px;
  font-size: 1.1rem;
  text-align: center;
  padding: 0.25rem 0.5rem;
  border-width: 2px;
}
.blank-input.correct {
  border-color: var(--p-green-500);
}
.blank-input.incorrect {
  border-color: var(--p-red-500);
}
.correct-answer-hint {
  font-size: 0.8rem;
  color: var(--p-green-600);
  font-weight: bold;
  text-align: center;
}

.dark-theme .correct-answer-hint {
  color: var(--p-green-400);
}
.actions-section {
  display: flex;
  gap: 0.5rem;
}
</style>
