<script setup lang="ts">
import {ref, computed} from 'vue'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import ProgressBar from 'primevue/progressbar'
import type {ModulePlanDto} from '@/types/pipeline'

const props = defineProps<{
  modulePlans?: ModulePlanDto[]
  isCurrent: boolean
}>()

const selectedModule = ref(0)
const playingAudio = ref<string | null>(null)
const audioElement = ref<HTMLAudioElement | null>(null)

interface EpisodeWithAudio {
  id: string
  episodeNumber: number
  title: string
  moduleNumber: number
  hasAudio: boolean
  audioUrl?: string
}

const allEpisodes = computed((): EpisodeWithAudio[] => {
  if (!props.modulePlans) return []
  return props.modulePlans.flatMap((m) =>
      m.episodes.map((e) => ({
        id: e.id,
        episodeNumber: e.episodeNumber,
        title: e.title || `Episode ${e.episodeNumber}`,
        moduleNumber: m.moduleNumber,
        hasAudio: false, // We don't have audio info in the DTO yet
        audioUrl: undefined
      }))
  )
})

const mediaStats = computed(() => {
  // Since we don't have direct audio status in the DTO,
  // we'll estimate based on stage completion
  const total = allEpisodes.value.length
  return {
    completed: props.isCurrent ? 0 : total,
    total,
    inProgress: props.isCurrent
  }
})

const progressPercent = computed(() => {
  if (mediaStats.value.total === 0) return 0
  return Math.round((mediaStats.value.completed / mediaStats.value.total) * 100)
})

function playAudio(url: string) {
  if (playingAudio.value === url) {
    audioElement.value?.pause()
    playingAudio.value = null
    return
  }

  if (audioElement.value) {
    audioElement.value.pause()
  }

  audioElement.value = new Audio(url)
  audioElement.value.play()
  playingAudio.value = url

  audioElement.value.onended = () => {
    playingAudio.value = null
  }
}
</script>

<template>
  <Card class="mb-4">
    <template #title>
      <div class="flex align-items-center gap-2">
        <i class="pi pi-volume-up"></i>
        <span>Media Generation</span>
        <Tag
            v-if="isCurrent"
            value="Current Stage"
            severity="info"
            class="ml-2"
        />
        <Tag
            v-else-if="!isCurrent && modulePlans?.length"
            value="Completed"
            severity="success"
            class="ml-2"
        />
      </div>
    </template>

    <template #content>
      <div v-if="!modulePlans?.length" class="text-color-secondary">
        No module plans available yet.
      </div>

      <template v-else>
        <!-- Progress -->
        <div class="mb-4">
          <div class="flex justify-content-between mb-2">
            <span class="text-sm">Audio Generation Progress</span>
            <span class="text-sm">
              <template v-if="isCurrent">
                <i class="pi pi-spin pi-spinner mr-1"></i>
                Generating audio...
              </template>
              <template v-else>
                {{ mediaStats.completed }}/{{ mediaStats.total }} episodes
              </template>
            </span>
          </div>
          <ProgressBar
              :value="isCurrent ? undefined : progressPercent"
              :mode="isCurrent ? 'indeterminate' : 'determinate'"
          />
        </div>

        <!-- Info -->
        <div class="info-box mb-4">
          <i class="pi pi-info-circle mr-2"></i>
          <span>
            Audio is generated using AI text-to-speech with character-specific voices.
            Each episode's dialogue is converted to natural-sounding speech.
          </span>
        </div>

        <!-- Episodes by Module -->
        <TabView v-model:activeIndex="selectedModule">
          <TabPanel
              v-for="(module, idx) in modulePlans"
              :key="module.id"
              :value="idx"
              :header="`Module ${module.moduleNumber}`"
          >
            <DataTable :value="module.episodes" class="p-datatable-sm" stripedRows>
              <Column field="episodeNumber" header="#" style="width: 50px"/>
              <Column field="title" header="Episode">
                <template #body="{ data }">
                  {{ data.title || `Episode ${data.episodeNumber}` }}
                </template>
              </Column>
              <Column header="Characters">
                <template #body="{ data }">
                  <div class="flex gap-1 flex-wrap">
                    <Tag
                        v-for="char in data.characterNames?.slice(0, 3)"
                        :key="char"
                        :value="char"
                        severity="secondary"
                        size="small"
                    />
                    <span v-if="(data.characterNames?.length || 0) > 3" class="text-color-secondary text-sm">
                      +{{ data.characterNames.length - 3 }} more
                    </span>
                  </div>
                </template>
              </Column>
              <Column header="Status" style="width: 120px">
                <template #body>
                  <Tag
                      v-if="isCurrent"
                      value="Generating"
                      severity="info"
                      size="small"
                  >
                    <template #default>
                      <i class="pi pi-spin pi-spinner mr-1"></i>
                      Generating
                    </template>
                  </Tag>
                  <Tag
                      v-else
                      value="Ready"
                      severity="success"
                      size="small"
                  />
                </template>
              </Column>
            </DataTable>
          </TabPanel>
        </TabView>
      </template>
    </template>
  </Card>
</template>

<style scoped>
.info-box {
  display: flex;
  align-items: flex-start;
  padding: 1rem;
  background: var(--blue-50);
  border-radius: 8px;
  color: var(--blue-700);
  font-size: 0.9rem;
}

.info-box i {
  margin-top: 2px;
}
</style>
