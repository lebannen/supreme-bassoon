<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import Dialog from 'primevue/dialog'
import {type CourseAdminDto, type CourseReviewDto, CourseService, type ModuleAdminDto} from '@/services/CourseService'

const route = useRoute()
const router = useRouter()
const courseId = ref<number>(Number(route.params.id))
const course = ref<CourseAdminDto | null>(null)
const modules = ref<ModuleAdminDto[]>([])
const loading = ref(true)
const expandedRows = ref<any[]>([])
const moduleEpisodes = ref<Map<number, any[]>>(new Map())

// Review state
const latestReview = ref<CourseReviewDto | null>(null)
const reviewLoading = ref(false)
const reviewError = ref<string | null>(null)
const showReviewDialog = ref(false)

onMounted(async () => {
  await loadCourseData()
  await loadLatestReview()
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

async function exportCourse() {
  try {
    const response = await fetch(`/api/admin/courses/${courseId.value}/export`)
    if (!response.ok) throw new Error('Export failed')

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${course.value?.slug || 'course'}-export.json`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
  } catch (e) {
    console.error('Failed to export course:', e)
  }
}

async function loadLatestReview() {
  try {
    latestReview.value = await CourseService.getLatestReview(courseId.value)
  } catch (e) {
    console.error('Failed to load latest review:', e)
  }
}

async function startReview() {
  reviewLoading.value = true
  reviewError.value = null

  try {
    latestReview.value = await CourseService.createReview(courseId.value)
    showReviewDialog.value = true
  } catch (e) {
    console.error('Failed to review course:', e)
    reviewError.value = 'Failed to generate review. Please try again.'
  } finally {
    reviewLoading.value = false
  }
}

function getScoreColor(score: number | null): string {
  if (score === null) return 'var(--text-color-secondary)'
  if (score >= 80) return 'var(--green-500)'
  if (score >= 60) return 'var(--yellow-500)'
  return 'var(--red-500)'
}

function getScoreSeverity(score: number | null): 'success' | 'warn' | 'danger' | 'secondary' {
  if (score === null) return 'secondary'
  if (score >= 80) return 'success'
  if (score >= 60) return 'warn'
  return 'danger'
}
</script>

<template>
  <div class="course-dashboard p-5 max-w-6xl mx-auto">
    <div class="header mb-5">
      <div class="flex justify-content-between align-items-center mb-4">
        <div>
          <h1 class="text-3xl font-bold mb-2">{{ course?.name || 'Course Dashboard' }}</h1>
          <p class="text-secondary">Manage and generate content for course modules.</p>
        </div>
        <Button label="Back to Courses" icon="pi pi-arrow-left" text @click="router.push('/admin/courses')"/>
      </div>

      <Card v-if="course" class="bg-surface-card mb-5">
        <template #title>Course Details</template>
        <template #content>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label class="block font-bold text-sm mb-1 text-secondary">Language & Level</label>
              <p class="m-0">{{ course.languageCode.toUpperCase() }} - {{ course.cefrLevel }}</p>
            </div>
            <div>
              <label class="block font-bold text-sm mb-1 text-secondary">Total Modules</label>
              <p class="m-0">{{ modules.length }}</p>
            </div>
            <div>
              <label class="block font-bold text-sm mb-1 text-secondary">Status</label>
              <Tag :value="course.isPublished ? 'PUBLISHED' : 'DRAFT'"
                   :severity="course.isPublished ? 'success' : 'warning'"/>
            </div>
          </div>

          <div v-if="course.seriesContext" class="mt-lg">
            <label class="block font-bold text-sm mb-1 text-secondary">Series Context</label>
            <p class="m-0 text-sm">{{
                course.seriesContext.substring(0, 300)
              }}{{ course.seriesContext.length > 300 ? '...' : '' }}</p>
          </div>

          <div class="mt-lg pt-lg border-t border-surface flex gap-3 align-items-start flex-wrap">
            <div>
              <Button
                  label="Generate All Course Content"
                  icon="pi pi-sparkles"
                  severity="success"
                  size="large"
                  @click="router.push({ name: 'course-import', params: { courseId: courseId } })"
              />
              <p class="text-sm text-secondary mt-sm">Generate and validate content for all episodes at once, then
                assign
                voices to characters.</p>
            </div>
            <div>
              <Button
                  label="Export Course"
                  icon="pi pi-download"
                  severity="secondary"
                  size="large"
                  @click="exportCourse"
              />
              <p class="text-sm text-secondary mt-sm">Download course as JSON for backup.</p>
            </div>
            <div>
              <Button
                  :label="reviewLoading ? 'Reviewing...' : 'AI Review'"
                  icon="pi pi-star"
                  severity="info"
                  size="large"
                  :loading="reviewLoading"
                  @click="startReview"
              />
              <p class="text-sm text-secondary mt-sm">Get AI feedback on course quality.</p>
            </div>
          </div>

          <!-- Latest Review Summary -->
          <div v-if="latestReview" class="mt-lg pt-lg border-t border-surface">
            <div class="flex justify-content-between align-items-center mb-3">
              <h3 class="m-0">Latest AI Review</h3>
              <Button label="View Details" icon="pi pi-eye" text size="small" @click="showReviewDialog = true"/>
            </div>

            <div class="grid grid-cols-4 gap-3 mb-3">
              <div class="text-center p-3 bg-surface-ground border-round">
                <div class="text-2xl font-bold" :style="{ color: getScoreColor(latestReview.overallScore) }">
                  {{ latestReview.overallScore ?? '-' }}
                </div>
                <div class="text-sm text-secondary">Overall</div>
              </div>
              <div class="text-center p-3 bg-surface-ground border-round">
                <div class="text-2xl font-bold" :style="{ color: getScoreColor(latestReview.cefrAlignmentScore) }">
                  {{ latestReview.cefrAlignmentScore ?? '-' }}
                </div>
                <div class="text-sm text-secondary">CEFR Alignment</div>
              </div>
              <div class="text-center p-3 bg-surface-ground border-round">
                <div class="text-2xl font-bold" :style="{ color: getScoreColor(latestReview.structureScore) }">
                  {{ latestReview.structureScore ?? '-' }}
                </div>
                <div class="text-sm text-secondary">Structure</div>
              </div>
              <div class="text-center p-3 bg-surface-ground border-round">
                <div class="text-2xl font-bold" :style="{ color: getScoreColor(latestReview.contentQualityScore) }">
                  {{ latestReview.contentQualityScore ?? '-' }}
                </div>
                <div class="text-sm text-secondary">Content Quality</div>
              </div>
            </div>

            <p class="m-0 text-secondary">{{ latestReview.summary }}</p>
          </div>

          <Message v-if="reviewError" severity="error" class="mt-3" :closable="false">{{ reviewError }}</Message>
        </template>
      </Card>
    </div>

    <div class="modules-section">
      <div class="flex justify-content-between align-items-center mb-3">
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
            <h4 class="font-bold mb-3">Episodes for {{ slotProps.data.title }}</h4>
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

    <!-- Review Details Dialog -->
    <Dialog
        v-model:visible="showReviewDialog"
        header="AI Course Review"
        :style="{ width: '800px' }"
        :modal="true"
        :dismissableMask="true"
    >
      <div v-if="latestReview" class="review-dialog-content">
        <!-- Scores -->
        <div class="grid grid-cols-4 gap-3 mb-4">
          <div class="text-center p-3 bg-surface-ground border-round">
            <div class="text-3xl font-bold" :style="{ color: getScoreColor(latestReview.overallScore) }">
              {{ latestReview.overallScore ?? '-' }}
            </div>
            <div class="text-sm text-secondary">Overall</div>
          </div>
          <div class="text-center p-3 bg-surface-ground border-round">
            <div class="text-3xl font-bold" :style="{ color: getScoreColor(latestReview.cefrAlignmentScore) }">
              {{ latestReview.cefrAlignmentScore ?? '-' }}
            </div>
            <div class="text-sm text-secondary">CEFR</div>
          </div>
          <div class="text-center p-3 bg-surface-ground border-round">
            <div class="text-3xl font-bold" :style="{ color: getScoreColor(latestReview.structureScore) }">
              {{ latestReview.structureScore ?? '-' }}
            </div>
            <div class="text-sm text-secondary">Structure</div>
          </div>
          <div class="text-center p-3 bg-surface-ground border-round">
            <div class="text-3xl font-bold" :style="{ color: getScoreColor(latestReview.contentQualityScore) }">
              {{ latestReview.contentQualityScore ?? '-' }}
            </div>
            <div class="text-sm text-secondary">Content</div>
          </div>
        </div>

        <!-- Summary -->
        <div class="mb-4">
          <h4 class="mt-0 mb-2">Summary</h4>
          <p class="m-0">{{ latestReview.summary }}</p>
        </div>

        <!-- Strengths -->
        <div v-if="latestReview.strengths?.length" class="mb-4">
          <h4 class="mt-0 mb-2 text-green-600">Strengths</h4>
          <ul class="m-0 pl-4">
            <li v-for="(strength, idx) in latestReview.strengths" :key="idx" class="mb-1">{{ strength }}</li>
          </ul>
        </div>

        <!-- Weaknesses -->
        <div v-if="latestReview.weaknesses?.length" class="mb-4">
          <h4 class="mt-0 mb-2 text-orange-600">Areas for Improvement</h4>
          <ul class="m-0 pl-4">
            <li v-for="(weakness, idx) in latestReview.weaknesses" :key="idx" class="mb-1">{{ weakness }}</li>
          </ul>
        </div>

        <!-- Recommendations -->
        <div v-if="latestReview.recommendations?.length" class="mb-4">
          <h4 class="mt-0 mb-2 text-blue-600">Recommendations</h4>
          <ul class="m-0 pl-4">
            <li v-for="(rec, idx) in latestReview.recommendations" :key="idx" class="mb-1">{{ rec }}</li>
          </ul>
        </div>

        <!-- Module Feedback -->
        <div v-if="latestReview.moduleFeedback?.length" class="mb-4">
          <h4 class="mt-0 mb-2">Module Feedback</h4>
          <div v-for="mf in latestReview.moduleFeedback" :key="mf.moduleNumber"
               class="mb-2 p-2 bg-surface-ground border-round">
            <div class="flex justify-content-between align-items-center mb-1">
              <strong>Module {{ mf.moduleNumber }}</strong>
              <Tag v-if="mf.score" :value="mf.score + '/100'" :severity="getScoreSeverity(mf.score)"/>
            </div>
            <p class="m-0 text-sm">{{ mf.feedback }}</p>
          </div>
        </div>

        <!-- Detailed Analysis -->
        <div v-if="latestReview.detailedAnalysis" class="mb-4">
          <h4 class="mt-0 mb-2">Detailed Analysis</h4>
          <div class="p-3 bg-surface-ground border-round text-sm" style="white-space: pre-wrap;">
            {{ latestReview.detailedAnalysis }}
          </div>
        </div>

        <div class="text-sm text-secondary text-right">
          Reviewed: {{ new Date(latestReview.createdAt).toLocaleString() }}
        </div>
      </div>
    </Dialog>
  </div>
</template>
