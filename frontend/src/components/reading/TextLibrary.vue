<template>
  <div class="text-library">
    <div class="filters">
      <div class="filter-group">
        <label for="language-filter">Language</label>
        <Dropdown
          id="language-filter"
          v-model="filters.languageCode"
          :options="languages"
          option-label="name"
          option-value="code"
          placeholder="All Languages"
          show-clear
          @change="applyFilters"
        />
      </div>

      <div class="filter-group">
        <label for="level-filter">Level</label>
        <Dropdown
          id="level-filter"
          v-model="filters.level"
          :options="levels"
          placeholder="All Levels"
          show-clear
          @change="applyFilters"
        />
      </div>

      <div class="filter-group">
        <label for="topic-filter">Topic</label>
        <Dropdown
          id="topic-filter"
          v-model="filters.topic"
          :options="topics"
          option-label="label"
          option-value="value"
          placeholder="All Topics"
          show-clear
          @change="applyFilters"
        />
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner />
      <p>Loading texts...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <Message severity="error" :closable="false">
        {{ error }}
      </Message>
    </div>

    <div v-else-if="texts.length === 0" class="empty-state">
      <i class="pi pi-book" style="font-size: 3rem; color: var(--text-color-secondary)"></i>
      <h3>No texts found</h3>
      <p>Try adjusting your filters or check back later for new content.</p>
    </div>

    <div v-else class="texts-grid">
      <TextCard v-for="text in texts" :key="text.id" :text="text" @select="handleSelectText"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import Dropdown from 'primevue/dropdown'
import ProgressSpinner from 'primevue/progressspinner'
import Message from 'primevue/message'
import TextCard from './TextCard.vue'
import {type TextFilters, useReadingTexts} from '@/composables/useReadingTexts'

const router = useRouter()
const { texts, loading, error, fetchTexts } = useReadingTexts()

const filters = ref<TextFilters>({
  languageCode: undefined,
  level: undefined,
  topic: undefined,
})

const languages = [
  { code: 'fr', name: 'French' },
  { code: 'de', name: 'German' },
  { code: 'es', name: 'Spanish' },
  { code: 'it', name: 'Italian' },
  { code: 'pt', name: 'Portuguese' },
  { code: 'ru', name: 'Russian' },
  { code: 'ja', name: 'Japanese' },
  { code: 'zh', name: 'Chinese' },
  { code: 'ko', name: 'Korean' },
  { code: 'ar', name: 'Arabic' },
  { code: 'hi', name: 'Hindi' },
  {code: 'pl', name: 'Polish'},
]

const levels = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']

const topics = [
  { value: 'daily_life', label: 'Daily Life' },
  { value: 'travel', label: 'Travel' },
  { value: 'food', label: 'Food' },
  { value: 'culture', label: 'Culture' },
  { value: 'shopping', label: 'Shopping' },
  { value: 'family', label: 'Family' },
  { value: 'work', label: 'Work' },
  { value: 'hobbies', label: 'Hobbies' },
  { value: 'health', label: 'Health' },
  {value: 'education', label: 'Education'},
]

async function applyFilters() {
  await fetchTexts(filters.value)
}

function handleSelectText(id: number) {
  router.push({ name: 'ReadingText', params: { id } })
}

onMounted(async () => {
  await fetchTexts()
})
</script>

<style scoped>
.text-library {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.filters {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
  padding: 1.5rem;
  background: var(--surface-card);
  border-radius: 8px;
  border: 1px solid var(--surface-border);
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 200px;
  flex: 1;
}

.filter-group label {
  font-weight: 500;
  color: var(--text-color);
  font-size: 0.875rem;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  gap: 1rem;
}

.empty-state h3 {
  margin: 0;
  color: var(--text-color);
}

.empty-state p {
  margin: 0;
  color: var(--text-color-secondary);
}

.error-state {
  padding: 2rem;
}

.texts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 1.5rem;
}

@media (max-width: 768px) {
  .text-library {
    padding: 1rem;
  }

  .filters {
    flex-direction: column;
  }

  .filter-group {
    min-width: 100%;
  }

  .texts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
