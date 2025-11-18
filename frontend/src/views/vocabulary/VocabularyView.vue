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

// Filter state
const searchQuery = ref('')
const selectedSource = ref('all')
const selectedStatus = ref('all')
const selectedSort = ref('recent')

// Filter options
const sourceOptions = [
  {label: 'All Sources', value: 'all'},
  {label: 'Dialogues', value: 'dialogues'},
  {label: 'Stories', value: 'stories'},
  {label: 'Exercises', value: 'exercises'},
  {label: 'Manual', value: 'manual'},
]

const statusOptions = [
  {label: 'All Status', value: 'all'},
  {label: 'Due for Review', value: 'due'},
  {label: 'Learning', value: 'learning'},
  {label: 'Mastered', value: 'mastered'},
]

const sortOptions = [
  {label: 'Sort: Recent', value: 'recent'},
  {label: 'Sort: Alphabetical', value: 'alphabetical'},
  {label: 'Sort: Next Review', value: 'next_review'},
]

// Filtered words
const filteredWords = computed(() => {
  let words = [...vocabularyStore.words]

  // Search filter
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    words = words.filter(
        (w) =>
            w.word.lemma.toLowerCase().includes(query) ||
            w.notes?.toLowerCase().includes(query)
    )
  }

  // Source filter (if we add source tracking in the future)
  // Status filter (if we add SRS status tracking in the future)

  // Sort
  if (selectedSort.value === 'alphabetical') {
    words.sort((a, b) => a.word.lemma.localeCompare(b.word.lemma))
  } else if (selectedSort.value === 'recent') {
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
    rejectLabel: 'Cancel',
    acceptLabel: 'Remove',
    accept: async () => {
      await vocabularyStore.removeWord(wordId)
    },
  })
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

const goToSearch = () => {
  router.push('/search')
}

const handleSearchUpdate = (value: string) => {
  searchQuery.value = value
}

const handleFilterUpdate = (filterIndex: number, value: string) => {
  if (filterIndex === 0) {
    selectedSource.value = value
  } else if (filterIndex === 1) {
    selectedStatus.value = value
  } else if (filterIndex === 2) {
    selectedSort.value = value
  }
}

// Study session actions
const handleDueReview = () => {
  router.push('/study')
}

const handleQuickReview = () => {
  router.push('/study')
}

const handleAllWordsReview = () => {
  router.push('/study')
}
</script>

<template>
  <div class="page-container-with-padding">
    <ConfirmDialog />

    <div class="view-container content-area-lg">
      <!-- Page Header -->
      <div class="page-header">
        <div>
          <h1>My Vocabulary</h1>
          <p class="header-subtitle">
            {{ vocabularyStore.wordCount }} words saved
            <template v-if="vocabularyStore.wordCount > 0">
              • 12 due for review today
            </template>
          </p>
        </div>
        <Button label="Add Words" icon="pi pi-plus" @click="goToSearch" size="large"/>
      </div>

      <Message
          v-if="vocabularyStore.error"
          severity="error"
          :closable="false"
          class="mb-lg"
      >
        {{ vocabularyStore.error }}
      </Message>

      <!-- Study Sessions Section -->
      <template v-if="!loading && vocabularyStore.words.length > 0">
        <div class="section-header">
          <h2>Study Sessions</h2>
        </div>

        <div class="study-sessions">
          <!-- Due Reviews -->
          <Card class="session-card session-card-due">
            <template #content>
              <div class="session-content">
                <div class="session-header-content">
                  <span class="session-icon">⏰</span>
                  <span class="session-title">Due Reviews</span>
                </div>
                <p class="session-description">Review words that are ready for practice</p>
                <div class="session-meta">
                  <span class="session-count">12 words</span>
                  <span class="session-separator">•</span>
                  <span class="session-time">~4 min</span>
                </div>
                <Button
                    label="Start Review"
                    icon="pi pi-play"
                    @click="handleDueReview"
                    class="w-full"
                    severity="primary"
                />
              </div>
            </template>
          </Card>

          <!-- Quick Review -->
          <Card class="session-card">
            <template #content>
              <div class="session-content">
                <div class="session-header-content">
                  <span class="session-icon">⚡</span>
                  <span class="session-title">Quick Review</span>
                </div>
                <p class="session-description">Practice 5 random words</p>
                <div class="session-meta">
                  <span class="session-count">5 words</span>
                  <span class="session-separator">•</span>
                  <span class="session-time">~2 min</span>
                </div>
                <Button
                    label="Start Review"
                    icon="pi pi-play"
                    @click="handleQuickReview"
                    class="w-full"
                />
              </div>
            </template>
          </Card>

          <!-- All Words -->
          <Card class="session-card">
            <template #content>
              <div class="session-content">
                <div class="session-header-content">
                  <span class="session-icon">📚</span>
                  <span class="session-title">All Words</span>
                </div>
                <p class="session-description">Comprehensive review of all saved words</p>
                <div class="session-meta">
                  <span class="session-count">{{ vocabularyStore.wordCount }} words</span>
                  <span class="session-separator">•</span>
                  <span class="session-time">~{{ Math.ceil(vocabularyStore.wordCount / 3) }} min</span>
                </div>
                <Button
                    label="Start Review"
                    icon="pi pi-play"
                    @click="handleAllWordsReview"
                    class="w-full"
                />
              </div>
            </template>
          </Card>
        </div>

        <!-- Saved Words Section -->
        <div class="section-header">
          <h2>Saved Words</h2>
        </div>

        <!-- Filter Bar -->
        <FilterBar
            searchPlaceholder="Search vocabulary..."
            :filters="[
            { label: 'Source', options: sourceOptions, modelValue: selectedSource },
            { label: 'Status', options: statusOptions, modelValue: selectedStatus },
            { label: 'Sort', options: sortOptions, modelValue: selectedSort },
          ]"
            :resultsCount="filteredWords.length"
            :showCount="false"
            @update:search="handleSearchUpdate"
            @update:filter="handleFilterUpdate"
        />

        <!-- Vocabulary Table -->
        <Card class="vocabulary-card">
          <template #content>
            <DataTable
                :value="filteredWords"
                :loading="vocabularyStore.loading || loading"
                striped-rows
                paginator
                :rows="20"
                :rows-per-page-options="[10, 20, 50]"
                responsive-layout="scroll"
                class="vocabulary-table"
            >
              <Column field="word.lemma" header="Word" :sortable="true">
                <template #body="slotProps">
                  <div class="flex items-center gap-sm">
                    <span class="lemma">{{ slotProps.data.word.lemma }}</span>
                    <Tag
                        v-if="slotProps.data.word.partOfSpeech"
                        :value="slotProps.data.word.partOfSpeech"
                        severity="info"
                        class="pos-tag"
                    />
                  </div>
                </template>
              </Column>

              <Column field="word.frequencyRank" header="Frequency" :sortable="true">
                <template #body="slotProps">
                  <span v-if="slotProps.data.word.frequencyRank">
                    #{{ slotProps.data.word.frequencyRank }}
                  </span>
                  <span v-else class="text-secondary">N/A</span>
                </template>
              </Column>

              <Column field="notes" header="Notes">
                <template #body="slotProps">
                  <span v-if="slotProps.data.notes" class="notes-text">
                    {{ slotProps.data.notes }}
                  </span>
                  <span v-else class="text-secondary">No notes</span>
                </template>
              </Column>

              <Column field="addedAt" header="Added" :sortable="true">
                <template #body="slotProps">
                  {{ formatDate(slotProps.data.addedAt) }}
                </template>
              </Column>

              <Column header="Actions">
                <template #body="slotProps">
                  <Button
                      icon="pi pi-trash"
                      severity="danger"
                      text
                      rounded
                      @click="handleRemoveWord(slotProps.data.word.id, slotProps.data.word.lemma)"
                      :aria-label="`Remove ${slotProps.data.word.lemma}`"
                  />
                </template>
              </Column>
            </DataTable>
          </template>
        </Card>
      </template>

      <!-- Empty State -->
      <Card v-if="!loading && vocabularyStore.words.length === 0">
        <template #content>
          <div class="empty-state">
            <i class="pi pi-book empty-icon"></i>
            <h3>Your vocabulary is empty</h3>
            <p>
              Start building your vocabulary by searching for words and adding them to your
              collection.
            </p>
            <Button label="Search for Words" icon="pi pi-search" @click="goToSearch"/>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
/* Header subtitle */
.header-subtitle {
  margin-top: var(--spacing-xs);
}

/* Study Sessions Grid */
.study-sessions {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-3xl);
}

.session-card {
  transition: all 0.2s;
}

.session-card:hover {
  transform: translateY(-2px);
  cursor: pointer;
}

.session-card-due {
  border: 2px solid var(--p-primary-color);
  background: var(--p-primary-50);
}

.session-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.session-header-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.session-icon {
  font-size: 24px;
}

.session-title {
  font-size: 18px;
  font-weight: 600;
}

.session-description {
  font-size: 14px;
  margin: 0;
  line-height: 1.5;
}

.session-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 13px;
  padding-bottom: var(--spacing-sm);
}

.session-count {
  font-weight: 600;
}

.session-separator {
  opacity: 0.5;
}

.session-time {
  opacity: 0.7;
}

/* Table customization */
.lemma {
  font-size: 15px;
  font-weight: 600;
}

.pos-tag {
  font-size: 0.75rem;
  text-transform: uppercase;
}

.notes-text {
  font-style: italic;
}

@media (max-width: 768px) {
  .study-sessions {
    grid-template-columns: 1fr;
  }
}
</style>
