<script setup lang="ts">
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import FileUpload, {type FileUploadUploaderEvent} from 'primevue/fileupload'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import InputText from 'primevue/inputtext'
import Dropdown from 'primevue/dropdown'
import type {ReadingText} from '@/types/reading'

const router = useRouter()
const isUploading = ref(false)
const uploadedTexts = ref<ReadingText[]>([])
const fileStatus = ref<Record<string, { status: 'uploading' | 'success' | 'error'; message?: string }>>({})
const uploadSummary = ref<{
  successful: number;
  failed: number;
  total: number;
  errors: { file: string; message: string }[]
} | null>(null)
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// Import settings
const languageCode = ref('fr')
const level = ref('A1')
const topic = ref('')
const author = ref('')
const description = ref('')

const languageOptions = [
  {label: 'French', value: 'fr'},
  {label: 'Spanish', value: 'es'},
  {label: 'German', value: 'de'},
  {label: 'Italian', value: 'it'}
]

const levelOptions = [
  {label: 'A1', value: 'A1'},
  {label: 'A2', value: 'A2'},
  {label: 'B1', value: 'B1'},
  {label: 'B2', value: 'B2'},
  {label: 'C1', value: 'C1'},
  {label: 'C2', value: 'C2'}
]

const onFilesSelect = () => {
  fileStatus.value = {}
  uploadSummary.value = null
}

const handleUpload = async (event: FileUploadUploaderEvent) => {
  isUploading.value = true
  uploadSummary.value = null
  const files = Array.isArray(event.files) ? event.files : [event.files]
  const results = {successful: 0, failed: 0, total: files.length, errors: [] as { file: string; message: string }[]}

  for (const file of files) {
    try {
      fileStatus.value[file.name] = {status: 'uploading'}
      const content = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = e => resolve(e.target?.result as string)
        reader.onerror = () => reject(new Error('Failed to read file'))
        reader.readAsText(file)
      })

      // Determine file type and handle accordingly
      const isMarkdown = file.name.endsWith('.md') || file.name.endsWith('.txt')

      let response: Response

      if (isMarkdown) {
        // Handle markdown/text files
        const requestData = {
          content: content,
          languageCode: languageCode.value,
          level: level.value || null,
          topic: topic.value || null,
          description: description.value || null,
          author: author.value || 'import',
          source: file.name
        }
        response = await fetch(`${API_BASE}/api/reading/texts/import-markdown`, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify(requestData),
        })
      } else {
        // Handle JSON files (legacy format)
        const jsonData = JSON.parse(content)
        const requestData = {
          title: jsonData.title,
          content: jsonData.content,
          languageCode: jsonData.language_code,
          level: jsonData.level,
          topic: jsonData.topic,
          description: jsonData.description,
          author: jsonData.author || 'import',
          source: jsonData.source || 'manual-upload',
        }
        response = await fetch(`${API_BASE}/api/reading/texts/import`, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify(requestData),
        })
      }

      if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      uploadedTexts.value.push(await response.json())
      fileStatus.value[file.name] = {status: 'success'}
      results.successful++
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unknown error'
      fileStatus.value[file.name] = {status: 'error', message}
      results.errors.push({file: file.name, message})
      results.failed++
    }
  }
  uploadSummary.value = results
  isUploading.value = false
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024, sizes = ['B', 'KB', 'MB', 'GB'], i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}
const formatTopic = (topic: string) => topic.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
const getStatusSeverity = (status: string) => ({
  success: 'success',
  error: 'danger',
  uploading: 'info'
}[status] || 'info')
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <h1>Import Reading Texts</h1>
      <p class="text-secondary">Upload markdown (.md), text (.txt), or JSON files containing reading texts.</p>
    </div>

    <!-- Import Settings -->
    <Card class="mb-4">
      <template #title>Import Settings</template>
      <template #content>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
          <div class="flex flex-column gap-1">
            <label for="language" class="font-semibold text-sm">Language</label>
            <Dropdown id="language" v-model="languageCode" :options="languageOptions" optionLabel="label"
                      optionValue="value"/>
          </div>
          <div class="flex flex-column gap-1">
            <label for="level" class="font-semibold text-sm">CEFR Level</label>
            <Dropdown id="level" v-model="level" :options="levelOptions" optionLabel="label" optionValue="value"/>
          </div>
          <div class="flex flex-column gap-1">
            <label for="topic" class="font-semibold text-sm">Topic (Optional)</label>
            <InputText id="topic" v-model="topic" placeholder="e.g., travel, food"/>
          </div>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-3 mt-md">
          <div class="flex flex-column gap-1">
            <label for="author" class="font-semibold text-sm">Author (Optional)</label>
            <InputText id="author" v-model="author" placeholder="e.g., John Doe"/>
          </div>
          <div class="flex flex-column gap-1">
            <label for="description" class="font-semibold text-sm">Description (Optional)</label>
            <InputText id="description" v-model="description" placeholder="Brief description"/>
          </div>
        </div>
        <Message severity="info" :closable="false" class="mt-md">
          <p class="text-sm">These settings apply to all markdown/text files. JSON files use their own metadata.</p>
        </Message>
      </template>
    </Card>

    <FileUpload name="files[]" :multiple="true" accept=".md,.txt,.json" :maxFileSize="10000000" :customUpload="true"
                @uploader="handleUpload" @select="onFilesSelect" :auto="false">
      <template #header="{ chooseCallback, uploadCallback, clearCallback, files }">
        <div class="upload-header">
          <Button @click="chooseCallback()" icon="pi pi-plus" label="Choose" outlined/>
          <Button @click="uploadCallback()" icon="pi pi-upload" label="Upload All"
                  :disabled="!files || files.length === 0 || isUploading" :loading="isUploading"/>
          <Button @click="clearCallback()" icon="pi pi-times" label="Clear" severity="danger" outlined
                  :disabled="!files || files.length === 0 || isUploading"/>
        </div>
      </template>
      <template #content="{ files, removeFileCallback }">
        <div v-if="files.length > 0" class="content-area">
          <h3 class="font-bold">Files to Upload ({{ files.length }})</h3>
          <div class="task-list">
            <div v-for="(file, index) of files" :key="file.name + index" class="file-item bg-surface">
              <div class="file-info"><i class="pi pi-file text-xl"></i>
                <div class="file-details">
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-size text-secondary">{{ formatFileSize(file.size) }}</span>
                </div>
              </div>
              <div class="file-actions">
                <Tag v-if="fileStatus[file.name]" :value="fileStatus[file.name].status"
                     :severity="getStatusSeverity(fileStatus[file.name].status)"/>
                <Button icon="pi pi-times" @click="removeFileCallback(index)" text rounded severity="danger"
                        :disabled="isUploading"/>
              </div>
            </div>
          </div>
        </div>
        <div v-if="uploadedTexts.length > 0" class="content-area">
          <h3 class="font-bold">Successfully Imported ({{ uploadedTexts.length }})</h3>
          <div class="content-grid">
            <Card v-for="text in uploadedTexts" :key="text.id">
              <template #title>
                <div class="card-title-icon text-success"><i class="pi pi-check-circle"></i><span>{{
                    text.title
                  }}</span></div>
              </template>
              <template #content>
                <div class="meta-badges">
                  <Tag :value="text.languageCode.toUpperCase()"/>
                  <Tag v-if="text.level" :value="text.level" severity="secondary"/>
                  <Tag v-if="text.topic" :value="formatTopic(text.topic)" severity="contrast"/>
                  <span class="ml-auto text-secondary">{{ text.wordCount }} words</span>
                </div>
              </template>
            </Card>
          </div>
        </div>
        <div v-if="!files.length && !uploadedTexts.length" class="empty-state">
          <i class="pi pi-cloud-upload empty-icon"></i>
          <h3>Drag & Drop Files Here</h3>
          <p class="text-secondary">Supports .md, .txt, and .json files. Click "Choose" to select multiple files.</p>
        </div>
      </template>
    </FileUpload>

    <Card v-if="uploadSummary" class="mt-lg">
      <template #title>Upload Summary</template>
      <template #content>
        <div class="summary-stats">
          <div class="stat success"><i class="pi pi-check-circle"></i>
            <div><span class="stat-value">{{ uploadSummary.successful }}</span><span
                class="stat-label">Successful</span></div>
          </div>
          <div class="stat failed"><i class="pi pi-times-circle"></i>
            <div><span class="stat-value">{{ uploadSummary.failed }}</span><span class="stat-label">Failed</span></div>
          </div>
          <div class="stat total"><i class="pi pi-file"></i>
            <div><span class="stat-value">{{ uploadSummary.total }}</span><span class="stat-label">Total</span></div>
          </div>
        </div>
        <div v-if="uploadSummary.errors.length > 0" class="mt-lg p-md bg-red-50 dark:bg-red-900 rounded-md">
          <h4 class="text-red-700 dark:text-red-300 font-bold mb-2">Errors:</h4>
          <ul class="list-disc pl-lg m-0 text-red-600 dark:text-red-400">
            <li v-for="(error, index) in uploadSummary.errors" :key="index"><strong>{{ error.file }}:</strong>
              {{ error.message }}
            </li>
          </ul>
        </div>
        <div class="text-center mt-xl">
          <Button label="View Library" icon="pi pi-book" @click="router.push('/reading')"/>
        </div>
      </template>
    </Card>
  </div>
</template>
