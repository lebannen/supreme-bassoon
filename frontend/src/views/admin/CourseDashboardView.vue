<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import {type CourseAdminDto, CourseService, type ModuleAdminDto} from '@/services/CourseService'

const route = useRoute()
const router = useRouter()
const courseId = ref<number>(Number(route.params.id))
const course = ref<CourseAdminDto | null>(null)
const modules = ref<ModuleAdminDto[]>([])
const loading = ref(true)
const expandedRows = ref<any[]>([])
const moduleEpisodes = ref<Map<number, any[]>>(new Map())

onMounted(async () => {
  await loadCourseData()
})

async function loadCourseData() {
  loading.value = true
  try {
    course.value = await CourseService.getCourse(courseId.value)
    modules.value = await CourseService.getModulesForCourse(courseId.value)
  } catch (e) {
    console.error('Failed to load course data', e)
  } finally {
    loading.value = false
  }
}

function navigateToModule(moduleId: number) {
  router.push({ name: 'module-detail', params: { id: moduleId } })
}

function navigateToWizard(moduleId: number) {
  router.push({name: 'episode-wizard', params: {courseId: courseId.value, moduleId: moduleId}})
}

function navigateToEpisodeViewer(episodeId: number) {
  router.push({name: 'episode-viewer', params: {courseId: courseId.value, episodeId}})
}

async function onRowExpand(event: any) {
  const moduleId = event.data.id
  if (!moduleEpisodes.value.has(moduleId)) {
    await loadEpisodesForModule(moduleId)
  }
}

async function loadEpisodesForModule(moduleId: number) {
  try {
    const response = await fetch(`/api/modules/${moduleId}`)
    if (!response.ok) throw new Error('Failed to load episodes')
    const moduleData = await response.json()
    moduleEpisodes.value.set(moduleId, moduleData.episodes || [])
  } catch (e) {
    console.error('Failed to load episodes:', e)
    moduleEpisodes.value.set(moduleId, [])
  }
}

function getStatusSeverity(episodeCount: number) {
  if (episodeCount === 0) return 'info' // PLANNED
  if (episodeCount > 0) return 'success' // READY/PUBLISHED
  return 'secondary'
}

function getStatusLabel(episodeCount: number) {
  if (episodeCount === 0) return 'PLANNED'
  if (episodeCount > 0) return 'READY'
  return 'UNKNOWN'
}
</script>

<template>
  <div class="course-dashboard p-xl max-w-6xl mx-auto">
    <div class="header mb-xl">
      <div class="flex justify-between items-center mb-lg">
        <div>
          <h1 class="text-3xl font-bold mb-sm">{{ course?.name || 'Course Dashboard' }}</h1>
          <p class="text-secondary">Manage and generate content for course modules.</p>
        </div>
        <Button label="Back to Courses" icon="pi pi-arrow-left" text @click="router.push('/admin/courses')"/>
      </div>

      <Card v-if="course" class="bg-surface-card mb-xl">
        <template #title>Course Details</template>
        <template #content>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-lg">
            <div>
              <label class="block font-bold text-sm mb-xs text-secondary">Language & Level</label>
              <p class="m-0">{{ course.languageCode.toUpperCase() }} - {{ course.cefrLevel }}</p>
            </div>
            <div>
              <label class="block font-bold text-sm mb-xs text-secondary">Total Modules</label>
              <p class="m-0">{{ modules.length }}</p>
            </div>
            <div>
              <label class="block font-bold text-sm mb-xs text-secondary">Status</label>
              <Tag :value="course.isPublished ? 'PUBLISHED' : 'DRAFT'"
                   :severity="course.isPublished ? 'success' : 'warning'"/>
            </div>
          </div>

          <div v-if="course.seriesContext" class="mt-lg">
            <label class="block font-bold text-sm mb-xs text-secondary">Series Context</label>
            <p class="m-0 text-sm">{{
                course.seriesContext.substring(0, 300)
              }}{{ course.seriesContext.length > 300 ? '...' : '' }}</p>
          </div>

          <div class="mt-lg pt-lg border-t border-surface">
            <Button
                label="Generate All Course Content"
                icon="pi pi-sparkles"
                severity="success"
                size="large"
                @click="router.push({ name: 'course-import', params: { courseId: courseId } })"
            />
            <p class="text-sm text-secondary mt-sm">Generate and validate content for all episodes at once, then assign
              voices to characters.</p>
          </div>
        </template>
      </Card>
    </div>

    <div class="modules-section">
      <div class="flex justify-between items-center mb-md">
        <h2 class="text-2xl font-bold">Modules</h2>
      </div>

      <DataTable
          :value="modules"
          :loading="loading"
          v-model:expandedRows="expandedRows"
          @row-expand="onRowExpand"
          stripedRows
          class="p-datatable-sm"
      >
        <Column expander style="width: 3rem"/>
        <Column field="moduleNumber" header="#" sortable style="width: 5rem"></Column>
        <Column field="title" header="Title" sortable></Column>
        <Column field="episodeCount" header="Episodes" sortable style="width: 8rem">
          <template #body="slotProps">
            {{ slotProps.data.episodeCount }}
          </template>
        </Column>
        <Column header="Status" style="width: 10rem">
          <template #body="slotProps">
            <Tag
                :value="getStatusLabel(slotProps.data.episodeCount)"
                :severity="getStatusSeverity(slotProps.data.episodeCount)"
            />
          </template>
        </Column>
        <Column header="Actions" style="width: 14rem">
          <template #body="slotProps">
            <Button
                v-if="slotProps.data.episodeCount === 0"
                icon="pi pi-sparkles"
                rounded
                text
                severity="success"
                v-tooltip="'Generate Episodes'"
                @click="navigateToWizard(slotProps.data.id)"
                class="mr-sm"
            />
            <Button
                v-else
                icon="pi pi-eye"
                rounded
                text
                severity="info"
                v-tooltip="'View Module'"
                @click="navigateToModule(slotProps.data.id)"
                class="mr-sm"
            />
            <Button
                icon="pi pi-pencil"
                rounded
                text
                severity="secondary"
                v-tooltip="'Edit'"
                @click="navigateToModule(slotProps.data.id)"
            />
          </template>
        </Column>
        <template #expansion="slotProps">
          <div class="p-md bg-surface-ground">
            <h4 class="font-bold mb-md">Episodes for {{ slotProps.data.title }}</h4>
            <DataTable
                :value="moduleEpisodes.get(slotProps.data.id) || []"
                class="p-datatable-sm"
            >
              <template #empty>
                <div class="text-center p-md text-secondary">No episodes found</div>
              </template>
              <Column field="episodeNumber" header="#" style="width: 4rem"></Column>
              <Column field="title" header="Title"></Column>
              <Column field="type" header="Type" style="width: 8rem">
                <template #body="episode">
                  <Tag :value="episode.data.type" severity="info"/>
                </template>
              </Column>
              <Column header="Actions" style="width: 8rem">
                <template #body="episode">
                  <Button
                      icon="pi pi-eye"
                      rounded
                      text
                      severity="primary"
                      v-tooltip.top="'View Content'"
                      @click="navigateToEpisodeViewer(episode.data.id)"
                  />
                </template>
              </Column>
            </DataTable>
          </div>
        </template>
      </DataTable>
    </div>
  </div>
</template>
