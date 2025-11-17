<script setup lang="ts">
import {onMounted, ref} from 'vue'
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
  <div class="page-container-with-padding">
    <ConfirmDialog />

    <div class="view-container content-area-lg">
      <div class="flex justify-between items-center mb-2xl flex-wrap gap-md">
        <h1 class="flex items-center gap-md text-primary" style="font-size: 2.5rem; font-weight: 700; margin: 0;">
          <i class="pi pi-book text-3xl icon-primary"></i>
          My Vocabulary
        </h1>
        <div class="flex items-center gap-md">
          <span class="word-count text-lg text-secondary"
          >{{ vocabularyStore.wordCount }} words</span
          >
          <Button label="Add Words" icon="pi pi-plus" @click="goToSearch"/>
        </div>
      </div>

      <Message
          v-if="vocabularyStore.error"
          severity="error"
          :closable="false"
          class="error-message"
      >
        {{ vocabularyStore.error }}
      </Message>

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
                <div class="flex items-center gap-sm">
                  <span class="lemma text-lg text-primary">{{ slotProps.data.word.lemma }}</span>
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
    </div>
  </div>
</template>

<style scoped>
/* Custom word count badge */
.word-count {
  font-weight: 600;
  padding: 0.5rem 1rem;
  background: var(--bg-hover);
  border-radius: var(--radius-sm);
}

/* Table customization */
.pos-tag {
  font-size: 0.75rem;
  text-transform: uppercase;
}

.notes-text {
  font-style: italic;
}
</style>
