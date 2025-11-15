# Exercise Import Backend Endpoint Plan

**Status:** 📋 PLANNED
**Created:** January 2025
**Purpose:** Backend endpoint to import exercise JSON files with server-side audio generation

---

## Overview

Create a backend admin endpoint that imports exercise JSON files and generates audio files server-side, eliminating the need for external scripts.

---

## Architecture

### 1. **Import Endpoint**

**Endpoint**: `POST /api/admin/exercises/import`

**Request**:
```json
{
  "moduleFile": "module_1_exercises.json",
  "generateAudio": true,
  "overwriteExisting": false
}
```

**OR** direct JSON upload:
```json
{
  "content": { /* entire module JSON */ },
  "generateAudio": true
}
```

**Response**:
```json
{
  "imported": 8,
  "skipped": 0,
  "audioGenerated": 1,
  "audioFailed": 0,
  "errors": [],
  "exercises": [
    {
      "id": 123,
      "title": "Listening: 'Salut'",
      "type": "listening",
      "audioUrl": "/audio/fr/module_1/salut.mp3",
      "status": "imported"
    }
  ]
}
```

---

### 2. **Audio Generation Service**

**New Kotlin Service**: `AudioGenerationService`

**Responsibilities**:
- Generate audio from text using Gemini TTS
- Save MP3 files to local storage or MinIO/S3
- Return audio URL for database storage
- Handle retries and errors

**Key Methods**:
```kotlin
interface AudioGenerationService {
    // Generate audio and return URL
    fun generateAudio(
        transcript: String,
        languageCode: String,
        voice: String = "Leda"
    ): String

    // Check if audio file exists
    fun audioExists(audioUrl: String): Boolean
}
```

---

### 3. **File Storage Strategy**

**Option A: Local File Storage (Simpler)**
- Save to `frontend/public/audio/` directly
- Serve via frontend static files
- Good for development/small scale

**Option B: MinIO/S3 (Production-ready)**
- Upload to MinIO (already configured in project)
- Return S3-compatible URL
- Better for scalability and backups

**Recommended**: Start with Option A, design for easy migration to Option B

---

## Implementation Plan

### Phase 1: Core Import Service

**File**: `backend/src/main/kotlin/com/vocabee/service/ExerciseImportService.kt`

```kotlin
@Service
class ExerciseImportService(
    private val exerciseService: ExerciseService,
    private val exerciseTypeRepository: ExerciseTypeRepository,
    private val audioGenerationService: AudioGenerationService
) {

    fun importFromJson(
        moduleData: ModuleExerciseData,
        generateAudio: Boolean = true
    ): ImportResult {
        // 1. Parse JSON structure
        // 2. For each exercise:
        //    a. If listening type and generateAudio = true:
        //       - Extract transcript from content
        //       - Generate audio
        //       - Add audioUrl to content
        //    b. Create Exercise entity
        //    c. Save to database
        // 3. Return summary
    }
}
```

**Data Classes**:
```kotlin
data class ModuleExerciseData(
    val module: Int,
    val title: String,
    val cefrLevel: String,
    val languageCode: String = "fr",
    val exercises: List<ExerciseData>
)

data class ExerciseData(
    val type: String,
    val title: String,
    val instructions: String,
    val content: JsonNode
)

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val audioGenerated: Int,
    val audioFailed: Int,
    val errors: List<String>,
    val exercises: List<ImportedExercise>
)
```

---

### Phase 2: Audio Generation Service

**File**: `backend/src/main/kotlin/com/vocabee/service/AudioGenerationService.kt`

**Implementation Options**:

**Option A: Direct Gemini API in Kotlin**
```kotlin
@Service
class GeminiAudioGenerationService(
    @Value("\${gemini.api.key}") private val apiKey: String,
    @Value("\${audio.storage.path}") private val audioPath: String
) : AudioGenerationService {

    override fun generateAudio(
        transcript: String,
        languageCode: String,
        voice: String
    ): String {
        // 1. Call Gemini TTS API
        // 2. Save MP3 to storage
        // 3. Return URL path
    }
}
```

**Option B: Shell out to Python script** (reuse existing `gemini_tts.py`)
```kotlin
@Service
class PythonAudioGenerationService : AudioGenerationService {

    override fun generateAudio(
        transcript: String,
        languageCode: String,
        voice: String
    ): String {
        // Execute Python script
        val process = ProcessBuilder(
            "python3",
            "scripts/genai/gemini_tts.py",
            "--text", transcript,
            "--output", outputPath,
            "--voice", voice
        ).start()
        // Handle result
    }
}
```

**Recommended**: Option B initially (reuse existing working code), migrate to Option A later

---

### Phase 3: Admin Controller

**File**: `backend/src/main/kotlin/com/vocabee/web/api/admin/ExerciseImportController.kt`

```kotlin
@RestController
@RequestMapping("/api/admin/exercises")
@PreAuthorize("hasRole('ADMIN')")
class ExerciseImportController(
    private val importService: ExerciseImportService
) {

    @PostMapping("/import")
    fun importExercises(
        @RequestBody request: ImportRequest
    ): ResponseEntity<ImportResult> {
        // Parse JSON content
        // Call importService
        // Return result
    }

    @PostMapping("/import/file")
    fun importFromFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("generateAudio", defaultValue = "true") generateAudio: Boolean
    ): ResponseEntity<ImportResult> {
        // Read uploaded file
        // Parse JSON
        // Import
    }
}
```

---

### Phase 4: JSON Processing Logic

**Handling Listening Exercises**:

**Current JSON format** (from `module_1_exercises.json`):
```json
{
  "type": "listening",
  "content": {
    "audioUrl": "/audio/fr/a1/m1/salut.mp3",
    "transcript": "Salut",
    "question": "What greeting did you hear?",
    ...
  }
}
```

**Processing Logic**:
1. Detect exercise type = "listening"
2. Check if `content.transcript` exists
3. **Ignore `content.audioUrl`** from JSON (if present)
4. If `generateAudio = true`:
   - Extract `transcript`, `languageCode` from content or module level
   - Call `audioGenerationService.generateAudio()`
   - **Replace** `content.audioUrl` with generated URL
5. Save exercise to database with new audioUrl

**Audio URL Convention**:
```
/audio/{languageCode}/module_{moduleNumber}/{sanitized_transcript}.mp3
```

Same sanitization rules as before (lowercase, remove special chars, etc.)

---

## File Storage Configuration

**application.yml**:
```yaml
audio:
  storage:
    type: local  # or 'minio'
    path: ../frontend/public/audio  # for local
    base-url: /audio  # URL prefix

gemini:
  api:
    key: ${GEMINI_API_KEY}
  tts:
    model: gemini-2.5-flash-preview-tts
    rate-limit-ms: 20000  # 20 seconds between calls
```

---

## Error Handling

### Audio Generation Failures

**Strategy**: Continue importing, mark audio as failed

```json
{
  "imported": 8,
  "audioGenerated": 0,
  "audioFailed": 1,
  "errors": [
    "Failed to generate audio for 'Listening: Salut': Transcript too short"
  ]
}
```

**Database**: Exercise still saved, but `audioUrl` could be:
- Null (to be generated later)
- Placeholder URL
- Error marker

**Retry Mechanism**: Separate endpoint to regenerate failed audio
```
POST /api/admin/exercises/{id}/regenerate-audio
```

---

## Security & Permissions

**Admin Only**:
- `@PreAuthorize("hasRole('ADMIN')")`
- Only authenticated admin users can import

**Validation**:
- File size limits (e.g., max 10MB for JSON)
- Module number validation
- Exercise type validation (must exist in `exercise_types` table)
- CEFR level validation

---

## Frontend Integration (Optional Phase 5)

**Admin UI**: New view for importing exercises

**Features**:
- File upload for JSON
- Preview exercises before import
- Toggle "Generate Audio" checkbox
- Show import progress
- Display results (success/errors)

**Location**: `/admin/exercises/import`

---

## Migration Path

**For existing exercises in database**:
- Already have `audioUrl` and audio files
- No changes needed

**For new `module_1_exercises.json`**:
- Upload via endpoint
- Backend generates audio
- Ignores hardcoded `audioUrl` in JSON
- Uses `transcript` field instead

---

## Testing Strategy

### Unit Tests
- `ExerciseImportServiceTest`: Test JSON parsing, exercise creation
- `AudioGenerationServiceTest`: Mock audio generation
- `ExerciseImportControllerTest`: Test endpoint security, validation

### Integration Tests
- Import full module JSON
- Verify exercises created in database
- Verify audio files created (or mock calls)

### Manual Testing
1. Upload `module_1_exercises.json` via Postman
2. Verify 8 exercises imported
3. Check audio file created for listening exercise
4. Test in frontend

---

## Implementation Order

1. **Phase 1**: Create `ExerciseImportService` (no audio yet)
   - Parse JSON
   - Create exercises
   - Return import result

2. **Phase 2**: Add `AudioGenerationService` (Python wrapper)
   - Shell out to existing `gemini_tts.py`
   - Save files locally
   - Return URL

3. **Phase 3**: Integrate audio into import
   - Detect listening exercises
   - Extract transcript
   - Generate audio
   - Update content JSON

4. **Phase 4**: Create admin controller endpoint
   - POST `/api/admin/exercises/import`
   - Security, validation
   - Error handling

5. **Phase 5** (Optional): Frontend admin UI

---

## Questions for Decision

1. **Audio Storage**: Local files (`frontend/public/audio/`) or MinIO/S3?

2. **Audio Generation**: Python wrapper (quick) or native Kotlin Gemini client (cleaner)?

3. **JSON Format**: Keep current format with `audioUrl` + `transcript`, or change to something else?

4. **Overwrite Behavior**: If exercise with same title exists, skip or overwrite?

5. **Admin UI**: Build it now, or just API endpoint first?

6. **Authentication**: Use existing admin role, or create new "content_manager" role?

---

**Document Created:** January 2025
**Status:** Ready for implementation
