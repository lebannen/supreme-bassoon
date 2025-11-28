<script setup lang="ts">
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import {computed} from 'vue'

export interface ContentCardProps {
  /**
   * Content type: 'dialogue', 'story', or 'grammar'
   */
  type: 'dialogue' | 'story' | 'grammar'

  /**
   * Content title
   */
  title: string

  /**
   * Level badge (e.g., "A1", "B2")
   */
  level?: string

  /**
   * Content description
   */
  description?: string

  /**
   * Icon class for the content type
   */
  icon?: string

  /**
   * Whether content is completed
   */
  completed?: boolean
}

const props = withDefaults(defineProps<ContentCardProps>(), {
  completed: false,
})

const emit = defineEmits<{
  click: []
}>()

const gradientStyle = computed(() => {
  const gradients = {
    dialogue: 'var(--gradient-dialogue)',
    story: 'var(--gradient-story)',
    grammar: 'var(--gradient-grammar)',
  }
  return {background: gradients[props.type]}
})
</script>

<template>
  <Card class="card-interactive content-card" @click="emit('click')">
    <template #header>
      <div
          class="h-160 flex align-items-center justify-content-center relative"
          :style="gradientStyle"
      >
        <i v-if="icon" class="text-4xl" :class="icon"></i>
        <Tag
            v-if="completed"
            value="Completed"
            icon="pi pi-check"
            severity="success"
            class="absolute top-sm left-sm"
        />
      </div>
    </template>

    <template #subtitle>
      <div class="meta-badges">
        <Tag :value="type" severity="contrast"/>
        <Tag v-if="level" :value="level"/>
      </div>
    </template>

    <template #title>
      <h3 class="text-lg font-bold">{{ title }}</h3>
    </template>

    <template #content>
      <p v-if="description" class="text-sm text-secondary m-0">{{ description }}</p>
    </template>
  </Card>
</template>

<style scoped>
.content-card {
  overflow: hidden;
}

.h-160 {
  height: 160px;
}

.top-sm {
  top: var(--spacing-sm);
}

.left-sm {
  left: var(--spacing-sm);
}
</style>
