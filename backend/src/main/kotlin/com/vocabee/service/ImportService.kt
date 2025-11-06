package com.vocabee.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vocabee.domain.model.*
import com.vocabee.domain.repository.LanguageRepository
import com.vocabee.domain.repository.WordRepository
import com.vocabee.web.dto.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

@Service
class ImportService(
    private val wordRepository: WordRepository,
    private val languageRepository: LanguageRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(ImportService::class.java)
    private val progressMap = ConcurrentHashMap<String, ImportProgress>()

    companion object {
        const val BATCH_SIZE = 50
    }

    fun getProgress(importId: String): ImportProgress? = progressMap[importId]

    fun getAllProgress(): List<ImportProgress> = progressMap.values.toList()

    @Async
    fun importLanguageData(
        importId: String,
        languageCode: String,
        inputStream: InputStream,
        isGzipped: Boolean
    ) {
        val language = languageRepository.findByCode(languageCode)
        if (language == null) {
            logger.error("Import $importId failed: Language not found: $languageCode")
            updateProgress(importId) {
                it.copy(
                    status = ImportStatus.FAILED,
                    error = "Language not found: $languageCode",
                    completedAt = LocalDateTime.now()
                )
            }
            return
        }

        updateProgress(importId) {
            it.copy(
                languageCode = languageCode,
                languageName = language.name,
                status = ImportStatus.PROCESSING,
                message = "Reading file..."
            )
        }

        try {
            logger.info("Starting import $importId for language $languageCode, isGzipped=$isGzipped")

            // Read all bytes into memory first
            val fileBytes = inputStream.readAllBytes()
            logger.info("Read ${fileBytes.size} bytes for import $importId")

            // Create input stream for decompression if needed
            val decompressedBytes = if (isGzipped) {
                logger.info("Decompressing gzipped file for import $importId")
                GZIPInputStream(fileBytes.inputStream()).readAllBytes()
            } else {
                fileBytes
            }
            logger.info("Processing ${decompressedBytes.size} bytes (after decompression) for import $importId")

            processJsonlFile(importId, languageCode, decompressedBytes)

            updateProgress(importId) {
                it.copy(
                    status = ImportStatus.COMPLETED,
                    message = "Import completed successfully",
                    completedAt = LocalDateTime.now()
                )
            }
            logger.info("Import $importId completed successfully")
        } catch (e: Exception) {
            logger.error("Import failed for $importId: ${e.javaClass.simpleName} - ${e.message}", e)
            updateProgress(importId) {
                it.copy(
                    status = ImportStatus.FAILED,
                    error = "${e.javaClass.simpleName}: ${e.message}",
                    message = "Import failed",
                    completedAt = LocalDateTime.now()
                )
            }
        }
    }

    private fun processJsonlFile(importId: String, languageCode: String, fileBytes: ByteArray) {
        logger.info("Starting to process JSONL file for import $importId, ${fileBytes.size} bytes")

        // First pass: count total entries
        val countReader = BufferedReader(InputStreamReader(fileBytes.inputStream(), Charsets.UTF_8))
        val totalEntries = countReader.useLines { it.count().toLong() }
        logger.info("Import $importId: Counted $totalEntries total entries")

        updateProgress(importId) {
            it.copy(
                totalEntries = totalEntries,
                message = "Processing entries..."
            )
        }

        var processedEntries = 0L
        var successfulEntries = 0L
        var failedEntries = 0L
        var currentBatch = 0

        val wordBatch = mutableListOf<Word>()

        // Cache for lemma lookup (word -> word ID)
        val lemmaCache = mutableMapOf<String, Long>()

        // Second pass: process entries
        val processReader = BufferedReader(InputStreamReader(fileBytes.inputStream(), Charsets.UTF_8))
        logger.info("Import $importId: Starting to process entries")

        processReader.useLines { lines ->
            lines.forEach { line ->
                try {
                    val entry = objectMapper.readValue<WordEntryJson>(line)

                    val word = createWordFromEntry(languageCode, entry, lemmaCache)
                    wordBatch.add(word)

                    // Process batch when it reaches BATCH_SIZE
                    if (wordBatch.size >= BATCH_SIZE) {
                        logger.info("Import $importId: Processing batch $currentBatch with ${wordBatch.size} words")
                        try {
                            val savedCount = processBatch(wordBatch, lemmaCache, languageCode)
                            currentBatch++
                            successfulEntries += savedCount
                            logger.info("Import $importId: Batch ${currentBatch-1} saved successfully, total successful: $successfulEntries")
                        } catch (e: Exception) {
                            logger.warn("Import $importId: Batch $currentBatch failed, retrying word-by-word: ${e.javaClass.simpleName} - ${e.message}")
                            // Retry word by word to isolate problematic entries
                            wordBatch.forEach { word ->
                                try {
                                    val savedCount = processBatch(listOf(word), lemmaCache, languageCode)
                                    successfulEntries += savedCount
                                } catch (wordError: Exception) {
                                    logger.warn("Import $importId: Failed to save word '${word.lemma}': ${wordError.javaClass.simpleName} - ${wordError.message}")
                                    failedEntries++
                                }
                            }
                            currentBatch++
                        }
                        wordBatch.clear()
                    }

                    processedEntries++

                    // Update progress every 100 entries
                    if (processedEntries % 100 == 0L) {
                        logger.debug("Import $importId: Processed $processedEntries/$totalEntries entries")
                        updateProgress(importId) {
                            it.copy(
                                processedEntries = processedEntries,
                                successfulEntries = successfulEntries,
                                failedEntries = failedEntries,
                                currentBatch = currentBatch,
                                message = "Processing entry $processedEntries of $totalEntries"
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Import $importId: Failed to process entry at line $processedEntries: ${e.javaClass.simpleName} - ${e.message}")
                    failedEntries++
                    processedEntries++
                }
            }
        }

        // Process remaining entries
        if (wordBatch.isNotEmpty()) {
            try {
                val savedCount = processBatch(wordBatch, lemmaCache, languageCode)
                successfulEntries += savedCount
            } catch (e: Exception) {
                logger.warn("Import $importId: Final batch failed, retrying word-by-word: ${e.javaClass.simpleName} - ${e.message}")
                // Retry word by word to isolate problematic entries
                wordBatch.forEach { word ->
                    try {
                        val savedCount = processBatch(listOf(word), lemmaCache, languageCode)
                        successfulEntries += savedCount
                    } catch (wordError: Exception) {
                        logger.warn("Import $importId: Failed to save word '${word.lemma}': ${wordError.javaClass.simpleName} - ${wordError.message}")
                        failedEntries++
                    }
                }
            }
        }

        updateProgress(importId) {
            it.copy(
                processedEntries = processedEntries,
                successfulEntries = successfulEntries,
                failedEntries = failedEntries,
                message = "Import completed: $successfulEntries successful, $failedEntries failed"
            )
        }
    }

    @Transactional
    protected fun processBatch(
        wordBatch: List<Word>,
        lemmaCache: MutableMap<String, Long>,
        languageCode: String
    ): Long {
        logger.debug("Saving batch of ${wordBatch.size} words to database")
        // Save words with their definitions, pronunciations, and examples
        // Cascade will automatically save all related entities
        val savedWords = wordRepository.saveAll(wordBatch)
        val savedCount = savedWords.count().toLong()
        logger.debug("Successfully saved $savedCount words with all related data")

        // Update lemma cache
        savedWords.forEach { word ->
            if (!word.isInflectedForm) {
                lemmaCache["${word.lemma}|${word.partOfSpeech}"] = word.id!!
            }
        }
        logger.debug("Updated lemma cache with ${savedWords.count { !it.isInflectedForm }} base forms")

        return savedCount
    }

    private fun createWordFromEntry(
        languageCode: String,
        entry: WordEntryJson,
        lemmaCache: Map<String, Long>
    ): Word {
        val wordText = entry.getWordText()
        val normalized = entry.normalized ?: wordText.lowercase()
        val isInflectedForm = entry.is_inflected_form ?: false

        val lemmaId = if (isInflectedForm && entry.inflected_form_of != null) {
            val lemmaKey = "${entry.inflected_form_of.lemma}|${entry.part_of_speech}"
            lemmaCache[lemmaKey]
        } else null

        val grammaticalFeatures = if (isInflectedForm && entry.inflected_form_of != null) {
            buildMap<String, Any> {
                entry.inflected_form_of.person?.let { put("person", it) }
                entry.inflected_form_of.number?.let { put("number", it) }
                entry.inflected_form_of.tense?.let { put("tense", it) }
                entry.inflected_form_of.mood?.let { put("mood", it) }
                entry.inflected_form_of.gender?.let { put("gender", it) }
                entry.inflected_form_of.case?.let { put("case", it) }
            }.takeIf { it.isNotEmpty() }
        } else null

        val word = Word(
            languageCode = languageCode,
            lemma = wordText,
            normalized = normalized,
            partOfSpeech = entry.part_of_speech,
            etymology = entry.etymology,
            usageNotes = entry.usage_notes,
            isInflectedForm = isInflectedForm,
            lemmaId = lemmaId,
            grammaticalFeatures = grammaticalFeatures
        )

        // Add pronunciations
        entry.pronunciations?.forEach { pronJson ->
            val pronunciation = Pronunciation(
                word = word,
                ipa = pronJson.ipa,
                audioUrl = pronJson.audio_file,
                dialect = pronJson.dialect
            )
            word.pronunciations.add(pronunciation)
        }

        // Add definitions with examples
        entry.definitions?.forEach { defJson ->
            val definition = Definition(
                word = word,
                definitionNumber = defJson.definition_number,
                definitionText = defJson.text
            )

            // Add examples to the definition
            defJson.examples?.forEach { exJson ->
                val example = Example(
                    definition = definition,
                    sentenceText = exJson.text,
                    translation = exJson.translation
                )
                definition.examples.add(example)
            }

            word.definitions.add(definition)
        }

        return word
    }

    fun createImport(languageCode: String): String {
        val importId = UUID.randomUUID().toString()
        val progress = ImportProgress(
            importId = importId,
            languageCode = languageCode,
            languageName = "",
            status = ImportStatus.QUEUED,
            message = "Import queued"
        )
        progressMap[importId] = progress
        return importId
    }

    @Transactional
    fun clearAllWords(): Long {
        logger.info("Clearing all words from database")
        val count = wordRepository.count()
        wordRepository.deleteAll()
        logger.info("Deleted $count words from database")
        return count
    }

    private fun updateProgress(importId: String, updater: (ImportProgress) -> ImportProgress) {
        progressMap.computeIfPresent(importId) { _, current -> updater(current) }
    }
}
