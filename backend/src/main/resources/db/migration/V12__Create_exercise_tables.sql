-- Exercise System Tables
-- Created: November 2025
-- Purpose: Support interactive language learning exercises

-- Exercise types (system table defining all exercise types)
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

-- Insert initial exercise types
INSERT INTO exercise_types (type_key, display_name, category, interaction_type, description) VALUES
('multiple_choice', 'Multiple Choice', 'vocabulary', 'select', 'Select the correct answer from multiple options'),
('fill_in_blank', 'Fill in the Blank', 'grammar', 'type', 'Complete the sentence with the correct word'),
('sentence_scramble', 'Sentence Scramble', 'grammar', 'drag', 'Arrange words to form a correct sentence'),
('matching', 'Matching', 'vocabulary', 'drag', 'Match words with definitions or images'),
('listening', 'Listening Comprehension', 'listening', 'select', 'Listen to audio and answer questions'),
('cloze_reading', 'Cloze Reading', 'reading', 'select', 'Fill in missing words in a text passage');

-- Exercises table (stores exercise content)
CREATE TABLE exercises (
    id BIGSERIAL PRIMARY KEY,
    exercise_type_id INT NOT NULL REFERENCES exercise_types(id),
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),

    -- Module/Topic Organization
    module_number INT,
    topic VARCHAR(100),
    cefr_level VARCHAR(10) DEFAULT 'A1',

    -- Content
    title VARCHAR(255) NOT NULL,
    instructions TEXT NOT NULL,
    content JSONB NOT NULL,

    -- Metadata
    estimated_duration_seconds INT DEFAULT 60,
    points_value INT DEFAULT 10,
    difficulty_rating DECIMAL(2,1) DEFAULT 1.0,

    -- Status
    is_published BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(50) DEFAULT 'system'
);

-- Indexes for exercises
CREATE INDEX idx_exercises_language ON exercises(language_code, cefr_level);
CREATE INDEX idx_exercises_module ON exercises(module_number, topic);
CREATE INDEX idx_exercises_type ON exercises(exercise_type_id);
CREATE INDEX idx_exercises_published ON exercises(is_published);
CREATE INDEX idx_exercises_content ON exercises USING gin(content);

-- User exercise attempts (tracks user performance)
CREATE TABLE user_exercise_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,

    -- Attempt Details
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    duration_seconds INT,

    -- Results
    score DECIMAL(5,2),
    max_score DECIMAL(5,2) DEFAULT 100,
    is_correct BOOLEAN,

    -- Detailed Response Data
    user_responses JSONB,
    correct_answers JSONB,

    -- Performance
    hints_used INT DEFAULT 0,
    attempts_count INT DEFAULT 1
);

-- Indexes for attempts
CREATE INDEX idx_user_attempts_user ON user_exercise_attempts(user_id);
CREATE INDEX idx_user_attempts_exercise ON user_exercise_attempts(exercise_id);
CREATE INDEX idx_user_attempts_completed ON user_exercise_attempts(user_id, completed_at);
CREATE INDEX idx_user_attempts_score ON user_exercise_attempts(user_id, score DESC);

-- Exercise collections (groups of related exercises)
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

-- Junction table for exercises in collections
CREATE TABLE exercise_collection_items (
    id BIGSERIAL PRIMARY KEY,
    collection_id BIGINT NOT NULL REFERENCES exercise_collections(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    order_index INT NOT NULL,
    is_optional BOOLEAN DEFAULT false,

    UNIQUE(collection_id, exercise_id)
);

-- Index for collection items
CREATE INDEX idx_collection_items_collection ON exercise_collection_items(collection_id, order_index);

-- Comments for documentation
COMMENT ON TABLE exercise_types IS 'Defines all available exercise types in the system';
COMMENT ON TABLE exercises IS 'Stores exercise content with flexible JSONB structure';
COMMENT ON TABLE user_exercise_attempts IS 'Tracks all user attempts at exercises with detailed performance data';
COMMENT ON TABLE exercise_collections IS 'Groups related exercises into curated collections';
COMMENT ON COLUMN exercises.content IS 'JSONB structure varies by exercise type - see exercise-system-design.md';
