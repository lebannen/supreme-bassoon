<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useToast} from 'primevue/usetoast'
import {useConfirm} from 'primevue/useconfirm'
import Card from 'primevue/card'
import Button from 'primevue/button'
import ProgressBar from 'primevue/progressbar'
import Message from 'primevue/message'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
import ProgressSpinner from 'primevue/progressspinner'
import PipelineService from '@/services/PipelineService'
import type {GenerationProgressResponse, GenerationStage} from '@/types/pipeline'
import {LANGUAGE_OPTIONS, STAGE_CONFIG} from '@/types/pipeline'
import BlueprintStage from './components/BlueprintStage.vue'
import ModulePlanStage from './components/ModulePlanStage.vue'
import EpisodeStage from './components/EpisodeStage.vue'
import CharacterStage from './components/CharacterStage.vue'
import ExerciseStage from './components/ExerciseStage.vue'
import MediaStage from './components/MediaStage.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const confirm = useConfirm()

const generationId = computed(() => route.params.id as string)
const loading = ref(true)
const processing = ref(false)
const generation = ref<GenerationProgressResponse | null>(null)
const showRegenerateDialog = ref(false)
const regenerateFeedback = ref('')
let pollInterval: ReturnType<typeof setInterval> | null = null

const stages: GenerationStage[] = [
  'BLUEPRINT',
  'MODULE_PLANNING',
  'EPISODE_CONTENT',
  'VOCABULARY_LINKING',
  'CHARACTER_PROFILES',
  'EXERCISES',
  'MEDIA',
  'COMPLETED'
]

const currentStageIndex = computed(() => {
  if (!generation.value) return 0
  return stages.indexOf(generation.value.currentStage)
})

const overallProgress = computed(() => {
  if (!generation.value) return 0
  const idx = currentStageIndex.value
  if (idx === -1) return 0
  // Each completed stage + progress in current stage
  const stageProgress = generation.value.stageProgress
  const currentProgress =
      stageProgress.total > 0 ? stageProgress.completed / stageProgress.total : 0
  return Math.round(((idx + currentProgress) / (stages.length - 1)) * 100)
})

const isCompleted = computed(() => generation.value?.currentStage === 'COMPLETED')
const isFailed = computed(() => generation.value?.currentStage === 'FAILED')
const isProcessing = computed(() => {
  if (!generation.value) return false
  const progress = generation.value.stageProgress
  return progress.total > 0 && progress.completed < progress.total
})

onMounted(async () => {
  await loadGeneration()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})

function startPolling() {
  pollInterval = setInterval(async () => {
    if (isProcessing.value && !processing.value) {
      await loadGeneration(true)
    }
  }, 3000)
}

function stopPolling() {
  if (pollInterval) {
    clearInterval(pollInterval)
    pollInterval = null
  }
}

async function loadGeneration(silent = false) {
  if (!silent) loading.value = true
  try {
    generation.value = await PipelineService.getProgress(generationId.value)
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to load generation progress',
      life: 3000
    })
  } finally {
    loading.value = false
  }
}

async function proceedToNextStage() {
  processing.value = true
  try {
    generation.value = await PipelineService.proceedToNextStage(generationId.value)
    toast.add({
      severity: 'success',
      summary: 'Stage Advanced',
      detail: `Now at: ${STAGE_CONFIG[generation.value.currentStage].label}`,
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to proceed to next stage',
      life: 3000
    })
  } finally {
    processing.value = false
  }
}

function openRegenerateDialog() {
  regenerateFeedback.value = ''
  showRegenerateDialog.value = true
}

async function regenerateStage() {
  processing.value = true
  showRegenerateDialog.value = false
  try {
    generation.value = await PipelineService.regenerateCurrentStage(generationId.value, {
      feedback: regenerateFeedback.value || undefined
    })
    toast.add({
      severity: 'info',
      summary: 'Regenerating',
      detail: 'Stage is being regenerated',
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to regenerate stage',
      life: 3000
    })
  } finally {
    processing.value = false
  }
}

function confirmPublish() {
  confirm.require({
    message: 'This will create a live course from this generation. Continue?',
    header: 'Publish Course',
    icon: 'pi pi-cloud-upload',
    accept: publishCourse
  })
}

async function publishCourse() {
  processing.value = true
  try {
    const course = await PipelineService.publishCourse(generationId.value)
    toast.add({
      severity: 'success',
      summary: 'Course Published',
      detail: `Course "${course.name}" has been created`,
      life: 5000
    })
    router.push({name: 'course-dashboard', params: {id: course.id}})
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to publish course',
      life: 3000
    })
  } finally {
    processing.value = false
  }
}

function goBack() {
  router.push({name: 'pipeline-list'})
}

function openDebugView() {
  router.push({name: 'pipeline-debug', params: {id: generationId.value}})
}

function getLanguageLabel(code: string): string {
  return LANGUAGE_OPTIONS.find((l) => l.value === code)?.label || code
}

function getStageIcon(stage: GenerationStage): string {
  return `pi ${STAGE_CONFIG[stage]?.icon || 'pi-circle'}`
}

function isStageCompleted(stage: GenerationStage): boolean {
  const stageIdx = stages.indexOf(stage)
  return stageIdx < currentStageIndex.value
}

function isStageCurrent(stage: GenerationStage): boolean {
  return generation.value?.currentStage === stage
}
</script>

<template>
  <div class="pipeline-detail">
    <div v-if="loading" class="flex justify-content-center p-8">
      <ProgressSpinner/>
    </div>

    <template v-else-if="generation">
      <!-- Header -->
      <Card class="mb-4">
        <template #content>
          <div class="flex justify-content-between align-items-start">
            <div>
              <Button
                  icon="pi pi-arrow-left"
                  text
                  severity="secondary"
                  class="mb-2"
                  @click="goBack"
              />
              <h2 class="m-0">{{ generation.blueprint?.courseTitle || 'Course Generation' }}</h2>
              <div class="flex gap-3 mt-2 text-color-secondary">
                <span>
                  <i class="pi pi-globe mr-1"></i>
                  {{ getLanguageLabel(generation.languageCode) }}
                </span>
                <span>
                  <i class="pi pi-chart-bar mr-1"></i>
                  {{ generation.cefrLevel }}
                </span>
                <span>
                  <i class="pi pi-folder mr-1"></i>
                  {{ generation.moduleCount }} modules
                </span>
                <span>
                  <i class="pi pi-file mr-1"></i>
                  {{ generation.episodesPerModule }} episodes each
                </span>
              </div>
            </div>

            <div class="flex gap-2">
              <Button
                  v-if="generation.canProceed && !isCompleted"
                  label="Proceed"
                  icon="pi pi-arrow-right"
                  :loading="processing"
                  @click="proceedToNextStage"
              />
              <Button
                  v-if="!isCompleted && !isFailed"
                  label="Regenerate"
                  icon="pi pi-refresh"
                  severity="secondary"
                  :loading="processing"
                  @click="openRegenerateDialog"
              />
              <Button
                  v-if="generation.canPublish"
                  label="Publish Course"
                  icon="pi pi-cloud-upload"
                  severity="success"
                  :loading="processing"
                  @click="confirmPublish"
              />
              <Button
                  label="Debug"
                  icon="pi pi-code"
                  severity="secondary"
                  text
                  @click="openDebugView"
              />
            </div>
          </div>
        </template>
      </Card>

      <!-- Error Message -->
      <Message v-if="isFailed && generation.errorMessage" severity="error" class="mb-4">
        {{ generation.errorMessage }}
      </Message>

      <!-- Stage Progress -->
      <Card class="mb-4">
        <template #title>Progress</template>
        <template #content>
          <ProgressBar :value="overallProgress" class="mb-4"/>

          <div class="stage-timeline">
            <div
                v-for="stage in stages.filter((s) => s !== 'COMPLETED')"
                :key="stage"
                class="stage-item"
                :class="{
                completed: isStageCompleted(stage),
                current: isStageCurrent(stage),
                failed: isFailed && isStageCurrent(stage)
              }"
            >
              <div class="stage-icon">
                <i :class="getStageIcon(stage)"></i>
              </div>
              <div class="stage-label">{{ STAGE_CONFIG[stage].label }}</div>
              <div v-if="isStageCurrent(stage) && isProcessing" class="stage-progress">
                {{ generation.stageProgress.completed }}/{{ generation.stageProgress.total }}
              </div>
            </div>
          </div>
        </template>
      </Card>

      <!-- Current Stage Content -->
      <BlueprintStage
          v-if="generation.currentStage === 'BLUEPRINT' || isStageCompleted('BLUEPRINT')"
          :blueprint="generation.blueprint"
          :characters="generation.characters"
          :is-current="generation.currentStage === 'BLUEPRINT'"
      />

      <ModulePlanStage
          v-if="
          generation.currentStage === 'MODULE_PLANNING' ||
          (isStageCompleted('MODULE_PLANNING') && generation.modulePlans)
        "
          :module-plans="generation.modulePlans"
          :is-current="generation.currentStage === 'MODULE_PLANNING'"
      />

      <EpisodeStage
          v-if="
          generation.currentStage === 'EPISODE_CONTENT' ||
          (isStageCompleted('EPISODE_CONTENT') && generation.modulePlans)
        "
          :module-plans="generation.modulePlans"
          :is-current="generation.currentStage === 'EPISODE_CONTENT'"
      />

      <CharacterStage
          v-if="
          generation.currentStage === 'CHARACTER_PROFILES' ||
          (isStageCompleted('CHARACTER_PROFILES') && generation.characters)
        "
          :characters="generation.characters"
          :is-current="generation.currentStage === 'CHARACTER_PROFILES'"
      />

      <ExerciseStage
          v-if="
          generation.currentStage === 'EXERCISES' ||
          (isStageCompleted('EXERCISES') && generation.modulePlans)
        "
          :module-plans="generation.modulePlans"
          :is-current="generation.currentStage === 'EXERCISES'"
      />

      <MediaStage
          v-if="generation.currentStage === 'MEDIA' || isStageCompleted('MEDIA')"
          :module-plans="generation.modulePlans"
          :is-current="generation.currentStage === 'MEDIA'"
      />
    </template>

    <!-- Regenerate Dialog -->
    <Dialog
        v-model:visible="showRegenerateDialog"
        header="Regenerate Stage"
        :modal="true"
        :style="{ width: '500px' }"
    >
      <div class="flex flex-columnumn gap-3">
        <p>Provide optional feedback to guide the regeneration:</p>
        <Textarea
            v-model="regenerateFeedback"
            rows="4"
            placeholder="e.g., Make the dialogue more natural, include more vocabulary from the list..."
            class="w-full"
        />
      </div>

      <template #footer>
        <Button label="Cancel" severity="secondary" text @click="showRegenerateDialog = false"/>
        <Button label="Regenerate" icon="pi pi-refresh" @click="regenerateStage"/>
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.pipeline-detail {
  padding: 1rem;
  max-width: 1400px;
  margin: 0 auto;
}

.stage-timeline {
  display: flex;
  justify-content: space-between;
  position: relative;
}

.stage-timeline::before {
  content: '';
  position: absolute;
  top: 20px;
  left: 40px;
  right: 40px;
  height: 2px;
  background: var(--surface-300);
}

.stage-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 1;
  flex: 1;
}

.stage-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--surface-200);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.5rem;
  transition: all 0.3s;
}

.stage-icon i {
  font-size: 1rem;
  color: var(--text-color-secondary);
}

.stage-item.completed .stage-icon {
  background: var(--green-500);
}

.stage-item.completed .stage-icon i {
  color: white;
}

.stage-item.current .stage-icon {
  background: var(--primary-color);
  box-shadow: 0 0 0 4px var(--primary-100);
}

.stage-item.current .stage-icon i {
  color: white;
}

.stage-item.failed .stage-icon {
  background: var(--red-500);
}

.stage-item.failed .stage-icon i {
  color: white;
}

.stage-label {
  font-size: 0.85rem;
  text-align: center;
  color: var(--text-color-secondary);
}

.stage-item.current .stage-label {
  color: var(--primary-color);
  font-weight: 600;
}

.stage-progress {
  font-size: 0.75rem;
  color: var(--text-color-secondary);
  margin-top: 0.25rem;
}
</style>
