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
import Image from 'primevue/image'

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

interface CharacterProfile {
  name: string
  description: string
  referenceImageUrl: string
  appearanceDetails: string
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
  characterProfiles: CharacterProfile[]
}

interface VoiceAssignment {
  characterName: string
  voiceName: string
  gender: string | null  // Kept for API compatibility
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

// Available Gemini TTS voices - organized by pitch
const availableVoices = [
  // Higher pitch voices
  {label: 'Zephyr (Bright, Higher)', value: 'Zephyr', pitch: 'higher'},
  {label: 'Leda (Youthful, Higher)', value: 'Leda', pitch: 'higher'},
  {label: 'Laomedeia (Upbeat, Higher)', value: 'Laomedeia', pitch: 'higher'},
  {label: 'Achernar (Soft, Higher)', value: 'Achernar', pitch: 'higher'},

  // Middle pitch voices
  {label: 'Puck (Upbeat, Middle)', value: 'Puck', pitch: 'middle'},
  {label: 'Kore (Firm, Middle)', value: 'Kore', pitch: 'middle'},
  {label: 'Aoede (Breezy, Middle)', value: 'Aoede', pitch: 'middle'},
  {label: 'Callirrhoe (Easy-going, Middle)', value: 'Callirrhoe', pitch: 'middle'},
  {label: 'Autonoe (Bright, Middle)', value: 'Autonoe', pitch: 'middle'},
  {label: 'Despina (Smooth, Middle)', value: 'Despina', pitch: 'middle'},
  {label: 'Erinome (Clear, Middle)', value: 'Erinome', pitch: 'middle'},
  {label: 'Rasalgethi (Informative, Middle)', value: 'Rasalgethi', pitch: 'middle'},
  {label: 'Gacrux (Mature, Middle)', value: 'Gacrux', pitch: 'middle'},
  {label: 'Pulcherrima (Forward, Middle)', value: 'Pulcherrima', pitch: 'middle'},
  {label: 'Vindemiatrix (Gentle, Middle)', value: 'Vindemiatrix', pitch: 'middle'},
  {label: 'Sadaltager (Knowledgeable, Middle)', value: 'Sadaltager', pitch: 'middle'},
  {label: 'Sulafat (Warm, Middle)', value: 'Sulafat', pitch: 'middle'},

  // Lower middle pitch voices
  {label: 'Fenrir (Excitable, Lower middle)', value: 'Fenrir', pitch: 'lower-middle'},
  {label: 'Orus (Firm, Lower middle)', value: 'Orus', pitch: 'lower-middle'},
  {label: 'Iapetus (Clear, Lower middle)', value: 'Iapetus', pitch: 'lower-middle'},
  {label: 'Umbriel (Easy-going, Lower middle)', value: 'Umbriel', pitch: 'lower-middle'},
  {label: 'Alnilam (Firm, Lower middle)', value: 'Alnilam', pitch: 'lower-middle'},
  {label: 'Schedar (Even, Lower middle)', value: 'Schedar', pitch: 'lower-middle'},
  {label: 'Achird (Friendly, Lower middle)', value: 'Achird', pitch: 'lower-middle'},
  {label: 'Zubenelgenubi (Casual, Lower middle)', value: 'Zubenelgenubi', pitch: 'lower-middle'},

  // Lower pitch voices
  {label: 'Charon (Informative, Lower)', value: 'Charon', pitch: 'lower'},
  {label: 'Enceladus (Breathy, Lower)', value: 'Enceladus', pitch: 'lower'},
  {label: 'Algieba (Smooth, Lower)', value: 'Algieba', pitch: 'lower'},
  {label: 'Algenib (Gravelly, Lower)', value: 'Algenib', pitch: 'lower'},
  {label: 'Sadachbia (Lively, Lower)', value: 'Sadachbia', pitch: 'lower'},
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
      gender: null  // Voices are neutral/can be used for any character
    })
  })
}

const characters = computed(() => {
  if (!generationResult.value?.characterAnalysis?.characters) return []
  return generationResult.value.characterAnalysis.characters.map(char => {
    const profile = generationResult.value?.characterProfiles?.find(p => p.name === char.name)
    return {
      ...char,
      assignment: voiceAssignments.value.get(char.name),
      profile: profile
    }
  })
})

const saving = ref(false)
const error = ref('')

function updateVoiceAssignment(characterName: string, voiceName: string) {
  const voice = availableVoices.find(v => v.value === voiceName)
  if (voice) {
    voiceAssignments.value.set(characterName, {
      characterName,
      voiceName: voice.value,
      gender: null  // Voices are neutral/can be used for any character
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
      characterProfiles: generationResult.value.characterProfiles || [],
      generateAudio: true,
      generateImages: true
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
            <Column header="Reference" style="width: 6rem">
              <template #body="slotProps">
                <div v-if="slotProps.data.profile?.referenceImageUrl" class="character-image-cell">
                  <Image
                      :src="slotProps.data.profile.referenceImageUrl"
                      :alt="slotProps.data.name"
                      preview
                      class="character-reference-image"
                      imageClass="character-reference-image-img"
                  />
                </div>
                <div v-else class="character-image-placeholder">
                  <i class="pi pi-user text-2xl text-secondary"></i>
                </div>
              </template>
            </Column>

            <Column field="name" header="Character" sortable>
              <template #body="slotProps">
                <div>
                  <strong class="block">{{ slotProps.data.name }}</strong>
                  <small v-if="slotProps.data.profile?.description" class="text-secondary block mt-xs">
                    {{ slotProps.data.profile.description }}
                  </small>
                </div>
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

            <Column header="Pitch" style="width: 10rem">
              <template #body="slotProps">
                <Tag
                    v-if="slotProps.data.assignment?.voiceName"
                    :value="availableVoices.find(v => v.value === slotProps.data.assignment.voiceName)?.pitch || ''"
                    :severity="availableVoices.find(v => v.value === slotProps.data.assignment.voiceName)?.pitch === 'higher' ? 'info' : availableVoices.find(v => v.value === slotProps.data.assignment.voiceName)?.pitch === 'lower' ? 'contrast' : 'secondary'"
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

.character-image-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.character-reference-image {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
}

.character-reference-image:hover {
  transform: scale(1.05);
}

.character-reference-image :deep(.character-reference-image-img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.character-image-placeholder {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: var(--surface-ground);
  border: 2px dashed var(--surface-border);
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
