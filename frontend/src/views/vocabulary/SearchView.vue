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
import WordCard from '@/components/vocabulary/WordCard.vue'
import {dictionaryAPI} from '@/api'
import type {Language, SearchResult, Word, WordSummary} from '@/types/dictionary'
import {useVocabularyStore} from '@/stores/vocabulary'
import {useAuthStore} from '@/stores/auth'

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
    languages.value = await dictionaryAPI.getLanguages()
    if (languages.value.length > 0) selectedLanguage.value = languages.value[0]?.code || ''
  } catch {
    searchError.value = 'Failed to load languages'
  } finally {
    isLoadingLanguages.value = false
  }
}

async function performSearch() {
  if (!selectedLanguage.value || !searchQuery.value.trim()) return
  isSearching.value = true
  searchError.value = null
  hasSearched.value = true
  try {
    searchResults.value = await dictionaryAPI.searchWords(selectedLanguage.value, searchQuery.value.trim())
  } catch {
    searchError.value = 'Search failed. Please try again.'
  } finally {
    isSearching.value = false
  }
}

async function loadWord(lemma: string) {
  if (!selectedLanguage.value) return
  showWordDialog.value = true
  isLoadingWord.value = true
  wordError.value = null
  try {
    const word = await dictionaryAPI.getWord(selectedLanguage.value, lemma)
    selectedWord.value = word
    if (!word) wordError.value = 'Word details not found'
  } catch {
    wordError.value = 'Failed to load word details'
  } finally {
    isLoadingWord.value = false
  }
}

async function addToVocabulary(word: WordSummary) {
  if (!authStore.isAuthenticated) {
    toast.add({severity: 'warn', summary: 'Authentication Required', detail: 'Please log in to add words.', life: 3000})
    return
  }
  const result = await vocabularyStore.addWord({wordId: word.id})
  if (result) {
    toast.add({
      severity: 'success',
      summary: 'Word Added',
      detail: `"${word.lemma}" added to your vocabulary.`,
      life: 3000
    })
  } else if (vocabularyStore.error) {
    toast.add({severity: 'error', summary: 'Error', detail: vocabularyStore.error, life: 3000})
  }
}

onMounted(() => {
  loadLanguages()
  if (authStore.isAuthenticated) vocabularyStore.fetchVocabulary()
})
</script>

<template>
  <Toast/>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <h1>Vocabulary Search</h1>
      <p class="text-secondary">Search for words and explore their definitions.</p>
    </div>

    <div class="p-lg bg-surface-card rounded-lg content-area">
      <div class="grid md:grid-cols-3 gap-lg">
        <div class="flex flex-col gap-sm md:col-span-1">
          <label for="language-select" class="font-semibold">Language</label>
          <Select id="language-select" v-model="selectedLanguage" :options="languages" optionLabel="name"
                  optionValue="code" placeholder="Select a language" :loading="isLoadingLanguages"/>
        </div>
        <div class="flex flex-col gap-sm md:col-span-2">
          <label for="search-input" class="font-semibold">Search for a word</label>
          <div class="flex gap-sm">
            <InputText id="search-input" v-model="searchQuery" placeholder="Enter a word..." class="flex-1"
                       :disabled="!selectedLanguage" @keyup.enter="performSearch"/>
            <Button label="Search" icon="pi pi-search" @click="performSearch"
                    :disabled="!selectedLanguage || !searchQuery.trim()" :loading="isSearching"/>
          </div>
        </div>
      </div>
    </div>

    <Message v-if="searchError" severity="error" @close="searchError = null">{{ searchError }}</Message>

    <div v-if="searchResults?.words.length" class="mt-lg">
      <DataTable :value="searchResults.words" :paginator="true" :rows="20" selectionMode="single"
                 @row-select="loadWord($event.data.lemma)" stripedRows responsiveLayout="scroll">
        <Column field="lemma" header="Word" sortable/>
        <Column field="partOfSpeech" header="Part of Speech" sortable>
          <template #body="{data}">
            <Tag v-if="data.partOfSpeech" :value="data.partOfSpeech"/>
          </template>
        </Column>
        <Column field="frequencyRank" header="Frequency" sortable>
          <template #body="{data}">{{ data.frequencyRank?.toLocaleString() || '-' }}</template>
        </Column>
        <Column v-if="authStore.isAuthenticated" header="Vocabulary">
          <template #body="{data}">
            <Button v-if="!vocabularyStore.isWordInVocabulary(data.id)" icon="pi pi-plus" label="Add" size="small"
                    outlined @click.stop="addToVocabulary(data)"/>
            <Button v-else icon="pi pi-check" label="Added" size="small" severity="success" text disabled/>
          </template>
        </Column>
      </DataTable>
    </div>

    <div v-else-if="hasSearched" class="empty-state">
      <i class="pi pi-search empty-icon"></i>
      <h3>No results found</h3>
      <p class="text-secondary">No words found for "{{ searchQuery }}".</p>
    </div>

    <WordCard v-model:visible="showWordDialog" :word="selectedWord" :loading="isLoadingWord" :error="wordError"
              @word-click="loadWord"/>
  </div>
</template>
