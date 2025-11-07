# Testing Guide

## Test Cases and Sample Queries

### Good Test Queries

#### 1. "avoir" - Multiple POS Sections
**Expected behavior:**
- Should return both noun and verb entries
- Verb should appear first (higher part-of-speech boost)
- Both should be exact matches with high scores

**Database check:**
```sql
SELECT lemma, part_of_speech, is_inflected_form,
       (SELECT COUNT(*) FROM definitions WHERE word_id = words.id) as def_count
FROM words
WHERE lemma = 'avoir' AND language_code = 'fr'
ORDER BY part_of_speech;
```

**Expected:**
- 2 rows: one noun, one verb
- Both should have definitions (def_count > 0)

---

#### 2. "fait" - Inflected Form
**Expected behavior:**
- Should appear in search results
- is_inflected_form = true
- lemma_id should point to "faire"
- grammatical_features should have person, number, tense

**Database check:**
```sql
SELECT w.lemma, w.part_of_speech, w.is_inflected_form, w.lemma_id,
       w.grammatical_features,
       base.lemma as base_lemma
FROM words w
LEFT JOIN words base ON w.lemma_id = base.id
WHERE w.lemma = 'fait' AND w.language_code = 'fr';
```

**Expected:**
- is_inflected_form = true
- lemma_id is not null
- base_lemma = 'faire'
- grammatical_features contains tense, person, number

---

#### 3. "ai" - Very Short Query, Many Matches
**Expected behavior:**
- Exact match "ai" should appear FIRST
- Then prefix matches: "aide", "aimer", "air", etc.
- Then contains matches: "avoir" (contains "ai"), "fait", etc.
- Should return max 500 results (application layer limit)

**Database check:**
```sql
SELECT lemma, part_of_speech,
       CASE
         WHEN LOWER(lemma) = 'ai' THEN 'exact'
         WHEN LOWER(lemma) LIKE 'ai%' THEN 'prefix'
         ELSE 'contains'
       END as match_type
FROM words
WHERE language_code = 'fr'
  AND LOWER(normalized) LIKE '%ai%'
ORDER BY match_type, frequency_rank NULLS LAST
LIMIT 20;
```

---

#### 4. "être" - Accented Characters
**Expected behavior:**
- Should match "être" exactly
- Normalized field handles case: "ÊTRE" → "être"
- Should return verb entry with many definitions

**Database check:**
```sql
SELECT lemma, normalized, part_of_speech
FROM words
WHERE lemma = 'être' AND language_code = 'fr';
```

---

#### 5. Empty/Whitespace Query
**Expected behavior:**
- Frontend should disable search button
- Backend should handle gracefully (no crash)

---

### Edge Cases to Watch

#### 1. Words with >200 Character Dialect Fields
**Issue:** Previously caused "value too long" errors

**Test:**
```sql
SELECT lemma, dialect, LENGTH(dialect) as dialect_length
FROM pronunciations
WHERE LENGTH(dialect) > 50
ORDER BY dialect_length DESC
LIMIT 10;
```

**Expected:** All values fit in VARCHAR(200), no errors

---

#### 2. Words with No Definitions
**Issue:** Some words parsed without definitions (e.g., old "avoir" bug)

**Test:**
```sql
SELECT w.lemma, w.part_of_speech, COUNT(d.id) as def_count
FROM words w
LEFT JOIN definitions d ON d.word_id = w.id
WHERE w.language_code = 'fr'
GROUP BY w.id, w.lemma, w.part_of_speech
HAVING COUNT(d.id) = 0
LIMIT 20;
```

**Expected:** Very few or zero results (indicates good data quality)

---

#### 3. Inflected Forms Without Valid Lemma
**Issue:** If lemma_id points to non-existent word, breaks display

**Test:**
```sql
SELECT w.lemma, w.lemma_id, base.lemma as base_lemma
FROM words w
LEFT JOIN words base ON w.lemma_id = base.id
WHERE w.is_inflected_form = true
  AND w.lemma_id IS NOT NULL
  AND base.id IS NULL
LIMIT 10;
```

**Expected:** Zero results (all inflected forms have valid lemma references)

---

#### 4. Duplicate Word Entries (Same Lemma + POS)
**Issue:** Could indicate parser bug or import bug

**Test:**
```sql
SELECT lemma, part_of_speech, language_code, COUNT(*) as count
FROM words
GROUP BY lemma, part_of_speech, language_code
HAVING COUNT(*) > 1
LIMIT 10;
```

**Expected:** Zero results (no duplicates)

---

#### 5. Special Characters in Search
**Test queries:**
- `l'avoir` (apostrophe)
- `être-là` (hyphen)
- `où` (accents)
- `naïve` (diaeresis)

**Expected:** Should match normalized forms correctly

---

### Search Ranking Validation

#### Test Exact Match Priority
```bash
# Search for "ai"
curl "http://localhost:8080/api/v1/search?q=ai&lang=fr"
```

**Validate:**
1. First result should be exact match "ai" (if exists)
2. Next results should start with "ai" (aide, aimer, air...)
3. Results containing "ai" should appear later (avoir, fait...)

#### Test Frequency Boost
```sql
-- Compare two similar words with different frequency ranks
SELECT lemma, frequency_rank
FROM words
WHERE lemma IN ('avoir', 'avoirs') AND language_code = 'fr';
```

**Expected:** Word with lower frequency_rank (more common) should rank higher in search

#### Test Inflected Form Penalty
**Search:** "faire"

**Expected:**
- "faire" (infinitive, is_inflected_form=false) should appear first
- "fait", "faisons", etc. (conjugations) should appear after
- Even if inflected forms have better frequency rank

---

### Import Testing

#### Test Batch Error Recovery
**Simulate:** Import file with one malformed entry in a batch

**Expected:**
- Batch fails initially
- Word-by-word retry kicks in
- Good words in batch are saved
- Bad word is logged and skipped
- Import continues

#### Test Progress Tracking
```bash
# Start import
curl -X POST http://localhost:8080/api/v1/import \
  -F "file=@french.jsonl.gz" \
  -F "languageCode=fr"

# Check progress
curl http://localhost:8080/api/v1/import/progress
```

**Validate:**
- progressMap contains import
- processedEntries increments
- successfulEntries tracked separately from failedEntries
- status transitions: QUEUED → PROCESSING → COMPLETED

---

### Frontend Testing

#### Search View Tests
1. **Initial state:** No results, search disabled without query
2. **Search execution:** Results appear, loading state shown
3. **Empty results:** "No words found" message displays
4. **Row selection:** Dialog opens with word details
5. **Pagination:** 20 results per page

#### Word Detail Dialog Tests
1. **Loading state:** Spinner shows while fetching
2. **Error state:** Error message if fetch fails
3. **Complete data:** All fields render correctly
   - Definitions with numbers
   - Examples indented under definitions
   - Etymology and usage notes (if present)
   - Inflected forms as tags (for lemmas)
4. **Missing data:** Gracefully handles missing fields

---

## Debugging Commands

### Check Word Data Quality
```sql
-- Words without definitions
SELECT COUNT(*) as words_without_defs
FROM words w
WHERE NOT EXISTS (SELECT 1 FROM definitions d WHERE d.word_id = w.id);

-- Orphaned definitions (word deleted)
SELECT COUNT(*) as orphaned_defs
FROM definitions d
WHERE NOT EXISTS (SELECT 1 FROM words w WHERE w.id = d.word_id);

-- Inflected forms summary
SELECT is_inflected_form, COUNT(*) as count
FROM words
WHERE language_code = 'fr'
GROUP BY is_inflected_form;
```

### Check Import Status
```sql
-- Word count by language
SELECT language_code, COUNT(*) as word_count
FROM words
GROUP BY language_code
ORDER BY word_count DESC;

-- Entry count vs actual count
SELECT l.code, l.name, l.entry_count, COUNT(w.id) as actual_count
FROM languages l
LEFT JOIN words w ON w.language_code = l.code
GROUP BY l.code, l.name, l.entry_count;
```

### Check Search Performance
```sql
-- Explain search query
EXPLAIN ANALYZE
SELECT w.* FROM words w
WHERE w.language_code = 'fr'
  AND LOWER(w.normalized) LIKE LOWER('%avoir%')
ORDER BY
  CASE
    WHEN LOWER(w.lemma) = LOWER('avoir') THEN 0
    WHEN LOWER(w.lemma) LIKE LOWER('avoir%') THEN 1
    WHEN LOWER(w.normalized) = LOWER('avoir') THEN 2
    WHEN LOWER(w.normalized) LIKE LOWER('avoir%') THEN 3
    ELSE 4
  END,
  w.frequency_rank NULLS LAST,
  w.lemma
LIMIT 2000;
```

**Look for:**
- Index usage on normalized column
- Execution time < 50ms

---

## Regression Tests

### After Parser Changes
1. Re-run test_parser_fixes.py with test words
2. Check word count: should be similar to previous run
3. Check avoir: should have 2 entries (noun + verb)
4. Check fait: should have proper inflected_form_of structure

### After Ranking Changes
1. Search for "ai", "avoir", "fait"
2. Verify exact matches appear first
3. Check top 20 results make sense (frequency + match quality)

### After Database Schema Changes
1. Run migrations on test database
2. Test import with small dataset
3. Test search and word detail endpoints
4. Check foreign key constraints work

---

## Known Test Data

### French Test Cases
- **avoir** - verb and noun, high frequency
- **être** - verb, very high frequency, accented
- **fait** - inflected form of "faire"
- **ai** - very short, high frequency, many partial matches
- **bonjour** - simple noun, common word
- **anticonstitutionnellement** - very long word
- **l'** - article with apostrophe

### Expected Counts (approximate)
- Total French entries: ~400,000-500,000
- Inflected forms: ~30-40% of total
- Words with definitions: >95%
- Unique lemmas (base forms): ~60-70% of total
