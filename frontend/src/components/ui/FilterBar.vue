<script setup lang="ts">
import {ref, watch} from 'vue'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

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

const props = withDefaults(defineProps<FilterBarProps>(), {
  searchPlaceholder: 'Search...',
  filters: () => [],
  showCount: true
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

<script lang="ts">
import {defineProps, withDefaults, defineEmits} from 'vue'
</script>

<template>
  <div class="filter-bar card card-padding">
    <!-- Search Box -->
    <div class="search-box">
      <span class="search-icon">🔍</span>
      <InputText
          v-model="searchQuery"
          :placeholder="searchPlaceholder"
          class="search-input"
      />
    </div>

    <!-- Filter Dropdowns -->
    <Select
        v-for="(filter, index) in filters"
        :key="index"
        :modelValue="filter.modelValue"
        :options="filter.options"
        optionLabel="label"
        optionValue="value"
        :placeholder="filter.label"
        class="filter-dropdown"
        @update:modelValue="(value) => handleFilterChange(index, value)"
    />

    <!-- Results Count -->
    <span v-if="showCount && resultsCount !== undefined" class="results-count">
      {{ resultsCount }} {{ resultsCount === 1 ? 'result' : 'results' }}
    </span>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
  margin-bottom: var(--spacing-3xl);
}

.search-box {
  flex: 1;
  min-width: 200px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg-tertiary);
  padding: 8px 14px;
  border-radius: var(--radius-md);
}

.search-icon {
  color: var(--text-tertiary);
  font-size: 16px;
}

.search-input {
  flex: 1;
  border: none;
  background: none;
  font-size: 14px;
  outline: none;
  color: var(--text-primary);
  padding: 0;
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

.filter-dropdown {
  min-width: 140px;
}

.results-count {
  font-size: 14px;
  color: var(--text-tertiary);
  white-space: nowrap;
  margin-left: auto;
  padding: 0 var(--spacing-xs);
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    width: 100%;
  }

  .filter-dropdown {
    width: 100%;
  }

  .results-count {
    margin-left: 0;
    text-align: center;
  }
}
</style>
