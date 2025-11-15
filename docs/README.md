# Vocabee Documentation

**Last Updated:** January 2025
**Project Status:** M1 Complete, M2 Complete (All Exercise Types Implemented)

---

## Quick Links

- **[Project Structure](PROJECT_STRUCTURE.md)** - Complete technical architecture overview
- **[Roadmap](roadmap.md)** - Development milestones and timeline
- **[Exercise System Design](exercise-system-design.md)** - Detailed technical design for interactive exercises
- **[Exercise Types Reference](exercise-types-reference.md)** - ⭐ NEW - Complete guide to all 6 exercise types

---

## Documentation by Status

### ✅ Completed Features

#### [Audio Integration](audio-integration-plan.md)
- **Status**: ✅ COMPLETED (November 2025)
- **Description**: File upload system with MinIO, TTS pipeline with Gemini API, MP3 generation
- **Key Features**: Audio player component, admin upload UI, 67% size reduction with MP3
- **Next Steps**: Consider word-level synchronization for advanced reading features

#### [Reading Texts System](reading-texts-plan.md)
- **Status**: ✅ COMPLETED (January 2025)
- **Description**: Complete reading text management with progress tracking
- **Key Features**: Text library, 3D book reader, drag-and-drop import, page-level progress
- **Database**: `reading_texts`, `user_reading_progress` tables

#### [Study Mode / Spaced Repetition](study-mode-design.md)
- **Status**: ✅ COMPLETED (January 2025)
- **Description**: Flashcard study system with SM-2 spaced repetition algorithm
- **Key Features**: Daily reviews, session tracking, adaptive intervals, progress statistics
- **Database**: `study_sessions`, `study_session_items`, `study_session_attempts` tables

#### [Exercise System](exercise-system-design.md)
- **Status**: ✅ COMPLETED (January 2025)
- **Description**: Interactive exercises aligned with French A1 curriculum
- **Exercise Types Implemented** (See [Exercise Types Reference](exercise-types-reference.md)):
  1. **Multiple Choice** - Vocabulary recognition and translation (5 exercises)
  2. **Fill in the Blank** - Grammar conjugation and vocabulary recall (6 exercises)
  3. **Sentence Scramble** - Word order and syntax practice (6 exercises)
  4. **Matching** - Vocabulary pairs and association (6 exercises)
  5. **Listening Comprehension** - Audio comprehension with 6 generated audio files (6 exercises)
  6. **Cloze Reading** - Context-based grammar and reading (6 exercises)
- **Total**: 35 sample exercises, 45 unit tests, 6 frontend components
- **Next Steps**: AI-assisted exercise generation, exercise collections

---

### 📋 Planned Features (M3+)

---

## Reference Documents

### [French A1 Curriculum Plan](french_a1_plan.md)
- **Type**: Curriculum Design
- **Description**: 10-module structure for French A1 learning
- **Purpose**: Foundation for exercise system and reading content
- **Modules**: Greetings → Daily Routine → Past Tense → Synthesis
- **Use Case**: Guide exercise creation and content generation

### [Project Structure](PROJECT_STRUCTURE.md)
- **Type**: Technical Reference
- **Description**: Complete architecture documentation
- **Contents**:
  - Backend structure (Kotlin + Spring Boot)
  - Frontend structure (Vue 3 + TypeScript)
  - Database schema (PostgreSQL + Flyway)
  - API endpoints and patterns
  - Development conventions

---

## Document Status Legend

- ✅ **COMPLETED**: Feature fully implemented and deployed
- 🔄 **IN PROGRESS**: Active development
- 📋 **PLANNED**: Designed but not yet started
- 🔧 **MAINTENANCE**: Periodic updates required
- 📚 **REFERENCE**: Static reference material
- 🗄️ **ARCHIVED**: Outdated or superseded

---

## Implementation Timeline

### M1: MVP (Completed - January 2025)
- User Authentication (OAuth2 + JWT)
- Personal Vocabulary Management
- Reading Interface with Word Lookup
- Spaced Repetition Study System
- Pre-made Word Sets
- Audio Integration & TTS

**Deliverables**: Fully functional language learning app with reading and spaced repetition

### M2: Interactive Exercises (Completed - January 2025)

**All 6 Core Exercise Types Implemented:**

1. **Multiple Choice** - Vocabulary and translation recognition
2. **Fill in the Blank** - Grammar conjugation practice
3. **Sentence Scramble** - Word order and syntax building
4. **Matching** - Vocabulary pair association
5. **Listening Comprehension** - Audio-based exercises with TTS-generated audio
6. **Cloze Reading** - Multi-blank paragraph comprehension

**Deliverables**:
- 35 sample exercises across French A1 topics
- 45 comprehensive unit tests
- 6 Vue.js exercise components
- 7 database migrations
- 6 audio files (140 KB) generated with Gemini TTS
- Complete documentation and API

**See**: [Exercise Types Reference](./exercise-types-reference.md) for detailed specifications

### M3: Exercise Content & AI Generation (Planned)
**Planned Features:**
- AI-powered exercise generation using curriculum plan
- Exercise collections and bundles
- Progress tracking across exercise types
- Difficulty adjustment algorithms
- Additional exercise variants

---

## Feature Comparison Matrix

| Feature | M1 Status | M2 Status | M3 Plans | AI Integration |
|---------|-----------|-----------|----------|----------------|
| **Vocabulary Lookup** | ✅ Complete | - | - | Wiktionary Data |
| **Reading Texts** | ✅ Complete | - | More texts | AI text generation |
| **Spaced Repetition** | ✅ Complete | - | Enhanced stats | - |
| **Audio/TTS** | ✅ Complete | ✅ 6 audio files | Word-level sync | Gemini TTS |
| **Exercises** | - | ✅ All 6 types | Collections | Gemini generation |
| **Pronunciation** | - | - | 📋 Recording | STT evaluation |
| **Progress Tracking** | ✅ Basic | ✅ Per-exercise | 📋 Analytics | AI recommendations |
| **Gamification** | ✅ Minimal | ✅ Scoring | 📋 Achievements | - |

---

## Database Schema Overview

### Core Tables (Implemented)

**Authentication:**
- `users` - User accounts
- `user_roles` - Role assignments

**Vocabulary:**
- `languages` - Supported languages
- `words` - Wiktionary entries (4.2M words)
- `definitions` - Word definitions
- `examples` - Example sentences
- `pronunciations` - IPA + audio URLs
- `user_vocabulary` - Personal vocabulary with SRS fields
- `word_sets` - Curated word collections
- `word_set_items` - Words in sets

**Reading:**
- `reading_texts` - Reading materials
- `user_reading_progress` - Reading progress tracking

**Study:**
- `study_sessions` - Flashcard review sessions
- `study_session_items` - Words in sessions
- `study_session_attempts` - Individual attempts

**Exercises:**
- `exercise_types` - Exercise type definitions (6 types)
- `exercises` - Exercise content (JSONB, 35 exercises)
- `user_exercise_attempts` - Attempt history and scoring

### Planned Tables (M3)

**Exercise Collections:**
- `exercise_collections` - Exercise bundles
- `exercise_collection_items` - Exercises in collections
- `user_exercise_progress` - Collection progress tracking

---

## API Endpoint Categories

### Vocabulary & Search (`/api/v1`)
- Search words, get definitions, list languages

### User Vocabulary (`/api/vocabulary`)
- Add/remove/list personal vocabulary

### Word Sets (`/api/word-sets`)
- Browse and import curated word sets

### Reading (`/api/reading`)
- Browse texts, track progress, import new texts

### Study Sessions (`/api/study`)
- Start sessions, get cards, submit answers

### Authentication (`/api/auth`)
- Register, login, logout, validate token

### File Upload (`/api/files`)
- Upload audio/images/documents to MinIO/S3

### Exercises (`/api/exercises`) ✅ **IMPLEMENTED**
- Browse exercises by type/module/topic
- Submit exercise attempts with validation
- Track progress and scoring
- Support for all 6 exercise types

---

## Tech Stack Summary

**Backend:**
- Kotlin 1.9.21
- Spring Boot 3.2.0
- PostgreSQL 15
- Flyway (migrations)
- Spring Security (OAuth2 + JWT)
- AWS S3 SDK (MinIO compatible)

**Frontend:**
- Vue 3.5
- TypeScript
- PrimeVue 4.4
- Pinia (state management)
- Vite 7.1

**AI/ML Services:**
- Google Gemini 1.5 Pro (TTS, text generation)
- Potential: OpenAI, ElevenLabs (alternatives)

**Infrastructure:**
- Docker (PostgreSQL, MinIO)
- MinIO (S3-compatible file storage)
- GitHub (version control)

---

## Development Workflow

### Adding a New Feature

1. **Plan**: Create or update design document in `/docs`
2. **Database**: Create Flyway migration if needed
3. **Backend**: Domain models → Repository → Service → Controller → Tests
4. **Frontend**: Types → Composable → Components → Views → Integration
5. **Test**: Unit tests, integration tests, manual testing
6. **Document**: Update relevant docs with implementation notes
7. **Commit**: Descriptive commit with documentation reference

### Documentation Standards

- **Design Docs**: Markdown with clear structure, examples, database schemas
- **Status Headers**: Always include status, dates, and key metrics
- **Code Examples**: Include realistic examples, not just interfaces
- **Migration Path**: Document how features evolve over time
- **Completion Summary**: Add summary section when marking complete

---

## Related Resources

**External Documentation:**
- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Vue 3 Docs](https://vuejs.org/guide/introduction.html)
- [PrimeVue Components](https://primevue.org/components/)
- [Gemini API Docs](https://ai.google.dev/docs)

**Project Resources:**
- [Wiktionary Data](https://en.wiktionary.org/)
- [CEFR Levels](https://en.wikipedia.org/wiki/Common_European_Framework_of_Reference_for_Languages)

---

## Archived Documentation

Outdated or superseded documents:
- `archive/requirements-old.md` - Generic AI requirements (replaced by specific design docs)

---

## Contributing to Documentation

When adding new features:

1. **Create design doc** before implementation
2. **Include**: Problem statement, solution design, database schema, API endpoints, UI mockups
3. **Update** PROJECT_STRUCTURE.md with new components/services
4. **Update** roadmap.md with timeline adjustments
5. **Add completion summary** when feature is done
6. **Archive** outdated docs rather than deleting

---

**Documentation Maintainer:** Development Team
**Questions?** Check PROJECT_STRUCTURE.md first, then ask in team chat
