-- Word sets table - predefined collections of words (e.g., "French A1 Greetings")
CREATE TABLE word_sets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),
    level VARCHAR(10),  -- CEFR level: A1, A2, B1, B2, C1, C2
    theme VARCHAR(255),
    word_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_word_sets_language ON word_sets(language_code);
CREATE INDEX idx_word_sets_level ON word_sets(level);
CREATE INDEX idx_word_sets_theme ON word_sets(theme);

-- Word set items table - links words to sets
CREATE TABLE word_set_items (
    id BIGSERIAL PRIMARY KEY,
    word_set_id BIGINT NOT NULL REFERENCES word_sets(id) ON DELETE CASCADE,
    word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL DEFAULT 0,
    UNIQUE(word_set_id, word_id)
);

CREATE INDEX idx_word_set_items_set ON word_set_items(word_set_id);
CREATE INDEX idx_word_set_items_word ON word_set_items(word_id);

-- User imported word sets table - tracks which sets a user has imported
CREATE TABLE user_imported_word_sets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    word_set_id BIGINT NOT NULL REFERENCES word_sets(id) ON DELETE CASCADE,
    imported_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, word_set_id)
);

CREATE INDEX idx_user_imported_word_sets_user ON user_imported_word_sets(user_id);
CREATE INDEX idx_user_imported_word_sets_set ON user_imported_word_sets(word_set_id);
