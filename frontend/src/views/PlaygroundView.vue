<script setup lang="ts">
import {ref} from 'vue'
import StatCard from '@/components/ui/StatCard.vue'
import TaskCard from '@/components/ui/TaskCard.vue'
import ContentCard from '@/components/ui/ContentCard.vue'
import CourseCard from '@/components/ui/CourseCard.vue'
import FilterBar from '@/components/ui/FilterBar.vue'

const filterBarData = ref({
  search: '',
  level: 'all',
  status: 'all',
})
const levelOptions = [
  {label: 'All Levels', value: 'all'},
  {label: 'A1', value: 'a1'},
  {label: 'A2', value: 'a2'},
]
const statusOptions = [
  {label: 'All Status', value: 'all'},
  {label: 'Not Started', value: 'not-started'},
  {label: 'Completed', value: 'completed'},
]
</script>

<template>
  <div class="playground-view">
    <div class="page-header">
      <h1>Component Playground</h1>
      <p class="text-secondary">A space to test and review components in isolation.</p>
    </div>

    <div class="content-area-lg">
      <!-- UI Components -->
      <div class="component-section">
        <h2 class="section-header">UI Components</h2>

        <div class="component-demo">
          <h3>1. StatCard</h3>
          <div class="demo-area stats-grid">
            <StatCard icon="pi pi-bolt" label="Study Streak" value="7 days" variant="warning"/>
            <StatCard icon="pi pi-book" label="Words Learned" value="142" variant="purple"/>
            <StatCard icon="pi pi-clock" label="Time This Week" value="3h 25m" variant="blue"/>
          </div>
        </div>

        <div class="component-demo">
          <h3>2. TaskCard</h3>
          <div class="demo-area task-list">
            <TaskCard icon="pi pi-book" title="Review 15 words" meta="15 words • 5 minutes" :completed="false"/>
            <TaskCard icon="pi pi-pencil" title="Complete daily exercises" meta="3 exercises • 10 minutes"
                      :completed="true"/>
          </div>
        </div>

        <div class="component-demo">
          <h3>3. ContentCard</h3>
          <div class="demo-area content-grid">
            <ContentCard type="dialogue" icon="pi pi-megaphone" title="At the Café" level="A1"
                         description="Practice ordering in French."/>
            <ContentCard type="story" icon="pi pi-book-open" title="La Petite Maison" level="A2" :completed="true"
                         description="A short story about a family."/>
            <ContentCard type="grammar" icon="pi pi-file-edit" title="Present Tense" level="B1"
                         description="Learn regular verb conjugations."/>
          </div>
        </div>

        <div class="component-demo">
          <h3>4. CourseCard</h3>
          <div class="demo-area content-grid">
            <CourseCard name="French for Beginners"
                        description="Start your French journey with basic vocabulary and grammar." language-code="FR"
                        cefr-level="A1" :total-modules="8" :total-episodes="32"/>
            <CourseCard name="Conversational Spanish"
                        description="Improve your speaking skills with real-life conversations." language-code="ES"
                        cefr-level="B1" :total-modules="12" :total-episodes="48"/>
          </div>
        </div>

        <div class="component-demo">
          <h3>5. FilterBar</h3>
          <div class="demo-area-column">
            <FilterBar
                search-placeholder="Search components..."
                :filters="[
                { label: 'Level', options: levelOptions, modelValue: filterBarData.level },
                { label: 'Status', options: statusOptions, modelValue: filterBarData.status }
              ]"
                :results-count="42"
                @update:search="val => filterBarData.search = val"
                @update:filter="(idx, val) => idx === 0 ? filterBarData.level = val : filterBarData.status = val"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.playground-view {
  padding: var(--spacing-2xl);
}

.component-section {
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
}

.component-demo {
  margin-bottom: var(--spacing-2xl);
  padding: var(--spacing-xl);
  border-radius: var(--radius-md);
  background-color: var(--surface-section);
}

.component-demo h3 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: var(--spacing-md);
}

.demo-area {
  padding: var(--spacing-lg);
}

.demo-area-column {
  padding: var(--spacing-lg);
}
</style>
