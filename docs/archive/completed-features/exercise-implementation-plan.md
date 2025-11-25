# Exercise System - Implementation Plan

**Status:** ✅ **COMPLETE** - All 6 Exercise Types Implemented
**Approach:** One exercise type at a time
**Order:** Backend → Tests → Frontend
**Design:** Simple/functional first, polish later
**Last Updated:** January 2025

---

## Implementation Order

All exercise types have been successfully implemented in this order:

1. ✅ **Multiple Choice (Vocabulary)** - COMPLETE - Simplest, most valuable
2. ✅ **Fill-in-the-Blank (Conjugation)** - COMPLETE - Tests grammar, uses Wiktionary data
3. ✅ **Sentence Scramble** - COMPLETE - Word order practice
4. ✅ **Matching (Image/Text)** - COMPLETE - Vocabulary reinforcement
5. ✅ **Listening Comprehension** - COMPLETE - Uses Gemini TTS
6. ✅ **Cloze Reading** - COMPLETE - Reading comprehension

---

## Phase 0: Infrastructure Setup (Week 1) ✅ **COMPLETE**

### Backend Foundation

#### Task 0.1: Database Migration

**File:** `backend/src/main/resources/db/migration/V12__Create_exercise_tables.sql`

```sql
-- Exercise types (system table)
CREATE TABLE exercise_types (
    id SERIAL PRIMARY KEY,
    type_key VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    difficulty_level VARCHAR(10),
    requires_audio BOOLEAN DEFAULT false,
    requires_images BOOLEAN DEFAULT false,
    interaction_type VARCHAR(50)
);

-- Insert initial types
INSERT INTO exercise_types (type_key, display_name, category, interaction_type) VALUES
('multiple_choice', 'Multiple Choice', 'vocabulary', 'select'),
('fill_in_blank', 'Fill in the Blank', 'grammar', 'type'),
('sentence_scramble', 'Sentence Scramble', 'grammar', 'drag'),
('matching', 'Matching', 'vocabulary', 'drag'),
('listening', 'Listening Comprehension', 'listening', 'select'),
('cloze_reading', 'Cloze Reading', 'reading', 'select');

-- Exercises table
CREATE TABLE exercises (
    id BIGSERIAL PRIMARY KEY,
    exercise_type_id INT NOT NULL REFERENCES exercise_types(id),
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),

    module_number INT,
    topic VARCHAR(100),
    cefr_level VARCHAR(10) DEFAULT 'A1',

    title VARCHAR(255) NOT NULL,
    instructions TEXT NOT NULL,
    content JSONB NOT NULL,

    estimated_duration_seconds INT DEFAULT 60,
    points_value INT DEFAULT 10,
    difficulty_rating DECIMAL(2,1) DEFAULT 1.0,

    is_published BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(50) DEFAULT 'system'
);

CREATE INDEX idx_exercises_language ON exercises(language_code, cefr_level);
CREATE INDEX idx_exercises_module ON exercises(module_number, topic);
CREATE INDEX idx_exercises_type ON exercises(exercise_type_id);
CREATE INDEX idx_exercises_published ON exercises(is_published);

-- User attempts
CREATE TABLE user_exercise_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,

    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    duration_seconds INT,

    score DECIMAL(5,2),
    max_score DECIMAL(5,2) DEFAULT 100,
    is_correct BOOLEAN,

    user_responses JSONB,
    correct_answers JSONB,

    hints_used INT DEFAULT 0,
    attempts_count INT DEFAULT 1
);

CREATE INDEX idx_user_attempts_user ON user_exercise_attempts(user_id);
CREATE INDEX idx_user_attempts_exercise ON user_exercise_attempts(exercise_id);
CREATE INDEX idx_user_attempts_completed ON user_exercise_attempts(user_id, completed_at);

-- Exercise collections (optional for Phase 0, can add later)
CREATE TABLE exercise_collections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),
    module_number INT,
    cefr_level VARCHAR(10) DEFAULT 'A1',
    is_published BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE exercise_collection_items (
    id BIGSERIAL PRIMARY KEY,
    collection_id BIGINT NOT NULL REFERENCES exercise_collections(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    is_optional BOOLEAN DEFAULT false,
    UNIQUE(collection_id, exercise_id)
);

CREATE INDEX idx_collection_items_collection ON exercise_collection_items(collection_id, order_index);
```

**Checklist:**

- [x] Create migration file (V12__Create_exercise_tables.sql)
- [x] Test migration locally
- [x] Verify all indexes created

**Status:** ✅ Complete - Migration deployed

---

#### Task 0.2: Domain Models

**Files:**

- `backend/src/main/kotlin/com/vocabee/domain/model/ExerciseType.kt`
- `backend/src/main/kotlin/com/vocabee/domain/model/Exercise.kt`
- `backend/src/main/kotlin/com/vocabee/domain/model/UserExerciseAttempt.kt`

```kotlin
// ExerciseType.kt
@Entity
@Table(name = "exercise_types")
data class ExerciseType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(unique = true, nullable = false, length = 50)
    val typeKey: String,

    @Column(nullable = false, length = 100)
    val displayName: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(nullable = false, length = 50)
    val category: String,

    @Column(length = 10)
    val difficultyLevel: String? = null,

    @Column(nullable = false)
    val requiresAudio: Boolean = false,

    @Column(nullable = false)
    val requiresImages: Boolean = false,

    @Column(length = 50)
    val interactionType: String? = null
)

// Exercise.kt
@Entity
@Table(name = "exercises")
data class Exercise(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_type_id", nullable = false)
    val exerciseType: ExerciseType,

    @Column(nullable = false, length = 10)
    val languageCode: String,

    val moduleNumber: Int? = null,

    @Column(length = 100)
    val topic: String? = null,

    @Column(length = 10)
    val cefrLevel: String = "A1",

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val instructions: String,

    @Column(nullable = false, columnDefinition = "JSONB")
    @Type(JsonBinaryType::class)
    val content: JsonNode,

    val estimatedDurationSeconds: Int = 60,
    val pointsValue: Int = 10,
    val difficultyRating: Double = 1.0,

    val isPublished: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: String = "system"
)

// UserExerciseAttempt.kt
@Entity
@Table(name = "user_exercise_attempts")
data class UserExerciseAttempt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val exerciseId: Long,

    @Column(nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),

    var completedAt: LocalDateTime? = null,
    var durationSeconds: Int? = null,

    var score: Double? = null,
    val maxScore: Double = 100.0,
    var isCorrect: Boolean? = null,

    @Column(columnDefinition = "JSONB")
    @Type(JsonBinaryType::class)
    var userResponses: JsonNode? = null,

    @Column(columnDefinition = "JSONB")
    @Type(JsonBinaryType::class)
    var correctAnswers: JsonNode? = null,

    var hintsUsed: Int = 0,
    val attemptsCount: Int = 1
)
```

**Checklist:**

- [x] Create domain models
- [x] Add JSONB type handler (using @JdbcTypeCode(SqlTypes.JSON))
- [x] Test entity mapping

**Status:** ✅ Complete

---

#### Task 0.3: Repositories

**File:** `backend/src/main/kotlin/com/vocabee/domain/repository/ExerciseRepository.kt`

```kotlin
@Repository
interface ExerciseTypeRepository : JpaRepository<ExerciseType, Int> {
    fun findByTypeKey(typeKey: String): ExerciseType?
}

@Repository
interface ExerciseRepository : JpaRepository<Exercise, Long> {
    fun findByLanguageCodeAndIsPublished(
        languageCode: String,
        isPublished: Boolean = true
    ): List<Exercise>

    fun findByLanguageCodeAndModuleNumber(
        languageCode: String,
        moduleNumber: Int
    ): List<Exercise>

    fun findByExerciseType_TypeKey(typeKey: String): List<Exercise>

    @Query("""
        SELECT e FROM Exercise e
        WHERE e.languageCode = :languageCode
        AND (:moduleNumber IS NULL OR e.moduleNumber = :moduleNumber)
        AND (:topic IS NULL OR e.topic = :topic)
        AND (:typeKey IS NULL OR e.exerciseType.typeKey = :typeKey)
        AND e.isPublished = true
        ORDER BY e.moduleNumber, e.topic, e.id
    """)
    fun findByFilters(
        @Param("languageCode") languageCode: String,
        @Param("moduleNumber") moduleNumber: Int?,
        @Param("topic") topic: String?,
        @Param("typeKey") typeKey: String?
    ): List<Exercise>
}

@Repository
interface UserExerciseAttemptRepository : JpaRepository<UserExerciseAttempt, Long> {
    fun findByUserId(userId: Long): List<UserExerciseAttempt>

    fun findByUserIdAndExerciseId(userId: Long, exerciseId: Long): List<UserExerciseAttempt>

    fun findByUserIdOrderByStartedAtDesc(userId: Long): List<UserExerciseAttempt>

    @Query("""
        SELECT COUNT(DISTINCT a.exerciseId)
        FROM UserExerciseAttempt a
        WHERE a.userId = :userId AND a.isCorrect = true
    """)
    fun countCompletedExercisesByUser(@Param("userId") userId: Long): Long
}
```

**Checklist:**

- [x] Create repository interfaces
- [x] Test query methods
- [x] Verify JSONB queries work

**Status:** ✅ Complete

---

#### Task 0.4: DTOs

**File:** `backend/src/main/kotlin/com/vocabee/web/dto/ExerciseDto.kt`

```kotlin
// Exercise DTOs
data class ExerciseSummaryDto(
    val id: Long,
    val type: String,
    val title: String,
    val languageCode: String,
    val moduleNumber: Int?,
    val topic: String?,
    val difficultyRating: Double,
    val estimatedDurationSeconds: Int,
    val pointsValue: Int
)

data class ExerciseDto(
    val id: Long,
    val type: String,
    val languageCode: String,
    val moduleNumber: Int?,
    val topic: String?,
    val cefrLevel: String,
    val title: String,
    val instructions: String,
    val content: JsonNode,
    val estimatedDurationSeconds: Int,
    val pointsValue: Int,
    val difficultyRating: Double
)

data class SubmitAttemptRequest(
    val userResponses: JsonNode,
    val durationSeconds: Int?,
    val hintsUsed: Int = 0
)

data class ValidationResult(
    val isCorrect: Boolean,
    val score: Double,
    val feedback: String,
    val correctAnswers: JsonNode? = null
)

data class AttemptResultDto(
    val attemptId: Long,
    val isCorrect: Boolean,
    val score: Double,
    val feedback: String,
    val correctAnswers: JsonNode? = null,
    val completedAt: LocalDateTime
)

data class UserProgressDto(
    val exerciseId: Long,
    val attemptsCount: Int,
    val bestScore: Double?,
    val lastAttemptAt: LocalDateTime?,
    val isCompleted: Boolean
)

// Extension functions
fun Exercise.toSummaryDto() = ExerciseSummaryDto(
    id = id!!,
    type = exerciseType.typeKey,
    title = title,
    languageCode = languageCode,
    moduleNumber = moduleNumber,
    topic = topic,
    difficultyRating = difficultyRating,
    estimatedDurationSeconds = estimatedDurationSeconds,
    pointsValue = pointsValue
)

fun Exercise.toDto() = ExerciseDto(
    id = id!!,
    type = exerciseType.typeKey,
    languageCode = languageCode,
    moduleNumber = moduleNumber,
    topic = topic,
    cefrLevel = cefrLevel,
    title = title,
    instructions = instructions,
    content = content,
    estimatedDurationSeconds = estimatedDurationSeconds,
    pointsValue = pointsValue,
    difficultyRating = difficultyRating
)
```

**Checklist:**

- [x] Create DTO classes
- [x] Add extension functions
- [x] Test serialization

**Status:** ✅ Complete

---

#### Task 0.5: Validation Service (Generic Framework)

**File:** `backend/src/main/kotlin/com/vocabee/service/ExerciseValidationService.kt`

```kotlin
@Service
class ExerciseValidationService {

    fun validate(
        exerciseType: String,
        content: JsonNode,
        userResponses: JsonNode
    ): ValidationResult {
        return when (exerciseType) {
            "multiple_choice" -> validateMultipleChoice(content, userResponses)
            "fill_in_blank" -> validateFillInBlank(content, userResponses)
            "sentence_scramble" -> validateSentenceScramble(content, userResponses)
            "matching" -> validateMatching(content, userResponses)
            "listening" -> validateListening(content, userResponses)
            "cloze_reading" -> validateClozeReading(content, userResponses)
            else -> throw UnsupportedExerciseTypeException("Unsupported exercise type: $exerciseType")
        }
    }

    // Placeholder methods - will implement each as we build that exercise type
    private fun validateMultipleChoice(content: JsonNode, userResponses: JsonNode): ValidationResult {
        throw NotImplementedError("Multiple choice validation not yet implemented")
    }

    private fun validateFillInBlank(content: JsonNode, userResponses: JsonNode): ValidationResult {
        throw NotImplementedError("Fill in blank validation not yet implemented")
    }

    private fun validateSentenceScramble(content: JsonNode, userResponses: JsonNode): ValidationResult {
        throw NotImplementedError("Sentence scramble validation not yet implemented")
    }

    private fun validateMatching(content: JsonNode, userResponses: JsonNode): ValidationResult {
        throw NotImplementedError("Matching validation not yet implemented")
    }

    private fun validateListening(content: JsonNode, userResponses: JsonNode): ValidationResult {
        throw NotImplementedError("Listening validation not yet implemented")
    }

    private fun validateClozeReading(content: JsonNode, userResponses: JsonNode): ValidationResult {
        throw NotImplementedError("Cloze reading validation not yet implemented")
    }
}

class UnsupportedExerciseTypeException(message: String) : RuntimeException(message)
```

**Checklist:**

- [x] Create validation service skeleton
- [x] Add exception handling
- [x] Prepare for incremental implementation
- [x] Implement Multiple Choice validation

**Status:** ✅ Complete - Multiple Choice validation implemented, framework ready for other types

---

#### Task 0.6: Exercise Service

**File:** `backend/src/main/kotlin/com/vocabee/service/ExerciseService.kt`

```kotlin
@Service
class ExerciseService(
    private val exerciseRepository: ExerciseRepository,
    private val attemptRepository: UserExerciseAttemptRepository,
    private val validationService: ExerciseValidationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getExercises(
        languageCode: String,
        moduleNumber: Int? = null,
        topic: String? = null,
        typeKey: String? = null
    ): List<ExerciseSummaryDto> {
        return exerciseRepository.findByFilters(languageCode, moduleNumber, topic, typeKey)
            .map { it.toSummaryDto() }
    }

    fun getExerciseById(id: Long): ExerciseDto {
        val exercise = exerciseRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Exercise not found: $id") }
        return exercise.toDto()
    }

    @Transactional
    fun submitAttempt(
        userId: Long,
        exerciseId: Long,
        request: SubmitAttemptRequest
    ): AttemptResultDto {
        logger.info("Submitting attempt for user $userId, exercise $exerciseId")

        // Get exercise
        val exercise = exerciseRepository.findById(exerciseId)
            .orElseThrow { EntityNotFoundException("Exercise not found: $exerciseId") }

        // Validate response
        val validation = validationService.validate(
            exercise.exerciseType.typeKey,
            exercise.content,
            request.userResponses
        )

        // Create attempt record
        val attempt = UserExerciseAttempt(
            userId = userId,
            exerciseId = exerciseId,
            completedAt = LocalDateTime.now(),
            durationSeconds = request.durationSeconds,
            score = validation.score,
            isCorrect = validation.isCorrect,
            userResponses = request.userResponses,
            correctAnswers = validation.correctAnswers,
            hintsUsed = request.hintsUsed
        )

        val savedAttempt = attemptRepository.save(attempt)

        return AttemptResultDto(
            attemptId = savedAttempt.id!!,
            isCorrect = validation.isCorrect,
            score = validation.score,
            feedback = validation.feedback,
            correctAnswers = validation.correctAnswers,
            completedAt = savedAttempt.completedAt!!
        )
    }

    fun getUserProgress(userId: Long, exerciseId: Long): UserProgressDto {
        val attempts = attemptRepository.findByUserIdAndExerciseId(userId, exerciseId)

        return UserProgressDto(
            exerciseId = exerciseId,
            attemptsCount = attempts.size,
            bestScore = attempts.mapNotNull { it.score }.maxOrNull(),
            lastAttemptAt = attempts.maxOfOrNull { it.startedAt },
            isCompleted = attempts.any { it.isCorrect == true }
        )
    }

    fun createExercise(exercise: Exercise): Exercise {
        return exerciseRepository.save(exercise)
    }
}
```

**Checklist:**

- [x] Create exercise service
- [x] Add transaction support
- [x] Test service methods

**Status:** ✅ Complete

---

#### Task 0.7: API Controller

**File:** `backend/src/main/kotlin/com/vocabee/web/api/ExerciseController.kt`

```kotlin
@RestController
@RequestMapping("/api/exercises")
class ExerciseController(
    private val exerciseService: ExerciseService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getExercises(
        @RequestParam languageCode: String,
        @RequestParam(required = false) module: Int?,
        @RequestParam(required = false) topic: String?,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<List<ExerciseSummaryDto>> {
        logger.info("Getting exercises: lang=$languageCode, module=$module, topic=$topic, type=$type")
        val exercises = exerciseService.getExercises(languageCode, module, topic, type)
        return ResponseEntity.ok(exercises)
    }

    @GetMapping("/{id}")
    fun getExercise(@PathVariable id: Long): ResponseEntity<ExerciseDto> {
        logger.info("Getting exercise: $id")
        val exercise = exerciseService.getExerciseById(id)
        return ResponseEntity.ok(exercise)
    }

    @PostMapping("/{id}/attempt")
    fun submitAttempt(
        @PathVariable id: Long,
        @RequestBody request: SubmitAttemptRequest,
        authentication: Authentication
    ): ResponseEntity<AttemptResultDto> {
        val userId = (authentication.principal as org.springframework.security.core.userdetails.User)
            .username.toLong()

        logger.info("User $userId submitting attempt for exercise $id")

        val result = exerciseService.submitAttempt(userId, id, request)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{id}/progress")
    fun getProgress(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<UserProgressDto> {
        val userId = (authentication.principal as org.springframework.security.core.userdetails.User)
            .username.toLong()

        val progress = exerciseService.getUserProgress(userId, id)
        return ResponseEntity.ok(progress)
    }
}
```

**Checklist:**

- [x] Create controller
- [x] Add authentication (JWT-based)
- [x] Test endpoints

**Status:** ✅ Complete

---

### Frontend Foundation

#### Task 0.8: TypeScript Types

**File:** `frontend/src/types/exercise.ts`

```typescript
export interface ExerciseSummary {
  id: number
  type: string
  title: string
  languageCode: string
  moduleNumber: number | null
  topic: string | null
  difficultyRating: number
  estimatedDurationSeconds: number
  pointsValue: number
}

export interface Exercise {
  id: number
  type: string
  languageCode: string
  moduleNumber: number | null
  topic: string | null
  cefrLevel: string
  title: string
  instructions: string
  content: any  // Will vary by exercise type
  estimatedDurationSeconds: number
  pointsValue: number
  difficultyRating: number
}

export interface SubmitAttemptRequest {
  userResponses: any
  durationSeconds: number | null
  hintsUsed: number
}

export interface AttemptResult {
  attemptId: number
  isCorrect: boolean
  score: number
  feedback: string
  correctAnswers: any | null
  completedAt: string
}

export interface UserProgress {
  exerciseId: number
  attemptsCount: number
  bestScore: number | null
  lastAttemptAt: string | null
  isCompleted: boolean
}
```

**Checklist:**

- [x] Create type definitions
- [x] Export from index

**Status:** ✅ Complete

---

#### Task 0.9: Exercise API Composable

**File:** `frontend/src/composables/useExerciseApi.ts`

```typescript
import { ref } from 'vue'
import type { Exercise, ExerciseSummary, SubmitAttemptRequest, AttemptResult, UserProgress } from '@/types/exercise'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export function useExerciseApi() {
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function getExercises(
    languageCode: string,
    filters?: {
      module?: number
      topic?: string
      type?: string
    }
  ): Promise<ExerciseSummary[]> {
    loading.value = true
    error.value = null

    try {
      const params = new URLSearchParams({ languageCode })
      if (filters?.module) params.append('module', filters.module.toString())
      if (filters?.topic) params.append('topic', filters.topic)
      if (filters?.type) params.append('type', filters.type)

      const response = await fetch(`${API_BASE}/api/exercises?${params}`)
      if (!response.ok) throw new Error('Failed to fetch exercises')
      return await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      return []
    } finally {
      loading.value = false
    }
  }

  async function getExercise(id: number): Promise<Exercise | null> {
    loading.value = true
    error.value = null

    try {
      const response = await fetch(`${API_BASE}/api/exercises/${id}`)
      if (!response.ok) throw new Error('Failed to fetch exercise')
      return await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      return null
    } finally {
      loading.value = false
    }
  }

  async function submitAttempt(
    exerciseId: number,
    request: SubmitAttemptRequest
  ): Promise<AttemptResult | null> {
    loading.value = true
    error.value = null

    try {
      const token = localStorage.getItem('jwt_token')
      const response = await fetch(`${API_BASE}/api/exercises/${exerciseId}/attempt`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(request)
      })

      if (!response.ok) throw new Error('Failed to submit attempt')
      return await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      return null
    } finally {
      loading.value = false
    }
  }

  async function getProgress(exerciseId: number): Promise<UserProgress | null> {
    try {
      const token = localStorage.getItem('jwt_token')
      const response = await fetch(`${API_BASE}/api/exercises/${exerciseId}/progress`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (!response.ok) throw new Error('Failed to fetch progress')
      return await response.json()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      return null
    }
  }

  return {
    loading,
    error,
    getExercises,
    getExercise,
    submitAttempt,
    getProgress
  }
}
```

**Checklist:**

- [x] Create composable
- [x] Add authentication headers
- [x] Test API calls

**Status:** ✅ Complete

---

#### Task 0.10: Basic Page Structure

**Files:**

- `frontend/src/views/ExercisesView.vue` (list page)
- `frontend/src/views/ExerciseDetailView.vue` (exercise runner)
- Update `frontend/src/router/index.ts`

```vue
<!-- ExercisesView.vue -->
<template>
  <div class="exercises-container">
    <div class="header">
      <h1>Exercises</h1>
      <p>Practice your language skills with interactive exercises</p>
    </div>

    <div class="filters">
      <Select
        v-model="selectedModule"
        :options="modules"
        optionLabel="label"
        optionValue="value"
        placeholder="All Modules"
      />
      <Select
        v-model="selectedType"
        :options="exerciseTypes"
        optionLabel="label"
        optionValue="value"
        placeholder="All Types"
      />
    </div>

    <div v-if="loading" class="loading">
      <ProgressSpinner />
    </div>

    <div v-else class="exercises-grid">
      <Card
        v-for="exercise in exercises"
        :key="exercise.id"
        class="exercise-card"
        @click="goToExercise(exercise.id)"
      >
        <template #title>{{ exercise.title }}</template>
        <template #subtitle>{{ exercise.type }}</template>
        <template #content>
          <div class="meta">
            <Tag :value="exercise.moduleNumber ? `Module ${exercise.moduleNumber}` : 'General'" />
            <span>{{ exercise.estimatedDurationSeconds }}s</span>
            <span>{{ exercise.pointsValue }} pts</span>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useExerciseApi } from '@/composables/useExerciseApi'
import Card from 'primevue/card'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import ProgressSpinner from 'primevue/progressspinner'

const router = useRouter()
const { getExercises, loading } = useExerciseApi()

const exercises = ref([])
const selectedModule = ref(null)
const selectedType = ref(null)

const modules = [
  { label: 'All Modules', value: null },
  ...Array.from({ length: 10 }, (_, i) => ({ label: `Module ${i + 1}`, value: i + 1 }))
]

const exerciseTypes = [
  { label: 'All Types', value: null },
  { label: 'Multiple Choice', value: 'multiple_choice' },
  { label: 'Fill in the Blank', value: 'fill_in_blank' },
  { label: 'Sentence Scramble', value: 'sentence_scramble' }
]

async function fetchExercises() {
  exercises.value = await getExercises('fr', {
    module: selectedModule.value,
    type: selectedType.value
  })
}

function goToExercise(id: number) {
  router.push(`/exercises/${id}`)
}

watch([selectedModule, selectedType], fetchExercises)

onMounted(fetchExercises)
</script>

<style scoped>
.exercises-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.exercises-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
  margin-top: 2rem;
}

.exercise-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.exercise-card:hover {
  transform: translateY(-4px);
}

.meta {
  display: flex;
  gap: 1rem;
  align-items: center;
}
</style>
```

```typescript
// router/index.ts - add routes
{
  path: '/exercises',
  name: 'exercises',
  component: () => import('../views/ExercisesView.vue')
},
{
  path: '/exercises/:id',
  name: 'exercise-detail',
  component: () => import('../views/ExerciseDetailView.vue'),
  props: true
}
```

**Checklist:**

- [x] Create exercise list view (ExercisesView)
- [x] Create exercise detail view (ExerciseDetailView)
- [x] Add basic styling
- [x] Add routes
- [x] Test navigation

**Status:** ✅ Complete

---

## Phase 1: Multiple Choice Exercise (Week 2) ✅ **COMPLETE**

### Completed Work

**Backend (100% Complete):**

- ✅ Multiple Choice validation logic in `ExerciseValidationService.kt`
- ✅ Database migration V13 with 5 sample exercises
- ✅ Full JSONB support with correct annotations
- ✅ Integration testing with 3 unit tests (all passing)

**Frontend (100% Complete):**

- ✅ `MultipleChoiceExercise.vue` component created
- ✅ Interactive option selection with A/B/C/D labels
- ✅ Hint system implemented
- ✅ Result feedback display
- ✅ Reset functionality
- ✅ Integration with ExerciseDetailView
- ✅ UI polish complete

**Sample Data:**

- ✅ 5 French A1 exercises created in module_2.json
    - Integrated into Episode 1 and Episode 3
    - Covers vocabulary, comprehension, and grammar

---

## Summary

**All Phases Completed:** ✅ **100% COMPLETE**

**Phase 0 - Infrastructure:** ✅ Complete

- ✅ Database schema (V12, V13)
- ✅ Backend infrastructure
- ✅ API endpoints with JWT auth
- ✅ Frontend routing
- ✅ Generic validation framework

**Phase 1 - Multiple Choice:** ✅ Complete

- ✅ Backend validation (3 tests)
- ✅ Frontend component
- ✅ 5 sample exercises

**Phase 2 - Fill in Blank:** ✅ Complete

- ✅ Backend validation (6 tests)
- ✅ Multi-blank support
- ✅ Frontend component
- ✅ 6 sample exercises

**Phase 3 - Sentence Scramble:** ✅ Complete

- ✅ Backend validation (7 tests)
- ✅ Drag-and-drop UI
- ✅ 6 sample exercises

**Phase 4 - Matching:** ✅ Complete

- ✅ Backend validation (7 tests)
- ✅ Click-to-match UI
- ✅ 6 sample exercises

**Phase 5 - Listening:** ✅ Complete

- ✅ Backend validation (10 tests)
- ✅ Audio player UI
- ✅ 6 sample exercises with audio

**Phase 6 - Cloze Reading:** ✅ Complete

- ✅ Backend validation (9 tests)
- ✅ Inline input UI
- ✅ 6 sample exercises

**Total Achievements:**

- 6 exercise types fully implemented
- 35 sample exercises
- 45 unit tests (all passing)
- 7 database migrations (V12-V18)
- 6 frontend Vue components
- 6 audio files generated
- Complete documentation

**Next Steps:**

1. Create additional course content (Modules 3-10)
2. Implement AI-assisted exercise generation
3. Add exercise collections and unlocking system
4. Expand to additional languages

