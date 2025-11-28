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
  // Higher pitch voices
  {label: 'Zephyr (Bright, Higher)', value: 'Zephyr'},
  {label: 'Leda (Youthful, Higher)', value: 'Leda'},
  {label: 'Laomedeia (Upbeat, Higher)', value: 'Laomedeia'},
  {label: 'Achernar (Soft, Higher)', value: 'Achernar'},

  // Middle pitch voices
  {label: 'Puck (Upbeat, Middle)', value: 'Puck'},
  {label: 'Kore (Firm, Middle)', value: 'Kore'},
  {label: 'Aoede (Breezy, Middle)', value: 'Aoede'},
  {label: 'Callirrhoe (Easy-going, Middle)', value: 'Callirrhoe'},
  {label: 'Autonoe (Bright, Middle)', value: 'Autonoe'},
  {label: 'Despina (Smooth, Middle)', value: 'Despina'},
  {label: 'Erinome (Clear, Middle)', value: 'Erinome'},
  {label: 'Rasalgethi (Informative, Middle)', value: 'Rasalgethi'},
  {label: 'Gacrux (Mature, Middle)', value: 'Gacrux'},
  {label: 'Pulcherrima (Forward, Middle)', value: 'Pulcherrima'},
  {label: 'Vindemiatrix (Gentle, Middle)', value: 'Vindemiatrix'},
  {label: 'Sadaltager (Knowledgeable, Middle)', value: 'Sadaltager'},
  {label: 'Sulafat (Warm, Middle)', value: 'Sulafat'},

  // Lower middle pitch voices
  {label: 'Fenrir (Excitable, Lower middle)', value: 'Fenrir'},
  {label: 'Orus (Firm, Lower middle)', value: 'Orus'},
  {label: 'Iapetus (Clear, Lower middle)', value: 'Iapetus'},
  {label: 'Umbriel (Easy-going, Lower middle)', value: 'Umbriel'},
  {label: 'Alnilam (Firm, Lower middle)', value: 'Alnilam'},
  {label: 'Schedar (Even, Lower middle)', value: 'Schedar'},
  {label: 'Achird (Friendly, Lower middle)', value: 'Achird'},
  {label: 'Zubenelgenubi (Casual, Lower middle)', value: 'Zubenelgenubi'},

  // Lower pitch voices
  {label: 'Charon (Informative, Lower)', value: 'Charon'},
  {label: 'Enceladus (Breathy, Lower)', value: 'Enceladus'},
  {label: 'Algieba (Smooth, Lower)', value: 'Algieba'},
  {label: 'Algenib (Gravelly, Lower)', value: 'Algenib'},
  {label: 'Sadachbia (Lively, Lower)', value: 'Sadachbia'},
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
          <div class="flex flex-column gap-2">
            <label for="text" class="font-semibold">Text to Convert</label>
            <Textarea id="text" v-model="text" rows="3" placeholder="Enter text..." :disabled="isGenerating"/>
          </div>

          <div class="grid md:grid-cols-2 gap-4">
            <div class="flex flex-column gap-2">
              <label for="language" class="font-semibold">Language</label>
              <Select id="language" v-model="languageCode" :options="languages" optionLabel="label" optionValue="value"
                      :disabled="isGenerating"/>
            </div>
            <div class="flex flex-column gap-2">
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
