-- Initial database schema for Vocabee

CREATE TABLE words (
    id BIGSERIAL PRIMARY KEY,
    language_code VARCHAR(10) NOT NULL,
    lemma TEXT NOT NULL,
    normalized TEXT NOT NULL,
    part_of_speech VARCHAR(50),
    frequency_rank INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (language_code, lemma, part_of_speech)
);

CREATE INDEX idx_words_language ON words(language_code);
CREATE INDEX idx_words_normalized ON words(normalized);
CREATE INDEX idx_words_frequency ON words(frequency_rank) WHERE frequency_rank IS NOT NULL;


CREATE TABLE word_forms (
    id BIGSERIAL PRIMARY KEY,
    word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    form TEXT NOT NULL,
    form_type VARCHAR(50),
    metadata JSONB,
    UNIQUE (word_id, form, form_type)
);

CREATE INDEX idx_word_forms_word ON word_forms(word_id);
CREATE INDEX idx_word_forms_form ON word_forms(form);
CREATE INDEX idx_word_forms_metadata ON word_forms USING GIN(metadata);


CREATE TABLE definitions (
    id BIGSERIAL PRIMARY KEY,
    word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    definition_number INTEGER NOT NULL,
    definition_text TEXT NOT NULL,
    etymology TEXT,
    usage_notes TEXT,
    metadata JSONB
);

CREATE INDEX idx_definitions_word ON definitions(word_id);


CREATE TABLE pronunciations (
    id BIGSERIAL PRIMARY KEY,
    word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
    ipa TEXT,
    audio_url TEXT,
    dialect VARCHAR(50),
    UNIQUE (word_id, ipa, dialect)
);

CREATE INDEX idx_pronunciations_word ON pronunciations(word_id);


CREATE TABLE examples (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT REFERENCES definitions(id) ON DELETE CASCADE,
    word_id BIGINT REFERENCES words(id) ON DELETE CASCADE,
    sentence_text TEXT NOT NULL,
    translation TEXT,
    source TEXT
);

CREATE INDEX idx_examples_definition ON examples(definition_id);
CREATE INDEX idx_examples_word ON examples(word_id);
