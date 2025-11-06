<template>
  <div class="import-container">
    <h1>Import Language Data</h1>
    <p class="description">
      Upload JSON or JSON.gz files containing language vocabulary data
    </p>

    <div class="upload-section">
      <div class="field">
        <label for="language-select">Language</label>
        <Select
          id="language-select"
          v-model="selectedLanguage"
          :options="languages"
          optionLabel="name"
          optionValue="code"
          placeholder="Select a language"
          class="w-full"
        />
      </div>

      <div class="field">
        <label>Select File</label>
        <FileUpload
          mode="basic"
          name="file"
          :auto="false"
          :customUpload="true"
          @select="onFileSelect"
          accept=".json,.gz"
          :maxFileSize="500000000"
          chooseLabel="Choose File"
        />
      </div>

      <div class="button-group">
        <Button
          label="Start Import"
          icon="pi pi-upload"
          @click="startImport"
          :disabled="!selectedFile || !selectedLanguage || isUploading"
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
            <Tag :value="currentImport.status" :severity="getStatusSeverity(currentImport.status)" />
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
                <span class="text-green-600">{{ currentImport.successfulEntries.toLocaleString() }}</span>
              </div>
              <div class="stat">
                <label>Failed</label>
                <span class="text-red-600">{{ currentImport.failedEntries.toLocaleString() }}</span>
              </div>
            </div>

            <div class="progress-bar-container">
              <ProgressBar
                :value="currentImport.progressPercentage"
                :showValue="true"
              />
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
            <Tag :value="slotProps.data.status" :severity="getStatusSeverity(slotProps.data.status)" />
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
          <template #body="slotProps">
            {{ slotProps.data.progressPercentage }}%
          </template>
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
import { ref, onMounted, onUnmounted } from 'vue'
import Button from 'primevue/button'
import FileUpload from 'primevue/fileupload'
import Select from 'primevue/select'
import ProgressBar from 'primevue/progressbar'
import Card from 'primevue/card'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { useConfirm } from 'primevue/useconfirm'
import { useImportApi, type ImportProgress } from '../composables/useImportApi'

const { isUploading, uploadError, uploadFile, connectToProgressStream, getAllImports, clearDatabase } = useImportApi()
const confirm = useConfirm()

const languages = [
  { code: 'es', name: 'Spanish' },
  { code: 'it', name: 'Italian' },
  { code: 'ru', name: 'Russian' },
  { code: 'pt', name: 'Portuguese' },
  { code: 'fr', name: 'French' },
  { code: 'de', name: 'German' },
  { code: 'sv', name: 'Swedish' },
  { code: 'zh', name: 'Chinese' },
  { code: 'fi', name: 'Finnish' },
  { code: 'ja', name: 'Japanese' },
  { code: 'pl', name: 'Polish' },
  { code: 'nl', name: 'Dutch' }
]

const selectedLanguage = ref('')
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

function confirmClearDatabase() {
  confirm.require({
    message: 'Are you sure you want to clear all words from the database? This action cannot be undone.',
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
    }
  })
}

async function startImport() {
  if (!selectedFile.value || !selectedLanguage.value) return

  const response = await uploadFile(selectedFile.value, selectedLanguage.value)

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

function getStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' | undefined {
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
  padding: 2rem;
}

.description {
  color: var(--text-color-secondary);
  margin-bottom: 2rem;
}

.upload-section {
  background: var(--surface-card);
  padding: 2rem;
  border-radius: 8px;
  margin-bottom: 2rem;
}

.field {
  margin-bottom: 1.5rem;
}

.field label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.button-group {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.progress-section {
  margin-bottom: 2rem;
}

.progress-info {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
}

.stat {
  text-align: center;
  padding: 1rem;
  background: var(--surface-ground);
  border-radius: 6px;
}

.stat label {
  display: block;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  margin-bottom: 0.5rem;
}

.stat span {
  font-size: 1.5rem;
  font-weight: 600;
}

.progress-bar-container {
  margin-top: 1rem;
}

.message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.history-section {
  margin-top: 3rem;
}

.history-section h2 {
  margin-bottom: 1rem;
}
</style>
