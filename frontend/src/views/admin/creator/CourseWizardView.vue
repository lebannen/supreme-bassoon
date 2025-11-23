<script setup lang="ts">
import {ref, computed} from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Dropdown from 'primevue/dropdown'
import Card from 'primevue/card'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import ProgressBar from 'primevue/progressbar'
import Steps from 'primevue/steps'
import { GenerationService } from '@/services/GenerationService'
import { CourseService } from '@/services/CourseService'
import type {
  GenerateSyllabusRequest,
  GeneratedSyllabus,
  GeneratedModuleSummary,
  GenerateModulePlanRequest,
  GeneratedModulePlan
} from '@/types/generation'

const router = useRouter()

// Step management
const activeStep = ref(0)
const steps = [
  {label: 'Course Settings'},
  {label: 'Generate Syllabus'},
  {label: 'Enrich Modules'},
  {label: 'Review & Create'}
]

// Step 1: Course Settings
const courseName = ref('')
const targetLanguage = ref('fr')
const level = ref('A1')
const seriesContext = ref('')

// Step 2: Syllabus
const isGeneratingSyllabus = ref(false)
const generatedSyllabus = ref<GeneratedSyllabus | null>(null)

// Step 3: Module Enrichment
const enrichedModules = ref<Map<number, GeneratedModulePlan>>(new Map())
const currentEnrichingModule = ref<number | null>(null)
const enrichmentProgress = ref(0)

// General
const error = ref('')

const languages = [
  { label: 'French', value: 'fr' },
  { label: 'German', value: 'de' }
]

const levels = [
  { label: 'A1 (Beginner)', value: 'A1' },
  { label: 'A2 (Elementary)', value: 'A2' },
  { label: 'B1 (Intermediate)', value: 'B1' },
  { label: 'B2 (Upper Intermediate)', value: 'B2' }
]

const canProceedFromStep1 = computed(() => {
  return courseName.value.trim().length > 0 &&
      targetLanguage.value &&
      level.value &&
      seriesContext.value.trim().length > 0
})

const canProceedFromStep2 = computed(() => {
  return generatedSyllabus.value !== null
})

const canProceedFromStep3 = computed(() => {
  return generatedSyllabus.value &&
      enrichedModules.value.size === generatedSyllabus.value.modules.length
})

async function generateSyllabus() {
  if (!seriesContext.value.trim()) {
    error.value = 'Please provide a context for the series.'
    return
  }

  isGeneratingSyllabus.value = true
  error.value = ''
  generatedSyllabus.value = null

  try {
    const request: GenerateSyllabusRequest = {
      targetLanguage: targetLanguage.value,
      level: level.value,
      seriesContext: seriesContext.value
    }
    generatedSyllabus.value = await GenerationService.generateSyllabus(request)
    activeStep.value = 2 // Move to enrichment step
  } catch (e: any) {
    error.value = e.message || 'Failed to generate syllabus'
  } finally {
    isGeneratingSyllabus.value = false
  }
}

async function regenerateSyllabus() {
  generatedSyllabus.value = null
  enrichedModules.value.clear()
  await generateSyllabus()
}

async function enrichAllModules() {
  if (!generatedSyllabus.value) return

  error.value = ''
  enrichedModules.value.clear()
  enrichmentProgress.value = 0

  const totalModules = generatedSyllabus.value.modules.length

  try {
    for (let i = 0; i < totalModules; i++) {
      const moduleSummary = generatedSyllabus.value.modules[i]
      currentEnrichingModule.value = moduleSummary.moduleNumber

      const plan = await enrichModule(moduleSummary)
      enrichedModules.value.set(moduleSummary.moduleNumber, plan)

      enrichmentProgress.value = Math.round(((i + 1) / totalModules) * 100)
    }

    currentEnrichingModule.value = null
    activeStep.value = 3 // Move to review step
  } catch (e: any) {
    error.value = e.message || 'Failed to enrich modules'
    currentEnrichingModule.value = null
  }
}

async function enrichModule(moduleSummary: GeneratedModuleSummary): Promise<GeneratedModulePlan> {
  const request: GenerateModulePlanRequest = {
    targetLanguage: targetLanguage.value,
    level: level.value,
    seriesContext: seriesContext.value,
    moduleNumber: moduleSummary.moduleNumber,
    moduleTitle: moduleSummary.title,
    moduleTheme: moduleSummary.theme,
    moduleDescription: moduleSummary.description
  }

  return await GenerationService.generateModulePlan(request)
}

async function regenerateModulePlan(moduleSummary: GeneratedModuleSummary) {
  try {
    error.value = ''
    currentEnrichingModule.value = moduleSummary.moduleNumber
    const plan = await enrichModule(moduleSummary)
    enrichedModules.value.set(moduleSummary.moduleNumber, plan)
  } catch (e: any) {
    error.value = e.message || `Failed to regenerate module ${moduleSummary.moduleNumber}`
  } finally {
    currentEnrichingModule.value = null
  }
}

async function createCourse() {
  if (!generatedSyllabus.value) return

  try {
    error.value = ''

    // Build enriched modules array
    const enrichedModulesArray = generatedSyllabus.value.modules.map(moduleSummary => {
      const plan = enrichedModules.value.get(moduleSummary.moduleNumber)
      return {
        moduleNumber: moduleSummary.moduleNumber,
        title: moduleSummary.title,
        theme: moduleSummary.theme,
        description: moduleSummary.description,
        detailedDescription: plan?.detailedDescription,
        objectives: plan?.objectives,
        vocabularyFocus: plan?.vocabularyFocus,
        grammarFocus: plan?.grammarFocus,
        episodeOutline: plan?.episodeOutline
      }
    })

    const request = {
      name: courseName.value,
      targetLanguage: targetLanguage.value,
      level: level.value,
      seriesContext: seriesContext.value,
      syllabus: generatedSyllabus.value,
      enrichedModules: enrichedModulesArray
    }

    const course = await CourseService.createCourse(request)
    router.push({ name: 'course-dashboard', params: { id: course.id } })
  } catch (e: any) {
    error.value = e.message || 'Failed to create course'
  }
}

function nextStep() {
  if (activeStep.value < steps.length - 1) {
    activeStep.value++
  }
}

function prevStep() {
  if (activeStep.value > 0) {
    activeStep.value--
  }
}
</script>

<template>
  <div class="course-wizard p-xl max-w-6xl mx-auto">
    <div class="mb-xl">
      <h1 class="text-3xl font-bold mb-sm">Create New Course</h1>
      <p class="text-secondary">AI-powered course creation with full planning and control.</p>
    </div>

    <Steps :model="steps" :activeStep="activeStep" class="mb-xl"/>

    <Message v-if="error" severity="error" class="mb-lg">{{ error }}</Message>

    <!-- STEP 1: COURSE SETTINGS -->
    <div v-if="activeStep === 0" class="step-content">
      <h2 class="text-2xl font-bold mb-lg">Course Settings</h2>

      <div class="field mb-xl">
        <label class="block font-bold mb-sm">Course Name</label>
        <InputText v-model="courseName" class="w-full" placeholder="e.g., Alex's Parisian Adventure"/>
        <small class="text-secondary">Give your course a descriptive name</small>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-xl mb-xl">
        <div class="field">
          <label class="block font-bold mb-sm">Target Language</label>
          <Dropdown v-model="targetLanguage" :options="languages" optionLabel="label" optionValue="value"
                    class="w-full"/>
        </div>
        <div class="field">
          <label class="block font-bold mb-sm">Level</label>
          <Dropdown v-model="level" :options="levels" optionLabel="label" optionValue="value" class="w-full"/>
        </div>
      </div>

      <div class="field mb-xl">
        <label class="block font-bold mb-sm">Series Context (The "Bible")</label>
        <p class="text-sm text-secondary mb-sm">
          Describe the main characters, setting, and overall story arc. This will guide the AI in generating cohesive
          modules.
        </p>
        <Textarea
            v-model="seriesContext"
            rows="8"
            class="w-full"
            placeholder="e.g. The protagonist is Alex, a young detective who moves to Paris to solve a mystery involving a lost painting. He meets Marie, a local historian..."
        />
      </div>

      <div class="flex justify-end">
        <Button
            label="Next: Generate Syllabus"
            icon="pi pi-arrow-right"
            @click="nextStep"
            :disabled="!canProceedFromStep1"
        />
      </div>
    </div>

    <!-- STEP 2: GENERATE SYLLABUS -->
    <div v-if="activeStep === 1" class="step-content">
      <h2 class="text-2xl font-bold mb-lg">Generate Syllabus</h2>

      <div v-if="!generatedSyllabus" class="text-center p-xl">
        <p class="mb-lg text-lg">Ready to generate a 10-module syllabus based on your series context?</p>
        <Button
            label="Generate Syllabus"
            icon="pi pi-sparkles"
            @click="generateSyllabus"
            :loading="isGeneratingSyllabus"
            size="large"
        />
      </div>

      <div v-else>
        <div class="flex justify-between items-center mb-md">
          <h3 class="text-xl font-bold">Generated Modules</h3>
          <Button
              label="Regenerate Syllabus"
              icon="pi pi-refresh"
              @click="regenerateSyllabus"
              severity="secondary"
              text
          />
        </div>

        <div class="grid gap-md mb-xl">
          <Card v-for="module in generatedSyllabus.modules" :key="module.moduleNumber" class="bg-surface-card">
            <template #title>
              <div class="flex items-center gap-sm">
                <span
                    class="bg-primary text-primary-contrast rounded-full w-8 h-8 flex items-center justify-center text-sm font-bold">
                  {{ module.moduleNumber }}
                </span>
                <span>{{ module.title }}</span>
              </div>
            </template>
            <template #subtitle>
              <span class="text-primary font-bold">{{ module.theme }}</span>
            </template>
            <template #content>
              <p class="m-0">{{ module.description }}</p>
            </template>
          </Card>
        </div>

        <div class="flex justify-between">
          <Button label="Back" icon="pi pi-arrow-left" text @click="prevStep"/>
          <Button
              label="Next: Enrich Modules"
              icon="pi pi-arrow-right"
              @click="nextStep"
              :disabled="!canProceedFromStep2"
          />
        </div>
      </div>
    </div>

    <!-- STEP 3: ENRICH MODULES -->
    <div v-if="activeStep === 2" class="step-content">
      <h2 class="text-2xl font-bold mb-lg">Enrich Modules with Details</h2>
      <p class="text-secondary mb-xl">
        Now we'll generate detailed plans for each module, including learning objectives, vocabulary focus, grammar
        points, and episode outlines.
      </p>

      <div v-if="enrichedModules.size === 0" class="text-center p-xl">
        <p class="mb-lg text-lg">Generate detailed plans for all {{ generatedSyllabus?.modules.length }} modules?</p>
        <p class="text-sm text-secondary mb-xl">This may take 2-3 minutes.</p>
        <Button
            label="Enrich All Modules"
            icon="pi pi-bolt"
            @click="enrichAllModules"
            :loading="currentEnrichingModule !== null"
            severity="success"
            size="large"
        />

        <div v-if="currentEnrichingModule !== null" class="mt-xl max-w-md mx-auto">
          <p class="mb-sm text-primary">Enriching Module {{ currentEnrichingModule }}...</p>
          <ProgressBar :value="enrichmentProgress"/>
        </div>
      </div>

      <div v-else class="enriched-modules">
        <div class="grid gap-lg mb-xl">
          <Card
              v-for="module in generatedSyllabus?.modules"
              :key="module.moduleNumber"
              class="bg-surface-card"
          >
            <template #title>
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-sm">
                  <span
                      class="bg-primary text-primary-contrast rounded-full w-8 h-8 flex items-center justify-center text-sm font-bold">
                    {{ module.moduleNumber }}
                  </span>
                  <span>{{ module.title }}</span>
                </div>
                <Button
                    icon="pi pi-refresh"
                    text
                    rounded
                    severity="secondary"
                    v-tooltip="'Regenerate Plan'"
                    @click="regenerateModulePlan(module)"
                    :loading="currentEnrichingModule === module.moduleNumber"
                />
              </div>
            </template>
            <template #content>
              <div v-if="enrichedModules.get(module.moduleNumber)" class="space-y-md">
                <div>
                  <h4 class="font-bold text-sm uppercase text-secondary mb-xs">Objectives</h4>
                  <ul class="list-disc pl-lg m-0">
                    <li v-for="(obj, idx) in enrichedModules.get(module.moduleNumber)?.objectives" :key="idx"
                        class="text-sm">
                      {{ obj }}
                    </li>
                  </ul>
                </div>

                <div>
                  <h4 class="font-bold text-sm uppercase text-secondary mb-xs">Episode Outline
                    ({{ enrichedModules.get(module.moduleNumber)?.episodeOutline.length }} episodes)</h4>
                  <div class="space-y-xs">
                    <div
                        v-for="ep in enrichedModules.get(module.moduleNumber)?.episodeOutline"
                        :key="ep.episodeNumber"
                        class="p-sm bg-surface-ground rounded text-sm"
                    >
                      <div class="font-bold">{{ ep.episodeNumber }}. {{ ep.title }} <span
                          class="text-xs text-secondary">({{ ep.type }})</span></div>
                      <div class="text-secondary">{{ ep.summary }}</div>
                    </div>
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-md">
                  <div>
                    <h4 class="font-bold text-sm uppercase text-secondary mb-xs">Vocabulary</h4>
                    <div class="flex flex-wrap gap-xs">
                      <span
                          v-for="(vocab, idx) in enrichedModules.get(module.moduleNumber)?.vocabularyFocus"
                          :key="idx"
                          class="px-sm py-xs bg-blue-100 text-blue-800 rounded text-xs"
                      >
                        {{ vocab }}
                      </span>
                    </div>
                  </div>
                  <div>
                    <h4 class="font-bold text-sm uppercase text-secondary mb-xs">Grammar</h4>
                    <div class="flex flex-wrap gap-xs">
                      <span
                          v-for="(grammar, idx) in enrichedModules.get(module.moduleNumber)?.grammarFocus"
                          :key="idx"
                          class="px-sm py-xs bg-green-100 text-green-800 rounded text-xs"
                      >
                        {{ grammar }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </Card>
        </div>

        <div class="flex justify-between">
          <Button label="Back" icon="pi pi-arrow-left" text @click="prevStep"/>
          <Button
              label="Next: Review & Create"
              icon="pi pi-arrow-right"
              @click="nextStep"
              :disabled="!canProceedFromStep3"
          />
        </div>
      </div>
    </div>

    <!-- STEP 4: REVIEW & CREATE -->
    <div v-if="activeStep === 3" class="step-content">
      <h2 class="text-2xl font-bold mb-lg">Review & Create Course</h2>

      <Card class="mb-xl bg-surface-card">
        <template #title>Course Overview</template>
        <template #content>
          <div class="grid grid-cols-2 gap-lg">
            <div>
              <label class="block font-bold text-sm mb-xs">Course Name</label>
              <p class="m-0">{{ courseName }}</p>
            </div>
            <div>
              <label class="block font-bold text-sm mb-xs">Language & Level</label>
              <p class="m-0">{{ languages.find(l => l.value === targetLanguage)?.label }} - {{ level }}</p>
            </div>
            <div>
              <label class="block font-bold text-sm mb-xs">Total Modules</label>
              <p class="m-0">{{ generatedSyllabus?.modules.length }}</p>
            </div>
            <div class="col-span-2">
              <label class="block font-bold text-sm mb-xs">Series Context</label>
              <p class="m-0 text-sm text-secondary">{{ seriesContext.substring(0, 200) }}...</p>
            </div>
          </div>
        </template>
      </Card>

      <div class="text-center p-xl">
        <p class="mb-lg text-lg">Ready to create your course?</p>
        <p class="text-sm text-secondary mb-xl">
          This will create the course with all {{ generatedSyllabus?.modules.length }} modules.
          You can then generate episode content for each module individually.
        </p>
        <Button
            label="Create Course"
            icon="pi pi-check-circle"
            @click="createCourse"
            severity="success"
            size="large"
        />
      </div>

      <div class="flex justify-start">
        <Button label="Back" icon="pi pi-arrow-left" text @click="prevStep"/>
      </div>
    </div>
  </div>
</template>

<style scoped>
.space-y-xs > * + * {
  margin-top: 0.5rem;
}

.space-y-md > * + * {
  margin-top: 1rem;
}
</style>
