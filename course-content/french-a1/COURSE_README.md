# French A1 Course Content

This directory contains the course structure and content for the French A1 beginner course.

## Files Structure

```
french-a1/
├── course.json          # Course metadata
├── module_1.json        # Module 1: Getting Started
├── module_2.json        # Module 2: (to be created)
├── module_3.json        # Module 3: (to be created)
└── ...
```

## Importing Content

### Step 1: Import Course Metadata

```bash
curl -X POST http://localhost:8080/api/admin/courses/import \
  -H "Content-Type: application/json" \
  -d @course.json
```

### Step 2: Import Modules

```bash
# Import Module 1
curl -X POST "http://localhost:8080/api/admin/courses/french-a1/modules/import?generateAudio=true" \
  -H "Content-Type: application/json" \
  -d @module_1.json

# Import Module 2
curl -X POST "http://localhost:8080/api/admin/courses/french-a1/modules/import?generateAudio=true" \
  -H "Content-Type: application/json" \
  -d @module_2.json
```

## JSON Structure Reference

### course.json
```json
{
  "slug": "french-a1",                    // URL-friendly identifier
  "name": "French A1 - Complete Beginner", // Display name
  "languageCode": "fr",                    // ISO language code
  "cefrLevel": "A1",                       // CEFR level
  "description": "Course description...",
  "objectives": ["Objective 1", "..."],    // Learning objectives
  "estimatedHours": 40                     // Total course duration
}
```

### module_X.json
```json
{
  "moduleNumber": 1,
  "title": "Module Title",
  "theme": "Theme Name",
  "description": "Module description...",
  "objectives": ["Objective 1", "..."],
  "estimatedMinutes": 120,
  "episodes": [
    {
      "episodeNumber": 1,
      "type": "STORY",  // or DIALOGUE, ARTICLE, AUDIO_LESSON
      "title": "Episode Title",
      "content": "Episode content text...",
      "generateAudio": false,
      "transcript": "Audio transcript...",  // For AUDIO_LESSON type
      "estimatedMinutes": 10,
      "contentItems": [
        {
          "orderIndex": 1,
          "type": "EXERCISE",  // or GRAMMAR_RULE
          "isRequired": true,
          "exercise": {
            "type": "multiple_choice",  // or fill_in_blank, sentence_scramble, etc.
            "title": "Exercise Title",
            "instructions": "Instructions...",
            "content": { /* exercise-specific structure */ },
            "estimatedDurationSeconds": 30,
            "pointsValue": 10,
            "difficultyRating": 1.0
          }
        }
      ]
    }
  ]
}
```

## Episode Types

- **STORY**: Narrative text (like "Marie Arrives in Paris")
- **DIALOGUE**: Conversation between characters (like "At the Hotel")
- **ARTICLE**: Educational article about culture, grammar, etc.
- **AUDIO_LESSON**: Audio-first content with transcript

## Exercise Types

All exercise types are demonstrated in Module 1 and Module 2.

### 1. Multiple Choice
Used for comprehension questions with 4 options.

```json
{
  "type": "multiple_choice",
  "content": {
    "question": { "type": "text", "content": "Question text" },
    "options": [
      { "id": "a", "text": "Option A", "isCorrect": true },
      { "id": "b", "text": "Option B", "isCorrect": false }
    ],
    "explanation": "Why this answer is correct...",
    "hint": "Helpful hint..."
  }
}
```

**Example in:** Module 1 Episode 1, Module 2 Episode 1

### 2. Fill in the Blank

Supports both single-blank and multi-blank formats.

**Single blank (with dropdown):**
```json
{
  "type": "fill_in_blank",
  "content": {
    "sentence": "Bonjour, je _____ Marie.",
    "correctAnswer": "m'appelle",
    "options": ["m'appelle", "suis", "parle", "vais"],
    "translation": "Hello, my name is Marie.",
    "hint": "Hint..."
  }
}
```

**Multi-blank (each blank has its own dropdown):**
```json
{
  "type": "fill_in_blank",
  "content": {
    "text": "J'ai une ___ au nom de Dubois. Voici votre ___.",
    "blanks": [
      {
        "correctAnswer": "réservation",
        "acceptableAnswers": ["réservation"],
        "options": ["réservation", "clé", "chambre", "carte"]
      },
      {
        "correctAnswer": "clé",
        "acceptableAnswers": ["clé", "cle"],
        "options": ["clé", "carte", "réservation", "passeport"]
      }
    ],
    "translation": "I have a reservation in the name of Dubois. Here is your key.",
    "hint": "Hint..."
  }
}
```

**Example in:** Module 1 Episode 1 (single), Module 2 Episode 1 & 3 (multi)

### 3. Sentence Scramble
Drag and drop words into correct order.

```json
{
  "type": "sentence_scramble",
  "content": {
    "words": ["Je", "m'appelle", "Marie"],
    "correctOrder": ["Je", "m'appelle", "Marie"],
    "translation": "My name is Marie",
    "explanation": "Explanation...",
    "hint": "Hint..."
  }
}
```

**Example in:** Module 1 Episode 2

### 4. Matching
Match items from left column to right column.

```json
{
  "type": "matching",
  "content": {
    "pairs": [
      { "left": "Bonjour", "right": "Hello" },
      { "left": "Merci", "right": "Thank you" }
    ],
    "hint": "Hint..."
  }
}
```

**Example in:** Module 2 Episode 1

### 5. Listening
Audio comprehension with multiple choice questions.

```json
{
  "type": "listening",
  "generateAudio": true,
  "content": {
    "audioUrl": "placeholder_will_be_generated",
    "transcript": "Le parking est gratuit pour nos clients.",
    "question": "Où se trouve le parking ?",
    "questionType": "multiple_choice",
    "options": [
      { "id": "a", "text": "Devant l'hôtel" },
      { "id": "b", "text": "Derrière l'hôtel" }
    ],
    "correctAnswer": "b",
    "explanation": "Explanation...",
    "hint": "Hint..."
  }
}
```

**Example in:** Module 2 Episode 2

### 6. Cloze Reading
Reading comprehension with multiple blanks to type in.

```json
{
  "type": "cloze_reading",
  "content": {
    "text": "Marc arrive à l'hôtel et parle avec le {blank1}. Il demande pour le {blank2}.",
    "blanks": [
      { "id": "blank1", "correctAnswer": "réceptionniste" },
      { "id": "blank2", "correctAnswer": ["parking", "stationnement"] }
    ],
    "hint": "Hint..."
  }
}
```

**Example in:** Module 2 Episode 2

## Generating Content with AI

You can use Claude or ChatGPT to generate module content. Here's a sample prompt:

```
I'm creating a French A1 course. Please generate a complete module JSON file following this structure:

Module Number: 2
Theme: "At the Restaurant"
Should include:
- 2 episodes (1 story, 1 dialogue)
- Each episode should have 3-4 exercises
- Mix exercise types (multiple choice, fill in blank, sentence scramble)
- Content should be engaging and practical for beginners
- Include vocabulary: food, drinks, ordering, paying

Please use the exact JSON structure from module_1.json and ensure all required fields are present.
```

## Audio Generation

When `generateAudio: true` is set:
- **For DIALOGUE episodes**: The full episode content will be converted to audio
- **For LISTENING exercises**: The transcript will be converted to audio

Audio is generated using Google's Gemini TTS API and automatically uploaded to storage.

### Voice Selection Guide

Gemini TTS provides 30+ voices with different characteristics. Here are recommendations for different character types:

#### Female-Sounding Voices (Higher/Middle Pitch)

**Young & Energetic:**
- **Puck** (Upbeat, Middle pitch) - Perfect for friendly travelers, students
- **Laomedeia** (Upbeat, Higher pitch) - Enthusiastic, cheerful characters
- **Zephyr** (Bright, Higher pitch) - Energetic, lively personalities
- **Leda** (Youthful, Higher pitch) - Young adults, teenagers

**Professional & Clear:**
- **Kore** (Firm, Middle pitch) - Receptionists, teachers, professionals
- **Erinome** (Clear, Middle pitch) - Customer service, announcements
- **Autonoe** (Bright, Middle pitch) - Friendly professionals

**Gentle & Warm:**
- **Achernar** (Soft, Higher pitch) - Gentle, kind characters
- **Vindemiatrix** (Gentle, Middle pitch) - Caring, patient personalities
- **Despina** (Smooth, Middle pitch) - Polished professionals
- **Sulafat** (Warm, Middle pitch) - Friendly, approachable characters

**Relaxed & Easy-going:**
- **Aoede** (Breezy, Middle pitch) - Casual conversations
- **Callirrhoe** (Easy-going, Middle pitch) - Laid-back characters

**Knowledgeable:**
- **Rasalgethi** (Informative, Middle pitch) - Guides, instructors
- **Sadaltager** (Knowledgeable, Middle pitch) - Experts, teachers

**Distinctive:**
- **Pulcherrima** (Forward, Middle pitch) - Assertive, confident
- **Gacrux** (Mature, Middle pitch) - Older, experienced characters

#### Male-Sounding Voices (Lower/Lower-Middle Pitch)

**Professional & Smooth:**
- **Algieba** (Smooth, Lower pitch) - Hotel staff, customer service
- **Charon** (Informative, Lower pitch) - Guides, presenters
- **Enceladus** (Breathy, Lower pitch) - Calm, composed professionals

**Friendly & Approachable:**
- **Achird** (Friendly, Lower middle pitch) - Helpful staff, guides
- **Umbriel** (Easy-going, Lower middle pitch) - Casual, relaxed
- **Zubenelgenubi** (Casual, Lower middle pitch) - Informal conversations
- **Iapetus** (Clear, Lower middle pitch) - Clear communicators

**Firm & Confident:**
- **Orus** (Firm, Lower middle pitch) - Authoritative figures
- **Alnilam** (Firm, Lower middle pitch) - Confident professionals
- **Schedar** (Even, Lower middle pitch) - Steady, reliable

**Energetic & Lively:**
- **Fenrir** (Excitable, Lower middle pitch) - Enthusiastic, animated
- **Sadachbia** (Lively, Lower pitch) - Energetic characters

**Distinctive:**
- **Algenib** (Gravelly, Lower pitch) - Older males, unique texture

#### Voice Pairing Tips

**Hotel Scenarios:**
- Receptionist: Kore, Algieba, Despina, Iapetus
- Guest (Female): Puck, Achernar, Callirrhoe
- Guest (Male): Enceladus, Achird, Umbriel

**Restaurant Scenarios:**
- Waiter/Waitress: Erinome, Despina, Achird, Umbriel
- Customer (Female): Laomedeia, Aoede, Vindemiatrix
- Customer (Male): Zubenelgenubi, Iapetus, Sadachbia

**Formal Settings:**
- Professional (Female): Kore, Rasalgethi, Gacrux
- Professional (Male): Algieba, Charon, Schedar

**Casual Conversations:**
- Young Friends (Female): Puck, Laomedeia, Autonoe, Leda
- Young Friends (Male): Fenrir, Zubenelgenubi, Sadachbia

**For Multi-Speaker Dialogues:**
- Contrast pitches (Higher + Lower middle, or Middle + Lower)
- Contrast characteristics (Upbeat + Calm, Firm + Gentle)
- Example good pairs:
  - Puck (Upbeat, Middle) + Algieba (Smooth, Lower)
  - Kore (Firm, Middle) + Enceladus (Breathy, Lower)
  - Laomedeia (Upbeat, Higher) + Achird (Friendly, Lower middle)

## Tips for Creating Content

1. **Keep it contextual**: Stories and dialogues should build on each other
2. **Progressive difficulty**: Start with simpler vocabulary and grammar
3. **Real-world scenarios**: Focus on practical, everyday situations
4. **Cultural notes**: Include cultural context where relevant
5. **Variety**: Mix different exercise types to keep learners engaged
6. **Immediate practice**: Add exercises right after introducing new concepts

## Module Ideas for French A1

- Module 1: Greetings & Introductions
- Module 2: At the Hotel
- Module 3: At the Restaurant
- Module 4: Shopping & Numbers
- Module 5: Asking for Directions
- Module 6: Daily Routines
- Module 7: Hobbies & Free Time
- Module 8: Family & Friends
- Module 9: Making Plans
- Module 10: Weather & Seasons
