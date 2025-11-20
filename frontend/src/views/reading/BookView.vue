<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import BookComponent from '@/components/reading/BookComponent.vue'
import WordCard from '@/components/vocabulary/WordCard.vue'
import Select from 'primevue/select'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import {dictionaryAPI} from '@/api'
import {useReadingStore} from '@/stores/reading'
import type {Language, Word} from '@/types/dictionary'

const router = useRouter()
const route = useRoute()
const readingStore = useReadingStore()
const {currentText, currentProgress: progress, loading, error} = storeToRefs(readingStore)

const id = computed(() => Number(route.params.id) || null)
const showWordDialog = ref(false)
const selectedWord = ref<Word | null>(null)
const isLoadingWord = ref(false)
const wordError = ref<string | null>(null)
const bookLanguage = computed(() => currentText.value?.languageCode)

async function loadText() {
  if (id.value) await readingStore.loadTextById(id.value)
}

async function onWordClick(lemma: string) {
  if (!bookLanguage.value) return
  showWordDialog.value = true
  isLoadingWord.value = true
  wordError.value = null
  try {
    const word = await dictionaryAPI.getWord(bookLanguage.value, lemma)
    selectedWord.value = word
    if (!word) wordError.value = 'Word details not found.'
  } catch {
    wordError.value = 'Failed to load word details.'
  } finally {
    isLoadingWord.value = false
  }
}

async function onPageChange(currentPage: number, totalPages: number) {
  if (id.value) await readingStore.updateProgress(id.value, currentPage, totalPages)
}

const getLanguageName = (code: string) => readingStore.getLanguageName(code)
const formatTopic = (topic: string) => topic.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())

onMounted(loadText)
</script>

<template>
  <div class="page-container">
    <div v-if="loading" class="loading-state">
      <ProgressSpinner/>
    </div>
    <Message v-else-if="error" severity="error">{{ error }}</Message>
    <div v-else-if="currentText" class="text-reader">
      <div class="text-header bg-surface-card">
        <Button icon="pi pi-arrow-left" text rounded @click="router.push('/reading')" aria-label="Back to library"/>
        <div class="text-info">
          <h1 class="truncate">{{ currentText.title }}</h1>
          <div class="meta">
            <Tag v-if="currentText.level" :value="currentText.level"/>
            <Tag :value="getLanguageName(currentText.languageCode)" severity="secondary"/>
            <Tag v-if="currentText.topic" :value="formatTopic(currentText.topic)" severity="contrast"/>
          </div>
        </div>
        <div class="ml-auto">
          <Tag v-if="progress?.completed" value="Completed" icon="pi pi-check-circle" severity="success"/>
        </div>
      </div>
      <BookComponent :content="currentText.content" :audio-url="currentText.audioUrl" :page-size="300"
                     @word-click="onWordClick" @page-change="onPageChange"/>
    </div>
    <div v-else class="empty-state">
      <i class="pi pi-book empty-icon"></i>
      <h3>Text not found</h3>
      <Button label="Back to Library" @click="router.push('/reading')"/>
    </div>
    <WordCard v-model:visible="showWordDialog" :word="selectedWord" :loading="isLoadingWord" :error="wordError"
              @word-click="onWordClick"/>
  </div>
</template>
