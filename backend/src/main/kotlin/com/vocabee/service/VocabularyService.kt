package com.vocabee.service

import com.vocabee.domain.repository.WordRepository
import com.vocabee.web.dto.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VocabularyService(
    private val wordRepository: WordRepository
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
        val word = wordRepository.findWithDetails(languageCode, lemma) ?: return null

        return WordDto(
            id = word.id!!,
            languageCode = word.languageCode,
            lemma = word.lemma,
            partOfSpeech = word.partOfSpeech,
            frequencyRank = word.frequencyRank,
            definitions = word.definitions.map { def ->
                DefinitionDto(
                    id = def.id!!,
                    definitionNumber = def.definitionNumber,
                    definitionText = def.definitionText,
                    etymology = def.etymology,
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
            wordForms = word.wordForms.map { form ->
                WordFormDto(
                    id = form.id!!,
                    form = form.form,
                    formType = form.formType,
                    metadata = form.metadata
                )
            }
        )
    }
}
