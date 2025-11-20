<script setup lang="ts">
import Card from 'primevue/card'
import Tag from 'primevue/tag'

export interface CourseCardProps {
  /**
   * Course name
   */
  name: string

  /**
   * Course description
   */
  description: string

  /**
   * Language code (e.g., "FR", "ES")
   */
  languageCode: string

  /**
   * CEFR level (e.g., "A1", "B2")
   */
  cefrLevel: string

  /**
   * Total number of modules
   */
  totalModules: number

  /**
   * Total number of episodes
   */
  totalEpisodes: number
}

defineProps<CourseCardProps>()

const emit = defineEmits<{
  click: []
}>()
</script>

<template>
  <Card class="card-interactive course-card" @click="emit('click')">
    <template #header>
      <div class="p-xl flex items-center justify-between" :style="{ background: 'var(--gradient-dialogue)' }">
        <div class="flex flex-col gap-sm">
          <Tag :value="languageCode.toUpperCase()"/>
          <Tag :value="cefrLevel" severity="contrast"/>
        </div>
      </div>
    </template>

    <template #title>
      <h3 class="text-lg font-bold">{{ name }}</h3>
    </template>

    <template #content>
      <p class="text-sm text-secondary mb-lg">{{ description }}</p>
    </template>

    <template #footer>
      <div class="icon-label-group compact">
        <span class="icon-label">
          <i class="pi pi-book"></i>
          <span>{{ totalModules }} modules</span>
        </span>
        <span class="icon-label">
          <i class="pi pi-video"></i>
          <span>{{ totalEpisodes }} episodes</span>
        </span>
      </div>
    </template>
  </Card>
</template>

<style scoped>
.course-card {
  overflow: hidden;
}
</style>
