# Vocabee 2025 Roadmap - Pedagogical Foundation

**Version:** 4.0
**Created:** November 24, 2024
**Updated:** November 25, 2024
**Status:** 📋 PLANNING - Revised with Pedagogical Focus

---

## 🎯 Core Insight: Pedagogy Over Story

### The Fundamental Realization

**Previous assumption:** Make content engaging and natural → students will learn
**Corrected assumption:** Make content pedagogically rigorous → students will learn (engagement varies by level)

**Key Insight:**

- **A1 learners** need textbook-like, repetitive, controlled content (clarity > naturalness)
- **B1+ learners** benefit from natural, story-driven content (engagement enhances learning)
- **Current problem:** We're generating "engaging" content for all levels, making A1 too complex

---

## 🏗️ The Architectural Problem

### Current System: Coupled Generation

```
Generate Everything → Audio → Images → Exercises → Hope it's pedagogically sound
```

**Problems:**

1. ❌ No pedagogical planning (vocabulary/grammar progression is accidental)
2. ❌ Content generation happens without constraints
3. ❌ Expensive operations (audio/images) happen before validation
4. ❌ Exercises generated from content without knowing what should be taught
5. ❌ Can't iterate on content without wasting resources

### Proposed System: Decoupled Phases

```
Phase 1: Pedagogical Planning
  → Define vocabulary scope, grammar progression, learning objectives

Phase 2: Content Generation (No Media)
  → Generate constrained content based on pedagogical plan
  → Validate against vocabulary/grammar requirements
  → Iterate cheaply until content is sound

Phase 3: Content Analysis
  → Extract character map from all content
  → Analyze vocabulary usage
  → Generate targeted exercises based on what was actually taught

Phase 4: Media Generation (Optional)
  → Generate audio using character map
  → Generate images using character references
  → Only for validated, approved content
```

---

## 🎓 PRIORITY 1: Pedagogical Foundation System

**Priority:** 🔥🔥🔥🔥🔥 CRITICAL (Must have before anything else)
**Complexity:** High
**Estimated Time:** 8-10 weeks
**Cost Impact:** Massive savings (don't regenerate expensive media)

### 1.1 Pedagogical Profile & Syllabus Generation

#### What It Is

A system that takes a course level and story premise, then generates a pedagogically rigorous syllabus with vocabulary
budgets, grammar progression, and learning objectives.

#### Why It Matters

- Ensures CEFR compliance
- Controls vocabulary introduction rate
- Sequences grammar logically
- Sets clear learning objectives per episode
- Foundation for all content generation

#### Implementation

**New Domain Models:**

```kotlin
data class PedagogicalProfile(
    val courseId: Long,
    val targetLevel: String,              // A1, A2, B1, etc.
    val storyPremise: String,
    val totalModules: Int,
    val vocabularyScope: VocabularyScope,
    val grammarScope: GrammarScope
)

data class VocabularyScope(
    val cefrLevel: String,
    val totalTargetWords: Int,            // e.g., 300 for A1
    val wordsPerEpisode: Int,             // e.g., 8-12 for A1
    val recyclingRate: Double,            // e.g., 0.80 (80% known words)
    val allowedWordLists: List<String>    // Pre-vetted CEFR lists
)

data class GrammarScope(
    val cefrLevel: String,
    val structures: List<GrammarStructure>,
    val progressionOrder: List<String>
)

data class EpisodePlan(
    val episodeNumber: Int,
    val title: String,
    val learningObjective: String,        // "Student can order food using 'je voudrais'"
    val targetVocabulary: List<String>,   // NEW words (8-12)
    val recycledVocabulary: List<String>, // From previous episodes
    val grammarStructure: GrammarStructure, // ONE structure to drill (A1)
    val repetitionsNeeded: Int            // Min times to use structure (5+ for A1)
)
```

**Database Schema:**

```sql
CREATE TABLE pedagogical_profiles
(
    id               BIGSERIAL PRIMARY KEY,
    course_id        BIGINT REFERENCES courses (id),
    target_level     VARCHAR(10),
    story_premise    TEXT,
    vocabulary_scope JSONB,
    grammar_scope    JSONB,
    created_at       TIMESTAMP DEFAULT NOW()
);

CREATE TABLE episode_plans
(
    id                  BIGSERIAL PRIMARY KEY,
    module_id           BIGINT REFERENCES modules (id),
    episode_number      INT,
    learning_objective  TEXT,
    target_vocabulary   JSONB,
    recycled_vocabulary JSONB,
    grammar_structure   JSONB,
    repetitions_needed  INT
);
```

**Service:**

```kotlin
@Service
class PedagogicalPlanningService {
    fun generatePedagogicalProfile(
        level: String,
        storyPremise: String,
        language: String
    ): PedagogicalProfile

    fun generateSyllabus(profile: PedagogicalProfile): GeneratedSyllabus
}
```

#### Success Criteria

- ✅ Vocabulary budget enforced per episode
- ✅ Grammar structures sequenced logically
- ✅ Learning objectives are specific and measurable
- ✅ A1 episodes have 8-12 new words max
- ✅ 80% vocabulary recycling rate maintained

---

### 1.2 Constrained Content Generation

#### What It Is

Content generation that respects pedagogical constraints: vocabulary limits, grammar focus, difficulty level.

#### Why It Matters

- Prevents overwhelming A1 learners
- Ensures content matches learning objectives
- Allows validation before expensive operations
- Can regenerate content without wasting money

#### Implementation

**Level-Specific Prompt Templates:**

**A1 Template:**

```
PEDAGOGICAL CONSTRAINTS (CRITICAL):
- You MUST use exactly these NEW words: [8-12 words from plan]
- You MUST recycle these known words: [words from previous episodes]
- You CANNOT use any other vocabulary
- Focus on this ONE grammar structure: [pattern]
- Use this structure at least [5-8] times
- Keep sentences simple (max 8 words)
- NO idioms, NO complex tenses

STYLE REQUIREMENTS:
- Textbook-like is GOOD (repetitive helps learning)
- Clarity is MORE important than naturalness
- Drill the grammar pattern explicitly

Learning Objective: [specific objective from plan]
```

**B1 Template:**

```
PEDAGOGICAL CONSTRAINTS:
- Introduce these vocabulary words naturally: [12-15 words]
- Assume A2 knowledge
- Demonstrate these grammar structures: [2-3 structures]
- Sentence complexity: moderate

STYLE REQUIREMENTS:
- Natural dialogue is important
- Story continuity matters
- Grammar should be contextual, not drilled

Learning Objective: [specific objective]
```

**Validation:**

```kotlin
fun validatePedagogicalCompliance(
    content: GeneratedEpisodeContent,
    plan: EpisodePlan
): ValidationResult {
    // Extract all words used
    val usedWords = extractVocabulary(content)

    // Check for unauthorized vocabulary
    val unauthorized = usedWords - (plan.targetVocabulary + plan.recycledVocabulary)

    // Check grammar structure repetition
    val structureCount = countStructureUsage(content, plan.grammarStructure)

    // Check complexity
    val avgWordsPerSentence = calculateComplexity(content)

    return ValidationResult(
        vocabularyCompliant = unauthorized.isEmpty(),
        grammarSufficient = structureCount >= plan.repetitionsNeeded,
        complexityAppropriate = avgWordsPerSentence <= getMaxForLevel(plan.level)
    )
}
```

**Updated Episode Schema:**

```kotlin
data class Episode(
    // ... existing fields
    val generationPhase: GenerationPhase = GenerationPhase.PLANNING,
    val pedagogicalCompliance: PedagogicalCompliance?,
    val audioGenerated: Boolean = false,
    val imagesGenerated: Boolean = false
)

enum class GenerationPhase {
    PLANNING,           // Syllabus exists
    CONTENT_GENERATED,  // Content created (no media)
    VALIDATED,          // Pedagogy validated
    EXERCISES_ADDED,    // Exercises generated
    MEDIA_GENERATED,    // Audio/images created
    PUBLISHED
}
```

#### Success Criteria

- ✅ A1 content uses only planned vocabulary
- ✅ Grammar structures repeated minimum times
- ✅ Content can be validated without media generation
- ✅ Failed validation triggers regeneration, not media waste
- ✅ Complexity metrics match CEFR level

---

### 1.3 Content Analysis & Targeted Exercise Generation

#### What It Is

After content is generated and validated, analyze it to extract character profiles, vocabulary usage, and generate
exercises that test what was actually taught.

#### Why It Matters

- Character profiles based on ALL their dialogue (not predicted)
- Exercises test specific learning objectives
- Fill-in-blank uses actual sentences from episode
- Multiple choice tests actual vocabulary introduced
- Quality exercises without guessing what to test

#### Implementation

**Character Map Extraction:**

```kotlin
@Service
class ContentAnalysisService {
    fun extractCharacterMap(moduleId: Long): CharacterMap {
        val episodes = episodeRepository.findByModuleId(moduleId)
        val characters = mutableMapOf<String, CharacterAppearances>()

        // Collect ALL dialogue for each character
        episodes.forEach { episode ->
            val dialogue = parseDialogue(episode)
            dialogue.lines.forEach { line ->
                characters.getOrPut(line.speaker) {
                    CharacterAppearances(name = line.speaker, lines = mutableListOf())
                }.lines.add(line.text)
            }
        }

        // Generate profiles based on actual dialogue
        return characters.mapValues { (name, appearances) ->
            generateCharacterProfile(name, appearances.lines)
        }
    }
}
```

**Targeted Exercise Generation:**

```kotlin
@Service
class ExerciseGenerationService {
    fun generateForEpisode(
        episodeId: Long,
        episodePlan: EpisodePlan
    ): List<GeneratedExercise> {
        val episode = episodeRepository.findById(episodeId)
        val content = parseContent(episode)

        return listOf(
            // Fill-in-blank: Test grammar structure from plan
            generateFillInBlank(
                sentences = extractSentencesWithStructure(content, episodePlan.grammarStructure),
                targetStructure = episodePlan.grammarStructure
            ),

            // Multiple choice: Test NEW vocabulary from plan
            generateMultipleChoice(
                contextSentences = findVocabularyUsage(content, episodePlan.targetVocabulary),
                targetWords = episodePlan.targetVocabulary
            ),

            // Sentence scramble: Drill grammar pattern
            generateSentenceScramble(
                sentences = extractSentencesWithStructure(content, episodePlan.grammarStructure)
            )
        )
    }
}
```

#### Success Criteria

- ✅ Character profiles reflect actual dialogue patterns
- ✅ Exercises test planned learning objectives
- ✅ Fill-in-blank uses real sentences from episode
- ✅ Multiple choice tests actual new vocabulary
- ✅ Exercises aligned with pedagogy, not random

---

### 1.4 Decoupled Media Generation

#### What It Is

Audio and image generation as a separate, optional phase that happens AFTER content is validated and approved.

#### Why It Matters

- **Cost savings:** Don't generate media for bad content
- **Iteration speed:** Regenerate content cheaply
- **Quality control:** Only generate media for approved content
- **Transparency:** Show cost estimates before generation

#### Implementation

**Separate Media Workflow:**

```kotlin
@Service
class MediaGenerationService {
    fun generateMediaForCourse(
        courseId: Long,
        options: MediaGenerationOptions
    ): MediaGenerationResult {
        // Get all validated episodes
        val episodes = episodeRepository.findByCourseIdAndPhase(
            courseId,
            GenerationPhase.VALIDATED
        )

        if (episodes.isEmpty()) {
            throw IllegalStateException("No validated content to generate media for")
        }

        // Estimate costs first
        val estimate = estimateCosts(episodes, options)

        // Generate in phases
        val results = if (options.generateCharacterImages) {
            val characterMap = contentAnalysisService.extractCharacterMap(courseId)
            generateCharacterReferenceImages(characterMap)
        }

        episodes.forEach { episode ->
            if (options.generateAudio) {
                generateAudioForEpisode(episode)
            }
            if (options.generateImages) {
                generateImagesForEpisode(episode, characterReferenceImages)
            }

            episode.generationPhase = GenerationPhase.MEDIA_GENERATED
            episodeRepository.save(episode)
        }

        return results
    }

    fun estimateCosts(
        episodes: List<Episode>,
        options: MediaGenerationOptions
    ): CostEstimate {
        // Calculate based on:
        // - Number of audio files
        // - Number of images
        // - Character images needed
        // Return estimate in USD
    }
}
```

**Updated Wizard:**

```
Phase 1: Pedagogical Planning (New)
  → Set level, story premise
  → Review vocabulary/grammar scope
  → Generate and approve syllabus

Phase 2: Content Generation (Updated)
  → Generate episode content (no media)
  → Validate pedagogical compliance
  → Review and regenerate if needed
  → Approve content

Phase 3: Analysis & Exercises (New)
  → Extract character map
  → Generate targeted exercises
  → Review exercises

Phase 4: Media Generation (New, Optional)
  → Show cost estimate
  → Select what to generate (audio, images, both)
  → Assign voices using character map
  → Generate media
  → Publish course
```

#### Success Criteria

- ✅ Can generate content without media
- ✅ Media generation is separate workflow
- ✅ Cost estimates shown before generation
- ✅ Can regenerate content without wasting media costs
- ✅ 60-80% cost savings during iteration

---

## 🔧 PRIORITY 2: Learning Loop Enhancement

**Priority:** 🔥🔥🔥 HIGH (After pedagogical foundation)
**Complexity:** Medium
**Estimated Time:** 4-6 weeks

These improvements remain important but depend on having pedagogically sound content first.

### 2.1 Inline Vocabulary Integration

- Detect new words in episodes
- One-click "Add to vocabulary"
- Post-episode vocabulary summary

### 2.2 Post-Episode Review

- Quick review of new vocabulary
- Test learning objective achievement
- Immediate SRS enrollment

### 2.3 Enhanced Study Sessions

- Show word context from episodes
- Display related images
- Audio pronunciation
- "First seen in Episode X"

### 2.4 Progress Analytics

- Words mastered per week
- Grammar structures learned
- CEFR level estimation
- Learning insights

---

## ✨ PRIORITY 3: Advanced Features

**Priority:** 🔥🔥 MEDIUM (After foundation + learning loop)
**Complexity:** High
**Estimated Time:** 8-12 weeks each

### 3.1 Speaking Practice

- Only pursue after content quality is excellent
- Record and compare pronunciation
- AI feedback using speech recognition
- Conversation simulation for B1+

### 3.2 Writing Practice

- Guided writing prompts
- AI feedback on grammar/vocabulary
- Progressive difficulty

### 3.3 Gamification

- Study streaks
- Achievement badges
- Progress milestones

---

## 📋 Implementation Roadmap

### Q1 2025: Pedagogical Foundation (Critical)

**Weeks 1-4: Pedagogical Planning System**

- [ ] Create PedagogicalProfile domain model
- [ ] Build vocabulary scope management
- [ ] Build grammar scope management
- [ ] Implement syllabus generation with constraints
- [ ] Update wizard Phase 1

**Weeks 5-8: Constrained Content Generation**

- [ ] Level-specific prompt templates (A1, A2, B1)
- [ ] Pedagogical validation system
- [ ] Content regeneration workflow
- [ ] Update wizard Phase 2

**Weeks 9-12: Content Analysis & Exercises**

- [ ] Character map extraction
- [ ] Vocabulary usage analysis
- [ ] Targeted exercise generation
- [ ] Update wizard Phase 3

**Weeks 13-14: Decoupled Media**

- [ ] Separate media generation service
- [ ] Cost estimation
- [ ] Update wizard Phase 4
- [ ] Testing and refinement

**Deliverable:** Complete pedagogical foundation with decoupled architecture

---

### Q2 2025: Learning Loop & Testing

**Weeks 1-4: Learning Loop**

- [ ] Inline vocabulary detection
- [ ] Post-episode review system
- [ ] Enhanced study sessions

**Weeks 5-8: Analytics & Validation**

- [ ] Progress tracking dashboard
- [ ] Learning effectiveness metrics
- [ ] User testing with real students
- [ ] Iterate based on feedback

**Deliverable:** Complete learning loop with validated effectiveness

---

### Q3-Q4 2025: Advanced Features

**Only if foundation is solid:**

- Speaking practice
- Writing practice
- Gamification

---

## 🎯 Success Metrics

### Pedagogical Effectiveness (Primary)

- **Vocabulary Control:** 100% of A1 episodes within budget (8-12 new words)
- **Grammar Coverage:** All CEFR-required structures covered in order
- **Repetition Rate:** 5+ uses of target structure per A1 episode
- **Recycling Rate:** 80%+ of words in episode are from previous episodes
- **Student Performance:** Students can use what was taught (test this!)

### Cost Efficiency (Secondary)

- **Iteration Cost:** 60-80% reduction vs current (no wasted media)
- **Content Quality:** >90% of content passes validation on first generation
- **Media ROI:** Only generate media for approved content

### User Satisfaction (Tertiary)

- **Retention:** Month-over-month retention improves
- **Completion:** Course completion rate >60%
- **Feedback:** Student reports of learning effectiveness

---

## 💭 Key Principles

1. **Pedagogy > Engagement:** For A1, boring but effective beats engaging but confusing
2. **Level-Appropriate:** A1 needs textbook exercises, B1 needs stories
3. **Measure Learning:** Not just engagement metrics, but actual learning outcomes
4. **Iterate Cheaply:** Validate content before expensive operations
5. **Foundation First:** Can't build features on pedagogically unsound content

---

## 🚫 What We're NOT Doing (Yet)

- ❌ Story continuity improvements (until B1+ content)
- ❌ Character personality enhancement (secondary to pedagogy)
- ❌ Cultural authenticity (nice-to-have, not critical for A1)
- ❌ Image quality improvements (happens in Phase 4)
- ❌ New features (until foundation is solid)

These may matter later, but **learning effectiveness comes first**.

---

## 📝 Open Questions

1. **CEFR Word Lists:** Do we have pre-vetted A1/A2/B1 vocabulary lists?
2. **Grammar Sequencing:** Do we have a definitive A1 grammar progression order?
3. **Testing Students:** How will we validate that students are actually learning?
4. **Cost Budget:** What's the acceptable cost per course for media generation?
5. **Iteration Timeline:** How many rounds of content generation before media?

---

**Document Status:** 📋 REVISED - Focuses on Pedagogical Foundation
**Next Review:** After feedback on decoupled architecture approach
**Owner:** Vocabee Product Team

**Last Updated:** November 25, 2024
