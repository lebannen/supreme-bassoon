<template>
  <div class="flashcard-container">
    <!-- Progress Header -->
    <div class="flashcard-header">
      <div class="progress-info">
        <span class="card-position"
        >Card {{ displayedProgress.position }} of {{ displayedProgress.total }}</span
        >
        <div class="streak-info">
          <i class="pi pi-bolt"></i>
          <span
          >{{ displayedProgress.currentStreak }}/{{ displayedProgress.needsStreak }} streak</span
          >
        </div>
      </div>
      <div class="srs-info">
        <Tag :value="`Review #${displayedSrsInfo.reviewCount + 1}`" severity="info" />
        <Tag :value="displayedSrsInfo.currentInterval" severity="secondary" />
      </div>
    </div>

    <!-- Card Flip Container -->
    <div class="card-wrapper" @click="handleCardClick">
      <div class="card" :class="{ flipped: isFlipped }">
        <!-- Front Face -->
        <div class="card-face card-front">
          <div class="card-content">
            <div class="word-display">
              <h1 class="word-lemma">{{ displayedWord.lemma }}</h1>
              <Tag
                  v-if="displayedWord.partOfSpeech"
                  :value="displayedWord.partOfSpeech"
                  severity="info"
              />
            </div>
            <div class="flip-hint">
              <i class="pi pi-refresh"></i>
              <span>Click to reveal definitions</span>
            </div>
          </div>
        </div>

        <!-- Back Face -->
        <div class="card-face card-back">
          <div class="card-content">
            <div class="word-header">
              <h2 class="word-lemma">{{ displayedWord.lemma }}</h2>
              <Tag
                  v-if="displayedWord.partOfSpeech"
                  :value="displayedWord.partOfSpeech"
                  severity="info"
              />
            </div>

            <div class="definitions-section">
              <div v-for="def in uniqueDefinitions" :key="def.id" class="definition">
                <div class="definition-text">
                  <span class="definition-number">{{ def.definitionNumber }}.</span>
                  {{ def.definitionText }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Action Buttons (only shown when flipped) -->
    <div v-if="isFlipped" class="flashcard-actions">
      <Button
        label="I need practice"
        icon="pi pi-times"
        severity="danger"
        @click="handleAnswer(false)"
        :loading="loading"
        outlined
        size="large"
      />
      <Button
        label="I know it"
        icon="pi pi-check"
        severity="success"
        @click="handleAnswer(true)"
        :loading="loading"
        size="large"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import type {CardProgressDto, SrsInfoDto, WordDetailDto} from '@/types/study'

interface Props {
  word: WordDetailDto
  progress: CardProgressDto
  srsInfo: SrsInfoDto
  loading?: boolean
}

interface Emits {
  (e: 'flip'): void
  (e: 'answer', correct: boolean): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
})

const emit = defineEmits<Emits>()

const isFlipped = ref(false)
const displayedWord = ref(props.word)
const displayedProgress = ref(props.progress)
const displayedSrsInfo = ref(props.srsInfo)

// Remove duplicate definitions based on definitionNumber
const uniqueDefinitions = computed(() => {
  const seen = new Set<number>()
  return displayedWord.value.definitions.filter((def) => {
    if (seen.has(def.definitionNumber)) {
      return false
    }
    seen.add(def.definitionNumber)
    return true
  })
})

// Update displayed content with a delay during flip animation
function updateDisplayedContent() {
  displayedWord.value = props.word
  displayedProgress.value = props.progress
  displayedSrsInfo.value = props.srsInfo
}

// Initialize displayed content
updateDisplayedContent()

function handleCardClick() {
  if (!isFlipped.value) {
    isFlipped.value = true
    emit('flip')
  }
}

function handleAnswer(correct: boolean) {
  emit('answer', correct)
  // Reset flip state will happen when new card loads
}

// Expose reset method for parent to call when new card loads
defineExpose({
  resetFlip: () => {
    // With linear easing, 300ms = exactly 90 degrees (halfway through 600ms)
    // Update slightly earlier to account for rendering lag
    setTimeout(() => {
      updateDisplayedContent()
    }, 295)
    // Start flip animation immediately
    isFlipped.value = false
  },
})
</script>

<style scoped>
.flashcard-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
}

.flashcard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: var(--surface-section);
  border: 1px solid var(--surface-border);
  border-radius: var(--border-radius);
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.card-position {
  font-weight: 600;
  color: var(--text-color);
}

.streak-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-color-secondary);
}

.streak-info i {
  color: var(--orange-500);
}

.srs-info {
  display: flex;
  gap: 0.5rem;
}

/* Card Flip Animation */
.card-wrapper {
  perspective: 1800px;
  min-height: 400px;
  cursor: pointer;
}

.card {
  width: 100%;
  height: 100%;
  position: relative;
  transform-style: preserve-3d;
  transition: transform 0.6s linear;
}

.card.flipped {
  transform: rotateY(180deg);
  cursor: default;
}

.card-face {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1),
  0 2px 4px -1px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.card-front {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  /* Same background as back for consistency */
  background: #ffffff;
  background: var(--surface-card, #ffffff);
}

/* Dark mode support for front */
@media (prefers-color-scheme: dark) {
  .card-front {
    background: #1e1e1e;
    background: var(--surface-card, #1e1e1e);
  }
}

.card-back {
  transform: rotateY(180deg);
  min-height: 400px;
  /* Solid opaque background - fallback to white/dark then theme color */
  background: #ffffff;
  background: var(--surface-card, #ffffff);
}

/* Dark mode support */
@media (prefers-color-scheme: dark) {
  .card-back {
    background: #1e1e1e;
    background: var(--surface-card, #1e1e1e);
  }
}

.card-content {
  padding: 3rem;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* Front Face Styles */
.card-front .card-content {
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 2rem;
}

.word-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.card-front .word-lemma {
  font-size: 3rem;
  font-weight: 700;
  color: var(--text-color);
  margin: 0;
}

.flip-hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-color-secondary);
  font-size: 0.95rem;
  margin-top: 1rem;
}

.flip-hint i {
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* Back Face Styles */
.card-back .card-content {
  gap: 1.5rem;
  overflow-y: auto;
}

.word-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid var(--surface-border);
}

.card-back .word-lemma {
  font-size: 2rem;
  font-weight: 600;
  color: var(--text-color);
  margin: 0;
}

.definitions-section {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.definition {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.definition-text {
  color: var(--text-color);
  line-height: 1.6;
  font-size: 1.1rem;
}

.definition-number {
  font-weight: 600;
  margin-right: 0.5rem;
  color: var(--primary-color);
}

.examples {
  margin-left: 2rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.example {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-style: italic;
  color: var(--text-color-secondary);
  line-height: 1.5;
}

.example i {
  margin-top: 0.25rem;
  font-size: 0.875rem;
}

.example-text {
  flex: 1;
}

/* Action Buttons */
.flashcard-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.flashcard-actions button {
  flex: 1;
  max-width: 250px;
}

/* Responsive */
@media (max-width: 768px) {
  .flashcard-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .card-content {
    padding: 2rem;
  }

  .card-front .word-lemma {
    font-size: 2.5rem;
  }

  .card-back .word-lemma {
    font-size: 1.5rem;
  }

  .flashcard-actions {
    flex-direction: column;
  }

  .flashcard-actions button {
    max-width: 100%;
  }
}
</style>
