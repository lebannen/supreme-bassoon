-- Initial database schema for Vocabee

-- Languages table for supported languages
CREATE TABLE languages (
    id SERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    entry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_languages_code ON languages(code);


-- Words table (lemmas)
CREATE TABLE words (
    id BIGSERIAL PRIMARY KEY,
    language_code VARCHAR(10) NOT NULL REFERENCES languages(code),
    lemma TEXT NOT NULL,
    normalized TEXT NOT NULL,
    part_of_speech VARCHAR(50),
    etymology TEXT,
    usage_notes TEXT,
    frequency_rank INTEGER,
    is_inflected_form BOOLEAN DEFAULT FALSE,
    lemma_id BIGINT REFERENCES words(id),
    grammatical_features JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (language_code, lemma, part_of_speech)
);

CREATE INDEX idx_words_language ON words(language_code);
CREATE INDEX idx_words_normalized ON words(normalized);
CREATE INDEX idx_words_frequency ON words(frequency_rank) WHERE frequency_rank IS NOT NULL;
CREATE INDEX idx_words_lemma_id ON words(lemma_id) WHERE lemma_id IS NOT NULL;
CREATE INDEX idx_words_grammatical_features ON words USING GIN(grammatical_features) WHERE grammatical_features IS NOT NULL;


-- Word definitions
CREATE TABLE definitions (
    id BIGSERIAL PRIMARY KEY,
    word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    definition_number INTEGER NOT NULL,
    definition_text TEXT NOT NULL,
    metadata JSONB
);

CREATE INDEX idx_definitions_word ON definitions(word_id);


-- Pronunciations (IPA, audio files)
CREATE TABLE pronunciations (
    id BIGSERIAL PRIMARY KEY,
    word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    ipa TEXT,
    audio_url TEXT,
    dialect VARCHAR(50)
);

CREATE INDEX idx_pronunciations_word ON pronunciations(word_id);
CREATE INDEX idx_pronunciations_ipa ON pronunciations(ipa) WHERE ipa IS NOT NULL;


-- Usage examples (linked to definitions)
CREATE TABLE examples (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL REFERENCES definitions(id) ON DELETE CASCADE,
    sentence_text TEXT NOT NULL,
    translation TEXT,
    source TEXT
);

CREATE INDEX idx_examples_definition ON examples(definition_id);


-- Insert supported languages
INSERT INTO languages (code, name) VALUES
    ('es', 'Spanish'),
    ('it', 'Italian'),
    ('ru', 'Russian'),
    ('pt', 'Portuguese'),
    ('fr', 'French'),
    ('de', 'German'),
    ('sv', 'Swedish'),
    ('zh', 'Chinese'),
    ('fi', 'Finnish'),
    ('ja', 'Japanese'),
    ('pl', 'Polish'),
    ('nl', 'Dutch');
