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
    private val languageRepository: LanguageRepository
) {

    fun searchWords(languageCode: String, query: String): SearchResultDto {
        val words = wordRepository.searchByLemma(languageCode, query)

        val summaries = words.map { word ->
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
        val word = wordRepository.findWithDefinitions(languageCode, lemma) ?: return null

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
