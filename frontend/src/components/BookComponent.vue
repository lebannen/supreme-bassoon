<template>
  <div class="book-container">
    <div class="book">
      <div class="page-wrapper">
        <!-- Left static page (hide during backward flip) -->
        <div class="page left-page" :class="{ hidden: isFlipping && flipDirection === 'backward' }">
          <div class="page-content">
            <div class="page-number">{{ currentPage * 2 - 1 }}</div>
            <div class="page-text">
              <slot name="left-page" :page="currentPage * 2 - 1">
                {{ getPageContent(currentPage * 2 - 1) }}
              </slot>
            </div>
          </div>
        </div>

        <!-- Right static page (hide during forward flip) -->
        <div class="page right-page" :class="{ hidden: isFlipping && flipDirection === 'forward' }">
          <div class="page-content">
            <div class="page-number">{{ currentPage * 2 }}</div>
            <div class="page-text">
              <slot name="right-page" :page="currentPage * 2">
                {{ getPageContent(currentPage * 2) }}
              </slot>
            </div>
          </div>
        </div>

        <!-- Next right page (visible behind flipping page during forward navigation) -->
        <div
          v-if="isFlipping && flipDirection === 'forward'"
          class="page right-page underneath-page underneath-page-right"
        >
          <div class="page-content">
            <div class="page-number">{{ currentPage * 2 + 2 }}</div>
            <div class="page-text">
              {{ getPageContent(currentPage * 2 + 2) }}
            </div>
          </div>
        </div>

        <!-- Previous left page (visible behind flipping page during backward navigation) -->
        <div
          v-if="isFlipping && flipDirection === 'backward'"
          class="page left-page underneath-page underneath-page-left"
        >
          <div class="page-content">
            <div class="page-number">{{ currentPage * 2 - 3 }}</div>
            <div class="page-text">
              {{ getPageContent(currentPage * 2 - 3) }}
            </div>
          </div>
        </div>

        <!-- Flipping page for forward navigation -->
        <div
          v-if="isFlipping && flipDirection === 'forward'"
          class="page flipping-page flipping-forward"
        >
          <div class="page-content front">
            <div class="page-number">{{ currentPage * 2 }}</div>
            <div class="page-text">
              {{ getPageContent(currentPage * 2) }}
            </div>
          </div>
          <div class="page-content back">
            <div class="page-number">{{ currentPage * 2 + 1 }}</div>
            <div class="page-text">
              {{ getPageContent(currentPage * 2 + 1) }}
            </div>
          </div>
        </div>

        <!-- Flipping page for backward navigation -->
        <div
          v-if="isFlipping && flipDirection === 'backward'"
          class="page flipping-page flipping-backward"
        >
          <div class="page-content front">
            <div class="page-number">{{ currentPage * 2 - 1 }}</div>
            <div class="page-text">
              {{ getPageContent(currentPage * 2 - 1) }}
            </div>
          </div>
          <div class="page-content back">
            <div class="page-number">{{ currentPage * 2 - 2 }}</div>
            <div class="page-text">
              {{ getPageContent(currentPage * 2 - 2) }}
            </div>
          </div>
        </div>
      </div>

      <!-- Book spine -->
      <div class="spine"></div>
    </div>

    <!-- Navigation controls -->
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
import { ref, computed } from 'vue'

interface Props {
  pages?: string[]
  content?: string
  pageSize?: number
}

const props = withDefaults(defineProps<Props>(), {
  pages: () => [],
  content: '',
  pageSize: 300
})

const currentPage = ref(1)
const isFlipping = ref(false)
const flipDirection = ref<'forward' | 'backward'>('forward')

const totalPages = computed(() => {
  if (props.pages.length > 0) {
    return props.pages.length
  }
  if (props.content) {
    // Split content into pages based on pageSize
    return Math.ceil(props.content.length / props.pageSize)
  }
  return 2
})

function getPageContent(pageNum: number): string {
  if (pageNum > totalPages.value || pageNum < 1) return ''

  if (props.pages.length > 0) {
    return props.pages[pageNum - 1] || ''
  }

  if (props.content) {
    const start = (pageNum - 1) * props.pageSize
    const end = start + props.pageSize
    return props.content.substring(start, end)
  }

  return `Page ${pageNum} content`
}

async function nextPage() {
  if (currentPage.value * 2 >= totalPages.value || isFlipping.value) return

  flipDirection.value = 'forward'
  isFlipping.value = true

  setTimeout(() => {
    currentPage.value++
    isFlipping.value = false
  }, 600)
}

async function previousPage() {
  if (currentPage.value === 1 || isFlipping.value) return

  flipDirection.value = 'backward'
  isFlipping.value = true

  setTimeout(() => {
    currentPage.value--
    isFlipping.value = false
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
  background: linear-gradient(to bottom,
    var(--surface-ground) 0%,
    var(--surface-section) 50%,
    var(--surface-ground) 100%);
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

.page-content.front {
  transform: rotateY(0deg);
}

.page-content.back {
  transform: rotateY(180deg);
}

.left-page .page-content {
  border-radius: 8px 0 0 8px;
}

.right-page .page-content {
  border-radius: 0 8px 8px 0;
}

.flipping-forward .page-content.front {
  border-radius: 0 8px 8px 0;
}

.flipping-forward .page-content.back {
  border-radius: 8px 0 0 8px;
}

.flipping-backward .page-content.front {
  border-radius: 8px 0 0 8px;
}

.flipping-backward .page-content.back {
  border-radius: 0 8px 8px 0;
}

.page-number {
  position: absolute;
  bottom: 2rem;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  font-family: serif;
}

.left-page .page-number {
  left: 2.5rem;
}

.right-page .page-number {
  right: 2.5rem;
}

.flipping-forward .page-content.front .page-number {
  right: 2.5rem;
}

.flipping-forward .page-content.back .page-number {
  left: 2.5rem;
}

.flipping-backward .page-content.front .page-number {
  left: 2.5rem;
}

.flipping-backward .page-content.back .page-number {
  right: 2.5rem;
}

.page-text {
  font-family: 'Georgia', serif;
  font-size: 1rem;
  line-height: 1.8;
  color: var(--text-color);
  text-align: justify;
  white-space: pre-wrap;
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

  .page-content {
    padding: 2rem 1.5rem;
  }

  .page-text {
    font-size: 0.9rem;
  }
}

@media (max-width: 650px) {
  .book {
    width: 400px;
    height: 300px;
  }

  .page-content {
    padding: 1.5rem 1rem;
  }

  .page-text {
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
