<script setup lang="ts">
import {ref} from 'vue'
import Card from 'primevue/card'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Divider from 'primevue/divider'
import AudioPlayer from '@/components/audio/AudioPlayer.vue'

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

async function generateAudio() {
  if (!text.value.trim()) return
  isGenerating.value = true
  errorMessage.value = ''
  successMessage.value = ''
  audioUrl.value = ''
  try {
    const token = localStorage.getItem('auth_token')
    const response = await fetch(`${API_BASE}/api/admin/audio/generate`, {
      method: 'POST',
      headers: {'Content-Type': 'application/json', ...(token && {'Authorization': `Bearer ${token}`})},
      body: JSON.stringify({text: text.value, languageCode: languageCode.value, voice: voice.value}),
    })
    if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    const data = await response.json()
    if (data.success) {
      successMessage.value = data.message
      audioUrl.value = data.audioUrl
    } else {
      errorMessage.value = data.message
    }
  } catch (error: any) {
    errorMessage.value = error.message || 'Failed to generate audio'
  } finally {
    isGenerating.value = false
  }
}
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <h1>Audio Generation Test</h1>
    </div>

    <Card>
      <template #content>
        <div class="content-area-lg">
          <div class="flex flex-col gap-sm">
            <label for="text" class="font-semibold">Text to Convert</label>
            <Textarea id="text" v-model="text" rows="3" placeholder="Enter text..." :disabled="isGenerating"/>
          </div>

          <div class="grid md:grid-cols-2 gap-lg">
            <div class="flex flex-col gap-sm">
              <label for="language" class="font-semibold">Language</label>
              <Select id="language" v-model="languageCode" :options="languages" optionLabel="label" optionValue="value"
                      :disabled="isGenerating"/>
            </div>
            <div class="flex flex-col gap-sm">
              <label for="voice" class="font-semibold">Voice</label>
              <Select id="voice" v-model="voice" :options="voices" optionLabel="label" optionValue="value"
                      :disabled="isGenerating"/>
            </div>
          </div>

          <Button label="Generate Audio" icon="pi pi-play" @click="generateAudio" :loading="isGenerating"
                  :disabled="!text.trim()"/>

          <Message v-if="errorMessage" severity="error">{{ errorMessage }}</Message>
          <Message v-if="successMessage" severity="success">{{ successMessage }}</Message>

          <div v-if="audioUrl" class="content-area">
            <Divider/>
            <h3 class="font-semibold">Generated Audio</h3>
            <AudioPlayer :audio-url="audioUrl"/>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>
