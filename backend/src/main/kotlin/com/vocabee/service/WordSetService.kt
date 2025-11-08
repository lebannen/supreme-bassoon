package com.vocabee.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vocabee.domain.model.UserImportedWordSet
import com.vocabee.domain.model.UserVocabulary
import com.vocabee.domain.model.WordSet
import com.vocabee.domain.repository.*
import com.vocabee.web.dto.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File

@Service
class WordSetService(
    private val wordSetRepository: WordSetRepository,
    private val wordSetItemRepository: WordSetItemRepository,
    private val userImportedWordSetRepository: UserImportedWordSetRepository,
    private val wordRepository: WordRepository,
    private val userRepository: UserRepository,
    private val userVocabularyRepository: UserVocabularyRepository,
    private val languageRepository: LanguageRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(WordSetService::class.java)

    fun getAllWordSets(): List<WordSetDto> {
        val wordSets = wordSetRepository.findAll()
        return wordSets.map { toWordSetDto(it, false, 0) }
    }

    fun getWordSetsByLanguage(languageCode: String, userId: Long?): List<WordSetDto> {
        val wordSets = wordSetRepository.findByLanguageCodeOrderByLevelAscThemeAsc(languageCode)
        val importedSetIds = userId?.let {
            userImportedWordSetRepository.findImportedWordSetIdsByUserId(it).toSet()
        } ?: emptySet()

        return wordSets.map { wordSet ->
            val isImported = importedSetIds.contains(wordSet.id)
            val userVocabularyCount = userId?.let {
                countUserVocabularyInSet(it, wordSet.id!!)
            } ?: 0
            toWordSetDto(wordSet, isImported, userVocabularyCount)
        }
    }

    fun getWordSetById(wordSetId: Long, userId: Long?): WordSetDetailDto? {
        val wordSet = wordSetRepository.findById(wordSetId).orElse(null) ?: return null
        val isImported = userId?.let {
            userImportedWordSetRepository.existsByUserIdAndWordSetId(it, wordSetId)
        } ?: false

        val items = wordSetItemRepository.findByWordSetIdOrderByDisplayOrderAsc(wordSetId)
        val words = items.map { item ->
            WordSummaryDto(
                id = item.word.id!!,
                lemma = item.word.lemma,
                partOfSpeech = item.word.partOfSpeech,
                frequencyRank = item.word.frequencyRank
            )
        }

        val language = languageRepository.findByCode(wordSet.languageCode)
        return WordSetDetailDto(
            id = wordSet.id!!,
            name = wordSet.name,
            description = wordSet.description,
            languageCode = wordSet.languageCode,
            languageName = language?.name ?: wordSet.languageCode,
            level = wordSet.level,
            theme = wordSet.theme,
            wordCount = wordSet.wordCount,
            isImported = isImported,
            words = words
        )
    }

    @Transactional
    fun importWordSetToUserVocabulary(
        userId: Long,
        wordSetId: Long,
        addNotes: Boolean = false,
        customNotes: String? = null
    ): ImportWordSetResponse? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        val wordSet = wordSetRepository.findById(wordSetId).orElse(null) ?: return null

        val items = wordSetItemRepository.findByWordSetIdOrderByDisplayOrderAsc(wordSetId)

        var addedCount = 0
        var alreadyInVocabulary = 0

        val notes = when {
            customNotes != null -> customNotes
            addNotes -> "From: ${wordSet.name}"
            else -> null
        }

        items.forEach { item ->
            val word = item.word
            val existing = userVocabularyRepository.findByUserIdAndWordId(userId, word.id!!)

            if (existing == null) {
                val vocabularyEntry = UserVocabulary(
                    user = user,
                    word = word,
                    notes = notes
                )
                userVocabularyRepository.save(vocabularyEntry)
                addedCount++
            } else {
                alreadyInVocabulary++
            }
        }

        // Mark this set as imported for the user
        if (!userImportedWordSetRepository.existsByUserIdAndWordSetId(userId, wordSetId)) {
            val importedSet = UserImportedWordSet(
                user = user,
                wordSet = wordSet
            )
            userImportedWordSetRepository.save(importedSet)
        }

        return ImportWordSetResponse(
            wordSetId = wordSetId,
            totalWords = items.size,
            addedWords = addedCount,
            alreadyInVocabulary = alreadyInVocabulary,
            message = "Successfully imported word set: $addedCount new words added, $alreadyInVocabulary already in vocabulary"
        )
    }

    @Transactional
    fun loadWordSetsFromJson(filePath: String, languageCode: String, level: String?): Int {
        logger.info("Loading word sets from JSON file: $filePath for language: $languageCode")

        val file = File(filePath)
        if (!file.exists()) {
            logger.error("File not found: $filePath")
            return 0
        }

        val jsonContent: Map<String, List<Map<String, Any>>> = objectMapper.readValue(file)
        return loadWordSetsFromJsonContent(jsonContent, languageCode, level)
    }

    @Transactional
    fun loadWordSetsFromJsonContent(jsonContent: Map<String, Any>, languageCode: String?, level: String?): Int {
        logger.info("Loading word sets from JSON content")

        // Try to get language and level from JSON metadata first
        val metadataLanguage = jsonContent["language"] as? String
        val metadataLevel = jsonContent["level"] as? String

        val finalLanguage = metadataLanguage ?: languageCode
        val finalLevel = metadataLevel ?: level

        if (finalLanguage == null) {
            logger.error("No language specified in JSON or parameters")
            return 0
        }

        logger.info("Processing word sets for language: $finalLanguage, level: $finalLevel")

        // Check if using new format with "sets" key or old format with direct collections
        @Suppress("UNCHECKED_CAST")
        val sets = jsonContent["sets"] as? List<Map<String, Any>>

        if (sets != null) {
            // New format: { "language": "fr", "level": "A1", "sets": [...] }
            return processThemeList(sets, finalLanguage, finalLevel, "vocabulary")
        } else {
            // Old format: { "french_a1_vocabulary": [...] }
            @Suppress("UNCHECKED_CAST")
            val content = jsonContent.filterKeys { it != "language" && it != "level" } as? Map<String, List<Map<String, Any>>> ?: return 0

            var createdSets = 0
            content.entries.forEach { (setKey, themeList) ->
                createdSets += processThemeList(themeList, finalLanguage, finalLevel, setKey)
            }
            return createdSets
        }
    }

    private fun processThemeList(themeList: List<Map<String, Any>>, languageCode: String, level: String?, setKey: String): Int {
        var createdSets = 0
        logger.info("Processing set: $setKey with ${themeList.size} themes")

        themeList.forEach themeLoop@ { themeData ->
                val theme = themeData["theme"] as? String
                @Suppress("UNCHECKED_CAST")
                val words = themeData["words"] as? List<Map<String, Any>> ?: emptyList()

                val setName = if (theme != null) {
                    "${languageCode.uppercase()} ${level ?: ""} - $theme".trim()
                } else {
                    "${languageCode.uppercase()} ${level ?: ""} - $setKey".trim()
                }

                // Check if set already exists
                if (wordSetRepository.existsByNameAndLanguageCode(setName, languageCode)) {
                    logger.info("Word set already exists: $setName, skipping...")
                    return@themeLoop
                }

                val wordSet = WordSet(
                    name = setName,
                    description = theme?.let { "Themed word collection: $it" },
                    languageCode = languageCode,
                    level = level,
                    theme = theme
                )

                var addedWords = 0
                words.forEachIndexed { index, wordData ->
                    val wordText = wordData["word"] as? String ?: return@forEachIndexed

                    // Try to find word in database by lemma and language
                    val dbWord = wordRepository.findByLanguageCodeAndNormalized(
                        languageCode,
                        wordText.lowercase()
                    ).firstOrNull()

                    if (dbWord != null) {
                        wordSet.addWord(dbWord, index)
                        addedWords++
                    } else {
                        logger.debug("Word not found in database: $wordText")
                    }
                }

                if (addedWords > 0) {
                    wordSetRepository.save(wordSet)
                    createdSets++
                    logger.info("Created word set: $setName with $addedWords words")
                } else {
                    logger.warn("Skipped word set $setName - no words found in database")
                }
        }

        logger.info("Loaded $createdSets word sets")
        return createdSets
    }

    private fun countUserVocabularyInSet(userId: Long, wordSetId: Long): Int {
        val items = wordSetItemRepository.findByWordSetIdOrderByDisplayOrderAsc(wordSetId)
        val wordIds = items.map { it.word.id!! }

        return wordIds.count { wordId ->
            userVocabularyRepository.existsByUserIdAndWordId(userId, wordId)
        }
    }

    private fun toWordSetDto(wordSet: WordSet, isImported: Boolean, userVocabularyCount: Int): WordSetDto {
        val language = languageRepository.findByCode(wordSet.languageCode)
        return WordSetDto(
            id = wordSet.id!!,
            name = wordSet.name,
            description = wordSet.description,
            languageCode = wordSet.languageCode,
            languageName = language?.name ?: wordSet.languageCode,
            level = wordSet.level,
            theme = wordSet.theme,
            wordCount = wordSet.wordCount,
            isImported = isImported,
            userVocabularyCount = userVocabularyCount
        )
    }
}
