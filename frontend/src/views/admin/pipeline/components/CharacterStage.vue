<script setup lang="ts">
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import Image from 'primevue/image'
import type {CharacterDto} from '@/types/pipeline'

defineProps<{
  characters?: CharacterDto[]
  isCurrent: boolean
}>()
</script>

<template>
  <Card class="mb-4">
    <template #title>
      <div class="flex align-items-center gap-2">
        <i class="pi pi-users"></i>
        <span>Character Profiles</span>
        <Tag
            v-if="isCurrent"
            value="Current Stage"
            severity="info"
            class="ml-2"
        />
        <Tag
            v-else-if="characters?.some(c => c.referenceImageUrl)"
            value="Completed"
            severity="success"
            class="ml-2"
        />
      </div>
    </template>

    <template #content>
      <div v-if="!characters?.length" class="text-color-secondary">
        <i class="pi pi-spin pi-spinner mr-2" v-if="isCurrent"></i>
        {{ isCurrent ? 'Processing character profiles...' : 'No characters defined yet.' }}
      </div>

      <div v-else class="characters-grid">
        <div
            v-for="char in characters"
            :key="char.id"
            class="character-card"
        >
          <!-- Character Image -->
          <div class="character-image">
            <Image
                v-if="char.referenceImageUrl"
                :src="char.referenceImageUrl"
                :alt="char.name"
                preview
                class="w-full"
            />
            <div v-else class="image-placeholder">
              <i class="pi pi-user text-4xl"></i>
              <span v-if="isCurrent" class="text-sm mt-2">Generating...</span>
            </div>
          </div>

          <!-- Character Info -->
          <div class="character-info">
            <div class="flex justify-content-between align-items-center mb-2">
              <h4 class="m-0">{{ char.name }}</h4>
              <Tag :value="char.role" severity="secondary" size="small"/>
            </div>

            <p v-if="char.initialDescription" class="text-sm text-color-secondary mb-2">
              {{ char.initialDescription }}
            </p>

            <div v-if="char.ageRange" class="text-sm mb-2">
              <strong>Age:</strong> {{ char.ageRange }}
            </div>

            <div v-if="char.personalityTraits?.length" class="mb-2">
              <Tag
                  v-for="trait in char.personalityTraits"
                  :key="trait"
                  :value="trait"
                  severity="info"
                  size="small"
                  class="mr-1 mb-1"
              />
            </div>

            <div v-if="char.background" class="text-sm text-color-secondary mb-2">
              <strong>Background:</strong> {{ char.background }}
            </div>

            <div v-if="char.voiceId" class="voice-info">
              <i class="pi pi-volume-up mr-1"></i>
              <span class="text-sm">{{ char.voiceId }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>
.characters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.character-card {
  background: var(--surface-50);
  border-radius: 12px;
  overflow: hidden;
}

.character-image {
  aspect-ratio: 1;
  overflow: hidden;
  background: var(--surface-100);
}

.character-image :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-color-secondary);
}

.character-info {
  padding: 1rem;
}

.voice-info {
  display: flex;
  align-items: center;
  color: var(--primary-color);
  margin-top: 0.5rem;
}
</style>
