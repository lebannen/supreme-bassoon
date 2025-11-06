# Vocabee Architecture

## Project Structure

```
vocabee/
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
│   └── extract_languages.py
├── parsed_data/           # Extracted Wiktionary data (JSONL.gz)
├── docker/               # Docker Compose configuration
│   └── docker-compose.yml
└── nginx/                # Nginx configuration
    └── nginx.conf
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

### Frontend Routes (SPA)
- `/` - Home page
- `/learn/:language` - Learning interface
- `/vocabulary/:language` - Vocabulary browser
- All handled by Vue Router

### Backend API Routes
- `GET /api/languages` - List available languages
- `GET /api/words/:language` - Get words for language
- `GET /api/word/:id` - Get word details
- `POST /api/progress` - Save learning progress
- ... more endpoints to be defined

## Data Flow

1. **Data Processing** (One-time)
   - Python scripts extract from Wiktionary XML
   - Output: JSONL.gz files per language
   - Import to PostgreSQL using Spring Boot importer

2. **Learning Flow** (Runtime)
   - User opens frontend (Vue app)
   - Frontend fetches vocabulary via API
   - Backend queries PostgreSQL
   - User progress saved to database
