package com.vocabee.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.model.ReviewStatus
import com.vocabee.domain.model.VocabularyCard
import com.vocabee.domain.repository.VocabularyCardRepository
import com.vocabee.domain.repository.WordSetRepository
import com.vocabee.service.external.gemini.GeminiTextClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for generating AI-powered vocabulary cards with consistent quality
 * and multi-language translations.
 */
@Service
class VocabularyCardGenerationService(
    private val geminiTextClient: GeminiTextClient,
    private val vocabularyCardRepository: VocabularyCardRepository,
    private val wordSetRepository: WordSetRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val BATCH_SIZE = 10
    }

    /**
     * Generate vocabulary cards for all words in a word set.
     * Skips words that already have cards. Uses batch generation for efficiency.
     */
    @Transactional
    fun generateForWordSet(wordSetId: Long, cefrLevel: String? = null): GenerationResult {
        val wordSet = wordSetRepository.findById(wordSetId)
            .orElseThrow { IllegalArgumentException("Word set not found: $wordSetId") }

        logger.info("Generating vocabulary cards for word set '${wordSet.name}' (${wordSet.wordCount} words)")

        val languageCode = wordSet.languageCode
        val level = cefrLevel ?: wordSet.level ?: "A1"

        // Filter out words that already have cards
        val wordsToGenerate = wordSet.items.filter { item ->
            !vocabularyCardRepository.existsByLanguageCodeAndLemmaAndPartOfSpeech(
                languageCode, item.word.lemma, item.word.partOfSpeech
            )
        }

        val skipped = wordSet.items.size - wordsToGenerate.size
        var generated = 0
        var failed = 0
        val errors = mutableListOf<String>()

        logger.info("Skipping $skipped existing cards, generating ${wordsToGenerate.size} new cards in batches of $BATCH_SIZE")

        // Process in batches
        wordsToGenerate.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
            val batchNumber = batchIndex + 1
            val totalBatches = (wordsToGenerate.size + BATCH_SIZE - 1) / BATCH_SIZE
            logger.info("Processing batch $batchNumber/$totalBatches (${batch.size} words)")

            try {
                val wordInputs = batch.map { item ->
                    WordInput(
                        lemma = item.word.lemma,
                        partOfSpeech = item.word.partOfSpeech,
                        sourceWordId = item.word.id
                    )
                }

                val cards = generateCardsBatch(
                    words = wordInputs,
                    languageCode = languageCode,
                    cefrLevel = level,
                    wordSetId = wordSetId
                )

                generated += cards.size
                logger.info("Batch $batchNumber complete: generated ${cards.size} cards (total: $generated/${wordsToGenerate.size})")

                // Track any words that weren't generated
                val generatedLemmas = cards.map { it.lemma }.toSet()
                batch.forEach { item ->
                    if (item.word.lemma !in generatedLemmas) {
                        failed++
                        errors.add("${item.word.lemma}: Not included in batch response")
                    }
                }
            } catch (e: Exception) {
                // If entire batch fails, count all as failed
                failed += batch.size
                batch.forEach { item ->
                    errors.add("${item.word.lemma}: ${e.message}")
                }
                logger.error("Batch $batchNumber failed", e)
            }
        }

        logger.info("Word set generation complete: $generated generated, $skipped skipped, $failed failed")

        return GenerationResult(
            wordSetId = wordSetId,
            wordSetName = wordSet.name,
            totalWords = wordSet.items.size,
            generated = generated,
            skipped = skipped,
            failed = failed,
            errors = errors
        )
    }

    /**
     * Generate multiple vocabulary cards in a single API call.
     */
    private fun generateCardsBatch(
        words: List<WordInput>,
        languageCode: String,
        cefrLevel: String,
        wordSetId: Long? = null
    ): List<VocabularyCard> {
        val prompt = buildBatchGenerationPrompt(words, languageCode, cefrLevel)
        val response = geminiTextClient.generateText(prompt, "application/json")
        val cardsData = parseResponse(response)

        val savedCards = mutableListOf<VocabularyCard>()

        // Response should be an array of card objects
        if (cardsData.isArray) {
            cardsData.forEach { cardData ->
                try {
                    val lemma = cardData.path("lemma").asText()
                    val partOfSpeech = cardData.path("partOfSpeech").asText(null)
                    val wordInput = words.find { it.lemma.equals(lemma, ignoreCase = true) }

                    // Check if card already exists (handles duplicates in response or race conditions)
                    val existingCard = vocabularyCardRepository.findByLanguageCodeAndLemmaAndPartOfSpeech(
                        languageCode, lemma, partOfSpeech ?: wordInput?.partOfSpeech
                    )
                    if (existingCard != null) {
                        logger.debug("Card already exists for '$lemma' ($partOfSpeech), skipping")
                        savedCards.add(existingCard) // Count as success since it exists
                        return@forEach
                    }

                    val card = VocabularyCard(
                        languageCode = languageCode,
                        lemma = lemma,
                        partOfSpeech = partOfSpeech ?: wordInput?.partOfSpeech,
                        ipa = cardData.path("ipa").asText(null),
                        gender = cardData.path("gender").asText(null),
                        pluralForm = cardData.path("pluralForm").asText(null),
                        verbGroup = cardData.path("verbGroup").asText(null),
                        grammarNotes = cardData.path("grammarNotes").asText(null),
                        definitions = cardData.path("definitions"),
                        examples = cardData.path("examples").takeIf { !it.isMissingNode && !it.isNull },
                        translations = cardData.path("translations"),
                        inflections = cardData.path("inflections").takeIf { !it.isMissingNode && !it.isNull },
                        cefrLevel = cefrLevel,
                        frequencyRank = cardData.path("frequencyRank").asInt().takeIf { it > 0 },
                        tags = cardData.path("tags").takeIf { !it.isMissingNode && !it.isNull },
                        wordSetId = wordSetId,
                        sourceWordId = wordInput?.sourceWordId,
                        modelUsed = geminiTextClient.getModelName(),
                        reviewStatus = ReviewStatus.PENDING
                    )

                    savedCards.add(vocabularyCardRepository.save(card))
                } catch (e: org.springframework.dao.DataIntegrityViolationException) {
                    // Duplicate key - card was created by another process, try to fetch it
                    val lemma = cardData.path("lemma").asText()
                    val partOfSpeech = cardData.path("partOfSpeech").asText(null)
                    logger.warn("Duplicate key for '$lemma', fetching existing card")
                    val existing = vocabularyCardRepository.findByLanguageCodeAndLemmaAndPartOfSpeech(
                        languageCode, lemma, partOfSpeech
                    )
                    if (existing != null) {
                        savedCards.add(existing)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to save card from batch response: ${cardData.path("lemma").asText()}", e)
                }
            }
        }

        return savedCards
    }

    /**
     * Generate a single vocabulary card for a word.
     */
    @Transactional
    fun generateCard(
        lemma: String,
        languageCode: String,
        partOfSpeech: String? = null,
        cefrLevel: String = "A1",
        wordSetId: Long? = null,
        sourceWordId: Long? = null
    ): VocabularyCard {
        logger.info("Generating vocabulary card for '$lemma' ($languageCode, $partOfSpeech)")

        val prompt = buildGenerationPrompt(lemma, languageCode, partOfSpeech, cefrLevel)
        val response = geminiTextClient.generateText(prompt, "application/json")
        val cardData = parseResponse(response)

        val card = VocabularyCard(
            languageCode = languageCode,
            lemma = lemma,
            partOfSpeech = cardData.path("partOfSpeech").asText(partOfSpeech),
            ipa = cardData.path("ipa").asText(null),
            gender = cardData.path("gender").asText(null),
            pluralForm = cardData.path("pluralForm").asText(null),
            verbGroup = cardData.path("verbGroup").asText(null),
            grammarNotes = cardData.path("grammarNotes").asText(null),
            definitions = cardData.path("definitions"),
            examples = cardData.path("examples").takeIf { !it.isMissingNode && !it.isNull },
            translations = cardData.path("translations"),
            inflections = cardData.path("inflections").takeIf { !it.isMissingNode && !it.isNull },
            cefrLevel = cefrLevel,
            frequencyRank = cardData.path("frequencyRank").asInt().takeIf { it > 0 },
            tags = cardData.path("tags").takeIf { !it.isMissingNode && !it.isNull },
            wordSetId = wordSetId,
            sourceWordId = sourceWordId,
            modelUsed = geminiTextClient.getModelName(),
            reviewStatus = ReviewStatus.PENDING
        )

        return vocabularyCardRepository.save(card)
    }

    /**
     * Generate cards for a list of words (batch generation without word set).
     */
    @Transactional
    fun generateBatch(
        words: List<String>,
        languageCode: String,
        cefrLevel: String = "A1"
    ): GenerationResult {
        logger.info("Batch generating ${words.size} vocabulary cards for $languageCode")

        // Filter out words that already have cards
        val wordsToGenerate = words.filter { word ->
            vocabularyCardRepository.findByLanguageCodeAndLemma(languageCode, word).isEmpty()
        }

        val skipped = words.size - wordsToGenerate.size
        var generated = 0
        var failed = 0
        val errors = mutableListOf<String>()

        logger.info("Skipping $skipped existing cards, generating ${wordsToGenerate.size} new cards in batches of $BATCH_SIZE")

        // Process in batches
        wordsToGenerate.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
            val batchNumber = batchIndex + 1
            val totalBatches = (wordsToGenerate.size + BATCH_SIZE - 1) / BATCH_SIZE
            logger.info("Processing batch $batchNumber/$totalBatches (${batch.size} words)")

            try {
                val wordInputs = batch.map { word -> WordInput(lemma = word) }

                val cards = generateCardsBatch(
                    words = wordInputs,
                    languageCode = languageCode,
                    cefrLevel = cefrLevel
                )

                generated += cards.size
                logger.info("Batch $batchNumber complete: generated ${cards.size} cards")

                // Track any words that weren't generated
                val generatedLemmas = cards.map { it.lemma.lowercase() }.toSet()
                batch.forEach { word ->
                    if (word.lowercase() !in generatedLemmas) {
                        failed++
                        errors.add("$word: Not included in batch response")
                    }
                }
            } catch (e: Exception) {
                failed += batch.size
                batch.forEach { word -> errors.add("$word: ${e.message}") }
                logger.error("Batch $batchNumber failed", e)
            }
        }

        return GenerationResult(
            wordSetId = null,
            wordSetName = null,
            totalWords = words.size,
            generated = generated,
            skipped = skipped,
            failed = failed,
            errors = errors
        )
    }

    private fun buildBatchGenerationPrompt(
        words: List<WordInput>,
        languageCode: String,
        cefrLevel: String
    ): String {
        val languageName = getLanguageName(languageCode)
        val wordsList = words.mapIndexed { index, word ->
            val posHint = word.partOfSpeech?.let { " ($it)" } ?: ""
            "${index + 1}. ${word.lemma}$posHint"
        }.joinToString("\n")

        return """
You are a professional language educator creating vocabulary flashcards for $cefrLevel level learners.

Generate comprehensive vocabulary cards for the following $languageName words:

$wordsList

Requirements:
1. Definitions should be simple and appropriate for $cefrLevel learners
2. Examples should use common, practical sentences
3. Translations must be accurate and natural in each target language
4. Include relevant grammar information for the language

Target translation languages: English (en), Spanish (es), Russian (ru)

Return ONLY a valid JSON array with one object per word in this exact format:
[
  {
    "lemma": "the word exactly as provided",
    "partOfSpeech": "noun|verb|adjective|adverb|etc",
    "ipa": "/phonetic transcription/",
    "gender": "masculine|feminine|neuter|null (for nouns in gendered languages)",
    "pluralForm": "plural form if noun, null otherwise",
    "verbGroup": "conjugation group if verb (e.g., '-er', '-ir', 'strong'), null otherwise",
    "grammarNotes": "Brief grammar note if relevant, null otherwise",
    "definitions": [
      "First definition (simple, $cefrLevel appropriate)",
      "Second definition if applicable"
    ],
    "examples": [
      {
        "sentence": "Example sentence in $languageName",
        "translation": "English translation of the example"
      }
    ],
    "translations": {
      "en": ["English translation 1", "English translation 2 if different meaning"],
      "es": ["Spanish translation 1"],
      "ru": ["Russian translation 1"]
    },
    "inflections": {
      "present": ["je ...", "tu ...", "il/elle ..."] (for verbs),
      "feminine": "feminine form" (for adjectives),
      "plural": "plural form" (for nouns/adjectives)
    },
    "frequencyRank": 0,
    "tags": ["category1", "category2"]
  }
]

Notes:
- IMPORTANT: Return exactly ${words.size} card objects, one for each word listed above
- Include the "lemma" field in each object matching the word exactly
- For frequencyRank, estimate 1-1000 for very common words, 1000-5000 for common, 5000+ for less common. Use 0 if unknown.
- Tags should include relevant categories like: "food", "travel", "body", "emotions", "time", "family", "work", etc.
- Inflections format varies by part of speech and language - include what's most useful for learners
- Keep definitions concise (under 15 words each)
- Provide 1-2 examples per word
- For Russian translations, use Cyrillic script

Generate all ${words.size} cards now:
        """.trimIndent()
    }

    private fun buildGenerationPrompt(
        lemma: String,
        languageCode: String,
        partOfSpeech: String?,
        cefrLevel: String
    ): String {
        val languageName = getLanguageName(languageCode)
        val posContext = partOfSpeech?.let { " as a $it" } ?: ""

        return """
You are a professional language educator creating vocabulary flashcards for $cefrLevel level learners.

Generate a comprehensive vocabulary card for the $languageName word "$lemma"$posContext.

Requirements:
1. Definitions should be simple and appropriate for $cefrLevel learners
2. Examples should use common, practical sentences
3. Translations must be accurate and natural in each target language
4. Include relevant grammar information for the language

Target translation languages: English (en), Spanish (es), Russian (ru)

Return ONLY valid JSON in this exact format:
{
  "partOfSpeech": "noun|verb|adjective|adverb|etc",
  "ipa": "/phonetic transcription/",
  "gender": "masculine|feminine|neuter|null (for nouns in gendered languages)",
  "pluralForm": "plural form if noun, null otherwise",
  "verbGroup": "conjugation group if verb (e.g., '-er', '-ir', 'strong'), null otherwise",
  "grammarNotes": "Brief grammar note if relevant, null otherwise",
  "definitions": [
    "First definition (simple, $cefrLevel appropriate)",
    "Second definition if applicable"
  ],
  "examples": [
    {
      "sentence": "Example sentence in $languageName",
      "translation": "English translation of the example"
    },
    {
      "sentence": "Another example",
      "translation": "Its translation"
    }
  ],
  "translations": {
    "en": ["English translation 1", "English translation 2 if different meaning"],
    "es": ["Spanish translation 1", "Spanish translation 2 if applicable"],
    "ru": ["Russian translation 1", "Russian translation 2 if applicable"]
  },
  "inflections": {
    "present": ["je ...", "tu ...", "il/elle ..."] (for verbs),
    "feminine": "feminine form" (for adjectives),
    "plural": "plural form" (for nouns/adjectives)
  },
  "frequencyRank": 0,
  "tags": ["category1", "category2"]
}

Notes:
- For frequencyRank, estimate 1-1000 for very common words, 1000-5000 for common, 5000+ for less common. Use 0 if unknown.
- Tags should include relevant categories like: "food", "travel", "body", "emotions", "time", "family", "work", etc.
- Inflections format varies by part of speech and language - include what's most useful for learners
- Keep definitions concise (under 15 words each)
- Provide 2-3 examples maximum
- For Russian translations, use Cyrillic script

Generate the card now:
        """.trimIndent()
    }

    private fun parseResponse(response: String): JsonNode {
        var json = response.trim()
        if (json.startsWith("```json")) {
            json = json.substring(7)
        } else if (json.startsWith("```")) {
            json = json.substring(3)
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length - 3)
        }
        return objectMapper.readTree(json.trim())
    }

    private fun getLanguageName(code: String): String = when (code.lowercase()) {
        "fr" -> "French"
        "de" -> "German"
        "es" -> "Spanish"
        "it" -> "Italian"
        "pt" -> "Portuguese"
        "nl" -> "Dutch"
        "ru" -> "Russian"
        "ja" -> "Japanese"
        "zh" -> "Chinese"
        "ko" -> "Korean"
        "en" -> "English"
        else -> code.uppercase()
    }
}

data class GenerationResult(
    val wordSetId: Long?,
    val wordSetName: String?,
    val totalWords: Int,
    val generated: Int,
    val skipped: Int,
    val failed: Int,
    val errors: List<String>
)

data class WordInput(
    val lemma: String,
    val partOfSpeech: String? = null,
    val sourceWordId: Long? = null
)
