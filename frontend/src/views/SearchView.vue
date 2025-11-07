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
import { ref, onMounted } from 'vue';
import Select from 'primevue/select';
import InputText from 'primevue/inputtext';
import Button from 'primevue/button';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Tag from 'primevue/tag';
import Message from 'primevue/message';
import WordCard from '../components/WordCard.vue';
import { useVocabularyApi, type Language, type SearchResult, type Word, type WordSummary } from '../composables/useVocabularyApi';

const { getLanguages, searchWords, getWord } = useVocabularyApi();

const languages = ref<Language[]>([]);
const selectedLanguage = ref<string>('');
const searchQuery = ref<string>('');
const searchResults = ref<SearchResult | null>(null);
const hasSearched = ref(false);
const isLoadingLanguages = ref(false);
const isSearching = ref(false);
const searchError = ref<string | null>(null);

const showWordDialog = ref(false);
const selectedWord = ref<Word | null>(null);
const isLoadingWord = ref(false);
const wordError = ref<string | null>(null);

async function loadLanguages() {
  isLoadingLanguages.value = true;
  try {
    languages.value = await getLanguages();
    if (languages.value.length > 0) {
      selectedLanguage.value = languages.value[0].code;
    }
  } catch (error) {
    searchError.value = 'Failed to load languages';
  } finally {
    isLoadingLanguages.value = false;
  }
}

async function performSearch() {
  if (!selectedLanguage.value || !searchQuery.value.trim()) {
    return;
  }
  isSearching.value = true;
  searchError.value = null;
  hasSearched.value = true;
  try {
    const results = await searchWords(selectedLanguage.value, searchQuery.value.trim());
    searchResults.value = results;
  } catch (error) {
    searchError.value = 'Search failed. Please try again.';
  } finally {
    isSearching.value = false;
  }
}

function onWordSelect(event: { data: WordSummary }) {
  loadWord(event.data.lemma);
}

async function loadWord(lemma: string) {
  if (!selectedLanguage.value) return;

  showWordDialog.value = true;
  isLoadingWord.value = true;
  wordError.value = null;
  selectedWord.value = null;

  try {
    const word = await getWord(selectedLanguage.value, lemma);
    if (word) {
      selectedWord.value = word;
    } else {
      wordError.value = 'Word details not found';
    }
  } catch (error) {
    wordError.value = 'Failed to load word details';
  } finally {
    isLoadingWord.value = false;
  }
}

onMounted(() => {
  loadLanguages();
});
</script>

<style scoped>
.search-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

h1 {
  color: var(--text-color);
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.description {
  color: var(--text-color-secondary);
  margin-bottom: 2rem;
}

.search-section {
  background: var(--surface-card);
  padding: 1.5rem;
  border-radius: var(--border-radius);
  border: 1px solid var(--surface-border);
  margin-bottom: 1.5rem;
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
  color: var(--text-color);
}

.search-input-wrapper {
  display: flex;
  gap: 0.75rem;
}

.search-input-wrapper .p-inputtext {
  flex: 1;
}

.results-section {
  margin-top: 1.5rem;
}

.results-section h2 {
  margin-bottom: 1rem;
  color: var(--text-color);
  font-weight: 600;
}

.no-results {
  text-align: center;
  padding: 4rem 2rem;
  background: var(--surface-card);
  border-radius: var(--border-radius);
  border: 1px solid var(--surface-border);
  color: var(--text-color-secondary);
}

.no-results p {
  margin-top: 1rem;
  font-size: 1.1rem;
  color: var(--text-color-secondary);
}

.text-muted {
  color: var(--text-color-secondary);
}

@media (max-width: 768px) {
  .search-container {
    padding: 1.5rem 1rem;
  }

  .search-section {
    padding: 1.25rem;
  }

  .search-input-wrapper {
    flex-direction: column;
  }
}
</style>
