-- Add enrichment fields to modules for better course planning
ALTER TABLE modules
    ADD COLUMN vocabulary_focus JSONB;
ALTER TABLE modules
    ADD COLUMN grammar_focus JSONB;
ALTER TABLE modules
    ADD COLUMN episode_outline JSONB;

-- Add comments for clarity
COMMENT ON COLUMN modules.vocabulary_focus IS 'JSON array of key vocabulary themes/words to be covered in this module';
COMMENT ON COLUMN modules.grammar_focus IS 'JSON array of key grammar points to be covered in this module';
COMMENT ON COLUMN modules.episode_outline IS 'JSON array of episode summaries generated during course planning';
