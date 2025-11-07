<template>
  <div class="book-view-container">
    <h1>Book Reader Demo</h1>

    <div class="view-controls">
      <div class="demo-controls">
        <button @click="demoMode = 'pages'" :class="{ active: demoMode === 'pages' }">Pages Prop</button>
        <button @click="demoMode = 'content'" :class="{ active: demoMode === 'content' }">Content Prop</button>
        <button @click="demoMode = 'slots'" :class="{ active: demoMode === 'slots' }">Slots</button>
      </div>
      <div class="language-selector-wrapper">
        <label for="language-select">Book Language</label>
        <Select
          id="language-select"
          v-model="selectedLanguage"
          :options="languages"
          optionLabel="name"
          optionValue="code"
          placeholder="Select a language"
          class="w-full"
          :loading="isLoadingLanguages"
        />
      </div>
    </div>

    <BookComponent v-if="demoMode === 'pages'" :pages="bookPages" @word-click="onWordClick" />
    <BookComponent v-if="demoMode === 'content'" :content="longText" :pageSize="300" @word-click="onWordClick" />
    <BookComponent v-if="demoMode === 'slots'">
      <template #left-page="{ page }">
        <div style="padding: 20px; text-align: center;">
          <h2>Page Personnalisée (Gauche)</h2>
          <p>Ceci est la page {{ page }}.</p>
          <p>Vous pouvez mettre n'importe quel contenu de composant Vue ici.</p>
        </div>
      </template>
      <template #right-page="{ page }">
        <div style="padding: 20px; text-align: center;">
          <h2>Page Personnalisée (Droite)</h2>
          <p>Ceci est la page {{ page }}.</p>
          <p>N'est-ce pas cool ?</p>
        </div>
      </template>
    </BookComponent>

    <WordCard
      v-model:visible="showWordDialog"
      :word="selectedWord"
      :loading="isLoadingWord"
      :error="wordError"
      @word-click="onWordClick"
    />

    <div class="demo-section">
      <h2>How to Use</h2>
      <pre><code>&lt;BookComponent :pages="bookPages" @word-click="handleWordClick" /&gt;</code></pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import BookComponent from '../components/BookComponent.vue';
import WordCard from '../components/WordCard.vue';
import Select from 'primevue/select';
import { useVocabularyApi, type Language, type Word } from '../composables/useVocabularyApi';

const { getLanguages, getWord } = useVocabularyApi();

const demoMode = ref('pages');

const languages = ref<Language[]>([]);
const selectedLanguage = ref<string>('');
const isLoadingLanguages = ref(false);

const showWordDialog = ref(false);
const selectedWord = ref<Word | null>(null);
const isLoadingWord = ref(false);
const wordError = ref<string | null>(null);

async function loadLanguages() {
  isLoadingLanguages.value = true;
  try {
    languages.value = await getLanguages();
    if (languages.value.length > 0) {
      selectedLanguage.value = languages.value[0].code;
    }
  } catch (error) {
    console.error('Failed to load languages', error);
  } finally {
    isLoadingLanguages.value = false;
  }
}

async function onWordClick(lemma: string) {
  if (!selectedLanguage.value) {
    alert('Please select a language first.');
    return;
  }

  showWordDialog.value = true;
  isLoadingWord.value = true;
  wordError.value = null;
  selectedWord.value = null;

  try {
    const word = await getWord(selectedLanguage.value, lemma);
    if (word) {
      selectedWord.value = word;
    } else {
      wordError.value = 'Word details not found';
    }
  } catch (error) {
    wordError.value = 'Failed to load word details';
  } finally {
    isLoadingWord.value = false;
  }
}

const bookPages = [
  `Chapitre 1: Le Voyage Inattendu\n\nDans un petit village niché au creux d'une vallée verdoyante, vivait un jeune homme nommé Léo. Il était connu pour son esprit curieux et son amour des livres. Chaque jour, il passait des heures dans la bibliothèque du village, un bâtiment ancien rempli de récits d'aventures et de savoirs oubliés.`,
  `Un matin, alors qu'il explorait une section poussiéreuse, il découvrit un livre étrange relié en cuir sombre. Le titre, écrit en lettres dorées, était illisible. Intrigué, Léo ouvrit le livre et sentit une énergie mystérieuse l'envahir. Les pages étaient couvertes d'une écriture qu'il n'avait jamais vue.`,
  `Soudain, une carte tomba du livre. Elle montrait un chemin vers une montagne lointaine, marquée d'un symbole de soleil. Poussé par une soif d'aventure, Léo décida de suivre cette carte. Il prépara un petit sac avec de la nourriture, de l'eau et le livre mystérieux, puis quitta le village à l'aube.`,
  `Le chemin était long et ardu. Il traversa des forêts denses, escalada des collines escarpées et traversa des rivières tumultueuses. Chaque soir, à la lueur du feu, il étudiait le livre, essayant de déchiffrer ses secrets. Lentement, il commença à comprendre certains mots, qui parlaient d'un ancien pouvoir caché dans la montagne.`,
  `Après plusieurs jours de marche, il arriva au pied de la montagne. Une grotte sombre, marquée du même symbole de soleil, se dressait devant lui. Prenant une profonde inspiration, Léo entra dans l'obscurité, le cœur battant d'excitation et d'un peu de peur. Il ne savait pas ce qu'il allait trouver, mais il était prêt à affronter l'inconnu.`,
];

const longText = bookPages.join('\n\n');

onMounted(() => {
  loadLanguages();
});
</script>

<style scoped>
.book-view-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

h1 {
  text-align: center;
  color: var(--text-color);
  font-weight: 700;
  margin-bottom: 2rem;
}

.view-controls {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.demo-controls {
  display: flex;
  justify-content: center;
  gap: 1rem;
}

.demo-controls button {
  padding: 0.5rem 1rem;
  font-size: 1rem;
  border-radius: 8px;
  border: 1px solid var(--primary-color);
  background: transparent;
  color: var(--primary-color);
  cursor: pointer;
  transition: all 0.2s;
}

.demo-controls button.active, .demo-controls button:hover {
  background: var(--primary-color);
  color: var(--primary-color-text);
}

.language-selector-wrapper {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.language-selector-wrapper label {
  font-weight: 600;
}

.demo-section {
  margin-top: 4rem;
  padding: 2rem;
  background: var(--surface-card);
  border-radius: var(--border-radius);
  border: 1px solid var(--surface-border);
}

.demo-section h2 {
  color: var(--text-color);
  margin-bottom: 1rem;
}

pre {
  background: var(--surface-ground);
  padding: 1.5rem;
  border-radius: 8px;
  overflow-x: auto;
}

code {
  font-family: 'Monaco', 'Courier New', monospace;
  font-size: 0.9rem;
  line-height: 1.6;
  color: var(--text-color);
}
</style>
