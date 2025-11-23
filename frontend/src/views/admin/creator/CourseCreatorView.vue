<script setup lang="ts">
import { ref } from 'vue'
import { GenerationService } from '@/services/GenerationService'
import type { GenerateModuleRequest, GenerateStructureRequest, GenerateBatchExercisesRequest, GeneratedModule, GeneratedContentItem } from '@/types/generation'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import RadioButton from 'primevue/radiobutton'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Dropdown from 'primevue/dropdown'
import Panel from 'primevue/panel'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import Steps from 'primevue/steps'

const activeStep = ref(0)
const steps = [
  { label: 'Drafting' },
  { label: 'Structure' },
  { label: 'Workshop' }
]

const voices = [
  // Female
  'Puck', 'Laomedeia', 'Zephyr', 'Leda', 'Kore', 'Erinome', 'Autonoe',
  'Achernar', 'Vindemiatrix', 'Despina', 'Sulafat', 'Aoede', 'Callirrhoe',
  'Rasalgethi', 'Sadaltager', 'Pulcherrima', 'Gacrux',
  // Male
  'Algieba', 'Charon', 'Enceladus', 'Achird', 'Umbriel', 'Zubenelgenubi',
  'Iapetus', 'Orus', 'Alnilam', 'Schedar', 'Fenrir', 'Sadachbia', 'Algenib'
]

// Step 1: Drafting
const theme = ref('')
const level = ref('A1')
const targetLanguage = ref('French')
const contentType = ref('DIALOGUE')
const focus = ref('')
const additionalInstructions = ref('')
const draftScript = ref('')
const loading = ref(false)
const error = ref('')

// Step 2: Structure
const generatedModule = ref<GeneratedModule | null>(null)
const speakerVoiceMap = ref<Record<string, string>>({})

// Step 3: Workshop
const generatingExercises = ref(false)


const levels = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']
const languages = ['French', 'Spanish', 'German', 'Italian', 'English']
const contentTypes = ['DIALOGUE', 'STORY']
const themes = [
  "At the Bakery", "Asking for Directions", "Introducing Yourself", 
  "Ordering Coffee", "At the Doctor", "Shopping for Clothes", 
  "Booking a Hotel", "At the Airport", "Meeting New People", "Discussing Hobbies"
]

const suggestTheme = () => {
  const randomTheme = themes[Math.floor(Math.random() * themes.length)]
  theme.value = randomTheme
}

const generateDraft = async () => {
  if (!theme.value) return

  loading.value = true
  error.value = ''
  
  try {
    const request: GenerateModuleRequest = {
      theme: theme.value,
      level: level.value,
      targetLanguage: targetLanguage.value,
      contentType: contentType.value || 'DIALOGUE',
      focus: focus.value || undefined,
      additionalInstructions: additionalInstructions.value || undefined
    }

    draftScript.value = await GenerationService.generateDialogue(request)
  } catch (e: any) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

const generateStructure = async () => {
  if (!draftScript.value) return

  loading.value = true
  error.value = ''
  activeStep.value = 1 // Move to next step immediately to show loading state there

  try {
    const request: GenerateStructureRequest = {
      script: draftScript.value,
      theme: theme.value,
      level: level.value,
      targetLanguage: targetLanguage.value
    }

    const response = await GenerationService.generateStructure(request)
    generatedModule.value = response.module
    
    // Initialize speaker map
    if (response.module.episodes[0]?.dialogue?.speakers) {
      speakerVoiceMap.value = {}
      Object.entries(response.module.episodes[0].dialogue.speakers).forEach(([name, speaker]) => {
        speakerVoiceMap.value[name] = speaker.voice
      })
    }
  } catch (e: any) {
    error.value = e.message
    activeStep.value = 0 // Go back on error
  } finally {
    loading.value = false
  }
}

const nextStep = () => {
  if (activeStep.value === 0) {
    generateStructure()
  } else {
    activeStep.value++
  }
}

const generateBatchExercises = async () => {
  if (!generatedModule.value?.episodes[0]?.dialogue) return

  generatingExercises.value = true
  
  try {
    // Use full context
    const contextLines = generatedModule.value.episodes[0].dialogue.lines
      .map(l => `${l.speaker}: ${l.text}`)
      .join('\n')

    const request: GenerateBatchExercisesRequest = {
      context: contextLines,
      targetLanguage: targetLanguage.value,
      level: level.value,
      exerciseCounts: {
        'MULTIPLE_CHOICE': 4,
        'FILL_IN_THE_BLANK': 4,
        'SENTENCE_SCRAMBLE': 2,
        'CLOZE_READING': 1,
        'MATCHING': 2
      }
    }

    const exercises = await GenerationService.generateBatchExercises(request)
    
    // Add to module
    if (!generatedModule.value.episodes[0].contentItems) {
      generatedModule.value.episodes[0].contentItems = []
    }
    generatedModule.value.episodes[0].contentItems.push(...exercises)
    
  } catch (e: any) {
    error.value = e.message
  } finally {
    generatingExercises.value = false
  }
}

const removeExercise = (idx: number) => {
  if (generatedModule.value?.episodes[0]?.contentItems) {
    generatedModule.value.episodes[0].contentItems.splice(idx, 1)
  }
}

const finalizeModule = () => {
  console.log('Final Module JSON:', JSON.stringify(generatedModule.value, null, 2))
  alert('Module JSON logged to console! In a real app, this would save to the database.')
}
</script>

<template>
  <div class="view-container content-area-lg">
    <div class="page-header mb-lg">
      <h1>Content Creator Studio</h1>
      <p class="text-secondary">Interactive Course Authoring</p>
    </div>

    <Steps :model="steps" v-model:activeStep="activeStep" class="mb-xl" :readonly="false" />

    <!-- STEP 1: DRAFTING -->
    <div v-if="activeStep === 0" class="grid grid-cols-1 lg:grid-cols-3 gap-lg">
      <div class="col-span-1">
        <Panel header="Story Settings">
          <div class="flex flex-col gap-md">
            <div class="flex flex-col gap-xs">
              <label for="language" class="font-bold">Target Language</label>
              <Dropdown id="language" v-model="targetLanguage" :options="languages" />
            </div>

            <div class="flex flex-col gap-xs">
              <label class="font-bold">Content Type</label>
              <Dropdown v-model="contentType" :options="contentTypes" />
            </div>

            <div class="flex flex-col gap-xs">
              <label for="theme" class="font-bold">Theme</label>
              <div class="flex gap-sm">
                <InputText id="theme" v-model="theme" placeholder="e.g., At the Bakery" class="flex-1" />
                <Button icon="pi pi-lightbulb" @click="suggestTheme" v-tooltip="'Suggest Theme'" severity="secondary" />
              </div>
            </div>

            <div class="flex flex-col gap-xs">
              <label for="level" class="font-bold">Level</label>
              <Dropdown id="level" v-model="level" :options="levels" />
            </div>

            <div class="flex flex-col gap-xs">
              <label for="focus" class="font-bold">Focus (Optional)</label>
              <InputText id="focus" v-model="focus" placeholder="e.g., Past Tense" />
            </div>
            
             <div class="flex flex-col gap-xs">
              <label for="instructions" class="font-bold">Extra Instructions</label>
              <Textarea id="instructions" v-model="additionalInstructions" rows="3" />
            </div>

            <Button label="Generate Draft" icon="pi pi-sparkles" @click="generateDraft" :loading="loading" />
          </div>
        </Panel>
      </div>

      <div class="col-span-1 lg:col-span-2">
        <div v-if="loading" class="flex justify-center p-xl">
          <ProgressSpinner />
        </div>
        
        <div v-else-if="error">
           <Message severity="error">{{ error }}</Message>
        </div>

        <Panel v-else header="Draft Script">
          <div class="flex flex-col gap-md">
            <p class="text-secondary text-sm">
              Review and edit the AI-generated script below. This text will be the foundation for your module.
            </p>
            <Textarea v-model="draftScript" rows="20" class="font-mono text-base w-full leading-relaxed" placeholder="Script will appear here..." />
            
            <div class="flex justify-end">
              <Button label="Next: Structure" icon="pi pi-arrow-right" @click="nextStep" :disabled="!draftScript" />
            </div>
          </div>
        </Panel>
      </div>
    </div>

    <!-- STEP 2: STRUCTURE -->
    <div v-if="activeStep === 1" class="grid grid-cols-1 lg:grid-cols-3 gap-lg">
      <div class="col-span-1">
        <Panel header="Voice Mapping">
          <div v-if="loading" class="flex justify-center p-md">
            <ProgressSpinner style="width: 30px; height: 30px" />
          </div>
          <div v-else-if="generatedModule?.episodes[0]?.dialogue" class="flex flex-col gap-md">
            <p class="text-secondary text-sm">Assign voices to detected speakers.</p>
            
            <div v-for="(voice, speaker) in speakerVoiceMap" :key="speaker" class="flex flex-col gap-xs">
              <label class="font-bold">{{ speaker }}</label>
              <Dropdown v-model="speakerVoiceMap[speaker]" :options="voices" filter placeholder="Select Voice" />
            </div>
            
            <Button label="Confirm Voices" icon="pi pi-check" @click="nextStep" class="mt-md" />
          </div>
          <div v-else>
             <p class="text-secondary">No speakers detected.</p>
          </div>
        </Panel>
      </div>

      <div class="col-span-1 lg:col-span-2">
        <Panel header="Structure Preview">
           <div v-if="loading" class="flex justify-center p-xl">
            <ProgressSpinner />
          </div>
          <div v-else-if="generatedModule?.episodes[0]?.dialogue" class="flex flex-col gap-md">
             <div v-for="(line, idx) in generatedModule.episodes[0].dialogue.lines" :key="idx" class="p-sm border rounded bg-surface-ground">
               <div class="flex justify-between items-center mb-xs">
                 <span class="font-bold text-primary">{{ line.speaker }}</span>
                 <span class="text-xs text-secondary">{{ speakerVoiceMap[line.speaker] }}</span>
               </div>
               <p class="m-0">{{ line.text }}</p>
             </div>
          </div>
        </Panel>
      </div>
    </div>

    <!-- STEP 3: WORKSHOP -->
    <div v-if="activeStep === 2" class="grid grid-cols-1 lg:grid-cols-3 gap-lg">
      <div class="col-span-1 lg:col-span-2">
        <Panel header="Dialogue Context">
          <p class="text-secondary text-sm mb-md">Exercises will be generated based on this context.</p>
          
          <div v-if="generatedModule?.episodes[0]?.dialogue" class="flex flex-col gap-sm">
             <div v-for="(line, idx) in generatedModule.episodes[0].dialogue.lines" :key="idx" 
                  class="p-sm border rounded flex items-start gap-sm bg-surface-ground">
               <div>
                 <span class="font-bold text-primary text-sm block">{{ line.speaker }}</span>
                 <p class="m-0">{{ line.text }}</p>
               </div>
             </div>
          </div>
        </Panel>
      </div>

      <div class="col-span-1">
        <div class="sticky top-4 flex flex-col gap-lg">
          <Button 
            label="Auto-Generate Workshop" 
            icon="pi pi-sparkles" 
            @click="generateBatchExercises" 
            :loading="generatingExercises"
            severity="help"
          />
          
          <Panel header="Module Exercises">
            <div v-if="generatedModule?.episodes[0]?.contentItems?.length" class="flex flex-col gap-md">
              <div v-for="(item, idx) in generatedModule.episodes[0].contentItems" :key="idx" class="p-md border rounded bg-surface-card shadow-sm relative group">
                <Button icon="pi pi-trash" text severity="danger" class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity" @click="removeExercise(idx)" />
                <span class="text-xs font-bold uppercase text-secondary mb-xs block">{{ item.exercise.type }}</span>
                <div v-if="item.exercise.type === 'MATCHING'">
                  <p class="font-bold m-0 mb-xs">{{ item.exercise.content.question.content }}</p>
                  <div class="grid grid-cols-2 gap-sm text-sm">
                    <template v-for="(pair, pIdx) in item.exercise.content.pairs" :key="pIdx">
                      <div class="p-xs bg-surface-ground rounded">{{ pair.left }}</div>
                      <div class="p-xs bg-surface-ground rounded">{{ pair.right }}</div>
                    </template>
                  </div>
                </div>

                <div v-else-if="item.exercise.type === 'SENTENCE_SCRAMBLE'">
                  <p class="font-bold m-0 mb-xs">Unscramble: {{ item.exercise.content.sentence }}</p>
                  <div class="flex flex-wrap gap-xs">
                    <span v-for="(word, wIdx) in item.exercise.content.words" :key="wIdx" class="p-xs bg-surface-ground rounded text-sm border border-surface-border">
                      {{ word }}
                    </span>
                  </div>
                </div>

                <div v-else-if="item.exercise.type === 'CLOZE_READING'">
                  <p class="font-bold m-0 mb-xs">Cloze Reading</p>
                  <p class="text-sm leading-relaxed bg-surface-ground p-sm rounded italic">
                    {{ item.exercise.content.text }}
                  </p>
                </div>
                
                <div v-else>
                  <p class="font-bold m-0 mb-xs">{{ item.exercise.content.question.content }}</p>
                  <ul class="list-disc pl-md m-0 text-sm text-secondary">
                    <li v-for="opt in item.exercise.content.options" :key="opt.id" :class="{'text-green-600 font-bold': opt.isCorrect}">
                      {{ opt.text }}
                    </li>
                  </ul>
                </div>
              </div>
            </div>
            <div v-else class="text-center p-lg text-secondary">
              No exercises yet. Select lines and click "Add Exercise".
            </div>
          </Panel>

          <Button label="Finalize Module" severity="success" icon="pi pi-check-circle" @click="finalizeModule" />
        </div>
      </div>
    </div>

    <!-- Exercise Dialog Removed -->

  </div>
</template>

<style scoped>
.font-mono {
  font-family: monospace;
}
</style>
