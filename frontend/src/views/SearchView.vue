<template>
  <Toast />
  <div class="search-container">
    <div class="page-header">
      <h1>Vocabulary Search</h1>
      <p>Search for words and explore their definitions</p>
    </div>

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
      <div class="section-header">
        <h2>Search Results ({{ searchResults.total }})</h2>
      </div>
      <DataTable
          :value="searchResults.words"
          :paginator="true"
          :rows="20"
          selectionMode="single"
          @row-select="onWordSelect"
      >
        <Column field="lemma" header="Word" sortable></Column>
        <Column field="partOfSpeech" header="Part of Speech" sortable>
          <template #body="slotProps">
            <Tag
                v-if="slotProps.data.partOfSpeech"
                :value="slotProps.data.partOfSpeech"
                severity="info"
            />
          </template>
        </Column>
        <Column field="frequencyRank" header="Frequency" sortable>
          <template #body="slotProps">
            <span v-if="slotProps.data.frequencyRank">{{
                slotProps.data.frequencyRank.toLocaleString()
              }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </Column>
        <Column header="Vocabulary" v-if="authStore.isAuthenticated">
          <template #body="slotProps">
            <Button
              v-if="!isInVocabulary(slotProps.data.id)"
              icon="pi pi-bookmark"
              label="Add"
              size="small"
              outlined
              @click.stop="addToVocabulary(slotProps.data)"
              :aria-label="`Add ${slotProps.data.lemma} to vocabulary`"
            />
            <Button
              v-else
              icon="pi pi-check"
              label="In Vocabulary"
              size="small"
              severity="success"
              text
              disabled
              :aria-label="`${slotProps.data.lemma} is already in vocabulary`"
            />
          </template>
        </Column>
      </DataTable>
    </div>

    <div
        v-else-if="hasSearched && searchResults && searchResults.words.length === 0"
        class="empty-state"
    >
      <i class="pi pi-search empty-icon"></i>
      <h3>No results found</h3>
      <p>No words found for "{{ searchQuery }}"</p>
    </div>

    <WordCard
      v-model:visible="showWordDialog"
      :word="selectedWord"
      :loading="isLoadingWord"
      :error="wordError"
      @word-click="loadWord"
    />
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import Toast from 'primevue/toast'
import {useToast} from 'primevue/usetoast'
import WordCard from '../components/WordCard.vue'
import {
  type Language,
  type SearchResult,
  useVocabularyApi,
  type Word,
  type WordSummary,
} from '../composables/useVocabularyApi'
import {useVocabularyStore} from '@/stores/vocabulary'
import {useAuthStore} from '@/stores/auth'

const {getLanguages, searchWords, getWord} = useVocabularyApi()
const vocabularyStore = useVocabularyStore()
const authStore = useAuthStore()
const toast = useToast()

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
    if (languages.value.length > 0) {
      selectedLanguage.value = languages.value[0].code
    }
  } catch {
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
  } catch {
    searchError.value = 'Search failed. Please try again.'
  } finally {
    isSearching.value = false
  }
}

function onWordSelect(event: { data: WordSummary }) {
  loadWord(event.data.lemma)
}

async function loadWord(lemma: string) {
  if (!selectedLanguage.value) return

  showWordDialog.value = true
  isLoadingWord.value = true
  wordError.value = null
  selectedWord.value = null

  try {
    const word = await getWord(selectedLanguage.value, lemma)
    if (word) {
      selectedWord.value = word
    } else {
      wordError.value = 'Word details not found'
    }
  } catch {
    wordError.value = 'Failed to load word details'
  } finally {
    isLoadingWord.value = false
  }
}

async function addToVocabulary(word: WordSummary) {
  if (!authStore.isAuthenticated) {
    toast.add({
      severity: 'warn',
      summary: 'Authentication Required',
      detail: 'Please log in to add words to your vocabulary',
      life: 3000,
    })
    return
  }

  const result = await vocabularyStore.addWord({
    wordId: word.id,
  })

  if (result) {
    toast.add({
      severity: 'success',
      summary: 'Word Added',
      detail: `"${word.lemma}" has been added to your vocabulary`,
      life: 3000,
    })
  } else if (vocabularyStore.error) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: vocabularyStore.error,
      life: 3000,
    })
  }
}

function isInVocabulary(wordId: number): boolean {
  return vocabularyStore.isWordInVocabulary(wordId)
}

onMounted(() => {
  loadLanguages()
  if (authStore.isAuthenticated) {
    vocabularyStore.fetchVocabulary()
  }
})
</script>

<style scoped>
.search-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-2xl) var(--spacing-xl);
}

.search-section {
  background: var(--surface-card);
  padding: var(--spacing-xl);
  border-radius: var(--border-radius);
  border: 1px solid var(--surface-border);
  margin-bottom: var(--spacing-xl);
}

.field {
  margin-bottom: var(--spacing-xl);
}

.field:last-child {
  margin-bottom: 0;
}

.field label {
  display: block;
  margin-bottom: var(--spacing-xs);
  font-weight: 600;
  color: var(--text-primary);
}

.search-input-wrapper {
  display: flex;
  gap: var(--spacing-sm);
}

.search-input-wrapper .p-inputtext {
  flex: 1;
}

.results-section {
  margin-top: var(--spacing-xl);
}

.text-muted {
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .search-container {
    padding: var(--spacing-xl) var(--spacing-md);
  }

  .search-section {
    padding: var(--spacing-lg);
  }

  .search-input-wrapper {
    flex-direction: column;
  }
}
</style>
