<script setup lang="ts">
import {ref, computed} from 'vue'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import type {ModulePlanDto, EpisodePlanDto} from '@/types/pipeline'

const props = defineProps<{
  modulePlans?: ModulePlanDto[]
  isCurrent: boolean
}>()

const selectedModule = ref(0)

const allEpisodes = computed(() => {
  if (!props.modulePlans) return []
  return props.modulePlans.flatMap((m) =>
      m.episodes.map((e) => ({
        ...e,
        moduleNumber: m.moduleNumber,
        moduleTitle: m.title
      }))
  )
})

const episodesWithContent = computed(() => {
  return allEpisodes.value.filter((e) => e.content?.status === 'COMPLETED')
})

const progressText = computed(() => {
  const completed = episodesWithContent.value.length
  const total = allEpisodes.value.length
  return `${completed}/${total} episodes`
})

function formatDialogue(content: string): string[] {
  if (!content) return []
  return content.split('\n').filter((line) => line.trim())
}
</script>

<template>
  <Card class="mb-4">
    <template #title>
      <div class="flex align-items-center gap-2">
        <i class="pi pi-book"></i>
        <span>Episode Content</span>
        <Tag
            v-if="isCurrent"
            value="Current Stage"
            severity="info"
            class="ml-2"
        />
        <Tag
            v-else-if="episodesWithContent.length === allEpisodes.length"
            value="Completed"
            severity="success"
            class="ml-2"
        />
        <span class="text-color-secondary text-sm ml-auto">{{ progressText }}</span>
      </div>
    </template>

    <template #content>
      <div v-if="!modulePlans?.length" class="text-color-secondary">
        No module plans available yet.
      </div>

      <TabView v-else v-model:activeIndex="selectedModule">
        <TabPanel
            v-for="(module, idx) in modulePlans"
            :key="module.id"
            :value="idx"
            :header="`Module ${module.moduleNumber}`"
        >
          <Accordion :multiple="true">
            <AccordionTab
                v-for="episode in module.episodes"
                :key="episode.id"
            >
              <template #header>
                <div class="flex align-items-center gap-2 w-full">
                  <span>{{ episode.episodeNumber }}. {{ episode.title || 'Untitled' }}</span>
                  <Tag
                      :value="episode.content?.status || 'PENDING'"
                      :severity="episode.content?.status === 'COMPLETED' ? 'success' : 'secondary'"
                      size="small"
                      class="ml-auto"
                  />
                </div>
              </template>

              <div class="episode-detail">
                <!-- Episode Info -->
                <div class="episode-info mb-3">
                  <div class="flex gap-2 flex-wrap mb-2">
                    <Tag :value="episode.episodeType" severity="info" size="small"/>
                    <Tag
                        v-for="char in episode.characterNames"
                        :key="char"
                        :value="char"
                        severity="secondary"
                        size="small"
                    />
                  </div>
                  <p v-if="episode.sceneDescription" class="text-color-secondary text-sm">
                    {{ episode.sceneDescription }}
                  </p>
                </div>

                <!-- Vocabulary & Grammar -->
                <div class="flex gap-4 mb-3">
                  <div v-if="episode.vocabulary?.length" class="flex-1">
                    <h5 class="mb-2 text-sm">Vocabulary ({{ episode.vocabulary.length }})</h5>
                    <div class="vocab-list">
                      <Tag
                          v-for="word in episode.vocabulary"
                          :key="word"
                          :value="word"
                          severity="info"
                          size="small"
                          class="mr-1 mb-1"
                      />
                    </div>
                  </div>
                  <div v-if="episode.grammarRules?.length" class="flex-1">
                    <h5 class="mb-2 text-sm">Grammar Rules</h5>
                    <div class="grammar-list">
                      <Tag
                          v-for="rule in episode.grammarRules"
                          :key="rule"
                          :value="rule"
                          severity="warn"
                          size="small"
                          class="mr-1 mb-1"
                      />
                    </div>
                  </div>
                </div>

                <!-- Content -->
                <div v-if="episode.content?.content" class="content-section">
                  <h5 class="mb-2 text-sm">Content</h5>
                  <div class="dialogue-box">
                    <div
                        v-for="(line, idx) in formatDialogue(episode.content.content)"
                        :key="idx"
                        class="dialogue-line"
                    >
                      {{ line }}
                    </div>
                  </div>
                </div>

                <!-- Summary -->
                <div v-if="episode.content?.summary" class="mt-3">
                  <h5 class="mb-2 text-sm">Summary</h5>
                  <p class="text-color-secondary text-sm">{{ episode.content.summary }}</p>
                </div>

                <!-- Vocabulary Coverage -->
                <div v-if="episode.content?.vocabularyUsed?.length" class="mt-3">
                  <h5 class="mb-2 text-sm">
                    Vocabulary Used ({{ episode.content.vocabularyUsed.length }}/{{ episode.vocabulary?.length || 0 }})
                  </h5>
                  <div class="flex gap-1 flex-wrap">
                    <Tag
                        v-for="word in episode.content.vocabularyUsed"
                        :key="word"
                        :value="word"
                        severity="success"
                        size="small"
                    />
                  </div>
                  <div v-if="episode.content.vocabularyMissing?.length" class="mt-2">
                    <span class="text-sm text-color-secondary">Missing: </span>
                    <Tag
                        v-for="word in episode.content.vocabularyMissing"
                        :key="word"
                        :value="word"
                        severity="danger"
                        size="small"
                        class="mr-1"
                    />
                  </div>
                </div>
              </div>
            </AccordionTab>
          </Accordion>
        </TabPanel>
      </TabView>
    </template>
  </Card>
</template>

<style scoped>
.episode-detail {
  padding: 0.5rem 0;
}

.dialogue-box {
  background: var(--surface-50);
  border-radius: 8px;
  padding: 1rem;
  max-height: 400px;
  overflow-y: auto;
}

.dialogue-line {
  padding: 0.25rem 0;
  font-size: 0.9rem;
  line-height: 1.5;
}

.dialogue-line:not(:last-child) {
  border-bottom: 1px solid var(--surface-200);
}
</style>
