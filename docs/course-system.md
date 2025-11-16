# Structured Course System

**Status:** ✅ **COMPLETE** - Full course content management system
**Created:** January 2025
**Last Updated:** January 2025

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Data Model](#data-model)
4. [Content Import](#content-import)
5. [Audio Generation](#audio-generation)
6. [API Reference](#api-reference)
7. [Frontend Integration](#frontend-integration)
8. [Content Creation Guide](#content-creation-guide)

---

## Overview

The Structured Course System provides a comprehensive framework for creating, managing, and delivering language learning courses through a hierarchical content structure.

### Key Features

- **Hierarchical Content**: Courses → Modules → Episodes → Content Items
- **Multiple Episode Types**: STORY, DIALOGUE, ARTICLE, AUDIO_LESSON
- **Integrated Exercises**: All 6 exercise types can be embedded in episodes
- **Multi-Speaker Audio**: Automatic dialogue generation with Gemini TTS
- **JSON-Based Import**: Easy content creation and version control
- **Admin Interface**: Complete course and module management

### Content Hierarchy

```
Course (French A1)
├── Module 1 (Greetings & Introductions)
│   ├── Episode 1 (STORY: "Marie Arrives in Paris")
│   │   ├── Content Item 1 (Exercise: Multiple Choice)
│   │   └── Content Item 2 (Grammar Rule: Greetings)
│   └── Episode 2 (DIALOGUE: "At the Hotel")
│       ├── Content Item 1 (Exercise: Fill in Blank)
│       └── Content Item 2 (Exercise: Matching)
└── Module 2 (At the Hotel)
    ├── Episode 1 (DIALOGUE: "Checking In")
    ├── Episode 2 (DIALOGUE: "Asking for Information")
    └── Episode 3 (DIALOGUE: "Reporting a Problem")
```

---

## Architecture

### Database Schema

```sql
-- Courses table
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    cefr_level VARCHAR(10),
    description TEXT,
    objectives TEXT[],
    estimated_hours INT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Modules table
CREATE TABLE modules (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    module_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    theme VARCHAR(255),
    description TEXT,
    objectives TEXT[],
    estimated_minutes INT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(course_id, module_number)
);

-- Episodes table
CREATE TABLE episodes (
    id BIGSERIAL PRIMARY KEY,
    module_id BIGINT NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    episode_number INT NOT NULL,
    type VARCHAR(50) NOT NULL, -- STORY, DIALOGUE, ARTICLE, AUDIO_LESSON
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    audio_url VARCHAR(500),
    transcript TEXT,
    estimated_minutes INT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(module_id, episode_number)
);

-- Episode content items (exercises and grammar rules)
CREATE TABLE episode_content_items (
    id BIGSERIAL PRIMARY KEY,
    episode_id BIGINT NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    type VARCHAR(50) NOT NULL, -- EXERCISE, GRAMMAR_RULE
    is_required BOOLEAN DEFAULT true,
    exercise_id BIGINT REFERENCES exercises(id) ON DELETE CASCADE,
    grammar_rule_id BIGINT REFERENCES grammar_rules(id) ON DELETE CASCADE,
    UNIQUE(episode_id, order_index)
);

-- Grammar rules table
CREATE TABLE grammar_rules (
    id BIGSERIAL PRIMARY KEY,
    language_code VARCHAR(10) NOT NULL,
    title VARCHAR(255) NOT NULL,
    rule_text TEXT NOT NULL,
    examples TEXT[],
    difficulty_level VARCHAR(10),
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Backend Services

**CourseService**
- CRUD operations for courses, modules, episodes
- Content item management
- Progress tracking integration

**CourseImportService**
- JSON parsing and validation
- Automatic exercise creation
- Audio generation orchestration
- Transaction management

**AudioGenerationService**
- Single-speaker TTS for stories/articles
- Multi-speaker TTS for dialogues (up to 2 speakers)
- Voice configuration and style prompts
- File storage integration

---

## Data Model

### Course

```kotlin
@Entity
@Table(name = "courses")
data class Course(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val slug: String,

    val name: String,
    val languageCode: String,
    val cefrLevel: String? = null,
    val description: String? = null,

    @Column(columnDefinition = "TEXT[]")
    val objectives: Array<String>? = null,

    val estimatedHours: Int? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### Module

```kotlin
@Entity
@Table(name = "modules")
data class Module(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    val course: Course,

    val moduleNumber: Int,
    val title: String,
    val theme: String? = null,
    val description: String? = null,

    @Column(columnDefinition = "TEXT[]")
    val objectives: Array<String>? = null,

    val estimatedMinutes: Int? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### Episode

```kotlin
@Entity
@Table(name = "episodes")
data class Episode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    val module: Module,

    val episodeNumber: Int,

    @Enumerated(EnumType.STRING)
    val type: EpisodeType,

    val title: String,
    val content: String,
    val audioUrl: String? = null,
    val transcript: String? = null,
    val estimatedMinutes: Int? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class EpisodeType {
    STORY,
    DIALOGUE,
    ARTICLE,
    AUDIO_LESSON
}
```

---

## Content Import

### JSON Format

**Course Metadata** (`course.json`):
```json
{
  "slug": "french-a1",
  "name": "French A1 - Complete Beginner",
  "languageCode": "fr",
  "cefrLevel": "A1",
  "description": "Complete beginner course for French language",
  "objectives": [
    "Greet people and introduce yourself",
    "Order food and drinks",
    "Ask for directions"
  ],
  "estimatedHours": 40
}
```

**Module Content** (`module_1.json`):
```json
{
  "moduleNumber": 1,
  "title": "Greetings & Introductions",
  "theme": "Basic Communication",
  "description": "Learn how to greet people and introduce yourself",
  "objectives": [
    "Understand basic greetings",
    "Introduce yourself"
  ],
  "estimatedMinutes": 120,
  "episodes": [
    {
      "episodeNumber": 1,
      "type": "STORY",
      "title": "Marie Arrives in Paris",
      "content": "Marie steps off the train at Gare du Nord...",
      "generateAudio": false,
      "estimatedMinutes": 15,
      "contentItems": [
        {
          "orderIndex": 1,
          "type": "EXERCISE",
          "isRequired": true,
          "exercise": {
            "type": "multiple_choice",
            "title": "Comprehension Check",
            "instructions": "Choose the correct answer",
            "content": {
              "question": {
                "type": "text",
                "content": "Where did Marie arrive?"
              },
              "options": [
                { "id": "a", "text": "Paris", "isCorrect": true },
                { "id": "b", "text": "Lyon", "isCorrect": false }
              ]
            },
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

### Import Process

**Step 1: Import Course**
```bash
curl -X POST http://localhost:8080/api/admin/courses/import \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d @course.json
```

**Step 2: Import Modules**
```bash
curl -X POST "http://localhost:8080/api/admin/courses/french-a1/modules/import?generateAudio=true" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d @module_1.json
```

### Import Features

- **Automatic Exercise Creation**: Exercises defined in JSON are automatically created
- **Audio Generation**: Set `generateAudio=true` to generate audio for episodes
- **Validation**: JSON structure is validated before import
- **Transaction Safety**: All operations are atomic (all-or-nothing)
- **Duplicate Handling**: Automatically updates if module already exists

---

## Audio Generation

### Single-Speaker Audio

Used for STORY, ARTICLE, and AUDIO_LESSON episodes.

**Configuration**:
```json
{
  "generateAudio": true,
  "voice": "Aoede",
  "audioStyle": "Narrative, storytelling tone"
}
```

### Multi-Speaker Audio

Used for DIALOGUE episodes (up to 2 speakers).

**Configuration**:
```json
{
  "generateAudio": true,
  "speakers": [
    {
      "name": "Marie",
      "voice": "Puck",
      "description": "A friendly female traveler"
    },
    {
      "name": "Réceptionniste",
      "voice": "Algieba",
      "description": "A professional hotel receptionist"
    }
  ],
  "audioStyle": "This is a polite, professional conversation at a hotel reception desk"
}
```

### Voice Selection

**Female-Sounding Voices:**
- Puck (Upbeat, Middle pitch)
- Kore (Firm, Middle pitch)
- Leda (Youthful, Higher pitch)
- Aoede (Breezy, Middle pitch)
- [30+ voices available - see course-content README]

**Male-Sounding Voices:**
- Algieba (Smooth, Lower pitch)
- Enceladus (Breathy, Lower pitch)
- Achird (Friendly, Lower middle pitch)
- Charon (Informative, Lower pitch)

### Audio Generation Process

1. Content is sent to Gemini TTS API
2. Audio file is received as base64
3. File is decoded and saved to storage
4. Episode's `audioUrl` is updated with file path

---

## API Reference

### Public Endpoints

**Get All Courses**
```http
GET /api/courses
Response: List<CourseDto>
```

**Get Course Details**
```http
GET /api/courses/{slug}
Response: CourseDto
```

**Get Modules for Course**
```http
GET /api/courses/{courseSlug}/modules
Response: List<ModuleDto>
```

**Get Episodes for Module**
```http
GET /api/modules/{moduleId}/episodes
Response: List<EpisodeDto>
```

**Get Episode Details**
```http
GET /api/episodes/{episodeId}
Response: EpisodeWithContentDto
```

### Admin Endpoints

**Import Course**
```http
POST /api/admin/courses/import
Request: CourseImportDto
Response: CourseDto
Requires: ADMIN role
```

**Import Module**
```http
POST /api/admin/courses/{courseSlug}/modules/import?generateAudio=true
Request: ModuleImportDto
Response: ModuleDto
Requires: ADMIN role
```

**Delete Module**
```http
DELETE /api/admin/courses/modules/{moduleId}
Response: 204 No Content
Requires: ADMIN role
```

**Get Modules for Admin View**
```http
GET /api/admin/courses/{courseId}/modules
Response: List<ModuleAdminDto>
Requires: ADMIN role
```

---

## Frontend Integration

### Course Browsing

```vue
<template>
  <div class="courses-container">
    <Card v-for="course in courses" :key="course.slug" @click="goToCourse(course.slug)">
      <template #title>{{ course.name }}</template>
      <template #subtitle>{{ course.cefrLevel }} - {{ course.languageCode }}</template>
      <template #content>
        <p>{{ course.description }}</p>
        <Tag :value="`${course.estimatedHours} hours`" />
      </template>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useCourseApi } from '@/composables/useCourseApi'

const { getCourses } = useCourseApi()
const courses = ref([])

onMounted(async () => {
  courses.value = await getCourses()
})
</script>
```

### Episode Viewer

```vue
<template>
  <div class="episode-viewer">
    <h2>{{ episode.title }}</h2>

    <!-- Audio Player -->
    <audio v-if="episode.audioUrl" :src="episode.audioUrl" controls />

    <!-- Content Display -->
    <div class="episode-content">
      {{ episode.content }}
    </div>

    <!-- Content Items (Exercises/Grammar) -->
    <div v-for="item in episode.contentItems" :key="item.id" class="content-item">
      <ExerciseRenderer v-if="item.type === 'EXERCISE'" :exercise="item.exercise" />
      <GrammarRule v-else-if="item.type === 'GRAMMAR_RULE'" :rule="item.grammarRule" />
    </div>
  </div>
</template>
```

---

## Content Creation Guide

### Creating a New Module

1. **Plan the module structure**:
   - Choose a theme (e.g., "At the Restaurant")
   - Determine number of episodes (2-4 recommended)
   - Select episode types (mix of STORY and DIALOGUE)

2. **Write episode content**:
   - Create engaging, level-appropriate text
   - For dialogues, write natural conversations
   - Include cultural context where relevant

3. **Design exercises**:
   - Place exercises after introducing new content
   - Use variety: different exercise types
   - Ensure exercises test the content just learned

4. **Configure audio**:
   - Choose appropriate voices for characters
   - Write style prompts to guide TTS
   - Test audio generation before final import

5. **Create JSON file**:
   - Use module_2.json as template
   - Validate JSON structure
   - Check all required fields

6. **Import and test**:
   - Import via admin API
   - Test all exercises
   - Listen to generated audio
   - Adjust and re-import if needed

### Best Practices

**Content**:
- Keep episodes focused on single topics
- Use repetition for vocabulary reinforcement
- Build on previous modules
- Include real-world scenarios

**Exercises**:
- Mix exercise types (3-4 per episode)
- Start easy, increase difficulty gradually
- Provide helpful hints
- Write clear explanations

**Audio**:
- Match voices to character personalities
- Use style prompts for natural tone
- Keep dialogues concise (< 300 words)
- Preview audio before publishing

---

## Example: Complete Module Structure

See `/course-content/french-a1/module_2.json` for a complete example featuring:
- 3 DIALOGUE episodes
- All 6 exercise types demonstrated
- Multi-speaker audio configuration
- Grammar rules integration
- Proper JSON structure

---

## Future Enhancements

1. **User Progress Tracking**: Track completion of modules/episodes
2. **Unlocking System**: Require completing Module N before Module N+1
3. **Certificates**: Award certificates upon course completion
4. **AI Content Generation**: Generate exercises from episode content
5. **User-Created Courses**: Allow users to create and share courses
6. **Video Support**: Add VIDEO episode type
7. **Interactive Dialogues**: Role-play exercises with STT

---

**Document Status:** ✅ Complete
**Author:** Claude Code
**Created:** January 2025
**Last Updated:** January 2025
