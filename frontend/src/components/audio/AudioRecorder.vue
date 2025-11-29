<script setup lang="ts">
import {onBeforeUnmount, ref} from 'vue'
import Button from 'primevue/button'

const emit = defineEmits<{
  recorded: [blob: Blob]
  error: [message: string]
}>()

const isRecording = ref(false)
const hasRecording = ref(false)
const recordingDuration = ref(0)
const audioBlob = ref<Blob | null>(null)
const audioUrl = ref<string | null>(null)

let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let durationInterval: number | null = null

function getSupportedMimeType(): string {
  // Priority order: webm (Chrome/Firefox), ogg (Firefox fallback), mp4 (Safari)
  const types = [
    'audio/webm;codecs=opus',
    'audio/webm',
    'audio/ogg;codecs=opus',
    'audio/ogg',
    'audio/mp4',
    'audio/mpeg',
    '', // Empty string = browser default
  ]

  for (const type of types) {
    if (type === '' || MediaRecorder.isTypeSupported(type)) {
      return type
    }
  }

  return ''
}

async function startRecording() {
  // Check if mediaDevices is available (requires HTTPS or localhost)
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    emit('error', 'Media devices not available. Make sure you are using HTTPS or localhost.')
    return
  }

  try {
    const stream = await navigator.mediaDevices.getUserMedia({audio: true})

    // Detect best supported MIME type
    const mimeType = getSupportedMimeType()
    console.log('Using MIME type:', mimeType)

    // Create MediaRecorder - Firefox may fail with specific mimeTypes, so fallback to default
    try {
      mediaRecorder = mimeType
          ? new MediaRecorder(stream, {mimeType})
          : new MediaRecorder(stream)
    } catch {
      console.warn('MediaRecorder failed with mimeType, using default')
      mediaRecorder = new MediaRecorder(stream)
    }
    audioChunks = []

    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.push(event.data)
      }
    }

    mediaRecorder.onstop = () => {
      // Use recorded MIME type or fallback to webm
      const blobType = mimeType || mediaRecorder?.mimeType || 'audio/webm'
      const blob = new Blob(audioChunks, {type: blobType})
      audioBlob.value = blob
      audioUrl.value = URL.createObjectURL(blob)
      hasRecording.value = true
      console.log('Recording complete, blob size:', blob.size, 'type:', blob.type)

      // Stop all tracks
      stream.getTracks().forEach((track) => track.stop())
    }

    mediaRecorder.start()
    isRecording.value = true
    recordingDuration.value = 0

    durationInterval = window.setInterval(() => {
      recordingDuration.value++
    }, 1000)
  } catch (err) {
    console.error('Microphone access error:', err)

    // Provide more specific error messages
    if (err instanceof DOMException) {
      switch (err.name) {
        case 'NotAllowedError':
          emit('error', 'Microphone permission denied. Please allow access in your browser settings.')
          break
        case 'NotFoundError':
          emit('error', 'No microphone found. Please connect a microphone and try again.')
          break
        case 'NotReadableError':
          emit('error', 'Microphone is in use by another application.')
          break
        case 'SecurityError':
          emit('error', 'Microphone access blocked. This page must be served over HTTPS.')
          break
        default:
          emit('error', `Microphone error: ${err.message}`)
      }
    } else {
      emit('error', `Failed to access microphone: ${err}`)
    }
  }
}

function stopRecording() {
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop()
    isRecording.value = false

    if (durationInterval) {
      clearInterval(durationInterval)
      durationInterval = null
    }
  }
}

function clearRecording() {
  if (audioUrl.value) {
    URL.revokeObjectURL(audioUrl.value)
  }
  audioBlob.value = null
  audioUrl.value = null
  hasRecording.value = false
  recordingDuration.value = 0
}

function submitRecording() {
  if (audioBlob.value) {
    emit('recorded', audioBlob.value)
  }
}

function formatDuration(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

onBeforeUnmount(() => {
  if (isRecording.value) stopRecording()
  if (audioUrl.value) URL.revokeObjectURL(audioUrl.value)
})

defineExpose({clearRecording})
</script>

<template>
  <div class="audio-recorder">
    <!-- Recording state -->
    <div v-if="isRecording" class="recording-active">
      <div class="recording-indicator">
        <span class="pulse"></span>
        <span class="duration">{{ formatDuration(recordingDuration) }}</span>
      </div>
      <Button label="Stop" icon="pi pi-stop" severity="danger" @click="stopRecording"/>
    </div>

    <!-- Has recording state -->
    <div v-else-if="hasRecording" class="recording-preview">
      <audio :src="audioUrl!" controls class="audio-preview"/>
      <div class="recording-actions">
        <Button label="Re-record" icon="pi pi-refresh" text @click="clearRecording"/>
        <Button label="Submit" icon="pi pi-check" @click="submitRecording"/>
      </div>
    </div>

    <!-- Ready to record state -->
    <div v-else class="recording-prompt">
      <Button label="Tap to record" icon="pi pi-microphone" size="large" @click="startRecording"/>
    </div>
  </div>
</template>

<style scoped>
.audio-recorder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background: var(--surface-card);
  border-radius: var(--border-radius);
  border: 2px dashed var(--surface-border);
}

.recording-active {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.recording-indicator {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.pulse {
  width: 12px;
  height: 12px;
  background-color: var(--red-500);
  border-radius: 50%;
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
}

.duration {
  font-size: 1.5rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.recording-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  width: 100%;
}

.audio-preview {
  width: 100%;
  max-width: 400px;
}

.recording-actions {
  display: flex;
  gap: 0.5rem;
}

.recording-prompt {
  padding: 1rem;
}
</style>
