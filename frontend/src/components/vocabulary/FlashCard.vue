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

const props = withDefaults(defineProps<Props>(), {loading: false})
const emit = defineEmits<{ (e: 'answer', correct: boolean): void }>()

const isFlipped = ref(false)

const uniqueDefinitions = computed(() => {
  const seen = new Set<number>()
  return props.word.definitions.filter(def => {
    if (seen.has(def.definitionNumber)) return false
    seen.add(def.definitionNumber)
    return true
  })
})

function handleCardClick() {
  if (!isFlipped.value) {
    isFlipped.value = true
  }
}

defineExpose({
  resetFlip: () => {
    isFlipped.value = false
  },
})
</script>

<template>
  <div class="flashcard-container">
    <Card class="flashcard-header">
      <template #content>
        <div class="flex justify-content-between align-items-center w-full">
          <div class="flex align-items-center gap-4">
            <span class="font-bold">Card {{ progress.position }} of {{ progress.total }}</span>
            <span class="flex align-items-center gap-2 text-secondary">
              <i class="pi pi-bolt"></i>
              <span>{{ progress.currentStreak }}/{{ progress.needsStreak }} streak</span>
            </span>
          </div>
          <div class="flex gap-2">
            <Tag :value="`Review #${srsInfo.reviewCount + 1}`" severity="contrast"/>
            <Tag :value="srsInfo.currentInterval" severity="secondary"/>
          </div>
        </div>
      </template>
    </Card>

    <div class="card-wrapper" @click="handleCardClick">
      <div class="flip-card" :class="{ flipped: isFlipped }">
        <!-- Front -->
        <Card class="card-face card-front">
          <template #content>
            <div class="flex flex-column align-items-center justify-content-center text-center gap-6 h-full">
              <div class="flex flex-column align-items-center gap-3">
                <h1 class="text-4xl font-bold m-0">{{ word.lemma }}</h1>
                <Tag v-if="word.partOfSpeech" :value="word.partOfSpeech"/>
              </div>
              <div class="flex align-items-center gap-2 text-secondary">
                <i class="pi pi-refresh"></i>
                <span>Click to reveal</span>
              </div>
            </div>
          </template>
        </Card>
        <!-- Back -->
        <Card class="card-face card-back">
          <template #content>
            <div class="flex flex-column gap-4 h-full">
              <div class="flex align-items-center gap-3 pb-md border-b border-surface">
                <h2 class="text-2xl font-bold m-0">{{ word.lemma }}</h2>
                <Tag v-if="word.partOfSpeech" :value="word.partOfSpeech"/>
              </div>
              <div class="flex-1 overflow-y-auto pr-sm">
                <div v-for="def in uniqueDefinitions" :key="def.id" class="mb-4">
                  <p class="m-0"><span class="font-bold">{{ def.definitionNumber }}.</span> {{ def.definitionText }}</p>
                </div>
              </div>
            </div>
          </template>
        </Card>
      </div>
    </div>

    <div v-if="isFlipped" class="flashcard-actions">
      <Button label="I need practice" icon="pi pi-times" severity="danger" @click="emit('answer', false)"
              :loading="loading" outlined size="large"/>
      <Button label="I know it" icon="pi pi-check" severity="success" @click="emit('answer', true)" :loading="loading"
              size="large"/>
    </div>
  </div>
</template>

<style scoped>
.flashcard-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
  max-width: 700px;
  margin: 0 auto;
}
.flashcard-header :deep(.p-card-content) {
  padding: 1rem 1.5rem;
}
.card-wrapper {
  perspective: 2000px;
  min-height: 350px;
  cursor: pointer;
}
.flip-card {
  width: 100%;
  height: 100%;
  position: relative;
  transform-style: preserve-3d;
  transition: transform 0.6s;
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
  min-height: 350px;
}
.card-back {
  transform: rotateY(180deg);
}

.card-face :deep(.p-card-content) {
  padding: 2rem;
  height: 100%;
}
.flashcard-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
</style>
