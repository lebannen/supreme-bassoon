<template>
  <div class="book-container">
    <!-- Audio Player -->
    <AudioPlayer v-if="audioUrl" :audio-url="audioUrl" />

    <div class="book">
      <div class="page-wrapper">
        <!-- Left static page -->
        <div class="page left-page" :class="{ hidden: isFlipping && flipDirection === 'backward' }">
          <PageComponent
            :page-number="currentPage * 2 - 1"
            :content="getPageContent(currentPage * 2 - 1)"
            @word-click="$emit('word-click', $event)"
          />
        </div>

        <!-- Right static page -->
        <div class="page right-page" :class="{ hidden: isFlipping && flipDirection === 'forward' }">
          <PageComponent
            :page-number="currentPage * 2"
            :content="getPageContent(currentPage * 2)"
            @word-click="$emit('word-click', $event)"
          />
        </div>

        <!-- Next right page (underneath) -->
        <div
            v-if="isFlipping && flipDirection === 'forward'"
            class="page right-page underneath-page underneath-page-right"
        >
          <PageComponent
            :page-number="currentPage * 2 + 2"
            :content="getPageContent(currentPage * 2 + 2)"
            @word-click="$emit('word-click', $event)"
          />
        </div>

        <!-- Previous left page (underneath) -->
        <div
            v-if="isFlipping && flipDirection === 'backward'"
            class="page left-page underneath-page underneath-page-left"
        >
          <PageComponent
            :page-number="currentPage * 2 - 3"
            :content="getPageContent(currentPage * 2 - 3)"
            @word-click="$emit('word-click', $event)"
          />
        </div>

        <!-- Flipping page (forward) -->
        <div
            v-if="isFlipping && flipDirection === 'forward'"
            class="page flipping-page flipping-forward"
        >
          <PageComponent
            class="front"
            :page-number="currentPage * 2"
            :content="getPageContent(currentPage * 2)"
            @word-click="$emit('word-click', $event)"
          />
          <PageComponent
            class="back"
            :page-number="currentPage * 2 + 1"
            :content="getPageContent(currentPage * 2 + 1)"
            @word-click="$emit('word-click', $event)"
          />
        </div>

        <!-- Flipping page (backward) -->
        <div
            v-if="isFlipping && flipDirection === 'backward'"
            class="page flipping-page flipping-backward"
        >
          <PageComponent
            class="front"
            :page-number="currentPage * 2 - 1"
            :content="getPageContent(currentPage * 2 - 1)"
            @word-click="$emit('word-click', $event)"
          />
          <PageComponent
            class="back"
            :page-number="currentPage * 2 - 2"
            :content="getPageContent(currentPage * 2 - 2)"
            @word-click="$emit('word-click', $event)"
          />
        </div>
      </div>

      <div class="spine"></div>
    </div>

    <div class="book-controls">
      <button
          class="control-btn prev-btn"
          @click="previousPage"
          :disabled="currentPage === 1 || isFlipping"
      >
        <i class="pi pi-chevron-left"></i>
        Previous
      </button>
      <span class="page-indicator">
        Page {{ currentPage * 2 - 1 }}-{{ currentPage * 2 }} of {{ totalPages }}
      </span>
      <button
          class="control-btn next-btn"
          @click="nextPage"
          :disabled="currentPage * 2 >= totalPages || isFlipping"
      >
        Next
        <i class="pi pi-chevron-right"></i>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue'
import PageComponent from './PageComponent.vue'
import AudioPlayer from '@/components/audio/AudioPlayer.vue'

interface Props {
  pages?: string[]
  content?: string
  pageSize?: number
  audioUrl?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  pages: () => [],
  content: '',
  pageSize: 300,
  audioUrl: null,
})

const emit = defineEmits(['word-click', 'page-change'])

const currentPage = ref(1)
const isFlipping = ref(false)
const flipDirection = ref<'forward' | 'backward'>('forward')

const bookPages = computed(() => {
  if (props.pages.length > 0) {
    return props.pages
  }
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
  flipDirection.value = 'forward'
  isFlipping.value = true
  setTimeout(() => {
    currentPage.value++
    isFlipping.value = false
    emit('page-change', currentPage.value * 2 - 1, totalPages.value)
  }, 600)
}

function previousPage() {
  if (currentPage.value === 1 || isFlipping.value) return
  flipDirection.value = 'backward'
  isFlipping.value = true
  setTimeout(() => {
    currentPage.value--
    isFlipping.value = false
    emit('page-change', currentPage.value * 2 - 1, totalPages.value)
  }, 600)
}
</script>

<style scoped>
.book-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rem;
  padding: 2rem;
  perspective: 2000px;
}

.book {
  position: relative;
  width: 800px;
  height: 600px;
  display: flex;
  transform-style: preserve-3d;
  background: linear-gradient(
      to bottom,
      var(--surface-ground) 0%,
      var(--surface-section) 50%,
      var(--surface-ground) 100%
  );
  padding: 20px;
  border-radius: 12px;
}

.page-wrapper {
  display: flex;
  width: 100%;
  height: 100%;
  position: relative;
  background: var(--surface-ground);
  border-radius: 8px;
  transform-style: preserve-3d;
}

.page {
  width: 50%;
  height: 100%;
  background: var(--surface-card);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  position: relative;
  transform-style: preserve-3d;
}

.left-page {
  border-right: 1px solid var(--surface-border);
  border-radius: 8px 0 0 8px;
}

.right-page {
  border-left: 1px solid var(--surface-border);
  border-radius: 0 8px 8px 0;
}

.underneath-page {
  position: absolute;
  top: 0;
  z-index: 1;
}

.underneath-page-right {
  right: 0;
}

.underneath-page-left {
  left: 0;
}

.flipping-page {
  position: absolute;
  width: 50%;
  height: 100%;
  top: 0;
  z-index: 10;
  transform-style: preserve-3d;
  background: var(--surface-card);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.flipping-forward {
  right: 0;
  transform-origin: left center;
  animation: flipForward 0.6s ease-in-out forwards;
  border-left: 1px solid var(--surface-border);
  border-radius: 0 8px 8px 0;
}

.flipping-backward {
  left: 0;
  transform-origin: right center;
  animation: flipBackward 0.6s ease-in-out forwards;
  border-right: 1px solid var(--surface-border);
  border-radius: 8px 0 0 8px;
}

@keyframes flipForward {
  0% {
    transform: rotateY(0deg);
  }
  100% {
    transform: rotateY(-180deg);
  }
}

@keyframes flipBackward {
  0% {
    transform: rotateY(0deg);
  }
  100% {
    transform: rotateY(180deg);
  }
}

:deep(.page-content.front) {
  transform: rotateY(0deg);
}

:deep(.page-content.back) {
  transform: rotateY(180deg);
}

.left-page :deep(.page-content) {
  border-radius: 8px 0 0 8px;
}

.right-page :deep(.page-content) {
  border-radius: 0 8px 8px 0;
}

.flipping-forward :deep(.page-content.front) {
  border-radius: 0 8px 8px 0;
}

.flipping-forward :deep(.page-content.back) {
  border-radius: 8px 0 0 8px;
}

.flipping-backward :deep(.page-content.front) {
  border-radius: 8px 0 0 8px;
}

.flipping-backward :deep(.page-content.back) {
  border-radius: 0 8px 8px 0;
}

.left-page :deep(.page-number) {
  left: 2.5rem;
}

.right-page :deep(.page-number) {
  right: 2.5rem;
}

.flipping-forward :deep(.front .page-number) {
  right: 2.5rem;
}

.flipping-forward :deep(.back .page-number) {
  left: 2.5rem;
}

.flipping-backward :deep(.front .page-number) {
  left: 2.5rem;
}

.flipping-backward :deep(.back .page-number) {
  right: 2.5rem;
}

.spine {
  position: absolute;
  left: 50%;
  top: 5%;
  bottom: 5%;
  width: 3px;
  background: linear-gradient(
      to bottom,
      rgba(0, 0, 0, 0.1),
      rgba(0, 0, 0, 0.2) 50%,
      rgba(0, 0, 0, 0.1)
  );
  transform: translateX(-50%);
  z-index: 1;
}

.book-controls {
  display: flex;
  align-items: center;
  gap: 2rem;
  margin-top: 1rem;
}

.control-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.control-btn:hover:not(:disabled) {
  background: var(--primary-color-dark, #0056b3);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.control-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-indicator {
  font-size: 1rem;
  color: var(--text-color-secondary);
  font-family: serif;
}

.page.hidden {
  visibility: hidden;
}

@media (max-width: 900px) {
  .book {
    width: 600px;
    height: 450px;
  }
  :deep(.page-content) {
    padding: 2rem 1.5rem;
  }
  :deep(.page-text) {
    font-size: 0.9rem;
  }
}

@media (max-width: 650px) {
  .book {
    width: 400px;
    height: 300px;
  }
  :deep(.page-content) {
    padding: 1.5rem 1rem;
  }
  :deep(.page-text) {
    font-size: 0.8rem;
    line-height: 1.6;
  }

  .book-controls {
    flex-direction: column;
    gap: 1rem;
  }

  .control-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
