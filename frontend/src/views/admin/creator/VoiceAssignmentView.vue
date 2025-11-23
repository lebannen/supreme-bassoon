<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {CourseService} from '@/services/CourseService'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Dropdown from 'primevue/dropdown'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'

const router = useRouter()
const route = useRoute()

const courseId = ref<number>(Number(route.params.courseId))

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
  modules: any[]
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

interface VoiceAssignment {
  characterName: string
  voiceName: string
  gender: string | null
}

const generationResult = ref<GenerateCourseContentResponse | null>(null)
const loading = ref(true)

onMounted(() => {
  // Load generation result from sessionStorage
  const stored = sessionStorage.getItem('generationResult')
  if (stored) {
    try {
      generationResult.value = JSON.parse(stored)
      initializeVoiceAssignments()
    } catch (e) {
      console.error('Failed to parse generation result:', e)
    }
  }
  loading.value = false
})

// Available Gemini TTS voices
const availableVoices = [
  {label: 'Puck (Neutral)', value: 'Puck', gender: 'neutral'},
  {label: 'Charon (Neutral)', value: 'Charon', gender: 'neutral'},
  {label: 'Kore (Neutral)', value: 'Kore', gender: 'neutral'},
  {label: 'Fenrir (Neutral)', value: 'Fenrir', gender: 'neutral'},
  {label: 'Aoede (Female)', value: 'Aoede', gender: 'female'},
  {label: 'Leda (Female)', value: 'Leda', gender: 'female'},
  {label: 'Juno (Female)', value: 'Juno', gender: 'female'},
]

// Voice assignments
const voiceAssignments = ref<Map<string, VoiceAssignment>>(new Map())

// Initialize default assignments when generation result is loaded
function initializeVoiceAssignments() {
  if (!generationResult.value?.characterAnalysis?.characters) return

  generationResult.value.characterAnalysis.characters.forEach((character, index) => {
    // Alternate between different voices
    const voice = availableVoices[index % availableVoices.length]
    voiceAssignments.value.set(character.name, {
      characterName: character.name,
      voiceName: voice.value,
      gender: voice.gender
    })
  })
}

const characters = computed(() => {
  if (!generationResult.value?.characterAnalysis?.characters) return []
  return generationResult.value.characterAnalysis.characters.map(char => ({
    ...char,
    assignment: voiceAssignments.value.get(char.name)
  }))
})

const saving = ref(false)
const error = ref('')

function updateVoiceAssignment(characterName: string, voiceName: string) {
  const voice = availableVoices.find(v => v.value === voiceName)
  if (voice) {
    voiceAssignments.value.set(characterName, {
      characterName,
      voiceName: voice.value,
      gender: voice.gender
    })
  }
}

function getEpisodesList(character: CharacterInfo): string {
  return character.episodes
      .map(ep => `M${ep.moduleNumber}E${ep.episodeNumber} (${ep.lineCount} lines)`)
      .join(', ')
}

async function saveAndGenerateAudio() {
  if (!generationResult.value) return

  saving.value = true
  error.value = ''

  try {
    // Prepare save request
    const saveRequest = {
      courseId: courseId.value,
      modules: generationResult.value.modules
          .filter(module => module.episodes.length > 0)
          .map(module => ({
            moduleId: module.moduleId,
            episodes: module.episodes
                .filter(ep => ep.validation.isValid && ep.content !== null)
                .map(ep => ({
                  episodeNumber: ep.episodeNumber,
                  title: ep.title,
                  type: ep.type,
                  summary: ep.content.dialogue?.lines[0]?.text.substring(0, 100) ||
                      ep.content.story?.substring(0, 100) ||
                      '',
                  content: ep.content
                }))
          }))
          .filter(module => module.episodes.length > 0),
      voiceAssignments: Array.from(voiceAssignments.value.values()),
      generateAudio: true
    }

    await CourseService.saveCourseContent(saveRequest)

    // Clear session storage
    sessionStorage.removeItem('generationResult')

    // Success - navigate back to course dashboard
    router.push({
      name: 'course-dashboard',
      params: {id: courseId.value}
    })
  } catch (e: any) {
    error.value = e.message || 'Failed to save course content'
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.back()
}
</script>

<template>
  <div class="voice-assignment p-xl max-w-7xl mx-auto">
    <div class="mb-xl">
      <div class="flex items-center justify-between mb-md">
        <div>
          <h1 class="text-3xl font-bold">Voice Assignment</h1>
          <p class="text-secondary">Assign Gemini TTS voices to characters for audio generation</p>
        </div>
        <Button label="Back" icon="pi pi-arrow-left" text @click="goBack"/>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center p-xl">
      <ProgressSpinner/>
    </div>

    <Message v-else-if="!generationResult" severity="warn" :closable="false" class="mb-lg">
      No generation result found. Please go back and generate content first.
    </Message>

    <div v-else>
      <Message v-if="error" severity="error" :closable="false" class="mb-lg">{{ error }}</Message>

      <!-- Summary Card -->
      <Card class="mb-lg">
        <template #title>Summary</template>
        <template #content>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-lg">
            <div>
              <div class="text-3xl font-bold text-primary">
                {{ characters.length }}
              </div>
              <div class="text-sm text-secondary mt-xs">Characters to Assign</div>
            </div>
            <div>
              <div class="text-3xl font-bold text-green-600">
                {{ generationResult?.validationSummary?.validEpisodes || 0 }}
              </div>
              <div class="text-sm text-secondary mt-xs">Valid Episodes</div>
            </div>
            <div>
              <div class="text-3xl font-bold text-blue-600">
                {{ generationResult?.characterAnalysis?.totalDialogues || 0 }}
              </div>
              <div class="text-sm text-secondary mt-xs">Total Dialogue Lines</div>
            </div>
          </div>
        </template>
      </Card>

      <!-- Voice Assignment Table -->
      <Card>
        <template #title>Character Voice Assignments</template>
        <template #content>
          <DataTable :value="characters" stripedRows class="p-datatable-sm">
            <Column field="name" header="Character" sortable>
              <template #body="slotProps">
                <strong>{{ slotProps.data.name }}</strong>
              </template>
            </Column>

            <Column field="appearances" header="Lines" sortable style="width: 8rem">
              <template #body="slotProps">
                <Tag :value="`${slotProps.data.appearances} lines`" severity="info"/>
              </template>
            </Column>

            <Column header="Episodes" style="min-width: 15rem">
              <template #body="slotProps">
                <div class="text-sm">{{ getEpisodesList(slotProps.data) }}</div>
              </template>
            </Column>

            <Column header="Voice" style="width: 15rem">
              <template #body="slotProps">
                <Dropdown
                    :modelValue="slotProps.data.assignment?.voiceName"
                    @update:modelValue="(value) => updateVoiceAssignment(slotProps.data.name, value)"
                    :options="availableVoices"
                    optionLabel="label"
                    optionValue="value"
                    placeholder="Select a voice"
                    class="w-full"
                />
              </template>
            </Column>

            <Column header="Gender" style="width: 8rem">
              <template #body="slotProps">
                <Tag
                    v-if="slotProps.data.assignment?.gender"
                    :value="slotProps.data.assignment.gender"
                    :severity="slotProps.data.assignment.gender === 'female' ? 'info' : 'secondary'"
                />
              </template>
            </Column>
          </DataTable>

          <div class="flex justify-between items-center mt-lg pt-lg border-t border-surface">
            <div class="text-sm text-secondary">
              <i class="pi pi-info-circle mr-xs"></i>
              All valid episodes will be saved and audio will be generated using assigned voices
            </div>
            <div class="flex gap-md">
              <Button
                  label="Cancel"
                  severity="secondary"
                  @click="goBack"
                  :disabled="saving"
              />
              <Button
                  label="Save & Generate Audio"
                  icon="pi pi-check"
                  severity="success"
                  @click="saveAndGenerateAudio"
                  :loading="saving"
              />
            </div>
          </div>
        </template>
      </Card>
    </div>

    <!-- Saving Overlay -->
    <div
        v-if="saving"
        class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
    >
      <Card class="p-xl">
        <template #content>
          <div class="flex flex-col items-center gap-lg">
            <ProgressSpinner/>
            <div class="text-center">
              <h3 class="text-xl font-bold mb-sm">Saving Course Content</h3>
              <p class="text-secondary">Generating audio for all episodes... This may take several minutes.</p>
            </div>
          </div>
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
</style>
