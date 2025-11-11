-- Insert Matching exercises for French A1

-- Exercise 1: Basic Greetings
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'matching'),
    'fr',
    1,
    'Greetings',
    'A1',
    'Match French Greetings',
    'Match each French greeting with its English translation',
    '{
        "pairs": [
            {"left": "Bonjour", "right": "Hello/Good day"},
            {"left": "Bonsoir", "right": "Good evening"},
            {"left": "Bonne nuit", "right": "Good night"},
            {"left": "Salut", "right": "Hi/Bye"},
            {"left": "Au revoir", "right": "Goodbye"}
        ],
        "hint": "Think about the time of day for each greeting"
    }'::jsonb,
    120,
    10,
    1.0,
    true,
    'system'
);

-- Exercise 2: Family Members
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'matching'),
    'fr',
    2,
    'Family',
    'A1',
    'Match Family Members',
    'Match each French family term with its English translation',
    '{
        "pairs": [
            {"left": "le père", "right": "the father"},
            {"left": "la mère", "right": "the mother"},
            {"left": "le frère", "right": "the brother"},
            {"left": "la sœur", "right": "the sister"},
            {"left": "les parents", "right": "the parents"},
            {"left": "les enfants", "right": "the children"}
        ],
        "hint": "Notice the gender: le for masculine, la for feminine, les for plural"
    }'::jsonb,
    180,
    10,
    1.0,
    true,
    'system'
);

-- Exercise 3: Colors
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'matching'),
    'fr',
    3,
    'Colors',
    'A1',
    'Match Colors',
    'Match each French color with its English translation',
    '{
        "pairs": [
            {"left": "rouge", "right": "red"},
            {"left": "bleu", "right": "blue"},
            {"left": "vert", "right": "green"},
            {"left": "jaune", "right": "yellow"},
            {"left": "noir", "right": "black"},
            {"left": "blanc", "right": "white"}
        ],
        "hint": "Colors in French do not have articles"
    }'::jsonb,
    120,
    10,
    1.0,
    true,
    'system'
);

-- Exercise 4: Food Items
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'matching'),
    'fr',
    4,
    'Food',
    'A1',
    'Match Food Items',
    'Match each French food word with its English translation',
    '{
        "pairs": [
            {"left": "le pain", "right": "the bread"},
            {"left": "le fromage", "right": "the cheese"},
            {"left": "la pomme", "right": "the apple"},
            {"left": "le café", "right": "the coffee"},
            {"left": "le vin", "right": "the wine"}
        ],
        "hint": "Notice the articles: le (masculine), la (feminine)"
    }'::jsonb,
    180,
    10,
    1.0,
    true,
    'system'
);

-- Exercise 5: Days of the Week
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'matching'),
    'fr',
    5,
    'Time',
    'A1',
    'Match Days of the Week',
    'Match each French day with its English translation',
    '{
        "pairs": [
            {"left": "lundi", "right": "Monday"},
            {"left": "mardi", "right": "Tuesday"},
            {"left": "mercredi", "right": "Wednesday"},
            {"left": "jeudi", "right": "Thursday"},
            {"left": "vendredi", "right": "Friday"},
            {"left": "samedi", "right": "Saturday"},
            {"left": "dimanche", "right": "Sunday"}
        ],
        "hint": "In French, days of the week are not capitalized"
    }'::jsonb,
    180,
    10,
    1.0,
    true,
    'system'
);

-- Exercise 6: Numbers 1-10
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'matching'),
    'fr',
    5,
    'Numbers',
    'A1',
    'Match Numbers 1-10',
    'Match each French number with its numeric value',
    '{
        "pairs": [
            {"left": "un", "right": "1"},
            {"left": "deux", "right": "2"},
            {"left": "trois", "right": "3"},
            {"left": "quatre", "right": "4"},
            {"left": "cinq", "right": "5"},
            {"left": "six", "right": "6"}
        ],
        "hint": "Practice counting from 1 to 6 in French"
    }'::jsonb,
    120,
    10,
    1.0,
    true,
    'system'
);
