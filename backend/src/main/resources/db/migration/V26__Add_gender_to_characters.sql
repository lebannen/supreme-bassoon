-- Add gender column to generation_characters table
-- This helps with consistent character image generation

ALTER TABLE generation_characters
    ADD COLUMN gender VARCHAR(20);

COMMENT ON COLUMN generation_characters.gender IS 'Character gender: MALE or FEMALE';
