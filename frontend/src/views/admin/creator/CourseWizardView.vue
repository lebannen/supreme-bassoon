<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Dropdown from 'primevue/dropdown'
import Card from 'primevue/card'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import { GenerationService } from '@/services/GenerationService'
import { CourseService } from '@/services/CourseService'
import type { GenerateSyllabusRequest, GeneratedSyllabus } from '@/types/generation'

const router = useRouter()

const targetLanguage = ref('fr')
const level = ref('A1')
const seriesContext = ref('')
const isGenerating = ref(false)
const error = ref('')
const generatedSyllabus = ref<GeneratedSyllabus | null>(null)

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

async function generateSyllabus() {
  if (!seriesContext.value.trim()) {
    error.value = 'Please provide a context for the series.'
    return
  }

  isGenerating.value = true
  error.value = ''
  generatedSyllabus.value = null

  try {
    const request: GenerateSyllabusRequest = {
      targetLanguage: targetLanguage.value,
      level: level.value,
      seriesContext: seriesContext.value
    }
    generatedSyllabus.value = await GenerationService.generateSyllabus(request)
  } catch (e: any) {
    error.value = e.message || 'Failed to generate syllabus'
  } finally {
    isGenerating.value = false
  }
}

async function saveAndContinue() {
  if (!generatedSyllabus.value) return

  try {
    const request = {
      targetLanguage: targetLanguage.value,
      level: level.value,
      seriesContext: seriesContext.value,
      syllabus: generatedSyllabus.value
    }
    
    const course = await CourseService.createCourse(request)
    router.push({ name: 'course-dashboard', params: { id: course.id } })
  } catch (e: any) {
    error.value = e.message || 'Failed to save course'
  }
}
</script>

<template>
  <div class="course-wizard p-xl max-w-4xl mx-auto">
    <div class="mb-xl">
      <h1 class="text-3xl font-bold mb-sm">Create New Course</h1>
      <p class="text-secondary">Define the core settings and narrative context for your language course.</p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-xl mb-xl">
      <div class="field">
        <label class="block font-bold mb-sm">Target Language</label>
        <Dropdown v-model="targetLanguage" :options="languages" optionLabel="label" optionValue="value" class="w-full" />
      </div>
      <div class="field">
        <label class="block font-bold mb-sm">Level</label>
        <Dropdown v-model="level" :options="levels" optionLabel="label" optionValue="value" class="w-full" />
      </div>
    </div>

    <div class="field mb-xl">
      <label class="block font-bold mb-sm">Series Context (The "Bible")</label>
      <p class="text-sm text-secondary mb-sm">Describe the main characters, setting, and overall story arc. This will guide the AI in generating cohesive modules.</p>
      <Textarea v-model="seriesContext" rows="6" class="w-full" placeholder="e.g. The protagonist is Alex, a young detective who moves to Paris to solve a mystery involving a lost painting. He meets Marie, a local historian..." />
    </div>

    <div class="actions mb-xl">
      <Button label="Generate Syllabus" icon="pi pi-sparkles" @click="generateSyllabus" :loading="isGenerating" />
    </div>

    <Message v-if="error" severity="error" class="mb-lg">{{ error }}</Message>

    <div v-if="generatedSyllabus" class="syllabus-preview">
      <h2 class="text-2xl font-bold mb-md">Generated Syllabus</h2>
      <div class="grid gap-md">
        <Card v-for="module in generatedSyllabus.modules" :key="module.moduleNumber" class="bg-surface-card">
          <template #title>
            <div class="flex items-center gap-sm">
              <span class="bg-primary text-primary-contrast rounded-full w-8 h-8 flex items-center justify-center text-sm font-bold">{{ module.moduleNumber }}</span>
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

      <div class="actions mt-xl flex justify-end">
        <Button label="Save & Continue" icon="pi pi-arrow-right" @click="saveAndContinue" severity="success" size="large" />
      </div>
    </div>
  </div>
</template>
