-- Fix enum types for course generation tables
-- PostgreSQL native enums don't work well with Hibernate's @Enumerated(EnumType.STRING)
-- Change to VARCHAR columns instead

-- First, drop the view that depends on these columns
DROP VIEW IF EXISTS generation_progress;

-- Drop default values that reference enum types
ALTER TABLE course_generations
    ALTER COLUMN current_stage DROP DEFAULT;
ALTER TABLE generation_blueprints
    ALTER COLUMN status DROP DEFAULT;
ALTER TABLE generation_module_plans
    ALTER COLUMN status DROP DEFAULT;
ALTER TABLE generation_episode_plans
    ALTER COLUMN status DROP DEFAULT;
ALTER TABLE generation_episode_content
    ALTER COLUMN status DROP DEFAULT;
ALTER TABLE generation_exercises
    ALTER COLUMN status DROP DEFAULT;
ALTER TABLE generation_media
    ALTER COLUMN status DROP DEFAULT;

-- Fix current_stage column in course_generations
ALTER TABLE course_generations
    ALTER COLUMN current_stage TYPE VARCHAR(50) USING current_stage::text;

-- Fix status columns in generation tables
ALTER TABLE generation_blueprints
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

ALTER TABLE generation_module_plans
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

ALTER TABLE generation_episode_plans
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

ALTER TABLE generation_episode_content
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

ALTER TABLE generation_exercises
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

ALTER TABLE generation_media
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

-- Set new defaults as VARCHAR values
ALTER TABLE course_generations
    ALTER COLUMN current_stage SET DEFAULT 'BLUEPRINT';
ALTER TABLE generation_blueprints
    ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE generation_module_plans
    ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE generation_episode_plans
    ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE generation_episode_content
    ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE generation_exercises
    ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE generation_media
    ALTER COLUMN status SET DEFAULT 'PENDING';

-- Drop the unused enum types
DROP TYPE IF EXISTS generation_stage;
DROP TYPE IF EXISTS generation_step_status;

-- Recreate the view with VARCHAR columns
CREATE VIEW generation_progress AS
SELECT g.id,
       g.language_code,
       g.cefr_level,
       g.module_count,
       g.episodes_per_module,
       g.current_stage,
       g.created_at,
       g.completed_at,

       -- Blueprint status
       b.status                                                                                                    as blueprint_status,

       -- Module planning progress
       (SELECT COUNT(*)
        FROM generation_module_plans mp
        WHERE mp.generation_id = g.id
          AND mp.status = 'COMPLETED')                                                                             as modules_planned,

       -- Episode content progress
       (SELECT COUNT(*)
        FROM generation_episode_content ec
                 JOIN generation_episode_plans ep ON ec.episode_plan_id = ep.id
                 JOIN generation_module_plans mp ON ep.module_plan_id = mp.id
        WHERE mp.generation_id = g.id
          AND ec.status = 'COMPLETED')                                                                             as episodes_generated,

       -- Total expected episodes
       g.module_count * g.episodes_per_module                                                                      as total_episodes,

       -- Character count
       (SELECT COUNT(*)
        FROM generation_characters c
        WHERE c.generation_id = g.id)                                                                              as character_count,

       -- Exercise progress
       (SELECT COUNT(*)
        FROM generation_exercises ex
                 JOIN generation_episode_plans ep ON ex.episode_plan_id = ep.id
                 JOIN generation_module_plans mp ON ep.module_plan_id = mp.id
        WHERE mp.generation_id = g.id
          AND ex.status = 'COMPLETED')                                                                             as episodes_with_exercises,

       -- Media progress
       (SELECT COUNT(*)
        FROM generation_media m
        WHERE m.generation_id = g.id
          AND m.status = 'COMPLETED')                                                                              as media_generated

FROM course_generations g
         LEFT JOIN generation_blueprints b ON b.generation_id = g.id;

COMMENT ON VIEW generation_progress IS 'Overview of generation progress across all stages';
