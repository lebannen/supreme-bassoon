package com.vocabee.web.api

import com.vocabee.service.VocabularyService
import com.vocabee.web.dto.SearchResultDto
import com.vocabee.web.dto.WordDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = ["*"])  // Configure properly in production
class VocabularyController(
    private val vocabularyService: VocabularyService
) {

    @GetMapping("/search")
    fun search(
        @RequestParam("q") query: String,
        @RequestParam("lang") languageCode: String
    ): ResponseEntity<SearchResultDto> {
        val result = vocabularyService.searchWords(languageCode, query)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/words/{languageCode}/{lemma}")
    fun getWord(
        @PathVariable languageCode: String,
        @PathVariable lemma: String
    ): ResponseEntity<WordDto> {
        val word = vocabularyService.getWord(languageCode, lemma)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(word)
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP"))
    }
}
