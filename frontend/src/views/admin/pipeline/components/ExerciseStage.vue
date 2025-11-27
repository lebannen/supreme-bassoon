<script setup lang="ts">
import {ref, computed} from 'vue'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import ProgressBar from 'primevue/progressbar'
import type {ModulePlanDto} from '@/types/pipeline'

const props = defineProps<{
  modulePlans?: ModulePlanDto[]
  isCurrent: boolean
}>()

const selectedModule = ref(0)

const exerciseStats = computed(() => {
  if (!props.modulePlans) return {completed: 0, total: 0, exerciseCount: 0}

  let completed = 0
  let total = 0
  let exerciseCount = 0

  props.modulePlans.forEach((m) => {
    m.episodes.forEach((e) => {
      total++
      if (e.exercises?.status === 'COMPLETED') {
        completed++
        exerciseCount += e.exercises.exerciseCount || 0
      }
    })
  })

  return {completed, total, exerciseCount}
})

const progressPercent = computed(() => {
  if (exerciseStats.value.total === 0) return 0
  return Math.round((exerciseStats.value.completed / exerciseStats.value.total) * 100)
})
</script>

<template>
  <Card class="mb-4">
    <template #title>
      <div class="flex align-items-center gap-2">
        <i class="pi pi-check-square"></i>
        <span>Exercises</span>
        <Tag
            v-if="isCurrent"
            value="Current Stage"
            severity="info"
            class="ml-2"
        />
        <Tag
            v-else-if="exerciseStats.completed === exerciseStats.total && exerciseStats.total > 0"
            value="Completed"
            severity="success"
            class="ml-2"
        />
        <span class="text-color-secondary text-sm ml-auto">
          {{ exerciseStats.exerciseCount }} exercises generated
        </span>
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
            <span class="text-sm">Progress</span>
            <span class="text-sm">{{ exerciseStats.completed }}/{{ exerciseStats.total }} episodes</span>
          </div>
          <ProgressBar :value="progressPercent"/>
        </div>

        <!-- Modules -->
        <TabView v-model:activeIndex="selectedModule">
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
                    <div class="ml-auto flex align-items-center gap-2">
                      <span v-if="episode.exercises?.exerciseCount" class="text-sm text-color-secondary">
                        {{ episode.exercises.exerciseCount }} exercises
                      </span>
                      <Tag
                          :value="episode.exercises?.status || 'PENDING'"
                          :severity="episode.exercises?.status === 'COMPLETED' ? 'success' : 'secondary'"
                          size="small"
                      />
                    </div>
                  </div>
                </template>

                <div class="exercise-detail">
                  <div v-if="!episode.exercises" class="text-color-secondary">
                    <i class="pi pi-spin pi-spinner mr-2" v-if="isCurrent"></i>
                    {{ isCurrent ? 'Generating exercises...' : 'No exercises yet.' }}
                  </div>

                  <template v-else>
                    <!-- Exercise Count -->
                    <div class="stat-row mb-3">
                      <div class="stat-item">
                        <div class="stat-value">{{ episode.exercises.exerciseCount }}</div>
                        <div class="stat-label">Total Exercises</div>
                      </div>
                    </div>

                    <!-- Vocabulary Coverage -->
                    <div v-if="episode.exercises.vocabularyCoverage?.length" class="coverage-section mb-3">
                      <h5 class="mb-2 text-sm">
                        Vocabulary Coverage ({{
                          episode.exercises.vocabularyCoverage.length
                        }}/{{ episode.vocabulary?.length || 0 }})
                      </h5>
                      <div class="flex gap-1 flex-wrap">
                        <Tag
                            v-for="word in episode.exercises.vocabularyCoverage"
                            :key="word"
                            :value="word"
                            severity="success"
                            size="small"
                        />
                      </div>

                      <!-- Missing vocabulary -->
                      <div
                          v-if="episode.vocabulary?.filter(v => !episode.exercises?.vocabularyCoverage?.includes(v)).length"
                          class="mt-2"
                      >
                        <span class="text-sm text-color-secondary">Not covered: </span>
                        <Tag
                            v-for="word in episode.vocabulary?.filter(v => !episode.exercises?.vocabularyCoverage?.includes(v))"
                            :key="word"
                            :value="word"
                            severity="warn"
                            size="small"
                            class="mr-1"
                        />
                      </div>
                    </div>

                    <!-- Grammar Coverage -->
                    <div v-if="episode.exercises.grammarCoverage?.length" class="coverage-section">
                      <h5 class="mb-2 text-sm">Grammar Coverage</h5>
                      <div class="flex gap-1 flex-wrap">
                        <Tag
                            v-for="rule in episode.exercises.grammarCoverage"
                            :key="rule"
                            :value="rule"
                            severity="info"
                            size="small"
                        />
                      </div>
                    </div>
                  </template>
                </div>
              </AccordionTab>
            </Accordion>
          </TabPanel>
        </TabView>
      </template>
    </template>
  </Card>
</template>

<style scoped>
.exercise-detail {
  padding: 0.5rem 0;
}

.stat-row {
  display: flex;
  gap: 2rem;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--primary-color);
}

.stat-label {
  font-size: 0.85rem;
  color: var(--text-color-secondary);
}

.coverage-section {
  background: var(--surface-50);
  padding: 1rem;
  border-radius: 8px;
}
</style>
