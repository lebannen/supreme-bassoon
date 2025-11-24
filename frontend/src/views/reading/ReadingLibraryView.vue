<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import Dropdown from 'primevue/dropdown'
import InputText from 'primevue/inputtext'
import ProgressSpinner from 'primevue/progressspinner'
import {useAuthStore} from '@/stores/auth'
import type {ReadingText} from '@/types/reading'

const router = useRouter()
const authStore = useAuthStore()
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const texts = ref<ReadingText[]>([])
const loading = ref(true)
const error = ref('')

// Filters
const selectedLanguage = ref<string | null>(null)
const selectedLevel = ref<string | null>(null)
const searchQuery = ref('')

const languageOptions = [
  {label: 'All Languages', value: null},
  {label: 'French', value: 'fr'},
  {label: 'Spanish', value: 'es'},
  {label: 'German', value: 'de'},
  {label: 'Italian', value: 'it'}
]

const levelOptions = [
  {label: 'All Levels', value: null},
  {label: 'A1', value: 'A1'},
  {label: 'A2', value: 'A2'},
  {label: 'B1', value: 'B1'},
  {label: 'B2', value: 'B2'},
  {label: 'C1', value: 'C1'},
  {label: 'C2', value: 'C2'}
]

const filteredTexts = computed(() => {
  return texts.value.filter(text => {
    const matchesLanguage = !selectedLanguage.value || text.languageCode === selectedLanguage.value
    const matchesLevel = !selectedLevel.value || text.level === selectedLevel.value
    const matchesSearch = !searchQuery.value ||
        text.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
        text.description?.toLowerCase().includes(searchQuery.value.toLowerCase())

    return matchesLanguage && matchesLevel && matchesSearch
  })
})

onMounted(async () => {
  await loadTexts()
})

async function loadTexts() {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams()
    if (selectedLanguage.value) params.append('languageCode', selectedLanguage.value)
    if (selectedLevel.value) params.append('level', selectedLevel.value)

    const url = `/api/reading/texts${params.toString() ? '?' + params.toString() : ''}`
    const response = await fetch(`${API_BASE}${url}`)

    if (!response.ok) throw new Error('Failed to load texts')

    texts.value = await response.json()
  } catch (e: any) {
    error.value = e.message || 'Failed to load reading texts'
  } finally {
    loading.value = false
  }
}

function viewText(textId: number) {
  router.push(`/reading/${textId}`)
}

function formatTopic(topic: string | null | undefined): string {
  if (!topic) return ''
  return topic.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
}
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <div class="flex justify-between items-center">
        <div>
          <h1>Reading Library</h1>
          <p class="text-secondary">Browse and select texts to practice reading.</p>
        </div>
        <Button
            v-if="authStore.isAdmin"
            label="Import Texts"
            icon="pi pi-upload"
            @click="router.push('/reading/import')"
        />
      </div>
    </div>

    <!-- Filters -->
    <Card class="mb-lg">
      <template #content>
        <div class="grid grid-cols-1 md:grid-cols-4 gap-md">
          <div class="flex flex-col gap-xs">
            <label class="font-semibold text-sm">Search</label>
            <InputText
                v-model="searchQuery"
                placeholder="Search titles..."
                :disabled="loading"
            >
              <template #prefix>
                <i class="pi pi-search"></i>
              </template>
            </InputText>
          </div>
          <div class="flex flex-col gap-xs">
            <label class="font-semibold text-sm">Language</label>
            <Dropdown
                v-model="selectedLanguage"
                :options="languageOptions"
                optionLabel="label"
                optionValue="value"
                :disabled="loading"
            />
          </div>
          <div class="flex flex-col gap-xs">
            <label class="font-semibold text-sm">Level</label>
            <Dropdown
                v-model="selectedLevel"
                :options="levelOptions"
                optionLabel="label"
                optionValue="value"
                :disabled="loading"
            />
          </div>
          <div class="flex items-end">
            <Button
                label="Clear Filters"
                icon="pi pi-filter-slash"
                text
                @click="selectedLanguage = null; selectedLevel = null; searchQuery = ''"
                :disabled="!selectedLanguage && !selectedLevel && !searchQuery"
            />
          </div>
        </div>
      </template>
    </Card>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center p-xl">
      <ProgressSpinner/>
    </div>

    <!-- Error State -->
    <Card v-else-if="error">
      <template #content>
        <div class="text-center p-lg text-red-600">
          <i class="pi pi-exclamation-triangle text-4xl mb-md"></i>
          <p>{{ error }}</p>
          <Button label="Retry" icon="pi pi-refresh" @click="loadTexts" class="mt-md"/>
        </div>
      </template>
    </Card>

    <!-- Empty State -->
    <Card v-else-if="filteredTexts.length === 0">
      <template #content>
        <div class="empty-state">
          <i class="pi pi-book empty-icon"></i>
          <h3>No Texts Found</h3>
          <p class="text-secondary">
            {{ texts.length === 0 ? 'No reading texts available yet.' : 'No texts match your filters.' }}
          </p>
          <Button
              v-if="authStore.isAdmin && texts.length === 0"
              label="Import Texts"
              icon="pi pi-upload"
              @click="router.push('/reading/import')"
              class="mt-lg"
          />
        </div>
      </template>
    </Card>

    <!-- Texts Grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-lg">
      <Card v-for="text in filteredTexts" :key="text.id" class="cursor-pointer hover:shadow-lg transition-shadow">
        <template #header>
          <div class="p-md pb-0">
            <div class="flex gap-sm mb-sm">
              <Tag :value="text.languageCode.toUpperCase()" severity="info"/>
              <Tag v-if="text.level" :value="text.level" severity="secondary"/>
              <Tag v-if="text.topic" :value="formatTopic(text.topic)" severity="contrast"/>
            </div>
          </div>
        </template>

        <template #title>
          <div class="px-md">{{ text.title }}</div>
        </template>

        <template #content>
          <div class="px-md">
            <p v-if="text.description" class="text-sm text-secondary mb-md">
              {{ text.description }}
            </p>

            <div class="flex gap-md text-sm text-secondary">
              <span><i class="pi pi-book mr-xs"></i>{{ text.wordCount }} words</span>
              <span><i class="pi pi-clock mr-xs"></i>{{ text.estimatedMinutes }} min</span>
            </div>
          </div>
        </template>

        <template #footer>
          <div class="flex gap-sm px-md pb-md">
            <Button
                label="Read"
                icon="pi pi-book"
                @click="viewText(text.id)"
                class="flex-grow"
            />
          </div>
        </template>
      </Card>
    </div>

    <!-- Results Count -->
    <div v-if="!loading && filteredTexts.length > 0" class="text-center mt-lg text-secondary">
      Showing {{ filteredTexts.length }} of {{ texts.length }} texts
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  text-align: center;
  padding: 3rem 1rem;
}

.empty-icon {
  font-size: 4rem;
  color: var(--text-color-secondary);
  margin-bottom: 1rem;
}

.empty-state h3 {
  margin-bottom: 0.5rem;
}
</style>
