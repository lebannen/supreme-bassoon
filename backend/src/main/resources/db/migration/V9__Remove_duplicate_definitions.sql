-- Remove duplicate definitions (keep the first occurrence for each word_id + definition_number)
DELETE FROM definitions
WHERE id NOT IN (
    SELECT MIN(id)
    FROM definitions
    GROUP BY word_id, definition_number
);

-- Add unique constraint to prevent future duplicates
ALTER TABLE definitions
ADD CONSTRAINT uk_definitions_word_number UNIQUE (word_id, definition_number);
