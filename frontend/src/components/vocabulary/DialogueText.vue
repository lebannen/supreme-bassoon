<script setup lang="ts">
import {computed, ref} from 'vue'
import WordCard from './WordCard.vue'
import {dictionaryAPI} from '@/api'
import type {Word} from '@/types/dictionary'
import type {WordSummary} from '@/types/wordSet'

interface Props {
  text: string
  languageCode: string
  vocabularyWords?: WordSummary[]
}

const props = defineProps<Props>()

const showWordDialog = ref(false)
const selectedWord = ref<Word | null>(null)
const wordLoading = ref(false)
const wordError = ref<string | null>(null)

// Create a Set of vocabulary lemmas for O(1) lookup (lowercase for comparison)
const vocabularyLemmas = computed(() => {
  if (!props.vocabularyWords) return new Set<string>()
  return new Set(props.vocabularyWords.map(w => w.lemma.toLowerCase()))
})

// Tokenize text into words and punctuation
interface Token {
  text: string
  type: 'word' | 'punctuation' | 'space'
  isVocabulary: boolean
  normalizedWord?: string
}

const tokens = computed((): Token[] => {
  const result: Token[] = []
  // Match: word characters (including unicode letters), or punctuation, or spaces
  const regex = /([a-zA-Z\u00C0-\u024F\u1E00-\u1EFF''-]+)|(\s+)|([^\w\s])/gu
  let match: RegExpExecArray | null

  while ((match = regex.exec(props.text)) !== null) {
    if (match[1]) {
      // Word
      const word = match[1]
      const normalized = normalizeWord(word)
      const isVocab = vocabularyLemmas.value.has(normalized)
      result.push({
        text: word,
        type: 'word',
        isVocabulary: isVocab,
        normalizedWord: normalized
      })
    } else if (match[2]) {
      // Space
      result.push({text: match[2], type: 'space', isVocabulary: false})
    } else if (match[3]) {
      // Punctuation
      result.push({text: match[3], type: 'punctuation', isVocabulary: false})
    }
  }

  return result
})

function normalizeWord(word: string): string {
  return word.toLowerCase()
      .replace(/^['']/g, '')  // Remove leading apostrophe
      .replace(/['']/g, "'")  // Normalize apostrophes
}

async function handleWordClick(token: Token) {
  if (token.type !== 'word') return

  wordLoading.value = true
  wordError.value = null
  showWordDialog.value = true

  try {
    // Try exact word first
    let word = await dictionaryAPI.getWord(props.languageCode, token.normalizedWord || token.text)

    // If not found, try the original text
    if (!word && token.normalizedWord !== token.text.toLowerCase()) {
      word = await dictionaryAPI.getWord(props.languageCode, token.text.toLowerCase())
    }

    if (word) {
      selectedWord.value = word
    } else {
      wordError.value = `Word "${token.text}" not found in dictionary`
    }
  } catch (err) {
    wordError.value = err instanceof Error ? err.message : 'Failed to load word'
  } finally {
    wordLoading.value = false
  }
}

async function handleWordCardClick(lemma: string) {
  wordLoading.value = true
  wordError.value = null

  try {
    const word = await dictionaryAPI.getWord(props.languageCode, lemma)
    if (word) {
      selectedWord.value = word
    } else {
      wordError.value = `Word "${lemma}" not found`
    }
  } catch (err) {
    wordError.value = err instanceof Error ? err.message : 'Failed to load word'
  } finally {
    wordLoading.value = false
  }
}
</script>

<template>
  <span class="dialogue-text">
    <template v-for="(token, index) in tokens" :key="index">
      <span
          v-if="token.type === 'word'"
          class="word"
          :class="{ 'vocabulary-word': token.isVocabulary }"
          @click="handleWordClick(token)"
      >{{ token.text }}</span>
      <span v-else>{{ token.text }}</span>
    </template>
  </span>

  <WordCard
      v-model:visible="showWordDialog"
      :word="selectedWord"
      :loading="wordLoading"
      :error="wordError"
      @word-click="handleWordCardClick"
  />
</template>

<style scoped>
.dialogue-text {
  display: inline;
}

.word {
  cursor: pointer;
  border-radius: 2px;
  transition: background-color 0.15s, color 0.15s;
}

.word:hover {
  background-color: var(--primary-100);
  color: var(--primary-700);
}

.vocabulary-word {
  background-color: var(--yellow-100);
  color: var(--yellow-900);
  padding: 0 2px;
  border-radius: 3px;
}

.vocabulary-word:hover {
  background-color: var(--yellow-200);
}

:deep(.dark) .word:hover {
  background-color: var(--primary-900);
  color: var(--primary-300);
}

:deep(.dark) .vocabulary-word {
  background-color: var(--yellow-900);
  color: var(--yellow-100);
}

:deep(.dark) .vocabulary-word:hover {
  background-color: var(--yellow-800);
}
</style>
