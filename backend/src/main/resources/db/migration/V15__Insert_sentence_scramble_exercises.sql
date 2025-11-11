-- Sample Sentence Scramble Exercises for French A1
-- Module 2-6: Basic Sentence Structure

-- Exercise 1: Simple present tense (Module 2)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'sentence_scramble'),
    'fr',
    2,
    'Basic Sentence Structure',
    'A1',
    'Arrange: I speak French',
    'Put the words in the correct order to form a sentence.',
    '{
        "words": ["Je", "parle", "français"],
        "sentence": "Je parle français",
        "translation": "I speak French",
        "grammarExplanation": "Basic French sentence structure: Subject + Verb + Object.",
        "hint": "Start with the subject pronoun ''je''."
    }'::jsonb,
    60,
    15,
    1.5,
    true,
    'system'
);

-- Exercise 2: Negation (Module 2)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'sentence_scramble'),
    'fr',
    2,
    'Negation',
    'A1',
    'Arrange: I do not speak English',
    'Put the words in the correct order to form a negative sentence.',
    '{
        "words": ["Je", "ne", "parle", "pas", "anglais"],
        "sentence": "Je ne parle pas anglais",
        "translation": "I do not speak English",
        "grammarExplanation": "French negation uses ''ne...pas'' around the verb.",
        "hint": "Remember: ne + verb + pas"
    }'::jsonb,
    75,
    20,
    2.0,
    true,
    'system'
);

-- Exercise 3: Question formation (Module 3)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'sentence_scramble'),
    'fr',
    3,
    'Questions',
    'A1',
    'Arrange: How are you?',
    'Put the words in the correct order to form a question.',
    '{
        "words": ["Comment", "allez-vous", "?"],
        "sentence": "Comment allez-vous ?",
        "translation": "How are you? (formal)",
        "grammarExplanation": "''Comment allez-vous'' is the formal way to ask how someone is doing.",
        "hint": "Start with the question word ''Comment''."
    }'::jsonb,
    60,
    15,
    1.5,
    true,
    'system'
);

-- Exercise 4: Adjective placement (Module 3)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'sentence_scramble'),
    'fr',
    3,
    'Adjectives',
    'A1',
    'Arrange: The red cat',
    'Put the words in the correct order.',
    '{
        "words": ["Le", "chat", "rouge"],
        "sentence": "Le chat rouge",
        "translation": "The red cat",
        "grammarExplanation": "Most French adjectives come AFTER the noun, unlike in English.",
        "hint": "Article + Noun + Adjective"
    }'::jsonb,
    60,
    15,
    2.0,
    true,
    'system'
);

-- Exercise 5: Verb avoir (Module 4)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'sentence_scramble'),
    'fr',
    4,
    'Avoir (to have)',
    'A1',
    'Arrange: I have a cat',
    'Put the words in the correct order.',
    '{
        "words": ["J''", "ai", "un", "chat"],
        "sentence": "J''ai un chat",
        "translation": "I have a cat",
        "grammarExplanation": "''Avoir'' (to have) is conjugated as ''ai'' with ''je''.",
        "hint": "J''ai + article + noun"
    }'::jsonb,
    60,
    15,
    1.5,
    true,
    'system'
);

-- Exercise 6: Preposition + place (Module 6)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'sentence_scramble'),
    'fr',
    6,
    'Prepositions',
    'A1',
    'Arrange: I go to the market',
    'Put the words in the correct order.',
    '{
        "words": ["Je", "vais", "au", "marché"],
        "sentence": "Je vais au marché",
        "translation": "I go to the market",
        "grammarExplanation": "''Au'' is the contraction of ''à'' + ''le'' (to the masculine noun).",
        "hint": "Subject + verb aller + au + place"
    }'::jsonb,
    60,
    15,
    2.0,
    true,
    'system'
);
