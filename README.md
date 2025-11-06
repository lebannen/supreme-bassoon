# Vocabee - Language Learning Platform

A language learning application powered by Wiktionary data, featuring vocabulary exercises and AI-generated content.

## Tech Stack

- **Frontend**: Vue 3 + TypeScript + PrimeVue
- **Backend**: Kotlin + Spring Boot 3
- **Database**: PostgreSQL 15
- **Deployment**: Docker + nginx

## Project Structure

```
vocabee/
├── frontend/          # Vue.js application
├── backend/           # Spring Boot API
├── scripts/           # Python data processing
├── parsed_data/       # Extracted Wiktionary data (4.2M entries)
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

- `GET /api/languages` - List available languages
- `GET /api/words/:language` - Get words for a language
- `GET /api/word/:id` - Get word details with definitions, pronunciations, etc.
- `POST /api/progress` - Save user learning progress
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
