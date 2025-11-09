-- Reading texts table
CREATE TABLE reading_texts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),
    level VARCHAR(10),
    topic VARCHAR(100),
    word_count INTEGER,
    description TEXT,
    estimated_minutes INTEGER,
    difficulty VARCHAR(50),
    is_published BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    author VARCHAR(100),
    source VARCHAR(100)
);

CREATE INDEX idx_reading_texts_language ON reading_texts(language_code);
CREATE INDEX idx_reading_texts_level ON reading_texts(level);
CREATE INDEX idx_reading_texts_topic ON reading_texts(topic);
CREATE INDEX idx_reading_texts_published ON reading_texts(is_published);
CREATE INDEX idx_reading_texts_language_level ON reading_texts(language_code, level);

-- User reading progress
CREATE TABLE user_reading_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    text_id BIGINT NOT NULL REFERENCES reading_texts(id) ON DELETE CASCADE,
    current_page INTEGER DEFAULT 1,
    total_pages INTEGER DEFAULT 1,
    completed BOOLEAN DEFAULT false,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    last_read_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, text_id)
);

CREATE INDEX idx_user_reading_progress_user ON user_reading_progress(user_id);
CREATE INDEX idx_user_reading_progress_text ON user_reading_progress(text_id);
CREATE INDEX idx_user_reading_progress_completed ON user_reading_progress(user_id, completed);

COMMENT ON TABLE reading_texts IS 'Reading texts for language learners';
COMMENT ON TABLE user_reading_progress IS 'Tracks individual user progress through reading texts';
COMMENT ON COLUMN reading_texts.level IS 'CEFR level: A1, A2, B1, B2, C1, C2';
COMMENT ON COLUMN reading_texts.topic IS 'Text topic: daily_life, travel, food, culture, etc.';
COMMENT ON COLUMN reading_texts.word_count IS 'Total word count (calculated automatically)';
COMMENT ON COLUMN reading_texts.estimated_minutes IS 'Estimated reading time in minutes';
