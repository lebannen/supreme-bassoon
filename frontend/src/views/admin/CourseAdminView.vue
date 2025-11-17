<script setup lang="ts">
import {onMounted, ref} from 'vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import FileUpload, {type FileUploadSelectEvent} from 'primevue/fileupload'
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
      'Content-Type': 'application/json',
    }
  }
  return {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
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
      headers: getAuthHeaders(),
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
      body: JSON.stringify(courseData),
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
    if (
        importingModule.value &&
        (!importProgress.value ||
            (importProgress.value.status !== 'COMPLETED' && importProgress.value.status !== 'FAILED'))
    ) {
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
        body: JSON.stringify(moduleData),
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
      headers: getAuthHeaders(),
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
  if (
      !confirm(
          `Are you sure you want to delete Module ${moduleNumber}? This will also delete all its episodes and exercises.`
      )
  ) {
    return
  }

  try {
    const response = await fetch(`${API_BASE}/api/admin/courses/modules/${moduleId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
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
  <div class="page-container-with-padding">
    <div class="view-container content-area-lg flex flex-col gap-xl">
      <div class="page-header">
        <h1 class="flex items-center gap-md">
          <i class="pi pi-cog icon-primary"></i>
          Course Administration
        </h1>
        <p>Import and manage course content</p>
      </div>

      <!-- Global Messages -->
      <Message v-if="error" severity="error" :closable="true" @close="error = null" class="mb-lg">
        {{ error }}
      </Message>

      <Message
          v-if="successMessage"
          severity="success"
          :closable="true"
          @close="successMessage = null"
          class="mb-lg"
      >
        {{ successMessage }}
      </Message>

      <!-- Import Course -->
      <Card>
        <template #title>
          <div class="card-title-icon">
            <i class="pi pi-upload"></i>
            <span>1. Import New Course</span>
          </div>
        </template>
        <template #content>
          <p class="text-secondary mb-lg">
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

          <div v-if="courseFile" class="file-info flex items-center gap-sm">
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
      <Card>
        <template #title>
          <div class="card-title-icon">
            <i class="pi pi-folder-open"></i>
            <span>2. Import Module for Course</span>
          </div>
        </template>
        <template #content>
          <p class="text-secondary mb-lg">
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
              <strong>Selected Course:</strong> {{ selectedCourse.name }} ({{
                selectedCourse.slug
              }})
            </p>

            <FileUpload
              mode="basic"
              accept="application/json"
              :auto="false"
              choose-label="Select module_X.json"
              @select="handleModuleFileSelect"
            />

            <div v-if="moduleFile" class="file-info flex items-center gap-sm">
              <i class="pi pi-file"></i>
              <span>{{ moduleFile.name }}</span>
            </div>

            <div class="audio-option">
              <Checkbox v-model="generateAudio" :binary="true" input-id="generateAudio" />
              <label for="generateAudio"
              >Generate audio for dialogues and listening exercises</label
              >
            </div>

            <Button
              label="Import Module"
              icon="pi pi-cloud-upload"
              :loading="importingModule"
              :disabled="!moduleFile || importingModule"
              @click="importModule"
              class="import-button-full"
            />

            <!-- Import Progress -->
            <div v-if="importProgress" class="import-progress-section">
              <Divider />
              <h4 class="import-progress-title">Import Progress</h4>

              <div class="flex-col gap-md">
                <div class="progress-bar-container flex items-center gap-md">
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

                <div class="stats-grid">
                  <div class="flex-col items-center gap-sm p-lg">
                    <Tag value="Modules" severity="info" />
                    <span class="font-semibold"
                    >{{ importProgress.processedModules }} /
                      {{ importProgress.totalModules }}</span
                    >
                  </div>
                  <div class="flex-col items-center gap-sm p-lg">
                    <Tag value="Episodes" severity="info" />
                    <span class="font-semibold"
                    >{{ importProgress.processedEpisodes }} /
                      {{ importProgress.totalEpisodes }}</span
                    >
                  </div>
                  <div class="flex-col items-center gap-sm p-lg">
                    <Tag value="Exercises" severity="info" />
                    <span class="font-semibold"
                    >{{ importProgress.processedExercises }} /
                      {{ importProgress.totalExercises }}</span
                    >
                  </div>
                  <div
                      v-if="importProgress.audioFilesGenerated > 0"
                      class="flex-col items-center gap-sm p-lg"
                  >
                    <Tag value="Audio" severity="success" />
                    <span class="font-semibold"
                    >{{ importProgress.audioFilesGenerated }} files</span
                    >
                  </div>
                </div>

                <div
                    v-if="importProgress.errors && importProgress.errors.length > 0"
                    class="progress-errors"
                >
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
      <Card>
        <template #title>
          <div class="card-title-icon">
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
            <Column expander class="col-expander"/>
            <Column field="id" header="ID" class="col-id"/>
            <Column field="slug" header="Slug" />
            <Column field="name" header="Name" />
            <Column field="languageCode" header="Language" class="col-language"/>
            <Column field="cefrLevel" header="Level" class="col-level">
              <template #body="slotProps">
                <Tag :value="slotProps.data.cefrLevel" />
              </template>
            </Column>
            <Column field="estimatedHours" header="Hours" class="col-hours"/>
            <Column field="isPublished" header="Published" class="col-published">
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
                <div v-if="loadingModules.has(slotProps.data.id)" class="loading-state">
                  <ProgressBar mode="indeterminate" />
                  <p>Loading modules...</p>
                </div>
                <div
                    v-else-if="courseModules.get(slotProps.data.id)?.length === 0"
                    class="no-modules"
                >
                  <i class="pi pi-inbox"></i>
                  <p>No modules yet for this course</p>
                </div>
                <DataTable
                  v-else
                  :value="courseModules.get(slotProps.data.id)"
                  class="modules-table"
                >
                  <Column field="moduleNumber" header="Module #" class="col-module-number"/>
                  <Column field="title" header="Title" />
                  <Column field="episodeCount" header="Episodes" class="col-episodes">
                    <template #body="moduleSlotProps">
                      <Tag
                          :value="`${moduleSlotProps.data.episodeCount} episodes`"
                          severity="info"
                      />
                    </template>
                  </Column>
                  <Column header="Actions" class="col-actions">
                    <template #body="moduleSlotProps">
                      <Button
                        icon="pi pi-trash"
                        label="Delete"
                        severity="danger"
                        size="small"
                        text
                        @click="
                          deleteModule(
                            moduleSlotProps.data.id,
                            moduleSlotProps.data.moduleNumber,
                            slotProps.data.id
                          )
                        "
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
.course-selection label {
  display: block;
  font-weight: 600;
  margin-bottom: var(--spacing-sm);
  color: var(--text-primary);
}

.selected-course {
  padding: var(--spacing-sm);
  background: var(--primary-light);
  border-left: 3px solid var(--primary);
  margin-bottom: var(--spacing-lg);
  border-radius: var(--radius-sm);
}

.audio-option {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-lg);
  padding: var(--spacing-sm);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.audio-option label {
  font-weight: 500;
  color: var(--text-primary);
  cursor: pointer;
}

.modules-section {
  padding: var(--spacing-lg);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  margin: var(--spacing-sm) 0;
}

.modules-section h4 {
  margin: 0 0 var(--spacing-md) 0;
  color: var(--text-primary);
  font-size: 1.125rem;
  font-weight: 600;
}

.modules-table {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.progress-bar-container :deep(.p-progressbar) {
  flex: 1;
  height: 1.5rem;
}

.progress-percentage {
  font-weight: 600;
  color: var(--text-primary);
  min-width: 3rem;
  text-align: right;
}

.progress-message {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm);
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  color: var(--text-primary);
}

.progress-message i {
  color: var(--primary);
}

.current-item {
  padding: var(--spacing-sm);
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.current-item strong {
  color: var(--text-primary);
}

.icon-primary {
  color: var(--primary);
}

.import-button-full {
  margin-top: var(--spacing-lg);
  width: 100%;
}

.import-progress-section {
  margin-top: var(--spacing-md);
}

.import-progress-title {
  margin: var(--spacing-md) 0 var(--spacing-sm) 0;
  color: var(--text-primary);
}

/* Column widths */
.col-expander {
  width: 3rem;
}

.col-id {
  width: 5rem;
}

.col-language {
  width: 8rem;
}

.col-level {
  width: 6rem;
}

.col-hours {
  width: 6rem;
}

.col-published {
  width: 8rem;
}

.col-module-number {
  width: 8rem;
}

.col-episodes {
  width: 8rem;
}

.col-actions {
  width: 10rem;
}

@media (max-width: 768px) {
  .course-admin-view {
    padding: var(--spacing-md);
  }
}
</style>
