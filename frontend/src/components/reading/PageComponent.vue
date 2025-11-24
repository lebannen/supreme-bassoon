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
  const cleanedWord = word.replace(/^[.,;:"?!(){}[\]]+|[.,;"?!(){}[\]]+$/g, '').toLowerCase()
  if (cleanedWord) {
    emit('word-click', cleanedWord)
  }
}
</script>

<template>
  <div class="page-content">
    <div class="page-number text-secondary">{{ pageNumber }}</div>
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

<style scoped>
.page-content {
  position: absolute;
  width: 100%;
  height: 100%;
  padding: 3rem 2.5rem;
  backface-visibility: hidden;
  overflow-y: auto;
  background-color: var(--surface-card);
}

.page-number {
  position: absolute;
  bottom: 1.5rem;
  font-size: 0.875rem;
}

.page-text {
  font-family: 'Georgia', serif;
  font-size: 1rem;
  line-height: 1.8;
  text-align: justify;
}

.word {
  cursor: pointer;
  transition: background-color 0.2s;
  border-radius: 3px;
  padding: 2px 1px;
}

.word:hover {
  background-color: var(--surface-hover);
}

@media (max-width: 768px) {
  .page-content {
    padding: 2rem 1.5rem;
  }

  .page-text {
    font-size: 0.9rem;
  }
}
</style>
