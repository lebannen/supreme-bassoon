<template>
  <div class="import-container">
    <h1>Import Language Data</h1>
    <p class="description">
      Upload JSONL.gz files containing language vocabulary data. Language will be automatically
      detected from the file.
    </p>

    <div class="upload-section">
      <div class="field">
        <label>Select File</label>
        <FileUpload
          mode="basic"
          name="file"
          :auto="false"
          :customUpload="true"
          @select="onFileSelect"
          accept=".jsonl.gz,.gz"
          :maxFileSize="1000000000"
          chooseLabel="Choose File"
        />
        <small v-if="selectedFile" class="file-info">
          Selected: {{ selectedFile.name }} ({{ formatFileSize(selectedFile.size) }})
        </small>
      </div>

      <div class="button-group">
        <Button
          label="Start Import"
          icon="pi pi-upload"
          @click="startImport"
          :disabled="!selectedFile || isUploading"
          :loading="isUploading"
        />
        <Button
          label="Clear Database"
          icon="pi pi-trash"
          severity="danger"
          @click="confirmClearDatabase"
          :disabled="isUploading || isClearing"
          :loading="isClearing"
        />
      </div>
    </div>

    <Message v-if="uploadError" severity="error" :closable="false">
      {{ uploadError }}
    </Message>

    <Message v-if="clearSuccess" severity="success" :closable="true" @close="clearSuccess = null">
      {{ clearSuccess }}
    </Message>

    <div v-if="currentImport" class="progress-section">
      <Card>
        <template #title>
          <div class="flex align-items-center justify-content-between">
            <span>Importing {{ currentImport.languageName }}</span>
            <Tag
                :value="currentImport.status"
                :severity="getStatusSeverity(currentImport.status)"
            />
          </div>
        </template>
        <template #content>
          <div class="progress-info">
            <div class="stats-grid">
              <div class="stat">
                <label>Total Entries</label>
                <span>{{ currentImport.totalEntries.toLocaleString() }}</span>
              </div>
              <div class="stat">
                <label>Processed</label>
                <span>{{ currentImport.processedEntries.toLocaleString() }}</span>
              </div>
              <div class="stat">
                <label>Successful</label>
                <span class="text-green-600">{{
                    currentImport.successfulEntries.toLocaleString()
                  }}</span>
              </div>
              <div class="stat">
                <label>Failed</label>
                <span class="text-red-600">{{ currentImport.failedEntries.toLocaleString() }}</span>
              </div>
            </div>

            <div class="progress-bar-container">
              <ProgressBar :value="currentImport.progressPercentage" :showValue="true"/>
            </div>

            <div class="message">
              <i class="pi pi-info-circle"></i>
              <span>{{ currentImport.message }}</span>
            </div>

            <div v-if="currentImport.error" class="error-message">
              <Message severity="error" :closable="false">
                {{ currentImport.error }}
              </Message>
            </div>
          </div>
        </template>
      </Card>
    </div>

    <div v-if="importHistory.length > 0" class="history-section">
      <h2>Import History</h2>
      <DataTable :value="importHistory" :paginator="true" :rows="10">
        <Column field="languageName" header="Language"></Column>
        <Column field="status" header="Status">
          <template #body="slotProps">
            <Tag
                :value="slotProps.data.status"
                :severity="getStatusSeverity(slotProps.data.status)"
            />
          </template>
        </Column>
        <Column field="totalEntries" header="Total Entries">
          <template #body="slotProps">
            {{ slotProps.data.totalEntries.toLocaleString() }}
          </template>
        </Column>
        <Column field="successfulEntries" header="Successful">
          <template #body="slotProps">
            {{ slotProps.data.successfulEntries.toLocaleString() }}
          </template>
        </Column>
        <Column field="progressPercentage" header="Progress">
          <template #body="slotProps"> {{ slotProps.data.progressPercentage }}%</template>
        </Column>
        <Column field="startedAt" header="Started">
          <template #body="slotProps">
            {{ formatDate(slotProps.data.startedAt) }}
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, onUnmounted, ref} from 'vue'
import Button from 'primevue/button'
import FileUpload from 'primevue/fileupload'
import ProgressBar from 'primevue/progressbar'
import Card from 'primevue/card'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import {useConfirm} from 'primevue/useconfirm'

// Import progress type
interface ImportProgress {
  importId: string
  languageName: string
  status: string
  totalEntries: number
  processedEntries: number
  successfulEntries: number
  failedEntries: number
  progressPercentage: number
  message: string
  error?: string
  startedAt: string
}

const confirm = useConfirm()
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// State
const isUploading = ref(false)
const uploadError = ref<string | null>(null)

// Upload file function
async function uploadFile(file: File) {
  isUploading.value = true
  uploadError.value = null

  try {
    const formData = new FormData()
    formData.append('file', file)

    const response = await fetch(`${API_BASE}/api/import/upload`, {
      method: 'POST',
      body: formData,
    })

    if (!response.ok) {
      const errorData = await response.json()
      throw new Error(errorData.message || 'Upload failed')
    }

    const data = await response.json()
    return data
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : 'Upload failed'
    return null
  } finally {
    isUploading.value = false
  }
}

// Connect to progress stream
function connectToProgressStream(
    importId: string,
    onProgress: (progress: ImportProgress) => void,
    onComplete: () => void,
    onError: (error: Error) => void
) {
  const eventSource = new EventSource(`${API_BASE}/api/import/${importId}/progress`)

  eventSource.addEventListener('progress', (event: MessageEvent) => {
    const progress = JSON.parse(event.data) as ImportProgress
    onProgress(progress)

    if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
      eventSource.close()
      onComplete()
    }
  })

  eventSource.addEventListener('error', () => {
    onError(new Error('Connection to progress stream failed'))
    eventSource.close()
  })

  return () => eventSource.close()
}

// Get all imports
async function getAllImports(): Promise<ImportProgress[]> {
  try {
    const response = await fetch(`${API_BASE}/api/import/history`)
    if (!response.ok) throw new Error('Failed to load import history')
    return await response.json()
  } catch {
    return []
  }
}

// Clear database
async function clearDatabase() {
  try {
    const response = await fetch(`${API_BASE}/api/import/clear`, {
      method: 'DELETE',
    })
    if (!response.ok) throw new Error('Failed to clear database')
    return await response.json()
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : 'Failed to clear database'
    return null
  }
}

const selectedFile = ref<File | null>(null)
const currentImport = ref<ImportProgress | null>(null)
const importHistory = ref<ImportProgress[]>([])
const isClearing = ref(false)
const clearSuccess = ref<string | null>(null)
let cleanupProgress: (() => void) | null = null

function onFileSelect(event: any) {
  const files = event.files
  if (files && files.length > 0) {
    selectedFile.value = files[0]
  }
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

function confirmClearDatabase() {
  confirm.require({
    message:
        'Are you sure you want to clear all words from the database? This action cannot be undone.',
    header: 'Confirm Clear Database',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: async () => {
      isClearing.value = true
      clearSuccess.value = null
      const result = await clearDatabase()
      isClearing.value = false

      if (result) {
        clearSuccess.value = `Successfully cleared ${result.deletedCount.toLocaleString()} words from database`
        await loadImportHistory()
      }
    },
  })
}

async function startImport() {
  if (!selectedFile.value) return

  // Language will be auto-detected from file metadata
  const response = await uploadFile(selectedFile.value)

  if (response) {
    // Connect to progress stream
    cleanupProgress = connectToProgressStream(
      response.importId,
      (progress) => {
        currentImport.value = progress
      },
      async () => {
        // Import completed, refresh history
        await loadImportHistory()
      },
      (error) => {
        uploadError.value = error.message
      }
    )
  }
}

async function loadImportHistory() {
  importHistory.value = await getAllImports()
}

function getStatusSeverity(
    status: string
): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' | undefined {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'PROCESSING':
      return 'info'
    case 'QUEUED':
      return 'secondary'
    case 'FAILED':
      return 'danger'
    case 'CANCELLED':
      return 'warn'
    default:
      return undefined
  }
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleString()
}

onMounted(() => {
  loadImportHistory()
})

onUnmounted(() => {
  if (cleanupProgress) {
    cleanupProgress()
  }
})
</script>

<style scoped>
.import-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-2xl) var(--spacing-xl);
}

h1 {
  color: var(--text-primary);
  font-weight: 700;
  margin-bottom: var(--spacing-xs);
}

.description {
  color: var(--text-secondary);
  margin-bottom: var(--spacing-2xl);
}

.upload-section {
  background: var(--bg-secondary);
  padding: var(--spacing-xl);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-medium);
  margin-bottom: var(--spacing-xl);
}

.field {
  margin-bottom: var(--spacing-xl);
}

.field:last-child {
  margin-bottom: 0;
}

.field label {
  display: block;
  margin-bottom: var(--spacing-xs);
  font-weight: 600;
  color: var(--text-primary);
}

.file-info {
  display: block;
  margin-top: var(--spacing-xs);
  color: var(--text-secondary);
}

.button-group {
  display: flex;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.progress-section {
  margin-bottom: var(--spacing-xl);
}

.progress-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--spacing-md);
}

.stat {
  text-align: center;
  padding: var(--spacing-md);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
}

.stat label {
  display: block;
  color: var(--text-secondary);
  margin-bottom: var(--spacing-xs);
}

.stat span {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
}

.progress-bar-container {
  margin-top: var(--spacing-md);
}

.message {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  color: var(--text-secondary);
}

.history-section {
  margin-top: var(--spacing-3xl);
}

.history-section h2 {
  margin-bottom: var(--spacing-md);
  color: var(--text-primary);
  font-weight: 600;
}

@media (max-width: 768px) {
  .import-container {
    padding: var(--spacing-xl) var(--spacing-md);
  }

  .upload-section {
    padding: var(--spacing-lg);
  }

  .button-group {
    flex-direction: column;
  }

  .button-group button {
    width: 100%;
  }

  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
