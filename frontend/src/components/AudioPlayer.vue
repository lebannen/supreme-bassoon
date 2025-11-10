<template>
  <div v-if="audioUrl" class="audio-player">
    <div class="audio-controls">
      <button
        @click="togglePlay"
        class="play-button"
        :disabled="loading"
      >
        <svg
          v-if="!isPlaying"
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="currentColor"
          class="icon"
        >
          <path d="M8 5v14l11-7z" />
        </svg>
        <svg
          v-else
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="currentColor"
          class="icon"
        >
          <path d="M6 4h4v16H6V4zm8 0h4v16h-4V4z" />
        </svg>
      </button>

      <div class="progress-container">
        <div class="time-display">
          <span class="current-time">{{ formatTime(currentTime) }}</span>
          <span class="duration">{{ formatTime(duration) }}</span>
        </div>
        <input
          type="range"
          min="0"
          :max="duration"
          :value="currentTime"
          @input="seek"
          class="progress-slider"
          :disabled="loading"
        />
      </div>

      <button
        @click="toggleMute"
        class="volume-button"
        :disabled="loading"
      >
        <svg
          v-if="!isMuted"
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="currentColor"
          class="icon"
        >
          <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z" />
        </svg>
        <svg
          v-else
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="currentColor"
          class="icon"
        >
          <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z" />
        </svg>
      </button>
    </div>

    <audio
      ref="audioElement"
      :src="audioUrl"
      @loadedmetadata="onLoadedMetadata"
      @timeupdate="onTimeUpdate"
      @ended="onEnded"
      @error="onError"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

interface Props {
  audioUrl: string | null
}

const props = defineProps<Props>()

const audioElement = ref<HTMLAudioElement | null>(null)
const isPlaying = ref(false)
const isMuted = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const loading = ref(true)

function togglePlay() {
  if (!audioElement.value) return

  if (isPlaying.value) {
    audioElement.value.pause()
    isPlaying.value = false
  } else {
    audioElement.value.play()
    isPlaying.value = true
  }
}

function toggleMute() {
  if (!audioElement.value) return

  audioElement.value.muted = !audioElement.value.muted
  isMuted.value = audioElement.value.muted
}

function seek(event: Event) {
  if (!audioElement.value) return

  const target = event.target as HTMLInputElement
  const time = parseFloat(target.value)
  audioElement.value.currentTime = time
  currentTime.value = time
}

function onLoadedMetadata() {
  if (!audioElement.value) return

  duration.value = audioElement.value.duration
  loading.value = false
}

function onTimeUpdate() {
  if (!audioElement.value) return

  currentTime.value = audioElement.value.currentTime
}

function onEnded() {
  isPlaying.value = false
  currentTime.value = 0
}

function onError(event: Event) {
  console.error('Audio playback error:', event)
  loading.value = false
}

function formatTime(seconds: number): string {
  if (isNaN(seconds) || !isFinite(seconds)) return '0:00'

  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

onMounted(() => {
  if (audioElement.value) {
    audioElement.value.load()
  }
})

onBeforeUnmount(() => {
  if (audioElement.value) {
    audioElement.value.pause()
  }
})
</script>

<style scoped>
.audio-player {
  width: 100%;
  padding: 1rem;
  background: linear-gradient(135deg, #1e3a5f 0%, #2d5a7b 50%, #1e3a5f 100%);
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
}

.audio-controls {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.play-button,
.volume-button {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.play-button:hover,
.volume-button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.play-button:active,
.volume-button:active {
  transform: scale(0.95);
}

.play-button:disabled,
.volume-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.icon {
  width: 24px;
  height: 24px;
  color: white;
}

.progress-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.time-display {
  display: flex;
  justify-content: space-between;
  color: white;
  font-size: 0.875rem;
  font-weight: 500;
}

.progress-slider {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.3);
  outline: none;
  -webkit-appearance: none;
  cursor: pointer;
}

.progress-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: white;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.progress-slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: white;
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.progress-slider:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.volume-button {
  width: 40px;
  height: 40px;
}

.volume-button .icon {
  width: 20px;
  height: 20px;
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .audio-player {
    padding: 0.75rem;
  }

  .audio-controls {
    gap: 0.75rem;
  }

  .play-button {
    width: 44px;
    height: 44px;
  }

  .volume-button {
    width: 36px;
    height: 36px;
  }

  .time-display {
    font-size: 0.8rem;
  }
}
</style>
