<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue'
import AudioPlayer from '@/components/audio/AudioPlayer.vue'

interface Props {
  content: string
  audioUrl?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  audioUrl: null,
})

const emit = defineEmits(['word-click', 'scroll-progress'])

const readerRef = ref<HTMLElement | null>(null)
const scrollProgress = ref(0)

const paragraphs = computed(() => {
  return props.content.split('\n\n').filter(p => p.trim().length > 0)
})

function onWordClick(word: string, event: MouseEvent) {
  event.preventDefault()
  const cleanedWord = word.replace(/^[.,;:"?!(){}[\]]+|[.,;"?!(){}[\]]+$/g, '').toLowerCase()
  if (cleanedWord) {
    emit('word-click', cleanedWord)
  }
}

function handleScroll() {
  if (!readerRef.value) return

  const element = readerRef.value
  const scrollTop = element.scrollTop
  const scrollHeight = element.scrollHeight - element.clientHeight

  if (scrollHeight > 0) {
    scrollProgress.value = Math.round((scrollTop / scrollHeight) * 100)
    emit('scroll-progress', scrollProgress.value)
  }
}

onMounted(() => {
  if (readerRef.value) {
    readerRef.value.addEventListener('scroll', handleScroll)
  }
})

onUnmounted(() => {
  if (readerRef.value) {
    readerRef.value.removeEventListener('scroll', handleScroll)
  }
})
</script>

<template>
  <div class="scroll-reader-container">
    <AudioPlayer v-if="audioUrl" :audio-url="audioUrl" class="w-full max-w-3xl mb-4"/>

    <div class="reading-progress-bar">
      <div class="progress-fill" :style="{ width: `${scrollProgress}%` }"></div>
    </div>

    <div ref="readerRef" class="reader-content">
      <div class="text-content">
        <div v-for="(paragraph, index) in paragraphs" :key="index" class="paragraph">
          <template v-for="(word, wordIndex) in paragraph.split(/\s+/)" :key="wordIndex">
            <span class="word" @click="onWordClick(word, $event)">{{ word }}</span>{{ ' ' }}
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.scroll-reader-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 900px;
  margin: 0 auto;
  padding: 0 2rem;
}

.reading-progress-bar {
  position: sticky;
  top: 0;
  width: 100%;
  height: 4px;
  background-color: var(--surface-border);
  border-radius: 2px;
  overflow: hidden;
  z-index: 10;
  margin-bottom: 1rem;
}

.progress-fill {
  height: 100%;
  background: var(--primary-color);
  transition: width 0.1s ease-out;
}

.reader-content {
  flex: 1;
  overflow-y: auto;
  padding: 2rem 0;
  scroll-behavior: smooth;
}

.text-content {
  background-color: var(--surface-card);
  border-radius: 8px;
  padding: 3rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  min-height: 100%;
}

.paragraph {
  font-family: var(--font-family);
  font-size: 1rem;
  line-height: 1.7;
  margin-bottom: 1.5rem;
  color: var(--text-color);
}

.paragraph:last-child {
  margin-bottom: 0;
}

.word {
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 3px;
  padding: 2px 1px;
  display: inline;
}

.word:hover {
  background-color: var(--primary-50);
  color: var(--primary-600);
}

.dark-theme .word:hover {
  background-color: var(--primary-900);
  color: var(--primary-200);
}

@media (max-width: 768px) {
  .scroll-reader-container {
    padding: 0 1rem;
  }

  .text-content {
    padding: 2rem 1.5rem;
  }

  .paragraph {
    font-size: 1rem;
  }
}
</style>
