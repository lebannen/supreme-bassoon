<template>
  <div class="flashcard-container">
    <!-- Progress Header -->
    <Card class="flashcard-header">
      <template #content>
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
          <Tag :value="`Review #${displayedSrsInfo.reviewCount + 1}`" severity="info"/>
          <Tag :value="displayedSrsInfo.currentInterval" severity="secondary"/>
        </div>
      </template>
    </Card>

    <!-- Card Flip Container -->
    <div class="card-wrapper" @click="handleCardClick">
      <div class="flip-card" :class="{ flipped: isFlipped }">
        <!-- Front Face -->
        <Card class="card-face card-front">
          <template #content>
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
          </template>
        </Card>

        <!-- Back Face -->
        <Card class="card-face card-back">
          <template #content>
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
          </template>
        </Card>
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
import Card from 'primevue/card'
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

.flashcard-header :deep(.p-card-content) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.card-position {
  font-weight: 600;
}

.streak-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
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

.flip-card {
  width: 100%;
  height: 100%;
  position: relative;
  transform-style: preserve-3d;
  transition: transform 0.6s linear;
}

.flip-card.flipped {
  transform: rotateY(180deg);
  cursor: default;
}

.card-face {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  overflow: hidden;
}

.card-front {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.card-back {
  transform: rotateY(180deg);
  min-height: 400px;
}

.card-front :deep(.p-card-content),
.card-back :deep(.p-card-content) {
  padding: 3rem;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* Front Face Styles */
.card-front :deep(.p-card-content) {
  align-items: center;
  justify-content: center;
  text-align: center;
}

.card-front .card-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2rem;
  width: 100%;
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
  margin: 0;
}

.flip-hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
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
.card-back :deep(.p-card-content) {
  overflow-y: auto;
}

.card-back .card-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.word-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding-bottom: 1rem;
}

.card-back .word-lemma {
  font-size: 2rem;
  font-weight: 600;
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
  line-height: 1.6;
  font-size: 1.1rem;
}

.definition-number {
  font-weight: 600;
  margin-right: 0.5rem;
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
  .flashcard-header :deep(.p-card-content) {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .card-front :deep(.p-card-content),
  .card-back :deep(.p-card-content) {
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
