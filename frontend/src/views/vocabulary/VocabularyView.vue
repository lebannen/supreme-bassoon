<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useVocabularyStore} from '@/stores/vocabulary'
import {useRouter} from 'vue-router'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import ConfirmDialog from 'primevue/confirmdialog'
import {useConfirm} from 'primevue/useconfirm'
import FilterBar from '@/components/ui/FilterBar.vue'

const vocabularyStore = useVocabularyStore()
const router = useRouter()
const confirm = useConfirm()

const loading = ref(true)
const searchQuery = ref('')
const selectedSort = ref('recent')

const sortOptions = [
  {label: 'Sort: Recent', value: 'recent'},
  {label: 'Sort: Alphabetical', value: 'alphabetical'},
]

const filteredWords = computed(() => {
  let words = [...vocabularyStore.words]
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    words = words.filter(w => w.word.lemma.toLowerCase().includes(query) || w.notes?.toLowerCase().includes(query))
  }
  if (selectedSort.value === 'alphabetical') {
    words.sort((a, b) => a.word.lemma.localeCompare(b.word.lemma))
  } else {
    words.sort((a, b) => new Date(b.addedAt).getTime() - new Date(a.addedAt).getTime())
  }
  return words
})

onMounted(async () => {
  await vocabularyStore.fetchVocabulary()
  loading.value = false
})

const handleRemoveWord = (wordId: number, lemma: string) => {
  confirm.require({
    message: `Are you sure you want to remove "${lemma}" from your vocabulary?`,
    header: 'Confirm Removal',
    icon: 'pi pi-exclamation-triangle',
    accept: () => vocabularyStore.removeWord(wordId),
  })
}

const formatDate = (dateString: string) => new Date(dateString).toLocaleDateString('en-US', {
  year: 'numeric',
  month: 'short',
  day: 'numeric'
})
</script>

<template>
  <div class="view-container content-area-lg">
    <ConfirmDialog/>
    <div class="page-header">
      <div>
        <h1>My Vocabulary</h1>
        <p class="text-secondary">{{ vocabularyStore.wordCount }} words saved</p>
      </div>
      <Button label="Add Words" icon="pi pi-plus" @click="router.push('/search')" size="large"/>
    </div>

    <Message v-if="vocabularyStore.error" severity="error">{{ vocabularyStore.error }}</Message>

    <template v-if="!loading && vocabularyStore.words.length > 0">
      <div class="section-header"><h2>Study Sessions</h2></div>
      <div class="study-sessions">
        <Card class="session-card due-reviews-card">
          <template #content>
            <div class="session-content">
              <div class="session-header-content"><span class="session-icon">⏰</span><span class="session-title">Due Reviews</span>
              </div>
              <p class="session-description">Review words ready for practice.</p>
              <Button label="Start Review" icon="pi pi-play" @click="router.push('/study')" class="w-full"/>
            </div>
          </template>
        </Card>
        <Card class="session-card">
          <template #content>
            <div class="session-content">
              <div class="session-header-content"><span class="session-icon">⚡</span><span class="session-title">Quick Review</span>
              </div>
              <p class="session-description">Practice 10 random words.</p>
              <Button label="Start Review" icon="pi pi-play" @click="router.push('/study')" class="w-full"
                      severity="secondary"/>
            </div>
          </template>
        </Card>
        <Card class="session-card">
          <template #content>
            <div class="session-content">
              <div class="session-header-content"><span class="session-icon">📚</span><span class="session-title">All Words</span>
              </div>
              <p class="session-description">Comprehensive review of all words.</p>
              <Button label="Start Review" icon="pi pi-play" @click="router.push('/study')" class="w-full"
                      severity="secondary"/>
            </div>
          </template>
        </Card>
      </div>

      <div class="section-header"><h2>Saved Words</h2></div>
      <FilterBar search-placeholder="Search vocabulary..."
                 :filters="[{ label: 'Sort', options: sortOptions, modelValue: selectedSort }]"
                 @update:search="searchQuery = $event" @update:filter="(idx, val) => selectedSort = val"/>
      <Card>
        <template #content>
          <DataTable :value="filteredWords" :loading="loading" stripedRows paginator :rows="20"
                     :rows-per-page-options="[10, 20, 50]" responsiveLayout="scroll">
            <Column field="word.lemma" header="Word" sortable>
              <template #body="{data}"><span class="font-semibold">{{ data.word.lemma }}</span>
                <Tag v-if="data.word.partOfSpeech" :value="data.word.partOfSpeech" class="ml-sm"/>
              </template>
            </Column>
            <Column field="notes" header="Notes">
              <template #body="{data}"><span class="italic text-secondary">{{ data.notes || '-' }}</span></template>
            </Column>
            <Column field="addedAt" header="Added" sortable>
              <template #body="{data}">{{ formatDate(data.addedAt) }}</template>
            </Column>
            <Column header="Actions">
              <template #body="{data}">
                <Button icon="pi pi-trash" severity="danger" text rounded
                        @click="handleRemoveWord(data.word.id, data.word.lemma)"/>
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>
    </template>

    <Card v-if="!loading && vocabularyStore.words.length === 0">
      <template #content>
        <div class="empty-state">
          <i class="pi pi-book empty-icon"></i>
          <h3>Your vocabulary is empty</h3>
          <p class="text-secondary">Start by searching for words and adding them to your collection.</p>
          <Button label="Search for Words" icon="pi pi-search" @click="router.push('/search')"/>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.study-sessions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.session-card {
  transition: all 0.2s;
}

.session-card:hover {
  transform: translateY(-2px);
  cursor: pointer;
}

.session-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.session-header-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.session-icon {
  font-size: 1.5rem;
}

.session-title {
  font-size: 1.25rem;
  font-weight: 600;
}

.session-description {
  font-size: 0.9rem;
  color: var(--text-color-secondary);
  margin: 0;
}

/* Due Reviews card - highlighted styling */
.due-reviews-card {
  border: 2px solid var(--p-primary-color);
  background: var(--p-primary-50);
}

/* Dark mode override */
:root.p-dark .due-reviews-card,
.dark-theme .due-reviews-card {
  background: color-mix(in srgb, var(--p-primary-color) 15%, var(--p-surface-900));
}

.due-reviews-card .session-description {
  color: var(--p-primary-700);
}

:root.p-dark .due-reviews-card .session-description,
.dark-theme .due-reviews-card .session-description {
  color: var(--p-primary-300);
}
</style>
