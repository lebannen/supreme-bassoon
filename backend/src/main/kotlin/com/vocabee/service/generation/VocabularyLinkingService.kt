package com.vocabee.service.generation

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.model.*
import com.vocabee.domain.repository.GenerationEpisodePlanRepository
import com.vocabee.domain.repository.GenerationModulePlanRepository
import com.vocabee.domain.repository.ModuleVocabularyMappingRepository
import com.vocabee.domain.repository.WordSetRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for linking generated vocabulary to dictionary words.
 * Creates module-level word sets during the VOCABULARY_LINKING pipeline stage.
 */
@Service
class VocabularyLinkingService(
    private val matchingService: VocabularyMatchingService,
    private val wordSetRepository: WordSetRepository,
    private val modulePlanRepository: GenerationModulePlanRepository,
    private val episodePlanRepository: GenerationEpisodePlanRepository,
    private val vocabMappingRepository: ModuleVocabularyMappingRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(VocabularyLinkingService::class.java)

    data class VocabularyLinkingProgress(
        val completed: Int,
        val total: Int,
        val currentItem: String?
    )

    data class ModuleVocabularyStats(
        val modulePlanId: java.util.UUID,
        val moduleNumber: Int,
        val totalPhrases: Int,
        val matchedPhrases: Int,
        val unmatchedPhrases: Int,
        val wordSetId: Long?,
        val wordCount: Int
    )

    /**
     * Process all modules for a generation and create word sets.
     */
    @Transactional
    fun processAll(generation: CourseGeneration): VocabularyLinkingProgress {
        val modulePlans = modulePlanRepository.findByGenerationIdOrderByModuleNumber(generation.id)
        logger.info("Processing vocabulary linking for ${modulePlans.size} modules in generation ${generation.id}")

        var completed = 0

        for (modulePlan in modulePlans) {
            try {
                processModuleVocabulary(modulePlan, generation.languageCode, generation.cefrLevel)
                completed++
                logger.info("Completed vocabulary linking for module ${modulePlan.moduleNumber}")
            } catch (e: Exception) {
                logger.error("Failed to process vocabulary for module ${modulePlan.moduleNumber}", e)
                throw e
            }
        }

        logger.info("Vocabulary linking complete: $completed/${modulePlans.size} modules processed")

        return VocabularyLinkingProgress(
            completed = completed,
            total = modulePlans.size,
            currentItem = null
        )
    }

    /**
     * Process vocabulary for a single module.
     */
    private fun processModuleVocabulary(
        modulePlan: GenerationModulePlan,
        languageCode: String,
        cefrLevel: String
    ): ModuleVocabularyStats {
        // Check if word set already exists for this module
        val existingWordSet = wordSetRepository.findByGenerationModulePlanId(modulePlan.id)
        if (existingWordSet != null) {
            logger.info("Word set already exists for module ${modulePlan.moduleNumber}, deleting and recreating")
            vocabMappingRepository.deleteByModulePlanId(modulePlan.id)
            wordSetRepository.delete(existingWordSet)
        }

        // Collect all vocabulary from episodes in this module
        val episodes = episodePlanRepository.findByModulePlanIdOrderByEpisodeNumber(modulePlan.id)
        val allVocabulary = episodes.flatMap { episode ->
            extractVocabulary(episode)
        }.distinct()

        logger.info("Module ${modulePlan.moduleNumber}: Found ${allVocabulary.size} unique vocabulary items from ${episodes.size} episodes")

        if (allVocabulary.isEmpty()) {
            logger.warn("No vocabulary found for module ${modulePlan.moduleNumber}")
            return ModuleVocabularyStats(
                modulePlanId = modulePlan.id,
                moduleNumber = modulePlan.moduleNumber,
                totalPhrases = 0,
                matchedPhrases = 0,
                unmatchedPhrases = 0,
                wordSetId = null,
                wordCount = 0
            )
        }

        // Match vocabulary to dictionary words
        val matchingResult = matchingService.matchVocabularyToWords(allVocabulary, languageCode)

        // Create word set
        val wordSet = WordSet(
            name = "Module ${modulePlan.moduleNumber}: ${modulePlan.title ?: "Untitled"}",
            description = modulePlan.description,
            languageCode = languageCode,
            level = cefrLevel,
            theme = modulePlan.theme,
            generationModulePlanId = modulePlan.id
        )
        wordSetRepository.save(wordSet)

        // Add matched words to set (deduplicated)
        val uniqueWords = matchingResult.matches
            .flatMap { it.matchedWords }
            .distinctBy { it.id }

        uniqueWords.forEachIndexed { index, word ->
            wordSet.addWord(word, index)
        }
        wordSetRepository.save(wordSet)

        // Track mappings for debugging/reporting
        matchingResult.matches.forEach { match ->
            val wordId = match.matchedWords.firstOrNull()?.id
            vocabMappingRepository.save(
                ModuleVocabularyMapping(
                    modulePlanId = modulePlan.id,
                    originalPhrase = match.originalPhrase,
                    wordId = wordId,
                    matched = match.matchedWords.isNotEmpty()
                )
            )
        }

        logger.info(
            "Module ${modulePlan.moduleNumber}: Created word set with ${uniqueWords.size} words " +
                    "(${matchingResult.matchedPhrases}/${matchingResult.totalPhrases} phrases matched)"
        )

        return ModuleVocabularyStats(
            modulePlanId = modulePlan.id,
            moduleNumber = modulePlan.moduleNumber,
            totalPhrases = matchingResult.totalPhrases,
            matchedPhrases = matchingResult.matchedPhrases,
            unmatchedPhrases = matchingResult.unmatchedPhrases,
            wordSetId = wordSet.id,
            wordCount = uniqueWords.size
        )
    }

    /**
     * Extract vocabulary list from an episode plan.
     */
    private fun extractVocabulary(episode: GenerationEpisodePlan): List<String> {
        return try {
            episode.vocabulary?.let { node ->
                objectMapper.convertValue(node, object : TypeReference<List<String>>() {})
            } ?: emptyList()
        } catch (e: Exception) {
            logger.warn("Failed to extract vocabulary from episode ${episode.episodeNumber}: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get vocabulary statistics for a generation.
     */
    @Transactional(readOnly = true)
    fun getVocabularyStats(generationId: java.util.UUID): List<ModuleVocabularyStats> {
        val modulePlans = modulePlanRepository.findByGenerationIdOrderByModuleNumber(generationId)

        return modulePlans.map { modulePlan ->
            val wordSet = wordSetRepository.findByGenerationModulePlanId(modulePlan.id)
            val totalMappings = vocabMappingRepository.countByModulePlanId(modulePlan.id)
            val matchedMappings = vocabMappingRepository.countByModulePlanIdAndMatched(modulePlan.id, true)

            ModuleVocabularyStats(
                modulePlanId = modulePlan.id,
                moduleNumber = modulePlan.moduleNumber,
                totalPhrases = totalMappings.toInt(),
                matchedPhrases = matchedMappings.toInt(),
                unmatchedPhrases = (totalMappings - matchedMappings).toInt(),
                wordSetId = wordSet?.id,
                wordCount = wordSet?.wordCount ?: 0
            )
        }
    }

    /**
     * Clear vocabulary linking data for a generation (for regeneration).
     */
    @Transactional
    fun clearVocabularyLinking(generationId: java.util.UUID) {
        val modulePlans = modulePlanRepository.findByGenerationIdOrderByModuleNumber(generationId)

        for (modulePlan in modulePlans) {
            vocabMappingRepository.deleteByModulePlanId(modulePlan.id)
            wordSetRepository.findByGenerationModulePlanId(modulePlan.id)?.let {
                wordSetRepository.delete(it)
            }
        }

        logger.info("Cleared vocabulary linking data for generation $generationId")
    }
}
