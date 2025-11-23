<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import { CourseService, type CourseAdminDto, type ModuleAdminDto } from '@/services/CourseService'

const route = useRoute()
const router = useRouter()
const courseId = ref<number>(Number(route.params.id))
const course = ref<CourseAdminDto | null>(null)
const modules = ref<ModuleAdminDto[]>([])
const loading = ref(true)

onMounted(async () => {
  await loadCourseData()
})

async function loadCourseData() {
  loading.value = true
  try {
    // In a real app, we'd fetch the course details too. For now, we'll just fetch modules.
    // TODO: Add getCourse endpoint to CourseAdminController
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
  router.push({ name: 'module-wizard', params: { courseId: courseId.value, moduleId: moduleId } })
}

function getStatusSeverity(status: string) {
  switch (status) {
    case 'PUBLISHED': return 'success'
    case 'DRAFT': return 'warning'
    case 'PLANNED': return 'info'
    default: return 'info'
  }
}
</script>

<template>
  <div class="course-dashboard p-xl max-w-6xl mx-auto">
    <div class="header mb-xl flex justify-between items-center">
      <div>
        <h1 class="text-3xl font-bold mb-sm">Course Dashboard</h1>
        <p class="text-secondary">Manage modules and episodes for this course.</p>
      </div>
      <Button label="Back to Courses" icon="pi pi-arrow-left" text @click="router.push('/admin/courses')" />
    </div>

    <div class="modules-section">
      <h2 class="text-2xl font-bold mb-md">Modules</h2>
      <DataTable :value="modules" :loading="loading" stripedRows class="p-datatable-sm">
        <Column field="moduleNumber" header="#" sortable style="width: 5rem"></Column>
        <Column field="title" header="Title" sortable></Column>
        <Column field="episodeCount" header="Episodes" sortable style="width: 8rem"></Column>
        <Column header="Actions" style="width: 14rem">
          <template #body="slotProps">
            <Button icon="pi pi-sparkles" rounded text severity="success" v-tooltip="'Generate Content'" @click="navigateToWizard(slotProps.data.id)" class="mr-sm" />
            <Button icon="pi pi-pencil" rounded text severity="secondary" v-tooltip="'Edit'" @click="navigateToModule(slotProps.data.id)" />
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>
