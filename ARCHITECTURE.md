# Vocabee Architecture

## Project Structure

```
vocabee/
├── .claude/               # AI assistant instructions and context
│   ├── instructions.md   # Development rules and preferences
│   ├── context.md        # Project context and design decisions
│   ├── testing.md        # Test cases and validation queries
│   └── debugging.md      # Debugging quick reference
├── frontend/              # Vue 3 + TypeScript + PrimeVue
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── Dockerfile
├── backend/               # Kotlin + Spring Boot
│   ├── src/
│   ├── build.gradle.kts
│   └── Dockerfile
├── scripts/               # Python data processing scripts
│   ├── wikitext_parser.py
│   ├── extract_languages.py
│   ├── test_parser_fixes.py
│   └── query_entry.py
├── parsed_data/           # Extracted Wiktionary data (JSONL.gz files)
├── nginx/                 # Nginx configuration
│   └── nginx.conf
├── docker-compose.yml     # Docker Compose configuration
├── ARCHITECTURE.md        # This file - architecture documentation
└── README.md              # Project overview
```

## Architecture

### Frontend (Port 80/443)
- **Framework**: Vue 3 with TypeScript
- **UI Library**: PrimeVue
- **Build Tool**: Vite
- **Served by**: Nginx (static files)
- **API Calls**: To `/api/*` (proxied by nginx to backend)

### Backend (Port 8080)
- **Framework**: Spring Boot 3 + Kotlin
- **Database**: PostgreSQL
- **API**: REST endpoints under `/api/*`
- **CORS**: Configured for frontend origin

### Database (Port 5432)
- **PostgreSQL 15**
- **Schema**: Flyway migrations
- **Data**: ~4.2M entries from Wiktionary

## Development Setup

### Frontend Development
```bash
cd frontend
npm install
npm run dev  # Runs on localhost:5173
```

### Backend Development
```bash
cd backend
./gradlew bootRun  # Runs on localhost:8080
```

### Database
```bash
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=vocabee \
  -e POSTGRES_USER=vocabee \
  -e POSTGRES_PASSWORD=vocabee \
  postgres:15
```

## Production Deployment

```bash
docker-compose up -d
```

This starts:
- Frontend (nginx) on port 80
- Backend (Spring Boot) on port 8080 (internal)
- PostgreSQL on port 5432 (internal)

## API Routes

### Frontend Routes (SPA - Current)
- `/` - Home page
- `/search` - Word search interface (main feature)
- `/import` - Admin import interface (for importing JSONL data)
- `/about` - About page
- All handled by Vue Router with lazy-loading

## Data Flow

1. **Data Processing** (One-time)
   - Python scripts extract from Wiktionary XML
   - Output: JSONL.gz files per language
   - Import to PostgreSQL using Spring Boot importer

2. **Search Flow** (Runtime)
   - User searches for a word (e.g., "avoir")
   - Frontend sends GET request to `/api/v1/search?q=avoir&lang=fr`
   - Backend Stage 1: Database returns ~2000 candidates with basic ranking
   - Backend Stage 2: Application layer scores and returns top 500
   - Frontend displays results in paginated DataTable

3. **Word Details Flow** (Runtime)
   - User clicks on a word
   - Frontend fetches GET `/api/v1/words/{lang}/{lemma}`
   - Backend loads word with definitions, examples, inflected forms
   - Frontend displays in Dialog modal

## Backend Module Structure

```
backend/src/main/kotlin/com/vocabee/
├── config/                    # Spring configuration
│   ├── AsyncConfig.kt        # Async task execution configuration
│   └── WebConfig.kt          # CORS settings for frontend
├── domain/
│   ├── model/                # JPA entities and data classes
│   │   ├── Word.kt           # @Entity - Main word entity
│   │   ├── Definition.kt     # @Entity - Word definitions
│   │   ├── Example.kt        # @Entity - Example sentences
│   │   ├── Pronunciation.kt  # @Entity - IPA and audio
│   │   ├── Language.kt       # @Entity - Supported languages
│   │   └── ImportProgress.kt # Data class - Import tracking (in-memory)
│   └── repository/           # Data access layer (Spring Data JPA)
│       ├── WordRepository.kt      # Word queries
│       └── LanguageRepository.kt  # Language queries
├── service/                  # Business logic
│   ├── VocabularyService.kt      # Word search and retrieval
│   ├── SearchRankingService.kt   # Application-layer ranking
│   └── ImportService.kt          # Data import pipeline
├── web/
│   ├── api/                  # REST controllers
│   │   ├── VocabularyController.kt  # Word endpoints
│   │   └── ImportController.kt      # Import endpoints
│   └── dto/                  # Data transfer objects
│       ├── WordDto.kt            # Full word response
│       ├── WordSummaryDto.kt     # Search result item
│       ├── DefinitionDto.kt
│       ├── ExampleDto.kt
│       ├── SearchResultDto.kt
│       └── ImportDto.kt
└── VocabeeApplication.kt     # Main application class
```

## Backend Conventions

### Package Organization
- **domain.model**: JPA entities, always map to database tables
- **domain.repository**: Spring Data JPA repositories, database queries
- **service**: Business logic, transaction boundaries
- **web.api**: REST controllers, HTTP request/response handling
- **web.dto**: DTOs for API responses, never expose entities directly
- **config**: Spring configuration classes

### Naming Patterns
- **Entities**: Singular noun (Word, Definition, Language)
- **Repositories**: `{Entity}Repository` (WordRepository)
- **Services**: `{Domain}Service` (VocabularyService, ImportService)
- **Controllers**: `{Domain}Controller` (VocabularyController)
- **DTOs**: `{Entity}Dto` (WordDto, DefinitionDto)

### Entity Relationships
```kotlin
Word (1) ----< (N) Definition
Definition (1) ----< (N) Example
Word (1) ----< (N) Pronunciation
Word (1) ----< (N) Word (inflected forms via lemma_id)
Language (1) ----< (N) Word
```

### Database Schema
```sql
languages:
  code (PK, varchar)
  name
  entry_count

words:
  id (PK)
  language_code (FK -> languages.code)
  lemma
  normalized (indexed)
  part_of_speech
  etymology
  usage_notes
  frequency_rank
  is_inflected_form
  lemma_id (FK -> words.id, nullable)
  grammatical_features (jsonb)

definitions:
  id (PK)
  word_id (FK -> words.id)
  definition_number
  definition_text

examples:
  id (PK)
  definition_id (FK -> definitions.id)
  sentence_text
  translation

pronunciations:
  id (PK)
  word_id (FK -> words.id)
  ipa
  audio_url
  dialect
```

### API Endpoints (Current)
- `GET /api/v1/languages` - List all languages
- `GET /api/v1/search?q={query}&lang={code}` - Search words
- `GET /api/v1/words/{lang}/{lemma}` - Get word details
- `POST /api/v1/import` - Start data import
- `GET /api/v1/import/progress` - List import progress
- `GET /api/v1/import/progress/{id}` - Get specific import status
- `DELETE /api/v1/import/clear` - Clear all words (dev only)

## Frontend Module Structure

```
frontend/src/
├── assets/           # Static assets (CSS, images)
├── components/       # Reusable Vue components
├── composables/      # Composition API composables
│   ├── useVocabularyApi.ts  # Search and word API client
│   └── useImportApi.ts      # Import API client
├── router/           # Vue Router configuration
│   └── index.ts
├── stores/           # Pinia stores (currently unused, reserved for future state management)
├── views/            # Page components
│   ├── HomeView.vue         # Landing page
│   ├── SearchView.vue       # Main search interface
│   ├── ImportView.vue       # Admin import interface
│   └── AboutView.vue        # About page
├── App.vue           # Root component
└── main.ts           # Application entry point
```

## Frontend Conventions

### Component Organization
- **views/**: Top-level page components (routable)
- **components/**: Reusable components used across views
- **composables/**: Shared logic, API clients, utilities

### Naming Patterns
- **Views**: `{Name}View.vue` (SearchView.vue, ImportView.vue)
- **Components**: PascalCase (WordCard.vue, DefinitionList.vue)
- **Composables**: `use{Name}.ts` (useVocabularyApi.ts)

### API Client Pattern
```typescript
// composables/useVocabularyApi.ts
export function useVocabularyApi() {
  const baseUrl = '/api/v1'

  async function searchWords(lang: string, query: string) {
    const response = await fetch(`${baseUrl}/search?q=${query}&lang=${lang}`)
    return await response.json() as SearchResult
  }

  return { searchWords, getWord, getLanguages }
}
```

### State Management
- Using Vue 3 Composition API with `ref()` and `reactive()`
- No global state management (Vuex/Pinia) yet
- Component-local state for search, selections, loading states

## Code Style Guidelines

### Kotlin
- Use data classes for DTOs and value objects
- Constructor injection (primary constructor parameter)
- Explicit types for public API, inference for local variables
- Named parameters for @Query annotations
- No!! operator (prefer safe calls ?. and elvis ?: )

### TypeScript
- Explicit types for function parameters and return values
- Interfaces for API response shapes
- Type imports: `import type { Word } from './types'`
- Prefer `const` over `let`

### SQL
- Snake_case for table and column names
- Uppercase for SQL keywords
- Always use named parameters in JPQL (`:paramName`)
- Index naming: `idx_{table}_{column}`

## Testing Strategy

### Backend Testing
- Unit tests: Service layer business logic
- Integration tests: Repository queries with test database
- Controller tests: MockMvc for API endpoints

### Frontend Testing
- Component tests: Vue Test Utils
- E2E tests: Playwright (future)

## Performance Considerations

### Database Optimization
- Index on `words.normalized` for fast search
- Index on `words.language_code` for filtering
- Index on `words.lemma_id` for inflected form lookups
- LIMIT queries to prevent loading too much data
- Use JOIN FETCH carefully to avoid MultipleBagFetchException

### Search Performance
- Two-stage ranking: DB (2000 limit) → App (500 limit)
- Both stages complete in <100ms typically
- Frontend pagination (20 results per page)

### Import Performance
- Batch processing: 50 words per transaction
- Error recovery: word-by-word retry on batch failure
- Speed: ~1000 words/second
- Async processing with progress tracking
