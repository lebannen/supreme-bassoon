<script setup lang="ts">
import {ref} from 'vue'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import type {ModulePlanDto} from '@/types/pipeline'

defineProps<{
  modulePlans?: ModulePlanDto[]
  isCurrent: boolean
}>()

const expandedModule = ref<number | null>(null)
</script>

<template>
  <Card class="mb-4">
    <template #title>
      <div class="flex align-items-center gap-2">
        <i class="pi pi-list"></i>
        <span>Module Plans</span>
        <Tag
            v-if="isCurrent"
            value="Current Stage"
            severity="info"
            class="ml-2"
        />
        <Tag
            v-else-if="modulePlans?.length"
            value="Completed"
            severity="success"
            class="ml-2"
        />
      </div>
    </template>

    <template #content>
      <div v-if="!modulePlans?.length" class="text-color-secondary">
        <i class="pi pi-spin pi-spinner mr-2" v-if="isCurrent"></i>
        {{ isCurrent ? 'Generating module plans...' : 'No module plans yet.' }}
      </div>

      <Accordion v-else :multiple="false">
        <AccordionTab
            v-for="module in modulePlans"
            :key="module.id"
            :header="`Module ${module.moduleNumber}: ${module.title || 'Untitled'}`"
        >
          <div class="module-detail">
            <!-- Module Info -->
            <div class="module-info mb-4">
              <div class="flex gap-3 mb-2">
                <Tag v-if="module.theme" :value="module.theme" severity="secondary"/>
                <Tag
                    :value="module.status"
                    :severity="module.status === 'COMPLETED' ? 'success' : 'info'"
                />
              </div>
              <p v-if="module.description" class="text-color-secondary">
                {{ module.description }}
              </p>
            </div>

            <!-- Objectives -->
            <div v-if="module.objectives?.length" class="mb-4">
              <h4 class="mb-2">Objectives</h4>
              <ul class="m-0 pl-4">
                <li v-for="obj in module.objectives" :key="obj">{{ obj }}</li>
              </ul>
            </div>

            <!-- Plot Summary -->
            <div v-if="module.plotSummary" class="mb-4">
              <h4 class="mb-2">Plot Summary</h4>
              <p class="text-color-secondary whitespace-pre-line">{{ module.plotSummary }}</p>
            </div>

            <!-- Episodes Table -->
            <div v-if="module.episodes?.length">
              <h4 class="mb-2">Episodes</h4>
              <DataTable :value="module.episodes" class="p-datatable-sm" stripedRows>
                <Column field="episodeNumber" header="#" style="width: 50px"/>
                <Column field="title" header="Title">
                  <template #body="{ data }">
                    {{ data.title || `Episode ${data.episodeNumber}` }}
                  </template>
                </Column>
                <Column field="episodeType" header="Type">
                  <template #body="{ data }">
                    <Tag :value="data.episodeType" severity="secondary" size="small"/>
                  </template>
                </Column>
                <Column header="Vocabulary">
                  <template #body="{ data }">
                    <span class="text-color-secondary">
                      {{ data.vocabulary?.length || 0 }} words
                    </span>
                  </template>
                </Column>
                <Column header="Grammar">
                  <template #body="{ data }">
                    <span class="text-color-secondary">
                      {{ data.grammarRules?.length || 0 }} rules
                    </span>
                  </template>
                </Column>
                <Column field="status" header="Status">
                  <template #body="{ data }">
                    <Tag
                        :value="data.status"
                        :severity="data.status === 'COMPLETED' ? 'success' : 'info'"
                        size="small"
                    />
                  </template>
                </Column>
              </DataTable>
            </div>
          </div>
        </AccordionTab>
      </Accordion>
    </template>
  </Card>
</template>

<style scoped>
.module-detail {
  padding: 0.5rem 0;
}

.whitespace-pre-line {
  white-space: pre-line;
}
</style>
