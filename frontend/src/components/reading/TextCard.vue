<template>
  <Card class="text-card">
    <template #header>
      <div class="card-header">
        <div class="title-row">
          <h3>{{ text.title }}</h3>
          <Tag v-if="text.level" :value="text.level" severity="info" />
        </div>
        <div class="meta-row">
          <span class="language-badge">{{ getLanguageName(text.languageCode) }}</span>
          <span v-if="text.topic" class="topic">{{ formatTopic(text.topic) }}</span>
        </div>
      </div>
    </template>

    <template #content>
      <div class="card-content">
        <p v-if="text.description" class="description">
          {{ text.description }}
        </p>
        <p v-else class="description preview">
          {{ getPreview(text.content) }}
        </p>

        <div class="stats">
          <div class="stat">
            <i class="pi pi-book"></i>
            <span>{{ text.wordCount || 0 }} words</span>
          </div>
          <div v-if="text.estimatedMinutes" class="stat">
            <i class="pi pi-clock"></i>
            <span>{{ text.estimatedMinutes }} min</span>
          </div>
          <div v-if="text.audioUrl" class="stat audio-stat">
            <i class="pi pi-volume-up"></i>
            <span>Audio</span>
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="card-footer">
        <Button
          label="Read"
          icon="pi pi-book"
          @click="$emit('select', text.id)"
          outlined
        />
      </div>
    </template>
  </Card>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import type { ReadingText } from '@/composables/useReadingTexts'

interface Props {
  text: ReadingText
}

defineProps<Props>()
defineEmits<{
  select: [id: number]
}>()

function getLanguageName(code: string): string {
  const languages: Record<string, string> = {
    fr: 'French',
    de: 'German',
    es: 'Spanish',
    it: 'Italian',
    pt: 'Portuguese',
    ru: 'Russian',
    ja: 'Japanese',
    zh: 'Chinese',
    ko: 'Korean',
    ar: 'Arabic',
    hi: 'Hindi',
    pl: 'Polish'
  }
  return languages[code] || code.toUpperCase()
}

function formatTopic(topic: string): string {
  return topic
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

function getPreview(content: string): string {
  const firstParagraph = content.split('\n\n')[0]
  return firstParagraph.length > 150
    ? firstParagraph.substring(0, 150) + '...'
    : firstParagraph
}
</script>

<style scoped>
.text-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.title-row h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
}

.meta-row {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  font-size: 0.875rem;
}

.language-badge {
  padding: 0.25rem 0.75rem;
  background: var(--primary-color);
  color: white;
  border-radius: 1rem;
  font-size: 0.75rem;
  font-weight: 500;
}

.topic {
  color: var(--text-color-secondary);
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  flex: 1;
}

.description {
  margin: 0;
  color: var(--text-color);
  line-height: 1.6;
}

.description.preview {
  color: var(--text-color-secondary);
  font-style: italic;
}

.stats {
  display: flex;
  gap: 1.5rem;
  margin-top: auto;
}

.stat {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.stat i {
  font-size: 1rem;
}

.stat.audio-stat {
  color: var(--primary-color);
  font-weight: 500;
}

.stat.audio-stat i {
  color: var(--primary-color);
}

.card-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
