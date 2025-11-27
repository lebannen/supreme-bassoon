-- Add auto_mode column to enable automatic stage progression
ALTER TABLE course_generations
    ADD COLUMN auto_mode BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN course_generations.auto_mode IS 'When true, automatically proceed through all stages without manual approval';
