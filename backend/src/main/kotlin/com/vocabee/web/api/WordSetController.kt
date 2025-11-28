package com.vocabee.web.api

import com.vocabee.service.JwtService
import com.vocabee.service.UserService
import com.vocabee.service.WordSetService
import com.vocabee.web.dto.ImportWordSetRequest
import com.vocabee.web.dto.ImportWordSetResponse
import com.vocabee.web.dto.WordSetDetailDto
import com.vocabee.web.dto.WordSetDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/word-sets")
class WordSetController(
    private val wordSetService: WordSetService,
    private val userService: UserService,
    private val jwtService: JwtService
) {

    @GetMapping
    fun getAllWordSets(): ResponseEntity<List<WordSetDto>> {
        val wordSets = wordSetService.getAllWordSets()
        return ResponseEntity.ok(wordSets)
    }

    @GetMapping("/language/{languageCode}")
    fun getWordSetsByLanguage(
        @PathVariable languageCode: String,
        @RequestHeader(value = "Authorization", required = false) authorization: String?
    ): ResponseEntity<List<WordSetDto>> {
        val userId = authorization?.let { getUserIdFromToken(it) }
        val wordSets = wordSetService.getWordSetsByLanguage(languageCode, userId)
        return ResponseEntity.ok(wordSets)
    }

    @GetMapping("/{id}")
    fun getWordSetById(
        @PathVariable id: Long,
        @RequestHeader(value = "Authorization", required = false) authorization: String?
    ): ResponseEntity<WordSetDetailDto> {
        val userId = authorization?.let { getUserIdFromToken(it) }
        val wordSet = wordSetService.getWordSetById(id, userId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(wordSet)
    }

    /**
     * Get word set for a published course module.
     */
    @GetMapping("/module/{moduleId}")
    fun getWordSetByModule(
        @PathVariable moduleId: Long,
        @RequestHeader(value = "Authorization", required = false) authorization: String?
    ): ResponseEntity<WordSetDetailDto> {
        val userId = authorization?.let { getUserIdFromToken(it) }
        val wordSet = wordSetService.getWordSetByModuleId(moduleId, userId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(wordSet)
    }

    /**
     * Get word set for a generation module plan (during pipeline).
     */
    @GetMapping("/generation-module/{modulePlanId}")
    fun getWordSetByGenerationModule(
        @PathVariable modulePlanId: java.util.UUID,
        @RequestHeader(value = "Authorization", required = false) authorization: String?
    ): ResponseEntity<WordSetDetailDto> {
        val userId = authorization?.let { getUserIdFromToken(it) }
        val wordSet = wordSetService.getWordSetByGenerationModulePlanId(modulePlanId, userId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(wordSet)
    }

    @PostMapping("/{id}/import")
    fun importWordSet(
        @PathVariable id: Long,
        @RequestHeader("Authorization") authorization: String,
        @RequestBody(required = false) request: ImportWordSetRequest?
    ): ResponseEntity<ImportWordSetResponse> {
        val userId = getUserIdFromToken(authorization)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val result = wordSetService.importWordSetToUserVocabulary(
            userId = userId,
            wordSetId = id,
            addNotes = request?.addNotes ?: false,
            customNotes = request?.notes
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(result)
    }

    @PostMapping("/load-from-json")
    fun loadWordSetsFromJson(
        @RequestParam filePath: String,
        @RequestParam languageCode: String,
        @RequestParam(required = false) level: String?
    ): ResponseEntity<Map<String, Any>> {
        val count = java.io.FileInputStream(filePath).use { inputStream ->
            wordSetService.loadWordSetsFromJson(inputStream, languageCode, level)
        }
        return ResponseEntity.ok(mapOf(
            "message" to "Successfully loaded word sets",
            "count" to count
        ))
    }

    @PostMapping("/load-from-json-content")
    fun loadWordSetsFromJsonContent(
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {
        @Suppress("UNCHECKED_CAST")
        val jsonContent = request["jsonContent"] as? Map<String, Any>
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Invalid JSON content"))

        // Language and level can come from JSON or request (optional overrides)
        val languageCode = request["languageCode"] as? String
        val level = request["level"] as? String

        val count = wordSetService.loadWordSetsFromJsonContent(jsonContent, languageCode, level)

        if (count == 0) {
            return ResponseEntity.badRequest().body(mapOf(
                "error" to "Failed to load word sets. Make sure 'language' is specified in the JSON or request."
            ))
        }

        return ResponseEntity.ok(mapOf(
            "message" to "Successfully loaded word sets",
            "count" to count
        ))
    }

    private fun getUserIdFromToken(authorization: String): Long? {
        val token = authorization.removePrefix("Bearer ")
        val email = jwtService.getUserEmailFromToken(token) ?: return null
        val user = userService.findByEmail(email) ?: return null
        return user.id
    }
}
