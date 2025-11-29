# Vocabee 2025 Roadmap

**Version:** 6.0
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

**Known Issues:**

- Homograph disambiguation: AI may generate wrong definition for words with multiple meanings (e.g., "tu" as past
  participle of "taire" instead of pronoun "you"). Needs context from word set or part-of-speech hints to disambiguate.

---

### Priority 3: Episode Flow & Grammar Integration

**Focus:** Make learning feel structured and pedagogically sound

**Current Problem:**
Episodes feel chaotic - users jump into dialogues/stories without clear learning objectives or grammar preparation. The
`grammarFocus` field exists on modules but isn't surfaced to users.

#### 3.1 Structured Episode Flow

Each episode should follow a clear pedagogical pattern:

```
1. Learning Objectives → "In this episode, you'll learn..."
2. Grammar Introduction → Brief explanation of new structures
3. Content (Dialogue/Story) → Apply grammar in context
4. Comprehension Check → Verify understanding
5. Practice Exercises → Reinforce learning
6. Summary → Recap key points
```

#### 3.2 Grammar Explanations

- Add `GrammarExplanation` content type to episodes
- Simple, CEFR-appropriate explanations with examples
- Visual aids for grammar patterns (tables, diagrams)
- Link to related grammar rules in database

#### 3.3 Learning Objectives Display

- Show module objectives before starting
- Episode-level objectives visible in UI
- Progress indicators tied to objectives
- "What you'll learn" preview cards

#### 3.4 Content Type Sequencing

- Enforce logical order of content items within episodes
- Grammar explanation → Example → Practice pattern
- Vocabulary introduction before content using new words

---

### Priority 4: Internationalization (i18n)

**Focus:** Support learners with different native languages

**Strategy:** Two-tier approach based on CEFR level

#### 4.1 A1-A2: Bilingual Support

For beginners, UI and exercise instructions in user's native language:

**Approach:**

- Define a limited set of translatable instruction phrases
- Examples: "Which statements are true?", "Listen and repeat", "Match the pairs"
- Store as translation keys, not hardcoded English
- All course content remains in target language

**Implementation:**

- Use vue-i18n for frontend translations
- Create `instruction_keys` table for exercise instructions
- Limit instruction variety to ~50-100 phrases
- Initially support: English, Spanish, Russian, German, French

**Benefits:**

- Reduces cognitive load for beginners
- Same exercises work for any native language
- Manageable translation scope

#### 4.2 B1+: Full Immersion

For intermediate+ learners, everything in target language:

- All users see identical content regardless of native language
- Exercise instructions in target language
- Grammar explanations in target language
- Simpler localization (no per-language-pair content)

#### 4.3 UI Localization

Separate from content i18n:

- Navigation labels
- Button text
- Error messages
- Settings and profile UI

**Target Languages (UI):** English, Spanish, French, German, Russian, Portuguese, Italian, Japanese, Korean, Chinese

#### 4.4 Technical Implementation

```
User Settings:
  - nativeLanguage: "es"
  - targetLanguage: "fr"
  - cefrLevel: "A1"

Content Selection Logic:
  if (cefrLevel in ["A1", "A2"]) {
    instructions = translate(instructionKey, nativeLanguage)
  } else {
    instructions = getInstructionInTargetLanguage(instructionKey, targetLanguage)
  }
```

---

### Priority 5: Content Generation Expansion

**Focus:** Expand generation capabilities beyond courses

#### 5.1 Standalone Reading Content

- Reuse `EpisodeContentGenerationService` for standalone stories/articles
- Generate graded readers at specific CEFR levels
- Topic-based generation (travel, food, culture, news)
- Variable length (short articles to multi-chapter stories)

#### 5.2 Standalone Exercise Generation

- Generate exercises without full course context
- Grammar-focused drill generation
- Vocabulary practice generation
- Speaking exercise generation (phrases for repetition, conversation prompts)

#### 5.3 Speaking Content Generation

- Generate phrases for pronunciation practice
- Create conversation scenarios
- Question-answer pair generation
- Dialogue completion exercises

#### 5.4 Admin Tools

- UI for generating standalone content
- Batch generation for content libraries
- Quality review workflow for generated content

---

### Priority 6: Speaking Practice

**Focus:** Add speaking component to complete the learning experience

#### 6.1 Tier 1: Pronunciation Comparison (Recommended Start)

- User records word/phrase
- Visual waveform comparison to native audio
- Similarity score (pattern matching)
- **Tech:** Web Audio API, simple spectrogram comparison

#### 6.2 Tier 2: Speech-to-Text Validation

- User speaks, system transcribes
- Compare transcription to expected text
- Accuracy scoring with feedback
- **Tech:** Whisper API or Google Speech-to-Text
- **Cost:** ~$0.006/minute

#### 6.3 Tier 3: AI Conversation Partner (Future/Premium)

- Chat with episode characters via voice
- AI responds contextually
- Gentle correction of mistakes
- **Tech:** Gemini Live / GPT-4o realtime API
- **Cost:** Higher, premium feature

#### 6.4 Tier 4: Pronunciation Coaching (Future/Premium)

- Phoneme-level feedback
- Language-specific pronunciation tips
- **Tech:** Specialized APIs (Speechace, ELSA)

**Recommended Implementation Path:**

1. Start with Tier 2 (Speech-to-Text Validation)
2. Integrate with existing episode dialogues
3. Flow: Show phrase → User records → Transcribe → Compare → Feedback

---

### Priority 7: Analytics & Learning Effectiveness

**Focus:** Measure actual learning outcomes, not just engagement

#### 7.1 Retention Metrics

- Track vocabulary recall rates over time
- Measure SRS effectiveness (% words retained at each interval)
- Compare predicted vs actual recall
- Identify words with unusually high failure rates

#### 7.2 Progress Tracking

- Grammar accuracy trends per structure
- Exercise type performance (which types help most?)
- Time-to-mastery metrics per word/grammar point
- CEFR level progression estimation

#### 7.3 Weak Point Detection

- Identify struggling vocabulary (high error rate, frequent resets)
- Flag problematic grammar patterns
- Detect pronunciation difficulties (if speaking enabled)
- Suggest targeted review sessions

#### 7.4 Learning Insights Dashboard

- Personal analytics for users:
    - Words mastered over time graph
    - Accuracy trends
    - Time spent per skill area
    - Comparison to goals
- Admin analytics:
    - Aggregate learning outcomes
    - Content effectiveness metrics
    - Drop-off points in courses

#### 7.5 Adaptive Recommendations

- Suggest optimal study times based on performance patterns
- Recommend session length based on retention data
- Identify when user is ready for next level
- Surface content that matches weak areas

---

### Priority 8: User Onboarding & Content Discovery

**Focus:** Help users find appropriate content quickly

#### 8.1 New User Flow

- Language selection with clear level descriptions
- Background assessment (not a test, just questions):
    - "Have you studied [language] before?"
    - "Can you understand basic conversations?"
    - "Can you read simple texts?"
- Recommend starting course based on responses
- Allow easy course switching if level is wrong

#### 8.2 Content Labeling

- Clear CEFR level badges on all content
- Topic tags for easy filtering
- Difficulty indicators beyond CEFR (vocabulary density, sentence complexity)
- "Good for beginners" / "Challenging" labels

#### 8.3 Home Page Improvements

- Personalized recommendations based on history
- "Continue Learning" prominent for returning users
- Discovery section for new content
- Clear paths: "Start French" → Course selection → Begin

#### 8.4 Search & Filtering

- Search courses by topic, level, duration
- Filter exercises by type, grammar point, difficulty
- Browse vocabulary by theme
- "Similar to what you've studied" recommendations

---

### Priority 9: Writing Practice (Future)

#### 9.1 Guided Writing Prompts

- Short answer exercises based on episode content
- Sentence construction from vocabulary
- Grammar-focused writing tasks

#### 9.2 AI Evaluation

- Grammar checking with explanations
- Vocabulary usage feedback
- Suggestions for improvement
- Progressive difficulty based on performance

---

### Priority 10: User Feedback & Content Quality

**Focus:** Get signal from users to improve content

#### 10.1 Content Flagging

- "Report issue" button on all content
- Categories: Wrong translation, Grammar error, Confusing, Too difficult, Audio problem
- Quick feedback (thumbs up/down) after exercises
- Optional comment field for details

#### 10.2 Feedback Processing

- Admin dashboard for flagged content
- Priority queue based on flag frequency
- Link feedback to specific content items
- Track resolution status

#### 10.3 Content Versioning

- Minor fixes applied to existing content
- Major issues → regenerate or hide content
- Track content quality scores over time
- A/B testing for content variations (future)

---

### Priority 11: Accommodating Experienced Learners

**Focus:** Users with existing knowledge should find their fit easily

#### 11.1 Vocabulary Import

- Import from Anki decks
- Import from CSV/text lists
- Mark imported words as "known"
- Adjust SRS intervals for known vocabulary

#### 11.2 Custom Learning Paths

- Choose specific modules without full course
- Mix content from different courses
- Create personal study collections

#### 11.3 Skip/Test-Out Options

- "I know this" option per module
- Quick quiz to verify knowledge
- Skip to more advanced content

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

## Production Readiness

### Infrastructure & Deployment

- [ ] Docker containerization for backend and frontend
- [ ] Docker Compose for local development
- [ ] Kubernetes manifests or cloud deployment configs
- [ ] Environment-specific configuration (dev/staging/prod)
- [ ] CORS configuration for production domains
- [ ] SSL/TLS setup
- [ ] CDN for static assets and audio files

### Monitoring & Observability

- [ ] Application logging (structured logs)
- [ ] Error tracking (Sentry or similar)
- [ ] Performance monitoring (APM)
- [ ] Health check endpoints
- [ ] Alerting for critical failures (API down, Gemini errors)
- [ ] Database monitoring

### Backup & Recovery

- [ ] Database backup strategy
- [ ] S3/MinIO backup for media files
- [ ] Disaster recovery plan
- [ ] Data export functionality for users

---

## Mobile Strategy

**Current Decision:** Native apps after web feature set is stable

**Rationale:**

- Prevents duplicate development effort
- Avoids API contract changes breaking mobile
- Prevents maintaining dead features across platforms
- Mobile apps don't need admin functionality

**Approach:**

- **NOT PWA** - Native apps preferred for better UX and offline capability
- **Technology:** React Native or Flutter (TBD based on team expertise)
- **Scope:** Learning features only (no course generation/admin)
- **Timeline:** After web MVP is stable and API contracts are finalized

**Preparation:**

- Keep API mobile-friendly (already mostly there)
- Design API responses with mobile bandwidth in mind
- Document API contracts thoroughly
- Plan offline-first architecture for vocabulary/SRS

**Mobile-Specific Features:**

- Push notifications for study reminders
- Offline vocabulary review
- Audio download for offline listening
- Widget for daily progress

---

## Content Volume Strategy

**Goal:** 15-20 courses per language, targeting A1-B1 initially

### Target Content Library

**Per Language (French, German, Spanish, Italian, Portuguese):**

- 5-7 courses at A1 level (complete beginner to basic)
- 5-7 courses at A2 level (elementary)
- 3-5 courses at B1 level (intermediate)
- Additional topic-specific courses (travel, business, culture)

**Content Types:**

- Structured courses (8-12 modules each)
- Standalone reading content (graded readers)
- Vocabulary practice sets
- Grammar drill exercises

### Generation Workflow

1. **Generate** - Use pipeline to create course structure and content
2. **Review** - Human verification of key content (dialogues, grammar explanations)
3. **Test** - Internal testing of full course flow
4. **Publish** - Release to users
5. **Monitor** - Track user feedback and engagement
6. **Iterate** - Fix issues, improve based on feedback

### Quality Control

- AI-generated content requires review before publishing
- Flag system for user-reported issues
- Quality score tracking per course
- Periodic content audits

### Wiktionary Dictionary

**Current Status:**

- French and German partially imported
- Quality issues with orphaned inflected forms
- Inconsistent formatting

**Improvement Plan:**

- Audit parsing logic for accuracy
- Fix inflected form linking
- Expand to additional languages
- Use as fallback/reference (Tier 2), not primary vocabulary source

### Content Priorities

1. **French A1** - Most complete, use as template
2. **Spanish A1** - High demand language
3. **German A1** - Already has dictionary data
4. **French A2** - Build on A1 foundation
5. **Expand to B1** - After A1-A2 content is solid

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
6. **Mobile Technology:** React Native vs Flutter - which has better audio/recording support?
7. **i18n Scope:** Exactly which phrases need translation for A1-A2? Need to audit existing exercises.
8. **Content Review:** Who reviews AI-generated content? Internal team or crowdsourced?
9. **Wiktionary Parsing:** How much effort to fix vs. rely entirely on AI-generated cards?
10. **Analytics Privacy:** What learning data can we collect while respecting user privacy?

---

## Not In Scope (Yet)

- Multi-user collaboration features
- Live tutoring integration
- Advanced gamification (leaderboards, competitions)
- User-generated content
- Enterprise/school admin features
- C1/C2 level content (focus on A1-B1 first)

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
**Next Review:** After MVP scope finalization
**Owner:** Vocabee Product Team

**Last Updated:** November 29, 2024

---

## Priority Summary

### Phase 1: Content & Learning Core

*Content is the product. Users come for rich, varied learning material.*

| Priority | Area                               | Why Critical                                                                    |
|----------|------------------------------------|---------------------------------------------------------------------------------|
| 1        | Content Generation Expansion       | Standalone reading, exercises, speaking - content variety is the differentiator |
| 2        | Content Pipeline Quality           | AI-generated vocabulary cards, pedagogical validation                           |
| 3        | Episode Flow & Grammar Integration | Structured learning feels professional, not chaotic                             |
| 4        | Analytics & Learning Effectiveness | Prove the platform actually teaches; identify weak points                       |
| 5        | Speaking Practice                  | Differentiator from text-only competitors                                       |
| -        | Content Volume                     | 15-20 courses per language (parallel effort)                                    |

### Phase 2: User Experience

*Make content discoverable and accessible to all users.*

| Priority | Area                                | Why Critical                                          |
|----------|-------------------------------------|-------------------------------------------------------|
| 6        | User Onboarding & Content Discovery | Match users to appropriate content quickly            |
| 7        | User Feedback & Content Quality     | Get signal to improve content                         |
| 8        | UI & Learning Experience Polish     | Study flow, progress visualization                    |
| 9        | Internationalization (i18n)         | Enable non-English speakers (A1-A2 bilingual support) |

### Phase 3: Scale & Deploy

*Infrastructure to reach users at scale.*

| Priority | Area                               | Why Critical                     |
|----------|------------------------------------|----------------------------------|
| 10       | Production Readiness               | Security, deployment, monitoring |
| 11       | Mobile Apps                        | Native apps after web is stable  |
| 12       | Accommodating Experienced Learners | Import, skip, custom paths       |
| 13       | Writing Practice                   | Future enhancement               |
