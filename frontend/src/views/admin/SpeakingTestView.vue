<script setup lang="ts">
import {ref} from 'vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Dropdown from 'primevue/dropdown'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import SpeakingExercise from '@/components/exercises/SpeakingExercise.vue'
import {exerciseAPI} from '@/api'
import type {SpeakingAttemptResult, SpeakingContent} from '@/types/exercise'

const speakingRef = ref<InstanceType<typeof SpeakingExercise> | null>(null)
const isLoading = ref(false)
const error = ref<string | null>(null)

// Test configuration
const selectedLanguage = ref('fr')
const customText = ref('')

const languages = [
  {label: 'French', value: 'fr'},
  {label: 'German', value: 'de'},
  {label: 'Spanish', value: 'es'},
  {label: 'English', value: 'en'},
]

// Preset test phrases
const testPhrases: Record<string, { text: string; hint: string }[]> = {
  fr: [
    {text: 'Bonjour, comment allez-vous?', hint: 'Remember: "comment" sounds like "ko-mah"'},
    {text: 'Je voudrais un café, s\'il vous plaît', hint: '"s\'il" is a contraction of "si il"'},
    {text: 'Excusez-moi, où est la gare?', hint: '"gare" means train station'},
    {text: 'Je m\'appelle...', hint: 'Fill in your name at the end'},
  ],
  de: [
    {text: 'Guten Tag, wie geht es Ihnen?', hint: '"Ihnen" is the formal "you"'},
    {text: 'Ich möchte einen Kaffee, bitte', hint: '"möchte" is "would like"'},
    {text: 'Entschuldigung, wo ist der Bahnhof?', hint: '"Bahnhof" means train station'},
  ],
  es: [
    {text: 'Hola, ¿cómo estás?', hint: 'Informal greeting'},
    {text: 'Me gustaría un café, por favor', hint: '"Me gustaría" = "I would like"'},
    {text: 'Disculpe, ¿dónde está la estación?', hint: 'Formal "excuse me"'},
  ],
  en: [
    {text: 'Hello, how are you today?', hint: 'Standard greeting'},
    {text: 'I would like a coffee, please', hint: 'Polite request'},
    {text: 'Excuse me, where is the train station?', hint: 'Asking for directions'},
  ],
}

const selectedPhraseIndex = ref(0)

const currentContent = ref<SpeakingContent>({
  mode: 'read_aloud',
  targetText: testPhrases.fr[0].text,
  targetLanguage: 'fr',
  hint: testPhrases.fr[0].hint,
})

function selectPhrase(index: number) {
  selectedPhraseIndex.value = index
  const phrases = testPhrases[selectedLanguage.value] || testPhrases.en
  const phrase = phrases[index]
  currentContent.value = {
    mode: 'read_aloud',
    targetText: phrase.text,
    targetLanguage: selectedLanguage.value,
    hint: phrase.hint,
  }
  speakingRef.value?.resetExercise()
  error.value = null
}

function useCustomText() {
  if (!customText.value.trim()) return
  currentContent.value = {
    mode: 'read_aloud',
    targetText: customText.value.trim(),
    targetLanguage: selectedLanguage.value,
  }
  speakingRef.value?.resetExercise()
  error.value = null
}

function onLanguageChange() {
  selectedPhraseIndex.value = 0
  selectPhrase(0)
}

async function handleSubmit(audioBlob: Blob) {
  isLoading.value = true
  error.value = null

  try {
    const result: SpeakingAttemptResult = await exerciseAPI.validateSpeaking(
        audioBlob,
        currentContent.value.targetText,
        currentContent.value.targetLanguage
    )
    speakingRef.value?.setResult(result)
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Failed to process audio'
    error.value = message
    speakingRef.value?.setError(message)
  } finally {
    isLoading.value = false
  }
}

function handleNext() {
  // Move to next phrase
  const phrases = testPhrases[selectedLanguage.value] || testPhrases.en
  const nextIndex = (selectedPhraseIndex.value + 1) % phrases.length
  selectPhrase(nextIndex)
}
</script>

<template>
  <div class="view-container content-area-md">
    <div class="page-header">
      <div>
        <h1>Speaking Test</h1>
        <p class="text-secondary">Test the speaking exercise functionality with Gemini transcription</p>
      </div>
    </div>

    <!-- Configuration -->
    <Card class="mb-4">
      <template #title>Configuration</template>
      <template #content>
        <div class="config-grid">
          <div class="config-item">
            <label>Language</label>
            <Dropdown
                v-model="selectedLanguage"
                :options="languages"
                optionLabel="label"
                optionValue="value"
                @change="onLanguageChange"
                class="w-full"
            />
          </div>
          <div class="config-item">
            <label>Custom Text (optional)</label>
            <div class="flex gap-2">
              <InputText
                  v-model="customText"
                  placeholder="Enter custom phrase..."
                  class="flex-1"
                  @keyup.enter="useCustomText"
              />
              <Button label="Use" @click="useCustomText" :disabled="!customText.trim()"/>
            </div>
          </div>
        </div>
      </template>
    </Card>

    <!-- Preset Phrases -->
    <Card class="mb-4">
      <template #title>Test Phrases</template>
      <template #content>
        <div class="phrases-grid">
          <Button
              v-for="(phrase, index) in (testPhrases[selectedLanguage] || testPhrases.en)"
              :key="index"
              :label="phrase.text"
              :severity="selectedPhraseIndex === index ? 'primary' : 'secondary'"
              :outlined="selectedPhraseIndex !== index"
              @click="selectPhrase(index)"
              class="phrase-button"
          />
        </div>
      </template>
    </Card>

    <!-- Error Message -->
    <Message v-if="error" severity="error" class="mb-4" :closable="false">
      {{ error }}
    </Message>

    <!-- Speaking Exercise -->
    <Card>
      <template #title>Speaking Exercise</template>
      <template #content>
        <SpeakingExercise
            ref="speakingRef"
            :content="currentContent"
            @submit="handleSubmit"
            @next="handleNext"
        />
      </template>
    </Card>

    <!-- Debug Info -->
    <Card class="mt-4">
      <template #title>Debug Info</template>
      <template #content>
        <pre class="debug-info">{{ JSON.stringify(currentContent, null, 2) }}</pre>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.config-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 1.5rem;
  align-items: end;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.config-item label {
  font-weight: 500;
  color: var(--text-color-secondary);
}

.phrases-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.phrase-button {
  text-align: left;
  white-space: normal;
  height: auto;
  padding: 0.75rem 1rem;
}

.debug-info {
  background: var(--surface-ground);
  padding: 1rem;
  border-radius: var(--border-radius);
  font-size: 0.875rem;
  overflow-x: auto;
  margin: 0;
}

@media (max-width: 768px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
