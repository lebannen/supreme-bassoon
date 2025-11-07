package com.vocabee.service

import com.vocabee.domain.model.Word
import org.springframework.stereotype.Service
import kotlin.math.abs

/**
 * Service for sophisticated search result ranking with language-specific optimizations.
 *
 * Applies application-layer ranking to pre-filtered search results from the database.
 * This allows for complex, language-specific ranking logic that would be difficult
 * or inefficient to implement in SQL.
 */
@Service
class SearchRankingService {

    companion object {
        const val FINAL_LIMIT = 500
    }

    /**
     * Rank search results and return top N results.
     *
     * @param words Pre-filtered candidates from database (typically ~2000)
     * @param query User's search query
     * @param languageCode Language being searched
     * @return Top-ranked results (up to FINAL_LIMIT)
     */
    fun rankAndLimit(
        words: List<Word>,
        query: String,
        languageCode: String
    ): List<Word> {
        return words
            .map { word -> word to calculateScore(word, query, languageCode) }
            .sortedByDescending { it.second }
            .take(FINAL_LIMIT)
            .map { it.first }
    }

    /**
     * Calculate relevance score for a word given the search query.
     *
     * Higher scores indicate better matches. Score components:
     * - Exact match bonus (10000+)
     * - Prefix match bonus (500-1000)
     * - Contains bonus (100)
     * - Length penalty for short queries (prefer concise results)
     * - Inflected form penalty (prefer base forms/lemmas)
     * - Frequency boost (0-1000 based on frequency_rank)
     * - Language-specific adjustments
     */
    private fun calculateScore(
        word: Word,
        query: String,
        languageCode: String
    ): Double {
        var score = 0.0

        val lemmaLower = word.lemma.lowercase()
        val normalizedLower = word.normalized.lowercase()
        val queryLower = query.lowercase()

        // Exact match bonus
        when {
            lemmaLower == queryLower -> score += 10000
            normalizedLower == queryLower -> score += 5000
            lemmaLower.startsWith(queryLower) -> score += 1000
            normalizedLower.startsWith(queryLower) -> score += 500
            else -> score += 100
        }

        // Length penalty (prefer shorter words for short queries)
        // For queries like "ai", prefer "ai" over "aimer" or "aimerait"
        if (queryLower.length <= 3) {
            val lengthDiff = lemmaLower.length - queryLower.length
            score -= lengthDiff * 10
        }

        // Inflected form penalty (prefer base forms)
        // e.g., prefer "avoir" (infinitive) over "avons" (conjugated)
        if (word.isInflectedForm) {
            score -= 200
        }

        // Frequency boost (more common words ranked higher)
        // frequency_rank: 1 (most common) to 1,000,000+ (rare)
        word.frequencyRank?.let { rank ->
            score += (1_000_000 - rank) / 1000.0
        }

        // Part of speech preference (nouns and verbs are generally more useful)
        score += getPartOfSpeechBoost(word.partOfSpeech)

        // Language-specific adjustments
        score += getLanguageSpecificBoost(word, query, languageCode)

        return score
    }

    /**
     * Boost certain parts of speech that are typically more useful in search results.
     */
    private fun getPartOfSpeechBoost(partOfSpeech: String?): Double {
        return when (partOfSpeech?.lowercase()) {
            "noun" -> 50.0
            "verb" -> 40.0
            "adjective" -> 30.0
            "adverb" -> 20.0
            else -> 0.0
        }
    }

    /**
     * Apply language-specific ranking adjustments.
     *
     * This is a hook for future language-specific logic. Examples:
     * - French: Handle elision (l'avoir), prefer infinitives for verb searches
     * - German: Compound word decomposition, capitalization handling
     * - Japanese: Kanji vs hiragana vs romaji matching preferences
     * - Russian: Case handling, stress patterns
     * - Chinese: Traditional vs simplified character preferences
     */
    private fun getLanguageSpecificBoost(
        word: Word,
        query: String,
        languageCode: String
    ): Double {
        return when (languageCode) {
            "fr" -> getFrenchBoost(word, query)
            "de" -> getGermanBoost(word, query)
            "es" -> getSpanishBoost(word, query)
            else -> 0.0
        }
    }

    /**
     * French-specific ranking adjustments.
     */
    private fun getFrenchBoost(word: Word, query: String): Double {
        var boost = 0.0

        // TODO: Future enhancements
        // - Handle elision: searching "avoir" should match "l'avoir" well
        // - Prefer infinitives for verb queries
        // - Handle accents: "être" vs "etre"

        return boost
    }

    /**
     * German-specific ranking adjustments.
     */
    private fun getGermanBoost(word: Word, query: String): Double {
        var boost = 0.0

        // TODO: Future enhancements
        // - Compound word decomposition
        // - Capitalization rules (nouns always capitalized)
        // - Umlaut handling: "über" vs "uber"

        return boost
    }

    /**
     * Spanish-specific ranking adjustments.
     */
    private fun getSpanishBoost(word: Word, query: String): Double {
        var boost = 0.0

        // TODO: Future enhancements
        // - Accent handling: "qué" vs "que"
        // - Prefer infinitives for verb queries
        // - Handle reflexive verbs

        return boost
    }
}
