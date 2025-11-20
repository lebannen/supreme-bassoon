<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import FileUpload, {type FileUploadSelectEvent} from 'primevue/fileupload'
import {useReadingStore} from '@/stores/reading'
import type {ReadingText} from '@/types/reading'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const readingStore = useReadingStore()
const {texts, loading, error} = storeToRefs(readingStore)

const selectedLanguage = ref<string | null>(null)
const selectedLevel = ref<string | null>(null)
const uploadingTexts = ref<Record<number, boolean>>({})
const uploadResults = ref<Record<number, { success: boolean; message: string } | null>>({})

const languages = [
  {label: 'All Languages', value: null}, {label: 'French', value: 'fr'}, {label: 'German', value: 'de'},
  {label: 'Spanish', value: 'es'}, {label: 'Italian', value: 'it'},
]
const levels = [
  {label: 'All Levels', value: null}, {label: 'A1', value: 'A1'}, {label: 'A2', value: 'A2'},
  {label: 'B1', value: 'B1'}, {label: 'B2', value: 'B2'}, {label: 'C1', value: 'C1'}, {label: 'C2', value: 'C2'},
]

const filteredTexts = computed(() => {
  let result = texts.value
  if (selectedLanguage.value) result = result.filter(t => t.languageCode === selectedLanguage.value)
  if (selectedLevel.value) result = result.filter(t => t.level?.toUpperCase() === selectedLevel.value)
  return result
})

const getLanguageName = (code: string) => languages.find(l => l.value === code)?.label || code.toUpperCase()
const formatTopic = (topic: string) => topic.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
const getAudioFilename = (url: string) => url.substring(url.lastIndexOf('/') + 1)

const handleAudioUpload = async (event: FileUploadSelectEvent, text: ReadingText) => {
  const file = event.files[0]
  if (!file) return
  uploadingTexts.value[text.id] = true
  uploadResults.value[text.id] = null
  try {
    const formData = new FormData()
    formData.append('file', file)
    const uploadResponse = await fetch(`${API_BASE}/api/files/upload/audio`, {method: 'POST', body: formData})
    if (!uploadResponse.ok) throw new Error((await uploadResponse.json()).message || 'Failed to upload audio')
    const audioUrl = (await uploadResponse.json()).url
    const updateResponse = await fetch(`${API_BASE}/api/reading/texts/${text.id}/audio`, {
      method: 'PATCH',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({audioUrl}),
    })
    if (!updateResponse.ok) throw new Error('Failed to update text with audio URL')
    await readingStore.loadTexts({})
    uploadResults.value[text.id] = {success: true, message: 'Audio uploaded successfully!'}
  } catch (err) {
    uploadResults.value[text.id] = {
      success: false,
      message: err instanceof Error ? err.message : 'Failed to upload audio'
    }
  } finally {
    uploadingTexts.value[text.id] = false
  }
}

onMounted(() => readingStore.loadTexts({}))
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <h1>Reading Texts Administration</h1>
      <p class="text-secondary">Manage reading texts and their audio files.</p>
    </div>

    <div class="filters">
      <Select v-model="selectedLanguage" :options="languages" optionLabel="label" optionValue="value"
              placeholder="Filter by language" class="filter-select"/>
      <Select v-model="selectedLevel" :options="levels" optionLabel="label" optionValue="value"
              placeholder="Filter by level" class="filter-select"/>
      <Button label="Clear Filters" icon="pi pi-times" severity="secondary"
              @click="selectedLanguage = null; selectedLevel = null"/>
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner/>
    </div>
    <Message v-else-if="error" severity="error">{{ error }}</Message>

    <div v-else-if="filteredTexts.length > 0" class="content-grid">
      <Card v-for="text in filteredTexts" :key="text.id">
        <template #header>
          <div class="p-md flex justify-between items-start gap-md bg-surface-section">
            <div class="flex-1">
              <h3 class="text-lg font-bold mb-sm">{{ text.title }}</h3>
              <div class="meta-badges">
                <Tag :value="text.level || 'N/A'"/>
                <Tag :value="getLanguageName(text.languageCode)" severity="secondary"/>
                <Tag v-if="text.topic" :value="formatTopic(text.topic)" severity="contrast"/>
              </div>
            </div>
            <div class="stat-icon-sm" :class="text.audioUrl ? 'stat-icon-success' : 'stat-icon-warning'">
              <i :class="text.audioUrl ? 'pi pi-volume-up' : 'pi pi-volume-off'"></i>
            </div>
          </div>
        </template>
        <template #content>
          <div class="content-area">
            <p v-if="text.description" class="text-secondary m-0">{{ text.description }}</p>
            <div class="icon-label-group compact">
              <span class="icon-label"><i class="pi pi-book"></i>{{ text.wordCount || 0 }} words</span>
              <span v-if="text.estimatedMinutes" class="icon-label"><i class="pi pi-clock"></i>{{
                  text.estimatedMinutes
                }} min</span>
            </div>
            <div v-if="text.audioUrl" class="p-md bg-surface-ground rounded-md">
              <label class="font-semibold mb-sm block">Current Audio</label>
              <a :href="text.audioUrl" target="_blank" class="audio-link text-primary hover:underline">
                <i class="pi pi-external-link"></i> {{ getAudioFilename(text.audioUrl) }}
              </a>
            </div>
            <div class="content-area">
              <label class="font-semibold">{{ text.audioUrl ? 'Replace Audio' : 'Upload Audio' }}</label>
              <FileUpload mode="basic" :name="`audio-${text.id}`" accept="audio/*" :maxFileSize="50000000"
                          :customUpload="true" @select="handleAudioUpload($event, text)" :auto="true"
                          chooseLabel="Choose Audio File"/>
              <Message v-if="uploadResults[text.id]" :severity="uploadResults[text.id]?.success ? 'success' : 'error'"
                       @close="uploadResults[text.id] = null">
                {{ uploadResults[text.id]?.message }}
              </Message>
            </div>
          </div>
        </template>
      </Card>
    </div>
    <div v-else class="empty-state"><i class="pi pi-inbox empty-icon"></i>
      <h3>No texts found</h3></div>
  </div>
</template>
