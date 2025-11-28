<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
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
import {useProgressStore} from '@/stores/progress'
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
const progressStore = useProgressStore()
const {currentModule: module, loading, error} = storeToRefs(courseStore)

const accessDenied = ref(false)

const moduleWordSet = ref<WordSetDetail | null>(null)
const wordSetLoading = ref(false)
const showWordDialog = ref(false)
const selectedWord = ref<Word | null>(null)
const wordLoading = ref(false)
const wordError = ref<string | null>(null)
const importing = ref(false)
const vocabularyExpanded = ref(false)

// Episode progress tracking
const completedEpisodes = computed(() => {
  if (!module.value?.courseId) return new Set<number>()
  const enrollment = progressStore.getEnrollment(module.value.courseId)
  // For now, track based on completed count - in future could be per-episode
  return new Set<number>()
})

const currentEpisodeId = computed(() => {
  if (!module.value?.courseId) return null
  const enrollment = progressStore.getEnrollment(module.value.courseId)
  return enrollment?.currentEpisodeId || null
})

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

    // Check enrollment
    if (module.value?.courseId && !progressStore.isEnrolled(module.value.courseId)) {
      accessDenied.value = true
      return
    }

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

    <!-- Access Denied - Not Enrolled -->
    <div v-else-if="accessDenied" class="access-denied-container">
      <Card class="access-denied-card">
        <template #content>
          <div class="text-center p-5">
            <i class="pi pi-lock text-5xl text-secondary mb-4"></i>
            <h2 class="text-2xl font-bold mb-2">Module Locked</h2>
            <p class="text-secondary mb-4">
              You need to enroll in this course to access its modules and episodes.
            </p>
            <div class="flex justify-content-center gap-3">
              <Button
                  label="Go Back"
                  icon="pi pi-arrow-left"
                  @click="router.back()"
              />
              <Button
                  label="Browse Courses"
                  severity="secondary"
                  outlined
                  @click="router.push('/courses')"
              />
            </div>
          </div>
        </template>
      </Card>
    </div>

    <div v-else-if="module" class="content-area-lg">
      <div class="detail-header">
        <Button icon="pi pi-arrow-left" text rounded @click="router.push(`/courses/${module.courseSlug}`)"
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

      <!-- Vocabulary Section (Collapsible) -->
      <Card v-if="moduleWordSet?.words?.length || wordSetLoading" class="vocabulary-card">
        <template #content>
          <div class="vocabulary-header" @click="vocabularyExpanded = !vocabularyExpanded">
            <div class="flex align-items-center gap-3">
              <i class="pi pi-bookmark text-primary"></i>
              <span class="font-semibold">Module Vocabulary</span>
              <Tag v-if="moduleWordSet" :value="`${moduleWordSet.wordCount} words`" severity="info"/>
            </div>
            <div class="flex align-items-center gap-2">
              <Button
                  v-if="authStore.isAuthenticated && moduleWordSet && !moduleWordSet.isImported"
                  label="Add All"
                  icon="pi pi-plus"
                  size="small"
                  outlined
                  :loading="importing"
                  @click.stop="importAllWords"
              />
              <Tag
                  v-else-if="moduleWordSet?.isImported"
                  value="Added"
                  severity="success"
                  icon="pi pi-check"
              />
              <Button
                  :icon="vocabularyExpanded ? 'pi pi-chevron-up' : 'pi pi-chevron-down'"
                  text
                  rounded
                  size="small"
                  @click.stop="vocabularyExpanded = !vocabularyExpanded"
              />
            </div>
          </div>
          <div v-if="vocabularyExpanded" class="vocabulary-content">
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
          </div>
        </template>
      </Card>

      <div class="section">
        <div class="section-header"><h2>Episodes</h2></div>
        <div class="content-grid">
          <Card v-for="episode in module.episodes" :key="episode.id" class="card-interactive episode-card"
                :class="{ 'current-episode': currentEpisodeId === episode.id }"
                @click="router.push(`/episodes/${episode.id}`)">
            <template #header>
              <div class="episode-card-header">
                <div class="number-badge number-badge-sm"
                     :class="completedEpisodes.has(episode.id) ? 'bg-green-500 text-white' : 'bg-primary text-primary-contrast'">
                  <i v-if="completedEpisodes.has(episode.id)" class="pi pi-check"></i>
                  <span v-else>{{ episode.episodeNumber }}</span>
                </div>
                <div class="flex gap-2 flex-wrap">
                  <Tag :value="episode.type" :icon="episodeTypeIcon(episode.type)"/>
                  <Tag v-if="episode.hasAudio" value="Audio" severity="success" icon="pi pi-volume-up"/>
                </div>
              </div>
            </template>
            <template #title>
              <div class="flex align-items-center gap-2">
                <span>{{ episode.title }}</span>
                <Tag v-if="currentEpisodeId === episode.id" value="Continue" severity="warn" size="small"/>
              </div>
            </template>
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
/* Vocabulary Section */
.vocabulary-card :deep(.p-card-body) {
  padding: 0;
}

.vocabulary-card :deep(.p-card-content) {
  padding: 0;
}

.vocabulary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  cursor: pointer;
  transition: background-color 0.15s;
}

.vocabulary-header:hover {
  background: var(--surface-50);
}

.vocabulary-content {
  padding: 0 1.25rem 1.25rem;
  border-top: 1px solid var(--surface-200);
}

.vocabulary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 0.75rem;
  padding-top: 1rem;
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

:deep(.dark) .vocabulary-header:hover {
  background: var(--surface-800);
}

:deep(.dark) .vocabulary-content {
  border-top-color: var(--surface-700);
}

:deep(.dark) .vocabulary-word {
  background: var(--surface-800);
  border-color: var(--surface-700);
}

:deep(.dark) .vocabulary-word:hover {
  background: var(--primary-900);
  border-color: var(--primary-700);
}

/* Episode Cards */
.episode-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: var(--surface-50);
}

:deep(.dark) .episode-card-header {
  background: var(--surface-800);
}

.episode-card.current-episode {
  border: 2px solid var(--yellow-500);
  box-shadow: 0 0 0 3px var(--yellow-100);
}

:deep(.dark) .episode-card.current-episode {
  box-shadow: 0 0 0 3px var(--yellow-900);
}

/* Access Denied State */
.access-denied-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 50vh;
}

.access-denied-card {
  max-width: 500px;
  width: 100%;
}
</style>
