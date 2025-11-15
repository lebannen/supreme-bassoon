-- Insert Listening exercises for French A1

-- Exercise 1: Greetings (Multiple Choice)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'listening'),
    'fr',
    1,
    'Greetings',
    'A1',
    'Listen: French Greeting',
    'Listen to the audio and select the correct greeting',
    '{
        "audioUrl": "/audio/fr/greetings/bonjour.mp3",
        "question": "What greeting did you hear?",
        "questionType": "multiple_choice",
        "options": [
            {"id": "a", "text": "Bonjour"},
            {"id": "b", "text": "Bonsoir"},
            {"id": "c", "text": "Au revoir"},
            {"id": "d", "text": "Salut"}
        ],
        "correctAnswer": "a",
        "transcript": "Bonjour",
        "explanation": "The speaker said ''Bonjour'', which means ''Hello'' or ''Good day'' in French.",
        "hint": "Listen for the time of day mentioned in the greeting"
    }'::jsonb,
    60,
    10,
    1.0,
    true,
    'system'
);

-- Exercise 2: Numbers (Text Input)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'listening'),
    'fr',
    2,
    'Numbers',
    'A1',
    'Listen: Number Dictation',
    'Listen to the audio and type the number you hear',
    '{
        "audioUrl": "/audio/fr/numbers/cinq.mp3",
        "question": "What number did you hear?",
        "questionType": "text_input",
        "correctAnswer": ["cinq", "5"],
        "transcript": "Le nombre est cinq",
        "explanation": "The speaker said ''Le nombre est cinq'', which means ''The number is five'' in French.",
        "hint": "Listen for the number word at the end of the sentence"
    }'::jsonb,
    60,
    10,
    1.5,
    true,
    'system'
);

-- Exercise 3: Common Phrases (Multiple Choice)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'listening'),
    'fr',
    3,
    'Phrases',
    'A1',
    'Listen: Common Phrase',
    'Listen to the audio and select what the speaker is saying',
    '{
        "audioUrl": "/audio/fr/phrases/comment_allez_vous.mp3",
        "question": "What phrase did you hear?",
        "questionType": "multiple_choice",
        "options": [
            {"id": "a", "text": "Comment allez-vous?"},
            {"id": "b", "text": "Comment vous appelez-vous?"},
            {"id": "c", "text": "Où allez-vous?"},
            {"id": "d", "text": "Que faites-vous?"}
        ],
        "correctAnswer": "a",
        "transcript": "Comment allez-vous?",
        "explanation": "''Comment allez-vous?'' is the formal way to ask ''How are you?'' in French.",
        "hint": "The speaker is asking about someone''s well-being"
    }'::jsonb,
    90,
    10,
    2.0,
    true,
    'system'
);

-- Exercise 4: Days of the Week (Text Input)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'listening'),
    'fr',
    4,
    'Time',
    'A1',
    'Listen: Day of the Week',
    'Listen to the audio and type the day of the week you hear',
    '{
        "audioUrl": "/audio/fr/days/lundi.mp3",
        "question": "Type the day of the week you heard:",
        "questionType": "text_input",
        "correctAnswer": "lundi",
        "transcript": "lundi",
        "explanation": "''Lundi'' means ''Monday'' in French. Remember that days of the week are not capitalized in French.",
        "hint": "Days of the week in French are not capitalized"
    }'::jsonb,
    60,
    10,
    1.5,
    true,
    'system'
);

-- Exercise 5: Food Items (Multiple Choice)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'listening'),
    'fr',
    5,
    'Food',
    'A1',
    'Listen: Food Item',
    'Listen to the audio and select the food item mentioned',
    '{
        "audioUrl": "/audio/fr/food/le_pain.mp3",
        "question": "What food item did you hear?",
        "questionType": "multiple_choice",
        "options": [
            {"id": "a", "text": "le pain (bread)"},
            {"id": "b", "text": "le vin (wine)"},
            {"id": "c", "text": "le lait (milk)"},
            {"id": "d", "text": "le fromage (cheese)"}
        ],
        "correctAnswer": "a",
        "transcript": "le pain",
        "explanation": "''Le pain'' means ''the bread'' in French. ''Le'' is the masculine article.",
        "hint": "Listen carefully to the vowel sounds"
    }'::jsonb,
    60,
    10,
    1.5,
    true,
    'system'
);

-- Exercise 6: Simple Question (Multiple Choice)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'listening'),
    'fr',
    6,
    'Questions',
    'A1',
    'Listen: Simple Question',
    'Listen to the question and select what is being asked',
    '{
        "audioUrl": "/audio/fr/questions/quel_age.mp3",
        "question": "What is the speaker asking about?",
        "questionType": "multiple_choice",
        "options": [
            {"id": "a", "text": "Your age"},
            {"id": "b", "text": "Your name"},
            {"id": "c", "text": "Your address"},
            {"id": "d", "text": "Your job"}
        ],
        "correctAnswer": "a",
        "transcript": "Quel âge avez-vous?",
        "explanation": "''Quel âge avez-vous?'' means ''How old are you?'' or ''What is your age?'' in French.",
        "hint": "The question is about a number related to you"
    }'::jsonb,
    90,
    10,
    2.0,
    true,
    'system'
);
