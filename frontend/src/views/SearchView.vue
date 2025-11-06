<template>
  <div class="search-container">
    <h1>Vocabulary Search</h1>
    <p class="description">Search for words and explore their definitions</p>

    <div class="search-section">
      <div class="field">
        <label for="language-select">Language</label>
        <Select
          id="language-select"
          v-model="selectedLanguage"
          :options="languages"
          optionLabel="name"
          optionValue="code"
          placeholder="Select a language"
          class="w-full"
          :loading="isLoadingLanguages"
        />
      </div>

      <div class="field">
        <label for="search-input">Search for a word</label>
        <div class="search-input-wrapper">
          <InputText
            id="search-input"
            v-model="searchQuery"
            placeholder="Enter a word to search..."
            class="w-full"
            :disabled="!selectedLanguage"
            @keyup.enter="performSearch"
          />
          <Button
            label="Search"
            icon="pi pi-search"
            @click="performSearch"
            :disabled="!selectedLanguage || !searchQuery.trim()"
            :loading="isSearching"
          />
        </div>
      </div>
    </div>

    <Message v-if="searchError" severity="error" :closable="true" @close="searchError = null">
      {{ searchError }}
    </Message>

    <div v-if="searchResults && searchResults.words.length > 0" class="results-section">
      <h2>Search Results ({{ searchResults.total }})</h2>
      <DataTable :value="searchResults.words" :paginator="true" :rows="20" selectionMode="single" @row-select="onWordSelect">
        <Column field="lemma" header="Word" sortable></Column>
        <Column field="partOfSpeech" header="Part of Speech" sortable>
          <template #body="slotProps">
            <Tag v-if="slotProps.data.partOfSpeech" :value="slotProps.data.partOfSpeech" severity="info" />
          </template>
        </Column>
        <Column field="frequencyRank" header="Frequency" sortable>
          <template #body="slotProps">
            <span v-if="slotProps.data.frequencyRank">{{ slotProps.data.frequencyRank.toLocaleString() }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </Column>
      </DataTable>
    </div>

    <div v-else-if="hasSearched && searchResults && searchResults.words.length === 0" class="no-results">
      <i class="pi pi-search" style="font-size: 3rem; color: var(--text-color-secondary);"></i>
      <p>No words found for "{{ searchQuery }}"</p>
    </div>

    <Dialog v-model:visible="showWordDialog" :header="selectedWord?.lemma" :modal="true" :style="{ width: '50vw' }" :breakpoints="{ '960px': '75vw', '640px': '90vw' }">
      <div v-if="selectedWord && !isLoadingWord" class="word-details">
        <div class="word-header">
          <div class="word-title">
            <h2>{{ selectedWord.lemma }}</h2>
            <Tag v-if="selectedWord.partOfSpeech" :value="selectedWord.partOfSpeech" severity="info" />
          </div>
        </div>

        <div v-if="selectedWord.definitions.length > 0" class="definitions">
          <h3>Definitions</h3>
          <div v-for="def in selectedWord.definitions" :key="def.id" class="definition">
            <div class="definition-text">
              <span class="definition-number">{{ def.definitionNumber }}.</span>
              {{ def.definitionText }}
            </div>
            <div v-if="def.examples.length > 0" class="examples">
              <div v-for="example in def.examples" :key="example.id" class="example">
                <i class="pi pi-angle-right"></i>
                <span class="example-text">{{ example.sentenceText }}</span>
                <span v-if="example.translation" class="example-translation">{{ example.translation }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="selectedWord.etymology" class="etymology">
          <h3>Etymology</h3>
          <p>{{ selectedWord.etymology }}</p>
        </div>

        <div v-if="selectedWord.usageNotes" class="usage-notes">
          <h3>Usage Notes</h3>
          <p>{{ selectedWord.usageNotes }}</p>
        </div>

        <div v-if="selectedWord.inflectedForms.length > 0" class="inflected-forms">
          <h3>Inflected Forms</h3>
          <div class="forms-grid">
            <Tag v-for="form in selectedWord.inflectedForms" :key="form.id" :value="form.form" />
          </div>
        </div>
      </div>
      <div v-else-if="isLoadingWord" class="loading-word">
        <ProgressSpinner />
      </div>
      <div v-else-if="wordError" class="word-error">
        <Message severity="error" :closable="false">{{ wordError }}</Message>
      </div>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import Dialog from 'primevue/dialog'
import ProgressSpinner from 'primevue/progressspinner'
import { useVocabularyApi, type Language, type SearchResult, type Word, type WordSummary } from '../composables/useVocabularyApi'

const { getLanguages, searchWords, getWord } = useVocabularyApi()

const languages = ref<Language[]>([])
const selectedLanguage = ref<string>('')
const searchQuery = ref<string>('')
const searchResults = ref<SearchResult | null>(null)
const hasSearched = ref(false)
const isLoadingLanguages = ref(false)
const isSearching = ref(false)
const searchError = ref<string | null>(null)

const showWordDialog = ref(false)
const selectedWord = ref<Word | null>(null)
const isLoadingWord = ref(false)
const wordError = ref<string | null>(null)

async function loadLanguages() {
  isLoadingLanguages.value = true
  try {
    languages.value = await getLanguages()
    // Auto-select first language if available
    if (languages.value.length > 0) {
      selectedLanguage.value = languages.value[0].code
    }
  } catch (error) {
    searchError.value = 'Failed to load languages'
  } finally {
    isLoadingLanguages.value = false
  }
}

async function performSearch() {
  if (!selectedLanguage.value || !searchQuery.value.trim()) {
    return
  }

  isSearching.value = true
  searchError.value = null
  hasSearched.value = true

  try {
    const results = await searchWords(selectedLanguage.value, searchQuery.value.trim())
    searchResults.value = results
  } catch (error) {
    searchError.value = 'Search failed. Please try again.'
  } finally {
    isSearching.value = false
  }
}

async function onWordSelect(event: { data: WordSummary }) {
  if (!selectedLanguage.value) return

  isLoadingWord.value = true
  wordError.value = null
  showWordDialog.value = true
  selectedWord.value = null

  try {
    const word = await getWord(selectedLanguage.value, event.data.lemma)
    if (word) {
      selectedWord.value = word
    } else {
      wordError.value = 'Word details not found'
    }
  } catch (error) {
    wordError.value = 'Failed to load word details'
  } finally {
    isLoadingWord.value = false
  }
}

onMounted(() => {
  loadLanguages()
})
</script>

<style scoped>
.search-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.description {
  color: var(--text-color-secondary);
  margin-bottom: 2rem;
}

.search-section {
  background: var(--surface-card);
  padding: 2rem;
  border-radius: 8px;
  margin-bottom: 2rem;
}

.field {
  margin-bottom: 1.5rem;
}

.field:last-child {
  margin-bottom: 0;
}

.field label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.search-input-wrapper {
  display: flex;
  gap: 1rem;
}

.search-input-wrapper .p-inputtext {
  flex: 1;
}

.results-section {
  margin-top: 2rem;
}

.results-section h2 {
  margin-bottom: 1rem;
}

.no-results {
  text-align: center;
  padding: 3rem;
  color: var(--text-color-secondary);
}

.no-results p {
  margin-top: 1rem;
  font-size: 1.1rem;
}

.word-details {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.word-header {
  border-bottom: 1px solid var(--surface-border);
  padding-bottom: 1rem;
}

.word-title {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.word-title h2 {
  margin: 0;
}

.definitions h3,
.etymology h3,
.usage-notes h3,
.inflected-forms h3 {
  font-size: 1.1rem;
  margin-bottom: 0.75rem;
  color: var(--text-color-secondary);
}

.definition {
  margin-bottom: 1.5rem;
}

.definition-text {
  margin-bottom: 0.5rem;
}

.definition-number {
  font-weight: 600;
  margin-right: 0.5rem;
}

.examples {
  margin-left: 2rem;
  margin-top: 0.5rem;
}

.example {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  font-style: italic;
  color: var(--text-color-secondary);
}

.example-text {
  flex: 1;
}

.example-translation {
  color: var(--text-color);
  font-style: normal;
}

.forms-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.loading-word {
  display: flex;
  justify-content: center;
  padding: 2rem;
}

.word-error {
  padding: 1rem;
}

.text-muted {
  color: var(--text-color-secondary);
}
</style>
