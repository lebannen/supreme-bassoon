<template>
  <div class="page-content">
    <div class="page-number">{{ pageNumber }}</div>
    <div class="page-text">
      <slot>
        <template v-for="(word, index) in words" :key="index">
          <span class="word" @click="onWordClick(word)">{{ word }}</span
          >{{ ' ' }}
        </template>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'

interface Props {
  pageNumber?: number
  content?: string
}

const props = defineProps<Props>()
const emit = defineEmits(['word-click'])

const words = computed(() => {
  if (!props.content) return []
  return props.content.split(/\s+/).filter((w) => w.length > 0)
})

function onWordClick(word: string) {
  // Clean punctuation from start and end of the word
  const cleanedWord = word.replace(/^[.,;:"?!(){}[\]]+|[.,;"?!(){}[\]]+$/g, '').toLowerCase()
  if (cleanedWord) {
    emit('word-click', cleanedWord)
  }
}
</script>

<style scoped>
.page-content {
  position: absolute;
  width: 100%;
  height: 100%;
  padding: 3rem 2.5rem;
  backface-visibility: hidden;
  overflow: hidden;
  background: #f5f5dc;
}

@media (prefers-color-scheme: dark) {
  .page-content {
    background: #2a2a2a;
  }
}

.page-number {
  position: absolute;
  bottom: 2rem;
  font-size: 0.875rem;
  font-family: serif;
}

.page-text {
  font-family: 'Georgia', serif;
  font-size: 1rem;
  line-height: 1.8;
  text-align: justify;
  white-space: pre-wrap;
}

.word {
  cursor: pointer;
  transition: background-color 0.2s;
  border-radius: 3px;
  padding: 2px 1px;
  margin: 0 -1px;
}

.word:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

@media (prefers-color-scheme: dark) {
  .word:hover {
    background-color: rgba(255, 255, 255, 0.1);
  }
}
</style>
