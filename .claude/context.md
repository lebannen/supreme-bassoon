# Project Context

## Overview
Vocabee is a vocabulary learning application that leverages Wiktionary data to provide comprehensive language learning tools. The system processes millions of dictionary entries and provides intelligent search and learning features.

## Architecture Overview
- **Backend**: Kotlin Spring Boot 3, PostgreSQL 15
- **Frontend**: Vue 3 (Composition API) + TypeScript + PrimeVue
- **Data Pipeline**: Python scripts for Wiktionary XML parsing
- **Data Source**: English Wiktionary (enwiktionary-latest-pages-articles.xml)
- **Data Format**: Compressed JSONL files (one per language)

## Key Design Decisions

### 1. Two-Stage Search Ranking
**Decision**: Split search ranking into database layer (simple, fast) and application layer (sophisticated)

**Rationale**:
- Database filters millions of entries → ~2000 candidates with basic ranking (LIMIT 2000)
- Application layer applies complex scoring → top 500 results
- Allows sophisticated language-specific logic without complex SQL
- Easy to test and iterate on ranking algorithms
- Performance: Both stages are fast (<100ms combined)

**Location**: `WordRepository.kt` (Stage 1), `SearchRankingService.kt` (Stage 2)

### 2. Separate Entries Per Part-of-Speech
**Decision**: Words with multiple parts of speech (e.g., "avoir" as noun and verb) create separate Word records

**Rationale**:
- Clean data model (one POS per Word entity)
- Simplified querying and display
- Avoids mixing definitions from different POS
- Fixed parser bug where definitions were merged incorrectly

**Implementation**: `wikitext_parser.py` splits by POS headers (`===Noun===`, `===Verb===`, etc.)

### 3. Inflected Forms Link to Lemmas
**Decision**: Inflected forms (conjugations, declensions) are separate Word records that reference their base lemma via `lemma_id`

**Schema**:
```sql
words:
  - id (primary key)
  - lemma (text) -- e.g., "fait" or "faire"
  - is_inflected_form (boolean)
  - lemma_id (foreign key to words.id) -- null for base forms
  - grammatical_features (jsonb) -- {"person": "3", "number": "singular", "tense": "present"}
```

**Rationale**:
- Supports efficient lookups (find all forms of a word)
- Flexible grammatical features via JSONB
- Handles cross-language variations (different features per language)

### 4. Batch Processing with Error Recovery
**Decision**: Import processes words in batches of 50, with word-by-word retry on batch failures

**Rationale**:
- Batch insert: 50x faster than individual inserts
- Error recovery: One bad word doesn't kill entire batch
- Logging: Track exactly which words failed and why
- Resilience: Continue import even with data quality issues

**Location**: `ImportService.kt:144-166`

### 5. Normalized Search Field
**Decision**: Every word has a `normalized` field (lowercase version of lemma) with index

**Rationale**:
- Case-insensitive search without LOWER() on every query
- Database can use index efficiently
- Supports future enhancements (accent removal, stemming)

### 6. JSONB for Grammatical Features
**Decision**: Use PostgreSQL JSONB column instead of fixed schema for grammatical features

**Rationale**:
- Languages have different grammatical categories
- Flexible: add new features without migrations
- Queryable: JSONB supports indexing and querying
- Type-safe on backend via Kotlin Map<String, Any>

## Common Patterns

### Repository Pattern
```kotlin
interface WordRepository : JpaRepository<Word, Long> {
    @Query("SELECT ... FROM Word w WHERE ...")
    fun customQuery(...): List<Word>
}
```
- Always use `@Query` with named parameters (`:paramName`)
- Use `@Param` annotations for clarity
- Entity fetching: Consider N+1 issues, use JOIN FETCH sparingly

### Service Layer
```kotlin
@Service
@Transactional(readOnly = true)
class MyService(
    private val repository: MyRepository
) {
    @Transactional // Override for write operations
    fun writeMethod() { ... }
}
```
- Default to `readOnly = true` for performance
- Override with `@Transactional` for writes
- Constructor injection (Kotlin primary constructor)

### DTO Pattern
```kotlin
data class WordDto(
    val id: Long,
    val lemma: String,
    ...
)
```
- DTOs in `web.dto` package
- Convert entities to DTOs in service layer, never expose entities directly
- Use data classes for automatic equals/hashCode/toString

### Error Handling in Imports
```kotlin
try {
    processBatch(batch)
} catch (e: Exception) {
    logger.warn("Batch failed, retrying word-by-word")
    batch.forEach { item ->
        try {
            processSingle(item)
        } catch (itemError: Exception) {
            logger.warn("Failed to process item: ${itemError.message}")
            failedCount++
        }
    }
}
```

## Technology Choices

### Why Kotlin?
- Null safety reduces runtime errors
- Data classes reduce boilerplate
- Extension functions for cleaner code
- Excellent Spring Boot integration

### Why Vue 3 Composition API?
- Better TypeScript support than Options API
- Easier to organize logic by feature
- Smaller bundle size
- Better performance

### Why PrimeVue?
- Comprehensive component library
- Good TypeScript support
- Customizable theming
- Active maintenance

### Why PostgreSQL?
- JSONB support for flexible schema
- Full-text search capabilities (future: pg_trgm)
- Excellent performance with proper indexing
- Mature ecosystem

## Data Statistics
- Total pages processed: ~7.9M Wiktionary pages
- Total entries extracted: ~4.2M dictionary entries
- Languages supported: 12 (Spanish, Italian, Russian, Portuguese, French, German, Swedish, Chinese, Finnish, Japanese, Polish, Dutch)
- Largest dataset: Spanish (~800K entries)
- Processing speed: ~16K pages/sec

## Performance Considerations
- Search queries: <100ms for 2-stage ranking
- Import speed: ~1000 entries/sec (with batch processing)
- Database: Indexes on language_code, normalized, lemma_id
- Frontend: Lazy loading, pagination on client side

## Known Limitations
- Search doesn't handle typos (future enhancement: pg_trgm)
- Frequency ranks only from Wiktionary (future: external corpora)
- No caching layer yet (future: Redis for search results)
- Language-specific ranking rules are placeholders
- No pronunciation audio playback

## Future Enhancements Roadmap
1. Language-specific ranking (French elision, German compounds, etc.)
2. Full-text search with PostgreSQL trigrams
3. Typo tolerance and "did you mean?" suggestions
4. User accounts and personalized learning paths
5. Spaced repetition system
6. Audio pronunciation support
7. Integration with frequency lists from external sources
8. Mobile app (React Native or Flutter)
