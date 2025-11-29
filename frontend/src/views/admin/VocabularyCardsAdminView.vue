<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import Message from 'primevue/message'
import Toast from 'primevue/toast'
import {useToast} from 'primevue/usetoast'
import {useConfirm} from 'primevue/useconfirm'
import ConfirmDialog from 'primevue/confirmdialog'
import ProgressSpinner from 'primevue/progressspinner'
import {type VocabularyCard, VocabularyCardService} from '@/services/VocabularyCardService'

const toast = useToast()
const confirm = useConfirm()

const languages = [
  {code: 'fr', name: 'French'},
  {code: 'de', name: 'German'},
  {code: 'es', name: 'Spanish'},
  {code: 'it', name: 'Italian'},
  {code: 'pt', name: 'Portuguese'},
]

const statusOptions = [
  {value: null, label: 'All'},
  {value: 'PENDING', label: 'Pending'},
  {value: 'APPROVED', label: 'Approved'},
  {value: 'REJECTED', label: 'Rejected'},
]

const selectedLanguage = ref('fr')
const selectedStatus = ref<string | null>(null)
const searchQuery = ref('')
const cards = ref<VocabularyCard[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const selectedCard = ref<VocabularyCard | null>(null)
const showCardDialog = ref(false)

const stats = ref<{ total: number } | null>(null)

async function loadCards() {
  loading.value = true
  error.value = null
  try {
    if (searchQuery.value.trim()) {
      cards.value = await VocabularyCardService.searchCards(selectedLanguage.value, searchQuery.value.trim())
    } else if (selectedStatus.value) {
      cards.value = await VocabularyCardService.getCardsByLanguage(selectedLanguage.value, selectedStatus.value)
    } else {
      cards.value = await VocabularyCardService.getCardsByLanguage(selectedLanguage.value)
    }
    // Also load stats
    stats.value = await VocabularyCardService.getStats({languageCode: selectedLanguage.value})
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load vocabulary cards'
  } finally {
    loading.value = false
  }
}

function viewCard(card: VocabularyCard) {
  selectedCard.value = card
  showCardDialog.value = true
}

async function updateStatus(card: VocabularyCard, status: 'PENDING' | 'APPROVED' | 'REJECTED') {
  try {
    const updated = await VocabularyCardService.updateCardStatus(card.id, status)
    // Update in list
    const index = cards.value.findIndex(c => c.id === card.id)
    if (index !== -1) {
      cards.value[index] = updated
    }
    // Update selected card if viewing
    if (selectedCard.value?.id === card.id) {
      selectedCard.value = updated
    }
    toast.add({
      severity: 'success',
      summary: 'Status Updated',
      detail: `Card marked as ${status.toLowerCase()}`,
      life: 3000
    })
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Update Failed',
      detail: err instanceof Error ? err.message : 'Failed to update status',
      life: 3000
    })
  }
}

function confirmDelete(card: VocabularyCard) {
  confirm.require({
    message: `Delete vocabulary card for "${card.lemma}"? This action cannot be undone.`,
    header: 'Confirm Deletion',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteCard(card),
  })
}

async function deleteCard(card: VocabularyCard) {
  try {
    await VocabularyCardService.deleteCard(card.id)
    cards.value = cards.value.filter(c => c.id !== card.id)
    if (selectedCard.value?.id === card.id) {
      showCardDialog.value = false
      selectedCard.value = null
    }
    toast.add({
      severity: 'success',
      summary: 'Card Deleted',
      detail: `Vocabulary card for "${card.lemma}" deleted`,
      life: 3000
    })
  } catch (err) {
    toast.add({
      severity: 'error',
      summary: 'Delete Failed',
      detail: err instanceof Error ? err.message : 'Failed to delete card',
      life: 3000
    })
  }
}

function getStatusSeverity(status: string): "success" | "info" | "warn" | "danger" | "secondary" | "contrast" | undefined {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'PENDING':
    default:
      return 'warn'
  }
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString()
}

const translationLanguages = computed(() => {
  if (!selectedCard.value?.translations) return []
  return Object.keys(selectedCard.value.translations)
})

onMounted(loadCards)
</script>

<template>
  <Toast/>
  <ConfirmDialog/>
  <div class="view-container content-area-lg">
    <div class="page-header">
      <h1 class="flex align-items-center gap-3">
        <i class="pi pi-id-card text-3xl icon-primary"></i>
        Vocabulary Cards
      </h1>
      <p class="text-secondary">AI-generated vocabulary cards with translations and grammar information.</p>
    </div>

    <!-- Filters -->
    <Card class="mb-4">
      <template #content>
        <div class="flex flex-wrap gap-3 align-items-end">
          <div class="flex flex-column gap-2">
            <label for="language-select" class="font-semibold">Language</label>
            <Select id="language-select" v-model="selectedLanguage" :options="languages" optionLabel="name"
                    optionValue="code" @change="loadCards" class="w-48"/>
          </div>
          <div class="flex flex-column gap-2">
            <label for="status-select" class="font-semibold">Status</label>
            <Select id="status-select" v-model="selectedStatus" :options="statusOptions" optionLabel="label"
                    optionValue="value" @change="loadCards" class="w-40"/>
          </div>
          <div class="flex flex-column gap-2 flex-grow-1">
            <label for="search-input" class="font-semibold">Search</label>
            <div class="p-inputgroup">
              <InputText id="search-input" v-model="searchQuery" placeholder="Search by lemma..."
                         @keyup.enter="loadCards"/>
              <Button icon="pi pi-search" @click="loadCards"/>
            </div>
          </div>
          <div v-if="stats" class="flex align-items-center gap-2 ml-auto">
            <Tag :value="`${stats.total} cards`" severity="info"/>
          </div>
        </div>
      </template>
    </Card>

    <!-- Error Message -->
    <Message v-if="error" severity="error" @close="error = null">{{ error }}</Message>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <ProgressSpinner/>
    </div>

    <!-- Cards Table -->
    <Card v-else>
      <template #content>
        <DataTable :value="cards" stripedRows responsiveLayout="scroll" :paginator="true" :rows="20"
                   :rowsPerPageOptions="[10, 20, 50, 100]" sortField="createdAt" :sortOrder="-1">
          <template #empty>
            <div class="text-center p-5">
              <i class="pi pi-inbox text-4xl text-secondary mb-3"></i>
              <p class="text-secondary">No vocabulary cards found.</p>
            </div>
          </template>

          <Column field="lemma" header="Lemma" sortable>
            <template #body="{data}">
              <span class="font-semibold">{{ data.lemma }}</span>
            </template>
          </Column>

          <Column field="partOfSpeech" header="POS" sortable>
            <template #body="{data}">
              <Tag v-if="data.partOfSpeech" :value="data.partOfSpeech" severity="secondary"/>
              <span v-else class="text-secondary">-</span>
            </template>
          </Column>

          <Column field="ipa" header="IPA">
            <template #body="{data}">
              <span v-if="data.ipa" class="font-mono text-sm">{{ data.ipa }}</span>
              <span v-else class="text-secondary">-</span>
            </template>
          </Column>

          <Column header="Translations">
            <template #body="{data}">
              <span v-if="data.translations?.en" class="text-sm">
                {{ data.translations.en.slice(0, 2).join(', ') }}
                <span v-if="data.translations.en.length > 2">...</span>
              </span>
              <span v-else class="text-secondary">-</span>
            </template>
          </Column>

          <Column field="cefrLevel" header="Level" sortable>
            <template #body="{data}">
              <Tag v-if="data.cefrLevel" :value="data.cefrLevel"/>
            </template>
          </Column>

          <Column field="reviewStatus" header="Status" sortable>
            <template #body="{data}">
              <Tag :value="data.reviewStatus" :severity="getStatusSeverity(data.reviewStatus)"/>
            </template>
          </Column>

          <Column field="createdAt" header="Created" sortable>
            <template #body="{data}">
              <span class="text-sm">{{ formatDate(data.createdAt) }}</span>
            </template>
          </Column>

          <Column header="Actions" style="width: 12rem">
            <template #body="{data}">
              <Button icon="pi pi-eye" rounded text severity="primary" v-tooltip.top="'View Details'"
                      @click="viewCard(data)" class="mr-1"/>
              <Button icon="pi pi-check" rounded text severity="success" v-tooltip.top="'Approve'"
                      @click="updateStatus(data, 'APPROVED')" :disabled="data.reviewStatus === 'APPROVED'"
                      class="mr-1"/>
              <Button icon="pi pi-times" rounded text severity="danger" v-tooltip.top="'Reject'"
                      @click="updateStatus(data, 'REJECTED')" :disabled="data.reviewStatus === 'REJECTED'"
                      class="mr-1"/>
              <Button icon="pi pi-trash" rounded text severity="danger" v-tooltip.top="'Delete'"
                      @click="confirmDelete(data)"/>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Card Detail Dialog -->
    <Dialog v-model:visible="showCardDialog" :header="selectedCard?.lemma || 'Card Details'" :modal="true"
            class="w-full" style="max-width: 800px">
      <div v-if="selectedCard" class="content-area">
        <!-- Header Info -->
        <div class="flex flex-wrap gap-2 mb-4">
          <Tag v-if="selectedCard.partOfSpeech" :value="selectedCard.partOfSpeech"/>
          <Tag v-if="selectedCard.cefrLevel" :value="selectedCard.cefrLevel" severity="info"/>
          <Tag v-if="selectedCard.gender" :value="selectedCard.gender" severity="secondary"/>
          <Tag :value="selectedCard.reviewStatus" :severity="getStatusSeverity(selectedCard.reviewStatus)"/>
        </div>

        <!-- Pronunciation -->
        <div v-if="selectedCard.ipa" class="mb-4">
          <h4 class="font-semibold mb-2">Pronunciation</h4>
          <span class="font-mono text-lg">{{ selectedCard.ipa }}</span>
        </div>

        <!-- Definitions -->
        <div v-if="selectedCard.definitions?.length" class="mb-4">
          <h4 class="font-semibold mb-2">Definitions</h4>
          <ol class="pl-4 m-0">
            <li v-for="(def, i) in selectedCard.definitions" :key="i" class="mb-1">{{ def }}</li>
          </ol>
        </div>

        <!-- Translations -->
        <div v-if="selectedCard.translations" class="mb-4">
          <h4 class="font-semibold mb-2">Translations</h4>
          <div class="flex flex-column gap-2">
            <div v-for="lang in translationLanguages" :key="lang" class="flex gap-2">
              <Tag :value="lang.toUpperCase()" severity="secondary" class="w-12"/>
              <span>{{ selectedCard.translations[lang]?.join(', ') }}</span>
            </div>
          </div>
        </div>

        <!-- Examples -->
        <div v-if="selectedCard.examples?.length" class="mb-4">
          <h4 class="font-semibold mb-2">Examples</h4>
          <div v-for="(ex, i) in selectedCard.examples" :key="i" class="mb-3 p-3 bg-surface-ground border-round">
            <p class="font-semibold m-0">{{ ex.sentence }}</p>
            <p class="text-secondary m-0 mt-1">{{ ex.translation }}</p>
          </div>
        </div>

        <!-- Grammar -->
        <div v-if="selectedCard.pluralForm || selectedCard.verbGroup || selectedCard.grammarNotes" class="mb-4">
          <h4 class="font-semibold mb-2">Grammar</h4>
          <div class="flex flex-column gap-1">
            <div v-if="selectedCard.pluralForm"><strong>Plural:</strong> {{ selectedCard.pluralForm }}</div>
            <div v-if="selectedCard.verbGroup"><strong>Verb Group:</strong> {{ selectedCard.verbGroup }}</div>
            <div v-if="selectedCard.grammarNotes"><strong>Notes:</strong> {{ selectedCard.grammarNotes }}</div>
          </div>
        </div>

        <!-- Inflections -->
        <div v-if="selectedCard.inflections" class="mb-4">
          <h4 class="font-semibold mb-2">Inflections</h4>
          <div class="flex flex-column gap-1">
            <div v-for="(value, key) in selectedCard.inflections" :key="key">
              <strong>{{ key }}:</strong>
              <span v-if="Array.isArray(value)">{{ value.join(', ') }}</span>
              <span v-else>{{ value }}</span>
            </div>
          </div>
        </div>

        <!-- Tags -->
        <div v-if="selectedCard.tags?.length" class="mb-4">
          <h4 class="font-semibold mb-2">Tags</h4>
          <div class="flex flex-wrap gap-2">
            <Tag v-for="tag in selectedCard.tags" :key="tag" :value="tag" severity="secondary"/>
          </div>
        </div>

        <!-- Metadata -->
        <div class="text-sm text-secondary border-t border-surface pt-3 mt-4">
          <div class="flex flex-wrap gap-4">
            <span v-if="selectedCard.frequencyRank"><strong>Frequency Rank:</strong> {{
                selectedCard.frequencyRank
              }}</span>
            <span v-if="selectedCard.modelUsed"><strong>Model:</strong> {{ selectedCard.modelUsed }}</span>
            <span><strong>Generated:</strong> {{ formatDate(selectedCard.generatedAt) }}</span>
            <span v-if="selectedCard.reviewedAt"><strong>Reviewed:</strong> {{
                formatDate(selectedCard.reviewedAt)
              }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-content-between w-full">
          <div class="flex gap-2">
            <Button label="Approve" icon="pi pi-check" severity="success"
                    @click="updateStatus(selectedCard!, 'APPROVED')"
                    :disabled="selectedCard?.reviewStatus === 'APPROVED'"/>
            <Button label="Reject" icon="pi pi-times" severity="danger"
                    @click="updateStatus(selectedCard!, 'REJECTED')"
                    :disabled="selectedCard?.reviewStatus === 'REJECTED'"/>
          </div>
          <Button label="Close" severity="secondary" @click="showCardDialog = false"/>
        </div>
      </template>
    </Dialog>
  </div>
</template>
