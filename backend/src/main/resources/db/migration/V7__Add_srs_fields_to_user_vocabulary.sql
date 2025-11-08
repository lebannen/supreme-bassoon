-- Add Spaced Repetition System (SRS) fields to user_vocabulary table

ALTER TABLE user_vocabulary ADD COLUMN next_review_at TIMESTAMP;
ALTER TABLE user_vocabulary ADD COLUMN review_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE user_vocabulary ADD COLUMN consecutive_successes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE user_vocabulary ADD COLUMN current_interval_hours INTEGER NOT NULL DEFAULT 20;
ALTER TABLE user_vocabulary ADD COLUMN last_reviewed_at TIMESTAMP;
ALTER TABLE user_vocabulary ADD COLUMN ease_factor DECIMAL(3,2) NOT NULL DEFAULT 1.00;

-- Create index for efficient querying of due words
CREATE INDEX idx_user_vocabulary_next_review ON user_vocabulary(user_id, next_review_at);

-- Add comment for documentation
COMMENT ON COLUMN user_vocabulary.next_review_at IS 'When this word is due for review in the SRS system';
COMMENT ON COLUMN user_vocabulary.review_count IS 'Total number of times this word has been reviewed';
COMMENT ON COLUMN user_vocabulary.consecutive_successes IS 'Number of consecutive correct reviews (for interval calculation)';
COMMENT ON COLUMN user_vocabulary.current_interval_hours IS 'Current spacing interval in hours';
COMMENT ON COLUMN user_vocabulary.last_reviewed_at IS 'Timestamp of the last review';
COMMENT ON COLUMN user_vocabulary.ease_factor IS 'Difficulty modifier (1.0 = normal/default, higher = easier intervals, lower = harder/shorter intervals)';
