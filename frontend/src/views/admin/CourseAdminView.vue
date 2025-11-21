<script setup lang="ts">
import {onMounted, ref} from 'vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import FileUpload from 'primevue/fileupload'
import Message from 'primevue/message'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Checkbox from 'primevue/checkbox'
import ProgressBar from 'primevue/progressbar'
import Divider from 'primevue/divider'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

interface Course {
  id: number;
  slug: string;
  name: string;
  languageCode: string;
  cefrLevel: string;
  isPublished: boolean
}

interface Module {
  id: number;
  moduleNumber: number;
  title: string;
  episodeCount: number
}

interface ImportProgress {
  importId: string;
  courseSlug: string;
  courseName: string;
  status: string;
  totalModules: number;
  processedModules: number;
  totalEpisodes: number;
  processedEpisodes: number;
  totalExercises: number;
  processedExercises: number;
  audioFilesGenerated: number;
  currentModule: string | null;
  currentEpisode: string | null;
  message: string;
  progressPercentage: number;
  errors: string[];
  error: string | null
}

const courses = ref<Course[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const courseFile = ref<File | null>(null)
const importingCourse = ref(false)
const selectedCourse = ref<Course | null>(null)
const moduleFile = ref<File | null>(null)
const generateAudio = ref(true)
const importingModule = ref(false)
const importProgress = ref<ImportProgress | null>(null)
const courseModules = ref<Map<number, Module[]>>(new Map())
const loadingModules = ref<Set<number>>(new Set())
const expandedRows = ref<Course[]>([])
const allImports = ref<ImportProgress[]>([])
const loadingImports = ref(false)

const getAuthHeaders = () => ({
  Authorization: `Bearer ${localStorage.getItem('auth_token')}`,
  'Content-Type': 'application/json'
})

async function loadCourses() {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE}/api/admin/courses`, {headers: getAuthHeaders()})
    if (!response.ok) throw new Error(`Failed to load courses (${response.status})`)
    courses.value = await response.json()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load courses'
  } finally {
    loading.value = false
  }
}

async function importCourseAction() {
  if (!courseFile.value) return
  importingCourse.value = true
  try {
    const courseData = JSON.parse(await courseFile.value.text())
    const response = await fetch(`${API_BASE}/api/admin/courses/import`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(courseData)
    })
    if (!response.ok) throw new Error((await response.json()).message || 'Failed to import course')
    successMessage.value = `Course "${courseData.name}" imported successfully!`
    await loadCourses()
    courseFile.value = null
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to import course'
  } finally {
    importingCourse.value = false
  }
}

function streamImportProgress(importId: string) {
  const url = new URL(`${API_BASE}/api/admin/courses/import/progress/${importId}`)
  url.searchParams.append('token', localStorage.getItem('auth_token') || '')
  const eventSource = new EventSource(url.toString())
  eventSource.onmessage = (event) => {
    const data = JSON.parse(event.data)
    importProgress.value = data
    if (['COMPLETED', 'FAILED'].includes(data.status)) {
      importingModule.value = false
      if (data.status === 'COMPLETED') successMessage.value = 'Module import completed!'
      if (data.errors?.length) error.value = `Import completed with ${data.errors.length} warnings.`
      eventSource.close()
      loadAllImports()
    }
  }
  eventSource.onerror = () => {
    error.value = 'Lost connection to import progress stream.'
    importingModule.value = false
    eventSource.close()
  }
}

async function importModuleAction() {
  if (!selectedCourse.value || !moduleFile.value) return
  importingModule.value = true
  importProgress.value = null
  try {
    const moduleData = JSON.parse(await moduleFile.value.text())
    const response = await fetch(`${API_BASE}/api/admin/courses/${selectedCourse.value.slug}/modules/import-async?generateAudio=${generateAudio.value}`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(moduleData)
    })
    if (!response.ok) throw new Error((await response.json()).message || 'Failed to start import')
    const result = await response.json()
    if (result.importId) streamImportProgress(result.importId)
    else throw new Error('No import ID received')
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to import module'
    importingModule.value = false
  }
}

async function onRowExpand(event: any) {
  const courseId = event.data.id
  if (!courseModules.value.has(courseId)) {
    await loadModulesForCourse(courseId)
  }
}

async function loadModulesForCourse(courseId: number) {
  loadingModules.value.add(courseId)
  try {
    const response = await fetch(`${API_BASE}/api/admin/courses/${courseId}/modules`, {headers: getAuthHeaders()})
    if (!response.ok) throw new Error(`Failed to load modules (${response.status})`)
    courseModules.value.set(courseId, await response.json())
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load modules'
  } finally {
    loadingModules.value.delete(courseId)
  }
}

async function deleteModule(courseId: number, moduleId: number) {
  if (!confirm('Are you sure you want to delete this module? This will also delete all episodes and audio files.')) {
    return
  }

  try {
    const response = await fetch(`${API_BASE}/api/admin/courses/modules/${moduleId}`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    })
    if (!response.ok) throw new Error(`Failed to delete module (${response.status})`)

    successMessage.value = 'Module deleted successfully!'
    await loadModulesForCourse(courseId)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to delete module'
  }
}

async function loadAllImports() {
  loadingImports.value = true
  try {
    const response = await fetch(`${API_BASE}/api/admin/courses/import/progress`, {headers: getAuthHeaders()})
    if (!response.ok) throw new Error(`Failed to load import progress (${response.status})`)
    allImports.value = await response.json()
  } catch (err) {
    console.error('Failed to load import progress:', err)
  } finally {
    loadingImports.value = false
  }
}

const getStatusSeverity = (status: string) => {
  const map: Record<string, string> = {
    COMPLETED: 'success',
    PROCESSING: 'info',
    QUEUED: 'secondary',
    FAILED: 'danger'
  }
  return map[status] || 'secondary'
}

onMounted(() => {
  loadCourses()
  loadAllImports()
})
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <h1 class="flex items-center gap-md"><i class="pi pi-cog icon-primary"></i> Course Administration</h1>
      <p class="text-secondary">Import and manage course content.</p>
    </div>

    <Message v-if="error" severity="error" @close="error = null">{{ error }}</Message>
    <Message v-if="successMessage" severity="success" @close="successMessage = null">{{ successMessage }}</Message>

    <Card>
      <template #title>
        <div class="card-title-icon"><i class="pi pi-upload"></i><span>1. Import New Course</span></div>
      </template>
      <template #content>
        <p class="text-secondary mb-lg">Upload a course.json file to create a new course.</p>
        <div class="flex flex-col gap-md">
          <FileUpload mode="basic" accept="application/json" :auto="false" choose-label="Select course.json"
                      @select="courseFile = $event.files[0]"/>
          <Button label="Import Course" icon="pi pi-cloud-upload" :loading="importingCourse" :disabled="!courseFile"
                  @click="importCourseAction"/>
        </div>
      </template>
    </Card>

    <Card>
      <template #title>
        <div class="card-title-icon"><i class="pi pi-folder-open"></i><span>2. Import Module for Course</span></div>
      </template>
      <template #content>
        <p class="text-secondary mb-lg">Select a course, then upload a module_X.json file.</p>
        <DataTable :value="courses" selection-mode="single" v-model:selection="selectedCourse" stripedRows
                   responsiveLayout="scroll">
          <Column field="name" header="Name"/>
          <Column field="languageCode" header="Lang"/>
          <Column field="cefrLevel" header="Level"/>
        </DataTable>
        <Divider class="my-lg"/>
        <div v-if="selectedCourse" class="content-area">
          <p class="font-bold">Selected Course: {{ selectedCourse.name }}</p>
          <FileUpload mode="basic" accept="application/json" :auto="false" choose-label="Select module_X.json"
                      @select="moduleFile = $event.files[0]"/>
          <div class="flex items-center gap-sm">
            <Checkbox v-model="generateAudio" :binary="true" input-id="generateAudio"/>
            <label for="generateAudio">Generate audio for dialogues</label>
          </div>
          <Button label="Import Module" icon="pi pi-cloud-upload" :loading="importingModule"
                  :disabled="!moduleFile || importingModule" @click="importModuleAction"/>
          <div v-if="importProgress" class="content-area-lg">
            <Divider/>
            <h4 class="font-bold">Import Progress</h4>
            <ProgressBar :value="importProgress.progressPercentage"/>
            <p class="text-secondary text-center">{{ importProgress.message }}</p>
            <Message v-if="importProgress.errors?.length" severity="warn">{{ importProgress.errors.length }} warnings
              occurred.
            </Message>
          </div>
        </div>
        <p v-else class="text-secondary text-center">Please select a course from the table above.</p>
      </template>
    </Card>

    <Card>
      <template #title>
        <div class="card-title-icon"><i class="pi pi-list"></i><span>Existing Courses & Modules</span></div>
      </template>
      <template #content>
        <DataTable :value="courses" v-model:expandedRows="expandedRows" @row-expand="onRowExpand" stripedRows
                   responsiveLayout="scroll" :loading="loading">
          <template #empty>
            <div class="text-center p-xl">No courses found.</div>
          </template>
          <Column expander style="width: 3rem"/>
          <Column field="name" header="Name"/>
          <Column field="languageCode" header="Lang"/>
          <Column field="cefrLevel" header="Level">
            <template #body="{data}">
              <Tag :value="data.cefrLevel"/>
            </template>
          </Column>
          <Column field="isPublished" header="Published">
            <template #body="{data}">
              <Tag :value="data.isPublished ? 'Yes' : 'No'" :severity="data.isPublished ? 'success' : 'warning'"/>
            </template>
          </Column>
          <template #expansion="{ data }">
            <div class="p-md bg-surface-ground">
              <h4 class="font-bold mb-md">Modules for {{ data.name }}</h4>
              <DataTable :value="courseModules.get(data.id)" :loading="loadingModules.has(data.id)">
                <template #empty>
                  <div class="text-center p-md">No modules for this course.</div>
                </template>
                <Column field="moduleNumber" header="Module #"/>
                <Column field="title" header="Title"/>
                <Column field="episodeCount" header="Episodes">
                  <template #body="m">
                    <Tag :value="`${m.data.episodeCount} episodes`"/>
                  </template>
                </Column>
                <Column header="Actions" style="width: 8rem">
                  <template #body="m">
                    <Button icon="pi pi-trash" severity="danger" text rounded
                            @click="deleteModule(data.id, m.data.id)"
                            v-tooltip.top="'Delete module'"/>
                  </template>
                </Column>
              </DataTable>
            </div>
          </template>
        </DataTable>
      </template>
    </Card>

    <Card v-if="allImports.length > 0">
      <template #title>
        <div class="card-title-icon"><i class="pi pi-history"></i><span>Import History</span></div>
      </template>
      <template #content>
        <DataTable :value="allImports" :loading="loadingImports" stripedRows responsiveLayout="scroll"
                   :paginator="true" :rows="10">
          <template #empty>
            <div class="text-center p-md">No import history.</div>
          </template>
          <Column field="courseName" header="Course"/>
          <Column field="status" header="Status">
            <template #body="{data}">
              <Tag :value="data.status" :severity="getStatusSeverity(data.status)"/>
            </template>
          </Column>
          <Column field="processedModules" header="Modules">
            <template #body="{data}">
              {{ data.processedModules }}/{{ data.totalModules }}
            </template>
          </Column>
          <Column field="processedEpisodes" header="Episodes">
            <template #body="{data}">
              {{ data.processedEpisodes }}/{{ data.totalEpisodes }}
            </template>
          </Column>
          <Column field="processedExercises" header="Exercises">
            <template #body="{data}">
              {{ data.processedExercises }}/{{ data.totalExercises }}
            </template>
          </Column>
          <Column field="audioFilesGenerated" header="Audio Files"/>
          <Column field="progressPercentage" header="Progress">
            <template #body="{data}">
              <ProgressBar :value="data.progressPercentage" :showValue="false" style="height: 0.5rem"/>
              <small class="text-secondary">{{ data.progressPercentage }}%</small>
            </template>
          </Column>
          <Column field="message" header="Status Message">
            <template #body="{data}">
              <small class="text-secondary">{{ data.message }}</small>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>
