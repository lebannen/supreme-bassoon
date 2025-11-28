<script setup lang="ts">
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'
import Message from 'primevue/message'
import Toast from 'primevue/toast'
import {useToast} from 'primevue/usetoast'
import type {Word} from '@/types/dictionary'
import {useVocabularyStore} from '@/stores/vocabulary'
import {useAuthStore} from '@/stores/auth'

interface Props {
  word: Word | null
  visible: boolean
  loading: boolean
  error: string | null
}

defineProps<Props>()
defineEmits(['update:visible', 'word-click'])

const vocabularyStore = useVocabularyStore()
const authStore = useAuthStore()
const toast = useToast()

async function addToVocabulary(word: Word) {
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

const isInVocabulary = (wordId: number) => vocabularyStore.isWordInVocabulary(wordId)
</script>

<template>
  <Toast/>
  <Dialog :visible="visible" @update:visible="$emit('update:visible', $event)" :header="word?.lemma" modal
          :style="{ width: '50vw' }" :breakpoints="{ '960px': '75vw', '640px': '90vw' }">
    <div v-if="word && !loading" class="word-details">
      <div class="flex justify-content-between align-items-center gap-3 pb-lg border-b border-surface">
        <div class="flex align-items-center gap-3 flex-1">
          <h2 class="text-2xl font-bold m-0">{{ word.lemma }}</h2>
          <Tag v-if="word.partOfSpeech" :value="word.partOfSpeech"/>
          <Tag v-if="word.isInflectedForm" value="inflected" severity="secondary"/>
        </div>
        <div v-if="authStore.isAuthenticated">
          <Button v-if="!isInVocabulary(word.id)" icon="pi pi-plus" label="Add" @click="addToVocabulary(word)"
                  outlined/>
          <Button v-else icon="pi pi-check" label="Added" severity="success" text disabled/>
        </div>
      </div>

      <div v-if="word.baseForm" class="p-4 bg-surface-section rounded-lg">
        <h3 class="text-lg font-semibold mb-3">Base Form</h3>
        <div class="flex align-items-center gap-3 mb-3">
          <a class="text-xl font-bold text-primary hover:underline cursor-pointer"
             @click="$emit('word-click', word.baseForm.lemma)">
            {{ word.baseForm.lemma }}
          </a>
          <Tag v-if="word.baseForm.partOfSpeech" :value="word.baseForm.partOfSpeech"/>
        </div>
        <div v-if="word.grammaticalFeatures" class="flex flex-wrap gap-2">
          <Tag v-for="(value, key) in word.grammaticalFeatures" :key="key" :value="`${key}: ${value}`"
               severity="contrast"/>
        </div>
      </div>

      <div v-if="word.definitions.length > 0" class="content-section">
        <h3 class="section-title">Definitions</h3>
        <div v-for="def in word.definitions" :key="def.id" class="mb-4">
          <div class="mb-2">
            <span class="font-bold mr-sm">{{ def.definitionNumber }}.</span>
            <span>{{ def.definitionText }}</span>
          </div>
          <div v-if="def.examples.length > 0" class="examples-section">
            <div v-for="example in def.examples" :key="example.id" class="example">
              <i class="pi pi-chevron-right text-secondary text-sm"></i>
              <div class="flex-1">
                <p class="italic m-0">{{ example.sentenceText }}</p>
                <p v-if="example.translation" class="text-secondary m-0">{{ example.translation }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="word.etymology" class="content-section">
        <h3 class="section-title">Etymology</h3>
        <p class="leading-relaxed">{{ word.etymology }}</p>
      </div>

      <div v-if="word.usageNotes" class="content-section">
        <h3 class="section-title">Usage Notes</h3>
        <p class="leading-relaxed">{{ word.usageNotes }}</p>
      </div>

      <div v-if="word.inflectedForms.length > 0" class="content-section">
        <h3 class="section-title">Inflected Forms</h3>
        <div class="flex flex-wrap gap-2">
          <Tag v-for="form in word.inflectedForms" :key="form.id" :value="form.form" severity="secondary"/>
        </div>
      </div>
    </div>
    <div v-else-if="loading" class="flex justify-content-center p-8">
      <ProgressSpinner/>
    </div>
    <div v-else-if="error">
      <Message severity="error" :closable="false">{{ error }}</Message>
    </div>
  </Dialog>
</template>

<style scoped>
.word-details {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.content-section {
  padding-top: 1rem;
  border-top: 1px solid var(--surface-border);
}

.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 1rem;
}

.examples-section {
  margin-left: 2rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.example {
  display: flex;
  gap: 0.75rem;
}
</style>
