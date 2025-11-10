# Vocabee Documentation

**Last Updated:** November 2025
**Project Status:** M1 MVP Complete, M2 Planning Phase

---

## Quick Links

- **[Project Structure](PROJECT_STRUCTURE.md)** - Complete technical architecture overview
- **[Roadmap](roadmap.md)** - Development milestones and timeline
- **[Exercise System Design](exercise-system-design.md)** - NEW - Detailed design for interactive exercises (M2)

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

---

### 📋 Planned Features (M2)

#### [Exercise System](exercise-system-design.md)
- **Status**: 📋 PLANNED (M2 Development)
- **Description**: Interactive exercises aligned with French A1 curriculum
- **Exercise Types**:
  - Vocabulary Recognition (matching, multiple choice)
  - Grammar & Structure (conjugation, word order)
  - Listening Comprehension (dictation, audio comprehension)
  - Reading Comprehension (cloze, true/false)
  - Production & Translation (guided/free translation)
- **Implementation**: 10-week phased rollout
- **AI Integration**: Gemini-based exercise generation (Phase 6)

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

### M2: Interactive Exercises (Planned - 10 weeks)
**Phase 1** (Week 1-2): Infrastructure
- Database schema for exercises
- Exercise validation framework
- Base UI components

**Phase 2** (Week 2-3): Multiple Choice Exercises
- First working exercise type
- 10 sample exercises for French A1 Module 1

**Phase 3** (Week 3-4): Matching Exercises
- Drag-and-drop interactions
- Image support
- Mobile touch optimization

**Phase 4** (Week 4-5): Exercise Collections
- Curated exercise bundles
- Progress tracking
- Unlocking system

**Phase 5** (Week 5-8): Additional Exercise Types
- Sentence Scramble
- Fill-in-the-Blank
- Listening Exercises
- Cloze Reading

**Phase 6** (Week 9-10): AI Exercise Generation
- Gemini-powered exercise creation
- Admin UI for generation
- Review/approval workflow

---

## Feature Comparison Matrix

| Feature | M1 Status | M2 Plans | AI Integration |
|---------|-----------|----------|----------------|
| **Vocabulary Lookup** | ✅ Complete | - | Wiktionary Data |
| **Reading Texts** | ✅ Complete | More texts | AI text generation |
| **Spaced Repetition** | ✅ Complete | Enhanced stats | - |
| **Audio/TTS** | ✅ Complete | Word-level sync | Gemini TTS |
| **Exercises** | - | 📋 Full system | Gemini generation |
| **Pronunciation** | - | 📋 Recording & feedback | STT evaluation |
| **Progress Tracking** | ✅ Basic | 📋 Advanced analytics | AI recommendations |
| **Gamification** | ✅ Minimal | 📋 Achievements | - |

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

### Planned Tables (M2)

**Exercises:**
- `exercise_types` - Exercise type definitions
- `exercises` - Exercise content (JSONB)
- `user_exercise_attempts` - Attempt history
- `exercise_collections` - Exercise bundles
- `exercise_collection_items` - Exercises in collections

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

### Exercises (`/api/exercises`) - PLANNED M2
- Browse exercises, submit attempts, track progress

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
