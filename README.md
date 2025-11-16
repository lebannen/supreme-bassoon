# Vocabee - Language Learning Platform

A comprehensive language learning application powered by Wiktionary data, featuring:
- 🔍 Word lookup from 4M+ Wiktionary entries
- 📚 Interactive reading texts with progress tracking
- 📖 Beautiful paginated book reader with word click definitions
- ✨ Spaced repetition study system
- 🎯 Personal vocabulary management
- 📊 OAuth2 authentication and user profiles
- 🎓 Structured course system with modules and episodes
- 🎮 6 interactive exercise types with validation
- 🔊 Multi-speaker audio generation with Gemini TTS

## Features

### ✅ Completed Features (M1 MVP)

**User Management:**
- User registration and login (email/password + OAuth2 Google)
- JWT-based authentication
- User profiles with language preferences

**Vocabulary System:**
- Browse 4M+ words from Wiktionary across 12 languages
- Add words to personal vocabulary with notes
- View and manage personal word lists
- Pre-made vocabulary sets for structured learning

**Reading System:**
- Browse reading texts filtered by language, level (A1-C2), and topic
- Beautiful 3D book reader with page flip animations
- Click any word to see full Wiktionary definition
- Automatic progress tracking (current page saved)
- Completion tracking
- Import custom texts via drag-and-drop UI

**Study System:**
- Spaced repetition flashcard reviews
- Multiple study modes (standard, detailed)
- Smart scheduling based on performance
- Progress tracking and statistics

**Structured Courses (M2):**
- JSON-based course content management
- Module and episode organization
- Support for STORY, DIALOGUE, ARTICLE, and AUDIO_LESSON episode types
- Multi-speaker dialogue audio with Gemini TTS
- Grammar rules and exercise integration
- Admin interface for course/module management

**Interactive Exercises (M2):**
- 6 exercise types: Multiple Choice, Fill-in-Blank, Sentence Scramble, Matching, Listening, Cloze Reading
- Real-time validation with detailed feedback
- Support for multi-blank fill-in exercises
- Audio generation for listening exercises
- Attempt tracking and progress monitoring
- 35+ sample exercises (French A1 level)

## Tech Stack

- **Frontend**: Vue 3 + TypeScript + PrimeVue
- **Backend**: Kotlin + Spring Boot 3
- **Database**: PostgreSQL 15
- **Authentication**: OAuth2 (Google) + JWT
- **Testing**: JUnit 5, TestContainers, MockK
- **Deployment**: Docker + nginx

## Project Structure

```
vocabee/
├── frontend/          # Vue.js application
│   ├── src/
│   │   ├── components/    # Reusable components
│   │   │   ├── reading/   # Reading text components
│   │   │   ├── study/     # Study session components
│   │   │   └── ...
│   │   ├── views/         # Page components
│   │   ├── composables/   # Composition API logic
│   │   ├── stores/        # Pinia state management
│   │   └── router/        # Vue Router config
├── backend/           # Spring Boot API
│   ├── src/main/kotlin/com/vocabee/
│   │   ├── domain/        # Entities & repositories
│   │   ├── service/       # Business logic
│   │   ├── web/           # Controllers & DTOs
│   │   ├── config/        # Security, CORS, etc.
│   │   └── ...
│   ├── src/main/resources/
│   │   └── db/migration/  # Flyway migrations
│   └── src/test/          # Unit & integration tests
├── scripts/           # Python data processing
├── parsed_data/       # Extracted Wiktionary data (4.2M entries)
├── texts/             # Sample reading texts (JSON format)
├── docs/              # Project documentation
├── nginx/             # nginx configuration
└── docker-compose.yml # Container orchestration
```

## Quick Start

### Development

**Prerequisites:**
- Node.js 18+
- Java 17+
- PostgreSQL 15
- Docker (optional)

**1. Start PostgreSQL**
```bash
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=vocabee \
  -e POSTGRES_USER=vocabee \
  -e POSTGRES_PASSWORD=vocabee \
  --name vocabee-postgres \
  postgres:15
```

**2. Start Backend**
```bash
cd backend
./gradlew bootRun
# Runs on http://localhost:8080
```

**3. Start Frontend**
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

### Production (Docker)

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

Access the app at `http://localhost`

## Available Languages

The parsed Wiktionary data includes:

- **Spanish**: 757,350 entries (9.6 MB)
- **Italian**: 585,747 entries (9.1 MB)
- **Russian**: 423,832 entries (7.6 MB)
- **Portuguese**: 387,334 entries (5.4 MB)
- **French**: 385,723 entries (7.9 MB)
- **German**: 341,984 entries (9.4 MB)
- **Swedish**: 295,178 entries (5.5 MB)
- **Chinese**: 293,220 entries (9.7 MB)
- **Finnish**: 248,737 entries (6.5 MB)
- **Japanese**: 163,889 entries (4.7 MB)
- **Polish**: 161,152 entries (3.8 MB)
- **Dutch**: 132,979 entries (4.5 MB)

**Total**: 4,177,125 entries

## API Endpoints

### Backend API (`/api/`)

**Vocabulary:**
- `GET /api/v1/languages` - List available languages
- `GET /api/v1/words/{language}/{lemma}` - Get word details
- `GET /api/vocabulary` - Get user's personal vocabulary
- `POST /api/vocabulary` - Add word to vocabulary
- `DELETE /api/vocabulary/{wordId}` - Remove word from vocabulary

**Reading Texts:**
- `GET /api/reading/texts` - Browse texts (with optional filters)
- `GET /api/reading/texts/{id}` - Get specific text
- `POST /api/reading/texts/import` - Import text from JSON
- `GET /api/reading/texts/{id}/progress` - Get user's reading progress
- `POST /api/reading/texts/{id}/progress` - Update reading progress
- `POST /api/reading/texts/{id}/complete` - Mark text as completed

**Study Sessions:**
- `POST /api/study/sessions` - Start new study session
- `POST /api/study/sessions/{id}/response` - Submit answer

**Courses & Episodes:**
- `GET /api/courses` - List available courses
- `GET /api/courses/{slug}` - Get course details
- `GET /api/courses/{courseSlug}/modules` - List modules for a course
- `GET /api/modules/{moduleId}/episodes` - List episodes in a module
- `GET /api/episodes/{episodeId}` - Get episode details
- `POST /api/admin/courses/import` - Import course metadata (admin)
- `POST /api/admin/courses/{slug}/modules/import` - Import module with audio generation (admin)

**Exercises:**
- `GET /api/exercises` - Browse exercises (with filters)
- `GET /api/exercises/{id}` - Get exercise details
- `POST /api/exercises/{id}/attempt` - Submit exercise attempt
- `GET /api/exercises/{id}/progress` - Get user progress on exercise

**Authentication:**
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login with credentials
- `GET /oauth2/authorization/google` - Google OAuth2 login

**Health:**
- `GET /actuator/health` - Health check

## Data Processing

The `scripts/` directory contains Python tools for processing Wiktionary dumps:

```bash
# Extract languages from Wiktionary XML
cd scripts
python3 extract_languages.py
```

This generates compressed JSONL files in `parsed_data/` directory.

## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for detailed architecture documentation.

## Development Commands

### Frontend
```bash
npm run dev      # Start dev server
npm run build    # Build for production
npm run preview  # Preview production build
npm run type-check  # TypeScript type checking
npm run lint     # Lint and fix files
```

### Backend
```bash
./gradlew bootRun           # Run application
./gradlew build             # Build JAR
./gradlew test              # Run tests
./gradlew clean             # Clean build
```

### Docker
```bash
docker-compose up           # Start all services
docker-compose up -d        # Start in background
docker-compose down         # Stop services
docker-compose logs -f      # Follow logs
docker-compose build        # Rebuild images
```

## License

MIT
