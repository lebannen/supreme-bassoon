<script setup lang="ts">
import Card from 'primevue/card'
import Button from 'primevue/button'
</script>

<template>
  <div class="about-view">
    <div class="about-container">
      <h1 class="text-4xl font-bold text-center text-primary mb-3xl">About Vocabee</h1>

      <Card class="mb-lg">
        <template #content>
          <h2 class="text-2xl font-semibold text-primary mb-md">What is Vocabee?</h2>
          <p class="leading-loose text-secondary card-text">
            Vocabee is a modern vocabulary exploration tool that makes it easy to search and
            discover words across multiple languages. Built on top of Wiktionary data, it provides a
            fast, intuitive interface for language learners, translators, and anyone interested in
            linguistics.
          </p>
        </template>
      </Card>

      <Card class="mb-lg">
        <template #content>
          <h2 class="text-2xl font-semibold text-primary mb-md">Key Features</h2>
          <ul class="checklist">
            <li>
              <i class="pi pi-check-circle"></i>
              <span class="text-secondary leading-relaxed">
                <strong class="text-primary">Multi-language support:</strong> Search across 12+
                languages including French, Spanish, German, Italian, Portuguese, Russian, and more
              </span>
            </li>
            <li>
              <i class="pi pi-check-circle"></i>
              <span class="text-secondary leading-relaxed">
                <strong class="text-primary">Comprehensive data:</strong> Access detailed
                definitions, examples, etymology, pronunciation guides, and inflected forms
              </span>
            </li>
            <li>
              <i class="pi pi-check-circle"></i>
              <span class="text-secondary leading-relaxed">
                <strong class="text-primary">Smart ranking:</strong> Advanced two-stage search
                algorithm prioritizes exact matches and common words
              </span>
            </li>
            <li>
              <i class="pi pi-check-circle"></i>
              <span class="text-secondary leading-relaxed">
                <strong class="text-primary">Frequency data:</strong> Word frequency rankings help
                you focus on the most commonly used vocabulary
              </span>
            </li>
            <li>
              <i class="pi pi-check-circle"></i>
              <span class="text-secondary leading-relaxed">
                <strong class="text-primary">Fast & responsive:</strong> Built with modern web
                technologies for instant results
              </span>
            </li>
          </ul>
        </template>
      </Card>

      <Card class="mb-lg">
        <template #content>
          <h2 class="text-2xl font-semibold text-primary mb-md">Technology Stack</h2>
          <div class="tech-grid">
            <div>
              <h3 class="text-lg font-semibold text-primary mb-sm">Frontend</h3>
              <ul class="tech-list">
                <li>Vue 3 + TypeScript</li>
                <li>PrimeVue UI Components</li>
                <li>Vite Build Tool</li>
              </ul>
            </div>
            <div>
              <h3 class="text-lg font-semibold text-primary mb-sm">Backend</h3>
              <ul class="tech-list">
                <li>Kotlin + Spring Boot</li>
                <li>PostgreSQL Database</li>
                <li>RESTful API</li>
              </ul>
            </div>
            <div>
              <h3 class="text-lg font-semibold text-primary mb-sm">Data</h3>
              <ul class="tech-list">
                <li>Wiktionary XML Dump</li>
                <li>Custom Python Parser</li>
                <li>4.2M+ Entries</li>
              </ul>
            </div>
          </div>
        </template>
      </Card>

      <Card class="mb-lg">
        <template #content>
          <h2 class="text-2xl font-semibold text-primary mb-md">Data Source</h2>
          <p class="leading-loose text-secondary mb-md">
            All vocabulary data is sourced from
            <a href="https://www.wiktionary.org/" target="_blank" rel="noopener" class="link"
            >Wiktionary</a
            >, the free multilingual dictionary. Wiktionary content is available under the
            <a
                href="https://creativecommons.org/licenses/by-sa/3.0/"
                target="_blank"
                rel="noopener"
                class="link"
            >
              Creative Commons Attribution-ShareAlike License</a
            >.
          </p>
          <div class="info-note">
            <i class="pi pi-info-circle"></i>
            <p class="text-secondary leading-relaxed info-note-text">
              Data is processed and indexed from English Wiktionary XML dumps, with ongoing updates
              to maintain accuracy and coverage.
            </p>
          </div>
        </template>
      </Card>

      <div class="cta-card">
        <h2 class="text-3xl font-semibold text-primary mb-lg">Ready to explore?</h2>
        <Button
          label="Start Searching Words"
          icon="pi pi-search"
          size="large"
          @click="$router.push('/search')"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.about-view {
  min-height: 100%;
  background: var(--bg-primary);
  padding: var(--spacing-4xl);
}

.about-container {
  max-width: 900px;
  margin: 0 auto;
}

.link {
  color: var(--primary);
  text-decoration: none;
  font-weight: 600;
}

.link:hover {
  text-decoration: underline;
}

.tech-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--spacing-xl);
  margin-top: var(--spacing-md);
}

.tech-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tech-list li {
  padding: 0.4rem 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.info-note {
  background: var(--bg-tertiary);
  border-left: 4px solid var(--primary);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
}

.info-note i {
  color: var(--primary);
  margin-top: 0.25rem;
  flex-shrink: 0;
}

.info-note-text {
  margin: 0;
}

.card-text {
  margin-bottom: 0;
}

.cta-card {
  text-align: center;
  padding: var(--spacing-4xl);
  background: var(--bg-secondary);
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
}

@media (max-width: 768px) {
  .tech-grid {
    grid-template-columns: 1fr;
  }
}
</style>
