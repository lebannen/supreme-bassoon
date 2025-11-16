-- Course-Driven Architecture
-- Created: November 2025
-- Purpose: Support structured learning paths with courses, modules, and episodes

-- ============================================================================
-- COURSES
-- ============================================================================
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),
    cefr_level VARCHAR(10) DEFAULT 'A1',

    -- Metadata
    description TEXT,
    objectives JSONB, -- Array of learning objectives
    estimated_hours INT DEFAULT 40,

    -- Publishing
    is_published BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(50) DEFAULT 'system'
);

CREATE INDEX idx_courses_language ON courses(language_code, cefr_level);
CREATE INDEX idx_courses_published ON courses(is_published);
CREATE INDEX idx_courses_slug ON courses(slug);

COMMENT ON TABLE courses IS 'Top-level courses (e.g., French A1, Spanish A2)';

-- ============================================================================
-- MODULES
-- ============================================================================
CREATE TABLE modules (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    module_number INT NOT NULL,

    -- Content
    title VARCHAR(255) NOT NULL,
    theme VARCHAR(255),
    description TEXT,
    objectives JSONB, -- Array of module-specific objectives

    -- Metadata
    estimated_minutes INT DEFAULT 120,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    UNIQUE(course_id, module_number)
);

CREATE INDEX idx_modules_course ON modules(course_id, module_number);

COMMENT ON TABLE modules IS 'Modules within a course, each with a specific theme';

-- ============================================================================
-- EPISODES
-- ============================================================================
CREATE TABLE episodes (
    id BIGSERIAL PRIMARY KEY,
    module_id BIGINT NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    episode_number INT NOT NULL,

    -- Type and Content
    episode_type VARCHAR(50) NOT NULL, -- DIALOGUE, STORY, ARTICLE, AUDIO_LESSON
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL, -- Main text/dialogue

    -- Media
    audio_url TEXT,
    transcript TEXT, -- For audio episodes

    -- Metadata
    estimated_minutes INT DEFAULT 15,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    UNIQUE(module_id, episode_number)
);

CREATE INDEX idx_episodes_module ON episodes(module_id, episode_number);
CREATE INDEX idx_episodes_type ON episodes(episode_type);

COMMENT ON TABLE episodes IS 'Learning episodes within modules (stories, dialogues, articles)';
COMMENT ON COLUMN episodes.content IS 'Main episode content - story text, dialogue, or article';

-- ============================================================================
-- EPISODE CONTENT ITEMS (Ordering System)
-- ============================================================================
CREATE TABLE episode_content_items (
    id BIGSERIAL PRIMARY KEY,
    episode_id BIGINT NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    order_index INT NOT NULL,

    -- Content reference
    content_type VARCHAR(50) NOT NULL, -- EXERCISE, GRAMMAR_RULE, VOCABULARY_LIST, CULTURAL_NOTE
    content_id BIGINT NOT NULL, -- References exercises.id, grammar_rules.id, etc.

    -- Settings
    is_required BOOLEAN DEFAULT true,
    points_value INT DEFAULT 0, -- Override default points if needed

    UNIQUE(episode_id, order_index)
);

CREATE INDEX idx_episode_content_episode ON episode_content_items(episode_id, order_index);
CREATE INDEX idx_episode_content_type ON episode_content_items(content_type, content_id);

COMMENT ON TABLE episode_content_items IS 'Ordered list of content items (exercises, grammar) within an episode';

-- ============================================================================
-- UPDATE EXERCISES TABLE
-- ============================================================================
-- Remove old module_number column, add episode relationship
ALTER TABLE exercises DROP COLUMN IF EXISTS module_number;
ALTER TABLE exercises DROP COLUMN IF EXISTS topic;

-- Exercises are now linked via episode_content_items table
-- No direct foreign key to episodes (many-to-many through content_items)

COMMENT ON TABLE exercises IS 'Reusable exercises linked to episodes via episode_content_items';

-- ============================================================================
-- GRAMMAR RULES (For Future)
-- ============================================================================
CREATE TABLE grammar_rules (
    id BIGSERIAL PRIMARY KEY,
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),
    cefr_level VARCHAR(10) DEFAULT 'A1',

    -- Content
    title VARCHAR(255) NOT NULL,
    explanation TEXT NOT NULL, -- Markdown/HTML formatted
    examples JSONB, -- Array of example sentences

    -- Metadata
    category VARCHAR(100), -- e.g., "Verbs", "Articles", "Pronouns"
    difficulty_level INT DEFAULT 1,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_grammar_rules_language ON grammar_rules(language_code, cefr_level);
CREATE INDEX idx_grammar_rules_category ON grammar_rules(category);

COMMENT ON TABLE grammar_rules IS 'Grammar explanations and rules (skeleton for future use)';

-- ============================================================================
-- USER PROGRESS TRACKING
-- ============================================================================

-- Course-level progress
CREATE TABLE user_course_enrollments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,

    -- Progress tracking
    current_module_id BIGINT REFERENCES modules(id),
    current_episode_id BIGINT REFERENCES episodes(id),

    -- Stats
    total_time_spent_seconds INT DEFAULT 0,
    completion_percentage DECIMAL(5,2) DEFAULT 0,

    -- Timestamps
    enrolled_at TIMESTAMP DEFAULT NOW(),
    last_activity_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,

    UNIQUE(user_id, course_id)
);

CREATE INDEX idx_user_enrollments_user ON user_course_enrollments(user_id);
CREATE INDEX idx_user_enrollments_course ON user_course_enrollments(course_id);

-- Module-level progress
CREATE TABLE user_module_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_id BIGINT NOT NULL REFERENCES modules(id) ON DELETE CASCADE,

    -- Progress
    episodes_completed INT DEFAULT 0,
    total_episodes INT DEFAULT 0,
    completion_percentage DECIMAL(5,2) DEFAULT 0,

    -- Stats
    time_spent_seconds INT DEFAULT 0,
    average_score DECIMAL(5,2),

    -- Timestamps
    started_at TIMESTAMP DEFAULT NOW(),
    last_activity_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,

    UNIQUE(user_id, module_id)
);

CREATE INDEX idx_user_module_progress_user ON user_module_progress(user_id);
CREATE INDEX idx_user_module_progress_module ON user_module_progress(module_id);

-- Episode-level progress
CREATE TABLE user_episode_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    episode_id BIGINT NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,

    -- Content consumption
    has_read_content BOOLEAN DEFAULT false,
    has_listened_audio BOOLEAN DEFAULT false,

    -- Exercise completion
    completed_content_items JSONB DEFAULT '[]'::jsonb, -- Array of content_item IDs
    total_content_items INT DEFAULT 0,
    required_content_items_completed INT DEFAULT 0,

    -- Stats
    time_spent_seconds INT DEFAULT 0,
    average_score DECIMAL(5,2),

    -- Status
    is_completed BOOLEAN DEFAULT false,

    -- Timestamps
    started_at TIMESTAMP DEFAULT NOW(),
    last_activity_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,

    UNIQUE(user_id, episode_id)
);

CREATE INDEX idx_user_episode_progress_user ON user_episode_progress(user_id);
CREATE INDEX idx_user_episode_progress_episode ON user_episode_progress(episode_id);
CREATE INDEX idx_user_episode_progress_completed ON user_episode_progress(is_completed);

-- ============================================================================
-- VIEWS FOR CONVENIENCE
-- ============================================================================

-- Course overview with module count
CREATE VIEW course_overview AS
SELECT
    c.*,
    COUNT(DISTINCT m.id) as total_modules,
    COUNT(DISTINCT e.id) as total_episodes,
    SUM(m.estimated_minutes) as total_estimated_minutes
FROM courses c
LEFT JOIN modules m ON m.course_id = c.id
LEFT JOIN episodes e ON e.module_id = m.id
GROUP BY c.id;

COMMENT ON VIEW course_overview IS 'Courses with aggregate statistics';
