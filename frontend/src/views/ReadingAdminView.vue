<template>
  <div class="reading-admin-container">
    <div class="header">
      <h1>Reading Texts Administration</h1>
      <p class="description">
        Manage reading texts and their audio files
      </p>
    </div>

    <div class="filters">
      <Select
        v-model="selectedLanguage"
        :options="languages"
        optionLabel="label"
        optionValue="value"
        placeholder="Filter by language"
        class="filter-select"
      />
      <Select
        v-model="selectedLevel"
        :options="levels"
        optionLabel="label"
        optionValue="value"
        placeholder="Filter by level"
        class="filter-select"
      />
      <Button
        label="Clear Filters"
        icon="pi pi-times"
        outlined
        @click="clearFilters"
      />
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner />
      <p>Loading texts...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <Message severity="error" :closable="false">{{ error }}</Message>
    </div>

    <div v-else class="texts-grid">
      <Card v-for="text in filteredTexts" :key="text.id" class="text-card">
        <template #header>
          <div class="card-header">
            <div class="text-info">
              <h3>{{ text.title }}</h3>
              <div class="meta">
                <Tag :value="text.level || 'N/A'" severity="info" />
                <span class="language">{{ getLanguageName(text.languageCode) }}</span>
                <span v-if="text.topic" class="topic">{{ formatTopic(text.topic) }}</span>
              </div>
            </div>
            <div class="audio-status">
              <i
                v-if="text.audioUrl"
                class="pi pi-volume-up audio-icon has-audio"
                v-tooltip="'Has audio'"
              />
              <i
                v-else
                class="pi pi-volume-off audio-icon no-audio"
                v-tooltip="'No audio'"
              />
            </div>
          </div>
        </template>

        <template #content>
          <div class="card-content">
            <p v-if="text.description" class="description">{{ text.description }}</p>

            <div class="stats">
              <span><i class="pi pi-book"></i> {{ text.wordCount || 0 }} words</span>
              <span v-if="text.estimatedMinutes">
                <i class="pi pi-clock"></i> {{ text.estimatedMinutes }} min
              </span>
            </div>

            <div v-if="text.audioUrl" class="current-audio">
              <label>Current Audio:</label>
              <a :href="text.audioUrl" target="_blank" class="audio-link">
                <i class="pi pi-external-link"></i> {{ getAudioFilename(text.audioUrl) }}
              </a>
            </div>

            <div class="audio-upload">
              <label class="upload-label">
                {{ text.audioUrl ? 'Replace Audio:' : 'Upload Audio:' }}
              </label>
              <FileUpload
                :name="`audio-${text.id}`"
                accept="audio/*"
                :maxFileSize="50000000"
                :customUpload="true"
                @select="(event) => onAudioSelect(event, text.id)"
                @uploader="(event) => handleAudioUpload(event, text)"
                :auto="false"
                chooseLabel="Choose Audio"
                uploadLabel="Upload"
                cancelLabel="Cancel"
                :showUploadButton="selectedFiles[text.id] !== undefined"
                :showCancelButton="selectedFiles[text.id] !== undefined"
              >
                <template #empty>
                  <p>Drag and drop audio file here or click to choose.</p>
                  <small>Supported formats: WAV, MP3, M4A, OGG (max 50MB)</small>
                </template>
              </FileUpload>

              <div v-if="uploadingTexts[text.id]" class="upload-progress">
                <ProgressBar mode="indeterminate" />
                <small>Uploading audio...</small>
              </div>

              <div v-if="uploadResults[text.id]" class="upload-result">
                <Message
                  :severity="uploadResults[text.id].success ? 'success' : 'error'"
                  :closable="true"
                  @close="uploadResults[text.id] = null"
                >
                  {{ uploadResults[text.id].message }}
                </Message>
              </div>
            </div>
          </div>
        </template>
      </Card>
    </div>

    <div v-if="!loading && filteredTexts.length === 0" class="empty-state">
      <i class="pi pi-inbox"></i>
      <p>No texts found matching the filters.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import ProgressBar from 'primevue/progressbar'
import FileUpload, { type FileUploadSelectEvent, type FileUploadUploaderEvent } from 'primevue/fileupload'
import { useReadingTexts, type ReadingText } from '@/composables/useReadingTexts'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const { texts, loading, error, fetchTexts } = useReadingTexts()

const selectedLanguage = ref<string | null>(null)
const selectedLevel = ref<string | null>(null)
const selectedFiles = ref<Record<number, File>>({})
const uploadingTexts = ref<Record<number, boolean>>({})
const uploadResults = ref<Record<number, { success: boolean; message: string } | null>>({})

const languages = [
  { label: 'All Languages', value: null },
  { label: 'French', value: 'fr' },
  { label: 'German', value: 'de' },
  { label: 'Spanish', value: 'es' },
  { label: 'Italian', value: 'it' }
]

const levels = [
  { label: 'All Levels', value: null },
  { label: 'A1', value: 'A1' },
  { label: 'A2', value: 'A2' },
  { label: 'B1', value: 'B1' },
  { label: 'B2', value: 'B2' },
  { label: 'C1', value: 'C1' },
  { label: 'C2', value: 'C2' }
]

const filteredTexts = computed(() => {
  let result = texts.value

  if (selectedLanguage.value) {
    result = result.filter(t => t.languageCode === selectedLanguage.value)
  }

  if (selectedLevel.value) {
    result = result.filter(t => t.level?.toUpperCase() === selectedLevel.value)
  }

  return result
})

function getLanguageName(code: string): string {
  const languageNames: Record<string, string> = {
    fr: 'French',
    de: 'German',
    es: 'Spanish',
    it: 'Italian',
    pt: 'Portuguese',
    ru: 'Russian',
    ja: 'Japanese',
    zh: 'Chinese',
    ko: 'Korean',
    ar: 'Arabic',
    hi: 'Hindi',
    pl: 'Polish'
  }
  return languageNames[code] || code.toUpperCase()
}

function formatTopic(topic: string): string {
  return topic
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

function getAudioFilename(url: string): string {
  try {
    const urlObj = new URL(url)
    const path = urlObj.pathname
    return path.substring(path.lastIndexOf('/') + 1)
  } catch {
    return url
  }
}

function clearFilters() {
  selectedLanguage.value = null
  selectedLevel.value = null
}

function onAudioSelect(event: FileUploadSelectEvent, textId: number) {
  if (event.files && event.files.length > 0) {
    selectedFiles.value[textId] = event.files[0]
  }
}

async function handleAudioUpload(event: FileUploadUploaderEvent, text: ReadingText) {
  const file = selectedFiles.value[text.id]
  if (!file) {
    uploadResults.value[text.id] = {
      success: false,
      message: 'No file selected'
    }
    return
  }

  uploadingTexts.value[text.id] = true
  uploadResults.value[text.id] = null

  try {
    // Step 1: Upload audio file to storage
    const formData = new FormData()
    formData.append('file', file)

    const uploadResponse = await fetch(`${API_BASE}/api/files/upload/audio`, {
      method: 'POST',
      body: formData
    })

    if (!uploadResponse.ok) {
      const errorData = await uploadResponse.json()
      throw new Error(errorData.message || 'Failed to upload audio file')
    }

    const uploadData = await uploadResponse.json()
    const audioUrl = uploadData.url

    // Step 2: Update text with audio URL
    const updateResponse = await fetch(`${API_BASE}/api/reading/texts/${text.id}/audio`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ audioUrl })
    })

    if (!updateResponse.ok) {
      throw new Error('Failed to update text with audio URL')
    }

    // Update local data
    const textIndex = texts.value.findIndex(t => t.id === text.id)
    if (textIndex !== -1) {
      texts.value[textIndex].audioUrl = audioUrl
    }

    uploadResults.value[text.id] = {
      success: true,
      message: 'Audio uploaded successfully!'
    }

    // Clear selected file
    delete selectedFiles.value[text.id]

    // Clear result after 5 seconds
    setTimeout(() => {
      uploadResults.value[text.id] = null
    }, 5000)
  } catch (err) {
    uploadResults.value[text.id] = {
      success: false,
      message: err instanceof Error ? err.message : 'Failed to upload audio'
    }
  } finally {
    uploadingTexts.value[text.id] = false
  }
}

onMounted(async () => {
  await fetchTexts()
})
</script>

<style scoped>
.reading-admin-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

.header {
  margin-bottom: 2rem;
}

.header h1 {
  margin: 0 0 0.5rem 0;
  color: var(--text-color);
}

.description {
  margin: 0;
  color: var(--text-color-secondary);
}

.filters {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  flex-wrap: wrap;
}

.filter-select {
  min-width: 200px;
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  gap: 1rem;
}

.texts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(500px, 1fr));
  gap: 2rem;
}

.text-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  background: var(--surface-ground);
}

.text-info {
  flex: 1;
  min-width: 0;
}

.text-info h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-color);
}

.meta {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
}

.language {
  padding: 0.25rem 0.75rem;
  background: var(--primary-color);
  color: white;
  border-radius: 1rem;
  font-size: 0.75rem;
  font-weight: 500;
}

.topic {
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.audio-status {
  display: flex;
  align-items: center;
}

.audio-icon {
  font-size: 1.5rem;
}

.audio-icon.has-audio {
  color: var(--green-500);
}

.audio-icon.no-audio {
  color: var(--surface-400);
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.description {
  margin: 0;
  color: var(--text-color);
  line-height: 1.6;
}

.stats {
  display: flex;
  gap: 1.5rem;
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.stats span {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.current-audio {
  padding: 0.75rem;
  background: var(--surface-ground);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.current-audio label {
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.audio-link {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--primary-color);
  text-decoration: none;
  font-size: 0.875rem;
}

.audio-link:hover {
  text-decoration: underline;
}

.audio-upload {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.upload-label {
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.upload-progress {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.upload-progress small {
  color: var(--text-color-secondary);
}

.upload-result {
  margin-top: 0.5rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  gap: 1rem;
  color: var(--text-color-secondary);
}

.empty-state i {
  font-size: 4rem;
  opacity: 0.5;
}

@media (max-width: 768px) {
  .reading-admin-container {
    padding: 1rem;
  }

  .texts-grid {
    grid-template-columns: 1fr;
  }

  .filters {
    flex-direction: column;
  }

  .filter-select {
    width: 100%;
  }
}
</style>
