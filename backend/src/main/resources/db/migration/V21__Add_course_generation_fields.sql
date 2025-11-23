-- Add series_context and status to courses
ALTER TABLE courses ADD COLUMN series_context TEXT;
ALTER TABLE courses ADD COLUMN status VARCHAR(20) DEFAULT 'DRAFT';

-- Add status to modules
ALTER TABLE modules ADD COLUMN status VARCHAR(20) DEFAULT 'PLANNED';

-- Add summary, data, and status to episodes
ALTER TABLE episodes ADD COLUMN summary TEXT;
ALTER TABLE episodes ADD COLUMN data JSONB;
ALTER TABLE episodes ADD COLUMN status VARCHAR(20) DEFAULT 'PLANNED';
