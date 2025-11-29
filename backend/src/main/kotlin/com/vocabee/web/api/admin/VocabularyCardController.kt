package com.vocabee.web.api.admin

import com.vocabee.domain.model.ReviewStatus
import com.vocabee.domain.model.VocabularyCard
import com.vocabee.domain.repository.VocabularyCardRepository
import com.vocabee.service.GenerationResult
import com.vocabee.service.VocabularyCardGenerationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/vocabulary-cards")
class VocabularyCardController(
    private val vocabularyCardGenerationService: VocabularyCardGenerationService,
    private val vocabularyCardRepository: VocabularyCardRepository
) {

    /**
     * Generate vocabulary cards for all words in a word set.
     */
    @PostMapping("/generate/word-set/{wordSetId}")
    fun generateForWordSet(
        @PathVariable wordSetId: Long,
        @RequestParam(required = false) cefrLevel: String?
    ): ResponseEntity<GenerationResult> {
        val result = vocabularyCardGenerationService.generateForWordSet(wordSetId, cefrLevel)
        return ResponseEntity.ok(result)
    }

    /**
     * Generate a single vocabulary card for a word.
     */
    @PostMapping("/generate/single")
    fun generateSingleCard(
        @RequestBody request: GenerateSingleCardRequest
    ): ResponseEntity<VocabularyCard> {
        val card = vocabularyCardGenerationService.generateCard(
            lemma = request.lemma,
            languageCode = request.languageCode,
            partOfSpeech = request.partOfSpeech,
            cefrLevel = request.cefrLevel ?: "A1"
        )
        return ResponseEntity.ok(card)
    }

    /**
     * Generate vocabulary cards for a batch of words (without word set).
     */
    @PostMapping("/generate/batch")
    fun generateBatch(
        @RequestBody request: GenerateBatchRequest
    ): ResponseEntity<GenerationResult> {
        val result = vocabularyCardGenerationService.generateBatch(
            words = request.words,
            languageCode = request.languageCode,
            cefrLevel = request.cefrLevel ?: "A1"
        )
        return ResponseEntity.ok(result)
    }

    /**
     * Get all vocabulary cards for a word set.
     */
    @GetMapping("/word-set/{wordSetId}")
    fun getCardsByWordSet(@PathVariable wordSetId: Long): ResponseEntity<List<VocabularyCard>> {
        val cards = vocabularyCardRepository.findByWordSetId(wordSetId)
        return ResponseEntity.ok(cards)
    }

    /**
     * Get vocabulary cards by language and review status.
     */
    @GetMapping("/language/{languageCode}")
    fun getCardsByLanguage(
        @PathVariable languageCode: String,
        @RequestParam(required = false) status: ReviewStatus?
    ): ResponseEntity<List<VocabularyCard>> {
        val cards = if (status != null) {
            vocabularyCardRepository.findByLanguageCodeAndReviewStatus(languageCode, status)
        } else {
            vocabularyCardRepository.searchByLemmaPrefix(languageCode, "")
        }
        return ResponseEntity.ok(cards)
    }

    /**
     * Search vocabulary cards by lemma prefix.
     */
    @GetMapping("/search")
    fun searchCards(
        @RequestParam languageCode: String,
        @RequestParam query: String
    ): ResponseEntity<List<VocabularyCard>> {
        val cards = vocabularyCardRepository.searchByLemmaPrefix(languageCode, query)
        return ResponseEntity.ok(cards)
    }

    /**
     * Get a single vocabulary card by ID.
     */
    @GetMapping("/{id}")
    fun getCardById(@PathVariable id: Long): ResponseEntity<VocabularyCard> {
        return vocabularyCardRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }

    /**
     * Update the review status of a vocabulary card.
     */
    @PutMapping("/{id}/status")
    fun updateCardStatus(
        @PathVariable id: Long,
        @RequestBody request: UpdateStatusRequest
    ): ResponseEntity<VocabularyCard> {
        val card = vocabularyCardRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Card not found: $id") }

        val updated = card.copy(
            reviewStatus = request.status,
            reviewedAt = java.time.LocalDateTime.now()
        )
        return ResponseEntity.ok(vocabularyCardRepository.save(updated))
    }

    /**
     * Delete a vocabulary card.
     */
    @DeleteMapping("/{id}")
    fun deleteCard(@PathVariable id: Long): ResponseEntity<Void> {
        if (!vocabularyCardRepository.existsById(id)) {
            return ResponseEntity.notFound().build()
        }
        vocabularyCardRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Get statistics for vocabulary cards.
     */
    @GetMapping("/stats")
    fun getStats(
        @RequestParam(required = false) languageCode: String?,
        @RequestParam(required = false) wordSetId: Long?
    ): ResponseEntity<VocabularyCardStats> {
        val stats = when {
            wordSetId != null -> {
                val count = vocabularyCardRepository.countByWordSetId(wordSetId)
                VocabularyCardStats(total = count)
            }

            languageCode != null -> {
                val count = vocabularyCardRepository.countByLanguageCode(languageCode)
                VocabularyCardStats(total = count)
            }

            else -> {
                val count = vocabularyCardRepository.count()
                VocabularyCardStats(total = count)
            }
        }
        return ResponseEntity.ok(stats)
    }
}

data class GenerateSingleCardRequest(
    val lemma: String,
    val languageCode: String,
    val partOfSpeech: String? = null,
    val cefrLevel: String? = null
)

data class GenerateBatchRequest(
    val words: List<String>,
    val languageCode: String,
    val cefrLevel: String? = null
)

data class UpdateStatusRequest(
    val status: ReviewStatus
)

data class VocabularyCardStats(
    val total: Long
)
