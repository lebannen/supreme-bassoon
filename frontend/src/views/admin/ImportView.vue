<script setup lang="ts">
import {onMounted, onUnmounted, ref} from 'vue'
import Button from 'primevue/button'
import FileUpload, {type FileUploadSelectEvent} from 'primevue/fileupload'
import ProgressBar from 'primevue/progressbar'
import Card from 'primevue/card'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import {useConfirm} from 'primevue/useconfirm'

interface ImportProgress {
  importId: string
  languageName: string
  status: 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
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

const selectedFile = ref<File | null>(null)
const currentImport = ref<ImportProgress | null>(null)
const importHistory = ref<ImportProgress[]>([])
const isUploading = ref(false)
const uploadError = ref<string | null>(null)
const isClearing = ref(false)
const clearSuccess = ref<string | null>(null)
let cleanupProgress: (() => void) | null = null

const onFileSelect = (event: FileUploadSelectEvent) => {
  selectedFile.value = event.files[0]
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

const startImport = async () => {
  if (!selectedFile.value) return
  isUploading.value = true
  uploadError.value = null
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const response = await fetch(`${API_BASE}/api/import/upload`, {method: 'POST', body: formData})
    if (!response.ok) throw new Error((await response.json()).message || 'Upload failed')
    const data = await response.json()
    connectToProgressStream(data.importId)
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : 'Upload failed'
  } finally {
    isUploading.value = false
  }
}

const connectToProgressStream = (importId: string) => {
  const eventSource = new EventSource(`${API_BASE}/api/import/${importId}/progress`)
  eventSource.onmessage = (event) => {
    const progress = JSON.parse(event.data) as ImportProgress
    currentImport.value = progress
    if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(progress.status)) {
      eventSource.close()
      loadImportHistory()
    }
  }
  eventSource.onerror = () => {
    uploadError.value = 'Connection to progress stream failed.'
    eventSource.close()
  }
  cleanupProgress = () => eventSource.close()
}

const loadImportHistory = async () => {
  try {
    const response = await fetch(`${API_BASE}/api/import/history`)
    if (!response.ok) throw new Error('Failed to load import history')
    importHistory.value = await response.json()
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : 'Failed to load history'
  }
}

const confirmClearDatabase = () => {
  confirm.require({
    message: 'Are you sure you want to clear all words from the database? This action cannot be undone.',
    header: 'Confirm Clear Database',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: async () => {
      isClearing.value = true
      clearSuccess.value = null
      try {
        const response = await fetch(`${API_BASE}/api/import/clear`, {method: 'DELETE'})
        if (!response.ok) throw new Error('Failed to clear database')
        const result = await response.json()
        clearSuccess.value = `Successfully cleared ${result.deletedCount.toLocaleString()} words.`
        loadImportHistory()
      } catch (err) {
        uploadError.value = err instanceof Error ? err.message : 'Failed to clear database'
      } finally {
        isClearing.value = false
      }
    },
  })
}

const getStatusSeverity = (status: ImportProgress['status']) => {
  const map = {COMPLETED: 'success', PROCESSING: 'info', QUEUED: 'secondary', FAILED: 'danger', CANCELLED: 'warn'}
  return map[status]
}

const formatDate = (dateString: string) => new Date(dateString).toLocaleString()

onMounted(loadImportHistory)
onUnmounted(() => cleanupProgress?.())
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <h1>Import Language Data</h1>
      <p class="text-secondary">Upload JSONL.gz files containing language vocabulary data.</p>
    </div>

    <Card>
      <template #content>
        <div class="content-area">
          <div>
            <label class="font-semibold mb-sm block">Select File</label>
            <FileUpload mode="basic" name="file" :auto="false" :customUpload="true" @select="onFileSelect"
                        accept=".jsonl.gz" :maxFileSize="1000000000" chooseLabel="Choose File"/>
            <small v-if="selectedFile" class="text-secondary mt-sm block">
              Selected: {{ selectedFile.name }} ({{ formatFileSize(selectedFile.size) }})
            </small>
          </div>
          <div class="flex gap-sm flex-wrap">
            <Button label="Start Import" icon="pi pi-upload" @click="startImport"
                    :disabled="!selectedFile || isUploading" :loading="isUploading"/>
            <Button label="Clear Database" icon="pi pi-trash" severity="danger" @click="confirmClearDatabase"
                    :disabled="isUploading || isClearing" :loading="isClearing" outlined/>
          </div>
        </div>
      </template>
    </Card>

    <Message v-if="uploadError" severity="error">{{ uploadError }}</Message>
    <Message v-if="clearSuccess" severity="success" @close="clearSuccess = null">{{ clearSuccess }}</Message>

    <Card v-if="currentImport">
      <template #title>
        <div class="flex items-center justify-between">
          <span>Importing {{ currentImport.languageName }}</span>
          <Tag :value="currentImport.status" :severity="getStatusSeverity(currentImport.status)"/>
        </div>
      </template>
      <template #content>
        <div class="content-area-lg">
          <div class="summary-stats">
            <div class="stat total">
              <i class="pi pi-database"></i>
              <div>
                <span class="stat-value">{{ currentImport.totalEntries.toLocaleString() }}</span>
                <span class="stat-label">Total</span>
              </div>
            </div>
            <div class="stat">
              <i class="pi pi-spin pi-spinner"></i>
              <div>
                <span class="stat-value">{{ currentImport.processedEntries.toLocaleString() }}</span>
                <span class="stat-label">Processed</span>
              </div>
            </div>
            <div class="stat success">
              <i class="pi pi-check-circle"></i>
              <div>
                <span class="stat-value">{{ currentImport.successfulEntries.toLocaleString() }}</span>
                <span class="stat-label">Successful</span>
              </div>
            </div>
            <div class="stat failed">
              <i class="pi pi-times-circle"></i>
              <div>
                <span class="stat-value">{{ currentImport.failedEntries.toLocaleString() }}</span>
                <span class="stat-label">Failed</span>
              </div>
            </div>
          </div>
          <ProgressBar :value="currentImport.progressPercentage" :showValue="true"/>
          <p class="text-secondary text-center">{{ currentImport.message }}</p>
          <Message v-if="currentImport.error" severity="error">{{ currentImport.error }}</Message>
        </div>
      </template>
    </Card>

    <Card v-if="importHistory.length > 0">
      <template #title>Import History</template>
      <template #content>
        <DataTable :value="importHistory" :paginator="true" :rows="10" stripedRows>
          <Column field="languageName" header="Language"></Column>
          <Column field="status" header="Status">
            <template #body="{ data }">
              <Tag :value="data.status" :severity="getStatusSeverity(data.status)"/>
            </template>
          </Column>
          <Column field="totalEntries" header="Total Entries">
            <template #body="{ data }">{{ data.totalEntries.toLocaleString() }}</template>
          </Column>
          <Column field="successfulEntries" header="Successful">
            <template #body="{ data }">{{ data.successfulEntries.toLocaleString() }}</template>
          </Column>
          <Column field="progressPercentage" header="Progress">
            <template #body="{ data }">{{ data.progressPercentage }}%</template>
          </Column>
          <Column field="startedAt" header="Started">
            <template #body="{ data }">{{ formatDate(data.startedAt) }}</template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>
