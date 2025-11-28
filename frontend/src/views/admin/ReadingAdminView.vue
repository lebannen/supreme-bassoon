<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import {type FileUploadSelectEvent} from 'primevue/fileupload'
import {useConfirm} from 'primevue/useconfirm'
import {useToast} from 'primevue/usetoast'
import {useReadingStore} from '@/stores/reading'
import type {ReadingText} from '@/types/reading'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const router = useRouter()
const confirm = useConfirm()
const toast = useToast()

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

async function deleteText(text: ReadingText) {
  confirm.require({
    message: `Are you sure you want to delete "${text.title}"? This action cannot be undone.`,
    header: 'Confirm Deletion',
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Cancel',
    acceptLabel: 'Delete',
    rejectClass: 'p-button-secondary',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        const response = await fetch(`${API_BASE}/api/reading/texts/${text.id}`, {
          method: 'DELETE'
        })

        if (!response.ok) throw new Error('Failed to delete text')

        toast.add({
          severity: 'success',
          summary: 'Deleted',
          detail: `"${text.title}" has been deleted`,
          life: 3000
        })

        await readingStore.loadTexts({})
      } catch (err) {
        toast.add({
          severity: 'error',
          summary: 'Error',
          detail: err instanceof Error ? err.message : 'Failed to delete text',
          life: 5000
        })
      }
    }
  })
}

onMounted(() => readingStore.loadTexts({}))
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <div class="flex justify-content-between align-items-center">
        <div>
          <h1>Reading Texts Administration</h1>
          <p class="text-secondary">Manage reading texts and their audio files.</p>
        </div>
        <Button label="Import Texts" icon="pi pi-upload" @click="router.push('/reading/import')"/>
      </div>
    </div>

    <Card class="mb-4">
      <template #content>
        <div class="flex gap-3 items-end">
          <div class="flex-1">
            <label class="font-semibold text-sm mb-1 block">Language</label>
            <Select v-model="selectedLanguage" :options="languages" optionLabel="label" optionValue="value"
                    placeholder="All Languages" class="w-full"/>
          </div>
          <div class="flex-1">
            <label class="font-semibold text-sm mb-1 block">Level</label>
            <Select v-model="selectedLevel" :options="levels" optionLabel="label" optionValue="value"
                    placeholder="All Levels" class="w-full"/>
          </div>
          <Button label="Clear Filters" icon="pi pi-times" severity="secondary"
                  @click="selectedLanguage = null; selectedLevel = null"/>
        </div>
      </template>
    </Card>

    <div v-if="loading" class="flex justify-content-center p-5">
      <ProgressSpinner/>
    </div>
    <Message v-else-if="error" severity="error">{{ error }}</Message>

    <Card v-else>
      <template #content>
        <DataTable
            v-if="filteredTexts.length > 0"
            :value="filteredTexts"
            stripedRows
            sortField="createdAt"
            :sortOrder="-1"
            class="p-datatable-sm"
        >
          <Column field="id" header="ID" sortable style="width: 5rem"></Column>
          <Column field="title" header="Title" sortable>
            <template #body="slotProps">
              <strong>{{ slotProps.data.title }}</strong>
            </template>
          </Column>
          <Column field="languageCode" header="Language" sortable style="width: 8rem">
            <template #body="slotProps">
              <Tag :value="slotProps.data.languageCode.toUpperCase()" severity="info"/>
            </template>
          </Column>
          <Column field="level" header="Level" sortable style="width: 6rem">
            <template #body="slotProps">
              <Tag v-if="slotProps.data.level" :value="slotProps.data.level" severity="secondary"/>
              <span v-else class="text-secondary">-</span>
            </template>
          </Column>
          <Column field="wordCount" header="Words" sortable style="width: 8rem"></Column>
          <Column field="estimatedMinutes" header="Time" sortable style="width: 7rem">
            <template #body="slotProps">
              {{ slotProps.data.estimatedMinutes }} min
            </template>
          </Column>
          <Column field="audioUrl" header="Audio" style="width: 7rem">
            <template #body="slotProps">
              <Tag v-if="slotProps.data.audioUrl" value="Yes" severity="success"/>
              <Tag v-else value="No" severity="warning"/>
            </template>
          </Column>
          <Column field="createdAt" header="Created" sortable style="width: 10rem">
            <template #body="slotProps">
              {{ new Date(slotProps.data.createdAt).toLocaleDateString() }}
            </template>
          </Column>
          <Column header="Actions" style="width: 10rem">
            <template #body="slotProps">
              <div class="flex gap-2">
                <Button
                    icon="pi pi-trash"
                    severity="danger"
                    text
                    rounded
                    v-tooltip.top="'Delete'"
                    @click="deleteText(slotProps.data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
        <div v-else class="text-center p-5 text-secondary">
          <i class="pi pi-inbox text-4xl mb-3"></i>
          <p>No texts found</p>
        </div>
      </template>
    </Card>
  </div>
</template>
