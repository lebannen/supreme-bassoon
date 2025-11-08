# Study Mode - Spaced Repetition System (SRS)

## Overview
A flashcard-based study system using spaced repetition to optimize long-term retention. Words are reviewed based on when they're due, with intervals increasing as mastery improves.

## Core Concepts

### Spaced Repetition Algorithm

**Key Principles:**
- Each word has a `next_review_at` timestamp
- Words are selected for study based on earliest review date (overdue first)
- Successful recall increases the review interval
- Failed recall resets to shorter interval

**Interval Progression:**
- **New/Difficult words:** Start at ~20 hours
- **Well-mastered words:** Up to 4-5 weeks
- **Failed reviews:** Reset to short interval (e.g., 20-24 hours)

**Interval Calculation Formula:**
```
Initial interval: 20 hours
Success multiplier increases with each success:
- 1st success: 20 hours
- 2nd success: 40 hours (~1.7 days)
- 3rd success: 80 hours (~3.3 days)
- 4th success: 160 hours (~6.7 days)
- 5th success: 320 hours (~13.3 days)
- 6th success: 640 hours (~26.7 days)
- 7th+ success: Cap at ~720 hours (30 days)

Failure: Reset to 20 hours
```

### Study Session Flow

**1. Session Creation**
- User selects source:
  - Specific word set
  - All vocabulary
  - Words due for review
- User selects session size: 10, 20, 50, or All Due Words
- System selects words with earliest `next_review_at` timestamps

**2. Within-Session Logic**
Each word must be answered correctly **twice in a row** within the session to be considered complete.

**Session Mastery Criteria:**
- 2 consecutive correct answers = Complete for this session
- Any incorrect answer = Restart counter (need 2 more correct)
- Avoid showing same word twice in a row

**3. Card Display**
- **Front:** Word + Part of Speech (e.g., "parler (verb)")
- **Back:** All definitions with examples (full detail view)
- **Buttons:** "I know it ✓" / "I need practice ✗"

**4. Post-Session Update**
- Words answered correctly: Update `next_review_at` (increase interval)
- Words answered incorrectly: Update `next_review_at` (reset to 20 hours)
- Save session statistics

## Database Schema

### user_vocabulary (Extension)
```sql
-- Add SRS fields to existing user_vocabulary table
ALTER TABLE user_vocabulary ADD COLUMN next_review_at TIMESTAMP;
ALTER TABLE user_vocabulary ADD COLUMN review_count INTEGER DEFAULT 0;
ALTER TABLE user_vocabulary ADD COLUMN consecutive_successes INTEGER DEFAULT 0;
ALTER TABLE user_vocabulary ADD COLUMN current_interval_hours INTEGER DEFAULT 20;
ALTER TABLE user_vocabulary ADD COLUMN last_reviewed_at TIMESTAMP;
ALTER TABLE user_vocabulary ADD COLUMN ease_factor DECIMAL(3,2) DEFAULT 2.00;

CREATE INDEX idx_user_vocabulary_next_review ON user_vocabulary(user_id, next_review_at);
```

**Field Explanations:**
- `next_review_at` - When this word is due for review
- `review_count` - Total number of times reviewed
- `consecutive_successes` - Consecutive correct reviews (for interval calculation)
- `current_interval_hours` - Current spacing interval in hours
- `last_reviewed_at` - Last review timestamp
- `ease_factor` - Difficulty modifier (2.0 = normal, lower = harder)

### study_sessions
```sql
CREATE TABLE study_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    word_set_id BIGINT REFERENCES word_sets(id) ON DELETE SET NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, ABANDONED
    session_size INTEGER NOT NULL,
    total_words INTEGER NOT NULL,
    words_completed INTEGER DEFAULT 0,
    total_attempts INTEGER DEFAULT 0,
    correct_attempts INTEGER DEFAULT 0,
    incorrect_attempts INTEGER DEFAULT 0,
    session_type VARCHAR(50), -- WORD_SET, VOCABULARY, DUE_REVIEW
    CONSTRAINT check_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'ABANDONED'))
);

CREATE INDEX idx_study_sessions_user ON study_sessions(user_id);
CREATE INDEX idx_study_sessions_status ON study_sessions(user_id, status);
CREATE INDEX idx_study_sessions_started ON study_sessions(started_at);
```

### study_session_items
```sql
CREATE TABLE study_session_items (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE,
    word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    user_vocabulary_id BIGINT REFERENCES user_vocabulary(id) ON DELETE CASCADE,

    -- Session-specific tracking
    attempts INTEGER DEFAULT 0,
    correct_count INTEGER DEFAULT 0,
    incorrect_count INTEGER DEFAULT 0,
    consecutive_correct INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'NEW', -- NEW, LEARNING, COMPLETED
    last_shown_at TIMESTAMP,
    display_order INTEGER,

    -- Result tracking
    final_result VARCHAR(20), -- SUCCESS, FAILED, SKIPPED

    CONSTRAINT check_item_status CHECK (status IN ('NEW', 'LEARNING', 'COMPLETED')),
    CONSTRAINT check_final_result CHECK (final_result IN ('SUCCESS', 'FAILED', 'SKIPPED', NULL))
);

CREATE INDEX idx_session_items_session ON study_session_items(session_id);
CREATE INDEX idx_session_items_word ON study_session_items(word_id);
CREATE INDEX idx_session_items_status ON study_session_items(session_id, status);
```

### study_session_attempts
```sql
-- Detailed log of each answer attempt
CREATE TABLE study_session_attempts (
    id BIGSERIAL PRIMARY KEY,
    session_item_id BIGINT NOT NULL REFERENCES study_session_items(id) ON DELETE CASCADE,
    attempted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    was_correct BOOLEAN NOT NULL,
    response_time_ms INTEGER -- Optional: track how long user took
);

CREATE INDEX idx_session_attempts_item ON study_session_attempts(session_item_id);
```

## Backend Architecture

### API Endpoints

```
Study Session Management:
POST   /api/study/sessions/start              - Start new study session
GET    /api/study/sessions/active             - Get user's active session (if any)
GET    /api/study/sessions/{id}               - Get session details & progress
DELETE /api/study/sessions/{id}               - Abandon session

Study Flow:
GET    /api/study/sessions/{id}/next-card     - Get next word to study
POST   /api/study/sessions/{id}/answer        - Submit answer for current word
POST   /api/study/sessions/{id}/complete      - Complete session & update SRS data

Session History:
GET    /api/study/sessions/history            - Get past sessions
GET    /api/study/sessions/{id}/summary       - Get detailed session summary

Due Words:
GET    /api/study/due-words                   - Get count of words due for review
GET    /api/study/due-words/list              - Get list of due words
```

### Request/Response DTOs

**StartSessionRequest:**
```json
{
  "source": "WORD_SET" | "VOCABULARY" | "DUE_REVIEW",
  "wordSetId": 123,  // Required if source=WORD_SET
  "sessionSize": 10 | 20 | 50 | 0,  // 0 = all available
  "includeNewWords": true  // Include words never reviewed
}
```

**SessionResponse:**
```json
{
  "sessionId": 456,
  "status": "ACTIVE",
  "startedAt": "2025-11-08T10:00:00Z",
  "totalWords": 20,
  "wordsCompleted": 5,
  "progress": {
    "completed": 5,
    "learning": 10,
    "new": 5
  },
  "stats": {
    "totalAttempts": 25,
    "correctAttempts": 20,
    "incorrectAttempts": 5,
    "accuracy": 80.0
  }
}
```

**NextCardResponse:**
```json
{
  "cardId": 789,
  "word": {
    "id": 123,
    "lemma": "parler",
    "partOfSpeech": "verb",
    "languageCode": "fr"
  },
  "progress": {
    "position": 6,
    "total": 20,
    "currentStreak": 1,
    "needsStreak": 2
  },
  "srsInfo": {
    "reviewCount": 5,
    "currentInterval": "3 days",
    "nextReview": "2025-11-11T10:00:00Z"
  }
}
```

**AnswerRequest:**
```json
{
  "cardId": 789,
  "correct": true,
  "responseTimeMs": 3500
}
```

**SessionSummaryResponse:**
```json
{
  "sessionId": 456,
  "completedAt": "2025-11-08T10:30:00Z",
  "duration": "30 minutes",
  "stats": {
    "totalWords": 20,
    "newWords": 5,
    "reviewWords": 15,
    "totalAttempts": 52,
    "correctAttempts": 42,
    "incorrectAttempts": 10,
    "accuracy": 80.77,
    "averageResponseTime": "3.2 seconds"
  },
  "srsUpdates": {
    "wordsAdvanced": 18,
    "wordsReset": 2,
    "nextDueCount": 5
  }
}
```

### Core Service Logic

**StudySessionService:**

```kotlin
class StudySessionService {

    // Start new session
    fun startSession(userId: Long, request: StartSessionRequest): StudySession {
        // 1. Validate no active session
        // 2. Select words based on source & size
        // 3. Create session with items
        // 4. Return session
    }

    // Get next word to study
    fun getNextCard(sessionId: Long): NextCardResponse? {
        // 1. Get incomplete items
        // 2. Exclude last shown word
        // 3. Sort by priority (failed > learning > new)
        // 4. Return top candidate
    }

    // Process answer
    fun submitAnswer(sessionId: Long, cardId: Long, correct: Boolean): AnswerResult {
        // 1. Record attempt
        // 2. Update item stats
        // 3. Update consecutive counter
        // 4. Check if item completed (2 consecutive correct)
        // 5. Update session progress
        // 6. Return result with next action
    }

    // Complete session
    fun completeSession(sessionId: Long): SessionSummary {
        // 1. Mark session as completed
        // 2. Update all user_vocabulary SRS fields
        // 3. Calculate new intervals
        // 4. Set next_review_at timestamps
        // 5. Generate summary
    }
}
```

**SRS Interval Calculator:**

```kotlin
class SrsIntervalCalculator {

    fun calculateNextInterval(
        consecutiveSuccesses: Int,
        wasCorrect: Boolean,
        currentIntervalHours: Int,
        easeFactor: Double
    ): Int {
        if (!wasCorrect) {
            return 20 // Reset to minimum
        }

        val baseMultiplier = 2.0
        val newInterval = when (consecutiveSuccesses) {
            0 -> 20
            1 -> 40
            2 -> 80
            3 -> 160
            4 -> 320
            5 -> 640
            else -> 720 // Cap at 30 days
        }

        return newInterval.coerceAtMost(720)
    }

    fun calculateNextReviewDate(intervalHours: Int): Instant {
        return Instant.now().plus(Duration.ofHours(intervalHours.toLong()))
    }
}
```

**Card Selection Algorithm:**

```kotlin
fun selectNextWord(session: StudySession, items: List<StudySessionItem>): StudySessionItem? {
    val incomplete = items.filter { it.status != COMPLETED }
    if (incomplete.isEmpty()) return null

    val lastShownId = session.lastShownWordId
    val candidates = incomplete.filter { it.wordId != lastShownId }

    // If only one word left, we might need to show it again
    if (candidates.isEmpty() && incomplete.size == 1) {
        return incomplete.first()
    }

    // Priority scoring (lower = higher priority)
    return candidates.minByOrNull { item ->
        val recentFailPenalty = if (item.incorrect_count > item.correct_count) -1000 else 0
        val attemptsBonus = item.attempts * 10
        val streakPenalty = item.consecutiveCorrect * 20
        val newWordPenalty = if (item.status == NEW) 100 else 0

        recentFailPenalty + attemptsBonus + streakPenalty + newWordPenalty
    }
}
```

## Frontend Architecture

### Pages & Components

**Pages:**
1. `/study` - Study home (select session type)
2. `/study/session/:id` - Active study session
3. `/study/history` - Past sessions
4. `/study/summary/:id` - Session summary

**Components:**
1. `StudyHome.vue` - Session creation options
2. `StudySession.vue` - Main study view
3. `FlashCard.vue` - Animated flashcard
4. `StudyProgress.vue` - Progress bar & stats
5. `SessionSummary.vue` - End summary
6. `DueWordsWidget.vue` - Show due count (for dashboard)

### Component Design

**FlashCard.vue:**
```vue
<template>
  <div class="flashcard" :class="{ flipped: isFlipped }">
    <div class="card-inner">
      <!-- Front -->
      <div class="card-front">
        <div class="word-display">
          <h1>{{ word.lemma }}</h1>
          <span class="pos-tag">{{ word.partOfSpeech }}</span>
        </div>
        <Button @click="flip">Reveal</Button>
      </div>

      <!-- Back -->
      <div class="card-back">
        <h2>{{ word.lemma }}</h2>
        <div class="definitions">
          <div v-for="def in word.definitions" :key="def.id">
            <p>{{ def.definitionNumber }}. {{ def.definitionText }}</p>
            <ul v-if="def.examples.length">
              <li v-for="ex in def.examples" :key="ex.id">
                {{ ex.sentenceText }}
              </li>
            </ul>
          </div>
        </div>

        <div class="answer-buttons">
          <Button @click="answer(false)" severity="danger">
            I need practice ✗
          </Button>
          <Button @click="answer(true)" severity="success">
            I know it ✓
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>
```

**StudyProgress.vue:**
```vue
<template>
  <Card>
    <template #content>
      <div class="progress-info">
        <div class="stats-row">
          <div class="stat">
            <label>Completed</label>
            <span>{{ session.wordsCompleted }}/{{ session.totalWords }}</span>
          </div>
          <div class="stat">
            <label>Accuracy</label>
            <span>{{ accuracy }}%</span>
          </div>
          <div class="stat">
            <label>Current Streak</label>
            <span>{{ currentStreak }}/2</span>
          </div>
        </div>

        <ProgressBar :value="progressPercentage" />

        <div class="word-status">
          <Tag severity="success">✓ {{ completedCount }} Complete</Tag>
          <Tag severity="warning">⟳ {{ learningCount }} Learning</Tag>
          <Tag severity="info">★ {{ newCount }} New</Tag>
        </div>
      </div>
    </template>
  </Card>
</template>
```

### State Management

**studyStore.ts:**
```typescript
export const useStudyStore = defineStore('study', () => {
  const activeSession = ref<StudySession | null>(null)
  const currentCard = ref<FlashCard | null>(null)
  const isCardFlipped = ref(false)

  async function startSession(request: StartSessionRequest) {
    const session = await api.startSession(request)
    activeSession.value = session
    await fetchNextCard()
  }

  async function fetchNextCard() {
    const card = await api.getNextCard(activeSession.value!.id)
    currentCard.value = card
    isCardFlipped.value = false
  }

  async function submitAnswer(correct: boolean) {
    await api.submitAnswer(
      activeSession.value!.id,
      currentCard.value!.cardId,
      correct
    )

    // Check if session complete
    const updated = await api.getSession(activeSession.value!.id)
    if (updated.wordsCompleted === updated.totalWords) {
      await completeSession()
    } else {
      await fetchNextCard()
    }
  }

  async function completeSession() {
    const summary = await api.completeSession(activeSession.value!.id)
    activeSession.value = null
    return summary
  }

  return {
    activeSession,
    currentCard,
    isCardFlipped,
    startSession,
    submitAnswer,
    completeSession
  }
})
```

## Implementation Phases

### Phase 1: Database & Core Backend (Priority: HIGH)
- [ ] Create database migration for SRS fields
- [ ] Create database migration for study sessions tables
- [ ] Create domain models (StudySession, StudySessionItem, etc.)
- [ ] Create repositories
- [ ] Implement SRS interval calculator
- [ ] Implement card selection algorithm

### Phase 2: Backend API (Priority: HIGH)
- [ ] Create DTOs for requests/responses
- [ ] Implement StudySessionService
- [ ] Create API endpoints (start, next, answer, complete)
- [ ] Add due words count endpoint
- [ ] Test backend logic thoroughly

### Phase 3: Frontend Core (Priority: HIGH)
- [ ] Create study store (Pinia)
- [ ] Create StudyHome page (session creation)
- [ ] Create FlashCard component with flip animation
- [ ] Create StudySession page (main study view)
- [ ] Integrate answer submission

### Phase 4: Progress & Polish (Priority: MEDIUM)
- [ ] Add StudyProgress component
- [ ] Add SessionSummary page
- [ ] Add navigation guards (prevent leaving active session)
- [ ] Add session history page
- [ ] Add due words widget for vocabulary page

### Phase 5: Enhancements (Priority: LOW)
- [ ] Add keyboard shortcuts (space = reveal, 1/2 = answer)
- [ ] Add audio pronunciation playback
- [ ] Add dark mode support for study
- [ ] Add session timer
- [ ] Add statistics graphs
- [ ] Export session data

## User Experience Flow

### Starting a Study Session

1. User navigates to `/study`
2. Sees options:
   - **Quick Review** - Study words due today (shows count badge)
   - **Word Set** - Select from available sets
   - **My Vocabulary** - Study from all saved words
3. Selects session size (10, 20, 50, All)
4. Session starts, first card appears

### During Study

1. Card shows word + part of speech (front)
2. User thinks of the meaning
3. Clicks "Reveal" to flip card
4. Card shows all definitions + examples (back)
5. User clicks "I know it ✓" or "I need practice ✗"
6. Next card appears immediately
7. Progress bar updates in real-time
8. Words repeat until each has 2 consecutive correct

### Session Complete

1. Summary screen shows:
   - Total time spent
   - Words reviewed
   - Accuracy percentage
   - Next due date for these words
   - Button to start new session
2. SRS intervals updated in background
3. User vocabulary next_review_at timestamps updated

## Success Metrics

- **Session Completion Rate:** % of started sessions completed
- **Daily Active Users:** Users doing at least 1 session/day
- **Retention Curve:** How well users remember after 1 day, 1 week, 1 month
- **Average Session Size:** Typical number of words studied
- **Time Efficiency:** Average time per word reviewed

## Future Enhancements

1. **Audio Support** - Play pronunciation on reveal
2. **Reverse Cards** - Show definition, guess word
3. **Typing Mode** - Type the word instead of just revealing
4. **Custom Card Templates** - Different card layouts
5. **Shared Decks** - Share word sets with SRS progress
6. **Mobile App** - Native app with notifications for due reviews
7. **Gamification** - Streaks, achievements, leaderboards
8. **AI Hints** - Generate mnemonic hints for difficult words
