<script setup lang="ts">
import {onBeforeUnmount, onMounted, ref} from 'vue'
import Button from 'primevue/button'
import Slider from 'primevue/slider'

interface Props {
  audioUrl: string | null
}

defineProps<Props>()

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

function onSliderChange(value: number) {
  if (!audioElement.value) return
  audioElement.value.currentTime = value
  currentTime.value = value
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
  // Do not reset to 0, so the user can see the full duration
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

<template>
  <div v-if="audioUrl" class="p-4 bg-surface rounded-lg shadow-md">
    <div class="flex align-items-center gap-3">
      <Button
          :icon="isPlaying ? 'pi pi-pause' : 'pi pi-play'"
          @click="togglePlay"
          :disabled="loading"
          rounded
          text
          severity="contrast"
          aria-label="Play/Pause"
      />

      <div class="flex-1 flex flex-column gap-1">
        <Slider
            :modelValue="currentTime"
            @update:modelValue="onSliderChange"
            :max="duration"
            :disabled="loading"
            class="w-full"
        />
        <div class="flex justify-content-between text-xs text-secondary">
          <span>{{ formatTime(currentTime) }}</span>
          <span>{{ formatTime(duration) }}</span>
        </div>
      </div>

      <Button
          :icon="isMuted ? 'pi pi-volume-off' : 'pi pi-volume-up'"
          @click="toggleMute"
          :disabled="loading"
          rounded
          text
          severity="contrast"
          aria-label="Mute/Unmute"
      />
    </div>

    <audio
        ref="audioElement"
        :src="audioUrl"
        @loadedmetadata="onLoadedMetadata"
        @timeupdate="onTimeUpdate"
        @ended="onEnded"
        @error="onError"
        class="hidden"
    />
  </div>
</template>
