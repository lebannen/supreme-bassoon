<template>
  <div class="page-container-with-padding">
    <Toast />

    <div class="view-container content-area-xl">
      <div class="page-header">
        <h1 class="flex items-center gap-md">
          <i class="pi pi-bookmark text-3xl icon-primary"></i>
          Word Sets
        </h1>
        <p>Pre-made collections of words to quickly build your vocabulary</p>
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
            Upload a JSON file with word sets. The file should include "language" and optionally
            "level" fields.
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
          <Button label="Cancel" text @click="showUploadDialog = false"/>
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
                No word sets available for
                {{ languages.find((l) => l.code === selectedLanguage)?.name }}
              </p>
              <p v-else>Select a language to view available word sets</p>
            </div>
          </template>
        </Card>
      </div>

      <div v-else class="content-grid">
        <Card v-for="wordSet in wordSets" :key="wordSet.id" class="word-set-card">
          <template #header>
            <div class="card-header">
              <div class="card-header-content">
                <Tag
                    v-if="wordSet.level"
                    :value="wordSet.level"
                    severity="info"
                    class="level-tag"
                />
                <Tag
                    v-if="wordSet.isImported"
                    value="Imported"
                    severity="success"
                    icon="pi pi-check"
                />
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
                <span
                >{{ wordSet.userVocabularyCount }}/{{ wordSet.wordCount }} in your
                  vocabulary</span
                >
              </div>
            </div>
          </template>
          <template #footer>
            <div class="card-actions">
              <Button
                label="View Details"
                icon="pi pi-eye"
                text
                @click="viewWordSetDetails()"
              />
              <Button
                v-if="authStore.isAuthenticated"
                :label="wordSet.isImported ? 'Re-import' : 'Import'"
                :icon="wordSet.isImported ? 'pi pi-refresh' : 'pi pi-download'"
                @click="confirmImportWordSet(wordSet)"
                :loading="importingSetId === wordSet.id"
                :disabled="importingSetId !== null && importingSetId !== wordSet.id"
              />
              <Button v-else label="Login to Import" icon="pi pi-sign-in" @click="goToLogin"/>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import Toast from 'primevue/toast'
import {useToast} from 'primevue/usetoast'
import {useConfirm} from 'primevue/useconfirm'
import ProgressSpinner from 'primevue/progressspinner'
import Dialog from 'primevue/dialog'
import FileUpload from 'primevue/fileupload'
import {storeToRefs} from 'pinia'
import {useAuthStore} from '@/stores/auth'
import {useVocabularyStore} from '@/stores/vocabulary'
import {useWordSetStore} from '@/stores/wordSet'
import type {WordSet} from '@/types/wordSet'

const router = useRouter()
const toast = useToast()
const confirm = useConfirm()
const authStore = useAuthStore()
const vocabularyStore = useVocabularyStore()
const wordSetStore = useWordSetStore()
const {wordSets, loading, error} = storeToRefs(wordSetStore)

const languages = [
  { code: 'fr', name: 'French' },
  { code: 'de', name: 'German' },
  { code: 'es', name: 'Spanish' },
  { code: 'it', name: 'Italian' },
  { code: 'pt', name: 'Portuguese' },
]

const selectedLanguage = ref<string>('')
const importingSetId = ref<number | null>(null)

// Upload dialog state
const showUploadDialog = ref(false)
const uploadFile = ref<File | null>(null)
const uploadError = ref<string | null>(null)
const isUploading = ref(false)

async function loadWordSets() {
  if (!selectedLanguage.value) {
    return
  }

  await wordSetStore.loadWordSetsByLanguage(selectedLanguage.value)
}

function viewWordSetDetails() {
  // TODO: Implement word set detail view
  toast.add({
    severity: 'info',
    summary: 'Coming Soon',
    detail: 'Word set detail view is coming soon!',
    life: 3000,
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
    },
  })
}

async function importWordSetToVocabulary(id: number) {
  importingSetId.value = id

  try {
    const result = await wordSetStore.importWordSet(id, {
      wordSetId: id,
      addNotes: true,
    })

    if (result) {
      toast.add({
        severity: 'success',
        summary: 'Word Set Imported',
        detail: result.message,
        life: 5000,
      })

      // Refresh vocabulary and word sets
      await Promise.all([vocabularyStore.fetchVocabulary(), loadWordSets()])
    } else {
      throw new Error('Failed to import word set')
    }
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Import Failed',
      detail: err instanceof Error ? err.message : 'Failed to import word set',
      life: 3000,
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
    const result = await wordSetStore.loadWordSetsFromJson(uploadFile.value)

    if (result) {
      toast.add({
        severity: 'success',
        summary: 'Word Sets Loaded',
        detail: `${result.count} word sets created successfully`,
        life: 5000,
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
/* Controls section */
.controls-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--spacing-2xl);
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.language-filter label {
  display: block;
  margin-bottom: var(--spacing-xs);
  font-weight: 600;
  color: var(--text-primary);
}

.language-select {
  max-width: 300px;
}

/* Upload dialog */
.upload-dialog {
  width: 500px;
  max-width: 90vw;
}

.upload-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.upload-form .field {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.upload-form .field label {
  font-weight: 600;
  color: var(--text-primary);
}

.file-name {
  display: block;
  margin-top: var(--spacing-xs);
  color: var(--text-secondary);
  font-style: italic;
}

/* Card customization */
.word-set-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  padding: var(--spacing-md);
  background: var(--bg-tertiary);
  border-bottom: 1px solid var(--border-medium);
}

.card-header-content {
  display: flex;
  gap: var(--spacing-xs);
  align-items: center;
  flex-wrap: wrap;
}

.word-set-name {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
}

.theme {
  color: var(--text-secondary);
  font-style: italic;
}

.description {
  color: var(--text-secondary);
  margin-bottom: var(--spacing-md);
}

.word-set-stats {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-md);
}

.word-set-stats .stat {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  color: var(--text-secondary);
}

.word-set-stats .stat i {
  color: var(--primary);
}

.card-actions {
  display: flex;
  gap: var(--spacing-xs);
  justify-content: space-between;
}

/* Responsive */
@media (max-width: 768px) {
  .card-actions {
    flex-direction: column;
  }

  .card-actions button {
    width: 100%;
  }
}
</style>
