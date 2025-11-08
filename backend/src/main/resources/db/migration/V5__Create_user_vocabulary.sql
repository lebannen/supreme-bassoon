-- User vocabulary table - tracks which words users have added to their vocabulary
CREATE TABLE user_vocabulary (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    notes TEXT,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, word_id)
);

CREATE INDEX idx_user_vocabulary_user ON user_vocabulary(user_id);
CREATE INDEX idx_user_vocabulary_word ON user_vocabulary(word_id);
CREATE INDEX idx_user_vocabulary_added_at ON user_vocabulary(added_at);
