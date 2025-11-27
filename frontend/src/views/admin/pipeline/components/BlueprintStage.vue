<script setup lang="ts">
import {computed} from 'vue'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import type {BlueprintDto, CharacterDto} from '@/types/pipeline'

const props = defineProps<{
  blueprint?: BlueprintDto
  characters?: CharacterDto[]
  isCurrent: boolean
}>()

const hasContent = computed(() => props.blueprint?.status === 'COMPLETED')
</script>

<template>
  <Card class="mb-4">
    <template #title>
      <div class="flex align-items-center gap-2">
        <i class="pi pi-file-edit"></i>
        <span>Blueprint</span>
        <Tag
            v-if="isCurrent"
            value="Current Stage"
            severity="info"
            class="ml-2"
        />
        <Tag
            v-else-if="hasContent"
            value="Completed"
            severity="success"
            class="ml-2"
        />
      </div>
    </template>

    <template #content>
      <div v-if="!blueprint || blueprint.status === 'PENDING'" class="text-color-secondary">
        Blueprint generation pending...
      </div>

      <div v-else-if="blueprint.status === 'IN_PROGRESS'" class="text-color-secondary">
        <i class="pi pi-spin pi-spinner mr-2"></i>
        Generating blueprint...
      </div>

      <div v-else-if="hasContent" class="blueprint-content">
        <!-- Course Info -->
        <div class="mb-4">
          <h3 class="text-xl font-semibold mb-2">{{ blueprint.courseTitle }}</h3>
          <p class="text-color-secondary">{{ blueprint.courseDescription }}</p>
        </div>

        <!-- Setting & Premise -->
        <Accordion :multiple="true" class="mb-4">
          <AccordionTab header="Setting">
            <p class="m-0 whitespace-pre-line">{{ blueprint.setting }}</p>
          </AccordionTab>

          <AccordionTab header="Premise">
            <p class="m-0 whitespace-pre-line">{{ blueprint.premise }}</p>
          </AccordionTab>

          <!-- Plot Arc -->
          <AccordionTab v-if="blueprint.plotArc?.length" header="Plot Arc">
            <div class="plot-arc">
              <div
                  v-for="point in blueprint.plotArc"
                  :key="point.moduleNumber"
                  class="plot-point"
              >
                <div class="plot-module">Module {{ point.moduleNumber }}</div>
                <div class="plot-text">{{ point.plotPoint }}</div>
              </div>
            </div>
          </AccordionTab>

          <!-- Module Topics -->
          <AccordionTab v-if="blueprint.moduleTopics?.length" header="Module Topics">
            <div class="module-topics">
              <div
                  v-for="topic in blueprint.moduleTopics"
                  :key="topic.moduleNumber"
                  class="topic-item"
              >
                <div class="topic-header">
                  <span class="topic-number">Module {{ topic.moduleNumber }}</span>
                  <Tag :value="topic.theme" severity="secondary"/>
                </div>
                <div class="topic-text">{{ topic.topic }}</div>
              </div>
            </div>
          </AccordionTab>

          <!-- Grammar Distribution -->
          <AccordionTab v-if="blueprint.grammarDistribution?.length" header="Grammar Distribution">
            <div class="grammar-dist">
              <div
                  v-for="item in blueprint.grammarDistribution"
                  :key="item.moduleNumber"
                  class="grammar-item"
              >
                <div class="grammar-module">Module {{ item.moduleNumber }}</div>
                <div class="grammar-rules">
                  <Tag
                      v-for="rule in item.grammarRules"
                      :key="rule"
                      :value="rule"
                      severity="info"
                      class="mr-1 mb-1"
                  />
                </div>
              </div>
            </div>
          </AccordionTab>

          <!-- Characters -->
          <AccordionTab v-if="characters?.length" header="Characters">
            <div class="characters-grid">
              <div
                  v-for="char in characters"
                  :key="char.id"
                  class="character-card"
              >
                <div class="character-header">
                  <strong>{{ char.name }}</strong>
                  <Tag :value="char.role" severity="secondary" size="small"/>
                </div>
                <p class="character-desc">{{ char.initialDescription }}</p>
                <div v-if="char.personalityTraits?.length" class="character-traits">
                  <Tag
                      v-for="trait in char.personalityTraits"
                      :key="trait"
                      :value="trait"
                      severity="info"
                      size="small"
                      class="mr-1"
                  />
                </div>
              </div>
            </div>
          </AccordionTab>
        </Accordion>
      </div>
    </template>
  </Card>
</template>

<style scoped>
.blueprint-content {
  max-width: 100%;
}

.plot-arc {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.plot-point {
  display: flex;
  gap: 1rem;
  padding: 0.75rem;
  background: var(--surface-50);
  border-radius: 8px;
}

.plot-module {
  font-weight: 600;
  min-width: 100px;
  color: var(--primary-color);
}

.module-topics {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.topic-item {
  padding: 0.75rem;
  background: var(--surface-50);
  border-radius: 8px;
}

.topic-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.topic-number {
  font-weight: 600;
  color: var(--primary-color);
}

.grammar-dist {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.grammar-item {
  padding: 0.75rem;
  background: var(--surface-50);
  border-radius: 8px;
}

.grammar-module {
  font-weight: 600;
  color: var(--primary-color);
  margin-bottom: 0.5rem;
}

.characters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

.character-card {
  padding: 1rem;
  background: var(--surface-50);
  border-radius: 8px;
}

.character-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.character-desc {
  font-size: 0.9rem;
  color: var(--text-color-secondary);
  margin-bottom: 0.5rem;
}

.whitespace-pre-line {
  white-space: pre-line;
}
</style>
