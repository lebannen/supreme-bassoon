# Debugging Quick Reference

## Useful Database Queries

### Check Word Data Quality
```sql
-- Find word by lemma
SELECT * FROM words WHERE lemma = 'avoir' AND language_code = 'fr';

-- Find word with all related data
SELECT
  w.id, w.lemma, w.part_of_speech, w.is_inflected_form,
  COUNT(DISTINCT d.id) as def_count,
  COUNT(DISTINCT e.id) as example_count,
  COUNT(DISTINCT p.id) as pronunciation_count
FROM words w
LEFT JOIN definitions d ON d.word_id = w.id
LEFT JOIN examples e ON e.definition_id = d.id
LEFT JOIN pronunciations p ON p.word_id = w.id
WHERE w.lemma = 'avoir' AND w.language_code = 'fr'
GROUP BY w.id;

-- Check inflected form linkage
SELECT
  w.lemma as inflected,
  w.grammatical_features,
  base.lemma as base_lemma,
  base.part_of_speech
FROM words w
LEFT JOIN words base ON w.lemma_id = base.id
WHERE w.lemma = 'fait' AND w.language_code = 'fr';

-- Words without definitions (data quality issue)
SELECT w.lemma, w.part_of_speech
FROM words w
WHERE w.language_code = 'fr'
  AND NOT EXISTS (SELECT 1 FROM definitions d WHERE d.word_id = w.id)
LIMIT 20;

-- Check for duplicates
SELECT lemma, part_of_speech, language_code, COUNT(*) as count
FROM words
GROUP BY lemma, part_of_speech, language_code
HAVING COUNT(*) > 1;
```

### Statistics and Counts
```sql
-- Total words by language
SELECT language_code, COUNT(*) as word_count
FROM words
GROUP BY language_code
ORDER BY word_count DESC;

-- Inflected forms ratio
SELECT
  language_code,
  COUNT(*) as total,
  SUM(CASE WHEN is_inflected_form THEN 1 ELSE 0 END) as inflected,
  ROUND(100.0 * SUM(CASE WHEN is_inflected_form THEN 1 ELSE 0 END) / COUNT(*), 1) as inflected_pct
FROM words
GROUP BY language_code
ORDER BY total DESC;

-- Words by part of speech
SELECT part_of_speech, COUNT(*) as count
FROM words
WHERE language_code = 'fr'
GROUP BY part_of_speech
ORDER BY count DESC;

-- Frequency rank distribution
SELECT
  CASE
    WHEN frequency_rank IS NULL THEN 'no rank'
    WHEN frequency_rank <= 1000 THEN 'top 1k'
    WHEN frequency_rank <= 10000 THEN 'top 10k'
    WHEN frequency_rank <= 100000 THEN 'top 100k'
    ELSE '100k+'
  END as rank_bucket,
  COUNT(*) as count
FROM words
WHERE language_code = 'fr'
GROUP BY rank_bucket
ORDER BY count DESC;
```

### Search Query Debugging
```sql
-- Simulate search ranking (database stage)
SELECT
  w.lemma,
  w.part_of_speech,
  w.frequency_rank,
  CASE
    WHEN LOWER(w.lemma) = LOWER('avoir') THEN 0
    WHEN LOWER(w.lemma) LIKE LOWER('avoir%') THEN 1
    WHEN LOWER(w.normalized) = LOWER('avoir') THEN 2
    WHEN LOWER(w.normalized) LIKE LOWER('avoir%') THEN 3
    ELSE 4
  END as rank_priority
FROM words w
WHERE w.language_code = 'fr'
  AND LOWER(w.normalized) LIKE '%avoir%'
ORDER BY rank_priority, w.frequency_rank NULLS LAST
LIMIT 20;

-- Check index usage (performance)
EXPLAIN ANALYZE
SELECT w.* FROM words w
WHERE w.language_code = 'fr'
  AND LOWER(w.normalized) LIKE LOWER('%avoir%')
LIMIT 2000;
```

## API Testing with curl

### Search
```bash
# Basic search
curl "http://localhost:8080/api/v1/search?q=avoir&lang=fr"

# Search with special characters
curl "http://localhost:8080/api/v1/search?q=%C3%AAtre&lang=fr"  # être (URL encoded)

# Short query test
curl "http://localhost:8080/api/v1/search?q=ai&lang=fr"
```

### Get Word Details
```bash
# Get word by lemma
curl "http://localhost:8080/api/v1/words/fr/avoir"

# Get inflected form
curl "http://localhost:8080/api/v1/words/fr/fait"

# Get word with accents
curl "http://localhost:8080/api/v1/words/fr/%C3%AAtre"  # être
```

### Languages
```bash
# List all languages
curl "http://localhost:8080/api/v1/languages"
```

### Import (Admin)
```bash
# Start import
curl -X POST http://localhost:8080/api/v1/import \
  -F "file=@/path/to/french.jsonl.gz" \
  -F "languageCode=fr"

# Get all import progress
curl http://localhost:8080/api/v1/import/progress

# Get specific import status
curl http://localhost:8080/api/v1/import/progress/{importId}

# Clear all words (CAUTION: Deletes all data)
curl -X DELETE http://localhost:8080/api/v1/import/clear
```

## Backend Logs

### View Spring Boot Logs
```bash
# If running in terminal
cd backend
./gradlew bootRun

# Look for:
# - "Started VocabeeApplicationKt" - server started successfully
# - "Flyway" messages - database migrations
# - Import progress: "Processing batch X with Y words"
# - Search queries (if logging enabled)
```

### Enable Debug Logging
Edit `backend/src/main/resources/application.yml`:
```yaml
logging:
  level:
    com.vocabee: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

## Frontend Debugging

### Browser Console
```javascript
// Check if API is reachable
fetch('/api/v1/languages')
  .then(r => r.json())
  .then(console.log)

// Test search
fetch('/api/v1/search?q=avoir&lang=fr')
  .then(r => r.json())
  .then(console.log)

// Test word details
fetch('/api/v1/words/fr/avoir')
  .then(r => r.json())
  .then(console.log)
```

### Vue DevTools
- Install Vue DevTools browser extension
- Inspect component state: `searchResults`, `selectedWord`, `searchQuery`
- Check network requests in browser DevTools Network tab

## Database Management

### Connect to PostgreSQL
```bash
# Using psql
psql -h localhost -p 5432 -U vocabee -d vocabee

# Using Docker (if DB is in container)
docker exec -it vocabee-db psql -U vocabee -d vocabee
```

### Common Database Commands
```sql
-- List tables
\dt

-- Describe table schema
\d words
\d definitions

-- Check database size
SELECT pg_size_pretty(pg_database_size('vocabee'));

-- Check table sizes
SELECT
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check indexes
\di

-- Vacuum and analyze (after large imports)
VACUUM ANALYZE words;
```

### Reset Database (CAUTION)
```bash
# Drop and recreate
cd backend
./gradlew flywayClean
./gradlew flywayMigrate
```

## Python Scripts

### Test Parser
```bash
cd scripts

# Test specific words
python3 test_parser_fixes.py

# Query single entry
python3 query_entry.py avoir

# Extract languages (full process)
python3 extract_languages.py
```

### Check Parsed Data
```bash
cd parsed_data

# Check file sizes
ls -lh *.jsonl.gz

# Peek at first few entries
zcat french.jsonl.gz | head -5 | jq

# Count entries
zcat french.jsonl.gz | wc -l

# Search for specific word
zcat french.jsonl.gz | grep '"word":"avoir"' | jq

# Check for parsing errors (should be empty)
zcat french.jsonl.gz | jq -e . > /dev/null || echo "JSON errors found"
```

## Common Issues and Solutions

### Issue: Port 8080 already in use
```bash
# Find process using port 8080
lsof -ti:8080

# Kill process
lsof -ti:8080 | xargs kill -9
```

### Issue: Database connection refused
```bash
# Check if PostgreSQL is running
docker ps | grep postgres
# OR
pg_isready -h localhost -p 5432

# Start PostgreSQL (if using Docker)
docker start vocabee-db
```

### Issue: Flyway migration errors
```bash
# Check migration status
cd backend
./gradlew flywayInfo

# Repair (if migration failed mid-way)
./gradlew flywayRepair

# Clean and reapply (CAUTION: drops all data)
./gradlew flywayClean flywayMigrate
```

### Issue: Import stuck or slow
```sql
-- Check import progress
SELECT * FROM import_progress ORDER BY started_at DESC;

-- Check if batch is hanging (no progress for 5+ minutes)
-- Kill and restart import if needed
```

### Issue: Search returns no results
```sql
-- Verify data exists
SELECT COUNT(*) FROM words WHERE language_code = 'fr';

-- Check if normalized field is populated
SELECT COUNT(*) FROM words WHERE normalized IS NULL;

-- Test query manually
SELECT * FROM words
WHERE language_code = 'fr'
  AND LOWER(normalized) LIKE '%avoir%'
LIMIT 5;
```

### Issue: Frontend not connecting to backend
```bash
# Check backend is running
curl http://localhost:8080/api/v1/health

# Check CORS configuration
# Should allow frontend origin (localhost:5173 in dev)

# Check browser console for CORS errors
```

## Performance Monitoring

### Slow Query Detection
```sql
-- Enable slow query logging (if needed)
ALTER DATABASE vocabee SET log_min_duration_statement = 1000; -- Log queries >1s

-- Check query performance
EXPLAIN ANALYZE
SELECT * FROM words WHERE language_code = 'fr' AND normalized LIKE '%avoir%';
```

### Index Usage
```sql
-- Check index statistics
SELECT
  schemaname,
  tablename,
  indexname,
  idx_scan as scans,
  idx_tup_read as tuples_read,
  idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

### Application Performance
- Search should complete in <100ms typically
- Import should process ~1000 words/second
- Backend startup should take <5 seconds
