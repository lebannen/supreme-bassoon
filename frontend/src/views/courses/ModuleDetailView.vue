<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {storeToRefs} from 'pinia'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import ProgressSpinner from 'primevue/progressspinner'
import Toast from 'primevue/toast'
import {useToast} from 'primevue/usetoast'
import {useCourseStore} from '@/stores/course'
import {useVocabularyStore} from '@/stores/vocabulary'
import {useAuthStore} from '@/stores/auth'
import {dictionaryAPI, wordSetAPI} from '@/api'
import type {WordSetDetail} from '@/types/wordSet'
import type {Word} from '@/types/dictionary'
import WordCard from '@/components/vocabulary/WordCard.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const courseStore = useCourseStore()
const vocabularyStore = useVocabularyStore()
const authStore = useAuthStore()
const {currentModule: module, loading, error} = storeToRefs(courseStore)

const moduleWordSet = ref<WordSetDetail | null>(null)
const wordSetLoading = ref(false)
const showWordDialog = ref(false)
const selectedWord = ref<Word | null>(null)
const wordLoading = ref(false)
const wordError = ref<string | null>(null)
const importing = ref(false)

const episodeTypeIcon = (type: string) => ({
  STORY: 'pi-book',
  DIALOGUE: 'pi-comments',
  ARTICLE: 'pi-file',
  AUDIO_LESSON: 'pi-volume-up'
}[type] || 'pi-circle')

async function loadWordSet(moduleId: number) {
  wordSetLoading.value = true
  try {
    moduleWordSet.value = await wordSetAPI.getWordSetByModuleId(moduleId)
  } catch (err) {
    console.warn('Could not load module word set:', err)
  } finally {
    wordSetLoading.value = false
  }
}

async function handleWordClick(lemma: string) {
  if (!moduleWordSet.value?.languageCode) return

  wordLoading.value = true
  wordError.value = null
  showWordDialog.value = true

  try {
    const word = await dictionaryAPI.getWord(moduleWordSet.value.languageCode, lemma)
    if (word) {
      selectedWord.value = word
    } else {
      wordError.value = `Word "${lemma}" not found in dictionary`
    }
  } catch (err) {
    wordError.value = err instanceof Error ? err.message : 'Failed to load word'
  } finally {
    wordLoading.value = false
  }
}

async function handleWordCardClick(lemma: string) {
  if (!moduleWordSet.value?.languageCode) return

  wordLoading.value = true
  wordError.value = null

  try {
    const word = await dictionaryAPI.getWord(moduleWordSet.value.languageCode, lemma)
    if (word) {
      selectedWord.value = word
    } else {
      wordError.value = `Word "${lemma}" not found`
    }
  } catch (err) {
    wordError.value = err instanceof Error ? err.message : 'Failed to load word'
  } finally {
    wordLoading.value = false
  }
}

async function importAllWords() {
  if (!moduleWordSet.value?.id || !authStore.isAuthenticated) return

  importing.value = true
  try {
    const result = await wordSetAPI.importWordSet(moduleWordSet.value.id, {addNotes: true})
    toast.add({
      severity: 'success',
      summary: 'Words Imported',
      detail: `${result.addedWords} new words added to your vocabulary.`,
      life: 3000
    })
    // Refresh vocabulary store
    await vocabularyStore.fetchVocabulary()
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Import Failed',
      detail: err instanceof Error ? err.message : 'Failed to import words',
      life: 3000
    })
  } finally {
    importing.value = false
  }
}

onMounted(async () => {
  const moduleId = Number(route.params.id)
  if (moduleId) {
    await courseStore.loadModule(moduleId)
    await loadWordSet(moduleId)
  }
})
</script>

<template>
  <div class="detail-container content-area-lg">
    <div v-if="loading" class="loading-state">
      <ProgressSpinner/>
    </div>
    <Message v-else-if="error" severity="error">{{ error }}</Message>

    <div v-else-if="module" class="content-area-lg">
      <div class="detail-header">
        <Button icon="pi pi-arrow-left" text rounded @click="router.push(`/courses/${module.courseId}`)"
                class="detail-header-back-btn"/>
        <div class="detail-header-content">
          <div class="meta-badges">
            <Tag severity="contrast">Module {{ module.moduleNumber }}</Tag>
            <Tag v-if="module.theme" :value="module.theme"/>
          </div>
          <h1>{{ module.title }}</h1>
          <p v-if="module.description" class="detail-description">{{ module.description }}</p>
          <div class="icon-label-group">
            <span class="icon-label"><i class="pi pi-list"></i>{{ module.episodes.length }} episodes</span>
            <span class="icon-label"><i class="pi pi-clock"></i>~{{ module.estimatedMinutes }} min</span>
          </div>
        </div>
      </div>

      <Card v-if="module.objectives?.length">
        <template #title>
          <div class="card-title-icon"><i class="pi pi-target"></i><span>Module Objectives</span></div>
        </template>
        <template #content>
          <ul class="checklist md:grid md:grid-cols-2">
            <li v-for="(objective, index) in module.objectives" :key="index">
              <i class="pi pi-check-circle text-primary"></i><span>{{ objective }}</span>
            </li>
          </ul>
        </template>
      </Card>

      <!-- Vocabulary Section -->
      <Card v-if="moduleWordSet?.words?.length || wordSetLoading">
        <template #title>
          <div class="flex justify-content-between align-items-center">
            <div class="card-title-icon"><i class="pi pi-bookmark"></i><span>Module Vocabulary</span></div>
            <div class="flex align-items-center gap-2">
              <Tag v-if="moduleWordSet" :value="`${moduleWordSet.wordCount} words`" severity="info"/>
              <Button
                  v-if="authStore.isAuthenticated && moduleWordSet && !moduleWordSet.isImported"
                  label="Add All to Vocabulary"
                  icon="pi pi-plus"
                  size="small"
                  outlined
                  :loading="importing"
                  @click="importAllWords"
              />
              <Tag
                  v-else-if="moduleWordSet?.isImported"
                  value="Added"
                  severity="success"
                  icon="pi pi-check"
              />
            </div>
          </div>
        </template>
        <template #content>
          <div v-if="wordSetLoading" class="flex justify-content-center p-4">
            <ProgressSpinner style="width: 40px; height: 40px"/>
          </div>
          <div v-else-if="moduleWordSet?.words" class="vocabulary-grid">
            <div
                v-for="word in moduleWordSet.words"
                :key="word.id"
                class="vocabulary-word"
                @click="handleWordClick(word.lemma)"
            >
              <span class="word-lemma">{{ word.lemma }}</span>
              <Tag v-if="word.partOfSpeech" :value="word.partOfSpeech" size="small" severity="secondary"/>
            </div>
          </div>
        </template>
      </Card>

      <div class="section">
        <div class="section-header"><h2>Episodes</h2></div>
        <div class="content-grid">
          <Card v-for="episode in module.episodes" :key="episode.id" class="card-interactive"
                @click="router.push(`/episodes/${episode.id}`)">
            <template #header>
              <div class="p-md flex justify-content-between align-items-center bg-surface-section">
                <div class="number-badge-sm bg-primary text-primary-contrast">{{ episode.episodeNumber }}</div>
                <div class="flex gap-2 flex-wrap">
                  <Tag :value="episode.type" :icon="episodeTypeIcon(episode.type)"/>
                  <Tag v-if="episode.hasAudio" value="Audio" severity="success" icon="pi pi-volume-up"/>
                </div>
              </div>
            </template>
            <template #title>{{ episode.title }}</template>
            <template #footer>
              <div class="icon-label-group compact">
                <span class="icon-label"><i class="pi pi-list"></i>{{ episode.totalExercises }} exercises</span>
                <span class="icon-label"><i class="pi pi-clock"></i>~{{ episode.estimatedMinutes }} min</span>
              </div>
            </template>
          </Card>
        </div>
      </div>
    </div>

    <Toast/>

    <WordCard
        v-model:visible="showWordDialog"
        :word="selectedWord"
        :loading="wordLoading"
        :error="wordError"
        @word-click="handleWordCardClick"
    />
  </div>
</template>

<style scoped>
.vocabulary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 0.75rem;
}

.vocabulary-word {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--surface-50);
  border: 1px solid var(--surface-200);
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.15s;
}

.vocabulary-word:hover {
  background: var(--primary-50);
  border-color: var(--primary-200);
  transform: translateY(-1px);
}

.word-lemma {
  font-weight: 500;
  color: var(--text-color);
}

:deep(.dark) .vocabulary-word {
  background: var(--surface-800);
  border-color: var(--surface-700);
}

:deep(.dark) .vocabulary-word:hover {
  background: var(--primary-900);
  border-color: var(--primary-700);
}
</style>
