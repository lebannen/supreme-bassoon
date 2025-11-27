-- ============================================================================
-- Course Generation Pipeline - Workflow Tables
-- Created: November 2025
-- Purpose: Support granular, multi-stage course generation with checkpoints
-- ============================================================================

-- ============================================================================
-- ENHANCE GRAMMAR RULES TABLE
-- ============================================================================
ALTER TABLE grammar_rules
    ADD COLUMN IF NOT EXISTS slug VARCHAR(100);
ALTER TABLE grammar_rules
    ADD COLUMN IF NOT EXISTS usage_notes TEXT;
ALTER TABLE grammar_rules
    ADD COLUMN IF NOT EXISTS common_errors JSONB;

-- Add unique constraint on slug per language
CREATE UNIQUE INDEX IF NOT EXISTS idx_grammar_rules_slug ON grammar_rules (language_code, slug) WHERE slug IS NOT NULL;

-- Rename 'title' to 'name' for consistency (if exists)
ALTER TABLE grammar_rules
    RENAME COLUMN title TO name;
ALTER TABLE grammar_rules
    RENAME COLUMN explanation TO description;

COMMENT ON COLUMN grammar_rules.slug IS 'URL-friendly identifier for the rule (e.g., present-er-verbs)';
COMMENT ON COLUMN grammar_rules.usage_notes IS 'Additional notes on when/how to use this grammar rule';
COMMENT ON COLUMN grammar_rules.common_errors IS 'JSON array of common mistakes learners make with this rule';

-- ============================================================================
-- GENERATION STAGES ENUM TYPE
-- ============================================================================
CREATE TYPE generation_stage AS ENUM (
    'BLUEPRINT',
    'MODULE_PLANNING',
    'EPISODE_CONTENT',
    'CHARACTER_PROFILES',
    'EXERCISES',
    'MEDIA',
    'COMPLETED',
    'FAILED'
    );

CREATE TYPE generation_step_status AS ENUM (
    'PENDING',
    'IN_PROGRESS',
    'COMPLETED',
    'FAILED'
    );

-- ============================================================================
-- MAIN GENERATION WORKFLOW TABLE
-- ============================================================================
CREATE TABLE course_generations
(
    id                  UUID PRIMARY KEY          DEFAULT gen_random_uuid(),

    -- Link to course (created during publish, null during generation)
    course_id           BIGINT           REFERENCES courses (id) ON DELETE SET NULL,

    -- Generation parameters
    language_code       VARCHAR(10)      NOT NULL REFERENCES languages (code),
    cefr_level          VARCHAR(10)      NOT NULL,
    module_count        INT              NOT NULL,
    episodes_per_module INT              NOT NULL DEFAULT 2,
    theme_preferences   TEXT,

    -- Progress tracking
    current_stage       generation_stage NOT NULL DEFAULT 'BLUEPRINT',
    error_message       TEXT,

    -- Timestamps
    created_at          TIMESTAMP                 DEFAULT NOW(),
    updated_at          TIMESTAMP                 DEFAULT NOW(),
    completed_at        TIMESTAMP
);

CREATE INDEX idx_course_generations_stage ON course_generations (current_stage);
CREATE INDEX idx_course_generations_course ON course_generations (course_id);

COMMENT ON TABLE course_generations IS 'Tracks the state of course generation workflows';

-- ============================================================================
-- BLUEPRINT (Stage 1 Output)
-- ============================================================================
CREATE TABLE generation_blueprints
(
    id                   UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    generation_id        UUID NOT NULL REFERENCES course_generations (id) ON DELETE CASCADE,

    -- Course metadata
    course_title         VARCHAR(200),
    course_description   TEXT,

    -- Plot and setting
    setting              TEXT,
    premise              TEXT,
    plot_arc             JSONB, -- [{module: 1, arc_point: "Introduction"}]

    -- Module topics
    module_topics        JSONB, -- [{module: 1, topic: "Greetings"}]

    -- Grammar distribution across course
    grammar_distribution JSONB, -- [{module: 1, rules: ["present-er-verbs"]}]

    -- Status
    status               generation_step_status DEFAULT 'PENDING',
    raw_response         TEXT,  -- Store raw AI response for debugging

    created_at           TIMESTAMP              DEFAULT NOW(),

    UNIQUE (generation_id)
);

CREATE INDEX idx_gen_blueprints_generation ON generation_blueprints (generation_id);

COMMENT ON TABLE generation_blueprints IS 'Stage 1 output: course structure, plot, and grammar distribution';

-- ============================================================================
-- CHARACTER REGISTRY (Builds up during generation)
-- ============================================================================
CREATE TABLE generation_characters
(
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    generation_id          UUID         NOT NULL REFERENCES course_generations (id) ON DELETE CASCADE,

    -- Identity
    name                   VARCHAR(100) NOT NULL,
    role                   VARCHAR(50),                          -- PROTAGONIST, SUPPORTING, MINOR, RECURRING

    -- Initial seeds (from blueprint)
    initial_description    TEXT,
    age_range              VARCHAR(50),
    personality_traits     JSONB,                                -- ["friendly", "curious"]
    background             TEXT,

    -- Built up during episode generation
    appearance_description TEXT,

    -- For media generation (populated in Stage 4)
    voice_id               VARCHAR(50),
    reference_image_url    TEXT,

    -- Track character developments across episodes
    developments           JSONB            DEFAULT '[]'::jsonb, -- [{episode_id, note}]

    created_at             TIMESTAMP        DEFAULT NOW(),
    updated_at             TIMESTAMP        DEFAULT NOW()
);

CREATE INDEX idx_gen_characters_generation ON generation_characters (generation_id);
CREATE INDEX idx_gen_characters_name ON generation_characters (generation_id, name);

COMMENT ON TABLE generation_characters IS 'Character registry that evolves during episode generation';

-- ============================================================================
-- MODULE PLANS (Stage 2 Output)
-- ============================================================================
CREATE TABLE generation_module_plans
(
    id            UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    generation_id UUID NOT NULL REFERENCES course_generations (id) ON DELETE CASCADE,
    module_number INT  NOT NULL,

    -- Module metadata
    title         VARCHAR(200),
    theme         VARCHAR(200),
    description   TEXT,
    objectives    JSONB, -- ["Learn to introduce yourself"]

    -- Plot progression for this module
    plot_summary  TEXT,

    -- Status
    status        generation_step_status DEFAULT 'PENDING',
    raw_response  TEXT,

    created_at    TIMESTAMP              DEFAULT NOW(),

    UNIQUE (generation_id, module_number)
);

CREATE INDEX idx_gen_module_plans_generation ON generation_module_plans (generation_id);

COMMENT ON TABLE generation_module_plans IS 'Stage 2 output: module-level plans with episode outlines';

-- ============================================================================
-- EPISODE PLANS (Stage 2 Output - child of module plan)
-- ============================================================================
CREATE TABLE generation_episode_plans
(
    id                UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    module_plan_id    UUID NOT NULL REFERENCES generation_module_plans (id) ON DELETE CASCADE,
    episode_number    INT  NOT NULL,

    -- Episode outline
    title             VARCHAR(200),
    scene_description TEXT,
    episode_type      VARCHAR(50)            DEFAULT 'DIALOGUE', -- DIALOGUE, STORY, ARTICLE

    -- Learning objectives
    vocabulary        JSONB,                                     -- ["bonjour", "café", "merci"]
    grammar_rules     JSONB,                                     -- ["present-er-verbs"]

    -- Characters in this episode
    character_ids     JSONB,                                     -- [uuid, uuid]

    -- Plot points
    plot_points       TEXT,

    -- Status
    status            generation_step_status DEFAULT 'PENDING',

    created_at        TIMESTAMP              DEFAULT NOW(),

    UNIQUE (module_plan_id, episode_number)
);

CREATE INDEX idx_gen_episode_plans_module ON generation_episode_plans (module_plan_id);

COMMENT ON TABLE generation_episode_plans IS 'Episode outlines with vocabulary and grammar assignments';

-- ============================================================================
-- EPISODE CONTENT (Stage 3 Output)
-- ============================================================================
CREATE TABLE generation_episode_content
(
    id                     UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    episode_plan_id        UUID NOT NULL REFERENCES generation_episode_plans (id) ON DELETE CASCADE,

    -- Generated content
    content_type           VARCHAR(50), -- DIALOGUE, STORY
    content                TEXT,        -- Full text of the episode
    content_structured     JSONB,       -- [{speaker: "Marie", text: "Bonjour!"}]

    -- For context continuity
    summary                TEXT,        -- Brief summary for next episode's context
    character_developments JSONB,       -- [{character_id, note}]

    -- Vocabulary tracking
    vocabulary_used        JSONB,       -- Words from target list that were used
    vocabulary_missing     JSONB,       -- Words that couldn't be incorporated

    -- Status
    status                 generation_step_status DEFAULT 'PENDING',
    raw_response           TEXT,

    created_at             TIMESTAMP              DEFAULT NOW(),

    UNIQUE (episode_plan_id)
);

CREATE INDEX idx_gen_episode_content_plan ON generation_episode_content (episode_plan_id);

COMMENT ON TABLE generation_episode_content IS 'Stage 3 output: generated dialogue/story content';

-- ============================================================================
-- EXERCISES (Stage 5 Output)
-- ============================================================================
CREATE TABLE generation_exercises
(
    id                  UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    episode_plan_id     UUID NOT NULL REFERENCES generation_episode_plans (id) ON DELETE CASCADE,

    -- Generated exercises
    exercises           JSONB, -- Array of exercise objects
    exercise_count      INT,

    -- Coverage tracking
    vocabulary_coverage JSONB, -- Which vocab words are tested
    grammar_coverage    JSONB, -- Which grammar rules are tested

    -- Status
    status              generation_step_status DEFAULT 'PENDING',
    raw_response        TEXT,

    created_at          TIMESTAMP              DEFAULT NOW(),

    UNIQUE (episode_plan_id)
);

CREATE INDEX idx_gen_exercises_plan ON generation_exercises (episode_plan_id);

COMMENT ON TABLE generation_exercises IS 'Stage 5 output: exercises for each episode';

-- ============================================================================
-- MEDIA GENERATION TRACKING (Stage 6)
-- ============================================================================
CREATE TABLE generation_media
(
    id              UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    generation_id   UUID        NOT NULL REFERENCES course_generations (id) ON DELETE CASCADE,

    -- What this media is for
    episode_plan_id UUID REFERENCES generation_episode_plans (id) ON DELETE CASCADE,
    character_id    UUID REFERENCES generation_characters (id) ON DELETE CASCADE,

    -- Media details
    media_type      VARCHAR(50) NOT NULL, -- EPISODE_AUDIO, CHARACTER_IMAGE, SCENE_IMAGE

    -- Result
    url             TEXT,
    metadata        JSONB,                -- Additional info (duration, dimensions, etc.)

    -- Status
    status          generation_step_status DEFAULT 'PENDING',
    error_message   TEXT,

    created_at      TIMESTAMP              DEFAULT NOW()
);

CREATE INDEX idx_gen_media_generation ON generation_media (generation_id);
CREATE INDEX idx_gen_media_episode ON generation_media (episode_plan_id);
CREATE INDEX idx_gen_media_character ON generation_media (character_id);
CREATE INDEX idx_gen_media_type ON generation_media (media_type);

COMMENT ON TABLE generation_media IS 'Stage 6 output: audio and images';

-- ============================================================================
-- HELPER VIEW: Generation Progress
-- ============================================================================
CREATE VIEW generation_progress AS
SELECT g.id,
       g.language_code,
       g.cefr_level,
       g.module_count,
       g.episodes_per_module,
       g.current_stage,
       g.created_at,
       g.completed_at,

       -- Blueprint status
       b.status                                                                                                    as blueprint_status,

       -- Module planning progress
       (SELECT COUNT(*)
        FROM generation_module_plans mp
        WHERE mp.generation_id = g.id
          AND mp.status = 'COMPLETED')                                                                             as modules_planned,

       -- Episode content progress
       (SELECT COUNT(*)
        FROM generation_episode_content ec
                 JOIN generation_episode_plans ep ON ec.episode_plan_id = ep.id
                 JOIN generation_module_plans mp ON ep.module_plan_id = mp.id
        WHERE mp.generation_id = g.id
          AND ec.status = 'COMPLETED')                                                                             as episodes_generated,

       -- Total expected episodes
       g.module_count * g.episodes_per_module                                                                      as total_episodes,

       -- Character count
       (SELECT COUNT(*)
        FROM generation_characters c
        WHERE c.generation_id = g.id)                                                                              as character_count,

       -- Exercise progress
       (SELECT COUNT(*)
        FROM generation_exercises ex
                 JOIN generation_episode_plans ep ON ex.episode_plan_id = ep.id
                 JOIN generation_module_plans mp ON ep.module_plan_id = mp.id
        WHERE mp.generation_id = g.id
          AND ex.status = 'COMPLETED')                                                                             as episodes_with_exercises,

       -- Media progress
       (SELECT COUNT(*)
        FROM generation_media m
        WHERE m.generation_id = g.id
          AND m.status = 'COMPLETED')                                                                              as media_generated

FROM course_generations g
         LEFT JOIN generation_blueprints b ON b.generation_id = g.id;

COMMENT ON VIEW generation_progress IS 'Overview of generation progress across all stages';
