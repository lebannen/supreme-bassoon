# Exercise Types Reference Guide

**Version:** 1.0
**Last Updated:** January 2025
**Status:** ✅ All 6 Exercise Types Implemented

---

## Overview

This document provides a comprehensive reference for all exercise types available in the Vocabee platform. Each exercise type is designed to target specific learning objectives and cognitive skills, following the CEFR framework for language acquisition.

### Exercise Type Philosophy

1. **Progressive Difficulty**: From recognition (easier) to production (harder)
2. **Multi-Skill Development**: Covering reading, listening, writing, and grammar
3. **Immediate Feedback**: Instant validation with educational explanations
4. **Flexible Content**: Supports various difficulty levels and topics

---

## Exercise Types Summary

| Type | Skill Focus | Difficulty | AI-Friendly | Use Cases |
|------|-------------|------------|-------------|-----------|
| Multiple Choice | Recognition, Reading | ⭐ Easy | ✅ Yes | Vocabulary, translation, comprehension |
| Fill in the Blank | Production, Grammar | ⭐⭐ Medium | ✅ Yes | Grammar conjugation, vocabulary recall |
| Sentence Scramble | Grammar, Syntax | ⭐⭐ Medium | ✅ Yes | Word order, sentence structure |
| Matching | Recognition, Association | ⭐ Easy | ✅ Yes | Vocabulary pairs, definitions, translations |
| Listening Comprehension | Listening, Comprehension | ⭐⭐⭐ Hard | ⚠️ Partial | Audio comprehension, pronunciation |
| Cloze Reading | Reading, Context | ⭐⭐⭐ Hard | ✅ Yes | Context-based grammar, reading comprehension |

---

## 1. Multiple Choice

### Description
Students select the correct answer from 2-4 options. Ideal for vocabulary recognition, translation practice, and concept comprehension.

### Content Structure
```json
{
  "question": {
    "type": "text",
    "content": "What is the French word for 'hello'?"
  },
  "options": [
    {"id": "a", "text": "Bonjour", "isCorrect": true},
    {"id": "b", "text": "Bonsoir", "isCorrect": false},
    {"id": "c", "text": "Au revoir", "isCorrect": false},
    {"id": "d", "text": "Salut", "isCorrect": false}
  ],
  "explanation": "Bonjour is the standard greeting meaning 'hello' or 'good day'.",
  "hint": "Think about greetings used during the day"
}
```

### User Response Format
```json
{
  "selectedOption": "a"
}
```

### Learning Objectives
- Vocabulary recognition
- Translation accuracy
- Conceptual understanding
- Quick recall

### Best For
- A1-A2 vocabulary building
- Quick assessments
- Translation practice
- Concept introduction

---

## 2. Fill in the Blank

### Description
Students type the missing word in a sentence. Tests production skills and reinforces grammar patterns.

### Content Structure
```json
{
  "sentence": "Je _____ au marché.",
  "correctAnswer": "vais",
  "grammarExplanation": "Use 'vais' (from aller) for 'I go/am going'",
  "translation": "I am going to the market.",
  "hint": "Use the verb 'aller' (to go) in first person"
}
```

### User Response Format
```json
{
  "answer": "vais"
}
```

### Validation
- Case-insensitive matching
- Trimmed whitespace
- Exact match required

### Learning Objectives
- Active vocabulary recall
- Grammar pattern reinforcement
- Spelling practice
- Conjugation accuracy

### Best For
- Grammar conjugation practice
- Verb tense exercises
- Vocabulary production
- A1-B1 grammar reinforcement

---

## 3. Sentence Scramble

### Description
Students rearrange scrambled words to form a grammatically correct sentence. Develops understanding of syntax and word order.

### Content Structure
```json
{
  "words": ["Je", "vais", "au", "cinéma"],
  "sentence": "Je vais au cinéma",
  "translation": "I am going to the cinema",
  "grammarExplanation": "French word order: Subject + Verb + Preposition + Object",
  "hint": "Start with the subject pronoun"
}
```

### User Response Format
```json
{
  "orderedWords": ["Je", "vais", "au", "cinéma"]
}
```

### Validation
- Array order comparison
- Exact sequence match

### Learning Objectives
- Sentence structure understanding
- Word order mastery
- Grammar pattern recognition
- Syntax awareness

### Best For
- Word order practice
- Grammar structure exercises
- A1-B1 syntax building
- Question formation practice

---

## 4. Matching

### Description
Students match items from two columns by clicking pairs. Excellent for vocabulary learning and concept association.

### Content Structure
```json
{
  "pairs": [
    {"left": "le chat", "right": "the cat"},
    {"left": "le chien", "right": "the dog"},
    {"left": "la maison", "right": "the house"}
  ],
  "hint": "Pay attention to the gender articles (le/la)"
}
```

### User Response Format
```json
{
  "matches": {
    "le chat": "the cat",
    "le chien": "the dog",
    "la maison": "the house"
  }
}
```

### Validation
- Each pair must match correctly
- Partial credit for partial correctness
- Score = (correct matches / total pairs) × 100

### Learning Objectives
- Vocabulary association
- Translation pairs
- Concept matching
- Visual learning reinforcement

### Best For
- Vocabulary translation practice
- Definition matching
- Synonym/antonym pairs
- A1-A2 vocabulary building
- Gender article practice

---

## 5. Listening Comprehension

### Description
Students listen to audio and answer questions either via multiple choice or text input. Develops listening skills and pronunciation awareness.

### Content Structure

**Multiple Choice Variant:**
```json
{
  "audioUrl": "/audio/fr/greetings/bonjour.mp3",
  "question": "What greeting did you hear?",
  "questionType": "multiple_choice",
  "options": [
    {"id": "a", "text": "Bonjour"},
    {"id": "b", "text": "Bonsoir"},
    {"id": "c", "text": "Au revoir"}
  ],
  "correctAnswer": "a",
  "transcript": "Bonjour",
  "explanation": "The speaker said 'Bonjour', which means 'Hello'",
  "hint": "Listen for the time of day in the greeting"
}
```

**Text Input Variant:**
```json
{
  "audioUrl": "/audio/fr/numbers/cinq.mp3",
  "question": "What number did you hear?",
  "questionType": "text_input",
  "correctAnswer": ["cinq", "5"],
  "transcript": "Le nombre est cinq",
  "explanation": "The number is 'cinq' (five)",
  "hint": "Listen for the number word"
}
```

### User Response Format
```json
// Multiple choice:
{"selectedOption": "a"}

// Text input:
{"answer": "cinq"}
```

### Validation
- Multiple choice: exact match
- Text input: case-insensitive, accepts multiple correct answers

### Learning Objectives
- Listening comprehension
- Phonetic recognition
- Pronunciation awareness
- Audio processing skills

### Best For
- Pronunciation practice
- Number dictation
- Greeting recognition
- A1-B1 listening exercises
- Minimal pairs practice

### Technical Requirements
- Audio files in MP3 format
- HTML5 audio player support
- Recommended: 16-24kHz sample rate
- File size: typically 10-30KB per exercise

---

## 6. Cloze Reading

### Description
Students fill in multiple blanks within a passage. Tests reading comprehension and contextual understanding of grammar and vocabulary.

### Content Structure
```json
{
  "text": "Je ___1___ au marché. J'___2___ des pommes et du pain. C'___3___ délicieux.",
  "blanks": [
    {"id": "1", "correctAnswer": "vais"},
    {"id": "2", "correctAnswer": ["achète", "prends"]},
    {"id": "3", "correctAnswer": "est"}
  ],
  "hint": "Think about common verbs for daily activities"
}
```

### User Response Format
```json
{
  "answers": {
    "1": "vais",
    "2": "achète",
    "3": "est"
  }
}
```

### Validation
- Each blank validated independently
- Case-insensitive matching
- Supports multiple acceptable answers per blank
- Partial scoring: (correct blanks / total blanks) × 100

### Learning Objectives
- Contextual grammar application
- Reading comprehension
- Multiple word recall
- Cohesive text understanding

### Best For
- Grammar in context
- Story-based exercises
- A2-B2 reading comprehension
- Verb conjugation practice
- Paragraph-level exercises

### Text Formatting
- Use `___1___`, `___2___`, etc. for blanks
- Blanks are numbered sequentially
- Text can be any length (recommend 2-4 sentences)

---

## Exercise Selection Guidelines

### By CEFR Level

**A1 (Beginner):**
- ✅ Multiple Choice (simple vocabulary)
- ✅ Fill in the Blank (basic verbs)
- ✅ Matching (vocabulary pairs)
- ✅ Listening (simple words/phrases)
- ⚠️ Sentence Scramble (short sentences only)
- ❌ Cloze Reading (too complex)

**A2 (Elementary):**
- ✅ All types
- Focus: Multiple Choice, Fill in the Blank, Sentence Scramble

**B1 (Intermediate):**
- ✅ All types
- Focus: Cloze Reading, Sentence Scramble, Listening

**B2+ (Upper Intermediate+):**
- ✅ All types
- Focus: Complex Cloze Reading, multi-step exercises

### By Learning Goal

**Vocabulary Building:**
1. Multiple Choice (recognition)
2. Matching (association)
3. Fill in the Blank (production)

**Grammar Practice:**
1. Fill in the Blank (conjugation)
2. Sentence Scramble (structure)
3. Cloze Reading (context)

**Listening Skills:**
1. Listening Comprehension (all variants)
2. Multiple Choice (audio-based questions)

**Reading Comprehension:**
1. Cloze Reading (context understanding)
2. Multiple Choice (comprehension questions)

### By Time Available

**Quick (1-2 minutes):**
- Multiple Choice
- Fill in the Blank
- Matching (small sets)

**Medium (2-4 minutes):**
- Sentence Scramble
- Listening Comprehension
- Matching (large sets)

**Extended (4-6 minutes):**
- Cloze Reading
- Multiple listening exercises

---

## Content Generation Tips

### For AI Generation

**High AI Success Rate:**
- Multiple Choice: Generate distractors automatically
- Fill in the Blank: Use grammar templates
- Sentence Scramble: Create from valid sentences
- Matching: Generate from vocabulary lists
- Cloze Reading: Use paragraph templates

**Requires Manual Curation:**
- Listening Comprehension: Needs audio file generation/recording

### Quality Guidelines

1. **Clarity**: Instructions should be unambiguous
2. **Difficulty**: Match CEFR level appropriately
3. **Context**: Provide relevant cultural/grammatical context
4. **Feedback**: Include educational explanations
5. **Hints**: Offer strategic guidance, not answers

### Common Pitfalls to Avoid

- ❌ Multiple correct answers without specifying
- ❌ Culturally inappropriate content
- ❌ Grammar that exceeds target level
- ❌ Overly long sentences for beginners
- ❌ Ambiguous or trick questions
- ❌ Missing context in cloze exercises

---

## Technical Implementation

### Backend Validation

All exercise types use a unified validation service:

```kotlin
class ExerciseValidationService {
    fun validate(
        exerciseType: String,
        content: JsonNode,
        userResponses: JsonNode
    ): ValidationResult
}
```

### Database Storage

Exercises use JSONB for flexible content storage:

```sql
CREATE TABLE exercises (
    id BIGSERIAL PRIMARY KEY,
    exercise_type_id BIGINT NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    content JSONB NOT NULL,
    -- ... other fields
);
```

### Frontend Components

Each type has a dedicated Vue component:
- `MultipleChoiceExercise.vue`
- `FillInBlankExercise.vue`
- `SentenceScrambleExercise.vue`
- `MatchingExercise.vue`
- `ListeningExercise.vue`
- `ClozeReadingExercise.vue`

---

## Sample Exercise Counts

**Currently in Database:**
- Multiple Choice: 5 exercises
- Fill in the Blank: 6 exercises
- Sentence Scramble: 6 exercises
- Matching: 6 exercises
- Listening Comprehension: 6 exercises (with audio files)
- Cloze Reading: 6 exercises

**Total: 35 sample exercises** covering French A1 topics

---

## Future Enhancements

### Planned Features
- Image-based multiple choice
- Audio recording for speaking practice
- Multi-step composite exercises
- Adaptive difficulty adjustment
- Timed challenges

### AI Integration
- Automatic exercise generation from course plan
- Personalized difficulty adjustment
- Content variation for repeated practice
- Error pattern analysis

---

## Related Documentation

- [Exercise System Design](./exercise-system-design.md) - Technical architecture
- [Exercise Implementation Plan](./exercise-implementation-plan.md) - Development timeline
- [French A1 Course Plan](./french_a1_plan.md) - Curriculum structure
- [Roadmap](./roadmap.md) - Overall project timeline
