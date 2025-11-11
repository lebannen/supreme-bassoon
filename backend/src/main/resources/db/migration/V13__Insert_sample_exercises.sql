-- Sample Multiple Choice Exercises for French A1
-- Module 1: Greetings and Introductions

-- Exercise 1: Translate "hello"
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'multiple_choice'),
    'fr',
    1,
    'Greetings',
    'A1',
    'Translate: Hello',
    'Choose the correct French translation for the English word.',
    '{
        "question": {
            "type": "text",
            "content": "hello"
        },
        "options": [
            { "id": "a", "text": "bonjour", "isCorrect": true },
            { "id": "b", "text": "au revoir", "isCorrect": false },
            { "id": "c", "text": "merci", "isCorrect": false },
            { "id": "d", "text": "salut", "isCorrect": false }
        ],
        "explanation": "''Bonjour'' is the standard way to say hello in French.",
        "hint": "Think of a formal greeting used during the day."
    }'::jsonb,
    30,
    10,
    1.0,
    true,
    'system'
);

-- Exercise 2: Translate "thank you"
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'multiple_choice'),
    'fr',
    1,
    'Greetings',
    'A1',
    'Translate: Thank you',
    'Choose the correct French translation for the English phrase.',
    '{
        "question": {
            "type": "text",
            "content": "thank you"
        },
        "options": [
            { "id": "a", "text": "bonjour", "isCorrect": false },
            { "id": "b", "text": "merci", "isCorrect": true },
            { "id": "c", "text": "s''il vous plaît", "isCorrect": false },
            { "id": "d", "text": "excusez-moi", "isCorrect": false }
        ],
        "explanation": "''Merci'' means thank you in French.",
        "hint": "This word is commonly used to express gratitude."
    }'::jsonb,
    30,
    10,
    1.0,
    true,
    'system'
);

-- Exercise 3: Translate "parler"
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'multiple_choice'),
    'fr',
    1,
    'Common Verbs',
    'A1',
    'What does "parler" mean?',
    'Choose the correct English translation for the French verb.',
    '{
        "question": {
            "type": "text",
            "content": "parler"
        },
        "options": [
            { "id": "a", "text": "to speak", "isCorrect": true },
            { "id": "b", "text": "to eat", "isCorrect": false },
            { "id": "c", "text": "to sleep", "isCorrect": false },
            { "id": "d", "text": "to work", "isCorrect": false }
        ],
        "explanation": "''Parler'' means to speak in French. It''s a regular -er verb.",
        "hint": "Think about communication and conversation."
    }'::jsonb,
    45,
    10,
    1.5,
    true,
    'system'
);

-- Exercise 4: Translate "manger"
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'multiple_choice'),
    'fr',
    1,
    'Common Verbs',
    'A1',
    'What does "manger" mean?',
    'Choose the correct English translation for the French verb.',
    '{
        "question": {
            "type": "text",
            "content": "manger"
        },
        "options": [
            { "id": "a", "text": "to drink", "isCorrect": false },
            { "id": "b", "text": "to eat", "isCorrect": true },
            { "id": "c", "text": "to sleep", "isCorrect": false },
            { "id": "d", "text": "to walk", "isCorrect": false }
        ],
        "explanation": "''Manger'' means to eat in French.",
        "hint": "This action happens at breakfast, lunch, and dinner."
    }'::jsonb,
    45,
    10,
    1.5,
    true,
    'system'
);

-- Exercise 5: Translate "cat"
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'multiple_choice'),
    'fr',
    2,
    'Animals',
    'A1',
    'Translate: cat',
    'Choose the correct French translation for this common pet.',
    '{
        "question": {
            "type": "text",
            "content": "cat"
        },
        "options": [
            { "id": "a", "text": "le chien", "isCorrect": false },
            { "id": "b", "text": "le chat", "isCorrect": true },
            { "id": "c", "text": "l''oiseau", "isCorrect": false },
            { "id": "d", "text": "le poisson", "isCorrect": false }
        ],
        "explanation": "''Le chat'' means the cat in French. It''s a masculine noun.",
        "hint": "This animal is known for saying ''meow''."
    }'::jsonb,
    40,
    10,
    1.0,
    true,
    'system'
);
