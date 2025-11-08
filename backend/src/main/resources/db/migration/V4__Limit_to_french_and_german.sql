-- Remove all languages except French and German
DELETE FROM languages WHERE code NOT IN ('fr', 'de');
