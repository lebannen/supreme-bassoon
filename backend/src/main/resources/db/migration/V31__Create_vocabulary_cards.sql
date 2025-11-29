-- AI-generated vocabulary cards as an alternative to Wiktionary-based dictionary
-- These cards are generated in batches with consistent quality and multi-language translations

CREATE TABLE vocabulary_cards
(
    id             BIGSERIAL PRIMARY KEY,

    -- Core word identification
    language_code  VARCHAR(10)  NOT NULL,            -- Source language (e.g., 'fr', 'de')
    lemma          VARCHAR(255) NOT NULL,            -- Base form of the word
    part_of_speech VARCHAR(50),                      -- noun, verb, adjective, etc.

    -- Pronunciation
    ipa            VARCHAR(255),                     -- IPA transcription
    audio_url      TEXT,                             -- Generated audio URL (optional)

    -- Grammar information (language-specific)
    gender         VARCHAR(20),                      -- For nouns: masculine, feminine, neuter
    plural_form    VARCHAR(255),                     -- Plural form if applicable
    verb_group     VARCHAR(50),                      -- For verbs: conjugation group
    grammar_notes  TEXT,                             -- Additional grammar info

    -- Definitions (in source language - simple, CEFR-appropriate)
    definitions    JSONB        NOT NULL,            -- Array of definition strings

    -- Examples (in source language)
    examples       JSONB,                            -- Array of {sentence, translation} objects

    -- Multi-language translations
    translations   JSONB        NOT NULL,            -- {en: [...], es: [...], ru: [...]}

    -- Common inflections (for quick reference)
    inflections    JSONB,                            -- {present: [...], past: [...], etc.}

    -- Metadata
    cefr_level     VARCHAR(5),                       -- A1, A2, B1, B2, C1, C2
    frequency_rank INT,                              -- Word frequency (1 = most common)
    tags           JSONB,                            -- Array of tags: ["food", "travel", etc.]

    -- Source tracking
    word_set_id    BIGINT REFERENCES word_sets (id), -- Which word set this was generated from
    source_word_id BIGINT REFERENCES words (id),     -- Link to original Wiktionary word (if exists)

    -- Generation metadata
    model_used     VARCHAR(100),                     -- AI model used for generation
    generated_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    reviewed_at    TIMESTAMP,                        -- When manually reviewed (if ever)
    review_status  VARCHAR(20) DEFAULT 'pending',    -- pending, approved, rejected

    -- Timestamps
    created_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,

    -- Ensure unique cards per language/lemma/pos combination
    CONSTRAINT uk_vocabulary_cards_lemma UNIQUE (language_code, lemma, part_of_speech)
);

-- Indexes for common queries
CREATE INDEX idx_vocabulary_cards_language ON vocabulary_cards (language_code);
CREATE INDEX idx_vocabulary_cards_lemma ON vocabulary_cards (lemma);
CREATE INDEX idx_vocabulary_cards_cefr ON vocabulary_cards (cefr_level);
CREATE INDEX idx_vocabulary_cards_word_set ON vocabulary_cards (word_set_id);
CREATE INDEX idx_vocabulary_cards_status ON vocabulary_cards (review_status);

-- Trigger to update updated_at
CREATE OR REPLACE FUNCTION update_vocabulary_cards_updated_at()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER vocabulary_cards_updated_at
    BEFORE UPDATE
    ON vocabulary_cards
    FOR EACH ROW
EXECUTE FUNCTION update_vocabulary_cards_updated_at();

-- Add comments for documentation
COMMENT ON TABLE vocabulary_cards IS 'AI-generated vocabulary cards with consistent quality and multi-language translations';
COMMENT ON COLUMN vocabulary_cards.definitions IS 'JSON array of simple definitions appropriate for the CEFR level';
COMMENT ON COLUMN vocabulary_cards.examples IS 'JSON array of {sentence, translation} objects';
COMMENT ON COLUMN vocabulary_cards.translations IS 'JSON object with language codes as keys and arrays of translations as values';
COMMENT ON COLUMN vocabulary_cards.inflections IS 'JSON object with common inflected forms grouped by type';
