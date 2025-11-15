-- Insert Cloze Reading exercises for French A1

-- Exercise 1: Daily Routine
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'cloze_reading'),
    'fr',
    1,
    'Daily Routine',
    'A1',
    'Complete: My Morning',
    'Fill in the blanks to complete the text about a morning routine',
    '{
        "text": "Chaque matin, je ___1___ à 7 heures. Je ___2___ mon petit déjeuner: du pain et du café. Puis, je ___3___ à l''école en bus. J''___4___ à 8 heures.",
        "blanks": [
            {"id": "1", "correctAnswer": ["me réveille", "me lève"]},
            {"id": "2", "correctAnswer": ["prends", "mange"]},
            {"id": "3", "correctAnswer": "vais"},
            {"id": "4", "correctAnswer": "arrive"}
        ],
        "hint": "Think about common morning routine verbs"
    }'::jsonb,
    180,
    15,
    1.5,
    true,
    'system'
);

-- Exercise 2: Family Introduction
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'cloze_reading'),
    'fr',
    2,
    'Family',
    'A1',
    'Complete: My Family',
    'Fill in the blanks to complete the text about family',
    '{
        "text": "Bonjour! Je m''___1___ Marie. J''ai 25 ans. Mon ___2___ s''appelle Pierre et ma ___3___ s''appelle Sophie. Nous ___4___ à Paris.",
        "blanks": [
            {"id": "1", "correctAnswer": "appelle"},
            {"id": "2", "correctAnswer": ["père", "papa"]},
            {"id": "3", "correctAnswer": ["mère", "maman"]},
            {"id": "4", "correctAnswer": "habitons"}
        ],
        "hint": "Remember the verb ''s''appeler'' and family member words"
    }'::jsonb,
    180,
    15,
    1.5,
    true,
    'system'
);

-- Exercise 3: At the Market
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'cloze_reading'),
    'fr',
    3,
    'Shopping',
    'A1',
    'Complete: At the Market',
    'Fill in the blanks to complete the text about shopping at the market',
    '{
        "text": "Aujourd''hui, je ___1___ au marché. J''achète des ___2___ rouges et des ___3___ jaunes. Le vendeur ___4___ très gentil. Ça ___5___ 10 euros.",
        "blanks": [
            {"id": "1", "correctAnswer": "vais"},
            {"id": "2", "correctAnswer": ["pommes", "tomates", "fraises"]},
            {"id": "3", "correctAnswer": ["bananes", "citrons"]},
            {"id": "4", "correctAnswer": "est"},
            {"id": "5", "correctAnswer": ["coûte", "fait"]}
        ],
        "hint": "Think about shopping vocabulary and colors"
    }'::jsonb,
    240,
    15,
    2.0,
    true,
    'system'
);

-- Exercise 4: Weekend Plans
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'cloze_reading'),
    'fr',
    4,
    'Leisure',
    'A1',
    'Complete: Weekend Plans',
    'Fill in the blanks to complete the text about weekend activities',
    '{
        "text": "Ce weekend, je ___1___ aller au cinéma avec mes amis. Nous ___2___ voir un film français. Après, nous ___3___ manger au restaurant. Je ___4___ très content!",
        "blanks": [
            {"id": "1", "correctAnswer": ["vais", "voudrais", "veux"]},
            {"id": "2", "correctAnswer": ["allons", "voulons"]},
            {"id": "3", "correctAnswer": ["allons", "voulons"]},
            {"id": "4", "correctAnswer": "suis"}
        ],
        "hint": "Use near future (aller + infinitive) and ''être''"
    }'::jsonb,
    240,
    15,
    2.0,
    true,
    'system'
);

-- Exercise 5: Weather Description
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'cloze_reading'),
    'fr',
    5,
    'Weather',
    'A1',
    'Complete: The Weather',
    'Fill in the blanks to complete the text about weather',
    '{
        "text": "En été, il ___1___ beau et il ___2___ chaud. J''aime aller à la plage. En hiver, il ___3___ froid et il ___4___ souvent. Je préfère rester à la maison.",
        "blanks": [
            {"id": "1", "correctAnswer": ["fait", "est"]},
            {"id": "2", "correctAnswer": "fait"},
            {"id": "3", "correctAnswer": "fait"},
            {"id": "4", "correctAnswer": ["neige", "pleut"]}
        ],
        "hint": "Use ''il fait'' for weather descriptions"
    }'::jsonb,
    180,
    15,
    1.5,
    true,
    'system'
);

-- Exercise 6: At the Restaurant
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'cloze_reading'),
    'fr',
    6,
    'Food',
    'A1',
    'Complete: At the Restaurant',
    'Fill in the blanks to complete the dialogue at a restaurant',
    '{
        "text": "Bonjour! Je ___1___ réserver une table pour deux personnes. Nous ___2___ arriver à 20 heures. Je ___3___ manger du poisson. Mon ami ___4___ végétarien, il ne ___5___ pas de viande.",
        "blanks": [
            {"id": "1", "correctAnswer": ["voudrais", "veux"]},
            {"id": "2", "correctAnswer": "allons"},
            {"id": "3", "correctAnswer": ["vais", "voudrais", "veux"]},
            {"id": "4", "correctAnswer": "est"},
            {"id": "5", "correctAnswer": "mange"}
        ],
        "hint": "Use polite forms and present tense"
    }'::jsonb,
    240,
    15,
    2.0,
    true,
    'system'
);
