<template>
  <div class="audio-test-view">
    <div class="container">
      <Card>
        <template #title>
          <h2>Audio Generation Test</h2>
        </template>
        <template #content>
          <div class="test-form">
            <div class="field">
              <label for="text">Text to Convert</label>
              <Textarea
                id="text"
                v-model="text"
                rows="3"
                placeholder="Enter text in French (e.g., 'Bonjour', 'Comment allez-vous ?')"
                :disabled="isGenerating"
              />
            </div>

            <div class="field">
              <label for="language">Language</label>
              <Select
                id="language"
                v-model="languageCode"
                :options="languages"
                optionLabel="label"
                optionValue="value"
                :disabled="isGenerating"
              />
            </div>

            <div class="field">
              <label for="voice">Voice</label>
              <Select
                id="voice"
                v-model="voice"
                :options="voices"
                optionLabel="label"
                optionValue="value"
                :disabled="isGenerating"
              />
            </div>

            <Button
              label="Generate Audio"
              icon="pi pi-play"
              @click="generateAudio"
              :loading="isGenerating"
              :disabled="!text.trim()"
            />

            <Message v-if="errorMessage" severity="error" :closable="false">
              {{ errorMessage }}
            </Message>

            <Message v-if="successMessage" severity="success" :closable="false">
              {{ successMessage }}
            </Message>

            <div v-if="audioUrl" class="audio-player-section">
              <Divider />
              <h3>Generated Audio</h3>
              <p class="audio-url">{{ audioUrl }}</p>
              <audio :src="audioUrl" controls preload="auto" class="audio-player">
                Your browser does not support the audio element.
              </audio>
            </div>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import Card from 'primevue/card'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Divider from 'primevue/divider'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const text = ref('')
const languageCode = ref('fr')
const voice = ref('Leda')
const isGenerating = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const audioUrl = ref('')

const languages = [
  { label: 'French', value: 'fr' },
  { label: 'English', value: 'en' },
  {label: 'German', value: 'de'},
]

const voices = [
  { label: 'Leda (French Female)', value: 'Leda' },
  { label: 'Puck (English Neutral)', value: 'Puck' },
  {label: 'Kore (German Female)', value: 'Kore'},
]

function getAuthHeaders(): HeadersInit {
  const token = localStorage.getItem('auth_token')
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

async function generateAudio() {
  if (!text.value.trim()) {
    return
  }

  isGenerating.value = true
  errorMessage.value = ''
  successMessage.value = ''
  audioUrl.value = ''

  try {
    const response = await fetch(`${API_BASE}/api/admin/audio/generate`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        text: text.value,
        languageCode: languageCode.value,
        voice: voice.value,
      }),
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const data = await response.json()

    if (data.success) {
      successMessage.value = data.message
      audioUrl.value = data.audioUrl
    } else {
      errorMessage.value = data.message
    }
  } catch (error: any) {
    console.error('Audio generation failed:', error)
    errorMessage.value = error.message || 'Failed to generate audio'
  } finally {
    isGenerating.value = false
  }
}
</script>

<style scoped>
.audio-test-view {
  padding: var(--spacing-2xl);
  background: var(--bg-primary);
  min-height: 100vh;
}

.container {
  max-width: 800px;
  margin: 0 auto;
}

.test-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.field label {
  font-weight: 600;
  color: var(--text-primary);
}

.audio-player-section {
  margin-top: var(--spacing-md);
}

.audio-player-section h3 {
  margin-bottom: var(--spacing-sm);
  color: var(--text-primary);
}

.audio-url {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-bottom: var(--spacing-md);
  word-break: break-all;
}

.audio-player {
  width: 100%;
  margin-top: var(--spacing-sm);
}
</style>
