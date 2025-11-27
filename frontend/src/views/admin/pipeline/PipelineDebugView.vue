<script setup lang="ts">
import {ref, computed, onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useToast} from 'primevue/usetoast'
import Card from 'primevue/card'
import Button from 'primevue/button'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import Tag from 'primevue/tag'
import Fieldset from 'primevue/fieldset'
import Divider from 'primevue/divider'
import ProgressSpinner from 'primevue/progressspinner'
import PipelineService from '@/services/PipelineService'
import type {GenerationDebugResponse, StepStatus} from '@/types/pipeline'
import {STAGE_CONFIG, LANGUAGE_OPTIONS} from '@/types/pipeline'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const generationId = computed(() => route.params.id as string)
const loading = ref(true)
const data = ref<GenerationDebugResponse | null>(null)
const expandedModules = ref<Set<string>>(new Set())
const expandedEpisodes = ref<Set<string>>(new Set())

onMounted(async () => {
  await loadDebugData()
})

async function loadDebugData() {
  loading.value = true
  try {
    data.value = await PipelineService.getDebugData(generationId.value)
    console.log('Debug data loaded:', data.value)
  } catch (error) {
    console.error('Failed to load debug data:', error)
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to load debug data',
      life: 3000
    })
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push({name: 'pipeline-detail', params: {id: generationId.value}})
}

function getLanguageLabel(code: string): string {
  return LANGUAGE_OPTIONS.find((l) => l.value === code)?.label || code
}

function getStatusSeverity(status: StepStatus): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'IN_PROGRESS':
      return 'info'
    case 'FAILED':
      return 'danger'
    default:
      return 'secondary'
  }
}

function formatJson(value: unknown): string {
  if (value === null || value === undefined) return 'null'

  // If it's already a string, try to parse and pretty-print it
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      return JSON.stringify(parsed, null, 2)
    } catch {
      return value
    }
  }

  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

function formatDate(dateString?: string): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}

function toggleModule(id: string) {
  if (expandedModules.value.has(id)) {
    expandedModules.value.delete(id)
  } else {
    expandedModules.value.add(id)
  }
}

function toggleEpisode(id: string) {
  if (expandedEpisodes.value.has(id)) {
    expandedEpisodes.value.delete(id)
  } else {
    expandedEpisodes.value.add(id)
  }
}

function isModuleExpanded(id: string): boolean {
  return expandedModules.value.has(id)
}

function isEpisodeExpanded(id: string): boolean {
  return expandedEpisodes.value.has(id)
}
</script>

<template>
  <div class="pipeline-debug">
    <div v-if="loading" class="flex justify-content-center p-8">
      <ProgressSpinner/>
    </div>

    <template v-else-if="data">
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
              <h2 class="m-0">Debug View: {{ data.blueprint?.courseTitle || 'Untitled' }}</h2>
              <div class="flex gap-3 mt-2 text-color-secondary">
                <span>
                  <i class="pi pi-globe mr-1"></i>
                  {{ getLanguageLabel(data.languageCode) }}
                </span>
                <span>
                  <i class="pi pi-chart-bar mr-1"></i>
                  {{ data.cefrLevel }}
                </span>
                <span>
                  <i class="pi pi-folder mr-1"></i>
                  {{ data.moduleCount }} modules
                </span>
                <span>
                  <i class="pi pi-cog mr-1"></i>
                  Stage: {{ STAGE_CONFIG[data.currentStage]?.label }}
                </span>
                <span v-if="data.autoMode">
                  <Tag value="Auto Mode" severity="info"/>
                </span>
              </div>
            </div>
            <Button
                icon="pi pi-refresh"
                label="Refresh"
                severity="secondary"
                @click="loadDebugData"
            />
          </div>
        </template>
      </Card>

      <!-- Main Content Tabs -->
      <TabView>
        <!-- Blueprint Tab -->
        <TabPanel header="Blueprint">
          <template v-if="data.blueprint">
            <div class="grid">
              <div class="col-12 lg:col-6">
                <Fieldset legend="Metadata" class="mb-3">
                  <div class="flex flex-column gap-2">
                    <div><strong>Title:</strong> {{ data.blueprint.courseTitle || '-' }}</div>
                    <div><strong>Description:</strong> {{ data.blueprint.courseDescription || '-' }}</div>
                    <div><strong>Setting:</strong> {{ data.blueprint.setting || '-' }}</div>
                    <div><strong>Premise:</strong> {{ data.blueprint.premise || '-' }}</div>
                    <div>
                      <strong>Status:</strong>
                      <Tag :value="data.blueprint.status" :severity="getStatusSeverity(data.blueprint.status)"
                           class="ml-2"/>
                    </div>
                    <div><strong>Created:</strong> {{ formatDate(data.blueprint.createdAt) }}</div>
                  </div>
                </Fieldset>
              </div>
              <div class="col-12 lg:col-6">
                <Fieldset legend="Plot Arc" class="mb-3">
                  <pre class="json-display">{{ formatJson(data.blueprint.plotArc) }}</pre>
                </Fieldset>
              </div>
              <div class="col-12 lg:col-6">
                <Fieldset legend="Module Topics" class="mb-3">
                  <pre class="json-display">{{ formatJson(data.blueprint.moduleTopics) }}</pre>
                </Fieldset>
              </div>
              <div class="col-12 lg:col-6">
                <Fieldset legend="Grammar Distribution" class="mb-3">
                  <pre class="json-display">{{ formatJson(data.blueprint.grammarDistribution) }}</pre>
                </Fieldset>
              </div>
              <div class="col-12" v-if="data.blueprint.rawResponse">
                <Fieldset legend="Raw AI Response" :toggleable="true" :collapsed="true">
                  <pre class="json-display raw-response">{{ data.blueprint.rawResponse }}</pre>
                </Fieldset>
              </div>
            </div>
          </template>
          <div v-else class="text-color-secondary p-4">No blueprint data available</div>
        </TabPanel>

        <!-- Module Plans Tab -->
        <TabPanel header="Module Plans">
          <template v-if="data.modulePlans && data.modulePlans.length > 0">
            <div v-for="module in data.modulePlans" :key="module.id" class="module-section mb-4">
              <div
                  class="module-header p-3 border-round cursor-pointer"
                  @click="toggleModule(module.id)"
              >
                <div class="flex align-items-center justify-content-between">
                  <div class="flex align-items-center gap-2">
                    <i :class="isModuleExpanded(module.id) ? 'pi pi-chevron-down' : 'pi pi-chevron-right'"></i>
                    <strong>Module {{ module.moduleNumber }}: {{ module.title || 'Untitled' }}</strong>
                    <Tag :value="module.status" :severity="getStatusSeverity(module.status)"/>
                  </div>
                  <span class="text-color-secondary">{{ module.episodes?.length || 0 }} episodes</span>
                </div>
              </div>

              <div v-if="isModuleExpanded(module.id)"
                   class="module-content p-3 border-1 border-top-none surface-ground border-round-bottom">
                <div class="grid">
                  <div class="col-12 lg:col-6">
                    <Fieldset legend="Module Info" class="mb-3">
                      <div class="flex flex-column gap-2">
                        <div><strong>Theme:</strong> {{ module.theme || '-' }}</div>
                        <div><strong>Description:</strong> {{ module.description || '-' }}</div>
                        <div><strong>Plot Summary:</strong> {{ module.plotSummary || '-' }}</div>
                      </div>
                    </Fieldset>
                  </div>
                  <div class="col-12 lg:col-6">
                    <Fieldset legend="Objectives" class="mb-3">
                      <pre class="json-display">{{ formatJson(module.objectives) }}</pre>
                    </Fieldset>
                  </div>
                  <div class="col-12" v-if="module.rawResponse">
                    <Fieldset legend="Raw AI Response" :toggleable="true" :collapsed="true">
                      <pre class="json-display raw-response">{{ module.rawResponse }}</pre>
                    </Fieldset>
                  </div>
                </div>

                <!-- Episodes -->
                <Divider/>
                <h4 class="mt-3 mb-3">Episodes</h4>

                <div v-for="episode in module.episodes" :key="episode.id" class="episode-section mb-3">
                  <div
                      class="episode-header p-2 border-round cursor-pointer surface-100"
                      @click="toggleEpisode(episode.id)"
                  >
                    <div class="flex align-items-center gap-2">
                      <i :class="isEpisodeExpanded(episode.id) ? 'pi pi-chevron-down' : 'pi pi-chevron-right'"></i>
                      <span>Episode {{ episode.episodeNumber }}: {{ episode.title || 'Untitled' }}</span>
                      <Tag :value="episode.episodeType" severity="secondary" class="text-xs"/>
                      <Tag :value="episode.status" :severity="getStatusSeverity(episode.status)"/>
                    </div>
                  </div>

                  <div v-if="isEpisodeExpanded(episode.id)"
                       class="episode-content p-3 border-1 border-top-none surface-50 border-round-bottom">
                    <div class="grid">
                      <div class="col-12 lg:col-6">
                        <Fieldset legend="Episode Info" class="mb-3">
                          <div class="flex flex-column gap-2">
                            <div><strong>Scene:</strong> {{ episode.sceneDescription || '-' }}</div>
                            <div><strong>Plot Points:</strong> {{ episode.plotPoints || '-' }}</div>
                          </div>
                        </Fieldset>
                      </div>
                      <div class="col-12 lg:col-6">
                        <Fieldset legend="Vocabulary" class="mb-3">
                          <pre class="json-display">{{ formatJson(episode.vocabulary) }}</pre>
                        </Fieldset>
                      </div>
                      <div class="col-12 lg:col-6">
                        <Fieldset legend="Grammar Rules" class="mb-3">
                          <pre class="json-display">{{ formatJson(episode.grammarRules) }}</pre>
                        </Fieldset>
                      </div>
                      <div class="col-12 lg:col-6">
                        <Fieldset legend="Character IDs" class="mb-3">
                          <pre class="json-display">{{ formatJson(episode.characterIds) }}</pre>
                        </Fieldset>
                      </div>

                      <!-- Episode Content -->
                      <div class="col-12" v-if="episode.content">
                        <Fieldset legend="Content" class="mb-3">
                          <div class="grid">
                            <div class="col-12">
                              <div class="flex flex-column gap-2 mb-3">
                                <div><strong>Type:</strong> {{ episode.content.contentType || '-' }}</div>
                                <div><strong>Summary:</strong> {{ episode.content.summary || '-' }}</div>
                                <div>
                                  <strong>Status:</strong>
                                  <Tag :value="episode.content.status"
                                       :severity="getStatusSeverity(episode.content.status)" class="ml-2"/>
                                </div>
                              </div>
                            </div>
                            <div class="col-12">
                              <Fieldset legend="Content Text" :toggleable="true" :collapsed="true" class="mb-2">
                                <pre class="json-display">{{ episode.content.content }}</pre>
                              </Fieldset>
                            </div>
                            <div class="col-12">
                              <Fieldset legend="Structured Content" :toggleable="true" :collapsed="true" class="mb-2">
                                <pre class="json-display">{{ formatJson(episode.content.contentStructured) }}</pre>
                              </Fieldset>
                            </div>
                            <div class="col-12 lg:col-6">
                              <Fieldset legend="Vocabulary Used" :toggleable="true" :collapsed="true" class="mb-2">
                                <pre class="json-display">{{ formatJson(episode.content.vocabularyUsed) }}</pre>
                              </Fieldset>
                            </div>
                            <div class="col-12 lg:col-6">
                              <Fieldset legend="Vocabulary Missing" :toggleable="true" :collapsed="true" class="mb-2">
                                <pre class="json-display">{{ formatJson(episode.content.vocabularyMissing) }}</pre>
                              </Fieldset>
                            </div>
                            <div class="col-12" v-if="episode.content.imagePrompts">
                              <Fieldset legend="Image Prompts" :toggleable="true" :collapsed="true" class="mb-2">
                                <pre class="json-display">{{ formatJson(episode.content.imagePrompts) }}</pre>
                              </Fieldset>
                            </div>
                            <div class="col-12" v-if="episode.content.rawResponse">
                              <Fieldset legend="Raw AI Response" :toggleable="true" :collapsed="true">
                                <pre class="json-display raw-response">{{ episode.content.rawResponse }}</pre>
                              </Fieldset>
                            </div>
                          </div>
                        </Fieldset>
                      </div>

                      <!-- Episode Exercises -->
                      <div class="col-12" v-if="episode.exercises">
                        <Fieldset legend="Exercises" class="mb-3">
                          <div class="grid">
                            <div class="col-12">
                              <div class="flex flex-column gap-2 mb-3">
                                <div><strong>Count:</strong> {{ episode.exercises.exerciseCount || 0 }}</div>
                                <div>
                                  <strong>Status:</strong>
                                  <Tag :value="episode.exercises.status"
                                       :severity="getStatusSeverity(episode.exercises.status)" class="ml-2"/>
                                </div>
                              </div>
                            </div>
                            <div class="col-12">
                              <Fieldset legend="Exercises Data" :toggleable="true" :collapsed="true" class="mb-2">
                                <pre class="json-display">{{ formatJson(episode.exercises.exercises) }}</pre>
                              </Fieldset>
                            </div>
                            <div class="col-12" v-if="episode.exercises.rawResponse">
                              <Fieldset legend="Raw AI Response" :toggleable="true" :collapsed="true">
                                <pre class="json-display raw-response">{{ episode.exercises.rawResponse }}</pre>
                              </Fieldset>
                            </div>
                          </div>
                        </Fieldset>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="text-color-secondary p-4">No module plans available</div>
        </TabPanel>

        <!-- Characters Tab -->
        <TabPanel header="Characters">
          <template v-if="data.characters && data.characters.length > 0">
            <div class="grid">
              <div class="col-12 md:col-6 lg:col-4" v-for="character in data.characters" :key="character.id">
                <Card>
                  <template #header v-if="character.referenceImageUrl">
                    <img :src="character.referenceImageUrl" :alt="character.name" class="character-image"/>
                  </template>
                  <template #title>{{ character.name }}</template>
                  <template #subtitle>{{ character.role }} - {{ character.gender || 'Unknown gender' }}</template>
                  <template #content>
                    <div class="flex flex-column gap-2">
                      <div><strong>Age:</strong> {{ character.ageRange || '-' }}</div>
                      <div><strong>Initial Description:</strong> {{ character.initialDescription || '-' }}</div>
                      <div><strong>Background:</strong> {{ character.background || '-' }}</div>
                      <div><strong>Appearance:</strong> {{ character.appearanceDescription || '-' }}</div>
                      <div><strong>Voice ID:</strong> {{ character.voiceId || '-' }}</div>
                      <div class="mt-2">
                        <strong>Personality Traits:</strong>
                        <pre class="json-display">{{ formatJson(character.personalityTraits) }}</pre>
                      </div>
                      <div v-if="character.developments">
                        <strong>Developments:</strong>
                        <pre class="json-display">{{ formatJson(character.developments) }}</pre>
                      </div>
                    </div>
                  </template>
                </Card>
              </div>
            </div>
          </template>
          <div v-else class="text-color-secondary p-4">No characters available</div>
        </TabPanel>

        <!-- Media Tab -->
        <TabPanel header="Media">
          <template v-if="data.media && data.media.length > 0">
            <div class="grid">
              <div class="col-12 md:col-6 lg:col-4" v-for="media in data.media" :key="media.id">
                <Card>
                  <template #title>
                    <div class="flex align-items-center gap-2">
                      <i :class="media.mediaType === 'EPISODE_AUDIO' ? 'pi pi-volume-up' : 'pi pi-image'"></i>
                      {{ media.mediaType }}
                    </div>
                  </template>
                  <template #content>
                    <div class="flex flex-column gap-2">
                      <div>
                        <strong>Status:</strong>
                        <Tag :value="media.status" :severity="getStatusSeverity(media.status)" class="ml-2"/>
                      </div>
                      <div v-if="media.episodePlanId"><strong>Episode ID:</strong> <span
                          class="text-xs">{{ media.episodePlanId }}</span></div>
                      <div v-if="media.characterId"><strong>Character ID:</strong> <span
                          class="text-xs">{{ media.characterId }}</span></div>
                      <div v-if="media.url">
                        <strong>URL:</strong>
                        <a :href="media.url" target="_blank" class="text-primary ml-1 text-xs">View</a>
                      </div>
                      <div v-if="media.errorMessage" class="text-red-500">
                        <strong>Error:</strong> {{ media.errorMessage }}
                      </div>
                      <div v-if="media.metadata">
                        <strong>Metadata:</strong>
                        <pre class="json-display">{{ formatJson(media.metadata) }}</pre>
                      </div>
                      <div><strong>Created:</strong> {{ formatDate(media.createdAt) }}</div>
                    </div>
                  </template>
                </Card>
              </div>
            </div>
          </template>
          <div v-else class="text-color-secondary p-4">No media available</div>
        </TabPanel>

        <!-- Raw Generation Info Tab -->
        <TabPanel header="Generation Info">
          <Fieldset legend="Generation Details">
            <div class="grid">
              <div class="col-12 md:col-6">
                <div class="flex flex-column gap-2">
                  <div><strong>ID:</strong> <span class="text-xs">{{ data.id }}</span></div>
                  <div><strong>Language:</strong> {{ getLanguageLabel(data.languageCode) }}</div>
                  <div><strong>CEFR Level:</strong> {{ data.cefrLevel }}</div>
                  <div><strong>Module Count:</strong> {{ data.moduleCount }}</div>
                  <div><strong>Episodes per Module:</strong> {{ data.episodesPerModule }}</div>
                  <div><strong>Auto Mode:</strong> {{ data.autoMode ? 'Yes' : 'No' }}</div>
                </div>
              </div>
              <div class="col-12 md:col-6">
                <div class="flex flex-column gap-2">
                  <div><strong>Current Stage:</strong> {{ STAGE_CONFIG[data.currentStage]?.label }}</div>
                  <div><strong>Created:</strong> {{ formatDate(data.createdAt) }}</div>
                  <div><strong>Completed:</strong> {{ formatDate(data.completedAt) }}</div>
                  <div v-if="data.themePreferences"><strong>Theme Preferences:</strong> {{ data.themePreferences }}
                  </div>
                  <div v-if="data.errorMessage" class="text-red-500">
                    <strong>Error:</strong> {{ data.errorMessage }}
                  </div>
                </div>
              </div>
            </div>
          </Fieldset>
        </TabPanel>
      </TabView>
    </template>
  </div>
</template>

<style scoped>
.pipeline-debug {
  padding: 1rem;
  max-width: 1600px;
  margin: 0 auto;
}

.json-display {
  background: var(--surface-100);
  border: 1px solid var(--surface-300);
  border-radius: 6px;
  padding: 0.75rem;
  margin: 0;
  font-size: 0.8rem;
  font-family: monospace;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
}

.raw-response {
  max-height: 500px;
  font-size: 0.75rem;
}

.character-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.module-header {
  background: var(--surface-100);
  border: 1px solid var(--surface-300);
}

.module-header:hover {
  background: var(--surface-200);
}

.episode-header:hover {
  background: var(--surface-200) !important;
}

:deep(.p-fieldset) {
  margin-bottom: 0;
}

:deep(.p-fieldset-legend) {
  font-size: 0.9rem;
  padding: 0.5rem 1rem;
}

:deep(.p-fieldset-content) {
  padding: 0.75rem;
}
</style>
