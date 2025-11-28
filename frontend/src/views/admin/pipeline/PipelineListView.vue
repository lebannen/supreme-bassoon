<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useToast} from 'primevue/usetoast'
import {useConfirm} from 'primevue/useconfirm'
import Card from 'primevue/card'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import Dropdown from 'primevue/dropdown'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Checkbox from 'primevue/checkbox'
import ProgressSpinner from 'primevue/progressspinner'
import PipelineService from '@/services/PipelineService'
import type {GenerationStage, GenerationSummaryDto, StartGenerationRequest} from '@/types/pipeline'
import {CEFR_LEVELS, LANGUAGE_OPTIONS, STAGE_CONFIG} from '@/types/pipeline'

const router = useRouter()
const toast = useToast()
const confirm = useConfirm()

const loading = ref(false)
const generations = ref<GenerationSummaryDto[]>([])
const showNewDialog = ref(false)
const creating = ref(false)

const newGeneration = ref<StartGenerationRequest>({
  languageCode: 'fr',
  cefrLevel: 'A1',
  moduleCount: 4,
  episodesPerModule: 4,
  themePreferences: '',
  autoMode: false
})

onMounted(async () => {
  await loadGenerations()
})

async function loadGenerations() {
  loading.value = true
  try {
    generations.value = await PipelineService.listGenerations()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to load generations',
      life: 3000
    })
  } finally {
    loading.value = false
  }
}

function openNewDialog() {
  newGeneration.value = {
    languageCode: 'fr',
    cefrLevel: 'A1',
    moduleCount: 4,
    episodesPerModule: 4,
    themePreferences: '',
    autoMode: false
  }
  showNewDialog.value = true
}

async function startNewGeneration() {
  creating.value = true
  try {
    const result = await PipelineService.startGeneration(newGeneration.value)
    showNewDialog.value = false
    toast.add({
      severity: 'success',
      summary: 'Generation Started',
      detail: 'Course generation workflow has been started',
      life: 3000
    })
    router.push({name: 'pipeline-detail', params: {id: result.id}})
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to start generation',
      life: 3000
    })
  } finally {
    creating.value = false
  }
}

function viewGeneration(generation: GenerationSummaryDto) {
  router.push({name: 'pipeline-detail', params: {id: generation.id}})
}

function confirmDelete(generation: GenerationSummaryDto) {
  confirm.require({
    message: `Are you sure you want to delete this generation${generation.blueprintTitle ? ` "${generation.blueprintTitle}"` : ''}?`,
    header: 'Confirm Delete',
    icon: 'pi pi-exclamation-triangle',
    rejectClass: 'p-button-secondary p-button-outlined',
    acceptClass: 'p-button-danger',
    accept: () => deleteGeneration(generation.id)
  })
}

async function deleteGeneration(id: string) {
  try {
    await PipelineService.cancelGeneration(id)
    toast.add({
      severity: 'success',
      summary: 'Deleted',
      detail: 'Generation has been deleted',
      life: 3000
    })
    await loadGenerations()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to delete generation',
      life: 3000
    })
  }
}

function getStageLabel(stage: GenerationStage): string {
  return STAGE_CONFIG[stage]?.label || stage
}

function getStageSeverity(stage: GenerationStage): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
  switch (stage) {
    case 'COMPLETED':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'BLUEPRINT':
    case 'MODULE_PLANNING':
      return 'info'
    default:
      return 'warn'
  }
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleString()
}

function getLanguageLabel(code: string): string {
  return LANGUAGE_OPTIONS.find((l) => l.value === code)?.label || code
}
</script>

<template>
  <div class="pipeline-list">
    <Card>
      <template #title>
        <div class="flex justify-content-between align-items-center">
          <span>Course Generation Pipeline</span>
          <Button label="New Generation" icon="pi pi-plus" @click="openNewDialog"/>
        </div>
      </template>

      <template #content>
        <div v-if="loading" class="flex justify-content-center p-4">
          <ProgressSpinner/>
        </div>

        <DataTable
            v-else
            :value="generations"
            :paginator="generations.length > 10"
            :rows="10"
            stripedRows
            class="p-datatable-sm"
        >
          <template #empty>
            <div class="text-center p-4 text-color-secondary">
              No generations found. Click "New Generation" to start one.
            </div>
          </template>

          <Column field="blueprintTitle" header="Title" sortable>
            <template #body="{ data }">
              <span class="font-semibold">{{ data.blueprintTitle || 'Untitled' }}</span>
            </template>
          </Column>

          <Column field="languageCode" header="Language" sortable>
            <template #body="{ data }">
              {{ getLanguageLabel(data.languageCode) }}
            </template>
          </Column>

          <Column field="cefrLevel" header="Level" sortable/>

          <Column field="moduleCount" header="Modules" sortable/>

          <Column field="currentStage" header="Stage" sortable>
            <template #body="{ data }">
              <Tag :value="getStageLabel(data.currentStage)" :severity="getStageSeverity(data.currentStage)"/>
            </template>
          </Column>

          <Column field="createdAt" header="Created" sortable>
            <template #body="{ data }">
              {{ formatDate(data.createdAt) }}
            </template>
          </Column>

          <Column header="Actions" style="width: 150px">
            <template #body="{ data }">
              <div class="flex gap-2">
                <Button
                    icon="pi pi-eye"
                    severity="info"
                    text
                    rounded
                    v-tooltip.top="'View'"
                    @click="viewGeneration(data)"
                />
                <Button
                    icon="pi pi-trash"
                    severity="danger"
                    text
                    rounded
                    v-tooltip.top="'Delete'"
                    @click="confirmDelete(data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- New Generation Dialog -->
    <Dialog
        v-model:visible="showNewDialog"
        header="Start New Generation"
        :modal="true"
        :style="{ width: '500px' }"
    >
      <div class="flex flex-columnumn gap-4">
        <div class="flex flex-columnumn gap-2">
          <label for="language">Language</label>
          <Dropdown
              id="language"
              v-model="newGeneration.languageCode"
              :options="LANGUAGE_OPTIONS"
              optionLabel="label"
              optionValue="value"
              class="w-full"
          />
        </div>

        <div class="flex flex-columnumn gap-2">
          <label for="level">CEFR Level</label>
          <Dropdown
              id="level"
              v-model="newGeneration.cefrLevel"
              :options="CEFR_LEVELS"
              optionLabel="label"
              optionValue="value"
              class="w-full"
          />
        </div>

        <div class="flex gap-4">
          <div class="flex flex-columnumn gap-2 flex-1">
            <label for="modules">Number of Modules</label>
            <InputNumber
                id="modules"
                v-model="newGeneration.moduleCount"
                :min="1"
                :max="12"
                showButtons
                class="w-full"
            />
          </div>

          <div class="flex flex-columnumn gap-2 flex-1">
            <label for="episodes">Episodes per Module</label>
            <InputNumber
                id="episodes"
                v-model="newGeneration.episodesPerModule"
                :min="1"
                :max="10"
                showButtons
                class="w-full"
            />
          </div>
        </div>

        <div class="flex flex-columnumn gap-2">
          <label for="themes">Theme Preferences (optional)</label>
          <Textarea
              id="themes"
              v-model="newGeneration.themePreferences"
              rows="3"
              placeholder="e.g., Travel, Food, Work, Daily Life..."
              class="w-full"
          />
        </div>

        <div class="flex align-items-center gap-2">
          <Checkbox
              id="autoMode"
              v-model="newGeneration.autoMode"
              :binary="true"
          />
          <label for="autoMode" class="cursor-pointer">
            Auto Mode
            <span class="text-color-secondary text-sm ml-1">
              (automatically proceed through all stages)
            </span>
          </label>
        </div>
      </div>

      <template #footer>
        <Button label="Cancel" severity="secondary" text @click="showNewDialog = false"/>
        <Button
            label="Start Generation"
            icon="pi pi-play"
            :loading="creating"
            @click="startNewGeneration"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.pipeline-list {
  padding: 1rem;
}
</style>
