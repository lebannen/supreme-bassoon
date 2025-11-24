<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import PageComponent from './PageComponent.vue'
import AudioPlayer from '@/components/audio/AudioPlayer.vue'
import Button from 'primevue/button'

interface Props {
  pages?: string[]
  content?: string
  pageSize?: number
  audioUrl?: string | null
  currentPage?: number
}

const props = withDefaults(defineProps<Props>(), {
  pages: () => [],
  content: '',
  pageSize: 300,
  audioUrl: null,
  currentPage: 1,
})

const emit = defineEmits(['word-click', 'page-change', 'update:current-page'])

const currentPage = ref(props.currentPage)
const isFlipping = ref(false)
const flipDirection = ref<'forward' | 'backward'>('forward')

// Sync prop changes with local ref (in case parent resets the page)
watch(() => props.currentPage, (newPage) => {
  currentPage.value = newPage
})

const bookPages = computed(() => {
  if (props.pages.length > 0) return props.pages
  if (props.content) {
    const result = []
    for (let i = 0; i < props.content.length; i += props.pageSize) {
      result.push(props.content.substring(i, i + props.pageSize))
    }
    return result
  }
  return ['Page 1 content', 'Page 2 content']
})

const totalPages = computed(() => bookPages.value.length)

function getPageContent(pageNum: number): string {
  if (pageNum < 1 || pageNum > totalPages.value) return ''
  return bookPages.value[pageNum - 1] || ''
}

function nextPage() {
  if (currentPage.value * 2 >= totalPages.value || isFlipping.value) return
  console.log('Next page clicked. Current page:', currentPage.value)
  flipDirection.value = 'forward'
  isFlipping.value = true
  setTimeout(() => {
    currentPage.value++
    emit('update:current-page', currentPage.value)
    console.log('Page updated to:', currentPage.value)
    emit('page-change', currentPage.value * 2 - 1, totalPages.value)
  }, 400)
  setTimeout(() => {
    isFlipping.value = false
    console.log('Flip animation complete. Current page:', currentPage.value)
  }, 600)
}

function previousPage() {
  if (currentPage.value === 1 || isFlipping.value) return
  flipDirection.value = 'backward'
  isFlipping.value = true
  setTimeout(() => {
    currentPage.value--
    emit('update:current-page', currentPage.value)
    emit('page-change', currentPage.value * 2 - 1, totalPages.value)
  }, 400)
  setTimeout(() => {
    isFlipping.value = false
  }, 600)
}
</script>

<template>
  <div class="book-container">
    <AudioPlayer v-if="audioUrl" :audio-url="audioUrl" class="w-full max-w-3xl"/>

    <div class="book">
      <div class="page-wrapper">
        <!-- Static Pages -->
        <div v-if="!isFlipping" class="page left-page" :key="`left-${currentPage}`">
          <PageComponent :page-number="currentPage * 2 - 1" :content="getPageContent(currentPage * 2 - 1)"
                         @word-click="$emit('word-click', $event)"/>
        </div>
        <div v-if="!isFlipping" class="page right-page" :key="`right-${currentPage}`">
          <PageComponent :page-number="currentPage * 2" :content="getPageContent(currentPage * 2)"
                         @word-click="$emit('word-click', $event)"/>
        </div>

        <!-- Flipping Page -->
        <div v-if="isFlipping" class="page flipping-page" :class="`flipping-${flipDirection}`">
          <div class="front">
            <PageComponent
                :page-number="flipDirection === 'forward' ? currentPage * 2 : currentPage * 2 - 1"
                :content="flipDirection === 'forward' ? getPageContent(currentPage * 2) : getPageContent(currentPage * 2 - 1)"
                @word-click="$emit('word-click', $event)"
            />
          </div>
          <div class="back">
            <PageComponent
                :page-number="flipDirection === 'forward' ? currentPage * 2 + 1 : currentPage * 2 - 2"
                :content="flipDirection === 'forward' ? getPageContent(currentPage * 2 + 1) : getPageContent(currentPage * 2 - 2)"
                @word-click="$emit('word-click', $event)"
            />
          </div>
        </div>
      </div>
      <div class="spine"></div>
    </div>

    <div class="book-controls">
      <Button @click="previousPage" :disabled="currentPage === 1 || isFlipping" icon="pi pi-arrow-left"
              label="Previous"/>
      <span class="page-indicator text-secondary">
        Page {{ currentPage * 2 - 1 }} of {{ totalPages }}
      </span>
      <Button @click="nextPage" :disabled="currentPage * 2 >= totalPages || isFlipping" icon="pi pi-arrow-right"
              iconPos="right" label="Next"/>
    </div>
  </div>
</template>

<style scoped>
.book-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rem;
  padding: 2rem;
  perspective: 2500px;
}

.book {
  position: relative;
  width: 90vw;
  max-width: 1200px;
  aspect-ratio: 1.5;
  transform-style: preserve-3d;
}

.page-wrapper {
  display: flex;
  width: 100%;
  height: 100%;
  position: relative;
  transform-style: preserve-3d;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  border-radius: 8px;
}

.page {
  width: 50%;
  height: 100%;
  background-color: var(--surface-card);
  position: absolute;
  transform-style: preserve-3d;
}

.left-page {
  left: 0;
  border-right: 1px solid var(--surface-border);
  border-radius: 8px 0 0 8px;
}

.right-page {
  right: 0;
  border-left: 1px solid var(--surface-border);
  border-radius: 0 8px 8px 0;
}

.flipping-page {
  z-index: 10;
}

.flipping-forward {
  right: 0;
  transform-origin: left center;
  animation: flipForward 0.8s ease-in-out forwards;
}

.flipping-backward {
  left: 0;
  transform-origin: right center;
  animation: flipBackward 0.8s ease-in-out forwards;
}

@keyframes flipForward {
  from {
    transform: rotateY(0deg);
  }
  to {
    transform: rotateY(-180deg);
  }
}

@keyframes flipBackward {
  from {
    transform: rotateY(0deg);
  }
  to {
    transform: rotateY(180deg);
  }
}

.front, .back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
}

.front {
  transform: rotateY(0deg);
}

.back {
  transform: rotateY(180deg);
}

.spine {
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--surface-border);
  transform: translateX(-50%);
  z-index: 1;
}

.book-controls {
  display: flex;
  align-items: center;
  gap: 2rem;
  margin-top: 1rem;
}

.page-indicator {
  font-weight: 600;
}

@media (max-width: 768px) {
  .book-container {
    padding: 1rem;
  }
  .book-controls {
    gap: 1rem;
  }
}
</style>
