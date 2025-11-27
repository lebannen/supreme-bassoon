# Course Generation Pipeline - Roadmap

## Overview

This document outlines the new granular, multi-stage course generation pipeline that replaces the current all-in-one
approach. The new system provides better control, reviewability, and quality output through structured checkpoints and
context accumulation.

## Goals

1. **Vocabulary-first design** - Words are intentionally selected per episode, not emergent byproducts
2. **Grammar integration** - Explicit grammar rule assignment and tracking
3. **Plot continuity** - Context accumulates across episodes for coherent storytelling
4. **Minimal manual intervention** - Optimize for quality output; checkpoints are for validation, not correction
5. **Resumability** - If generation fails at any stage, previous work is preserved

---

## Pipeline Stages

```
┌─────────────────────────────────────────────────────────────────────┐
│  STAGE 1: BLUEPRINT                                                 │
│  ─────────────────                                                  │
│  Input:  Language, CEFR level, module count, theme preferences      │
│  Output:                                                            │
│    • Course title & description                                     │
│    • Module topics (1 per module)                                   │
│    • Plot arc + setting + premise                                   │
│    • Character seeds (name, role, age, personality)                 │
│    • Grammar taxonomy for entire course                             │
│    • Grammar distribution across modules                            │
│                                                                     │
│  ✓ CHECKPOINT: Review blueprint before proceeding                   │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│  STAGE 2: MODULE PLANNING (sequential, all modules)                 │
│  ──────────────────────────────────────────────────                 │
│  Input:  Blueprint, previous module plans (for continuity)          │
│  Output per module:                                                 │
│    • Module title, theme, description, objectives                   │
│    • Episode outlines (2-3 per module)                              │
│    • Per-episode: scene description, vocabulary (15-20 words),      │
│      grammar assignments (1-2), character appearances               │
│                                                                     │
│  Context: Previous module plans for plot continuity                 │
│                                                                     │
│  ✓ CHECKPOINT: Review all module plans + vocabulary lists           │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│  STAGE 3: EPISODE CONTENT (sequential, all episodes)                │
│  ───────────────────────────────────────────────────                │
│  Input:  Episode plan, vocabulary, grammar, character registry,     │
│          previous episode summaries                                 │
│  Output per episode:                                                │
│    • Full dialogue/story incorporating vocabulary + grammar         │
│    • Structured content (speaker/text pairs)                        │
│    • Episode summary (for next episode's context)                   │
│    • Character development notes                                    │
│    • Vocabulary usage report (used vs. missed)                      │
│                                                                     │
│  Context: Character registry, all previous episode summaries        │
│                                                                     │
│  ✓ CHECKPOINT: Review all episode content                           │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│  STAGE 4: CHARACTER PROFILES                                        │
│  ───────────────────────────                                        │
│  Input:  Character registry with accumulated developments           │
│  Output:                                                            │
│    • Consolidated character descriptions                            │
│    • Detailed physical descriptions (for image generation)          │
│    • Personality summaries (for voice selection)                    │
│    • Reference images (generated)                                   │
│    • Voice assignments                                              │
│                                                                     │
│  ✓ CHECKPOINT: Review characters + images + voice assignments       │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│  STAGE 5: EXERCISES (all episodes)                                  │
│  ─────────────────────────────────                                  │
│  Input:  Episode content, focus vocabulary, grammar rules           │
│  Output per episode:                                                │
│    • 13+ exercises targeting learning objectives                    │
│    • Vocabulary coverage report                                     │
│    • Grammar coverage report                                        │
│                                                                     │
│  ✓ CHECKPOINT: Review exercises (sampling)                          │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│  STAGE 6: MEDIA GENERATION                                          │
│  ─────────────────────────                                          │
│  Input:  Episode content, character profiles with voice/image refs  │
│  Output:                                                            │
│    • Audio for all episodes (with consistent voice assignments)     │
│    • Scene images (with character reference consistency)            │
│                                                                     │
│  ✓ CHECKPOINT: Final review                                         │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
                            PUBLISH COURSE
```

---

## Implementation Phases

### Phase 1: Foundation (Database & Entities)

- [x] Design database schema
- [ ] Create migration V23 for grammar_rules table
- [ ] Create migration V24 for generation workflow tables
- [ ] Implement JPA entities for all new tables
- [ ] Implement repositories

### Phase 2: Blueprint Generation (Stage 1)

- [ ] Implement BlueprintGenerationService
- [ ] Grammar taxonomy generation/reuse logic
- [ ] Character seed creation
- [ ] CourseGenerationController - start endpoint
- [ ] DTOs for blueprint stage

### Phase 3: Module Planning (Stage 2)

- [ ] Implement ModulePlanGenerationService
- [ ] Episode plan generation with vocabulary lists
- [ ] Grammar rule assignment logic
- [ ] Context building from blueprint

### Phase 4: Episode Content (Stage 3)

- [ ] Implement EpisodeContentGenerationService
- [ ] Context accumulation (previous summaries)
- [ ] Character registry updates
- [ ] Vocabulary usage tracking

### Phase 5: Character Profiles (Stage 4)

- [ ] Implement CharacterProfileConsolidationService
- [ ] Physical description generation
- [ ] Reference image generation
- [ ] Voice assignment logic

### Phase 6: Exercises (Stage 5)

- [ ] Adapt existing ExerciseGenerationService
- [ ] Vocabulary-targeted exercise generation
- [ ] Grammar-targeted exercise generation
- [ ] Coverage reporting

### Phase 7: Media Generation (Stage 6)

- [ ] Adapt existing AudioGenerationService
- [ ] Scene image generation
- [ ] Character consistency via reference images

### Phase 8: API & Frontend

- [ ] Complete CourseGenerationController endpoints
- [ ] Progress tracking API
- [ ] Regeneration support
- [ ] Publish to course flow
- [ ] Admin UI for generation workflow

---

## Database Schema

### Grammar Rules (Reusable across courses)

```sql
CREATE TABLE grammar_rules (
    id SERIAL PRIMARY KEY,
    language_code VARCHAR(10) NOT NULL,
    cefr_level VARCHAR(5) NOT NULL,
    category VARCHAR(50) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    examples JSONB,
    usage_notes TEXT,
    common_errors JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(language_code, slug)
);
```

### Generation Workflow

```sql
-- Main generation tracking
CREATE TABLE course_generations (
    id UUID PRIMARY KEY,
    course_id BIGINT REFERENCES courses(id),
    language_code VARCHAR(10) NOT NULL,
    cefr_level VARCHAR(5) NOT NULL,
    module_count INT NOT NULL,
    episodes_per_module INT NOT NULL DEFAULT 2,
    theme_preferences TEXT,
    current_stage VARCHAR(50) NOT NULL DEFAULT 'BLUEPRINT',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- Stage 1 output
CREATE TABLE generation_blueprints (
    id UUID PRIMARY KEY,
    generation_id UUID REFERENCES course_generations(id),
    course_title VARCHAR(200),
    course_description TEXT,
    setting TEXT,
    premise TEXT,
    plot_arc JSONB,
    module_topics JSONB,
    grammar_distribution JSONB,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Character registry
CREATE TABLE generation_characters (
    id UUID PRIMARY KEY,
    generation_id UUID REFERENCES course_generations(id),
    name VARCHAR(100) NOT NULL,
    role VARCHAR(50),
    initial_description TEXT,
    age_range VARCHAR(30),
    personality_traits JSONB,
    appearance_description TEXT,
    background TEXT,
    voice_id VARCHAR(50),
    reference_image_url TEXT,
    developments JSONB DEFAULT '[]',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Stage 2 output
CREATE TABLE generation_module_plans (
    id UUID PRIMARY KEY,
    generation_id UUID REFERENCES course_generations(id),
    module_number INT NOT NULL,
    title VARCHAR(200),
    theme VARCHAR(200),
    description TEXT,
    objectives JSONB,
    plot_summary TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(generation_id, module_number)
);

CREATE TABLE generation_episode_plans (
    id UUID PRIMARY KEY,
    module_plan_id UUID REFERENCES generation_module_plans(id),
    episode_number INT NOT NULL,
    title VARCHAR(200),
    scene_description TEXT,
    episode_type VARCHAR(50) DEFAULT 'DIALOGUE',
    vocabulary JSONB,
    grammar_rules JSONB,
    character_ids JSONB,
    plot_points TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(module_plan_id, episode_number)
);

-- Stage 3 output
CREATE TABLE generation_episode_content (
    id UUID PRIMARY KEY,
    episode_plan_id UUID REFERENCES generation_episode_plans(id),
    content_type VARCHAR(50),
    content TEXT,
    content_structured JSONB,
    summary TEXT,
    character_developments JSONB,
    vocabulary_used JSONB,
    vocabulary_missing JSONB,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Stage 5 output
CREATE TABLE generation_exercises (
    id UUID PRIMARY KEY,
    episode_plan_id UUID REFERENCES generation_episode_plans(id),
    exercises JSONB,
    exercise_count INT,
    vocabulary_coverage JSONB,
    grammar_coverage JSONB,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Stage 6 output
CREATE TABLE generation_media (
    id UUID PRIMARY KEY,
    generation_id UUID REFERENCES course_generations(id),
    episode_plan_id UUID REFERENCES generation_episode_plans(id),
    character_id UUID REFERENCES generation_characters(id),
    media_type VARCHAR(50),
    url TEXT,
    metadata JSONB,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## API Endpoints

### Generation Management

```
POST   /api/admin/generations                    # Start new generation
GET    /api/admin/generations                    # List all generations
GET    /api/admin/generations/{id}               # Get progress + all content
POST   /api/admin/generations/{id}/proceed       # Approve & proceed to next stage
POST   /api/admin/generations/{id}/regenerate    # Regenerate current stage
DELETE /api/admin/generations/{id}               # Cancel generation
POST   /api/admin/generations/{id}/publish       # Publish to actual course
```

### Response Structure

```json
{
  "id": "uuid",
  "currentStage": "MODULE_PLANNING",
  "stageProgress": {
    "completed": 2,
    "total": 4,
    "currentItem": "Generating Module 3..."
  },
  "blueprint": {
    "courseTitle": "Café des Rêves",
    "setting": "Modern Paris...",
    "characters": [...],
    "moduleTopics": [...],
    "grammarDistribution": [...]
  },
  "modulePlans": [
    {
      "moduleNumber": 1,
      "title": "Greetings & Introductions",
      "episodes": [
        {
          "episodeNumber": 1,
          "title": "First Day",
          "vocabulary": ["bonjour", "café", ...],
          "grammarRules": ["present-er-verbs"],
          "status": "COMPLETED"
        }
      ]
    }
  ],
  "episodes": [...],
  "characters": [...],
  "canProceed": false,
  "canPublish": false
}
```

---

## Context Flow

### Episode Generation Context

Each episode receives:

1. **Series context** - Setting, premise from blueprint
2. **Character profiles** - Current state including recent developments
3. **Plot history** - Summaries of all previous episodes
4. **Learning objectives** - Vocabulary list + grammar rules for this episode
5. **Scene setup** - Description of where/when this episode takes place

### Character Registry

Characters are living entities that evolve:

- **Created** in Blueprint stage with initial seeds
- **Updated** after each episode with new developments
- **Consolidated** in Character Profiles stage for media generation

---

## Quality Considerations

### Vocabulary Validation

- Track which target words were used in each episode
- Flag episodes where vocabulary coverage is low
- Consider validation against dictionary database

### Grammar Integration

- Grammar rules are selected from taxonomy (generated or existing)
- Each episode has 1-2 assigned grammar rules
- Exercises specifically target assigned grammar

### Plot Continuity

- Previous episode summaries provide context
- Character developments accumulate
- Module-level plot arcs guide episode generation

---

## Future Enhancements

1. **Branching/regeneration** - Regenerate specific episodes without losing others
2. **Vocabulary constraints** - Limit AI to words in dictionary database
3. **Parallel generation** - Generate independent modules concurrently
4. **Human-in-the-loop editing** - Edit generated content before proceeding
5. **Quality scoring** - Automated assessment of vocabulary coverage, grammar usage
6. **Template support** - Pre-defined course structures for common patterns
