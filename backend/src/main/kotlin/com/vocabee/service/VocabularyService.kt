package com.vocabee.service

import com.vocabee.domain.repository.LanguageRepository
import com.vocabee.domain.repository.WordRepository
import com.vocabee.web.dto.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VocabularyService(
    private val wordRepository: WordRepository,
    private val languageRepository: LanguageRepository,
    private val searchRankingService: SearchRankingService
) {

    fun searchWords(languageCode: String, query: String): SearchResultDto {
        // Stage 1: Get up to 2000 candidates from database with basic ranking
        val candidates = wordRepository.searchByLemma(languageCode, query)

        // Stage 2: Apply sophisticated application-layer ranking and limit to top 500
        val rankedWords = searchRankingService.rankAndLimit(candidates, query, languageCode)

        val summaries = rankedWords.map { word ->
            WordSummaryDto(
                id = word.id!!,
                lemma = word.lemma,
                partOfSpeech = word.partOfSpeech,
                frequencyRank = word.frequencyRank
            )
        }

        return SearchResultDto(
            words = summaries,
            total = summaries.size
        )
    }

    fun getWord(languageCode: String, lemma: String): WordDto? {
        // Get all entries for this lemma (e.g., "avoir" has both noun and verb)
        val words = wordRepository.findAllWithDefinitions(languageCode, lemma)

        // Order by POS priority: verb > noun > adjective > other
        val word = words.sortedBy {
            when (it.partOfSpeech?.lowercase()) {
                "verb" -> 0
                "noun" -> 1
                "adjective" -> 2
                else -> 3
            }
        }.firstOrNull() ?: return null

        // Get inflected forms if this is a lemma (not an inflected form itself)
        val inflectedForms = if (!word.isInflectedForm) {
            wordRepository.findByIsInflectedFormTrueAndLemmaId(word.id!!)
                .map { form ->
                    InflectedFormDto(
                        id = form.id!!,
                        form = form.lemma,
                        partOfSpeech = form.partOfSpeech,
                        grammaticalFeatures = form.grammaticalFeatures
                    )
                }
        } else {
            emptyList()
        }

        // Get base form if this is an inflected form
        val baseForm = if (word.isInflectedForm && word.lemmaId != null) {
            wordRepository.findById(word.lemmaId).map { base ->
                BaseFormDto(
                    id = base.id!!,
                    lemma = base.lemma,
                    partOfSpeech = base.partOfSpeech
                )
            }.orElse(null)
        } else {
            null
        }

        return WordDto(
            id = word.id!!,
            languageCode = word.languageCode,
            lemma = word.lemma,
            partOfSpeech = word.partOfSpeech,
            etymology = word.etymology,
            usageNotes = word.usageNotes,
            frequencyRank = word.frequencyRank,
            isInflectedForm = word.isInflectedForm,
            lemmaId = word.lemmaId,
            grammaticalFeatures = word.grammaticalFeatures,
            baseForm = baseForm,
            definitions = word.definitions.map { def ->
                DefinitionDto(
                    id = def.id!!,
                    definitionNumber = def.definitionNumber,
                    definitionText = def.definitionText,
                    examples = def.examples.map { ex ->
                        ExampleDto(
                            id = ex.id!!,
                            sentenceText = ex.sentenceText,
                            translation = ex.translation
                        )
                    }
                )
            },
            pronunciations = word.pronunciations.map { pron ->
                PronunciationDto(
                    id = pron.id!!,
                    ipa = pron.ipa,
                    audioUrl = pron.audioUrl,
                    dialect = pron.dialect
                )
            },
            inflectedForms = inflectedForms
        )
    }

    fun getAllLanguages(): List<LanguageDto> {
        return languageRepository.findAllByOrderByNameAsc()
            .map { lang ->
                LanguageDto(
                    code = lang.code,
                    name = lang.name,
                    entryCount = lang.entryCount
                )
            }
    }
}
