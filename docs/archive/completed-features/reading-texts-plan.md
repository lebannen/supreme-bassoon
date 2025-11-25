# Reading Texts System - Implementation Plan

**Status:** ✅ **COMPLETED**
**Completed Date:** January 2025

## Overview

Implement a complete reading texts management system that allows:

1. ✅ Importing texts from JSON files
2. ✅ Automatic conversion of raw text into paginated book format
3. ✅ Browsing and selecting texts by level, topic, language
4. ✅ Storing metadata separate from content formatting
5. ✅ Progress tracking with page-level granularity

---

## 1. Data Model

### Backend Entity: ReadingText

```kotlin
@Entity
@Table(name = "reading_texts")
data class ReadingText(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,  // Raw text, not pre-formatted

    @Column(nullable = false, length = 10)
    val languageCode: String,

    @Column(length = 10)
    val level: String? = null,  // A1, A2, B1, B2, C1, C2

    @Column(length = 100)
    val topic: String? = null,  // daily_life, travel, food, culture, etc.

    @Column
    val wordCount: Int? = null,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column
    val estimatedMinutes: Int? = null,  // Reading time estimate

    @Column
    val difficulty: String? = null,  // beginner, intermediate, advanced

    @Column
    val isPublished: Boolean = false,

    @Column
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(length = 100)
    val author: String? = null,  // 'system', 'ai', username

    @Column(length = 100)
    val source: String? = null  // 'manual', 'ai-generated', 'import'
)
```

### User Progress Tracking

```kotlin
@Entity
@Table(name = "user_reading_progress")
data class UserReadingProgress(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val textId: Long,

    @Column
    val currentPage: Int = 1,

    @Column
    val totalPages: Int = 1,

    @Column
    val completed: Boolean = false,

    @Column
    val startedAt: LocalDateTime = LocalDateTime.now(),

    @Column
    val completedAt: LocalDateTime? = null,

    @Column
    val lastReadAt: LocalDateTime = LocalDateTime.now()
)
```

---

## 2. JSON Import Format

### Example: `texts/fr_a1_cafe.json`

```json
{
  "title": "Au Café",
  "language_code": "fr",
  "level": "A1",
  "topic": "daily_life",
  "description": "A simple conversation at a French café",
  "author": "ai",
  "source": "ai-generated",
  "content": "Marie entre dans un petit café à Paris. Elle s'assoit près de la fenêtre.\n\nLe serveur arrive avec un sourire.\n\n« Bonjour ! Que voulez-vous boire ? » demande-t-il.\n\n« Un café, s'il vous plaît », répond Marie.\n\n« Avec du sucre ? »\n\n« Oui, merci. Et un croissant aussi. »\n\nLe serveur note la commande et part vers la cuisine.\n\nMarie regarde par la fenêtre. Les gens marchent dans la rue. Il fait beau aujourd'hui.\n\nLe serveur revient avec le café chaud et un croissant doré.\n\n« Voilà ! Bon appétit ! »\n\n« Merci beaucoup ! »\n\nMarie boit son café lentement. C'est délicieux. Le croissant est croustillant et frais.\n\nElle aime ce moment de calme le matin."
}
```

### Import Multiple Texts: `texts/index.json`

```json
{
  "texts": [
    {
      "file": "fr_a1_cafe.json"
    },
    {
      "file": "fr_a1_family.json"
    },
    {
      "file": "de_a1_greeting.json"
    }
  ]
}
```

---

## 3. Text-to-Pages Conversion

### Algorithm (Frontend)

The book component already handles pagination. We need to:

1. Split text into paragraphs
2. Let the BookComponent handle word wrapping and page breaks
3. Calculate pages dynamically based on viewport size

**No pre-processing needed** - the BookComponent's `computePages()` method already:

- Splits by paragraphs
- Handles word wrapping
- Creates page breaks
- Maintains word boundaries

**Backend only stores raw text**, frontend handles pagination.

---

## 4. Database Migration

### V10__Create_reading_texts.sql

```sql
-- Reading texts table
CREATE TABLE reading_texts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),
    level VARCHAR(10),
    topic VARCHAR(100),
    word_count INTEGER,
    description TEXT,
    estimated_minutes INTEGER,
    difficulty VARCHAR(50),
    is_published BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    author VARCHAR(100),
    source VARCHAR(100)
);

CREATE INDEX idx_reading_texts_language ON reading_texts(language_code);
CREATE INDEX idx_reading_texts_level ON reading_texts(level);
CREATE INDEX idx_reading_texts_topic ON reading_texts(topic);
CREATE INDEX idx_reading_texts_published ON reading_texts(is_published);

-- User reading progress
CREATE TABLE user_reading_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    text_id BIGINT NOT NULL REFERENCES reading_texts(id) ON DELETE CASCADE,
    current_page INTEGER DEFAULT 1,
    total_pages INTEGER DEFAULT 1,
    completed BOOLEAN DEFAULT false,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    last_read_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, text_id)
);

CREATE INDEX idx_user_reading_progress_user ON user_reading_progress(user_id);
CREATE INDEX idx_user_reading_progress_text ON user_reading_progress(text_id);
CREATE INDEX idx_user_reading_progress_completed ON user_reading_progress(user_id, completed);
```

---

## 5. Backend Implementation

### Service Layer

```kotlin
@Service
class ReadingTextService(
    private val readingTextRepository: ReadingTextRepository,
    private val progressRepository: UserReadingProgressRepository
) {
    fun getAllTexts(languageCode: String?, level: String?, topic: String?): List<ReadingText>

    fun getTextById(id: Long): ReadingText

    fun importFromJson(jsonContent: String): ReadingText

    fun getUserProgress(userId: Long, textId: Long): UserReadingProgress?

    fun updateProgress(userId: Long, textId: Long, currentPage: Int, totalPages: Int)

    fun markCompleted(userId: Long, textId: Long)
}
```

### API Endpoints

```kotlin
@RestController
@RequestMapping("/api/reading")
class ReadingController(private val readingTextService: ReadingTextService) {

    @GetMapping("/texts")
    fun getTexts(
        @RequestParam(required = false) languageCode: String?,
        @RequestParam(required = false) level: String?,
        @RequestParam(required = false) topic: String?
    ): List<ReadingTextDto>

    @GetMapping("/texts/{id}")
    fun getText(@PathVariable id: Long): ReadingTextDto

    @PostMapping("/texts/import")
    fun importText(@RequestBody json: String): ReadingTextDto

    @GetMapping("/texts/{id}/progress")
    fun getProgress(@PathVariable id: Long): UserReadingProgressDto

    @PostMapping("/texts/{id}/progress")
    fun updateProgress(
        @PathVariable id: Long,
        @RequestBody request: UpdateProgressRequest
    ): UserReadingProgressDto
}
```

---

## 6. Frontend Implementation

### New Components

```
src/components/reading/
├── TextLibrary.vue          # Browse all texts with filters
├── TextCard.vue             # Display text preview card
├── TextReader.vue           # Full reading view with BookComponent
└── TextFilters.vue          # Level, topic, language filters

src/views/
├── ReadingLibraryView.vue   # Main texts browsing page
└── ReadingView.vue          # Update to load texts from API
```

### Routes

```typescript
{
  path: '/reading',
  name: 'ReadingLibrary',
  component: ReadingLibraryView
},
{
  path: '/reading/:id',
  name: 'ReadingText',
  component: ReadingView  // Updated to accept ID param
}
```

### Composable: useReadingTexts.ts

```typescript
export function useReadingTexts() {
  const texts = ref<ReadingText[]>([])
  const currentText = ref<ReadingText | null>(null)
  const progress = ref<ReadingProgress | null>(null)

  async function fetchTexts(filters: TextFilters): Promise<ReadingText[]>
  async function fetchTextById(id: number): Promise<ReadingText>
  async function updateProgress(textId: number, page: number, totalPages: number)

  return {
    texts,
    currentText,
    progress,
    fetchTexts,
    fetchTextById,
    updateProgress
  }
}
```

---

## 7. Implementation Phases

### Phase 1: Backend Foundation

1. Create database migration V10
2. Create ReadingText and UserReadingProgress entities
3. Create repositories
4. Implement ReadingTextService
5. Create API endpoints
6. Test with manual database inserts

### Phase 2: Import System

1. Create JSON import endpoint
2. Create script to batch import from `texts/` directory
3. Test importing sample texts
4. Calculate word count automatically

### Phase 3: Frontend - Library View

1. Create TextLibrary component
2. Implement filtering by level, topic, language
3. Create TextCard for preview
4. Add navigation to reading view

### Phase 4: Frontend - Reading View

1. Update ReadingView to load from API (not hardcoded)
2. Integrate with BookComponent
3. Track reading progress
4. Save progress to backend
5. Show completion status

### Phase 5: Cleanup & Polish

1. Remove example/hardcoded text from BookExample
2. Update navigation to point to library
3. Add loading states
4. Error handling
5. Empty states (no texts available)

---

## 8. AI Text Generation Prompt

### Prompt Template

```
Generate a short reading text for French language learners at A1 level.

Requirements:
- Topic: [daily_life / travel / food / culture / shopping / family]
- Length: 150-250 words
- Use simple present tense and common vocabulary
- Include dialogue if appropriate
- Natural, engaging story or scenario
- Avoid complex grammar structures
- Include common, useful phrases

Format the output as:
- Plain text only
- Use paragraphs (separated by blank lines)
- Include dialogue with proper formatting

Example topic: "At a café"

Do not include:
- Title (I'll add it separately)
- Any metadata
- English translations
```

### Specific Example Prompt

```
Generate a short reading text for French A1 learners about going to a bakery.

Requirements:
- 150-200 words
- Simple present tense
- Include dialogue between customer and baker
- Use common food vocabulary (pain, croissant, baguette, etc.)
- Include polite expressions (bonjour, s'il vous plaît, merci)
- Natural, realistic scenario

Output plain text only with paragraphs and dialogue formatting.
```

---

## 9. Sample Texts to Create

### French A1

1. Au Café (At the café)
2. À la Boulangerie (At the bakery)
3. Ma Famille (My family)
4. Une Journée Normale (A normal day)
5. Au Supermarché (At the supermarket)

### German A1

1. Im Café (At the café)
2. Meine Familie (My family)
3. Einkaufen (Shopping)
4. Ein Normaler Tag (A normal day)
5. Im Restaurant (At the restaurant)

---

## 10. Success Criteria

- [x] Can import texts from JSON files - ✅ **Frontend UI with drag & drop**
- [x] Texts display in paginated book format - ✅ **BookComponent integration**
- [x] Can filter texts by level, topic, language - ✅ **TextLibrary with filters**
- [x] Reading progress saves automatically - ✅ **Page-level tracking**
- [x] Can mark texts as completed - ✅ **Completion tracking**
- [x] Word lookup works in reading view - ✅ **WordCard integration**
- [x] No hardcoded example texts remain - ✅ **Demo mode preserved, reading mode uses API**

---

## Implementation Summary

### Completed Features

**Backend (Phase 1):**

- ✅ Database migration V10 with `reading_texts` and `user_reading_progress` tables
- ✅ ReadingText and UserReadingProgress entities
- ✅ ReadingTextRepository with custom query methods
- ✅ ReadingTextService with business logic
- ✅ ReadingController with REST API endpoints
- ✅ Unit tests (ReadingTextServiceTest) and integration tests (ReadingTextIntegrationTest)

**Backend (Phase 2 - Import):**

- ✅ JSON import endpoint `/api/reading/texts/import`
- ✅ Automatic word count calculation
- ✅ Automatic reading time estimation
- ✅ 4 sample texts created (3 French A1, 1 German A1)
- ✅ Security config: public text browsing, authenticated progress tracking

**Frontend (Phase 3 - Library):**

- ✅ `useReadingTexts` composable with native fetch API
- ✅ `TextCard` component for text previews
- ✅ `TextLibrary` component with filtering
- ✅ `ReadingLibraryView` with "Import Texts" button
- ✅ Routes: `/reading` (library), `/reading/:id` (reader), `/reading/import` (import UI)

**Frontend (Phase 4 - Reading):**

- ✅ `BookView` supports both demo and reading modes
- ✅ Dynamic text loading from API by ID
- ✅ Progress tracking integration
- ✅ Page-change event emission
- ✅ WordCard integration for vocabulary lookup

**Frontend (Phase 5 - Import UI):**

- ✅ `ReadingImportView` with drag & drop
- ✅ Multi-file upload capability
- ✅ Per-file status tracking
- ✅ Upload summary with stats
- ✅ snake_case to camelCase conversion

### Known Issues (Fixed)

- ✅ Authentication bug in ReadingController (using Authentication instead of JWT token)
- ✅ Missing tooltip directive import (replaced with aria-label)

### Files Created/Modified

**Backend:**

- `backend/src/main/resources/db/migration/V10__Create_reading_texts.sql`
- `backend/src/main/kotlin/com/vocabee/domain/model/ReadingText.kt`
- `backend/src/main/kotlin/com/vocabee/domain/model/UserReadingProgress.kt`
- `backend/src/main/kotlin/com/vocabee/domain/repository/ReadingTextRepository.kt`
- `backend/src/main/kotlin/com/vocabee/domain/repository/UserReadingProgressRepository.kt`
- `backend/src/main/kotlin/com/vocabee/service/ReadingTextService.kt`
- `backend/src/main/kotlin/com/vocabee/web/api/ReadingController.kt`
- `backend/src/main/kotlin/com/vocabee/web/dto/ReadingTextDto.kt`
- `backend/src/main/kotlin/com/vocabee/web/dto/UserReadingProgressDto.kt`
- `backend/src/main/kotlin/com/vocabee/web/dto/ImportTextRequest.kt`
- `backend/src/main/kotlin/com/vocabee/web/dto/UpdateProgressRequest.kt`
- `backend/src/test/kotlin/com/vocabee/service/ReadingTextServiceTest.kt`
- `backend/src/test/kotlin/com/vocabee/integration/ReadingTextIntegrationTest.kt`

**Frontend:**

- `frontend/src/composables/useReadingTexts.ts`
- `frontend/src/components/reading/TextCard.vue`
- `frontend/src/components/reading/TextLibrary.vue`
- `frontend/src/views/ReadingLibraryView.vue`
- `frontend/src/views/ReadingImportView.vue`
- `frontend/src/views/BookView.vue` (modified for API integration)
- `frontend/src/components/BookComponent.vue` (modified for events)
- `frontend/src/router/index.ts` (added routes)
- `frontend/src/App.vue` (updated navigation)

**Sample Texts:**

- `texts/fr_a1_boulangerie.json`
- `texts/fr_a1_famille.json`
- `texts/de_a1_cafe.json`
- `texts/fr_a1_example.json` (already existed)
