# Vocabee 2025 Roadmap

**Version:** 5.0
**Created:** November 24, 2024
**Updated:** November 29, 2024
**Status:** Active Development

---

## Executive Summary

Vocabee is a language learning platform with AI-powered course generation, spaced repetition vocabulary learning, and
structured content delivery. The platform targets self-learners initially, with plans to evolve into a comprehensive
learning solution suitable for both independent study and tutor-assisted learning.

---

## Current State Analysis

### What's Built

**Frontend (Vue 3 + TypeScript + Pinia + PrimeVue)**

- Complete authentication flow (JWT + OAuth2)
- Course browsing and enrollment
- Module/episode content delivery
- 6 exercise types (multiple choice, fill-in-blank, matching, sentence scramble, cloze reading, listening)
- Flashcard study sessions with SRS
- Progress tracking and daily goals
- Admin dashboard with generation pipeline UI
- Reading library

**Backend (Kotlin + Spring Boot 3.2 + PostgreSQL)**

- 8-stage course generation pipeline (Blueprint → Module Planning → Episode Content → Vocabulary Linking → Character
  Profiles → Exercises → Media → Publishing)
- Spaced repetition system (20h → 30 day intervals)
- User progress tracking across courses/modules/episodes
- Exercise validation and attempt logging
- Audio generation via Google Gemini
- S3/MinIO file storage
- 29 database migrations

**Content Pipeline**

- AI-powered course structure generation
- Dialogue and story content generation
- Character profile management
- Vocabulary linking to episodes
- Exercise auto-generation
- Voice assignment for audio

### Architecture Strengths

1. **Clean separation of concerns** - Clear domain/service/controller boundaries
2. **Type safety** - Full TypeScript (frontend) and Kotlin (backend)
3. **Decoupled generation phases** - Can iterate on content before expensive media generation
4. **Comprehensive SRS** - Interval progression with ease factor adjustments
5. **Flexible content types** - 4 episode formats (DIALOGUE, STORY, ARTICLE, AUDIO_LESSON)

### Known Technical Debt

| Area        | Issue                                                         | Priority              |
|-------------|---------------------------------------------------------------|-----------------------|
| Security    | `/api/admin/generation/**` is `permitAll()`                   | High (pre-production) |
| Security    | Admin endpoints lack RBAC (just `authenticated()`)            | High (pre-production) |
| Performance | `.block()` calls in GeminiTextClient defeat reactive benefits | Medium                |
| Frontend    | No token refresh mechanism                                    | Medium                |
| Frontend    | localStorage token storage (XSS vulnerable)                   | Medium                |
| Frontend    | No error boundaries                                           | Low                   |
| Backend     | N+1 query potential in CourseController                       | Low                   |
| Testing     | Only ~10 integration tests                                    | Medium                |

---

## Core Insight: Pedagogy Over Story

### The Fundamental Principle

**Key Insight:**
- **A1 learners** need textbook-like, repetitive, controlled content (clarity > naturalness)
- **B1+ learners** benefit from natural, story-driven content (engagement enhances learning)
- Content generation must be pedagogically constrained, not just "engaging"

### Current Architecture: Decoupled Generation Phases

```
Phase 1: Blueprint & Planning
  → Define course structure, characters, grammar taxonomy

Phase 2: Module Planning
  → Define vocabulary scope, grammar focus per module

Phase 3: Episode Content (No Media)
  → Generate constrained content based on pedagogical plan
  → Validate against vocabulary/grammar requirements
  → Iterate cheaply until content is sound

Phase 4: Vocabulary Linking
  → Map vocabulary to episodes
  → Ensure proper recycling and introduction rates

Phase 5: Character Profiles
  → Consolidate character descriptions from all dialogue

Phase 6: Exercise Generation
  → Generate targeted exercises based on what was taught

Phase 7: Media Generation (Optional)
  → Generate audio using character voices
  → Generate images using character references
  → Only for validated, approved content

Phase 8: Publishing
  → Make course available to users
```

---

## Strategic Vision

### Target Audience Evolution

**Phase 1 (Current):** Self-learners

- Focus on polishing core learning experience
- Build content library across languages and CEFR levels
- Perfect the study session flow

**Phase 2 (Future):** Tutor-compatible platform

- Tutors can assign courses to students
- Progress visible to tutors
- Custom content creation for specific needs

### Monetization Strategy (Future)

**Free Tier:**

- Unlimited access to AI-generated content
- Daily review limits (aligned with learning science - prevents cramming)
- Core SRS and exercise functionality

**Premium Tier:**

- Unlimited daily activity
- Priority course generation (faster custom courses)
- Human-curated "Featured Courses"
- Advanced analytics (weak point detection, optimal study times)
- Speaking practice (when implemented)
- Offline access

**One-Time Purchases:**

- Premium course packs (human-verified, professionally designed)
- Topic-specific bundles (Business French, Medical Spanish, etc.)

### Content as Differentiator

The generation pipeline is the competitive advantage:

- **Breadth:** Generate content for any CEFR level, any topic
- **Personalization:** User interests → custom courses (future)
- **Freshness:** Current events, trending topics (future)
- **Volume:** Large library with consistent quality

---

## Development Priorities

### Priority 1: UI & Learning Experience Polish

**Focus:** Make the core learning loop feel excellent

#### 1.1 Study Session Flow Enhancement

- Flashcard → exercise → completion loop should feel satisfying
- Smooth transitions and feedback animations
- Clear progress indicators during sessions
- Session summary with meaningful insights

#### 1.2 Progress Visualization

- Module map showing journey through course
- Completion percentages with visual progress bars
- Streak calendar for daily activity
- CEFR level progress estimation

#### 1.3 Episode Visual Variety

- Distinct visual treatments for episode types:
    - DIALOGUE: Conversation bubbles, character avatars
    - STORY: Narrative layout with illustrations
    - ARTICLE: Reading-focused layout
    - AUDIO_LESSON: Audio-centric with transcript

#### 1.4 Vocabulary Integration

- One-click "Add to vocabulary" from episode content
- Post-episode vocabulary summary
- Show word context from episodes in study sessions
- "First seen in Episode X" references

---

### Priority 2: Content Pipeline Quality

**Focus:** Make generated content pedagogically excellent

#### 2.1 Pedagogical Validation

- Automated vocabulary budget enforcement (8-12 new words for A1)
- Grammar structure repetition checks (5+ uses per episode)
- Complexity metrics validation (sentence length, vocabulary level)
- CEFR compliance scoring

#### 2.2 Content Variety

- Ensure modules don't feel repetitive
- Different scenarios and character rotations
- Topic diversity within courses
- Cultural context integration

#### 2.3 Quality Feedback Loop

- User flag system for problematic content
- Quality metrics dashboard for admin
- Feedback integration into generation prompts
- A/B testing for content variations (future)

#### 2.4 Course Vocabulary Cards (AI-Generated)

**Problem:** Current Wiktionary-based dictionary has quality issues:

- 4,000+ orphaned inflected forms (French), 24,000+ (German)
- Inconsistent formatting across entries
- Missing links between inflected forms and base forms
- Variable definition quality

**Solution:** Two-tier vocabulary system:

**Tier 1: Course Vocabulary (AI-Generated, Curated)**

- Generate cards only for words appearing in course episodes
- Consistent structure: base form, IPA, definitions, examples, translations
- Multi-language translations (configurable target languages)
- CEFR level tagging
- Linked to episodes where word appears ("First seen in Module 2, Episode 3")
- Validation pipeline before release

**Tier 2: Reference Dictionary (Wiktionary, As-Is)**

- Keep for "look up any word" functionality
- Accept variable quality as supplementary resource

**Generation Flow:**

```
Episode Content → Extract Vocabulary → Check if Card Exists →
If not: Generate with AI → Validate → Store as CourseVocabulary
```

**Card Structure:**

- Lemma (base form) + common inflections
- IPA pronunciation
- 2-3 definitions (CEFR-appropriate)
- 2-3 contextual examples (from course or generated)
- Translations to user's native language
- Grammar notes (gender, conjugation class, etc.)

**Benefits:**

- Manageable scope (500-2000 words per course)
- Consistent quality across all cards
- Multi-language support without Wiktionary limitations
- Course-relevant examples and context

---

### Priority 3: Accommodating Experienced Learners

**Focus:** Users with existing knowledge should find their fit easily

#### 3.1 Placement Assessment

- Quick assessment to recommend starting point
- Adaptive questioning (adjusts based on answers)
- CEFR level estimation
- Recommended course suggestions

#### 3.2 Flexible Entry Points

- Topic-based entry ("I want business vocabulary")
- Skip basics option per module
- "I know this" test-out feature
- Module-level placement tests

#### 3.3 Vocabulary Import

- Import from Anki decks
- Import from CSV/text lists
- Mark imported words as "known"
- Adjust SRS intervals for known vocabulary

#### 3.4 Custom Learning Paths

- Choose specific modules without full course
- Mix content from different courses
- Create personal study collections

---

### Priority 4: Speaking Practice (New Feature Area)

**Focus:** Add speaking component to complete the learning experience

#### 4.1 Tier 1: Pronunciation Comparison (Recommended Start)

- User records word/phrase
- Visual waveform comparison to native audio
- Similarity score (pattern matching)
- **Tech:** Web Audio API, simple spectrogram comparison

#### 4.2 Tier 2: Speech-to-Text Validation

- User speaks, system transcribes
- Compare transcription to expected text
- Accuracy scoring with feedback
- **Tech:** Whisper API or Google Speech-to-Text
- **Cost:** ~$0.006/minute

#### 4.3 Tier 3: AI Conversation Partner (Future/Premium)

- Chat with episode characters via voice
- AI responds contextually
- Gentle correction of mistakes
- **Tech:** Gemini Live / GPT-4o realtime API
- **Cost:** Higher, premium feature

#### 4.4 Tier 4: Pronunciation Coaching (Future/Premium)

- Phoneme-level feedback
- Language-specific pronunciation tips
- **Tech:** Specialized APIs (Speechace, ELSA)

**Recommended Implementation Path:**

1. Start with Tier 2 (Speech-to-Text Validation)
2. Integrate with existing episode dialogues
3. Flow: Show phrase → User records → Transcribe → Compare → Feedback

---

### Priority 5: Writing Practice (Future)

#### 5.1 Guided Writing Prompts

- Short answer exercises based on episode content
- Sentence construction from vocabulary
- Grammar-focused writing tasks

#### 5.2 AI Evaluation

- Grammar checking with explanations
- Vocabulary usage feedback
- Suggestions for improvement
- Progressive difficulty based on performance

---

### Priority 6: Analytics & Insights

#### 6.1 Learning Analytics Dashboard

- Words mastered over time
- Grammar structures learned
- Time spent per skill area
- Comparison to learning goals

#### 6.2 Weak Point Detection

- Identify struggling vocabulary
- Flag problematic grammar patterns
- Suggest targeted review sessions

#### 6.3 Optimal Study Recommendations

- Best times to study (based on performance patterns)
- Session length recommendations
- Spaced repetition optimization insights

---

## Technical Improvements (Pre-Production)

### Security Hardening

- [ ] Implement RBAC with `@PreAuthorize("hasRole('ADMIN')")`
- [ ] Remove `permitAll()` from admin generation endpoints
- [ ] Move to HttpOnly cookies for tokens
- [ ] Implement token refresh mechanism
- [ ] Add rate limiting on public endpoints

### Performance Optimization

- [ ] Replace `.block()` calls with proper async patterns
- [ ] Add `@EntityGraph` for N+1 query prevention
- [ ] Implement Redis caching for frequently accessed data
- [ ] Add request debouncing on frontend

### Quality Assurance

- [ ] Add error boundaries to Vue app
- [ ] Expand integration test coverage
- [ ] Add API documentation (OpenAPI/Swagger)
- [ ] Implement comprehensive form validation (Zod)

---

## Mobile Strategy

**Current Decision:** Wait until feature set is finalized

**Rationale:**

- Prevents duplicate development effort
- Avoids API contract changes breaking mobile
- Prevents maintaining dead features across platforms

**Preparation:**

- Keep API mobile-friendly (already mostly there)
- Design components with responsive-first approach
- Consider PWA as intermediate step

**Future Options:**

- Native apps (React Native or Flutter)
- PWA enhancement for offline capability

---

## Success Metrics

### Learning Effectiveness (Primary)

- **Vocabulary Retention:** >80% recall after 30 days
- **Grammar Accuracy:** Improvement in exercise scores over time
- **Course Completion:** >60% completion rate
- **User Progression:** Users advancing through CEFR levels

### Content Quality

- **Pedagogical Compliance:** >90% of A1 episodes within vocabulary budget
- **Grammar Coverage:** All CEFR-required structures covered
- **User Satisfaction:** <5% content flagged as problematic

### Engagement

- **Daily Active Users:** Growth month-over-month
- **Session Length:** Average >10 minutes
- **Streak Retention:** >50% maintain 7-day streaks
- **Return Rate:** >60% weekly return rate

### Cost Efficiency

- **Content Iteration Cost:** 60-80% reduction vs media-first approach
- **Media ROI:** Only generate media for approved content
- **Infrastructure Cost:** Sustainable per-user cost

---

## Open Questions

1. **CEFR Word Lists:** Do we have pre-vetted A1/A2/B1 vocabulary lists per language?
2. **Grammar Sequencing:** Do we have definitive grammar progression orders per language?
3. **Learning Validation:** How will we measure actual learning outcomes?
4. **Speaking Implementation:** Which speech-to-text provider offers best quality/cost for language learning?
5. **Tutor Features:** What specific features would tutors need?
6. **Offline Capability:** How important is offline access for target users?

---

## Not In Scope (Yet)

- Native mobile apps (until feature set is stable)
- Multi-user collaboration features
- Live tutoring integration
- Advanced gamification (leaderboards, competitions)
- User-generated content
- Enterprise/school admin features

---

## Key Principles

1. **Pedagogy > Engagement:** For A1, effective beats engaging
2. **Level-Appropriate:** A1 needs drills, B1 needs stories
3. **Measure Learning:** Not just engagement, but actual outcomes
4. **Iterate Cheaply:** Validate content before expensive operations
5. **Foundation First:** Polish core experience before adding features
6. **Self-Learner Focus:** Build for independent study, extend to tutors later
7. **Content Volume:** Large, quality library is the differentiator

---

**Document Status:** Active Development
**Next Review:** After speaking practice scoping
**Owner:** Vocabee Product Team

**Last Updated:** November 29, 2024
