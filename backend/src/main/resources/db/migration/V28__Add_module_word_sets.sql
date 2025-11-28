-- Link word sets to course modules and generation module plans
ALTER TABLE word_sets
    ADD COLUMN module_id BIGINT REFERENCES modules (id) ON DELETE CASCADE;
ALTER TABLE word_sets
    ADD COLUMN generation_module_plan_id UUID REFERENCES generation_module_plans (id) ON DELETE SET NULL;

-- Index for quick lookups
CREATE INDEX idx_word_sets_module_id ON word_sets (module_id);
CREATE INDEX idx_word_sets_generation_module_plan_id ON word_sets (generation_module_plan_id);

-- Track which vocabulary phrases mapped to which words (for debugging/reporting)
CREATE TABLE module_vocabulary_mappings
(
    id              BIGSERIAL PRIMARY KEY,
    module_plan_id  UUID         NOT NULL REFERENCES generation_module_plans (id) ON DELETE CASCADE,
    original_phrase VARCHAR(500) NOT NULL,
    word_id         BIGINT       REFERENCES words (id) ON DELETE SET NULL,
    matched         BOOLEAN      NOT NULL DEFAULT false,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_module_vocab_mappings_module ON module_vocabulary_mappings (module_plan_id);
CREATE INDEX idx_module_vocab_mappings_word ON module_vocabulary_mappings (word_id);
