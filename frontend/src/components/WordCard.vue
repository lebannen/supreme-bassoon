<template>
  <Toast />
  <Dialog :visible="visible" @update:visible="$emit('update:visible', $event)" :header="word?.lemma" :modal="true" :style="{ width: '50vw' }" :breakpoints="{ '960px': '75vw', '640px': '90vw' }">
    <div v-if="word && !loading" class="word-details">
      <div class="word-header">
        <div class="word-title">
          <h2>{{ word.lemma }}</h2>
          <Tag v-if="word.partOfSpeech" :value="word.partOfSpeech" severity="info" />
          <Tag v-if="word.isInflectedForm" value="inflected form" severity="secondary" />
        </div>
        <div v-if="authStore.isAuthenticated" class="vocabulary-action">
          <Button
            v-if="!isInVocabulary(word.id)"
            icon="pi pi-bookmark"
            label="Add to Vocabulary"
            @click="addToVocabulary(word)"
            outlined
          />
          <Button
            v-else
            icon="pi pi-check"
            label="In Vocabulary"
            severity="success"
            text
            disabled
          />
        </div>
      </div>

      <div v-if="word.baseForm" class="base-form-section">
        <h3>Base Form</h3>
        <div class="base-form-info">
          <a class="base-form-lemma" @click="$emit('word-click', word.baseForm.lemma)">
            {{ word.baseForm.lemma }}
          </a>
          <Tag v-if="word.baseForm.partOfSpeech" :value="word.baseForm.partOfSpeech" severity="info" />
        </div>
        <div v-if="word.grammaticalFeatures" class="grammatical-features">
          <Tag
            v-for="(value, key) in word.grammaticalFeatures"
            :key="key"
            :value="`${key}: ${value}`"
            severity="secondary"
          />
        </div>
      </div>

      <div v-if="word.definitions.length > 0" class="definitions">
        <h3>Definitions</h3>
        <div v-for="def in word.definitions" :key="def.id" class="definition">
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

      <div v-if="word.etymology" class="etymology">
        <h3>Etymology</h3>
        <p>{{ word.etymology }}</p>
      </div>

      <div v-if="word.usageNotes" class="usage-notes">
        <h3>Usage Notes</h3>
        <p>{{ word.usageNotes }}</p>
      </div>

      <div v-if="word.inflectedForms.length > 0" class="inflected-forms">
        <h3>Inflected Forms</h3>
        <div class="forms-grid">
          <Tag v-for="form in word.inflectedForms" :key="form.id" :value="form.form" />
        </div>
      </div>
    </div>
    <div v-else-if="loading" class="loading-word">
      <ProgressSpinner />
    </div>
    <div v-else-if="error" class="word-error">
      <Message severity="error" :closable="false">{{ error }}</Message>
    </div>
  </Dialog>
</template>

<script setup lang="ts">
import Dialog from 'primevue/dialog';
import Tag from 'primevue/tag';
import Button from 'primevue/button';
import ProgressSpinner from 'primevue/progressspinner';
import Message from 'primevue/message';
import Toast from 'primevue/toast';
import { useToast } from 'primevue/usetoast';
import type { Word } from '../composables/useVocabularyApi';
import { useVocabularyStore } from '@/stores/vocabulary';
import { useAuthStore } from '@/stores/auth';

interface Props {
  word: Word | null;
  visible: boolean;
  loading: boolean;
  error: string | null;
}

defineProps<Props>();
defineEmits(['update:visible', 'word-click']);

const vocabularyStore = useVocabularyStore();
const authStore = useAuthStore();
const toast = useToast();

async function addToVocabulary(word: Word) {
  if (!authStore.isAuthenticated) {
    toast.add({
      severity: 'warn',
      summary: 'Authentication Required',
      detail: 'Please log in to add words to your vocabulary',
      life: 3000,
    });
    return;
  }

  const result = await vocabularyStore.addWord({
    wordId: word.id,
  });

  if (result) {
    toast.add({
      severity: 'success',
      summary: 'Word Added',
      detail: `"${word.lemma}" has been added to your vocabulary`,
      life: 3000,
    });
  } else if (vocabularyStore.error) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: vocabularyStore.error,
      life: 3000,
    });
  }
}

function isInVocabulary(wordId: number): boolean {
  return vocabularyStore.isWordInVocabulary(wordId);
}
</script>

<style scoped>
.word-details {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.word-header {
  border-bottom: 1px solid var(--surface-border);
  padding-bottom: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.word-title {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
}

.vocabulary-action {
  display: flex;
  align-items: center;
}

.word-title h2 {
  margin: 0;
  color: var(--text-color);
}

.base-form-section {
  padding: 1rem;
  background: var(--surface-section);
  border: 1px solid var(--surface-border);
  border-radius: var(--border-radius);
}

.base-form-section h3 {
  font-size: 1.1rem;
  margin-top: 0;
  margin-bottom: 0.75rem;
  color: var(--text-color-secondary);
  font-weight: 600;
}

.base-form-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.base-form-lemma {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--primary-color);
  cursor: pointer;
  text-decoration: none;
  transition: opacity 0.2s;
}

.base-form-lemma:hover {
  opacity: 0.7;
  text-decoration: underline;
}

.grammatical-features {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.definitions h3,
.etymology h3,
.usage-notes h3,
.inflected-forms h3 {
  font-size: 1.1rem;
  margin-bottom: 0.75rem;
  color: var(--text-color-secondary);
  font-weight: 600;
}

.definition {
  margin-bottom: 1.5rem;
}

.definition:last-child {
  margin-bottom: 0;
}

.definition-text {
  margin-bottom: 0.5rem;
  color: var(--text-color);
  line-height: 1.6;
}

.definition-number {
  font-weight: 600;
  margin-right: 0.5rem;
  color: var(--primary-color);
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
  line-height: 1.5;
}

.example-text {
  flex: 1;
}

.example-translation {
  color: var(--text-color);
  font-style: normal;
}

.etymology p,
.usage-notes p {
  color: var(--text-color-secondary);
  line-height: 1.7;
}

.forms-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.loading-word {
  display: flex;
  justify-content: center;
  padding: 3rem;
}

.word-error {
  padding: 1rem;
}
</style>
