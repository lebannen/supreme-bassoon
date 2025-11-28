package com.vocabee.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vocabee.domain.model.UserImportedWordSet
import com.vocabee.domain.model.UserVocabulary
import com.vocabee.domain.model.Word
import com.vocabee.domain.model.WordSet
import com.vocabee.domain.repository.*
import com.vocabee.web.dto.ImportWordSetResponse
import com.vocabee.web.dto.WordSetDetailDto
import com.vocabee.web.dto.WordSetDto
import com.vocabee.web.dto.WordSummaryDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.util.*

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

    @Transactional(readOnly = true)
    fun getWordSetsByLanguage(languageCode: String, userId: Long?): List<WordSetDto> {
        val wordSets = wordSetRepository.findByLanguageCodeOrderByLevelAscThemeAsc(languageCode)
        if (userId == null) {
            return wordSets.map { toWordSetDto(it, false, 0) }
        }

        val wordSetIds = wordSets.mapNotNull { it.id }
        val importedSetIds = userImportedWordSetRepository.findImportedWordSetIdsByUserId(userId).toSet()

        val userVocabCounts = wordSetItemRepository.countUserVocabularyInSets(userId, wordSetIds)
            .associate { (it[0] as Long) to (it[1] as Long) }

        return wordSets.map { wordSet ->
            val isImported = importedSetIds.contains(wordSet.id)
            val userVocabularyCount = userVocabCounts[wordSet.id!!]?.toInt() ?: 0
            toWordSetDto(wordSet, isImported, userVocabularyCount)
        }
    }

    @Transactional(readOnly = true)
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

    /**
     * Get word set by published course module ID.
     */
    @Transactional(readOnly = true)
    fun getWordSetByModuleId(moduleId: Long, userId: Long?): WordSetDetailDto? {
        val wordSet = wordSetRepository.findByModuleId(moduleId) ?: return null
        return getWordSetDetailDto(wordSet, userId)
    }

    /**
     * Get word set by generation module plan ID (for pipeline preview).
     */
    @Transactional(readOnly = true)
    fun getWordSetByGenerationModulePlanId(modulePlanId: UUID, userId: Long?): WordSetDetailDto? {
        val wordSet = wordSetRepository.findByGenerationModulePlanId(modulePlanId) ?: return null
        return getWordSetDetailDto(wordSet, userId)
    }

    private fun getWordSetDetailDto(wordSet: WordSet, userId: Long?): WordSetDetailDto {
        val isImported = userId?.let {
            userImportedWordSetRepository.existsByUserIdAndWordSetId(it, wordSet.id!!)
        } ?: false

        val items = wordSetItemRepository.findByWordSetIdOrderByDisplayOrderAsc(wordSet.id!!)
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
    ): ImportWordSetResponse {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        val wordSet =
            wordSetRepository.findById(wordSetId).orElseThrow { IllegalArgumentException("WordSet not found") }

        val items = wordSetItemRepository.findByWordSetIdOrderByDisplayOrderAsc(wordSetId)
        val wordIds = items.map { it.word.id!! }

        val existingVocab = userVocabularyRepository.findByUserIdAndWordIdIn(userId, wordIds)
        val existingWordIds = existingVocab.map { it.word.id }.toSet()

        val notes = when {
            customNotes != null -> customNotes
            addNotes -> "From: ${wordSet.name}"
            else -> null
        }

        val newVocabEntries = items
            .filter { it.word.id !in existingWordIds }
            .map { item ->
                UserVocabulary(
                    user = user,
                    word = item.word,
                    notes = notes
                )
            }

        if (newVocabEntries.isNotEmpty()) {
            userVocabularyRepository.saveAll(newVocabEntries)
        }

        if (!userImportedWordSetRepository.existsByUserIdAndWordSetId(userId, wordSetId)) {
            val importedSet = UserImportedWordSet(user = user, wordSet = wordSet)
            userImportedWordSetRepository.save(importedSet)
        }

        return ImportWordSetResponse(
            wordSetId = wordSetId,
            totalWords = items.size,
            addedWords = newVocabEntries.size,
            alreadyInVocabulary = items.size - newVocabEntries.size,
            message = "Successfully imported word set: ${newVocabEntries.size} new words added."
        )
    }

    @Transactional
    fun loadWordSetsFromJson(inputStream: InputStream, languageCode: String, level: String?): Int {
        logger.info("Loading word sets from JSON stream for language: $languageCode")
        val jsonContent: Map<String, Any> = objectMapper.readValue(inputStream)
        return loadWordSetsFromJsonContent(jsonContent, languageCode, level)
    }

    @Transactional
    fun loadWordSetsFromJsonContent(jsonContent: Map<String, Any>, languageCode: String?, level: String?): Int {
        logger.info("Loading word sets from JSON content")

        val metadataLanguage = jsonContent["language"] as? String
        val metadataLevel = jsonContent["level"] as? String
        val finalLanguage =
            metadataLanguage ?: languageCode ?: throw IllegalArgumentException("Language code must be provided")
        val finalLevel = metadataLevel ?: level

        logger.info("Processing word sets for language: $finalLanguage, level: $finalLevel")

        @Suppress("UNCHECKED_CAST")
        val sets = jsonContent["sets"] as? List<Map<String, Any>>
            ?: throw IllegalArgumentException("JSON must contain a 'sets' key with a list of word sets")

        return processThemeList(sets, finalLanguage, finalLevel)
    }

    private fun processThemeList(themeList: List<Map<String, Any>>, languageCode: String, level: String?): Int {
        var createdSets = 0
        logger.info("Processing ${themeList.size} themes for language $languageCode")

        // 1. Collect all unique words from all themes
        val allWordsFromAllThemes = themeList.flatMap { themeData ->
            @Suppress("UNCHECKED_CAST")
            (themeData["words"] as? List<Map<String, Any>>)?.mapNotNull { it["word"] as? String } ?: emptyList()
        }.map { it.lowercase() }.distinct()

        // 2. Fetch all matching words from the database in a single query
        val dbWords = wordRepository.findByLanguageCodeAndNormalizedIn(languageCode, allWordsFromAllThemes)
        val dbWordsMap = dbWords.associateBy { it.normalized }

        // 3. Iterate and create word sets
        themeList.forEach themeLoop@ { themeData ->
            val theme = themeData["theme"] as? String
            val setName = if (theme != null) {
                "${languageCode.uppercase()} ${level ?: ""} - $theme".trim()
            } else {
                "${languageCode.uppercase()} ${level ?: ""} - General".trim()
            }

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

            @Suppress("UNCHECKED_CAST")
            val wordsData = themeData["words"] as? List<Map<String, Any>> ?: emptyList()
            val wordsToAdd = mutableListOf<Word>()

            wordsData.forEach { wordData ->
                val wordText = (wordData["word"] as? String)?.lowercase()
                dbWordsMap[wordText]?.let { wordsToAdd.add(it) }
            }

            if (wordsToAdd.isNotEmpty()) {
                wordsToAdd.forEachIndexed { index, word -> wordSet.addWord(word, index) }
                wordSetRepository.save(wordSet)
                createdSets++
                logger.info("Created word set: $setName with ${wordsToAdd.size} words")
            } else {
                logger.warn("Skipped word set $setName - no words found in database")
            }
        }

        logger.info("Loaded $createdSets word sets for language $languageCode")
        return createdSets
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
