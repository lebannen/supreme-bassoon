<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Checkbox from 'primevue/checkbox'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import ProgressBar from 'primevue/progressbar'
import Steps from 'primevue/steps'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'
import {type CourseAdminDto, CourseService, type ModuleDetailDto} from '@/services/CourseService'
import {GenerationService} from '@/services/GenerationService'
import type {GeneratedEpisodeContent, GeneratedEpisodeSummary, GenerateEpisodeContentRequest} from '@/types/generation'

const route = useRoute()
const router = useRouter()
const moduleId = Number(route.params.moduleId)
const courseId = Number(route.params.courseId)

const module = ref<ModuleDetailDto | null>(null)
const course = ref<CourseAdminDto | null>(null)
const loading = ref(true)
const isGenerating = ref(false)
const error = ref('')
const activeStep = ref(0)

const steps = [
  {label: 'Review Module Plan'},
  {label: 'Generate & Review Episodes'},
  {label: 'Save Episodes'}
]

const generateAudio = ref(true)
const currentEpisodeIndex = ref(0)
const generatedEpisodes = ref<Map<number, any>>(new Map())

const episodeOutline = computed(() => {
  if (!module.value?.episodeOutline) return []

  if (Array.isArray(module.value.episodeOutline)) {
    return module.value.episodeOutline as GeneratedEpisodeSummary[]
  }

  return module.value.episodeOutline as unknown as GeneratedEpisodeSummary[]
})

const currentEpisode = computed(() => {
  if (currentEpisodeIndex.value < episodeOutline.value.length) {
    return episodeOutline.value[currentEpisodeIndex.value]
  }
  return null
})

const currentGeneratedContent = computed(() => {
  return generatedEpisodes.value.get(currentEpisodeIndex.value)
})

const allEpisodesGenerated = computed(() => {
  return generatedEpisodes.value.size === episodeOutline.value.length
})

onMounted(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    module.value = await CourseService.getModule(moduleId)
    course.value = await CourseService.getCourse(courseId)

    if (!module.value || !course.value) {
      error.value = 'Module or Course not found'
    }
  } catch (e: any) {
    error.value = e.message || 'Failed to load data'
  } finally {
    loading.value = false
  }
}

async function generateCurrentEpisode() {
  if (!currentEpisode.value || !course.value || !module.value) return

  isGenerating.value = true
  error.value = ''

  try {
    const episode = currentEpisode.value
    const request: GenerateEpisodeContentRequest = {
      targetLanguage: course.value.languageCode,
      level: course.value.cefrLevel,
      seriesContext: course.value.seriesContext || '',
      moduleTheme: module.value.title,
      episodeTitle: episode.title,
      episodeType: episode.type,
      episodeSummary: episode.summary
    }

    const content = await GenerationService.generateEpisodeContent(request)

    generatedEpisodes.value.set(currentEpisodeIndex.value, {
      ...episode,
      content
    })
  } catch (e: any) {
    error.value = e.message || 'Failed to generate episode'
  } finally {
    isGenerating.value = false
  }
}

async function regenerateCurrentEpisode() {
  generatedEpisodes.value.delete(currentEpisodeIndex.value)
  await generateCurrentEpisode()
}

function approveAndNext() {
  if (currentEpisodeIndex.value < episodeOutline.value.length - 1) {
    currentEpisodeIndex.value++
  } else {
    // All episodes reviewed, move to save step
    activeStep.value = 2
  }
}

function goToPreviousEpisode() {
  if (currentEpisodeIndex.value > 0) {
    currentEpisodeIndex.value--
  }
}

async function saveAllEpisodes() {
  if (!allEpisodesGenerated.value) return

  isGenerating.value = true
  error.value = ''

  try {
    const episodesWithContent = Array.from(generatedEpisodes.value.values())

    await CourseService.saveModule(moduleId, {
      episodes: episodesWithContent,
      generateAudio: generateAudio.value
    })

    router.push({name: 'course-dashboard', params: {id: courseId}})
  } catch (e: any) {
    error.value = e.message || 'Failed to save episodes'
  } finally {
    isGenerating.value = false
  }
}

function nextStep() {
  if (activeStep.value === 1 && episodeOutline.value.length > 0) {
    // Moving to generate step, start with first episode
    currentEpisodeIndex.value = 0
  }
  activeStep.value++
}

function prevStep() {
  if (activeStep.value > 0) {
    activeStep.value--
  }
}

function getContentPreview(content: GeneratedEpisodeContent) {
  if (content.dialogue) {
    return content.dialogue.lines.map(line => `${line.speaker}: ${line.text}`).join('\n')
  } else if (content.story) {
    return content.story
  }
  return ''
}
</script>

<template>
  <div class="episode-wizard p-xl max-w-6xl mx-auto">
    <div class="mb-xl">
      <h1 class="text-3xl font-bold mb-sm">Episode Content Generation</h1>
      <p class="text-secondary" v-if="module && course">
        Generating episodes for <strong>{{ module.title }}</strong> ({{ course.name }})
      </p>
    </div>

    <Steps :model="steps" :activeStep="activeStep" class="mb-xl"/>

    <div v-if="loading" class="flex justify-center p-xl">
      <ProgressSpinner/>
    </div>

    <div v-else-if="error && !currentGeneratedContent">
      <Message severity="error">{{ error }}</Message>
      <Button label="Retry" icon="pi pi-refresh" @click="loadData" class="mt-md"/>
    </div>

    <div v-else>
      <!-- Step 0: Review Module Plan -->
      <div v-if="activeStep === 0">
        <h2 class="text-2xl font-bold mb-lg">Module Plan</h2>

        <Card class="mb-xl bg-surface-card">
          <template #title>
            <div class="flex items-center justify-between">
              <div>Module {{ module?.moduleNumber }}: {{ module?.title }}</div>
              <Tag :value="module?.theme" severity="info"/>
            </div>
          </template>
          <template #content>
            <div class="space-y-lg">
              <div v-if="module?.description">
                <h4 class="font-bold text-sm uppercase text-secondary mb-xs">Description</h4>
                <p class="m-0 text-secondary">{{ module.description }}</p>
              </div>

              <div v-if="module?.objectives && module.objectives.length > 0">
                <h4 class="font-bold text-sm uppercase text-secondary mb-xs">Learning Objectives</h4>
                <ul class="list-disc pl-lg m-0">
                  <li v-for="(obj, idx) in module.objectives" :key="idx" class="text-sm">
                    {{ obj }}
                  </li>
                </ul>
              </div>

              <div class="grid grid-cols-2 gap-md"
                   v-if="(module?.vocabularyFocus && module.vocabularyFocus.length > 0) || (module?.grammarFocus && module.grammarFocus.length > 0)">
                <div v-if="module?.vocabularyFocus && module.vocabularyFocus.length > 0">
                  <h4 class="font-bold text-sm uppercase text-secondary mb-xs">Vocabulary Focus</h4>
                  <div class="flex flex-wrap gap-xs">
                    <span
                        v-for="(vocab, idx) in module.vocabularyFocus"
                        :key="idx"
                        class="px-sm py-xs bg-blue-100 text-blue-800 rounded text-xs"
                    >
                      {{ vocab }}
                    </span>
                  </div>
                </div>

                <div v-if="module?.grammarFocus && module.grammarFocus.length > 0">
                  <h4 class="font-bold text-sm uppercase text-secondary mb-xs">Grammar Focus</h4>
                  <div class="flex flex-wrap gap-xs">
                    <span
                        v-for="(grammar, idx) in module.grammarFocus"
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

        <h3 class="text-xl font-bold mb-md">Episode Outline ({{ episodeOutline.length }} episodes)</h3>
        <div class="grid gap-md mb-xl">
          <Card v-for="episode in episodeOutline" :key="episode.episodeNumber" class="bg-surface-card">
            <template #title>
              <div class="flex items-center gap-sm">
                <span
                    class="bg-primary text-primary-contrast rounded-full w-8 h-8 flex items-center justify-center text-sm font-bold">
                  {{ episode.episodeNumber }}
                </span>
                <span>{{ episode.title }}</span>
                <Tag :value="episode.type" severity="secondary" class="ml-auto"/>
              </div>
            </template>
            <template #content>
              <p class="m-0 text-secondary text-sm">{{ episode.summary }}</p>
            </template>
          </Card>
        </div>

        <div class="flex justify-end">
          <Button
              label="Next: Generate Episodes"
              icon="pi pi-arrow-right"
              @click="nextStep"
              :disabled="!episodeOutline || episodeOutline.length === 0"
          />
        </div>
      </div>

      <!-- Step 1: Generate & Review Episodes -->
      <div v-if="activeStep === 1">
        <h2 class="text-2xl font-bold mb-lg">Generate & Review Episodes</h2>

        <div class="mb-lg">
          <ProgressBar :value="Math.round((currentEpisodeIndex / episodeOutline.length) * 100)" class="mb-sm"/>
          <p class="text-sm text-secondary text-center">
            Episode {{ currentEpisodeIndex + 1 }} of {{ episodeOutline.length }}
          </p>
        </div>

        <div v-if="currentEpisode">
          <!-- Episode Info -->
          <Card class="mb-lg">
            <template #title>
              <div class="flex items-center justify-between">
                <div>Episode {{ currentEpisode.episodeNumber }}: {{ currentEpisode.title }}</div>
                <Tag :value="currentEpisode.type" severity="info"/>
              </div>
            </template>
            <template #content>
              <p class="text-secondary">{{ currentEpisode.summary }}</p>
            </template>
          </Card>

          <!-- Generate or Show Content -->
          <div v-if="!currentGeneratedContent" class="text-center p-xl">
            <p class="mb-lg text-lg">Ready to generate content for this episode?</p>
            <Button
                label="Generate Episode"
                icon="pi pi-bolt"
                @click="generateCurrentEpisode"
                severity="success"
                size="large"
                :loading="isGenerating"
            />
          </div>

          <div v-else>
            <Message v-if="error" severity="error" class="mb-lg" @close="error = ''">{{ error }}</Message>

            <!-- Show Generated Content -->
            <Card class="mb-lg">
              <template #title>Generated Content</template>
              <template #content>
                <div class="flex flex-col gap-lg">
                  <div>
                    <label class="block font-bold text-sm mb-sm text-secondary">{{
                        currentEpisode.type === 'DIALOGUE' ? 'Dialogue' : 'Story'
                      }}</label>
                    <Textarea
                        :value="getContentPreview(currentGeneratedContent.content)"
                        rows="20"
                        class="w-full font-mono text-sm"
                        readonly
                    />
                    <small class="block mt-sm text-secondary">
                      Review the content above. Check for issues like unwanted "Narrator:" lines in dialogues.
                    </small>
                  </div>

                  <div>
                    <label class="block font-bold text-sm mb-sm text-secondary">Exercises Generated</label>
                    <p class="text-sm text-secondary">{{ currentGeneratedContent.content.exercises?.length || 0 }}
                      exercises created</p>
                  </div>
                </div>
              </template>
            </Card>

            <!-- Action Buttons -->
            <div class="flex justify-between items-center">
              <div class="flex gap-sm">
                <Button
                    v-if="currentEpisodeIndex > 0"
                    label="Previous"
                    icon="pi pi-arrow-left"
                    text
                    @click="goToPreviousEpisode"
                />
              </div>

              <div class="flex gap-sm">
                <Button
                    label="Regenerate"
                    icon="pi pi-refresh"
                    severity="secondary"
                    @click="regenerateCurrentEpisode"
                    :loading="isGenerating"
                />
                <Button
                    :label="currentEpisodeIndex < episodeOutline.length - 1 ? 'Approve & Next' : 'Approve & Finish'"
                    icon="pi pi-check"
                    severity="success"
                    @click="approveAndNext"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-start mt-lg">
          <Button label="Back to Plan" icon="pi pi-arrow-left" text @click="prevStep"/>
        </div>
      </div>

      <!-- Step 2: Save Episodes -->
      <div v-if="activeStep === 2">
        <h2 class="text-2xl font-bold mb-lg">Save Episodes</h2>

        <Message severity="success" class="mb-lg">
          All {{ episodeOutline.length }} episodes have been reviewed and approved!
        </Message>

        <Card class="mb-xl">
          <template #title>Summary</template>
          <template #content>
            <div class="grid gap-sm">
              <div v-for="(episode, idx) in Array.from(generatedEpisodes.values())" :key="idx"
                   class="flex items-center gap-sm p-sm bg-surface-ground rounded">
                <i class="pi pi-check-circle text-green-500"></i>
                <span class="font-bold">Episode {{ episode.episodeNumber }}:</span>
                <span>{{ episode.title }}</span>
                <Tag :value="episode.type" severity="info" class="ml-auto"/>
              </div>
            </div>
          </template>
        </Card>

        <div class="text-center p-xl">
          <div class="flex items-center justify-center gap-sm mb-xl">
            <Checkbox v-model="generateAudio" :binary="true" inputId="generateAudio"/>
            <label for="generateAudio" class="cursor-pointer">Generate audio for dialogue episodes</label>
          </div>

          <Button
              label="Save All Episodes"
              icon="pi pi-save"
              @click="saveAllEpisodes"
              severity="success"
              size="large"
              :loading="isGenerating"
          />

          <p v-if="generateAudio" class="text-sm text-secondary mt-md">
            Note: Audio generation may take several minutes
          </p>
        </div>

        <div class="flex justify-start">
          <Button label="Back to Review" icon="pi pi-arrow-left" text @click="activeStep = 1"/>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.space-y-lg > * + * {
  margin-top: 1.5rem;
}
</style>
