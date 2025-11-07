<script setup lang="ts">
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'

const router = useRouter()

const features = [
  {
    icon: 'pi pi-search',
    title: 'Advanced Search',
    description: 'Search through millions of words with intelligent ranking and instant results'
  },
  {
    icon: 'pi pi-book',
    title: 'Rich Definitions',
    description: 'Detailed definitions, examples, etymology, and usage notes from Wiktionary'
  },
  {
    icon: 'pi pi-language',
    title: 'Multiple Languages',
    description: 'Support for 12+ languages including French, Spanish, German, and more'
  },
  {
    icon: 'pi pi-chart-line',
    title: 'Frequency Data',
    description: 'Word frequency rankings to help you learn the most common words first'
  }
]
</script>

<template>
  <div class="home-container">
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">
          <i class="pi pi-book"></i>
          Vocabee
        </h1>
        <p class="hero-subtitle">
          Your multilingual vocabulary companion powered by Wiktionary
        </p>
        <p class="hero-description">
          Explore millions of words across multiple languages with detailed definitions,
          examples, and linguistic insights
        </p>
        <div class="hero-actions">
          <Button
            label="Start Searching"
            icon="pi pi-search"
            size="large"
            @click="router.push('/search')"
          />
          <Button
            label="Learn More"
            icon="pi pi-info-circle"
            size="large"
            severity="secondary"
            outlined
            @click="router.push('/about')"
          />
        </div>
      </div>
    </section>

    <section class="features-section">
      <div class="features-container">
        <h2 class="features-title">Features</h2>
        <div class="features-grid">
          <Card v-for="feature in features" :key="feature.title" class="feature-card">
            <template #content>
              <div class="feature-icon">
                <i :class="feature.icon"></i>
              </div>
              <h3 class="feature-title">{{ feature.title }}</h3>
              <p class="feature-description">{{ feature.description }}</p>
            </template>
          </Card>
        </div>
      </div>
    </section>

    <section class="stats-section">
      <div class="stats-container">
        <div class="stat-item">
          <div class="stat-value">4.2M+</div>
          <div class="stat-label">Word Entries</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">12+</div>
          <div class="stat-label">Languages</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">Instant</div>
          <div class="stat-label">Search Results</div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-container {
  min-height: 100%;
}

.hero-section {
  background: var(--surface-section);
  padding: 5rem 1.5rem;
  text-align: center;
  border-bottom: 1px solid var(--surface-border);
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 800;
  margin-bottom: 1.5rem;
  color: var(--text-color);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
}

.hero-title i {
  font-size: 3rem;
  color: var(--primary-color);
}

.hero-subtitle {
  font-size: 1.5rem;
  color: var(--text-color);
  margin-bottom: 1rem;
  font-weight: 600;
}

.hero-description {
  font-size: 1.1rem;
  color: var(--text-color-secondary);
  margin-bottom: 2.5rem;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.features-section {
  padding: 5rem 1.5rem;
  background: var(--surface-ground);
}

.features-container {
  max-width: 1200px;
  margin: 0 auto;
}

.features-title {
  text-align: center;
  font-size: 2.5rem;
  margin-bottom: 3rem;
  color: var(--text-color);
  font-weight: 700;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
}

.feature-card {
  text-align: center;
  transition: transform 0.2s, box-shadow 0.2s;
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  font-size: 3rem;
  color: var(--primary-color);
  margin-bottom: 1rem;
}

.feature-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
  color: var(--text-color);
}

.feature-description {
  color: var(--text-color-secondary);
  line-height: 1.6;
}

.stats-section {
  background: var(--primary-color);
  color: white;
  padding: 4rem 1.5rem;
  border-top: 1px solid var(--surface-border);
}

.stats-container {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 3rem;
  text-align: center;
}

.stat-value {
  font-size: 3rem;
  font-weight: 800;
  margin-bottom: 0.5rem;
}

.stat-label {
  font-size: 1.1rem;
  opacity: 0.95;
}

@media (max-width: 768px) {
  .hero-section {
    padding: 3rem 1rem;
  }

  .hero-title {
    font-size: 2.5rem;
    flex-direction: column;
    gap: 0.5rem;
  }

  .hero-subtitle {
    font-size: 1.25rem;
  }

  .features-section {
    padding: 3rem 1rem;
  }

  .features-title {
    font-size: 2rem;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .stats-section {
    padding: 3rem 1rem;
  }

  .stat-value {
    font-size: 2rem;
  }
}
</style>
