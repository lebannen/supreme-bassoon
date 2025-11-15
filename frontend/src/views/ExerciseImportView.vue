<template>
  <div class="exercise-import-view">
    <div class="container">
      <Card>
        <template #title>
          <h2>Exercise Import</h2>
        </template>
        <template #content>
          <div class="import-form">
            <div class="field">
              <label for="jsonContent">Module JSON Content</label>
              <Textarea
                id="jsonContent"
                v-model="jsonContent"
                rows="15"
                placeholder="Paste JSON from module_X_exercises.json file here..."
                :disabled="isImporting"
              />
            </div>

            <div class="options-section">
              <div class="field-checkbox">
                <Checkbox
                  id="generateAudio"
                  v-model="generateAudio"
                  :binary="true"
                  :disabled="isImporting"
                />
                <label for="generateAudio">Generate Audio for Listening Exercises</label>
              </div>

              <div class="field-checkbox">
                <Checkbox
                  id="overwriteExisting"
                  v-model="overwriteExisting"
                  :binary="true"
                  :disabled="isImporting"
                />
                <label for="overwriteExisting">Overwrite Existing Exercises</label>
              </div>
            </div>

            <Button
              label="Import Exercises"
              icon="pi pi-upload"
              @click="importExercises"
              :loading="isImporting"
              :disabled="!jsonContent.trim()"
            />

            <Message v-if="errorMessage" severity="error" :closable="false">
              {{ errorMessage }}
            </Message>

            <div v-if="importResult" class="results-section">
              <Divider />
              <h3>Import Results</h3>

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

              <div v-if="importResult.errors.length > 0" class="errors-section">
                <h4>Errors</h4>
                <Message
                  v-for="(error, index) in importResult.errors"
                  :key="index"
                  severity="warn"
                  :closable="false"
                >
                  {{ error }}
                </Message>
              </div>

              <div v-if="importResult.exercises.length > 0" class="exercises-section">
                <h4>Exercises</h4>
                <DataTable :value="importResult.exercises" striped-rows>
                  <Column field="status" header="Status">
                    <template #body="{ data }">
                      <Tag
                        :value="data.status"
                        :severity="getStatusSeverity(data.status)"
                      />
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
import { ref } from 'vue'
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

function getAuthHeaders(): HeadersInit {
  const token = localStorage.getItem('auth_token')
  const headers: HeadersInit = {
    'Content-Type': 'application/json'
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

async function importExercises() {
  if (!jsonContent.value.trim()) {
    return
  }

  isImporting.value = true
  errorMessage.value = ''
  importResult.value = null

  try {
    // Parse JSON to validate
    let moduleData
    try {
      moduleData = JSON.parse(jsonContent.value)
    } catch (e) {
      errorMessage.value = 'Invalid JSON format'
      return
    }

    const response = await fetch(
      `${API_BASE}/api/admin/exercises/import?generateAudio=${generateAudio.value}&overwriteExisting=${overwriteExisting.value}`,
      {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(moduleData)
      }
    )

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const result = await response.json()
    importResult.value = result

    if (result.errors.length === 0 && result.imported > 0) {
      // Success - clear the form
      jsonContent.value = ''
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
.exercise-import-view {
  padding: 2rem;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.import-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field label {
  font-weight: 600;
  color: var(--text-color);
}

.options-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.field-checkbox label {
  cursor: pointer;
  user-select: none;
}

.results-section {
  margin-top: 1rem;
}

.results-section h3 {
  margin-bottom: 1rem;
}

.results-section h4 {
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stat-card {
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-radius: 6px;
  padding: 1rem;
  text-align: center;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  margin-bottom: 0.5rem;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
}

.stat-value.success {
  color: var(--green-500);
}

.stat-value.info {
  color: var(--blue-500);
}

.stat-value.error {
  color: var(--red-500);
}

.errors-section {
  margin-top: 1rem;
}

.errors-section .p-message {
  margin-bottom: 0.5rem;
}

.exercises-section {
  margin-top: 1rem;
}

.audio-url-cell {
  font-family: monospace;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.text-muted {
  color: var(--text-color-secondary);
}
</style>
