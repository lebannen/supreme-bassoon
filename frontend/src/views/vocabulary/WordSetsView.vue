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
import {type GenerationResult, VocabularyCardService} from '@/services/VocabularyCardService'

const router = useRouter()
const toast = useToast()
const confirm = useConfirm()
const authStore = useAuthStore()
const vocabularyStore = useVocabularyStore()
const wordSetStore = useWordSetStore()
const {wordSets, loading, error} = storeToRefs(wordSetStore)

// Vocabulary card generation
const generatingCardSetId = ref<number | null>(null)
const showGenerationResultDialog = ref(false)
const generationResult = ref<GenerationResult | null>(null)

const languages = [
  {code: 'fr', name: 'French'}, {code: 'de', name: 'German'}, {code: 'es', name: 'Spanish'},
  {code: 'it', name: 'Italian'}, {code: 'pt', name: 'Portuguese'},
]
const selectedLanguage = ref('fr')
const importingSetId = ref<number | null>(null)
const showUploadDialog = ref(false)
const uploadFile = ref<File | null>(null)
const uploadError = ref<string | null>(null)
const isUploading = ref(false)

const loadWordSets = () => wordSetStore.loadWordSetsByLanguage(selectedLanguage.value)

function confirmImportWordSet(wordSet: WordSet) {
  const newWords = wordSet.wordCount - wordSet.userVocabularyCount
  confirm.require({
    message: wordSet.isImported ? `Re-import "${wordSet.name}"? This will add ${newWords} new words.` : `Import "${wordSet.name}" with ${wordSet.wordCount} words?`,
    header: 'Confirm Import',
    icon: 'pi pi-download',
    accept: () => importWordSetToVocabulary(wordSet.id),
  })
}

async function importWordSetToVocabulary(id: number) {
  importingSetId.value = id
  try {
    const result = await wordSetStore.importWordSet(id, {wordSetId: id, addNotes: true})
    if (result) {
      toast.add({severity: 'success', summary: 'Word Set Imported', detail: result.message, life: 5000})
      await Promise.all([vocabularyStore.fetchVocabulary(), loadWordSets()])
    } else throw new Error('Failed to import word set')
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Import Failed',
      detail: err instanceof Error ? err.message : 'An unknown error occurred',
      life: 3000
    })
  } finally {
    importingSetId.value = null
  }
}

async function uploadWordSets() {
  if (!uploadFile.value) return
  isUploading.value = true
  uploadError.value = null
  try {
    const result = await wordSetStore.loadWordSetsFromJson(uploadFile.value)
    if (result) {
      toast.add({severity: 'success', summary: 'Word Sets Loaded', detail: `${result.count} sets created.`, life: 5000})
      showUploadDialog.value = false
      uploadFile.value = null
      await loadWordSets()
    }
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : 'Failed to load word sets'
  } finally {
    isUploading.value = false
  }
}

async function generateVocabularyCards(wordSet: WordSet) {
  generatingCardSetId.value = wordSet.id
  try {
    const result = await VocabularyCardService.generateForWordSet(wordSet.id, wordSet.level || undefined)
    generationResult.value = result
    showGenerationResultDialog.value = true
    toast.add({
      severity: result.failed > 0 ? 'warn' : 'success',
      summary: 'Generation Complete',
      detail: `Generated ${result.generated} cards, skipped ${result.skipped}, failed ${result.failed}`,
      life: 5000
    })
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Generation Failed',
      detail: err instanceof Error ? err.message : 'Failed to generate vocabulary cards',
      life: 5000
    })
  } finally {
    generatingCardSetId.value = null
  }
}

onMounted(loadWordSets)
</script>

<template>
  <Toast/>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <h1 class="flex align-items-center gap-3"><i class="pi pi-bookmark text-3xl icon-primary"></i> Word Sets</h1>
      <p class="text-secondary">Pre-made collections of words to build your vocabulary.</p>
    </div>

    <div class="flex justify-content-between items-end flex-wrap gap-3">
      <div class="flex flex-column gap-2">
        <label for="language-select" class="font-semibold">Filter by Language</label>
        <Select id="language-select" v-model="selectedLanguage" :options="languages" optionLabel="name"
                optionValue="code" @change="loadWordSets" class="w-full md:w-64"/>
      </div>
      <Button label="Load from JSON" icon="pi pi-upload" severity="secondary" @click="showUploadDialog = true"/>
    </div>

    <Dialog v-model:visible="showUploadDialog" header="Load Word Sets from JSON" :modal="true" class="w-full max-w-lg">
      <div class="content-area">
        <Message severity="info">Upload a JSON file with word sets, including "language" and "level" fields.</Message>
        <FileUpload mode="basic" accept=".json" :auto="false" @select="uploadFile = $event.files[0]"
                    chooseLabel="Choose File"/>
        <small v-if="uploadFile" class="text-secondary">{{ uploadFile.name }}</small>
        <Message v-if="uploadError" severity="error">{{ uploadError }}</Message>
      </div>
      <template #footer>
        <Button label="Cancel" text @click="showUploadDialog = false"/>
        <Button label="Load" icon="pi pi-upload" @click="uploadWordSets" :disabled="!uploadFile || isUploading"
                :loading="isUploading"/>
      </template>
    </Dialog>

    <Message v-if="error" severity="error">{{ error }}</Message>
    <div v-if="loading" class="loading-state">
      <ProgressSpinner/>
    </div>
    <div v-else-if="wordSets.length === 0" class="empty-state">
      <i class="pi pi-inbox empty-icon"></i>
      <h3>No word sets found</h3>
      <p class="text-secondary">No sets available for {{ languages.find(l => l.code === selectedLanguage)?.name }}.</p>
    </div>

    <div v-else class="content-grid">
      <Card v-for="wordSet in wordSets" :key="wordSet.id" class="flex flex-column">
        <template #header>
          <div class="p-md flex justify-end align-items-center gap-2 bg-surface-section">
            <Tag v-if="wordSet.level" :value="wordSet.level"/>
            <Tag v-if="wordSet.isImported" value="Imported" icon="pi pi-check" severity="success"/>
          </div>
        </template>
        <template #title>{{ wordSet.name }}</template>
        <template #subtitle v-if="wordSet.theme">{{ wordSet.theme }}</template>
        <template #content>
          <p v-if="wordSet.description" class="text-secondary m-0">{{ wordSet.description }}</p>
        </template>
        <template #footer>
          <div class="mt-auto pt-md border-t border-surface">
            <div class="icon-label-group vertical compact mb-3">
              <span class="icon-label"><i class="pi pi-book"></i>{{ wordSet.wordCount }} words</span>
              <span v-if="authStore.isAuthenticated && wordSet.userVocabularyCount > 0" class="icon-label"><i
                  class="pi pi-check-circle text-success"></i>{{
                  wordSet.userVocabularyCount
                }} in your vocabulary</span>
            </div>
            <div class="flex flex-column gap-2">
              <Button v-if="authStore.isAuthenticated" :label="wordSet.isImported ? 'Re-import' : 'Import'"
                      :icon="wordSet.isImported ? 'pi pi-refresh' : 'pi pi-download'"
                      @click="confirmImportWordSet(wordSet)" :loading="importingSetId === wordSet.id"
                      :disabled="importingSetId !== null && importingSetId !== wordSet.id"/>
              <Button v-else label="Login to Import" icon="pi pi-sign-in" @click="router.push('/login')" outlined/>
              <Button v-if="authStore.isAuthenticated" label="Generate Cards" icon="pi pi-sparkles"
                      severity="secondary" outlined
                      @click="generateVocabularyCards(wordSet)" :loading="generatingCardSetId === wordSet.id"
                      :disabled="generatingCardSetId !== null && generatingCardSetId !== wordSet.id"/>
            </div>
          </div>
        </template>
      </Card>
    </div>

    <!-- Generation Result Dialog -->
    <Dialog v-model:visible="showGenerationResultDialog" header="Vocabulary Card Generation Results" :modal="true"
            class="w-full max-w-lg">
      <div v-if="generationResult" class="content-area">
        <div class="flex flex-column gap-3">
          <div class="flex justify-content-between align-items-center p-3 bg-surface-ground border-round">
            <span class="font-semibold">Total Words</span>
            <span>{{ generationResult.totalWords }}</span>
          </div>
          <div class="flex justify-content-between align-items-center p-3 bg-green-50 border-round">
            <span class="font-semibold text-green-700">Generated</span>
            <Tag :value="generationResult.generated.toString()" severity="success"/>
          </div>
          <div class="flex justify-content-between align-items-center p-3 bg-blue-50 border-round">
            <span class="font-semibold text-blue-700">Skipped (already exist)</span>
            <Tag :value="generationResult.skipped.toString()" severity="info"/>
          </div>
          <div v-if="generationResult.failed > 0"
               class="flex justify-content-between align-items-center p-3 bg-red-50 border-round">
            <span class="font-semibold text-red-700">Failed</span>
            <Tag :value="generationResult.failed.toString()" severity="danger"/>
          </div>
          <div v-if="generationResult.errors.length > 0" class="mt-3">
            <h4 class="font-semibold mb-2">Errors:</h4>
            <ul class="text-sm text-secondary pl-4">
              <li v-for="(err, index) in generationResult.errors" :key="index">{{ err }}</li>
            </ul>
          </div>
        </div>
      </div>
      <template #footer>
        <Button label="View Cards" icon="pi pi-external-link"
                @click="router.push('/admin/vocabulary-cards'); showGenerationResultDialog = false"/>
        <Button label="Close" severity="secondary" @click="showGenerationResultDialog = false"/>
      </template>
    </Dialog>
  </div>
</template>
