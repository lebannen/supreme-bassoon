<template>
  <div class="reading-import-container">
    <div class="view-header">
      <h1>Import Reading Texts</h1>
      <p class="description">
        Upload JSON files containing reading texts. Each file should contain one text with title, content, and metadata.
      </p>
    </div>

    <div class="upload-section">
      <FileUpload
        name="files[]"
        :multiple="true"
        accept=".json"
        :maxFileSize="10000000"
        :customUpload="true"
        @uploader="handleUpload"
        @select="onFilesSelect"
        :auto="false"
      >
        <template #header="{ chooseCallback, uploadCallback, clearCallback, files }">
          <div class="upload-header">
            <Button
              @click="chooseCallback()"
              icon="pi pi-plus"
              label="Choose Files"
              outlined
            />
            <Button
              @click="uploadCallback()"
              icon="pi pi-upload"
              label="Upload All"
              :disabled="!files || files.length === 0 || isUploading"
              :loading="isUploading"
            />
            <Button
              @click="clearCallback()"
              icon="pi pi-times"
              label="Clear"
              severity="danger"
              outlined
              :disabled="!files || files.length === 0 || isUploading"
            />
          </div>
        </template>

        <template #content="{ files, uploadedFiles, removeUploadedFileCallback, removeFileCallback }">
          <div v-if="files.length > 0" class="files-list">
            <h3>Files to Upload ({{ files.length }})</h3>
            <div class="file-items">
              <div v-for="(file, index) of files" :key="file.name + index" class="file-item">
                <div class="file-info">
                  <i class="pi pi-file"></i>
                  <div class="file-details">
                    <span class="file-name">{{ file.name }}</span>
                    <span class="file-size">{{ formatFileSize(file.size) }}</span>
                  </div>
                </div>
                <div class="file-actions">
                  <Tag v-if="fileStatus[file.name]" :value="fileStatus[file.name].status" :severity="getStatusSeverity(fileStatus[file.name].status)" />
                  <Button
                    icon="pi pi-times"
                    @click="removeFileCallback(index)"
                    text
                    rounded
                    severity="danger"
                    :disabled="isUploading"
                  />
                </div>
              </div>
            </div>
          </div>

          <div v-if="uploadedTexts.length > 0" class="uploaded-section">
            <h3>Successfully Imported ({{ uploadedTexts.length }})</h3>
            <div class="uploaded-items">
              <Card v-for="text in uploadedTexts" :key="text.id" class="uploaded-card">
                <template #title>
                  <div class="uploaded-title">
                    <i class="pi pi-check-circle text-green-600"></i>
                    {{ text.title }}
                  </div>
                </template>
                <template #content>
                  <div class="uploaded-meta">
                    <Tag :value="text.languageCode.toUpperCase()" severity="info" />
                    <Tag v-if="text.level" :value="text.level" />
                    <span v-if="text.topic" class="topic-badge">{{ formatTopic(text.topic) }}</span>
                    <span class="word-count">{{ text.wordCount }} words</span>
                  </div>
                </template>
              </Card>
            </div>
          </div>

          <div v-if="files.length === 0 && uploadedTexts.length === 0" class="empty-state">
            <i class="pi pi-cloud-upload empty-icon"></i>
            <h3>Drag & Drop JSON Files Here</h3>
            <p>or click "Choose Files" to select multiple files</p>
            <small>Accepted format: .json files (max 10MB each)</small>
          </div>
        </template>

        <template #empty>
          <div class="empty-state">
            <i class="pi pi-cloud-upload empty-icon"></i>
            <h3>Drag & Drop JSON Files Here</h3>
            <p>or click "Choose Files" to select multiple files</p>
            <small>Accepted format: .json files (max 10MB each)</small>
          </div>
        </template>
      </FileUpload>
    </div>

    <Message v-if="globalError" severity="error" :closable="true" @close="globalError = null">
      {{ globalError }}
    </Message>

    <div v-if="uploadSummary" class="summary-section">
      <Card>
        <template #title>Upload Summary</template>
        <template #content>
          <div class="summary-stats">
            <div class="stat success">
              <i class="pi pi-check-circle"></i>
              <div>
                <span class="stat-value">{{ uploadSummary.successful }}</span>
                <span class="stat-label">Successful</span>
              </div>
            </div>
            <div class="stat failed">
              <i class="pi pi-times-circle"></i>
              <div>
                <span class="stat-value">{{ uploadSummary.failed }}</span>
                <span class="stat-label">Failed</span>
              </div>
            </div>
            <div class="stat total">
              <i class="pi pi-file"></i>
              <div>
                <span class="stat-value">{{ uploadSummary.total }}</span>
                <span class="stat-label">Total</span>
              </div>
            </div>
          </div>

          <div v-if="uploadSummary.errors.length > 0" class="errors-list">
            <h4>Errors:</h4>
            <ul>
              <li v-for="(error, index) in uploadSummary.errors" :key="index">
                <strong>{{ error.file }}:</strong> {{ error.message }}
              </li>
            </ul>
          </div>

          <div class="flex justify-center mt-library-button">
            <Button label="View Library" icon="pi pi-book" @click="router.push('/reading')" />
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import FileUpload, { FileUploadUploaderEvent } from 'primevue/fileupload'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import type { ReadingText } from '@/composables/useReadingTexts'

const router = useRouter()

const isUploading = ref(false)
const uploadedTexts = ref<ReadingText[]>([])
const fileStatus = ref<Record<string, { status: string; message?: string }>>({})
const globalError = ref<string | null>(null)
const uploadSummary = ref<{ successful: number; failed: number; total: number; errors: Array<{ file: string; message: string }> } | null>(null)

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

function onFilesSelect() {
  // Reset status when new files are selected
  fileStatus.value = {}
  uploadSummary.value = null
  globalError.value = null
}

async function handleUpload(event: FileUploadUploaderEvent) {
  isUploading.value = true
  uploadSummary.value = null
  globalError.value = null

  const files = Array.isArray(event.files) ? event.files : [event.files]
  const results = {
    successful: 0,
    failed: 0,
    total: files.length,
    errors: [] as Array<{ file: string; message: string }>
  }

  for (const file of files) {
    try {
      fileStatus.value[file.name] = { status: 'uploading' }

      // Read file content
      const content = await readFileAsText(file)
      const jsonData = JSON.parse(content)

      // Prepare request data (convert snake_case to camelCase)
      const requestData = {
        title: jsonData.title,
        content: jsonData.content,
        languageCode: jsonData.language_code,
        level: jsonData.level || null,
        topic: jsonData.topic || null,
        description: jsonData.description || null,
        author: jsonData.author || 'import',
        source: jsonData.source || 'manual-upload'
      }

      // Upload to API
      const response = await fetch(`${API_BASE}/api/reading/texts/import`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const text = await response.json()
      uploadedTexts.value.push(text)
      fileStatus.value[file.name] = { status: 'success' }
      results.successful++
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unknown error'
      fileStatus.value[file.name] = { status: 'error', message }
      results.errors.push({ file: file.name, message })
      results.failed++
    }
  }

  uploadSummary.value = results
  isUploading.value = false
}

function readFileAsText(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => resolve(e.target?.result as string)
    reader.onerror = () => reject(new Error('Failed to read file'))
    reader.readAsText(file)
  })
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

function formatTopic(topic: string): string {
  return topic
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

function getStatusSeverity(status: string): 'success' | 'danger' | 'info' | 'warn' {
  switch (status) {
    case 'success': return 'success'
    case 'error': return 'danger'
    case 'uploading': return 'info'
    default: return 'info'
  }
}
</script>

<style scoped>
.reading-import-container {
  min-height: 100vh;
  background: var(--surface-ground);
}

.view-header {
  text-align: center;
}

.view-header h1 {
  margin: 0 0 0.5rem 0;
  color: var(--text-color);
  font-size: 2rem;
  font-weight: 600;
}

.description {
  margin: 0;
  color: var(--text-color-secondary);
  font-size: 1.125rem;
}

.upload-section {
  max-width: 1200px;
  margin: 2rem auto;
  padding: 0 2rem;
}

.files-list,
.uploaded-section {
  padding: 1.5rem;
}

.files-list h3,
.uploaded-section h3 {
  margin: 0 0 1rem 0;
  color: var(--text-color);
}

.file-items {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.uploaded-items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

.uploaded-card {
  border: 1px solid var(--surface-border);
}

.uploaded-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
}

.uploaded-meta {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
}

.word-count {
  color: var(--text-color-secondary);
  margin-left: auto;
}

.empty-state {
  border: 2px dashed var(--surface-border);
  background: var(--surface-section);
}

.summary-section {
  max-width: 800px;
  margin: 2rem auto;
  padding: 0 2rem;
}

.errors-list {
  margin-top: 1.5rem;
  padding: 1rem;
  background: var(--red-50);
  border-radius: 8px;
}

.errors-list h4 {
  margin: 0 0 0.5rem 0;
  color: var(--red-700);
}

.errors-list ul {
  margin: 0;
  padding-left: 1.5rem;
  color: var(--red-600);
}

.errors-list li {
  margin: 0.25rem 0;
}

.mt-library-button {
  margin-top: 1.5rem;
}

@media (max-width: 768px) {
  .view-header h1 {
    font-size: 1.5rem;
  }

  .upload-section,
  .summary-section {
    padding: 0 1rem;
  }

  .uploaded-items {
    grid-template-columns: 1fr;
  }
}
</style>
