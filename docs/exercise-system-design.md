# Exercise System Design - Interactive Language Learning

**Status:** 📋 **PLANNED** (M2 Feature)
**Created:** November 2025
**Target Implementation:** M2 Development Phase

---

## Table of Contents

1. [Overview](#overview)
2. [Design Principles](#design-principles)
3. [Exercise Type Categories](#exercise-type-categories)
4. [Database Schema](#database-schema)
5. [Exercise Type Specifications](#exercise-type-specifications)
6. [Backend Architecture](#backend-architecture)
7. [Frontend Architecture](#frontend-architecture)
8. [Implementation Phases](#implementation-phases)
9. [AI Integration Strategy](#ai-integration-strategy)

---

## Overview

The Exercise System extends Vocabee's learning capabilities beyond passive reading and spaced repetition flashcards to include interactive, structured exercises aligned with the French A1 curriculum (extensible to other languages and levels).

### Core Goals

1. **Curriculum-Aligned**: Exercises map to the 10-module French A1 plan
2. **Progressive Difficulty**: Scaffolded from recognition → production
3. **Immediate Feedback**: Instant validation with explanations
4. **Data-Driven**: Leverage existing Wiktionary data + AI generation
5. **Reusable Infrastructure**: Generic exercise framework supporting multiple types

### Integration with Existing Features

- **Vocabulary System**: Exercises use words from user vocabulary and word sets
- **Reading Texts**: Reading comprehension exercises based on existing texts
- **Spaced Repetition**: Exercise performance influences SRS scheduling
- **TTS/Audio**: Pronunciation and listening exercises use existing audio pipeline

---

## Design Principles

### 1. Fail-Fast Validation
- Users get immediate feedback on every interaction
- Clear indication of correct/incorrect answers
- Explanatory hints available on demand

### 2. Adaptive Scaffolding
- Earlier exercises use recognition (multiple choice, matching)
- Later exercises use recall (fill-in-blank, typing)
- Hardest exercises require production (translation, speaking)

### 3. Context-Rich Content
- All exercises tied to real vocabulary and grammar
- Examples from authentic language use (Wiktionary)
- Cultural context where relevant

### 4. Gamification Without Distraction
- Points and progress tracking
- Unlocking system for motivation
- No artificial time pressure (except optional challenges)

### 5. Mobile-First Design
- Touch-friendly interactions (tap, drag, swipe)
- Minimal typing on mobile
- Responsive layouts

---

## Exercise Type Categories

### Category 1: Vocabulary Recognition (A1 Modules 1-3)
**Cognitive Load:** Low
**Input Method:** Tap/Click

- **Image-to-Word Matching**: Match pictures with French words
- **Word-to-Translation Matching**: Match French ↔ English
- **Audio-to-Word**: Hear word → select written form
- **Multiple Choice (Definition)**: "What does 'bonjour' mean?"

### Category 2: Grammar & Structure (A1 Modules 2-5)
**Cognitive Load:** Medium
**Input Method:** Select, Drag

- **Article Selection**: Choose le/la/un/une for nouns
- **Verb Conjugation (Multiple Choice)**: Select correct form
- **Word Order (Sentence Scramble)**: Arrange words into sentence
- **Gender Identification**: Masculine or feminine?

### Category 3: Listening Comprehension (A1 Modules 4-6)
**Cognitive Load:** Medium
**Input Method:** Select, Type

- **Listen & Select Image**: Hear sentence → choose picture
- **Dictation (Assisted)**: Hear word → select from word bank
- **Dictation (Free)**: Hear word → type it
- **Dialogue Comprehension**: Answer questions about dialogue

### Category 4: Reading Comprehension (A1 Modules 5-7)
**Cognitive Load:** Medium-High
**Input Method:** Select, Type

- **Cloze Reading**: Fill blanks in paragraph
- **True/False Questions**: Answer about short text
- **Highlight the Answer**: Click word/phrase that answers question
- **Sentence Completion**: Finish the sentence

### Category 5: Production & Translation (A1 Modules 7-10)
**Cognitive Load:** High
**Input Method:** Type, Speak

- **Guided Translation**: Translate with word bank
- **Free Translation**: Translate without help
- **Fill-in-the-Blank (Type)**: Type missing word
- **Sentence Construction**: Build sentence from prompt

---

## Database Schema

### Core Exercise Tables

```sql
-- Exercise Types (system table)
CREATE TABLE exercise_types (
    id SERIAL PRIMARY KEY,
    type_key VARCHAR(50) UNIQUE NOT NULL,  -- 'multiple_choice', 'matching', etc.
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,  -- 'vocabulary', 'grammar', 'listening', etc.
    difficulty_level VARCHAR(10),  -- 'beginner', 'intermediate', 'advanced'
    requires_audio BOOLEAN DEFAULT false,
    requires_images BOOLEAN DEFAULT false,
    interaction_type VARCHAR(50)  -- 'select', 'type', 'drag', 'speak'
);

-- Exercise Definitions
CREATE TABLE exercises (
    id BIGSERIAL PRIMARY KEY,
    exercise_type_id INT NOT NULL REFERENCES exercise_types(id),
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),

    -- Module/Topic Organization
    module_number INT,  -- 1-10 for A1 French
    topic VARCHAR(100),  -- 'greetings', 'family', 'directions', etc.
    cefr_level VARCHAR(10) DEFAULT 'A1',

    -- Content
    title VARCHAR(255) NOT NULL,
    instructions TEXT NOT NULL,

    -- Exercise Data (JSONB for flexibility)
    content JSONB NOT NULL,  -- Structure varies by exercise type

    -- Metadata
    estimated_duration_seconds INT DEFAULT 60,
    points_value INT DEFAULT 10,
    difficulty_rating DECIMAL(2,1) DEFAULT 1.0,  -- 1.0 = easiest, 5.0 = hardest

    -- Status
    is_published BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(50) DEFAULT 'system'
);

CREATE INDEX idx_exercises_language ON exercises(language_code, cefr_level);
CREATE INDEX idx_exercises_module ON exercises(module_number, topic);
CREATE INDEX idx_exercises_type ON exercises(exercise_type_id);

-- User Exercise Attempts
CREATE TABLE user_exercise_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,

    -- Attempt Details
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    duration_seconds INT,

    -- Results
    score DECIMAL(5,2),  -- 0-100
    max_score DECIMAL(5,2) DEFAULT 100,
    is_correct BOOLEAN,

    -- Detailed Response Data
    user_responses JSONB,  -- Stores user's answers
    correct_answers JSONB,  -- Stores correct answers for review

    -- Performance
    hints_used INT DEFAULT 0,
    attempts_count INT DEFAULT 1
);

CREATE INDEX idx_user_attempts_user ON user_exercise_attempts(user_id);
CREATE INDEX idx_user_attempts_exercise ON user_exercise_attempts(exercise_id);
CREATE INDEX idx_user_attempts_completed ON user_exercise_attempts(user_id, completed_at);

-- Exercise Collections (groups of related exercises)
CREATE TABLE exercise_collections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),
    module_number INT,
    cefr_level VARCHAR(10) DEFAULT 'A1',

    is_published BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE exercise_collection_items (
    id BIGSERIAL PRIMARY KEY,
    collection_id BIGINT NOT NULL REFERENCES exercise_collections(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    is_optional BOOLEAN DEFAULT false,

    UNIQUE(collection_id, exercise_id)
);

CREATE INDEX idx_collection_items_collection ON exercise_collection_items(collection_id, order_index);
```

---

## Exercise Type Specifications

### 1. Multiple Choice (Vocabulary/Definition)

**Exercise Type:** `multiple_choice_definition`
**Category:** Vocabulary Recognition
**Interaction:** Select

**Content Structure (JSONB):**
```json
{
  "question": {
    "type": "text",  // or "audio", "image"
    "content": "parler",
    "audioUrl": "https://..."  // optional
  },
  "options": [
    { "id": "a", "text": "to speak", "isCorrect": true },
    { "id": "b", "text": "to eat", "isCorrect": false },
    { "id": "c", "text": "to sleep", "isCorrect": false },
    { "id": "d", "text": "to work", "isCorrect": false }
  ],
  "explanation": "Parler means 'to speak' in French.",
  "hint": "Think about communication."
}
```

**Generation Strategy:**
- Question: Use word from vocabulary/word set
- Correct answer: Use Wiktionary definition or translation
- Distractors: Select semantically similar words or same part of speech

**Validation Logic:**
```kotlin
fun validateMultipleChoice(userAnswer: String, content: JsonNode): ValidationResult {
    val correctOption = content["options"].find { it["isCorrect"].asBoolean() }
    val isCorrect = userAnswer == correctOption["id"].asText()

    return ValidationResult(
        isCorrect = isCorrect,
        score = if (isCorrect) 100 else 0,
        feedback = if (isCorrect) "Correct!" else content["explanation"].asText()
    )
}
```

---

### 2. Image-to-Text Matching

**Exercise Type:** `image_text_matching`
**Category:** Vocabulary Recognition
**Interaction:** Drag-and-drop or tap pairs

**Content Structure (JSONB):**
```json
{
  "pairs": [
    { "id": 1, "imageUrl": "https://.../bread.jpg", "word": "le pain" },
    { "id": 2, "imageUrl": "https://.../coffee.jpg", "word": "le café" },
    { "id": 3, "imageUrl": "https://.../croissant.jpg", "word": "le croissant" },
    { "id": 4, "imageUrl": "https://.../water.jpg", "word": "l'eau" }
  ],
  "hint": "Match the French words with their images."
}
```

**User Response Structure:**
```json
{
  "matches": [
    { "imageId": 1, "wordId": 1 },  // Correct
    { "imageId": 2, "wordId": 2 },  // Correct
    { "imageId": 3, "wordId": 4 },  // Incorrect
    { "imageId": 4, "wordId": 3 }   // Incorrect
  ]
}
```

**Validation Logic:**
```kotlin
fun validateMatching(userMatches: List<Match>, correctPairs: List<Pair>): ValidationResult {
    val correctCount = userMatches.count { match ->
        correctPairs.any { it.imageId == match.imageId && it.wordId == match.wordId }
    }

    val score = (correctCount.toDouble() / correctPairs.size * 100).toInt()

    return ValidationResult(
        isCorrect = correctCount == correctPairs.size,
        score = score,
        feedback = "$correctCount out of ${correctPairs.size} correct"
    )
}
```

---

### 3. Sentence Scramble (Word Order)

**Exercise Type:** `sentence_scramble`
**Category:** Grammar & Structure
**Interaction:** Drag-and-drop

**Content Structure (JSONB):**
```json
{
  "correctSentence": "Je parle français",
  "words": [
    { "id": "w1", "text": "Je", "order": 0 },
    { "id": "w2", "text": "parle", "order": 1 },
    { "id": "w3", "text": "français", "order": 2 }
  ],
  "translation": "I speak French",
  "hint": "Subject comes first, then verb, then object.",
  "grammarPoint": "SVO word order"
}
```

**User Response Structure:**
```json
{
  "orderedWords": ["w1", "w2", "w3"]
}
```

**Validation Logic:**
```kotlin
fun validateSentenceScramble(userOrder: List<String>, correctOrder: List<String>): ValidationResult {
    val isCorrect = userOrder == correctOrder

    return ValidationResult(
        isCorrect = isCorrect,
        score = if (isCorrect) 100 else 0,
        feedback = if (isCorrect) {
            "Perfect! Correct word order."
        } else {
            "Incorrect. The correct order is: ${correctOrder.joinToString(" ")}"
        }
    )
}
```

---

### 4. Fill-in-the-Blank (Verb Conjugation)

**Exercise Type:** `fill_in_blank_conjugation`
**Category:** Grammar & Structure
**Interaction:** Select from dropdown or type

**Content Structure (JSONB):**
```json
{
  "sentence": "Je ____ français.",
  "blankIndex": 1,
  "verb": "parler",
  "subject": "je",
  "tense": "present",
  "correctAnswer": "parle",
  "options": ["parle", "parles", "parlons", "parlent"],  // For dropdown version
  "translation": "I speak French.",
  "grammarExplanation": "With 'je', use the first person singular: parle"
}
```

**User Response Structure (Dropdown):**
```json
{
  "answer": "parle"
}
```

**Validation Logic:**
```kotlin
fun validateFillInBlank(userAnswer: String, correctAnswer: String): ValidationResult {
    val isCorrect = userAnswer.trim().equals(correctAnswer, ignoreCase = true)

    return ValidationResult(
        isCorrect = isCorrect,
        score = if (isCorrect) 100 else 0,
        feedback = if (isCorrect) {
            "Correct conjugation!"
        } else {
            "The correct answer is '$correctAnswer'. ${content["grammarExplanation"]}"
        }
    )
}
```

---

### 5. Listening Comprehension (Audio → Text)

**Exercise Type:** `listening_select`
**Category:** Listening Comprehension
**Interaction:** Select

**Content Structure (JSONB):**
```json
{
  "audioUrl": "https://.../bonjour.mp3",
  "audioText": "Bonjour, comment ça va?",
  "question": "What did you hear?",
  "options": [
    { "id": "a", "text": "Bonjour, comment ça va?", "isCorrect": true },
    { "id": "b", "text": "Bonsoir, comment allez-vous?", "isCorrect": false },
    { "id": "c", "text": "Salut, ça va bien?", "isCorrect": false }
  ],
  "playCount": 0,  // Track how many times user can play
  "hint": "Listen for the greeting and question."
}
```

---

### 6. Cloze Reading (Context-Based Fill-in)

**Exercise Type:** `cloze_reading`
**Category:** Reading Comprehension
**Interaction:** Select or type

**Content Structure (JSONB):**
```json
{
  "passage": "Marie entre dans un petit café. Elle s'assoit près de la __1__. Le __2__ arrive avec un sourire.",
  "blanks": [
    {
      "id": 1,
      "correctAnswer": "fenêtre",
      "options": ["fenêtre", "porte", "table", "chaise"],
      "hint": "Where might she sit to see outside?"
    },
    {
      "id": 2,
      "correctAnswer": "serveur",
      "options": ["serveur", "client", "ami", "professeur"],
      "hint": "Who works at a café?"
    }
  ],
  "translation": "Marie enters a small café. She sits near the window. The waiter arrives with a smile."
}
```

---

## Backend Architecture

### Service Layer

```kotlin
// Exercise Service
@Service
class ExerciseService(
    private val exerciseRepository: ExerciseRepository,
    private val exerciseAttemptRepository: UserExerciseAttemptRepository,
    private val validationService: ExerciseValidationService
) {
    fun getExercisesByModule(languageCode: String, moduleNumber: Int): List<ExerciseDto>

    fun getExerciseById(id: Long): ExerciseDto

    fun submitExerciseAttempt(
        userId: Long,
        exerciseId: Long,
        userResponses: JsonNode
    ): AttemptResult

    fun getUserProgress(userId: Long, exerciseId: Long): List<UserExerciseAttempt>

    fun getCollectionProgress(userId: Long, collectionId: Long): CollectionProgress
}

// Exercise Validation Service
@Service
class ExerciseValidationService {
    fun validate(exerciseType: String, content: JsonNode, userResponse: JsonNode): ValidationResult {
        return when (exerciseType) {
            "multiple_choice" -> validateMultipleChoice(content, userResponse)
            "matching" -> validateMatching(content, userResponse)
            "sentence_scramble" -> validateSentenceScramble(content, userResponse)
            "fill_in_blank" -> validateFillInBlank(content, userResponse)
            "listening_select" -> validateListeningSelect(content, userResponse)
            else -> throw UnsupportedExerciseTypeException(exerciseType)
        }
    }

    private fun validateMultipleChoice(content: JsonNode, userResponse: JsonNode): ValidationResult
    private fun validateMatching(content: JsonNode, userResponse: JsonNode): ValidationResult
    // ... other validators
}

// Exercise Generation Service (for AI-generated exercises)
@Service
class ExerciseGenerationService(
    private val vocabularyService: VocabularyService,
    private val geminiService: GeminiService,
    private val ttsService: TtsService
) {
    fun generateMultipleChoice(
        word: Word,
        targetLanguage: String,
        nativeLanguage: String
    ): Exercise

    fun generateSentenceScramble(
        example: Example,
        languageCode: String
    ): Exercise

    fun generateClozeReading(
        text: ReadingText,
        targetWords: List<Word>
    ): Exercise
}
```

### API Endpoints

```kotlin
@RestController
@RequestMapping("/api/exercises")
class ExerciseController(
    private val exerciseService: ExerciseService
) {

    // Browse exercises
    @GetMapping
    fun getExercises(
        @RequestParam languageCode: String,
        @RequestParam(required = false) module: Int?,
        @RequestParam(required = false) topic: String?,
        @RequestParam(required = false) type: String?
    ): List<ExerciseSummaryDto>

    // Get exercise details
    @GetMapping("/{id}")
    fun getExercise(@PathVariable id: Long): ExerciseDto

    // Submit attempt
    @PostMapping("/{id}/attempt")
    fun submitAttempt(
        @PathVariable id: Long,
        @RequestBody request: SubmitAttemptRequest
    ): AttemptResultDto

    // Get user progress
    @GetMapping("/{id}/progress")
    fun getProgress(@PathVariable id: Long): UserProgressDto

    // Collections
    @GetMapping("/collections")
    fun getCollections(
        @RequestParam languageCode: String,
        @RequestParam(required = false) module: Int?
    ): List<CollectionSummaryDto>

    @GetMapping("/collections/{id}")
    fun getCollection(@PathVariable id: Long): CollectionDto

    @GetMapping("/collections/{id}/progress")
    fun getCollectionProgress(@PathVariable id: Long): CollectionProgressDto
}
```

---

## Frontend Architecture

### Component Structure

```
src/components/exercises/
├── ExerciseShell.vue              # Container with timer, progress, hints
├── ExerciseRenderer.vue           # Dynamic component renderer by type
│
├── types/
│   ├── MultipleChoiceExercise.vue
│   ├── MatchingExercise.vue
│   ├── SentenceScrambleExercise.vue
│   ├── FillInBlankExercise.vue
│   ├── ListeningExercise.vue
│   └── ClozeReadingExercise.vue
│
├── common/
│   ├── ExerciseQuestion.vue       # Question display with audio button
│   ├── ExerciseOptions.vue        # Multiple choice options grid
│   ├── DraggableWord.vue          # For sentence scramble
│   ├── MatchingGrid.vue           # For matching exercises
│   ├── FeedbackPanel.vue          # Show correct/incorrect with explanation
│   └── HintButton.vue             # Expandable hint display
│
└── collections/
    ├── CollectionBrowser.vue      # Browse exercise collections
    ├── CollectionCard.vue         # Collection preview
    └── CollectionProgress.vue     # Track completion
```

### Vue Composables

```typescript
// useExercise.ts
export function useExercise() {
  const currentExercise = ref<Exercise | null>(null)
  const userResponse = ref<any>({})
  const showFeedback = ref(false)
  const attemptResult = ref<AttemptResult | null>(null)

  async function loadExercise(id: number) {
    currentExercise.value = await api.getExercise(id)
    resetState()
  }

  async function submitAnswer() {
    showFeedback.value = true
    attemptResult.value = await api.submitAttempt(
      currentExercise.value!.id,
      userResponse.value
    )
  }

  function resetState() {
    userResponse.value = {}
    showFeedback.value = false
    attemptResult.value = null
  }

  return {
    currentExercise,
    userResponse,
    showFeedback,
    attemptResult,
    loadExercise,
    submitAnswer,
    resetState
  }
}

// useExerciseCollection.ts
export function useExerciseCollection() {
  const collection = ref<ExerciseCollection | null>(null)
  const currentExerciseIndex = ref(0)
  const completedExercises = ref<Set<number>>(new Set())

  const progress = computed(() => {
    if (!collection.value) return 0
    return (completedExercises.value.size / collection.value.exercises.length) * 100
  })

  function nextExercise() {
    if (currentExerciseIndex.value < collection.value!.exercises.length - 1) {
      currentExerciseIndex.value++
    }
  }

  function markComplete(exerciseId: number) {
    completedExercises.value.add(exerciseId)
  }

  return {
    collection,
    currentExerciseIndex,
    completedExercises,
    progress,
    nextExercise,
    markComplete
  }
}
```

### Example Component: MultipleChoiceExercise.vue

```vue
<template>
  <div class="multiple-choice-exercise">
    <ExerciseQuestion
      :question="exercise.content.question"
      :audioUrl="exercise.content.question.audioUrl"
    />

    <div class="options-grid">
      <Button
        v-for="option in exercise.content.options"
        :key="option.id"
        :label="option.text"
        :class="getOptionClass(option.id)"
        :disabled="showFeedback"
        @click="selectOption(option.id)"
      />
    </div>

    <HintButton v-if="!showFeedback" :hint="exercise.content.hint" />

    <FeedbackPanel
      v-if="showFeedback"
      :isCorrect="attemptResult.isCorrect"
      :explanation="exercise.content.explanation"
    />

    <div class="actions">
      <Button
        v-if="!showFeedback"
        label="Check Answer"
        :disabled="!selectedOption"
        @click="submitAnswer"
      />
      <Button
        v-else
        label="Next Exercise"
        @click="$emit('next')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Exercise, AttemptResult } from '@/types/exercise'

const props = defineProps<{
  exercise: Exercise
  attemptResult: AttemptResult | null
  showFeedback: boolean
}>()

const emit = defineEmits<{
  answer: [answer: string]
  next: []
}>()

const selectedOption = ref<string | null>(null)

function selectOption(optionId: string) {
  selectedOption.value = optionId
}

function submitAnswer() {
  if (selectedOption.value) {
    emit('answer', selectedOption.value)
  }
}

function getOptionClass(optionId: string) {
  if (!props.showFeedback) {
    return { selected: selectedOption.value === optionId }
  }

  const isCorrect = props.exercise.content.options.find(
    o => o.id === optionId
  )?.isCorrect

  const isSelected = selectedOption.value === optionId

  return {
    correct: isCorrect && isSelected,
    incorrect: !isCorrect && isSelected,
    'show-correct': isCorrect && !isSelected
  }
}
</script>
```

---

## Implementation Phases

### Phase 1: Infrastructure (Week 1-2)
**Priority: Critical**

- [ ] Create database migrations for exercise tables
- [ ] Create domain models (Exercise, ExerciseType, UserExerciseAttempt)
- [ ] Create repositories
- [ ] Implement ExerciseValidationService
- [ ] Create base API endpoints
- [ ] Create ExerciseShell component
- [ ] Create ExerciseRenderer component

**Deliverable:** Infrastructure ready to support any exercise type

---

### Phase 2: First Exercise Type - Multiple Choice (Week 2-3)
**Priority: High**

- [ ] Implement multiple choice validation logic
- [ ] Create MultipleChoiceExercise.vue component
- [ ] Create 10 sample multiple choice exercises (French A1 Module 1)
- [ ] Test full flow: load → answer → validate → save
- [ ] Add to navigation

**Deliverable:** One fully working exercise type

---

### Phase 3: Second Exercise Type - Matching (Week 3-4)
**Priority: High**

- [ ] Implement matching validation logic
- [ ] Create MatchingExercise.vue with drag-and-drop
- [ ] Create 10 matching exercises (French A1 Module 2)
- [ ] Add image support
- [ ] Test on mobile (touch interactions)

**Deliverable:** Two exercise types working

---

### Phase 4: Exercise Collections (Week 4-5)
**Priority: Medium**

- [ ] Implement collection browsing
- [ ] Create CollectionBrowser.vue
- [ ] Create first collection: "French A1 Module 1 - Greetings"
- [ ] Add collection progress tracking
- [ ] Add "unlock" system (complete Collection 1 to unlock Collection 2)

**Deliverable:** Users can work through curated exercise collections

---

### Phase 5: Additional Exercise Types (Week 5-8)
**Priority: Medium**

Implement in order:
1. Sentence Scramble (Week 5)
2. Fill-in-the-Blank (Week 6)
3. Listening Exercises (Week 7)
4. Cloze Reading (Week 8)

Each follows same pattern:
- Backend validation
- Frontend component
- 5-10 sample exercises
- Test & polish

**Deliverable:** 6 distinct exercise types

---

### Phase 6: AI Exercise Generation (Week 9-10)
**Priority: Low (Enhancement)**

- [ ] Implement ExerciseGenerationService
- [ ] Create admin UI for generating exercises
- [ ] Generate exercises from existing vocabulary
- [ ] Generate exercises from reading texts
- [ ] Add manual review/approval flow

**Deliverable:** Ability to rapidly create new exercises using AI

---

## AI Integration Strategy

### Approach 1: Manual Curation (MVP)
**Timeline:** Immediate
**Effort:** High upfront, low ongoing

- Manually create 50-100 exercises per module
- Use templates and Wiktionary data
- Quality guaranteed
- Slow to scale

### Approach 2: AI-Assisted Generation (Post-MVP)
**Timeline:** Phase 6
**Effort:** Medium upfront, low ongoing

**For Multiple Choice:**
```
Prompt to Gemini:
"Create a multiple choice vocabulary exercise for A1 French learners.

Word: {lemma}
Part of Speech: {partOfSpeech}
Definition: {definition}

Generate 3 plausible but incorrect distractors that are:
- Same part of speech
- Similar difficulty level
- Not obviously wrong
- Semantically related but distinct

Format as JSON with question, correct answer, and 3 distractors."
```

**For Sentence Scramble:**
```
Prompt:
"Create a sentence scramble exercise for A1 French learners using this sentence:
'{example.sentenceText}'

Requirements:
- Ensure sentence tests a specific grammar point
- Provide translation
- Add a hint about word order
- Include the grammar concept being tested"
```

**For Cloze Reading:**
```
Prompt:
"Convert this French A1 text into a cloze reading exercise:
'{text.content}'

Requirements:
- Remove 4-6 key vocabulary words
- Provide 3 options per blank (1 correct, 2 distractors)
- Distractors should be grammatically plausible
- Focus on nouns, verbs, and key vocabulary"
```

### Quality Assurance for AI-Generated Content

1. **Human Review Required**: All AI-generated exercises reviewed before publishing
2. **User Feedback Loop**: "Report Issue" button on every exercise
3. **A/B Testing**: Test AI vs manual exercises for effectiveness
4. **Continuous Improvement**: Use user performance data to refine prompts

---

## Success Metrics

### User Engagement
- **Exercise Completion Rate**: % of started exercises completed
- **Daily Exercise Users**: Users doing at least 1 exercise per day
- **Average Session Length**: Time spent on exercises per session

### Learning Effectiveness
- **First-Attempt Accuracy**: % correct on first try
- **Improvement Over Time**: Accuracy increase after repeated attempts
- **Retention**: Performance on review exercises after 1 week

### Content Quality
- **User Satisfaction**: Rating of exercise quality (1-5 stars)
- **Report Rate**: % of exercises flagged as problematic
- **Completion Time**: Average time per exercise type

---

## Future Enhancements

### Advanced Exercise Types
1. **Speaking Exercises**: Record and compare pronunciation
2. **Dialogue Role-Play**: Interactive conversation practice
3. **Writing Exercises**: Free-form sentence construction with AI evaluation
4. **Adaptive Difficulty**: Exercises adjust based on user performance

### Gamification
1. **Achievements**: Badges for completing collections, streaks, accuracy
2. **Leaderboards**: Compare progress with friends (opt-in)
3. **Daily Challenges**: Special time-limited exercises
4. **Unlockables**: New exercise types, themes, or features

### Social Features
1. **Exercise Sharing**: Users create and share custom exercises
2. **Collaborative Learning**: Compete or cooperate with friends
3. **Teacher Dashboard**: Create exercises for students

---

## Open Questions

1. **Exercise Difficulty Calibration**: How to accurately rate difficulty 1-5?
2. **Partial Credit**: Should we give partial credit for partially correct answers?
3. **Time Limits**: Should exercises have optional time challenges?
4. **Mobile vs Desktop**: Different UX patterns needed?
5. **Offline Support**: Should exercises work offline?

---

**Document Status:** 📋 Ready for Implementation Planning
**Next Steps:** Review → Approve → Begin Phase 1 Implementation
**Author:** Claude Code
**Last Updated:** November 2025
