<script setup lang="ts">
import {computed, ref} from 'vue'
import Card from 'primevue/card'
import Textarea from 'primevue/textarea'
import Checkbox from 'primevue/checkbox'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Divider from 'primevue/divider'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

interface ImportResult {
  imported: number;
  skipped: number;
  audioGenerated: number;
  audioFailed: number;
  errors: string[];
  exercises: {
    id: number | null;
    title: string;
    type: string;
    audioUrl: string | null;
    status: 'IMPORTED' | 'SKIPPED' | 'FAILED'
  }[]
}

const fileInput = ref<HTMLInputElement | null>(null)
const selectedFiles = ref<File[]>([])
const isDragging = ref(false)
const jsonContent = ref('')
const generateAudio = ref(true)
const overwriteExisting = ref(false)
const isImporting = ref(false)
const errorMessage = ref('')
const importResult = ref<ImportResult | null>(null)

const canImport = computed(() => selectedFiles.value.length > 0 || jsonContent.value.trim().length > 0)

const addFiles = (files: File[]) => {
  selectedFiles.value.push(...files.filter(f => f.name.endsWith('.json')))
  if (selectedFiles.value.length > 0) jsonContent.value = ''
}

const handleFileSelect = (event: Event) => addFiles(Array.from((event.target as HTMLInputElement).files || []))
const handleDrop = (event: DragEvent) => {
  isDragging.value = false
  addFiles(Array.from(event.dataTransfer?.files || []))
}

const importSingleModule = async (moduleData: any) => {
  const token = localStorage.getItem('auth_token')
  const response = await fetch(`${API_BASE}/api/admin/exercises/import?generateAudio=${generateAudio.value}&overwriteExisting=${overwriteExisting.value}`, {
    method: 'POST',
    headers: {'Content-Type': 'application/json', ...(token && {'Authorization': `Bearer ${token}`})},
    body: JSON.stringify(moduleData),
  })
  if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  return response.json()
}

const importExercises = async () => {
  isImporting.value = true
  errorMessage.value = ''
  importResult.value = null
  try {
    let results: ImportResult[] = []
    if (selectedFiles.value.length > 0) {
      for (const file of selectedFiles.value) {
        try {
          results.push(await importSingleModule(JSON.parse(await file.text())))
        } catch (e: any) {
          errorMessage.value += `\nFailed to import ${file.name}: ${e.message}`
        }
      }
    } else if (jsonContent.value.trim()) {
      results.push(await importSingleModule(JSON.parse(jsonContent.value)))
    }
    if (results.length > 0) {
      importResult.value = results.reduce((acc, r) => ({
        imported: acc.imported + r.imported, skipped: acc.skipped + r.skipped,
        audioGenerated: acc.audioGenerated + r.audioGenerated, audioFailed: acc.audioFailed + r.audioFailed,
        errors: [...acc.errors, ...r.errors], exercises: [...acc.exercises, ...r.exercises],
      }), {imported: 0, skipped: 0, audioGenerated: 0, audioFailed: 0, errors: [], exercises: []})
      if (importResult.value.errors.length === 0 && importResult.value.imported > 0) {
        jsonContent.value = '';
        selectedFiles.value = [];
        if (fileInput.value) fileInput.value.value = ''
      }
    }
  } catch (error: any) {
    errorMessage.value = error.message || 'Failed to import exercises'
  } finally {
    isImporting.value = false
  }
}

const getStatusSeverity = (status: string) => ({
  IMPORTED: 'success',
  SKIPPED: 'info',
  FAILED: 'danger'
}[status] || 'secondary')
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header"><h1>Exercise Import</h1></div>
    <Card>
      <template #content>
        <div class="content-area-lg">
          <div class="content-area">
            <label class="font-semibold">Upload Files</label>
            <input ref="fileInput" type="file" multiple accept=".json" @change="handleFileSelect" class="hidden"/>
            <div class="drop-zone" :class="{ 'drag-over': isDragging }" @drop.prevent="handleDrop"
                 @dragover.prevent="isDragging = true" @dragleave.prevent="isDragging = false"
                 @click="fileInput?.click()">
              <i class="pi pi-cloud-upload text-4xl text-primary"></i>
              <p class="font-semibold">Click to browse or drag and drop JSON files</p>
            </div>
            <div v-if="selectedFiles.length > 0" class="task-list">
              <div v-for="(file, index) in selectedFiles" :key="index" class="file-item bg-surface">
                <div class="file-info"><i class="pi pi-file text-xl"></i><span class="file-name">{{ file.name }}</span>
                </div>
                <Button icon="pi pi-times" text rounded severity="danger" @click="selectedFiles.splice(index, 1)"
                        :disabled="isImporting"/>
              </div>
            </div>
          </div>
          <Divider>OR</Divider>
          <div class="content-area">
            <label for="jsonContent" class="font-semibold">Paste JSON Content</label>
            <Textarea id="jsonContent" v-model="jsonContent" rows="10"
                      placeholder="Paste module_X_exercises.json content here..."
                      :disabled="isImporting || selectedFiles.length > 0"/>
          </div>
          <div class="flex flex-col gap-md">
            <div class="flex items-center gap-sm">
              <Checkbox id="generateAudio" v-model="generateAudio" :binary="true" :disabled="isImporting"/>
              <label for="generateAudio">Generate Audio</label></div>
            <div class="flex items-center gap-sm">
              <Checkbox id="overwriteExisting" v-model="overwriteExisting" :binary="true" :disabled="isImporting"/>
              <label for="overwriteExisting">Overwrite Existing</label></div>
          </div>
          <Button label="Import Exercises" icon="pi pi-upload" @click="importExercises" :loading="isImporting"
                  :disabled="!canImport" size="large"/>
          <Message v-if="errorMessage" severity="error">{{ errorMessage }}</Message>
          <div v-if="importResult" class="content-area-lg">
            <Divider/>
            <h3 class="font-bold">Import Results</h3>
            <div class="summary-stats">
              <div class="stat success"><i class="pi pi-check-circle"></i>
                <div><span class="stat-value">{{ importResult.imported }}</span><span class="stat-label">Imported</span>
                </div>
              </div>
              <div class="stat"><i class="pi pi-forward"></i>
                <div><span class="stat-value">{{ importResult.skipped }}</span><span class="stat-label">Skipped</span>
                </div>
              </div>
              <div class="stat success"><i class="pi pi-volume-up"></i>
                <div><span class="stat-value">{{ importResult.audioGenerated }}</span><span
                    class="stat-label">Audio OK</span></div>
              </div>
              <div class="stat failed"><i class="pi pi-volume-off"></i>
                <div><span class="stat-value">{{ importResult.audioFailed }}</span><span
                    class="stat-label">Audio Fail</span></div>
              </div>
            </div>
            <Message v-for="(error, i) in importResult.errors" :key="i" severity="warn">{{ error }}</Message>
            <DataTable v-if="importResult.exercises.length > 0" :value="importResult.exercises" stripedRows>
              <Column header="Status">
                <template #body="{data}">
                  <Tag :value="data.status" :severity="getStatusSeverity(data.status)"/>
                </template>
              </Column>
              <Column field="title" header="Title"/>
              <Column field="type" header="Type"/>
            </DataTable>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.hidden {
  display: none;
}
.drop-zone {
  border: 2px dashed var(--surface-border);
  border-radius: var(--radius-lg);
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.drop-zone:hover, .drop-zone.drag-over {
  border-color: var(--primary-color);
  background-color: var(--surface-hover);
}
</style>
