-- Script to completely reset the database for fresh migrations
-- WARNING: This will DELETE ALL DATA!

-- Drop all tables in the correct order (respecting foreign keys)
DROP TABLE IF EXISTS study_session_attempts CASCADE;
DROP TABLE IF EXISTS study_session_items CASCADE;
DROP TABLE IF EXISTS study_sessions CASCADE;
DROP TABLE IF EXISTS user_vocabulary CASCADE;
DROP TABLE IF EXISTS word_set_items CASCADE;
DROP TABLE IF EXISTS user_imported_word_sets CASCADE;
DROP TABLE IF EXISTS word_sets CASCADE;
DROP TABLE IF EXISTS examples CASCADE;
DROP TABLE IF EXISTS pronunciations CASCADE;
DROP TABLE IF EXISTS definitions CASCADE;
DROP TABLE IF EXISTS words CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS languages CASCADE;

-- Drop Flyway schema history table
DROP TABLE IF EXISTS flyway_schema_history CASCADE;

-- Verify all tables are dropped
SELECT tablename
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

-- The above query should return 0 rows if everything is clean
