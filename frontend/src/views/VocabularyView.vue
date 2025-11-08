<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useVocabularyStore } from '@/stores/vocabulary'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import ConfirmDialog from 'primevue/confirmdialog'
import { useConfirm } from 'primevue/useconfirm'

const vocabularyStore = useVocabularyStore()
const router = useRouter()
const confirm = useConfirm()

const loading = ref(true)

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
</script>

<template>
  <div class="vocabulary-container">
    <ConfirmDialog />

    <div class="vocabulary-content">
      <div class="header">
        <h1 class="vocabulary-title">
          <i class="pi pi-book"></i>
          My Vocabulary
        </h1>
        <div class="header-actions">
          <span class="word-count">{{ vocabularyStore.wordCount }} words</span>
          <Button
            label="Add Words"
            icon="pi pi-plus"
            @click="goToSearch"
          />
        </div>
      </div>

      <Message v-if="vocabularyStore.error" severity="error" :closable="false" class="error-message">
        {{ vocabularyStore.error }}
      </Message>

      <Card v-if="!loading && vocabularyStore.words.length === 0" class="empty-state">
        <template #content>
          <div class="empty-content">
            <i class="pi pi-book empty-icon"></i>
            <h3>Your vocabulary is empty</h3>
            <p>Start building your vocabulary by searching for words and adding them to your collection.</p>
            <Button
              label="Search for Words"
              icon="pi pi-search"
              @click="goToSearch"
              class="search-button"
            />
          </div>
        </template>
      </Card>

      <Card v-else class="vocabulary-card">
        <template #content>
          <DataTable
            :value="vocabularyStore.words"
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
                <div class="word-cell">
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
                <span v-else class="text-muted">N/A</span>
              </template>
            </Column>

            <Column field="notes" header="Notes">
              <template #body="slotProps">
                <span v-if="slotProps.data.notes" class="notes-text">
                  {{ slotProps.data.notes }}
                </span>
                <span v-else class="text-muted">No notes</span>
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
    </div>
  </div>
</template>

<style scoped>
.vocabulary-container {
  min-height: 100vh;
  background: var(--surface-ground);
  padding: 2rem 1rem;
}

.vocabulary-content {
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.vocabulary-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--text-color);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.vocabulary-title i {
  color: var(--primary-color);
  font-size: 2rem;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.word-count {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-color-secondary);
  padding: 0.5rem 1rem;
  background: var(--surface-100);
  border-radius: 6px;
}

.error-message {
  margin-bottom: 1.5rem;
}

.vocabulary-card {
  margin-top: 1rem;
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
  margin-bottom: 2rem;
  font-size: 1rem;
}

.search-button {
  margin-top: 1rem;
}

.word-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.lemma {
  font-weight: 600;
  font-size: 1.125rem;
  color: var(--text-color);
}

.pos-tag {
  font-size: 0.75rem;
  text-transform: uppercase;
}

.notes-text {
  color: var(--text-color);
  font-style: italic;
}

.text-muted {
  color: var(--text-color-secondary);
}

@media (max-width: 768px) {
  .vocabulary-container {
    padding: 1rem;
  }

  .vocabulary-title {
    font-size: 2rem;
  }

  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .word-count {
    font-size: 1rem;
  }
}
</style>
