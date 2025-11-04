# Vocabee Backend

Kotlin + Spring Boot backend for the Vocabee language learning application.

## Tech Stack

- **Kotlin 1.9.21**
- **Spring Boot 3.2.0**
- **PostgreSQL** (database)
- **Flyway** (database migrations)
- **Gradle** (build tool)

## Prerequisites

- JDK 17 or higher
- PostgreSQL 14 or higher
- Gradle 8.x (or use wrapper)

## Database Setup

1. Create PostgreSQL database and user:

```sql
CREATE DATABASE vocabee;
CREATE USER vocabee WITH ENCRYPTED PASSWORD 'vocabee';
GRANT ALL PRIVILEGES ON DATABASE vocabee TO vocabee;
```

2. Update `src/main/resources/application.yml` if needed (defaults to localhost:5432)

## Running the Application

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Or build and run JAR
./gradlew bootJar
java -jar build/libs/vocabee-backend-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Endpoints

### Health Check
```
GET /api/v1/health
```

### Search Words
```
GET /api/v1/search?q={query}&lang={languageCode}
```

Example:
```bash
curl "http://localhost:8080/api/v1/search?q=correr&lang=es"
```

### Get Word Details
```
GET /api/v1/words/{languageCode}/{lemma}
```

Example:
```bash
curl "http://localhost:8080/api/v1/words/es/correr"
```

## Project Structure

```
src/main/kotlin/com/vocabee/
├── VocabeeApplication.kt          # Main application class
├── domain/
│   ├── model/                     # JPA entities
│   │   ├── Word.kt
│   │   ├── WordForm.kt
│   │   ├── Definition.kt
│   │   ├── Pronunciation.kt
│   │   └── Example.kt
│   └── repository/                # Spring Data repositories
│       └── WordRepository.kt
├── service/                       # Business logic
│   └── VocabularyService.kt
├── web/
│   ├── api/                       # REST controllers
│   │   └── VocabularyController.kt
│   └── dto/                       # Data transfer objects
│       └── WordDto.kt
└── config/                        # Configuration classes

src/main/resources/
├── application.yml                # Application configuration
└── db/migration/                  # Flyway migrations
    └── V1__Initial_schema.sql
```

## Database Schema

- **words** - Base vocabulary (lemmas)
- **word_forms** - Inflected forms (conjugations, declensions)
- **definitions** - Word meanings
- **pronunciations** - IPA and audio
- **examples** - Example sentences

## Development

### Running Tests
```bash
./gradlew test
```

### Database Migrations

Migrations are managed by Flyway and run automatically on startup.

To create a new migration:
1. Create file: `src/main/resources/db/migration/V{version}__{description}.sql`
2. Restart application

### IDE Setup

Import as Gradle project in IntelliJ IDEA or Eclipse.

## TODO

- [ ] Add authentication/authorization
- [ ] Add user vocabulary tracking
- [ ] Implement spaced repetition system
- [ ] Add full-text search with PostgreSQL tsvector
- [ ] Add integration tests
- [ ] Add API documentation (Swagger/OpenAPI)
- [ ] Add caching (Redis)
- [ ] Add rate limiting
