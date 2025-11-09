<template>
  <div class="word-sets-container">
    <Toast />

    <div class="word-sets-content">
      <div class="header">
        <h1 class="title">
          <i class="pi pi-bookmark"></i>
          Word Sets
        </h1>
        <p class="subtitle">Pre-made collections of words to quickly build your vocabulary</p>
      </div>

      <div class="controls-section">
        <div class="language-filter">
          <label for="language-select">Filter by Language</label>
          <Select
            id="language-select"
            v-model="selectedLanguage"
            :options="languages"
            optionLabel="name"
            optionValue="code"
            placeholder="Select a language"
            @change="loadWordSets"
            class="language-select"
          />
        </div>

        <div class="admin-section">
          <Button
            label="Load from JSON"
            icon="pi pi-upload"
            severity="secondary"
            outlined
            @click="showUploadDialog = true"
          />
        </div>
      </div>

      <!-- Upload Dialog -->
      <Dialog
        v-model:visible="showUploadDialog"
        header="Load Word Sets from JSON"
        :modal="true"
        class="upload-dialog"
      >
        <div class="upload-form">
          <Message severity="info" :closable="false">
            Upload a JSON file with word sets. The file should include "language" and optionally "level" fields.
          </Message>

          <div class="field">
            <label>Select JSON File</label>
            <FileUpload
              mode="basic"
              accept=".json"
              :auto="false"
              @select="onFileSelect"
              chooseLabel="Choose File"
            />
            <small v-if="uploadFile" class="file-name">{{ uploadFile.name }}</small>
          </div>

          <Message v-if="uploadError" severity="error" :closable="false">
            {{ uploadError }}
          </Message>
        </div>

        <template #footer>
          <Button
            label="Cancel"
            text
            @click="showUploadDialog = false"
          />
          <Button
            label="Load Word Sets"
            icon="pi pi-upload"
            @click="uploadWordSets"
            :disabled="!uploadFile || isUploading"
            :loading="isUploading"
          />
        </template>
      </Dialog>

      <Message v-if="error" severity="error" :closable="false" class="error-message">
        {{ error }}
      </Message>

      <div v-if="loading" class="loading-state">
        <ProgressSpinner />
      </div>

      <div v-else-if="wordSets.length === 0" class="empty-state">
        <Card>
          <template #content>
            <div class="empty-content">
              <i class="pi pi-inbox empty-icon"></i>
              <h3>No word sets found</h3>
              <p v-if="selectedLanguage">
                No word sets available for {{ languages.find(l => l.code === selectedLanguage)?.name }}
              </p>
              <p v-else>Select a language to view available word sets</p>
            </div>
          </template>
        </Card>
      </div>

      <div v-else class="word-sets-grid">
        <Card v-for="wordSet in wordSets" :key="wordSet.id" class="word-set-card">
          <template #header>
            <div class="card-header">
              <div class="card-header-content">
                <Tag v-if="wordSet.level" :value="wordSet.level" severity="info" class="level-tag" />
                <Tag v-if="wordSet.isImported" value="Imported" severity="success" icon="pi pi-check" />
              </div>
            </div>
          </template>
          <template #title>
            <span class="word-set-name">{{ wordSet.name }}</span>
          </template>
          <template #subtitle>
            <span v-if="wordSet.theme" class="theme">{{ wordSet.theme }}</span>
          </template>
          <template #content>
            <p v-if="wordSet.description" class="description">{{ wordSet.description }}</p>

            <div class="word-set-stats">
              <div class="stat">
                <i class="pi pi-book"></i>
                <span>{{ wordSet.wordCount }} words</span>
              </div>
              <div v-if="authStore.isAuthenticated && wordSet.userVocabularyCount > 0" class="stat">
                <i class="pi pi-check-circle"></i>
                <span>{{ wordSet.userVocabularyCount }}/{{ wordSet.wordCount }} in your vocabulary</span>
              </div>
            </div>
          </template>
          <template #footer>
            <div class="card-actions">
              <Button
                label="View Details"
                icon="pi pi-eye"
                text
                @click="viewWordSetDetails(wordSet.id)"
              />
              <Button
                v-if="authStore.isAuthenticated"
                :label="wordSet.isImported ? 'Re-import' : 'Import'"
                :icon="wordSet.isImported ? 'pi pi-refresh' : 'pi pi-download'"
                @click="confirmImportWordSet(wordSet)"
                :loading="importingSetId === wordSet.id"
                :disabled="importingSetId !== null && importingSetId !== wordSet.id"
              />
              <Button
                v-else
                label="Login to Import"
                icon="pi pi-sign-in"
                @click="goToLogin"
              />
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import ProgressSpinner from 'primevue/progressspinner'
import Dialog from 'primevue/dialog'
import FileUpload from 'primevue/fileupload'
import { useWordSetApi } from '@/composables/useWordSetApi'
import { useAuthStore } from '@/stores/auth'
import { useVocabularyStore } from '@/stores/vocabulary'
import type { WordSet } from '@/types/wordSet'

const router = useRouter()
const toast = useToast()
const confirm = useConfirm()
const authStore = useAuthStore()
const vocabularyStore = useVocabularyStore()
const { getWordSetsByLanguage, importWordSet, loadWordSetsFromJson } = useWordSetApi()

const languages = [
  { code: 'fr', name: 'French' },
  { code: 'de', name: 'German' },
  { code: 'es', name: 'Spanish' },
  { code: 'it', name: 'Italian' },
  { code: 'pt', name: 'Portuguese' },
]

const selectedLanguage = ref<string>('')
const wordSets = ref<WordSet[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const importingSetId = ref<number | null>(null)

// Upload dialog state
const showUploadDialog = ref(false)
const uploadFile = ref<File | null>(null)
const uploadError = ref<string | null>(null)
const isUploading = ref(false)

async function loadWordSets() {
  if (!selectedLanguage.value) {
    wordSets.value = []
    return
  }

  loading.value = true
  error.value = null

  try {
    wordSets.value = await getWordSetsByLanguage(selectedLanguage.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load word sets'
  } finally {
    loading.value = false
  }
}

function viewWordSetDetails(id: number) {
  // TODO: Implement word set detail view
  toast.add({
    severity: 'info',
    summary: 'Coming Soon',
    detail: 'Word set detail view is coming soon!',
    life: 3000
  })
}

function confirmImportWordSet(wordSet: WordSet) {
  const newWords = wordSet.wordCount - wordSet.userVocabularyCount

  confirm.require({
    message: wordSet.isImported
      ? `Re-import "${wordSet.name}"? This will add ${newWords} new words to your vocabulary.`
      : `Import "${wordSet.name}" with ${wordSet.wordCount} words to your vocabulary?`,
    header: 'Confirm Import',
    icon: 'pi pi-download',
    acceptLabel: 'Import',
    rejectLabel: 'Cancel',
    accept: async () => {
      await importWordSetToVocabulary(wordSet.id)
    }
  })
}

async function importWordSetToVocabulary(id: number) {
  importingSetId.value = id

  try {
    const result = await importWordSet(id, {
      wordSetId: id,
      addNotes: true
    })

    if (result) {
      toast.add({
        severity: 'success',
        summary: 'Word Set Imported',
        detail: result.message,
        life: 5000
      })

      // Refresh vocabulary and word sets
      await Promise.all([
        vocabularyStore.fetchVocabulary(),
        loadWordSets()
      ])
    } else {
      throw new Error('Failed to import word set')
    }
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Import Failed',
      detail: err instanceof Error ? err.message : 'Failed to import word set',
      life: 3000
    })
  } finally {
    importingSetId.value = null
  }
}

function goToLogin() {
  router.push('/login')
}

function onFileSelect(event: any) {
  const files = event.files
  if (files && files.length > 0) {
    uploadFile.value = files[0]
    uploadError.value = null
  }
}

async function uploadWordSets() {
  if (!uploadFile.value) return

  isUploading.value = true
  uploadError.value = null

  try {
    const result = await loadWordSetsFromJson(uploadFile.value)

    if (result) {
      toast.add({
        severity: 'success',
        summary: 'Word Sets Loaded',
        detail: `${result.count} word sets created successfully`,
        life: 5000
      })

      // Close dialog and reset form
      showUploadDialog.value = false
      uploadFile.value = null

      // Reload current language view
      await loadWordSets()
    }
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : 'Failed to load word sets'
  } finally {
    isUploading.value = false
  }
}

onMounted(() => {
  // Default to French
  selectedLanguage.value = 'fr'
  loadWordSets()
})
</script>

<style scoped>
.word-sets-container {
  min-height: 100vh;
  background: var(--surface-ground);
  padding: 2rem 1rem;
}

.word-sets-content {
  max-width: 1400px;
  margin: 0 auto;
}

.header {
  margin-bottom: 2rem;
}

.title {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--text-color);
  margin: 0 0 0.5rem 0;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.title i {
  color: var(--primary-color);
  font-size: 2rem;
}

.subtitle {
  color: var(--text-color-secondary);
  font-size: 1.125rem;
  margin: 0;
}

.controls-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 2rem;
  gap: 1rem;
  flex-wrap: wrap;
}

.language-filter label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: var(--text-color);
}

.language-select {
  max-width: 300px;
}

.admin-section {
  display: flex;
  align-items: center;
}

.upload-dialog {
  width: 500px;
  max-width: 90vw;
}

.upload-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.upload-form .field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.upload-form .field label {
  font-weight: 600;
  color: var(--text-color);
}

.file-name {
  display: block;
  margin-top: 0.5rem;
  color: var(--text-color-secondary);
  font-style: italic;
}

.w-full {
  width: 100%;
}

.error-message {
  margin-bottom: 1.5rem;
}

.loading-state {
  display: flex;
  justify-content: center;
  padding: 4rem 2rem;
}

.empty-state {
  margin-top: 2rem;
}

.empty-content {
  text-align: center;
  padding: 3rem 2rem;
}

.empty-icon {
  font-size: 4rem;
  color: var(--text-color-secondary);
  margin-bottom: 1rem;
}

.empty-content h3 {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 0.5rem;
}

.empty-content p {
  color: var(--text-color-secondary);
  font-size: 1rem;
}

.word-sets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 1.5rem;
  margin-top: 1rem;
}

.word-set-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  padding: 1rem;
  background: var(--surface-50);
  border-bottom: 1px solid var(--surface-border);
}

.card-header-content {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
}

.level-tag {
  font-weight: 600;
}

.word-set-name {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
}

.theme {
  color: var(--text-color-secondary);
  font-size: 0.875rem;
  font-style: italic;
}

.description {
  color: var(--text-color-secondary);
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

.word-set-stats {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 1rem;
}

.stat {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.stat i {
  color: var(--primary-color);
}

.card-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: space-between;
}

@media (max-width: 768px) {
  .word-sets-container {
    padding: 1rem;
  }

  .title {
    font-size: 2rem;
  }

  .word-sets-grid {
    grid-template-columns: 1fr;
  }

  .card-actions {
    flex-direction: column;
  }

  .card-actions button {
    width: 100%;
  }
}
</style>
