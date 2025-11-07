<script setup lang="ts">
import Card from 'primevue/card'
import Button from 'primevue/button'
</script>

<template>
  <div class="about-container">
    <div class="about-content">
      <h1 class="about-title">About Vocabee</h1>

      <Card class="about-card">
        <template #content>
          <h2>What is Vocabee?</h2>
          <p>
            Vocabee is a modern vocabulary exploration tool that makes it easy to search and
            discover words across multiple languages. Built on top of Wiktionary data, it
            provides a fast, intuitive interface for language learners, translators, and
            anyone interested in linguistics.
          </p>
        </template>
      </Card>

      <Card class="about-card">
        <template #content>
          <h2>Key Features</h2>
          <ul class="features-list">
            <li>
              <i class="pi pi-check-circle"></i>
              <strong>Multi-language support:</strong> Search across 12+ languages including
              French, Spanish, German, Italian, Portuguese, Russian, and more
            </li>
            <li>
              <i class="pi pi-check-circle"></i>
              <strong>Comprehensive data:</strong> Access detailed definitions, examples,
              etymology, pronunciation guides, and inflected forms
            </li>
            <li>
              <i class="pi pi-check-circle"></i>
              <strong>Smart ranking:</strong> Advanced two-stage search algorithm prioritizes
              exact matches and common words
            </li>
            <li>
              <i class="pi pi-check-circle"></i>
              <strong>Frequency data:</strong> Word frequency rankings help you focus on the
              most commonly used vocabulary
            </li>
            <li>
              <i class="pi pi-check-circle"></i>
              <strong>Fast & responsive:</strong> Built with modern web technologies for
              instant results
            </li>
          </ul>
        </template>
      </Card>

      <Card class="about-card">
        <template #content>
          <h2>Technology Stack</h2>
          <div class="tech-grid">
            <div class="tech-item">
              <h3>Frontend</h3>
              <ul>
                <li>Vue 3 + TypeScript</li>
                <li>PrimeVue UI Components</li>
                <li>Vite Build Tool</li>
              </ul>
            </div>
            <div class="tech-item">
              <h3>Backend</h3>
              <ul>
                <li>Kotlin + Spring Boot</li>
                <li>PostgreSQL Database</li>
                <li>RESTful API</li>
              </ul>
            </div>
            <div class="tech-item">
              <h3>Data</h3>
              <ul>
                <li>Wiktionary XML Dump</li>
                <li>Custom Python Parser</li>
                <li>4.2M+ Entries</li>
              </ul>
            </div>
          </div>
        </template>
      </Card>

      <Card class="about-card">
        <template #content>
          <h2>Data Source</h2>
          <p>
            All vocabulary data is sourced from
            <a href="https://www.wiktionary.org/" target="_blank" rel="noopener">Wiktionary</a>,
            the free multilingual dictionary. Wiktionary content is available under the
            <a href="https://creativecommons.org/licenses/by-sa/3.0/" target="_blank" rel="noopener">
              Creative Commons Attribution-ShareAlike License</a>.
          </p>
          <p class="data-note">
            <i class="pi pi-info-circle"></i>
            Data is processed and indexed from English Wiktionary XML dumps, with ongoing
            updates to maintain accuracy and coverage.
          </p>
        </template>
      </Card>

      <div class="cta-section">
        <h2>Ready to explore?</h2>
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
.about-container {
  min-height: 100%;
  padding: 3rem 1.5rem;
  background: var(--surface-ground);
}

.about-content {
  max-width: 900px;
  margin: 0 auto;
}

.about-title {
  font-size: 2.5rem;
  margin-bottom: 2.5rem;
  text-align: center;
  color: var(--text-color);
  font-weight: 700;
}

.about-card {
  margin-bottom: 1.5rem;
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
}

.about-card h2 {
  font-size: 1.5rem;
  margin-bottom: 1rem;
  color: var(--primary-color);
  font-weight: 600;
}

.about-card p {
  line-height: 1.8;
  color: var(--text-color-secondary);
  margin-bottom: 1rem;
}

.about-card p:last-child {
  margin-bottom: 0;
}

.about-card a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 600;
}

.about-card a:hover {
  text-decoration: underline;
}

.features-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.features-list li {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 1rem;
  line-height: 1.7;
  color: var(--text-color-secondary);
}

.features-list li:last-child {
  margin-bottom: 0;
}

.features-list li i {
  color: var(--green-500);
  margin-top: 0.25rem;
  flex-shrink: 0;
}

.features-list li strong {
  color: var(--text-color);
}

.tech-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 2rem;
  margin-top: 1rem;
}

.tech-item h3 {
  font-size: 1.1rem;
  margin-bottom: 0.75rem;
  color: var(--primary-color);
  font-weight: 600;
}

.tech-item ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tech-item li {
  padding: 0.4rem 0;
  color: var(--text-color-secondary);
  line-height: 1.6;
}

.data-note {
  background: var(--highlight-bg);
  border-left: 4px solid var(--primary-color);
  padding: 1rem;
  border-radius: var(--border-radius);
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  margin-top: 1rem;
}

.data-note i {
  color: var(--primary-color);
  margin-top: 0.25rem;
  flex-shrink: 0;
}

.cta-section {
  text-align: center;
  padding: 3rem 2rem;
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-radius: var(--border-radius);
  margin-top: 2rem;
}

.cta-section h2 {
  font-size: 1.75rem;
  margin-bottom: 1.5rem;
  color: var(--text-color);
  font-weight: 600;
}

@media (max-width: 768px) {
  .about-container {
    padding: 2rem 1rem;
  }

  .about-title {
    font-size: 2rem;
  }

  .about-card h2 {
    font-size: 1.25rem;
  }

  .tech-grid {
    grid-template-columns: 1fr;
  }

  .cta-section {
    padding: 2rem 1rem;
  }
}
</style>
