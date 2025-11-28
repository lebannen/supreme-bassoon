-- User daily activity tracking for daily goals
CREATE TABLE user_daily_activity
(
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    activity_date       DATE      NOT NULL,
    words_reviewed      INT       NOT NULL DEFAULT 0,
    exercises_completed INT       NOT NULL DEFAULT 0,
    episodes_completed  INT       NOT NULL DEFAULT 0,
    study_time_seconds  INT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, activity_date)
);

CREATE INDEX idx_user_daily_activity_user_date ON user_daily_activity (user_id, activity_date DESC);
