package com.vocabee.web.api

import com.vocabee.service.JwtService
import com.vocabee.service.UserService
import com.vocabee.service.UserVocabularyService
import com.vocabee.web.dto.AddWordToVocabularyRequest
import com.vocabee.web.dto.VocabularyWordDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/vocabulary")
class UserVocabularyController(
    private val userVocabularyService: UserVocabularyService,
    private val userService: UserService,
    private val jwtService: JwtService
) {

    @GetMapping
    fun getUserVocabulary(
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<List<VocabularyWordDto>> {
        val userId = getUserIdFromToken(authorization) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val vocabulary = userVocabularyService.getUserVocabulary(userId)
        return ResponseEntity.ok(vocabulary)
    }

    @PostMapping
    fun addWordToVocabulary(
        @RequestHeader("Authorization") authorization: String,
        @RequestBody request: AddWordToVocabularyRequest
    ): ResponseEntity<VocabularyWordDto> {
        val userId = getUserIdFromToken(authorization) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val vocabularyWord = userVocabularyService.addWordToVocabulary(userId, request.wordId, request.notes)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity.status(HttpStatus.CREATED).body(vocabularyWord)
    }

    @DeleteMapping("/{wordId}")
    fun removeWordFromVocabulary(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable wordId: Long
    ): ResponseEntity<Void> {
        val userId = getUserIdFromToken(authorization) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val removed = userVocabularyService.removeWordFromVocabulary(userId, wordId)
        return if (removed) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @GetMapping("/check/{wordId}")
    fun checkIfWordInVocabulary(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable wordId: Long
    ): ResponseEntity<Map<String, Boolean>> {
        val userId = getUserIdFromToken(authorization) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val inVocabulary = userVocabularyService.isWordInVocabulary(userId, wordId)
        return ResponseEntity.ok(mapOf("inVocabulary" to inVocabulary))
    }

    private fun getUserIdFromToken(authorization: String): Long? {
        val token = authorization.removePrefix("Bearer ")
        val email = jwtService.getUserEmailFromToken(token) ?: return null
        val user = userService.findByEmail(email) ?: return null
        return user.id
    }
}
