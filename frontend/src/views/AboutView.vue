<script setup lang="ts">
import Card from 'primevue/card'
import Button from 'primevue/button'
</script>

<template>
  <div class="about-view">
    <div class="detail-container content-area-lg">
      <h1 class="text-4xl font-bold text-center text-primary">About Vocabee</h1>

      <Card>
        <template #title>What is Vocabee?</template>
        <template #content>
          <p class="m-0 leading-loose text-secondary">
            Vocabee is a modern vocabulary exploration tool that makes it easy to search and
            discover words across multiple languages. Built on top of Wiktionary data, it provides a
            fast, intuitive interface for language learners, translators, and anyone interested in
            linguistics.
          </p>
        </template>
      </Card>

      <Card>
        <template #title>Key Features</template>
        <template #content>
          <ul class="checklist">
            <li>
              <i class="pi pi-check-circle text-primary"></i>
              <span class="text-secondary">
                <strong>Multi-language support:</strong> Search across 12+ languages.
              </span>
            </li>
            <li>
              <i class="pi pi-check-circle text-primary"></i>
              <span class="text-secondary">
                <strong>Comprehensive data:</strong> Access definitions, examples, etymology, and more.
              </span>
            </li>
            <li>
              <i class="pi pi-check-circle text-primary"></i>
              <span class="text-secondary">
                <strong>Smart ranking:</strong> Prioritizes exact matches and common words.
              </span>
            </li>
            <li>
              <i class="pi pi-check-circle text-primary"></i>
              <span class="text-secondary">
                <strong>Frequency data:</strong> Focus on the most commonly used vocabulary.
              </span>
            </li>
            <li>
              <i class="pi pi-check-circle text-primary"></i>
              <span class="text-secondary">
                <strong>Fast & responsive:</strong> Built with modern web technologies.
              </span>
            </li>
          </ul>
        </template>
      </Card>

      <Card>
        <template #title>Technology Stack</template>
        <template #content>
          <div class="grid md:grid-cols-3 gap-xl">
            <div>
              <h3 class="text-lg font-semibold text-primary mb-sm">Frontend</h3>
              <ul class="list-none p-0 m-0">
                <li class="py-1">Vue 3 + TypeScript</li>
                <li class="py-1">PrimeVue UI Components</li>
                <li class="py-1">Vite Build Tool</li>
              </ul>
            </div>
            <div>
              <h3 class="text-lg font-semibold text-primary mb-sm">Backend</h3>
              <ul class="list-none p-0 m-0">
                <li class="py-1">Kotlin + Spring Boot</li>
                <li class="py-1">PostgreSQL Database</li>
                <li class="py-1">RESTful API</li>
              </ul>
            </div>
            <div>
              <h3 class="text-lg font-semibold text-primary mb-sm">Data</h3>
              <ul class="list-none p-0 m-0">
                <li class="py-1">Wiktionary XML Dump</li>
                <li class="py-1">Custom Python Parser</li>
                <li class="py-1">4.2M+ Entries</li>
              </ul>
            </div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>Data Source</template>
        <template #content>
          <p class="m-0 leading-loose text-secondary">
            All vocabulary data is sourced from
            <a href="https://www.wiktionary.org/" target="_blank" rel="noopener"
               class="text-primary font-semibold hover:underline">Wiktionary</a>,
            the free multilingual dictionary, available under the
            <a href="https://creativecommons.org/licenses/by-sa/3.0/" target="_blank" rel="noopener"
               class="text-primary font-semibold hover:underline">Creative Commons Attribution-ShareAlike License</a>.
          </p>
        </template>
      </Card>

      <div class="text-center p-4xl bg-surface-section rounded-lg">
        <h2 class="text-3xl font-semibold text-primary mb-lg">Ready to explore?</h2>
        <Button label="Start Searching Words" icon="pi pi-search" size="large" @click="$router.push('/search')"/>
      </div>
    </div>
  </div>
</template>

<style scoped>
.about-view {
  padding: var(--spacing-4xl) 0;
}
</style>
