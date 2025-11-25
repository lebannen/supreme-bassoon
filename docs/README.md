# Vocabee Documentation

**Last Updated:** November 24, 2024
**Project Status:** ✅ M1 Complete | ✅ M2 Complete | ✅ M2.5 Complete | 📋 Planning 2025 Roadmap

---

## Quick Links

- **[ROADMAP 2025](ROADMAP_2025.md)** - ⭐ NEW - Strategic planning for 2025 (AI quality, learning loop, new features)
- **[Project Structure](PROJECT_STRUCTURE.md)** - Complete technical architecture overview
- **[Exercise Types Reference](exercise-types-reference.md)** - Complete guide to all 6 exercise types
- **[Course System](course-system.md)** - Structured courses with AI generation and multi-speaker audio
- **[Study Mode Design](study-mode-design.md)** - Spaced repetition flashcard system
- **[French A1 Curriculum](french_a1_plan.md)** - 10-module French A1 course structure

---

## What is Vocabee?

Vocabee is an AI-powered language learning platform that uses **narrative-driven storytelling** combined with **spaced
repetition** and **multimedia content** to create immersive learning experiences.

### Key Features

- 📚 **AI-Generated Courses**: Complete courses with dialogues, stories, and contextual exercises
- 🎭 **Character Consistency**: Realistic character portraits with consistent appearance across all images
- 🎙️ **Multi-Speaker Audio**: Natural dialogues with 31 Gemini TTS voices
- 🖼️ **Scene Illustrations**: AI-generated images that bring stories to life
- 📝 **6 Exercise Types**: Interactive exercises embedded in course content
- 🧠 **Spaced Repetition**: SM-2 algorithm for optimal vocabulary retention
- 📖 **Reading Library**: Curated texts with integrated vocabulary lookup

---

## Documentation by Status

### ✅ Completed Features

#### [Structured Course System](course-system.md)

- **Status**: ✅ COMPLETED (November 2024)
- **Description**: Complete AI-powered course generation and management system
- **Key Features**:
    - Course/Module/Episode hierarchy
    - Multi-speaker dialogues with voice assignment
    - AI-generated content (syllabus, episodes, exercises, images)
    - Character consistency system with reference images
    - JSON import/export for content management
- **Database**: `courses`, `modules`, `episodes`, `episode_content_items` tables
- **See also**: [Milestone M2.5 Summary](milestones/M2.5_COMPLETION_SUMMARY.md)

#### [Character Consistency System](course-system.md#character-profiles)

- **Status**: ✅ COMPLETED (November 2024)
- **Description**: Generate consistent character appearances across all episode images
- **Key Features**:
    - Character profile extraction from dialogues
    - AI-generated character descriptions and personalities
    - Reference portrait images for each character
    - Scene images use character references (Gemini 3 Pro)
    - Display in voice assignment interface
- **Models**: CharReferenceProfile, CharacterProfileService, ImageGenerationService

#### [Audio Integration](archive/completed-features/audio-integration-plan.md)

- **Status**: ✅ COMPLETED (November 2024)
- **Description**: File upload system with MinIO, TTS pipeline with Gemini API, MP3 generation
- **Key Features**:
    - Multi-speaker TTS with 31 Gemini voices
    - Audio player component
    - Admin upload UI
    - 67% size reduction with MP3
- **Next Steps**: Consider word-level synchronization for advanced reading features

#### [Reading Texts System](archive/completed-features/reading-texts-plan.md)

- **Status**: ✅ COMPLETED (January 2024)
- **Description**: Complete reading text management with progress tracking
- **Key Features**: Text library, 3D book reader, drag-and-drop import, page-level progress
- **Database**: `reading_texts`, `user_reading_progress` tables

#### [Study Mode / Spaced Repetition](study-mode-design.md)

- **Status**: ✅ COMPLETED (January 2024)
- **Description**: Flashcard study system with SM-2 spaced repetition algorithm
- **Key Features**: Daily reviews, session tracking, adaptive intervals, progress statistics
- **Database**: `study_sessions`, `study_session_items`, `study_session_attempts` tables

#### [Exercise System](archive/completed-features/exercise-system-design.md)

- **Status**: ✅ COMPLETED (January 2024)
- **Description**: Interactive exercises aligned with language curriculum
- **Exercise Types Implemented** (See [Exercise Types Reference](exercise-types-reference.md)):
  1. **Multiple Choice** - Vocabulary recognition and translation (5 exercises)
  2. **Fill in the Blank** - Grammar conjugation and vocabulary recall (6 exercises)
  3. **Sentence Scramble** - Word order and syntax practice (6 exercises)
  4. **Matching** - Vocabulary pairs and association (6 exercises)
  5. **Listening Comprehension** - Audio comprehension with 6 generated audio files (6 exercises)
  6. **Cloze Reading** - Context-based grammar and reading (6 exercises)
- **Total**: 35 sample exercises, 45 unit tests, 6 frontend components
- **Integration**: Embedded in episode content workflow

---

### 📋 Planned Features (2025 Roadmap)

See **[ROADMAP 2025](ROADMAP_2025.md)** for detailed planning across three strategic directions:

#### Direction 1: AI Generation Quality (Priority)

- Episode-to-episode story continuity
- Enhanced character personality and dialogue naturalness
- Exercise-content alignment (use actual episode vocabulary)
- Improved image prompt quality
- Cultural authenticity enhancements

#### Direction 2: Learning Loop Enhancement

- Inline vocabulary detection in episodes
- Post-episode review system
- Enhanced study sessions with context
- Progress insights and analytics

#### Direction 3: New Features

- Speaking practice with AI pronunciation feedback
- Writing practice with AI review
- Gamification (streaks, achievements, challenges)

---

## Reference Documents

### [Course System](course-system.md)

- **Type**: Technical Design
- **Description**: Complete course generation and management system
- **Contents**:
    - Course/Module/Episode data model
    - AI generation pipeline (syllabus → episodes → exercises)
    - Multi-speaker audio generation with Gemini TTS
    - Character consistency system
    - JSON import/export format
    - Voice assignment workflow

### [French A1 Curriculum Plan](french_a1_plan.md)
- **Type**: Curriculum Design
- **Description**: 10-module structure for French A1 learning (Sophie's Parisian Journey)
- **Purpose**: Foundation for course content and exercise system
- **Modules**: Greetings → Daily Routine → Past Tense → Synthesis
- **Status**: Complete course with 24 episodes, 240+ exercises

### [Exercise Types Reference](exercise-types-reference.md)

- **Type**: Developer Reference
- **Description**: Complete specifications for all 6 exercise types
- **Contents**:
    - JSON structure for each type
    - Backend validation rules
    - Frontend component design
    - Sample exercises with solutions
- **Use Case**: Creating new exercises, understanding validation logic

### [Project Structure](PROJECT_STRUCTURE.md)
- **Type**: Technical Reference
- **Description**: Complete architecture documentation
- **Contents**:
  - Backend structure (Kotlin + Spring Boot)
  - Frontend structure (Vue 3 + TypeScript)
  - Database schema (PostgreSQL + Flyway)
  - API endpoints and patterns
  - Development conventions
- **Note**: Needs update for character consistency feature

---

## Implementation Timeline

### M1: MVP (Completed - January 2024)
- User Authentication (OAuth2 + JWT)
- Personal Vocabulary Management
- Reading Interface with Word Lookup
- Spaced Repetition Study System
- Pre-made Word Sets
- Audio Integration & TTS

**Deliverables**: Fully functional language learning app with reading and spaced repetition

### M2: Interactive Exercises (Completed - January 2024)

**All 6 Core Exercise Types Implemented:**

1. **Multiple Choice** - Vocabulary and translation recognition
2. **Fill in the Blank** - Grammar conjugation practice
3. **Sentence Scramble** - Word order and syntax building (✅ punctuation bug fixed)
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

**See**: [M2 Completion Summary](milestones/M2_COMPLETION_SUMMARY.md)

### M2.5: Structured Course System (Completed - November 2024)

**Key Achievements:**

- Complete course/module/episode data model
- AI-powered course generation wizard (4 steps)
- Multi-speaker dialogue support with voice assignment
- Episode image generation with scene prompts
- Character consistency system with reference images
- JSON import/export for content management
- Admin creator studio interface
- Complete French A1 course (10 modules, 24 episodes, 240+ exercises)

**Deliverables**:

- Course management API and admin UI
- Character profile generation service
- Enhanced image generation with Gemini 3 Pro
- Voice assignment interface with character images
- Async course import with progress tracking
- 5 database migrations (V19-V23)

**See**: [M2.5 Completion Summary](milestones/M2.5_COMPLETION_SUMMARY.md)

### M3: 2025 Roadmap (Planning)

See **[ROADMAP 2025](ROADMAP_2025.md)** for strategic planning and detailed feature proposals.

---

## Database Schema Overview

### Core Tables (Implemented)

**Authentication:**

- `users` - User accounts with OAuth2 support
- `user_roles` - Role assignments (USER, ADMIN)

**Vocabulary:**

- `languages` - Supported languages (12 languages)
- `words` - Wiktionary entries (4.2M words)
- `definitions` - Word definitions with translations
- `examples` - Example sentences
- `pronunciations` - IPA + audio URLs
- `user_vocabulary` - Personal vocabulary with SRS fields
- `word_sets` - Curated word collections
- `word_set_items` - Words in sets

**Courses:**

- `courses` - Course metadata (slug, name, language, CEFR level)
- `modules` - Course modules with objectives and planning
- `episodes` - Episode content (DIALOGUE, STORY, ARTICLE, AUDIO_LESSON)
- `episode_content_items` - Links exercises and grammar to episodes

**Reading:**

- `reading_texts` - Reading materials with difficulty levels
- `user_reading_progress` - Reading progress tracking

**Study:**
- `study_sessions` - Flashcard review sessions
- `study_session_items` - Words in sessions
- `study_session_attempts` - Individual attempts with SRS data

**Exercises:**
- `exercise_types` - Exercise type definitions (6 types)
- `exercises` - Exercise content (JSONB, 240+ exercises)
- `user_exercise_attempts` - Attempt history and scoring

**Progress Tracking:**

- `user_course_enrollment` - Course subscriptions
- `user_module_progress` - Module completion tracking
- `user_episode_progress` - Episode completion and content progress

---

## API Endpoint Categories

### Public Content

- `/api/courses` - Browse courses, modules, episodes (some protected)
- `/api/reading` - Reading texts (public library, protected progress)
- `/api/exercises` - Exercise browsing (public) and attempts (protected)

### User Features (Protected)

- `/api/vocabulary` - Personal vocabulary management
- `/api/study` - Study sessions and flashcard reviews
- `/api/word-sets` - Browse and import word collections
- `/api/users/analytics` - Progress tracking and statistics

### Admin Features (Protected)

- `/api/admin/courses` - Course management and publishing
- `/api/admin/generation` - AI content generation endpoints
- `/api/admin/course-import` - Course import with progress tracking
- `/api/admin/audio-test` - TTS testing interface

### Authentication

- `/api/auth` - Register, login, OAuth2 callback, profile

### Infrastructure

- `/api/files` - File upload to MinIO/S3
- `/api/v1` - Vocabulary search and language listing

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

- Vue 3.5.22
- TypeScript 5.9.0
- PrimeVue 4.4.1
- Pinia 3.0.3 (state management)
- Vite 7.1.11
- Vue Router 4.6.3

**AI/ML Services:**

- Google Gemini 2.5 Pro (text generation, JSON responses)
- Google Gemini 2.5 Flash Image (basic image generation)
- Google Gemini 3 Pro Image Preview (character-consistent images)
- Google Gemini TTS (multi-speaker audio, 31 voices)

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
4. **Frontend**: Types → API service → Store (if needed) → Components → Views
5. **Test**: Unit tests, integration tests, manual testing
6. **Document**: Update relevant docs with implementation notes
7. **Commit**: Descriptive commit with documentation reference

### Documentation Standards

- **Design Docs**: Markdown with clear structure, examples, database schemas
- **Status Headers**: Always include status, dates, and key metrics
- **Code Examples**: Include realistic examples, not just interfaces
- **Migration Path**: Document how features evolve over time
- **Completion Summary**: Add summary section when marking complete
- **Archive**: Move completed planning docs to `archive/completed-features/`

---

## Archived Documentation

Historical milestone documentation and completed feature plans:

**Milestones:**

- [`milestones/M2.5_COMPLETION_SUMMARY.md`](milestones/M2.5_COMPLETION_SUMMARY.md) - Structured course system completion
- [`milestones/M2_COMPLETION_SUMMARY.md`](milestones/M2_COMPLETION_SUMMARY.md) - Exercise system completion

**Completed Feature Plans:**

- [`archive/completed-features/audio-integration-plan.md`](archive/completed-features/audio-integration-plan.md)
- [`archive/completed-features/reading-texts-plan.md`](archive/completed-features/reading-texts-plan.md)
- [`archive/completed-features/exercise-system-design.md`](archive/completed-features/exercise-system-design.md)
- [
  `archive/completed-features/exercise-implementation-plan.md`](archive/completed-features/exercise-implementation-plan.md)
- [`archive/completed-features/exercise-import-plan.md`](archive/completed-features/exercise-import-plan.md)

**Updates Archive:**

- [`archive/2025/LATEST_UPDATES.md`](archive/2025/LATEST_UPDATES.md) - M2.5 updates from January 2025

**Future Planning:**

- [`planning/REDESIGN_REQUIREMENTS.md`](planning/REDESIGN_REQUIREMENTS.md) - UI/UX redesign concepts

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

## Contributing to Documentation

When adding new features:

1. **Create design doc** before implementation (or use ROADMAP_2025.md for ideation)
2. **Include**: Problem statement, solution design, database schema, API endpoints, UI concepts
3. **Update** PROJECT_STRUCTURE.md with new components/services
4. **Update** README.md with feature status
5. **Add completion summary** when feature is done
6. **Archive** planning docs to `archive/completed-features/` when implementation is complete
7. **Update** ROADMAP_2025.md to reflect completed work and adjust priorities

---

**Documentation Maintainer:** Development Team
**Questions?** Check PROJECT_STRUCTURE.md first, then ROADMAP_2025.md for future plans

**Last Reviewed:** November 24, 2024
