package com.vocabee.service.generation

import com.vocabee.domain.model.Word
import com.vocabee.domain.repository.WordRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for matching vocabulary phrases to dictionary Word entities.
 * Handles article stripping, phrase parsing, and multi-word combinations.
 */
@Service
class VocabularyMatchingService(
    private val wordRepository: WordRepository
) {
    private val logger = LoggerFactory.getLogger(VocabularyMatchingService::class.java)

    // Language-specific article patterns (sorted by length for proper stripping)
    private val ARTICLE_PATTERNS = mapOf(
        "fr" to listOf("de la", "de l'", "du", "des", "le", "la", "l'", "les", "un", "une"),
        "de" to listOf("einen", "einem", "einer", "eine", "ein", "der", "die", "das"),
        "es" to listOf("unos", "unas", "los", "las", "un", "una", "el", "la"),
        "it" to listOf("gli", "uno", "una", "il", "lo", "la", "i", "le", "un"),
        "pt" to listOf("umas", "uns", "uma", "um", "os", "as", "o", "a")
    )

    data class MatchedVocabulary(
        val originalPhrase: String,
        val matchedWords: List<Word>,
        val unmatchedParts: List<String>
    )

    data class MatchingResult(
        val matches: List<MatchedVocabulary>,
        val totalPhrases: Int,
        val matchedPhrases: Int,
        val unmatchedPhrases: Int,
        val totalWords: Int
    )

    /**
     * Match a list of vocabulary phrases to dictionary words.
     */
    fun matchVocabularyToWords(
        vocabularyList: List<String>,
        languageCode: String
    ): MatchingResult {
        val matches = vocabularyList.map { phrase ->
            matchPhrase(phrase.trim(), languageCode)
        }

        val matchedPhrases = matches.count { it.matchedWords.isNotEmpty() }
        val totalWords = matches.flatMap { it.matchedWords }.distinctBy { it.id }.size

        return MatchingResult(
            matches = matches,
            totalPhrases = vocabularyList.size,
            matchedPhrases = matchedPhrases,
            unmatchedPhrases = vocabularyList.size - matchedPhrases,
            totalWords = totalWords
        )
    }

    /**
     * Match a single phrase to dictionary words.
     * Tries exact match, article stripping, then multi-word combinations.
     */
    private fun matchPhrase(phrase: String, languageCode: String): MatchedVocabulary {
        val normalized = phrase.lowercase().trim()

        if (normalized.isEmpty()) {
            return MatchedVocabulary(phrase, emptyList(), emptyList())
        }

        // Step 1: Try exact match first
        val exactMatches = wordRepository.findByLanguageCodeAndNormalized(languageCode, normalized)
        if (exactMatches.isNotEmpty()) {
            val bestMatch = selectBestWord(exactMatches)
            logger.debug("Exact match for '{}': {}", phrase, bestMatch.lemma)
            return MatchedVocabulary(phrase, listOf(bestMatch), emptyList())
        }

        // Step 2: Strip articles and try again
        val stripped = stripArticles(normalized, languageCode)
        if (stripped != normalized && stripped.isNotEmpty()) {
            val strippedMatches = wordRepository.findByLanguageCodeAndNormalized(languageCode, stripped)
            if (strippedMatches.isNotEmpty()) {
                val bestMatch = selectBestWord(strippedMatches)
                logger.debug("Match after stripping articles '{}' -> '{}': {}", phrase, stripped, bestMatch.lemma)
                return MatchedVocabulary(phrase, listOf(bestMatch), emptyList())
            }
        }

        // Step 3: Split into words and try combinations
        val words = normalized.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size <= 1) {
            // Single word that didn't match - might be conjugated or unknown
            val searchResults = wordRepository.searchByLemma(languageCode, stripped.ifEmpty { normalized })
            if (searchResults.isNotEmpty()) {
                // Take the best match from search results
                val bestMatch = selectBestWord(searchResults.take(5))
                logger.debug("Search match for '{}': {}", phrase, bestMatch.lemma)
                return MatchedVocabulary(phrase, listOf(bestMatch), emptyList())
            }
            return MatchedVocabulary(phrase, emptyList(), listOf(phrase))
        }

        // Multi-word phrase: try to match each word or combination
        return matchMultiWordPhrase(phrase, words, languageCode)
    }

    /**
     * Match a multi-word phrase by trying combinations of words.
     */
    private fun matchMultiWordPhrase(
        originalPhrase: String,
        words: List<String>,
        languageCode: String
    ): MatchedVocabulary {
        val matchedWords = mutableListOf<Word>()
        val unmatched = mutableListOf<String>()
        val articles = getArticles(languageCode)

        var i = 0
        while (i < words.size) {
            val word = words[i]

            // Skip articles
            if (word in articles) {
                i++
                continue
            }

            // Try multi-word combinations first (up to 3 words)
            var found = false
            for (len in minOf(3, words.size - i) downTo 1) {
                val candidateWords = words.subList(i, i + len)
                    .filter { it !in articles }

                if (candidateWords.isEmpty()) {
                    i++
                    found = true
                    break
                }

                val candidate = candidateWords.joinToString(" ")

                // Try direct match
                var matches = wordRepository.findByLanguageCodeAndNormalized(languageCode, candidate)

                // Try with articles stripped
                if (matches.isEmpty()) {
                    val strippedCandidate = stripArticles(candidate, languageCode)
                    if (strippedCandidate != candidate) {
                        matches = wordRepository.findByLanguageCodeAndNormalized(languageCode, strippedCandidate)
                    }
                }

                if (matches.isNotEmpty()) {
                    matchedWords.add(selectBestWord(matches))
                    i += len
                    found = true
                    break
                }
            }

            if (!found) {
                // Single word didn't match as part of combination, try search
                val searchResults = wordRepository.searchByLemma(languageCode, word)
                if (searchResults.isNotEmpty()) {
                    // Check if top result is a good match (exact or prefix)
                    val topResult = searchResults.first()
                    if (topResult.normalized == word || topResult.lemma.lowercase() == word) {
                        matchedWords.add(topResult)
                    } else {
                        unmatched.add(word)
                    }
                } else {
                    unmatched.add(word)
                }
                i++
            }
        }

        if (matchedWords.isNotEmpty()) {
            logger.debug(
                "Multi-word match for '{}': {} words matched, {} unmatched",
                originalPhrase, matchedWords.size, unmatched.size
            )
        }

        return MatchedVocabulary(originalPhrase, matchedWords.distinctBy { it.id }, unmatched)
    }

    /**
     * Strip language-specific articles from the beginning of a phrase.
     */
    private fun stripArticles(text: String, languageCode: String): String {
        var result = text
        val articles = getArticles(languageCode).sortedByDescending { it.length }

        for (article in articles) {
            // Handle elision (l', d')
            if (article.endsWith("'") && result.startsWith(article)) {
                result = result.removePrefix(article).trim()
                break
            }
            // Handle space-separated articles
            if (result.startsWith("$article ")) {
                result = result.removePrefix("$article ").trim()
                break
            }
        }

        return result
    }

    private fun getArticles(languageCode: String): List<String> =
        ARTICLE_PATTERNS[languageCode] ?: emptyList()

    /**
     * Select the best word from multiple matches.
     * Prefers lemmas (base forms) over inflected forms, and more frequent words.
     */
    private fun selectBestWord(words: List<Word>): Word {
        return words
            .sortedWith(
                compareBy<Word> { it.isInflectedForm }  // Prefer lemmas
                    .thenBy { it.frequencyRank ?: Int.MAX_VALUE }  // Prefer frequent words
            )
            .first()
    }
}
