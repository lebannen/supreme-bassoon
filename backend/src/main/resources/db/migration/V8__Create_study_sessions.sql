-- Study sessions table - tracks individual study sessions
CREATE TABLE study_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    word_set_id BIGINT REFERENCES word_sets(id) ON DELETE SET NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    session_size INTEGER NOT NULL,
    total_words INTEGER NOT NULL,
    words_completed INTEGER NOT NULL DEFAULT 0,
    total_attempts INTEGER NOT NULL DEFAULT 0,
    correct_attempts INTEGER NOT NULL DEFAULT 0,
    incorrect_attempts INTEGER NOT NULL DEFAULT 0,
    session_type VARCHAR(50),
    last_shown_word_id BIGINT,
    CONSTRAINT check_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'ABANDONED'))
);

CREATE INDEX idx_study_sessions_user ON study_sessions(user_id);
CREATE INDEX idx_study_sessions_status ON study_sessions(user_id, status);
CREATE INDEX idx_study_sessions_started ON study_sessions(started_at);

COMMENT ON TABLE study_sessions IS 'Tracks individual study sessions for spaced repetition learning';
COMMENT ON COLUMN study_sessions.session_type IS 'Type of session: WORD_SET, VOCABULARY, DUE_REVIEW';
COMMENT ON COLUMN study_sessions.last_shown_word_id IS 'ID of the last word shown to avoid immediate repetition';

-- Study session items table - tracks individual words within a session
CREATE TABLE study_session_items (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE,
    word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    user_vocabulary_id BIGINT REFERENCES user_vocabulary(id) ON DELETE CASCADE,

    -- Session-specific tracking
    attempts INTEGER NOT NULL DEFAULT 0,
    correct_count INTEGER NOT NULL DEFAULT 0,
    incorrect_count INTEGER NOT NULL DEFAULT 0,
    consecutive_correct INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    last_shown_at TIMESTAMP,
    display_order INTEGER,

    -- Result tracking
    final_result VARCHAR(20),

    CONSTRAINT check_item_status CHECK (status IN ('NEW', 'LEARNING', 'COMPLETED')),
    CONSTRAINT check_final_result CHECK (final_result IN ('SUCCESS', 'FAILED', 'SKIPPED', NULL))
);

CREATE INDEX idx_session_items_session ON study_session_items(session_id);
CREATE INDEX idx_session_items_word ON study_session_items(word_id);
CREATE INDEX idx_session_items_status ON study_session_items(session_id, status);

COMMENT ON TABLE study_session_items IS 'Tracks individual words within a study session';
COMMENT ON COLUMN study_session_items.consecutive_correct IS 'Consecutive correct answers in this session (need 2 to complete)';
COMMENT ON COLUMN study_session_items.status IS 'NEW = not yet shown, LEARNING = shown but not mastered, COMPLETED = 2 consecutive correct';

-- Study session attempts table - detailed log of each answer
CREATE TABLE study_session_attempts (
    id BIGSERIAL PRIMARY KEY,
    session_item_id BIGINT NOT NULL REFERENCES study_session_items(id) ON DELETE CASCADE,
    attempted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    was_correct BOOLEAN NOT NULL,
    response_time_ms INTEGER
);

CREATE INDEX idx_session_attempts_item ON study_session_attempts(session_item_id);

COMMENT ON TABLE study_session_attempts IS 'Detailed log of every answer attempt during study sessions';
COMMENT ON COLUMN study_session_attempts.response_time_ms IS 'Time taken to answer in milliseconds (optional)';
