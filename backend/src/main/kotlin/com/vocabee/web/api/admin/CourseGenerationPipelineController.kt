package com.vocabee.web.api.admin

import com.vocabee.service.generation.CourseGenerationPipelineService
import com.vocabee.service.generation.CoursePublishService
import com.vocabee.web.dto.CourseDto
import com.vocabee.web.dto.admin.generation.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * REST API for the course generation pipeline.
 * Provides endpoints to start, monitor, and control the multi-stage generation workflow.
 */
@RestController
@RequestMapping("/api/admin/generations")
class CourseGenerationPipelineController(
    private val generationService: CourseGenerationPipelineService,
    private val publishService: CoursePublishService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Start a new course generation workflow.
     */
    @PostMapping
    fun startGeneration(@RequestBody request: StartGenerationRequest): ResponseEntity<GenerationProgressResponse> {
        logger.info("Starting generation: ${request.languageCode} ${request.cefrLevel}")
        val response = generationService.startGeneration(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Get current progress of a generation workflow.
     * Returns all generated content up to the current stage.
     */
    @GetMapping("/{id}")
    fun getProgress(@PathVariable id: UUID): ResponseEntity<GenerationProgressResponse> {
        val response = generationService.getProgress(id)
        return ResponseEntity.ok(response)
    }

    /**
     * List all generation workflows.
     */
    @GetMapping
    fun listGenerations(): ResponseEntity<List<GenerationSummaryDto>> {
        val response = generationService.listGenerations()
        return ResponseEntity.ok(response)
    }

    /**
     * Approve current stage and proceed to the next one.
     */
    @PostMapping("/{id}/proceed")
    fun proceedToNextStage(@PathVariable id: UUID): ResponseEntity<GenerationProgressResponse> {
        logger.info("Proceeding to next stage for generation: $id")
        val response = generationService.proceedToNextStage(id)
        return ResponseEntity.ok(response)
    }

    /**
     * Regenerate the current stage with optional feedback.
     */
    @PostMapping("/{id}/regenerate")
    fun regenerateCurrentStage(
        @PathVariable id: UUID,
        @RequestBody(required = false) feedback: RegenerateFeedback?
    ): ResponseEntity<GenerationProgressResponse> {
        logger.info("Regenerating current stage for generation: $id")
        val response = generationService.regenerateCurrentStage(id, feedback)
        return ResponseEntity.ok(response)
    }

    /**
     * Cancel and delete a generation workflow.
     */
    @DeleteMapping("/{id}")
    fun cancelGeneration(@PathVariable id: UUID): ResponseEntity<Void> {
        logger.info("Cancelling generation: $id")
        generationService.cancelGeneration(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Publish a completed generation to an actual course.
     */
    @PostMapping("/{id}/publish")
    fun publishCourse(@PathVariable id: UUID): ResponseEntity<CourseDto> {
        logger.info("Publishing generation: $id")
        val course = publishService.publish(id)
        return ResponseEntity.status(HttpStatus.CREATED).body(course)
    }

    /**
     * Get debug data for a generation including all raw AI responses.
     */
    @GetMapping("/{id}/debug")
    fun getDebugData(@PathVariable id: UUID): ResponseEntity<GenerationDebugResponse> {
        logger.info("Getting debug data for generation: $id")
        val response = generationService.getDebugData(id)
        return ResponseEntity.ok(response)
    }
}
