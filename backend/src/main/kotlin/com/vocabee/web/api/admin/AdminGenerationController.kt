package com.vocabee.web.api.admin

import com.vocabee.domain.generation.GeneratedContentItem
import com.vocabee.domain.generation.GeneratedModule
import com.vocabee.service.content.ContentGenerationService
import com.vocabee.service.content.ContentValidator
import com.vocabee.web.dto.admin.generation.DialogueGenerationResponse
import com.vocabee.web.dto.admin.generation.GenerateExerciseRequest
import com.vocabee.web.dto.admin.generation.GenerateModuleRequest
import com.vocabee.web.dto.admin.generation.GenerateStructureRequest
import com.vocabee.web.dto.admin.generation.ModuleGenerationResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/generation")
class AdminGenerationController(
    private val contentGenerationService: ContentGenerationService,
    private val contentValidator: ContentValidator
) {

    @PostMapping("/module")
    fun generateModule(@RequestBody request: GenerateModuleRequest): ResponseEntity<ModuleGenerationResponse> {
        val response = contentGenerationService.generateModule(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/dialogue")
    fun generateDialogue(@RequestBody request: GenerateModuleRequest): ResponseEntity<DialogueGenerationResponse> {
        val dialogue = contentGenerationService.generateDialogue(request)
        return ResponseEntity.ok(DialogueGenerationResponse(dialogue))
    }

    @PostMapping("/structure")
    fun generateStructure(@RequestBody request: GenerateStructureRequest): ResponseEntity<ModuleGenerationResponse> {
        val response = contentGenerationService.generateStructure(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/exercise")
    fun generateExercise(@RequestBody request: GenerateExerciseRequest): ResponseEntity<GeneratedContentItem> {
        val exercise = contentGenerationService.generateExercise(request)
        return ResponseEntity.ok(exercise)
    }

    @PostMapping("/validate")
    fun validateModule(@RequestBody module: GeneratedModule): ResponseEntity<List<String>> {
        val errors = contentValidator.validate(module)
        return ResponseEntity.ok(errors)
    }
}
