<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import Steps from 'primevue/steps'
import { CourseService, type ModuleAdminDto, type CourseAdminDto } from '@/services/CourseService'
import { GenerationService } from '@/services/GenerationService'
import type { GenerateOutlineRequest, GeneratedOutline, GeneratedEpisodeSummary, GenerateEpisodeContentRequest, GeneratedEpisodeContent } from '@/types/generation'

const route = useRoute()
const router = useRouter()
const moduleId = Number(route.params.moduleId)
const courseId = Number(route.params.courseId)

const module = ref<ModuleAdminDto | null>(null)
const course = ref<CourseAdminDto | null>(null)
const loading = ref(true)
const isGenerating = ref(false)
const error = ref('')
const activeStep = ref(0)

const generatedOutline = ref<GeneratedOutline | null>(null)

const steps = [
  { label: 'Review Module' },
  { label: 'Generate Outline' },
  { label: 'Review Episodes' },
  { label: 'Generate Content' }
]

onMounted(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    // Fetch module and course details
    // For now, we'll just fetch the module list and find the correct one since we don't have a direct getModule endpoint yet
    const modules = await CourseService.getModulesForCourse(courseId)
    module.value = modules.find(m => m.id === moduleId) || null
    
    // We also need course details for the language and level
    // TODO: Add getCourse endpoint. For now, we'll mock it or fetch all courses
    const courses = await CourseService.getAllCourses()
    course.value = courses.find(c => c.id === courseId) || null

    if (!module.value || !course.value) {
      error.value = 'Module or Course not found'
    }
  } catch (e: any) {
    error.value = e.message || 'Failed to load data'
  } finally {
    loading.value = false
  }
}

async function generateOutline() {
  if (!module.value || !course.value) return

  isGenerating.value = true
  error.value = ''
  generatedOutline.value = null

  try {
    const request: GenerateOutlineRequest = {
      targetLanguage: course.value.languageCode,
      level: course.value.cefrLevel,
      moduleTheme: module.value.title,
      moduleDescription: `Module ${module.value.moduleNumber} of the course.`, // We should probably fetch the description too
      seriesContext: 'Series context not available in this view yet' // TODO: Pass series context
    }
    generatedOutline.value = await GenerationService.generateOutline(request)
    activeStep.value = 2
  } catch (e: any) {
    error.value = e.message || 'Failed to generate outline'
  } finally {
    isGenerating.value = false
  }
}

function nextStep() {
  activeStep.value++
}

function prevStep() {
  activeStep.value--
}

const generationProgress = ref(0)
const currentEpisodeGenerating = ref('')

async function generateContent() {
  if (!generatedOutline.value || !module.value || !course.value) return

  isGenerating.value = true
  error.value = ''
  generationProgress.value = 0
  
  const episodesWithContent: any[] = []
  const totalEpisodes = generatedOutline.value.episodes.length

  try {
    for (let i = 0; i < totalEpisodes; i++) {
      const episode = generatedOutline.value.episodes[i]
      currentEpisodeGenerating.value = `Generating episode ${i + 1}/${totalEpisodes}: ${episode.title}`
      
      const request: GenerateEpisodeContentRequest = {
        targetLanguage: course.value.languageCode,
        level: course.value.cefrLevel,
        seriesContext: course.value.seriesContext || '', // Assuming we have this or it's optional
        moduleTheme: module.value.title,
        episodeTitle: episode.title,
        episodeType: episode.type,
        episodeSummary: episode.summary
      }

      const content = await GenerationService.generateEpisodeContent(request)
      episodesWithContent.push({
        ...episode,
        content
      })
      
      generationProgress.value = Math.round(((i + 1) / totalEpisodes) * 100)
    }

    // Save the module with all generated content
    currentEpisodeGenerating.value = 'Saving module...'
    await CourseService.saveModule(moduleId, { episodes: episodesWithContent })

    // Redirect to dashboard
    router.push({ name: 'course-dashboard', params: { id: courseId } })
    
  } catch (e: any) {
    error.value = e.message || 'Failed to generate content'
    isGenerating.value = false
  }
}
</script>

<template>
  <div class="module-wizard p-xl max-w-4xl mx-auto">
    <div class="mb-xl">
      <h1 class="text-3xl font-bold mb-sm">Module Content Wizard</h1>
      <p class="text-secondary" v-if="module && course">
        Generating content for <strong>{{ module.title }}</strong> ({{ course.name }})
      </p>
    </div>

    <Steps :model="steps" :activeStep="activeStep" class="mb-xl" />

    <div v-if="loading" class="flex justify-center p-xl">
      <ProgressSpinner />
    </div>

    <div v-else-if="error">
      <Message severity="error">{{ error }}</Message>
      <Button label="Retry" icon="pi pi-refresh" @click="loadData" class="mt-md" />
    </div>

    <div v-else>
      <!-- Step 0: Review Module -->
      <div v-if="activeStep === 0">
        <Card class="mb-xl">
          <template #title>Module Details</template>
          <template #content>
            <div class="grid grid-cols-2 gap-lg">
              <div>
                <label class="block font-bold mb-xs">Title</label>
                <p>{{ module?.title }}</p>
              </div>
              <div>
                <label class="block font-bold mb-xs">Module Number</label>
                <p>{{ module?.moduleNumber }}</p>
              </div>
            </div>
          </template>
        </Card>
        <div class="flex justify-end">
          <Button label="Next: Generate Outline" icon="pi pi-arrow-right" @click="nextStep" />
        </div>
      </div>

      <!-- Step 1: Generate Outline -->
      <div v-if="activeStep === 1">
        <div class="text-center p-xl">
          <p class="mb-lg text-lg">Ready to generate the episode structure for this module?</p>
          <Button label="Generate Outline" icon="pi pi-sparkles" @click="generateOutline" :loading="isGenerating" size="large" />
        </div>
        <div class="flex justify-start">
          <Button label="Back" icon="pi pi-arrow-left" text @click="prevStep" />
        </div>
      </div>

      <!-- Step 2: Review Episodes -->
      <div v-if="activeStep === 2 && generatedOutline">
        <h2 class="text-2xl font-bold mb-md">Generated Episodes</h2>
        <div class="grid gap-md mb-xl">
          <Card v-for="episode in generatedOutline.episodes" :key="episode.episodeNumber" class="bg-surface-card">
            <template #title>
              <div class="flex items-center gap-sm">
                <span class="bg-primary text-primary-contrast rounded-full w-8 h-8 flex items-center justify-center text-sm font-bold">{{ episode.episodeNumber }}</span>
                <span>{{ episode.title }}</span>
                <span class="text-sm font-normal text-secondary ml-auto">{{ episode.type }}</span>
              </div>
            </template>
            <template #content>
              <p class="m-0">{{ episode.summary }}</p>
            </template>
          </Card>
        </div>
        <div class="flex justify-between">
          <Button label="Back" icon="pi pi-arrow-left" text @click="prevStep" />
          <Button label="Next: Generate Content" icon="pi pi-arrow-right" @click="nextStep" />
        </div>
      </div>

      <!-- Step 3: Generate Content -->
      <div v-if="activeStep === 3">
        <div class="text-center p-xl">
          <p class="mb-lg text-lg">Ready to generate the actual content (dialogues, stories, exercises) for these episodes?</p>
          <p class="text-secondary mb-xl">This process may take a few minutes.</p>
          <Button label="Generate Content" icon="pi pi-bolt" @click="generateContent" severity="success" size="large" :loading="isGenerating" />
          <div v-if="isGenerating" class="mt-lg">
            <p class="mb-sm text-primary">{{ currentEpisodeGenerating }}</p>
            <div class="w-full bg-surface-ground rounded-full h-2 overflow-hidden">
              <div class="bg-primary h-full transition-all duration-500" :style="{ width: `${generationProgress}%` }"></div>
            </div>
          </div>
        </div>
        <div class="flex justify-start">
          <Button label="Back" icon="pi pi-arrow-left" text @click="prevStep" />
        </div>
      </div>
    </div>
  </div>
</template>
