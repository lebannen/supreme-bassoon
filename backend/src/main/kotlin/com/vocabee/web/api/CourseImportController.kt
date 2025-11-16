package com.vocabee.web.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.service.CourseImportRequest
import com.vocabee.service.CourseImportResult
import com.vocabee.service.CourseImportService
import com.vocabee.service.ModuleImportRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/courses")
class CourseImportController(
    private val courseImportService: CourseImportService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/import")
    fun importCourse(
        @RequestBody request: CourseImportRequest
    ): ResponseEntity<CourseImportResult> {
        logger.info("Importing course: ${request.name}")

        try {
            val result = courseImportService.importCourse(request)
            return ResponseEntity.ok(result)
        } catch (e: Exception) {
            logger.error("Failed to import course", e)
            return ResponseEntity.badRequest().body(
                CourseImportResult(
                    courseId = 0,
                    modulesImported = 0,
                    episodesImported = 0,
                    exercisesImported = 0,
                    errors = listOf(e.message ?: "Unknown error")
                )
            )
        }
    }

    @PostMapping("/{courseSlug}/modules/import")
    fun importModule(
        @PathVariable courseSlug: String,
        @RequestBody request: ModuleImportRequest,
        @RequestParam(defaultValue = "true") generateAudio: Boolean
    ): ResponseEntity<CourseImportResult> {
        logger.info("Importing module ${request.moduleNumber} for course $courseSlug")

        try {
            val result = courseImportService.importModule(courseSlug, request, generateAudio)
            return ResponseEntity.ok(result)
        } catch (e: Exception) {
            logger.error("Failed to import module", e)
            return ResponseEntity.badRequest().body(
                CourseImportResult(
                    courseId = 0,
                    modulesImported = 0,
                    episodesImported = 0,
                    exercisesImported = 0,
                    errors = listOf(e.message ?: "Unknown error")
                )
            )
        }
    }
}
