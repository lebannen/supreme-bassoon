<template>
  <div class="page-container-with-padding">
    <div class="view-container content-area-lg">
      <Card>
        <template #title>
          <h2>Exercise Import</h2>
        </template>
        <template #content>
          <div class="flex flex-col gap-lg">
            <!-- File Upload Section -->
            <div class="flex flex-col gap-sm">
              <label class="font-semibold text-primary">Drag & Drop JSON Files</label>
              <div class="file-upload-container">
                <input
                  ref="fileInput"
                  type="file"
                  multiple
                  accept=".json"
                  @change="handleFileSelect"
                  class="hidden-file-input"
                />
                <div
                  class="drop-zone"
                  :class="{ 'drag-over': isDragging, 'has-files': selectedFiles.length > 0 }"
                  @drop.prevent="handleDrop"
                  @dragover.prevent="isDragging = true"
                  @dragleave.prevent="isDragging = false"
                  @click="triggerFileSelect"
                >
                  <i class="pi pi-cloud-upload upload-icon"></i>
                  <p class="drop-zone-text">
                    <strong>Click to browse</strong> or drag and drop JSON files here
                  </p>
                  <p class="drop-zone-hint">You can select multiple module files at once</p>
                </div>
              </div>

              <!-- Selected Files List -->
              <div v-if="selectedFiles.length > 0" class="mt-lg">
                <h4 class="mb-md text-primary m-0">Selected Files ({{ selectedFiles.length }})</h4>
                <div class="file-list">
                  <div v-for="(file, index) in selectedFiles" :key="index" class="file-item">
                    <i class="pi pi-file"></i>
                    <span class="file-name">{{ file.name }}</span>
                    <span class="file-size">{{ formatFileSize(file.size) }}</span>
                    <Button
                      icon="pi pi-times"
                      text
                      rounded
                      severity="danger"
                      size="small"
                      @click="removeFile(index)"
                      :disabled="isImporting"
                    />
                  </div>
                </div>
              </div>
            </div>

            <Divider>OR</Divider>

            <!-- Paste JSON Section -->
            <div class="flex flex-col gap-sm">
              <label for="jsonContent" class="font-semibold text-primary">Paste JSON Content</label>
              <Textarea
                id="jsonContent"
                v-model="jsonContent"
                rows="10"
                placeholder="Paste JSON from module_X_exercises.json file here..."
                :disabled="isImporting || selectedFiles.length > 0"
              />
            </div>

            <div class="flex flex-col gap-md">
              <div class="flex items-center gap-sm">
                <Checkbox
                  id="generateAudio"
                  v-model="generateAudio"
                  :binary="true"
                  :disabled="isImporting"
                />
                <label for="generateAudio" class="cursor-pointer">Generate Audio for Listening Exercises</label>
              </div>

              <div class="flex items-center gap-sm">
                <Checkbox
                  id="overwriteExisting"
                  v-model="overwriteExisting"
                  :binary="true"
                  :disabled="isImporting"
                />
                <label for="overwriteExisting" class="cursor-pointer">Overwrite Existing Exercises</label>
              </div>
            </div>

            <Button
              label="Import Exercises"
              icon="pi pi-upload"
              @click="importExercises"
              :loading="isImporting"
              :disabled="!canImport"
              size="large"
            />

            <Message v-if="errorMessage" severity="error" :closable="false">
              {{ errorMessage }}
            </Message>

            <div v-if="importResult" class="mt-md">
              <Divider />
              <h3 class="mb-md text-primary">Import Results</h3>

              <div class="stats-grid">
                <div class="stat-card">
                  <div class="stat-label">Imported</div>
                  <div class="stat-value success">{{ importResult.imported }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">Skipped</div>
                  <div class="stat-value info">{{ importResult.skipped }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">Audio Generated</div>
                  <div class="stat-value success">{{ importResult.audioGenerated }}</div>
                </div>
                <div class="stat-card">
                  <div class="stat-label">Audio Failed</div>
                  <div class="stat-value error">{{ importResult.audioFailed }}</div>
                </div>
              </div>

              <div v-if="importResult.errors.length > 0" class="mt-md">
                <h4 class="mt-lg mb-sm text-primary">Errors</h4>
                <Message
                  v-for="(error, index) in importResult.errors"
                  :key="index"
                  severity="warn"
                  :closable="false"
                  class="mb-sm"
                >
                  {{ error }}
                </Message>
              </div>

              <div v-if="importResult.exercises.length > 0" class="mt-md">
                <h4 class="mt-lg mb-sm text-primary">Exercises</h4>
                <DataTable :value="importResult.exercises" striped-rows>
                  <Column field="status" header="Status">
                    <template #body="{ data }">
                      <Tag :value="data.status" :severity="getStatusSeverity(data.status)"/>
                    </template>
                  </Column>
                  <Column field="title" header="Title" />
                  <Column field="type" header="Type" />
                  <Column field="audioUrl" header="Audio URL">
                    <template #body="{ data }">
                      <span v-if="data.audioUrl" class="audio-url-cell">
                        {{ truncateUrl(data.audioUrl) }}
                      </span>
                      <span v-else class="text-muted">-</span>
                    </template>
                  </Column>
                </DataTable>
              </div>
            </div>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

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

const fileInput = ref<HTMLInputElement | null>(null)
const selectedFiles = ref<File[]>([])
const isDragging = ref(false)
const jsonContent = ref('')
const generateAudio = ref(true)
const overwriteExisting = ref(false)
const isImporting = ref(false)
const errorMessage = ref('')
const importResult = ref<ImportResult | null>(null)

interface ImportResult {
  imported: number
  skipped: number
  audioGenerated: number
  audioFailed: number
  errors: string[]
  exercises: ImportedExercise[]
}

interface ImportedExercise {
  id: number | null
  title: string
  type: string
  audioUrl: string | null
  status: 'IMPORTED' | 'SKIPPED' | 'FAILED'
}

const canImport = computed(() => {
  return selectedFiles.value.length > 0 || jsonContent.value.trim().length > 0
})

function triggerFileSelect() {
  if (!isImporting.value) {
    fileInput.value?.click()
  }
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files) {
    addFiles(Array.from(target.files))
  }
}

function handleDrop(event: DragEvent) {
  isDragging.value = false
  if (event.dataTransfer?.files) {
    addFiles(Array.from(event.dataTransfer.files))
  }
}

function addFiles(files: File[]) {
  const jsonFiles = files.filter((file) => file.name.endsWith('.json'))
  selectedFiles.value.push(...jsonFiles)

  // Clear paste area if files are selected
  if (selectedFiles.value.length > 0) {
    jsonContent.value = ''
  }
}

function removeFile(index: number) {
  selectedFiles.value.splice(index, 1)
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  const kb = bytes / 1024
  if (kb < 1024) return kb.toFixed(1) + ' KB'
  const mb = kb / 1024
  return mb.toFixed(1) + ' MB'
}

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

async function importSingleModule(moduleData: any): Promise<ImportResult> {
  const response = await fetch(
    `${API_BASE}/api/admin/exercises/import?generateAudio=${generateAudio.value}&overwriteExisting=${overwriteExisting.value}`,
    {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(moduleData),
    }
  )

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }

  return await response.json()
}

async function importExercises() {
  isImporting.value = true
  errorMessage.value = ''
  importResult.value = null

  try {
    let results: ImportResult[]

    // Import from files if selected
    if (selectedFiles.value.length > 0) {
      results = []

      for (const file of selectedFiles.value) {
        try {
          const text = await file.text()
          const moduleData = JSON.parse(text)
          const result = await importSingleModule(moduleData)
          results.push(result)
        } catch (e: any) {
          console.error(`Failed to import ${file.name}:`, e)
          errorMessage.value = `${errorMessage.value}\nFailed to import ${file.name}: ${e.message}`
        }
      }
    }
    // Import from pasted JSON
    else if (jsonContent.value.trim()) {
      let moduleData
      try {
        moduleData = JSON.parse(jsonContent.value)
      } catch {
        errorMessage.value = 'Invalid JSON format'
        return
      }

      const result = await importSingleModule(moduleData)
      results = [result]
    } else {
      return
    }

    // Combine results
    if (results.length > 0) {
      importResult.value = {
        imported: results.reduce((sum, r) => sum + r.imported, 0),
        skipped: results.reduce((sum, r) => sum + r.skipped, 0),
        audioGenerated: results.reduce((sum, r) => sum + r.audioGenerated, 0),
        audioFailed: results.reduce((sum, r) => sum + r.audioFailed, 0),
        errors: results.flatMap((r) => r.errors),
        exercises: results.flatMap((r) => r.exercises),
      }

      // Clear form on success
      if (importResult.value.errors.length === 0 && importResult.value.imported > 0) {
        jsonContent.value = ''
        selectedFiles.value = []
        if (fileInput.value) {
          fileInput.value.value = ''
        }
      }
    }
  } catch (error: any) {
    console.error('Import failed:', error)
    errorMessage.value = error.message || 'Failed to import exercises'
  } finally {
    isImporting.value = false
  }
}

function getStatusSeverity(status: string): string {
  switch (status) {
    case 'IMPORTED':
      return 'success'
    case 'SKIPPED':
      return 'info'
    case 'FAILED':
      return 'danger'
    default:
      return 'secondary'
  }
}

function truncateUrl(url: string): string {
  if (url.length > 50) {
    return '...' + url.substring(url.length - 47)
  }
  return url
}
</script>

<style scoped>
/* File upload */
.file-upload-container {
  margin-top: var(--spacing-sm);
}

.hidden-file-input {
  display: none;
}

.upload-icon {
  font-size: 3rem;
}

/* Drop zone */
.drop-zone {
  border-radius: var(--radius-lg);
  padding: var(--spacing-4xl) var(--spacing-2xl);
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.drop-zone:hover {
}

.drop-zone.drag-over {
  transform: scale(1.02);
}

.drop-zone.has-files {
}

.drop-zone-text {
  margin: var(--spacing-md) 0 var(--spacing-sm) 0;
  font-size: 1.125rem;
}

.drop-zone-text strong {
}

.drop-zone-hint {
  margin: 0;
}

/* File list */
.file-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.file-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.file-item:hover {
}

.file-item i.pi-file {
  font-size: 1.25rem;
}

.file-name {
  flex: 1;
  font-weight: 500;
}

.file-size {
  font-family: monospace;
}

/* Stats grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--spacing-md);
}

.stat-card {
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  text-align: center;
}

.stat-label {
  margin-bottom: var(--spacing-sm);
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
}

.stat-value.success {
}

.stat-value.info {
}

.stat-value.error {
}

/* Table cells */
.audio-url-cell {
  font-family: monospace;
}

.text-muted {
}
</style>
