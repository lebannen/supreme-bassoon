-- Major Refactoring: Moving to Course/Module/Episode structure
-- This migration clears all exercise-related data for a fresh start

-- Delete all user progress data
DELETE FROM user_exercise_attempts;

-- Delete all exercises
DELETE FROM exercises;

-- Delete exercise collections (will be replaced by episodes)
DELETE FROM exercise_collection_items;
DELETE FROM exercise_collections;

-- Note: We keep exercise_types table as it defines the base exercise types
-- Note: We keep users, languages, and vocabulary data intact
