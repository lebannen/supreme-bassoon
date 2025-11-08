# Vocabee Project Structure Analysis

## Project Overview

**Vocabee** is a vocabulary learning application built with:
- **Backend**: Spring Boot 3 + Kotlin + PostgreSQL
- **Frontend**: Vue 3 + TypeScript + PrimeVue + Pinia
- **Architecture**: RESTful API with JWT authentication + OAuth2 support
- **Database**: PostgreSQL with Flyway migrations

---

## 1. Backend Structure

### Technology Stack
- **Framework**: Spring Boot 3.2.0
- **Language**: Kotlin 1.9.21
- **Database**: PostgreSQL 15 with Flyway migrations
- **Security**: Spring Security + JWT (jjwt 0.12.3) + OAuth2
- **Build**: Gradle

### Directory Structure
```
backend/src/main/kotlin/com/vocabee/
├── config/                           # Spring configuration classes
│   ├── AsyncConfig.kt               # Async task execution
│   ├── PasswordEncoderConfig.kt      # Password encoding setup
│   ├── SecurityConfig.kt            # Spring Security config
│   └── WebConfig.kt                 # CORS settings
│
├── domain/
│   ├── model/                       # JPA entities
│   │   ├── User.kt                  # User account
│   │   ├── UserRole.kt              # User roles (ADMIN, USER)
│   │   ├── Word.kt                  # Dictionary word entry
│   │   ├── Definition.kt            # Word definitions
│   │   ├── Example.kt               # Example sentences
│   │   ├── Pronunciation.kt         # IPA + audio URLs
│   │   ├── Language.kt              # Supported languages
│   │   ├── UserVocabulary.kt        # User's saved words
│   │   ├── WordSet.kt               # Curated word collections
│   │   ├── WordSetItem.kt           # Words in a set
│   │   └── UserImportedWordSet.kt   # User's imported sets
│   │
│   └── repository/                  # Spring Data JPA repos
│       ├── WordRepository.kt        # Word queries
│       ├── LanguageRepository.kt    # Language queries
│       ├── UserRepository.kt        # User queries
│       ├── UserVocabularyRepository.kt  # Vocab entries
│       ├── WordSetRepository.kt     # Word set queries
│       ├── WordSetItemRepository.kt # Set items
│       └── UserImportedWordSetRepository.kt
│
├── service/                         # Business logic layer
│   ├── VocabularyService.kt         # Word search & retrieval
│   ├── SearchRankingService.kt      # Application-level ranking
│   ├── UserVocabularyService.kt     # Vocabulary management
│   ├── WordSetService.kt            # Word set management
│   ├── UserService.kt               # User management
│   ├── JwtService.kt                # JWT token handling
│   └── ImportService.kt             # Data import pipeline
│
├── security/                        # Security components
│   ├── JwtAuthenticationFilter.kt   # JWT token validation
│   └── OAuth2AuthenticationSuccessHandler.kt
│
├── web/
│   ├── api/                         # REST controllers
│   │   ├── VocabularyController.kt  # /api/v1 search & word endpoints
│   │   ├── AuthController.kt        # /api/auth authentication endpoints
│   │   ├── UserVocabularyController.kt  # /api/vocabulary vocab management
│   │   ├── WordSetController.kt     # /api/word-sets word set management
│   │   └── ImportController.kt      # /api/v1/import data import
│   │
│   └── dto/                         # Data transfer objects
│       ├── WordDto.kt               # Full word response
│       ├── WordSummaryDto.kt        # Search result item
│       ├── DefinitionDto.kt
│       ├── ExampleDto.kt
│       ├── PronunciationDto.kt
│       ├── SearchResultDto.kt
│       ├── WordSetDto.kt
│       ├── VocabularyWordDto.kt
│       ├── AuthDto.kt
│       └── ImportDto.kt
│
└── VocabeeApplication.kt            # Main Spring Boot entry point
```

### Database Migrations (Flyway)
Located in: `backend/src/main/resources/db/migration/`

1. **V1__Initial_schema.sql** - Core entities
   - `languages` - Supported languages
   - `words` - Dictionary entries with indexes
   - `definitions` - Word definitions
   - `examples` - Example sentences
   - `pronunciations` - IPA + audio URLs

2. **V2__Increase_dialect_length.sql** - Schema enhancement

3. **V3__Create_users_and_auth_tables.sql** - Authentication
   - `users` - User accounts (email, password_hash, oauth_id)
   - `user_roles` - Role assignments (ADMIN, USER)

4. **V4__Limit_to_french_and_german.sql** - Data constraint

5. **V5__Create_user_vocabulary.sql** - Vocabulary tracking
   - `user_vocabulary` - User's saved words with timestamps

6. **V6__Create_word_sets.sql** - Curated collections
   - `word_sets` - Themed word collections (A1-C2 levels)
   - `word_set_items` - Words in a set
   - `user_imported_word_sets` - User's imported sets

### Key Models & Relationships

**Word Entity** (@Entity)
```kotlin
- id (PK)
- languageCode (FK -> languages.code)
- lemma (indexed for search)
- normalized (lowercase version, indexed)
- partOfSpeech (verb, noun, adjective, etc.)
- etymology, usageNotes
- frequencyRank (sorting by frequency)
- isInflectedForm (boolean - is this a conjugation/declension?)
- lemmaId (FK -> words.id, points to base form)
- grammaticalFeatures (JSONB - case, gender, mood, tense, etc.)
- definitions (1:N relationship)
- pronunciations (1:N relationship)
```

**UserVocabulary Entity** (@Entity)
```kotlin
- id (PK)
- user (FK, many-to-one)
- word (FK, many-to-one)
- notes (TEXT, user's personal notes)
- addedAt (timestamp)
```

**WordSet Entity** (@Entity)
```kotlin
- id (PK)
- name, description
- languageCode (FK)
- level (A1, A2, B1, B2, C1, C2)
- theme (category/topic)
- wordCount
- items (1:N, WordSetItem)
```

### API Endpoints (Existing)

```
Vocabulary & Search:
GET    /api/v1/languages                  - List all languages
GET    /api/v1/search?q=query&lang=code   - Search words
GET    /api/v1/words/{lang}/{lemma}       - Get word details
GET    /api/v1/health                     - Health check

User Vocabulary:
GET    /api/vocabulary                    - Get user's vocabulary
POST   /api/vocabulary                    - Add word to vocabulary
DELETE /api/vocabulary/{wordId}           - Remove word
GET    /api/vocabulary/check/{wordId}     - Check if word in vocabulary

Word Sets:
GET    /api/word-sets                     - All word sets
GET    /api/word-sets/language/{code}     - Sets by language
GET    /api/word-sets/{id}                - Set details
POST   /api/word-sets/{id}/import         - Import set to vocabulary
POST   /api/word-sets/load-from-json      - Load sets from JSON

Authentication:
POST   /api/auth/register                 - Register new user
POST   /api/auth/login                    - Login
POST   /api/auth/logout                   - Logout
GET    /api/auth/validate                 - Validate token

Data Import:
POST   /api/v1/import                     - Start import
GET    /api/v1/import/progress            - Get all import progress
GET    /api/v1/import/progress/{id}       - Get specific import status
```

---

## 2. Frontend Structure

### Technology Stack
- **Framework**: Vue 3.5.22 with TypeScript
- **State Management**: Pinia 3.0.3
- **UI Library**: PrimeVue 4.4.1 (comprehensive component library)
- **Routing**: Vue Router 4.6.3
- **Build Tool**: Vite 7.1.11

### Directory Structure
```
frontend/src/
├── assets/                          # Static assets (CSS, images)
│
├── components/                      # Reusable Vue components
│   ├── WordCard.vue                # Word summary display
│   ├── BookComponent.vue           # 3D book reader component
│   ├── PageComponent.vue           # Book page display
│   └── icons/                      # Icon components
│
├── composables/                     # Composition API composables (API clients)
│   ├── useVocabularyApi.ts         # Search & word API client
│   ├── useWordSetApi.ts            # Word set API client
│   └── useImportApi.ts             # Import API client
│
├── stores/                          # Pinia stores (state management)
│   ├── auth.ts                     # Authentication state
│   ├── vocabulary.ts               # User vocabulary state
│   └── counter.ts                  # Demo store
│
├── types/                           # TypeScript type definitions
│   ├── vocabulary.ts               # Vocabulary types
│   ├── wordSet.ts                  # Word set types
│   └── auth.ts                     # Auth types
│
├── views/                           # Page components (routable)
│   ├── HomeView.vue                # Landing page
│   ├── LoginView.vue               # Login form
│   ├── RegisterView.vue            # Registration form
│   ├── AuthCallbackView.vue        # OAuth2 callback handler
│   ├── ProfileView.vue             # User profile
│   ├── SearchView.vue              # Main word search
│   ├── VocabularyView.vue          # User's saved vocabulary
│   ├── WordSetsView.vue            # Available word sets
│   ├── BookView.vue                # 3D book reader
│   ├── ImportView.vue              # Admin import interface
│   └── AboutView.vue               # About page
│
├── services/                        # Service utilities
│   └── api.ts                      # API service (centralized API client)
│
├── router/                          # Vue Router configuration
│   └── index.ts                    # Route definitions with auth guards
│
├── App.vue                          # Root component
└── main.ts                          # Application entry point
```

### Key Types

**Vocabulary Types** (`types/vocabulary.ts`):
```typescript
interface WordSummary {
  id: number
  lemma: string
  partOfSpeech: string | null
  frequencyRank: number | null
}

interface Word extends WordSummary {
  languageCode: string
  etymology: string | null
  usageNotes: string | null
  isInflectedForm: boolean
  lemmaId: number | null
  grammaticalFeatures: Record<string, any> | null
  baseForm: BaseForm | null
  definitions: Definition[]
  pronunciations: Pronunciation[]
  inflectedForms: InflectedForm[]
}

interface Definition {
  id: number
  definitionNumber: number
  definitionText: string
  examples: Example[]
}

interface VocabularyWord {
  vocabularyId: number
  word: WordSummary
  notes: string | null
  addedAt: string
}
```

### Routes
```typescript
/                    - Home (landing)
/login               - Login page
/register            - Registration page
/auth/callback       - OAuth2 callback
/profile             - User profile (protected)
/vocabulary          - User's saved words (protected)
/search              - Word search
/word-sets           - Available word sets
/import              - Admin import interface (protected)
/book                - 3D book reader
/about               - About page
```

### Store: `stores/vocabulary.ts` (Pinia)
```typescript
State:
- words: VocabularyWord[]
- loading: boolean
- error: string | null

Computed:
- wordCount: number
- isWordInVocabulary(wordId): boolean

Actions:
- fetchVocabulary(): Promise<boolean>
- addWord(request: AddWordToVocabularyRequest): Promise<VocabularyWord | null>
- removeWord(wordId: number): Promise<boolean>
- checkIfWordInVocabulary(wordId: number): Promise<boolean>
- clearVocabulary(): void
```

---

## 3. Database Schema Summary

### Core Tables

**words** (4.2M entries from Wiktionary)
- PK: id (BIGSERIAL)
- Unique constraint: (language_code, lemma, part_of_speech)
- Indexes: language_code, normalized, frequency_rank, lemma_id, grammatical_features (GIN)

**user_vocabulary**
- PK: id
- FK: user_id, word_id
- Unique constraint: (user_id, word_id)
- Fields: notes, added_at

**word_sets** & **word_set_items**
- Support curated collections organized by language/level/theme
- Track which users have imported which sets

**users** & **user_roles**
- Support both traditional login and OAuth2
- Fields: email, password_hash, oauth_provider, oauth_id, display_name, learning_languages

### Current SRS Fields (Missing - needs implementation)
The design document outlines needed additions to `user_vocabulary`:
- next_review_at (TIMESTAMP)
- review_count (INTEGER)
- consecutive_successes (INTEGER)
- current_interval_hours (INTEGER)
- last_reviewed_at (TIMESTAMP)
- ease_factor (DECIMAL)

---

## 4. Existing Vocabulary-Related Code

### Models/Entities

**UserVocabulary.kt** (`domain/model/`)
- Maps to `user_vocabulary` table
- Links users to words
- Has timestamps and notes
- **Missing SRS fields** (as per design doc)

**Word.kt** (`domain/model/`)
- Complete word entry with definitions, examples, pronunciations
- Supports inflected forms via `lemmaId` foreign key
- Has grammatical features in JSONB format
- Supports frequency ranking

**WordSet.kt** (`domain/model/`)
- Organized word collections
- CEFR level support (A1-C2)
- Has helper method `addWord()` for adding words

**WordSetItem.kt** (`domain/model/`)
- Junction table linking words to sets
- Has display_order for sequencing

### Repositories

**UserVocabularyRepository.kt**
```kotlin
interface UserVocabularyRepository : JpaRepository<UserVocabulary, Long> {
    fun findByUserId(userId: Long): List<UserVocabulary>
    fun findByUserIdOrderByAddedAtDesc(userId: Long): List<UserVocabulary>
    fun findByUserIdAndWordId(userId: Long, wordId: Long): UserVocabulary?
    fun existsByUserIdAndWordId(userId: Long, wordId: Long): Boolean
    fun deleteByUserIdAndWordId(userId: Long, wordId: Long)
}
```

**WordRepository.kt** - Has methods for:
- `searchByLemma(languageCode, query)` - Full-text search
- `findAllWithDefinitions(languageCode, lemma)` - Load word with all relationships
- `findByIsInflectedFormTrueAndLemmaId(id)` - Get inflected forms
- `findByLanguageCodeAndNormalized()` - Exact lookups

### Services

**VocabularyService.kt**
- `searchWords(languageCode, query)` - Two-stage ranking (DB + app layer)
- `getWord(languageCode, lemma)` - Full word details with inflected forms
- `getAllLanguages()` - List supported languages

**UserVocabularyService.kt**
- `addWordToVocabulary(userId, wordId, notes)` - Add/update word
- `getUserVocabulary(userId)` - List user's vocabulary
- `removeWordFromVocabulary(userId, wordId)` - Remove word
- `isWordInVocabulary(userId, wordId)` - Check membership

**WordSetService.kt**
- `getAllWordSets()` - List all word sets
- `getWordSetsByLanguage(languageCode, userId)` - Filter by language
- `getWordSetById(wordSetId, userId)` - Get details
- `importWordSetToUserVocabulary()` - Bulk add words from set to vocabulary
- `loadWordSetsFromJson()` - Admin import function

### Controllers

**VocabularyController.kt** (@RequestMapping("/api/v1"))
- GET /search - Word search
- GET /words/{lang}/{lemma} - Word details
- GET /languages - List languages
- GET /health - Health check

**UserVocabularyController.kt** (@RequestMapping("/api/vocabulary"))
- GET / - Get user's vocabulary
- POST / - Add word
- DELETE /{wordId} - Remove word
- GET /check/{wordId} - Check membership

**WordSetController.kt** (@RequestMapping("/api/word-sets"))
- GET / - All word sets
- GET /language/{code} - By language
- GET /{id} - Details
- POST /{id}/import - Import to vocabulary
- POST /load-from-json - Admin function

---

## 5. API Request/Response Examples

### Search Words
```
GET /api/v1/search?q=parler&lang=fr

Response:
{
  "words": [
    {
      "id": 12345,
      "lemma": "parler",
      "partOfSpeech": "verb",
      "frequencyRank": 1234
    }
  ],
  "total": 1
}
```

### Get Word Details
```
GET /api/v1/words/fr/parler

Response:
{
  "id": 12345,
  "languageCode": "fr",
  "lemma": "parler",
  "partOfSpeech": "verb",
  "etymology": "from Latin...",
  "definitions": [
    {
      "id": 1,
      "definitionNumber": 1,
      "definitionText": "to speak",
      "examples": [
        {
          "id": 1,
          "sentenceText": "Je parle français",
          "translation": "I speak French"
        }
      ]
    }
  ],
  "pronunciations": [
    {
      "id": 1,
      "ipa": "/paʁle/",
      "dialect": "standard"
    }
  ],
  "inflectedForms": [...],
  "baseForm": null
}
```

### User Vocabulary
```
GET /api/vocabulary
Authorization: Bearer {jwt_token}

Response:
[
  {
    "vocabularyId": 1,
    "word": {
      "id": 12345,
      "lemma": "parler",
      "partOfSpeech": "verb",
      "frequencyRank": 1234
    },
    "notes": "important verb",
    "addedAt": "2025-11-08T10:00:00Z"
  }
]
```

---

## 6. Development Patterns & Conventions

### Kotlin Patterns
- **Constructor injection** in services and controllers
- **Data classes** for DTOs
- **Safe navigation** (`?.`) instead of null checks
- **Named parameters** in @Query annotations
- **Sequence<T>.asIterable()** for lazy evaluation

### TypeScript Patterns
- **Type imports**: `import type { Word } from '@/types'`
- **Composition API** with `ref()` and `computed()`
- **Async/await** for API calls
- **Error handling** with try/catch blocks

### Vue Components
- **Script setup** syntax
- **PrimeVue components** for UI (DataTable, Card, Button, etc.)
- **Event handling** via @click, @change directives
- **Conditional rendering** with v-if, v-for

### API Call Pattern (Composables)
```typescript
export function useVocabularyApi() {
  const API_BASE = 'http://localhost:8080/api/v1'
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  async function searchWords(lang: string, query: string): Promise<SearchResult | null> {
    isLoading.value = true
    error.value = null
    try {
      const response = await fetch(`${API_BASE}/search?lang=${lang}&q=${query}`)
      return await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      return null
    } finally {
      isLoading.value = false
    }
  }

  return { isLoading, error, searchWords }
}
```

---

## 7. Study Mode Implementation Plan

Based on the design document (`docs/study-mode-design.md`), the Study Mode feature requires:

### Backend Implementation
1. **New Models**:
   - StudySession - Main session entity
   - StudySessionItem - Words in session
   - StudySessionAttempt - Individual answer records

2. **New Services**:
   - StudySessionService - Session lifecycle management
   - SrsIntervalCalculator - Spaced repetition algorithm

3. **New API Endpoints**:
   - POST /api/study/sessions/start
   - GET /api/study/sessions/{id}/next-card
   - POST /api/study/sessions/{id}/answer
   - POST /api/study/sessions/{id}/complete
   - GET /api/study/due-words

4. **Database Migrations**:
   - Add SRS fields to user_vocabulary
   - Create study_sessions table
   - Create study_session_items table
   - Create study_session_attempts table

### Frontend Implementation
1. **New Pages**:
   - /study - Study home (session creation)
   - /study/session/:id - Active study view
   - /study/history - Session history
   - /study/summary/:id - Session summary

2. **New Components**:
   - FlashCard.vue - Card flip animation
   - StudyProgress.vue - Progress tracking
   - SessionSummary.vue - Results screen

3. **New Store**:
   - stores/study.ts - Study session state

4. **New Composables**:
   - useStudyApi.ts - Study API client

---

## 8. Key Files Reference

### Backend Entry Points
- Main app: `/Users/andrii/Projects/vocabee/backend/src/main/kotlin/com/vocabee/VocabeeApplication.kt`
- Config: `/Users/andrii/Projects/vocabee/backend/src/main/kotlin/com/vocabee/config/`

### Database Migrations
- Location: `/Users/andrii/Projects/vocabee/backend/src/main/resources/db/migration/`
- Migration tool: Flyway

### Frontend Entry Points
- Main app: `/Users/andrii/Projects/vocabee/frontend/src/main.ts`
- Root component: `/Users/andrii/Projects/vocabee/frontend/src/App.vue`
- Router: `/Users/andrii/Projects/vocabee/frontend/src/router/index.ts`

### Key Documentation
- Architecture: `/Users/andrii/Projects/vocabee/ARCHITECTURE.md`
- Study Mode Design: `/Users/andrii/Projects/vocabee/docs/study-mode-design.md`

---

## Summary

The Vocabee project is a well-structured vocabulary learning application with:
- **Strong foundation**: Complete word database, user management, word sets
- **Clean architecture**: Clear separation of concerns (repository, service, controller layers)
- **Modern stack**: Spring Boot 3 + Kotlin on backend, Vue 3 + TypeScript on frontend
- **Authentication**: JWT + OAuth2 support with authentication guards
- **Ready for Study Mode**: Design document complete, clear patterns for implementation

For Study Mode implementation, you'll follow the existing patterns:
1. Add database migrations for SRS fields and session tables
2. Create domain models (StudySession, StudySessionItem, etc.)
3. Implement service layer with SRS logic
4. Create REST controllers with documented endpoints
5. Build Vue components following existing patterns
6. Integrate with Pinia store for state management
