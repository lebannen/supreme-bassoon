-- Course AI Reviews
-- Stores AI-generated feedback on course quality, structure, and CEFR alignment

CREATE TABLE course_reviews
(
    id                    BIGSERIAL PRIMARY KEY,
    course_id             BIGINT      NOT NULL REFERENCES courses (id) ON DELETE CASCADE,

    -- Review metadata
    review_type           VARCHAR(50) NOT NULL DEFAULT 'FULL_REVIEW', -- FULL_REVIEW, QUICK_CHECK, CEFR_ALIGNMENT
    model_used            VARCHAR(100),                               -- e.g., "gemini-2.0-flash"

    -- Review content
    overall_score         INT,                                        -- 1-100
    cefr_alignment_score  INT,                                        -- 1-100, how well it matches stated CEFR level
    structure_score       INT,                                        -- 1-100, course structure and progression
    content_quality_score INT,                                        -- 1-100, quality of dialogues, exercises, etc.

    summary               TEXT        NOT NULL,                       -- Brief overall summary
    strengths             JSONB,                                      -- Array of strength points
    weaknesses            JSONB,                                      -- Array of weakness/improvement points
    recommendations       JSONB,                                      -- Array of specific recommendations

    -- Detailed feedback (optional, for full reviews)
    module_feedback       JSONB,                                      -- Per-module feedback
    detailed_analysis     TEXT,                                       -- Full detailed analysis

    -- Timestamps
    created_at            TIMESTAMP            DEFAULT NOW(),
    reviewed_by           VARCHAR(100)                                -- 'system' or user email who triggered it
);

CREATE INDEX idx_course_reviews_course ON course_reviews (course_id);
CREATE INDEX idx_course_reviews_created ON course_reviews (created_at DESC);

COMMENT ON TABLE course_reviews IS 'AI-generated reviews of course quality and CEFR alignment';
