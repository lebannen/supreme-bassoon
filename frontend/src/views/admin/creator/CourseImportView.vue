<script setup lang="ts">
import {ref, onMounted, computed} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {CourseService} from '@/services/CourseService'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import Tag from 'primevue/tag'
import Panel from 'primevue/panel'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import ProgressBar from 'primevue/progressbar'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.courseId)

interface ValidationIssue {
  field: string
  message: string
  severity: string
}

interface EpisodeGenerationResult {
  episodeNumber: number
  title: string
  type: string
  content: any
  validation: {
    isValid: boolean
    issues: ValidationIssue[]
    hasErrors: boolean
    hasWarnings: boolean
  }
}

interface ModuleGenerationResult {
  moduleId: number
  moduleNumber: number
  title: string
  episodes: EpisodeGenerationResult[]
  validCount: number
  totalCount: number
}

interface CharacterInfo {
  name: string
  appearances: number
  episodes: Array<{
    moduleNumber: number
    episodeNumber: number
    lineCount: number
  }>
}

interface GenerateCourseContentResponse {
  modules: ModuleGenerationResult[]
  characterAnalysis: {
    characters: CharacterInfo[]
    totalDialogues: number
  }
  validationSummary: {
    totalEpisodes: number
    validEpisodes: number
    episodesWithWarnings: number
    episodesWithErrors: number
  }
}

const generationResult = ref<GenerateCourseContentResponse | null>(null)
const loading = ref(false)
const error = ref('')
const generating = ref(false)

const progressPercent = computed(() => {
  if (!generationResult.value) return 0
  const summary = generationResult.value.validationSummary
  return summary.totalEpisodes > 0
      ? Math.round((summary.validEpisodes / summary.totalEpisodes) * 100)
      : 0
})

onMounted(async () => {
  // Check if we already have generation results (from navigation state)
  if (route.params.generated) {
    // Load from history state if available
  }
})

async function startGeneration() {
  generating.value = true
  error.value = ''

  try {
    generationResult.value = await CourseService.generateAllCourseContent(courseId)
  } catch (e: any) {
    error.value = e.message || 'Failed to generate course content'
  } finally {
    generating.value = false
  }
}

function getSeverityColor(severity: string) {
  switch (severity) {
    case 'CRITICAL':
    case 'ERROR':
      return 'danger'
    case 'WARNING':
      return 'warning'
    default:
      return 'info'
  }
}

function getValidationStatusColor(episode: EpisodeGenerationResult) {
  if (!episode.validation.isValid) return 'danger'
  if (episode.validation.hasWarnings) return 'warning'
  return 'success'
}

function getValidationStatusIcon(episode: EpisodeGenerationResult) {
  if (!episode.validation.isValid) return 'pi pi-times-circle'
  if (episode.validation.hasWarnings) return 'pi pi-exclamation-triangle'
  return 'pi pi-check-circle'
}

function goBack() {
  router.push({name: 'course-dashboard', params: {id: courseId}})
}

function proceedToVoiceAssignment() {
  if (!generationResult.value) return

  // Store the generation result in sessionStorage to pass to next view
  sessionStorage.setItem('generationResult', JSON.stringify(generationResult.value))

  router.push({
    name: 'voice-assignment',
    params: {
      courseId: courseId
    }
  })
}
</script>

<template>
  <div class="course-import p-xl max-w-7xl mx-auto">
    <div class="mb-xl">
      <div class="flex items-center justify-between mb-md">
        <h1 class="text-3xl font-bold">Course Content Generation</h1>
        <Button label="Back to Dashboard" icon="pi pi-arrow-left" text @click="goBack"/>
      </div>
    </div>

    <!-- Start Generation -->
    <div v-if="!generationResult && !generating" class="text-center p-xl">
      <Card>
        <template #content>
          <div class="space-y-lg">
            <i class="pi pi-sparkles text-6xl text-primary"></i>
            <h2 class="text-2xl font-bold">Generate All Course Content</h2>
            <p class="text-secondary">
              This will generate content for all episodes in all modules. Each episode will be validated
              and you'll see a detailed report before saving.
            </p>
            <Button
                label="Start Generation"
                icon="pi pi-play"
                size="large"
                @click="startGeneration"
            />
          </div>
        </template>
      </Card>
    </div>

    <!-- Generating -->
    <div v-if="generating" class="flex flex-col items-center justify-center p-xl">
      <ProgressSpinner/>
      <p class="mt-lg text-lg">Generating course content... This may take several minutes.</p>
    </div>

    <!-- Error -->
    <div v-if="error">
      <Message severity="error" :closable="false">{{ error }}</Message>
      <Button label="Retry" icon="pi pi-refresh" @click="startGeneration" class="mt-md"/>
    </div>

    <!-- Results -->
    <div v-if="generationResult" class="space-y-lg">
      <!-- Summary Card -->
      <Card>
        <template #title>Generation Summary</template>
        <template #content>
          <div class="grid grid-cols-1 md:grid-cols-4 gap-lg mb-lg">
            <div class="text-center">
              <div class="text-3xl font-bold text-primary">
                {{ generationResult.validationSummary.totalEpisodes }}
              </div>
              <div class="text-sm text-secondary mt-xs">Total Episodes</div>
            </div>
            <div class="text-center">
              <div class="text-3xl font-bold text-green-600">
                {{ generationResult.validationSummary.validEpisodes }}
              </div>
              <div class="text-sm text-secondary mt-xs">Valid</div>
            </div>
            <div class="text-center">
              <div class="text-3xl font-bold text-orange-600">
                {{ generationResult.validationSummary.episodesWithWarnings }}
              </div>
              <div class="text-sm text-secondary mt-xs">Warnings</div>
            </div>
            <div class="text-center">
              <div class="text-3xl font-bold text-red-600">
                {{ generationResult.validationSummary.episodesWithErrors }}
              </div>
              <div class="text-sm text-secondary mt-xs">Errors</div>
            </div>
          </div>

          <ProgressBar
              :value="progressPercent"
              :showValue="true"
              class="mb-md"
          />

          <div class="flex justify-end gap-md">
            <Button
                label="Regenerate"
                icon="pi pi-refresh"
                severity="secondary"
                @click="startGeneration"
            />
            <Button
                label="Proceed to Voice Assignment"
                icon="pi pi-arrow-right"
                iconPos="right"
                @click="proceedToVoiceAssignment"
                :disabled="generationResult.validationSummary.validEpisodes === 0"
            />
          </div>
        </template>
      </Card>

      <!-- Character Analysis -->
      <Card v-if="generationResult.characterAnalysis.characters.length > 0">
        <template #title>
          Characters Found ({{ generationResult.characterAnalysis.characters.length }})
        </template>
        <template #content>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-md">
            <div
                v-for="character in generationResult.characterAnalysis.characters"
                :key="character.name"
                class="p-md border border-surface rounded"
            >
              <div class="flex items-center justify-between mb-sm">
                <div class="font-bold text-lg">{{ character.name }}</div>
                <Tag :value="`${character.appearances} lines`" severity="info"/>
              </div>
              <div class="text-sm text-secondary">
                Appears in {{ character.episodes.length }} episode(s)
              </div>
            </div>
          </div>
        </template>
      </Card>

      <!-- Modules with Episodes -->
      <Card>
        <template #title>Generated Content by Module</template>
        <template #content>
          <Accordion :multiple="true" :activeIndex="[0]">
            <AccordionTab
                v-for="module in generationResult.modules"
                :key="module.moduleId"
            >
              <template #header>
                <div class="flex items-center justify-between w-full pr-md">
                  <span class="font-bold">
                    Module {{ module.moduleNumber }}: {{ module.title }}
                  </span>
                  <Tag
                      :value="`${module.validCount}/${module.totalCount} valid`"
                      :severity="module.validCount === module.totalCount ? 'success' : 'warning'"
                  />
                </div>
              </template>

              <div class="space-y-md">
                <Panel
                    v-for="episode in module.episodes"
                    :key="`${module.moduleId}-${episode.episodeNumber}`"
                    :toggleable="true"
                >
                  <template #header>
                    <div class="flex items-center gap-md w-full">
                      <i
                          :class="getValidationStatusIcon(episode)"
                          :style="{ color: `var(--${getValidationStatusColor(episode)}-500)` }"
                      ></i>
                      <span class="font-medium">
                        Episode {{ episode.episodeNumber }}: {{ episode.title }}
                      </span>
                      <Tag :value="episode.type" severity="info" class="ml-auto"/>
                    </div>
                  </template>

                  <div class="space-y-md">
                    <!-- Validation Issues -->
                    <div v-if="episode.validation.issues.length > 0">
                      <h4 class="font-bold mb-sm">Validation Issues:</h4>
                      <div class="space-y-xs">
                        <Message
                            v-for="(issue, idx) in episode.validation.issues"
                            :key="idx"
                            :severity="getSeverityColor(issue.severity)"
                            :closable="false"
                        >
                          <strong>{{ issue.field }}:</strong> {{ issue.message }}
                        </Message>
                      </div>
                    </div>

                    <!-- Content Preview -->
                    <div v-if="episode.content">
                      <h4 class="font-bold mb-sm">Content:</h4>

                      <!-- Dialogue Preview -->
                      <div v-if="episode.type === 'DIALOGUE' && episode.content.dialogue">
                        <div class="text-sm text-secondary mb-xs">
                          {{ episode.content.dialogue.lines.length }} dialogue lines
                        </div>
                        <div class="bg-surface-ground p-md rounded max-h-60 overflow-y-auto">
                          <div
                              v-for="(line, idx) in episode.content.dialogue.lines.slice(0, 5)"
                              :key="idx"
                              class="mb-sm"
                          >
                            <strong>{{ line.speaker }}:</strong> {{ line.text }}
                          </div>
                          <div v-if="episode.content.dialogue.lines.length > 5" class="text-secondary text-sm italic">
                            ... and {{ episode.content.dialogue.lines.length - 5 }} more lines
                          </div>
                        </div>
                      </div>

                      <!-- Story Preview -->
                      <div v-else-if="episode.type === 'STORY' && episode.content.story">
                        <div class="bg-surface-ground p-md rounded max-h-60 overflow-y-auto">
                          <p class="text-sm">{{ episode.content.story.substring(0, 300) }}...</p>
                        </div>
                      </div>

                      <!-- Exercise Count -->
                      <div v-if="episode.content.exercises" class="mt-md">
                        <Tag
                            :value="`${episode.content.exercises.length} exercises`"
                            severity="info"
                        />
                      </div>
                    </div>
                  </div>
                </Panel>
              </div>
            </AccordionTab>
          </Accordion>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
.space-y-lg > * + * {
  margin-top: 2rem;
}

.space-y-md > * + * {
  margin-top: 1rem;
}

.space-y-sm > * + * {
  margin-top: 0.5rem;
}

.space-y-xs > * + * {
  margin-top: 0.25rem;
}
</style>
