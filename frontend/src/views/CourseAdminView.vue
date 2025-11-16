<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import FileUpload, { type FileUploadSelectEvent } from 'primevue/fileupload'
import Message from 'primevue/message'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Checkbox from 'primevue/checkbox'
import ProgressBar from 'primevue/progressbar'
import Divider from 'primevue/divider'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

interface Course {
  id: number
  slug: string
  name: string
  languageCode: string
  cefrLevel: string
  estimatedHours: number
  isPublished: boolean
}

interface Module {
  id: number
  moduleNumber: number
  title: string
  episodeCount: number
}

interface ImportResult {
  success: boolean
  message: string
  courseId?: number
  moduleId?: number
  stats?: {
    episodesCreated: number
    exercisesCreated: number
    audioGenerated: number
    audioFailed: number
  }
}

const courses = ref<Course[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

// Course import
const courseFile = ref<File | null>(null)
const importingCourse = ref(false)
const courseImportResult = ref<ImportResult | null>(null)

// Module import
const selectedCourse = ref<Course | null>(null)
const moduleFile = ref<File | null>(null)
const generateAudio = ref(true)
const importingModule = ref(false)
const moduleImportResult = ref<ImportResult | null>(null)

// Module import progress
const importProgress = ref<{
  importId: string
  status: string
  totalModules: number
  processedModules: number
  totalEpisodes: number
  processedEpisodes: number
  totalExercises: number
  processedExercises: number
  audioFilesGenerated: number
  currentModule: string | null
  currentEpisode: string | null
  message: string
  progressPercentage: number
  errors: string[]
} | null>(null)

// Modules management
const courseModules = ref<Map<number, Module[]>>(new Map())
const loadingModules = ref<Set<number>>(new Set())
const expandedRows = ref<Course[]>([])

function getAuthHeaders() {
  const token = localStorage.getItem('auth_token')
  if (!token) {
    console.error('No auth token found in localStorage')
    return {
      'Content-Type': 'application/json'
    }
  }
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
}

async function loadCourses() {
  try {
    loading.value = true
    error.value = null

    const token = localStorage.getItem('auth_token')
    if (!token) {
      error.value = 'You must be logged in to access this page'
      loading.value = false
      return
    }

    const response = await fetch(`${API_BASE}/api/admin/courses`, {
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      if (response.status === 401) {
        error.value = 'Authentication failed. Please log in again.'
      } else {
        error.value = `Failed to load courses (${response.status})`
      }
      return
    }

    courses.value = await response.json()
  } catch (err) {
    console.error('Error loading courses:', err)
    error.value = err instanceof Error ? err.message : 'Failed to load courses'
  } finally {
    loading.value = false
  }
}

async function handleCourseFileSelect(event: FileUploadSelectEvent) {
  if (event.files && event.files.length > 0) {
    courseFile.value = event.files[0]
  }
}

async function importCourse() {
  if (!courseFile.value) {
    error.value = 'Please select a course file'
    return
  }

  try {
    importingCourse.value = true
    error.value = null
    successMessage.value = null
    courseImportResult.value = null

    const fileContent = await courseFile.value.text()
    const courseData = JSON.parse(fileContent)

    const response = await fetch(`${API_BASE}/api/admin/courses/import`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(courseData)
    })

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to import course' }))
      throw new Error(errorData.message || 'Failed to import course')
    }

    const result = await response.json()
    courseImportResult.value = result
    successMessage.value = `Course "${courseData.name}" imported successfully!`

    // Reload courses list
    await loadCourses()

    // Clear file
    courseFile.value = null
  } catch (err) {
    console.error('Error importing course:', err)
    error.value = err instanceof Error ? err.message : 'Failed to import course'
  } finally {
    importingCourse.value = false
  }
}

async function handleModuleFileSelect(event: FileUploadSelectEvent) {
  if (event.files && event.files.length > 0) {
    moduleFile.value = event.files[0]
  }
}

function streamImportProgress(importId: string): EventSource | null {
  const token = localStorage.getItem('auth_token')
  if (!token) {
    error.value = 'Authentication token not found. Please log in again.'
    importingModule.value = false
    return null
  }

  const url = new URL(`${API_BASE}/api/admin/courses/import/progress/${importId}`)
  url.searchParams.append('token', token)

  const eventSource = new EventSource(url.toString())

  eventSource.addEventListener('progress', (event: MessageEvent) => {
    try {
      const data = JSON.parse(event.data)
      importProgress.value = data

      // Check if completed or failed
      if (data.status === 'COMPLETED') {
        importingModule.value = false
        successMessage.value = `Module imported successfully! ${data.processedEpisodes} episodes, ${data.processedExercises} exercises, ${data.audioFilesGenerated} audio files`

        // Show any errors that occurred during import
        if (data.errors && data.errors.length > 0) {
          error.value = `Import completed with ${data.errors.length} warnings. Check console for details.`
          console.warn('Import warnings:', data.errors)
        }

        // Clear file and reload if needed
        moduleFile.value = null
        // Don't close here - wait for 'complete' event
      } else if (data.status === 'FAILED') {
        importingModule.value = false
        error.value = data.error || 'Import failed'
        // Don't close here - wait for 'complete' event
      }
    } catch (err) {
      console.error('Error parsing SSE data:', err)
    }
  })

  eventSource.addEventListener('complete', () => {
    console.log('SSE stream completed, closing connection')
    eventSource.close()
  })

  eventSource.addEventListener('not_found', () => {
    console.error('Import not found')
    error.value = 'Import not found. It may have expired or never existed.'
    importingModule.value = false
    eventSource.close()
  })

  eventSource.onerror = (err) => {
    console.error('SSE error:', err)

    // Only show error if we're still importing (not if stream just ended normally)
    if (importingModule.value && (!importProgress.value ||
        (importProgress.value.status !== 'COMPLETED' && importProgress.value.status !== 'FAILED'))) {
      error.value = 'Lost connection to import progress stream'
      importingModule.value = false
    }

    eventSource.close()
  }

  return eventSource
}

async function importModule() {
  if (!selectedCourse.value) {
    error.value = 'Please select a course'
    return
  }

  if (!moduleFile.value) {
    error.value = 'Please select a module file'
    return
  }

  try {
    importingModule.value = true
    error.value = null
    successMessage.value = null
    moduleImportResult.value = null
    importProgress.value = null

    const fileContent = await moduleFile.value.text()
    const moduleData = JSON.parse(fileContent)

    // Use async endpoint
    const response = await fetch(
      `${API_BASE}/api/admin/courses/${selectedCourse.value.slug}/modules/import-async?generateAudio=${generateAudio.value}`,
      {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(moduleData)
      }
    )

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to start import' }))
      throw new Error(errorData.message || 'Failed to start import')
    }

    const result = await response.json()

    // Start streaming progress
    if (result.importId) {
      const eventSource = streamImportProgress(result.importId)
      if (!eventSource) {
        // Error already set in streamImportProgress
        return
      }
    } else {
      throw new Error('No import ID received')
    }
  } catch (err) {
    console.error('Error importing module:', err)
    error.value = err instanceof Error ? err.message : 'Failed to import module'
    importingModule.value = false
  }
}

function selectCourse(course: Course) {
  selectedCourse.value = course
  moduleImportResult.value = null
}

async function onRowExpand(event: any) {
  const courseId = event.data.id
  if (!courseModules.value.has(courseId)) {
    await loadModulesForCourse(courseId)
  }
}

async function loadModulesForCourse(courseId: number) {
  try {
    loadingModules.value.add(courseId)

    const response = await fetch(`${API_BASE}/api/admin/courses/${courseId}/modules`, {
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error(`Failed to load modules (${response.status})`)
    }

    const modules = await response.json()
    courseModules.value.set(courseId, modules)
  } catch (err) {
    console.error('Error loading modules:', err)
    error.value = err instanceof Error ? err.message : 'Failed to load modules'
  } finally {
    loadingModules.value.delete(courseId)
  }
}

async function deleteModule(moduleId: number, moduleNumber: number, courseId: number) {
  if (!confirm(`Are you sure you want to delete Module ${moduleNumber}? This will also delete all its episodes and exercises.`)) {
    return
  }

  try {
    const response = await fetch(`${API_BASE}/api/admin/courses/modules/${moduleId}`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error(`Failed to delete module (${response.status})`)
    }

    successMessage.value = `Module ${moduleNumber} deleted successfully`

    // Reload modules for the course
    await loadModulesForCourse(courseId)
  } catch (err) {
    console.error('Error deleting module:', err)
    error.value = err instanceof Error ? err.message : 'Failed to delete module'
  }
}

onMounted(() => {
  loadCourses()
})
</script>

<template>
  <div class="course-admin-view">
    <div class="admin-container">
      <div class="header">
        <h1>
          <i class="pi pi-cog"></i>
          Course Administration
        </h1>
        <p class="subtitle">Import and manage course content</p>
      </div>

      <!-- Global Messages -->
      <Message v-if="error" severity="error" :closable="true" @close="error = null">
        {{ error }}
      </Message>

      <Message v-if="successMessage" severity="success" :closable="true" @close="successMessage = null">
        {{ successMessage }}
      </Message>

      <!-- Import Course -->
      <Card class="import-card">
        <template #title>
          <div class="card-title">
            <i class="pi pi-upload"></i>
            <span>1. Import New Course</span>
          </div>
        </template>
        <template #content>
          <p class="card-description">
            Upload a course.json file to create a new course. This defines the course metadata,
            name, language, and objectives.
          </p>

          <FileUpload
            mode="basic"
            accept="application/json"
            :auto="false"
            choose-label="Select course.json"
            @select="handleCourseFileSelect"
          />

          <div v-if="courseFile" class="file-info">
            <i class="pi pi-file"></i>
            <span>{{ courseFile.name }}</span>
          </div>

          <Button
            label="Import Course"
            icon="pi pi-cloud-upload"
            :loading="importingCourse"
            :disabled="!courseFile"
            @click="importCourse"
            class="import-button"
          />

          <!-- Course Import Result -->
          <div v-if="courseImportResult" class="import-result">
            <Divider />
            <h4>Import Complete</h4>
            <p>{{ courseImportResult.message }}</p>
            <div v-if="courseImportResult.courseId" class="result-detail">
              <Tag value="Course Created" severity="success" />
              <span>Course ID: {{ courseImportResult.courseId }}</span>
            </div>
          </div>
        </template>
      </Card>

      <!-- Import Module -->
      <Card class="import-card">
        <template #title>
          <div class="card-title">
            <i class="pi pi-folder-open"></i>
            <span>2. Import Module for Course</span>
          </div>
        </template>
        <template #content>
          <p class="card-description">
            Upload a module_X.json file to add episodes and exercises to an existing course.
          </p>

          <!-- Course Selection -->
          <div class="course-selection">
            <label>Select Course:</label>
            <DataTable
              :value="courses"
              selection-mode="single"
              v-model:selection="selectedCourse"
              @row-select="selectCourse($event.data)"
              striped-rows
              class="course-table"
            >
              <Column field="slug" header="Slug" />
              <Column field="name" header="Name" />
              <Column field="languageCode" header="Language" />
              <Column field="cefrLevel" header="Level">
                <template #body="slotProps">
                  <Tag :value="slotProps.data.cefrLevel" />
                </template>
              </Column>
            </DataTable>
          </div>

          <Divider />

          <!-- Module File Upload -->
          <div v-if="selectedCourse">
            <p class="selected-course">
              <strong>Selected Course:</strong> {{ selectedCourse.name }} ({{ selectedCourse.slug }})
            </p>

            <FileUpload
              mode="basic"
              accept="application/json"
              :auto="false"
              choose-label="Select module_X.json"
              @select="handleModuleFileSelect"
            />

            <div v-if="moduleFile" class="file-info">
              <i class="pi pi-file"></i>
              <span>{{ moduleFile.name }}</span>
            </div>

            <div class="audio-option">
              <Checkbox v-model="generateAudio" :binary="true" input-id="generateAudio" />
              <label for="generateAudio">Generate audio for dialogues and listening exercises</label>
            </div>

            <Button
              label="Import Module"
              icon="pi pi-cloud-upload"
              :loading="importingModule"
              :disabled="!moduleFile || importingModule"
              @click="importModule"
              class="import-button"
            />

            <!-- Import Progress -->
            <div v-if="importProgress" class="import-progress">
              <Divider />
              <h4>Import Progress</h4>

              <div class="progress-details">
                <div class="progress-bar-container">
                  <ProgressBar :value="importProgress.progressPercentage" />
                  <span class="progress-percentage">{{ importProgress.progressPercentage }}%</span>
                </div>

                <div class="progress-message">
                  <i class="pi pi-info-circle"></i>
                  <span>{{ importProgress.message }}</span>
                </div>

                <div v-if="importProgress.currentModule" class="current-item">
                  <strong>Module:</strong> {{ importProgress.currentModule }}
                </div>

                <div v-if="importProgress.currentEpisode" class="current-item">
                  <strong>Episode:</strong> {{ importProgress.currentEpisode }}
                </div>

                <div class="progress-stats">
                  <div class="progress-stat">
                    <Tag value="Modules" severity="info" />
                    <span>{{ importProgress.processedModules }} / {{ importProgress.totalModules }}</span>
                  </div>
                  <div class="progress-stat">
                    <Tag value="Episodes" severity="info" />
                    <span>{{ importProgress.processedEpisodes }} / {{ importProgress.totalEpisodes }}</span>
                  </div>
                  <div class="progress-stat">
                    <Tag value="Exercises" severity="info" />
                    <span>{{ importProgress.processedExercises }} / {{ importProgress.totalExercises }}</span>
                  </div>
                  <div v-if="importProgress.audioFilesGenerated > 0" class="progress-stat">
                    <Tag value="Audio" severity="success" />
                    <span>{{ importProgress.audioFilesGenerated }} files</span>
                  </div>
                </div>

                <div v-if="importProgress.errors && importProgress.errors.length > 0" class="progress-errors">
                  <Message severity="warn" :closable="false">
                    {{ importProgress.errors.length }} warning(s) occurred during import
                  </Message>
                </div>
              </div>
            </div>
          </div>
          <p v-else class="no-selection">Please select a course from the table above</p>
        </template>
      </Card>

      <!-- Existing Courses -->
      <Card class="courses-card">
        <template #title>
          <div class="card-title">
            <i class="pi pi-list"></i>
            <span>Existing Courses & Modules</span>
          </div>
        </template>
        <template #content>
          <div v-if="loading" class="loading-state">
            <ProgressBar mode="indeterminate" />
            <p>Loading courses...</p>
          </div>

          <DataTable
            v-else
            :value="courses"
            v-model:expandedRows="expandedRows"
            striped-rows
            @row-expand="onRowExpand"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-inbox"></i>
                <p>No courses yet. Import a course to get started!</p>
              </div>
            </template>
            <Column expander style="width: 3rem" />
            <Column field="id" header="ID" style="width: 5rem" />
            <Column field="slug" header="Slug" />
            <Column field="name" header="Name" />
            <Column field="languageCode" header="Language" style="width: 8rem" />
            <Column field="cefrLevel" header="Level" style="width: 6rem">
              <template #body="slotProps">
                <Tag :value="slotProps.data.cefrLevel" />
              </template>
            </Column>
            <Column field="estimatedHours" header="Hours" style="width: 6rem" />
            <Column field="isPublished" header="Published" style="width: 8rem">
              <template #body="slotProps">
                <Tag
                  :value="slotProps.data.isPublished ? 'Yes' : 'No'"
                  :severity="slotProps.data.isPublished ? 'success' : 'warning'"
                />
              </template>
            </Column>
            <template #expansion="slotProps">
              <div class="modules-section">
                <h4>Modules for {{ slotProps.data.name }}</h4>
                <div v-if="loadingModules.has(slotProps.data.id)" class="loading-modules">
                  <ProgressBar mode="indeterminate" />
                  <p>Loading modules...</p>
                </div>
                <div v-else-if="courseModules.get(slotProps.data.id)?.length === 0" class="no-modules">
                  <i class="pi pi-inbox"></i>
                  <p>No modules yet for this course</p>
                </div>
                <DataTable
                  v-else
                  :value="courseModules.get(slotProps.data.id)"
                  class="modules-table"
                >
                  <Column field="moduleNumber" header="Module #" style="width: 8rem" />
                  <Column field="title" header="Title" />
                  <Column field="episodeCount" header="Episodes" style="width: 8rem">
                    <template #body="moduleSlotProps">
                      <Tag :value="`${moduleSlotProps.data.episodeCount} episodes`" severity="info" />
                    </template>
                  </Column>
                  <Column header="Actions" style="width: 10rem">
                    <template #body="moduleSlotProps">
                      <Button
                        icon="pi pi-trash"
                        label="Delete"
                        severity="danger"
                        size="small"
                        text
                        @click="deleteModule(moduleSlotProps.data.id, moduleSlotProps.data.moduleNumber, slotProps.data.id)"
                      />
                    </template>
                  </Column>
                </DataTable>
              </div>
            </template>
          </DataTable>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
.course-admin-view {
  min-height: 100vh;
  background: var(--surface-ground);
  padding: 2rem 1rem;
}

.admin-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.header {
  margin-bottom: 1rem;
}

.header h1 {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--text-color);
  margin: 0 0 0.5rem 0;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.header h1 i {
  color: var(--primary-color);
}

.subtitle {
  font-size: 1.125rem;
  color: var(--text-color-secondary);
  margin: 0;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--text-color);
}

.card-title i {
  color: var(--primary-color);
}

.card-description {
  color: var(--text-color-secondary);
  line-height: 1.6;
  margin-bottom: 1.5rem;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--surface-100);
  border-radius: 6px;
  margin-top: 1rem;
}

.file-info i {
  color: var(--primary-color);
}

.import-button {
  margin-top: 1.5rem;
  width: 100%;
}

.import-result {
  margin-top: 1rem;
}

.import-result h4 {
  margin: 1rem 0 0.5rem 0;
  color: var(--text-color);
}

.result-detail {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 0.75rem;
}

.import-stats {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--text-color);
}

.stat-item i {
  color: var(--primary-color);
  font-size: 1.125rem;
}

.stat-item.warning {
  color: var(--orange-500);
}

.stat-item.warning i {
  color: var(--orange-500);
}

.course-selection {
  margin-bottom: 1.5rem;
}

.course-selection label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.75rem;
  color: var(--text-color);
}

.course-table {
  margin-bottom: 1rem;
}

.selected-course {
  padding: 0.75rem;
  background: var(--primary-50);
  border-left: 3px solid var(--primary-color);
  margin-bottom: 1.5rem;
  border-radius: 4px;
}

.audio-option {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding: 0.75rem;
  background: var(--surface-50);
  border-radius: 6px;
}

.audio-option label {
  font-weight: 500;
  color: var(--text-color);
  cursor: pointer;
}

.no-selection {
  color: var(--text-color-secondary);
  font-style: italic;
  text-align: center;
  padding: 2rem;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 2rem;
}

.empty-state {
  text-align: center;
  padding: 3rem 2rem;
}

.empty-state i {
  font-size: 4rem;
  color: var(--text-color-secondary);
  opacity: 0.5;
  margin-bottom: 1rem;
}

.empty-state p {
  color: var(--text-color-secondary);
  margin: 0;
}

.modules-section {
  padding: 1.5rem;
  background: var(--surface-50);
  border-radius: 8px;
  margin: 0.5rem 0;
}

.modules-section h4 {
  margin: 0 0 1rem 0;
  color: var(--text-color);
  font-size: 1.125rem;
  font-weight: 600;
}

.loading-modules {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 2rem;
}

.loading-modules p {
  color: var(--text-color-secondary);
  margin: 0;
}

.no-modules {
  text-align: center;
  padding: 2rem;
}

.no-modules i {
  font-size: 2.5rem;
  color: var(--text-color-secondary);
  opacity: 0.5;
  margin-bottom: 0.75rem;
}

.no-modules p {
  color: var(--text-color-secondary);
  margin: 0;
}

.modules-table {
  background: var(--surface-card);
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.import-progress {
  margin-top: 1rem;
}

.import-progress h4 {
  margin: 1rem 0 0.5rem 0;
  color: var(--text-color);
}

.progress-details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.progress-bar-container {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.progress-bar-container :deep(.p-progressbar) {
  flex: 1;
  height: 1.5rem;
}

.progress-percentage {
  font-weight: 600;
  color: var(--text-color);
  min-width: 3rem;
  text-align: right;
}

.progress-message {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: var(--surface-100);
  border-radius: 6px;
  color: var(--text-color);
}

.progress-message i {
  color: var(--primary-color);
}

.current-item {
  padding: 0.5rem;
  color: var(--text-color-secondary);
  font-size: 0.9rem;
}

.current-item strong {
  color: var(--text-color);
}

.progress-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  margin-top: 0.5rem;
}

.progress-stat {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--surface-50);
  border-radius: 6px;
  align-items: center;
}

.progress-stat span {
  font-weight: 600;
  color: var(--text-color);
}

.progress-errors {
  margin-top: 0.5rem;
}

@media (max-width: 768px) {
  .course-admin-view {
    padding: 1rem;
  }

  .header h1 {
    font-size: 2rem;
  }

  .import-stats {
    font-size: 0.9rem;
  }

  .progress-stats {
    grid-template-columns: 1fr;
  }
}
</style>
