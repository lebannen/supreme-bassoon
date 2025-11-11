-- Sample Fill-in-the-Blank Exercises for French A1
-- Module 4: Regular -ER Verbs (Daily Routine)

-- Exercise 1: parler (je)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'fill_in_blank'),
    'fr',
    4,
    'Regular -ER Verbs',
    'A1',
    'Conjugate: parler (je)',
    'Fill in the blank with the correct conjugation of the verb "parler" (to speak).',
    '{
        "sentence": "Je ____ français.",
        "blankIndex": 1,
        "verb": "parler",
        "subject": "je",
        "tense": "present",
        "correctAnswer": "parle",
        "options": ["parle", "parles", "parlons", "parlent"],
        "translation": "I speak French.",
        "grammarExplanation": "With ''je'', use the first person singular form by removing ''-er'' and adding ''e''."
    }'::jsonb,
    45,
    10,
    1.5,
    true,
    'system'
);

-- Exercise 2: habiter (tu)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'fill_in_blank'),
    'fr',
    4,
    'Regular -ER Verbs',
    'A1',
    'Conjugate: habiter (tu)',
    'Fill in the blank with the correct conjugation of the verb "habiter" (to live).',
    '{
        "sentence": "Tu ____ à Paris.",
        "blankIndex": 1,
        "verb": "habiter",
        "subject": "tu",
        "tense": "present",
        "correctAnswer": "habites",
        "options": ["habite", "habites", "habitons", "habitent"],
        "translation": "You live in Paris.",
        "grammarExplanation": "With ''tu'', add ''es'' to the verb stem (habit + es = habites)."
    }'::jsonb,
    45,
    10,
    1.5,
    true,
    'system'
);

-- Exercise 3: aimer (il/elle)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'fill_in_blank'),
    'fr',
    4,
    'Regular -ER Verbs',
    'A1',
    'Conjugate: aimer (elle)',
    'Fill in the blank with the correct conjugation of the verb "aimer" (to love/like).',
    '{
        "sentence": "Elle ____ le chocolat.",
        "blankIndex": 1,
        "verb": "aimer",
        "subject": "elle",
        "tense": "present",
        "correctAnswer": "aime",
        "options": ["aime", "aimes", "aimons", "aiment"],
        "translation": "She likes chocolate.",
        "grammarExplanation": "With ''il/elle/on'', use the third person singular form ending in ''e''."
    }'::jsonb,
    45,
    10,
    1.5,
    true,
    'system'
);

-- Exercise 4: manger (nous)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'fill_in_blank'),
    'fr',
    4,
    'Regular -ER Verbs',
    'A1',
    'Conjugate: manger (nous)',
    'Fill in the blank with the correct conjugation of the verb "manger" (to eat).',
    '{
        "sentence": "Nous ____ au restaurant.",
        "blankIndex": 1,
        "verb": "manger",
        "subject": "nous",
        "tense": "present",
        "correctAnswer": "mangeons",
        "options": ["mange", "manges", "mangeons", "mangent"],
        "translation": "We eat at the restaurant.",
        "grammarExplanation": "With ''nous'', add ''ons'' to the verb stem. Note: for ''-ger'' verbs, keep the ''e'' before ''ons'' (mangeons).",
        "hint": "Remember that -ger verbs keep the e before -ons."
    }'::jsonb,
    60,
    15,
    2.0,
    true,
    'system'
);

-- Exercise 5: travailler (vous)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'fill_in_blank'),
    'fr',
    4,
    'Regular -ER Verbs',
    'A1',
    'Conjugate: travailler (vous)',
    'Fill in the blank with the correct conjugation of the verb "travailler" (to work).',
    '{
        "sentence": "Vous ____ beaucoup.",
        "blankIndex": 1,
        "verb": "travailler",
        "subject": "vous",
        "tense": "present",
        "correctAnswer": "travaillez",
        "options": ["travaille", "travailles", "travaillez", "travaillent"],
        "translation": "You work a lot.",
        "grammarExplanation": "With ''vous'', add ''ez'' to the verb stem (travaill + ez = travaillez)."
    }'::jsonb,
    45,
    10,
    1.5,
    true,
    'system'
);

-- Exercise 6: étudier (ils/elles)
INSERT INTO exercises (exercise_type_id, language_code, module_number, topic, cefr_level, title, instructions, content, estimated_duration_seconds, points_value, difficulty_rating, is_published, created_by)
VALUES (
    (SELECT id FROM exercise_types WHERE type_key = 'fill_in_blank'),
    'fr',
    4,
    'Regular -ER Verbs',
    'A1',
    'Conjugate: étudier (ils)',
    'Fill in the blank with the correct conjugation of the verb "étudier" (to study).',
    '{
        "sentence": "Ils ____ le français.",
        "blankIndex": 1,
        "verb": "étudier",
        "subject": "ils",
        "tense": "present",
        "correctAnswer": "étudient",
        "options": ["étudie", "étudies", "étudions", "étudient"],
        "translation": "They study French.",
        "grammarExplanation": "With ''ils/elles'', add ''ent'' to the verb stem (étudi + ent = étudient)."
    }'::jsonb,
    45,
    10,
    1.5,
    true,
    'system'
);
