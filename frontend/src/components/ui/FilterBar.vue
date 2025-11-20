<script setup lang="ts">
import {ref, watch} from 'vue'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'

export interface FilterOption {
  label: string
  value: string
}

export interface FilterBarProps {
  /**
   * Placeholder for search input
   */
  searchPlaceholder?: string

  /**
   * Options for filter dropdowns (array of dropdown configs)
   */
  filters?: {
    label: string
    options: FilterOption[]
    modelValue: string
  }[]

  /**
   * Show results count
   */
  resultsCount?: number

  /**
   * Show count label
   */
  showCount?: boolean
}

withDefaults(defineProps<FilterBarProps>(), {
  searchPlaceholder: 'Search...',
  filters: () => [],
  showCount: true,
})

const emit = defineEmits<{
  'update:search': [value: string]
  'update:filter': [filterIndex: number, value: string]
}>()

const searchQuery = ref('')

watch(searchQuery, (newValue) => {
  emit('update:search', newValue)
})

const handleFilterChange = (index: number, value: string) => {
  emit('update:filter', index, value)
}
</script>

<template>
  <div class="filters">
    <!-- Search Box -->
    <IconField iconPosition="left" class="flex-1">
      <InputIcon class="pi pi-search"/>
      <InputText
          v-model="searchQuery"
          :placeholder="searchPlaceholder"
          class="w-full"
      />
    </IconField>

    <!-- Filter Dropdowns -->
    <Select
        v-for="(filter, index) in filters"
        :key="index"
        :modelValue="filter.modelValue"
        :options="filter.options"
        optionLabel="label"
        optionValue="value"
        :placeholder="filter.label"
        class="filter-select"
        @update:modelValue="(value) => handleFilterChange(index, value)"
    />

    <!-- Results Count -->
    <span v-if="showCount && resultsCount !== undefined" class="text-sm text-secondary ml-auto">
      {{ resultsCount }} {{ resultsCount === 1 ? 'result' : 'results' }}
    </span>
  </div>
</template>
