package com.vocabee.web.api

import com.vocabee.domain.model.CourseImportProgress
import com.vocabee.domain.model.ImportStatus
import com.vocabee.service.CourseImportRequest
import com.vocabee.service.CourseImportResult
import com.vocabee.service.CourseImportService
import com.vocabee.service.ModuleImportRequest
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
@RequestMapping("/api/admin/courses")
class CourseImportController(
    private val courseImportService: CourseImportService
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

    @PostMapping("/{courseSlug}/modules/import-async")
    fun importModuleAsync(
        @PathVariable courseSlug: String,
        @RequestBody request: ModuleImportRequest,
        @RequestParam(defaultValue = "true") generateAudio: Boolean
    ): ResponseEntity<ModuleImportStartResponse> {
        logger.info("Starting async import for module ${request.moduleNumber} in course $courseSlug")

        try {
            // Create import and get ID
            val importId = courseImportService.createImport(courseSlug, request.title)

            // Start async import
            courseImportService.importModuleAsync(importId, courseSlug, request, generateAudio)

            return ResponseEntity.ok(
                ModuleImportStartResponse(
                    importId = importId,
                    message = "Module import started",
                    courseSlug = courseSlug,
                    moduleNumber = request.moduleNumber,
                    moduleTitle = request.title
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to start module import", e)
            return ResponseEntity.badRequest().body(
                ModuleImportStartResponse(
                    importId = "",
                    message = "Failed to start import: ${e.message}",
                    courseSlug = courseSlug,
                    moduleNumber = request.moduleNumber,
                    moduleTitle = request.title
                )
            )
        }
    }

    @GetMapping("/import/progress/{importId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamProgress(@PathVariable importId: String): Flux<ServerSentEvent<CourseImportProgressDto>> {
        logger.info("Starting SSE stream for import $importId")

        return Flux.interval(Duration.ofMillis(500))
            .map {
                val progress = courseImportService.getProgress(importId)
                if (progress != null) {
                    val dto = CourseImportProgressDto(
                        importId = progress.importId,
                        courseSlug = progress.courseSlug,
                        courseName = progress.courseName,
                        status = progress.status.name,
                        totalModules = progress.totalModules,
                        processedModules = progress.processedModules,
                        totalEpisodes = progress.totalEpisodes,
                        processedEpisodes = progress.processedEpisodes,
                        totalExercises = progress.totalExercises,
                        processedExercises = progress.processedExercises,
                        audioFilesGenerated = progress.audioFilesGenerated,
                        currentModule = progress.currentModule,
                        currentEpisode = progress.currentEpisode,
                        message = progress.message,
                        progressPercentage = progress.progressPercentage,
                        errors = progress.errors,
                        error = progress.error
                    )

                    ServerSentEvent.builder(dto)
                        .id(it.toString())
                        .event("progress")
                        .build()
                } else {
                    ServerSentEvent.builder<CourseImportProgressDto>()
                        .id(it.toString())
                        .event("not_found")
                        .comment("Import not found")
                        .build()
                }
            }
            .takeUntil { event ->
                // Stop streaming when import is completed or failed
                event.data()?.let { dto ->
                    dto.status == ImportStatus.COMPLETED.name ||
                    dto.status == ImportStatus.FAILED.name
                } ?: false
            }
            .concatWith(
                Flux.just(
                    ServerSentEvent.builder<CourseImportProgressDto>()
                        .event("complete")
                        .comment("Stream completed")
                        .build()
                )
            )
            .doOnComplete { logger.info("SSE stream completed for import $importId") }
            .doOnCancel { logger.info("SSE stream cancelled for import $importId") }
            .doOnError { error -> logger.error("SSE stream error for import $importId", error) }
    }
}

data class ModuleImportStartResponse(
    val importId: String,
    val message: String,
    val courseSlug: String,
    val moduleNumber: Int,
    val moduleTitle: String
)

data class CourseImportProgressDto(
    val importId: String,
    val courseSlug: String,
    val courseName: String,
    val status: String,
    val totalModules: Int,
    val processedModules: Int,
    val totalEpisodes: Int,
    val processedEpisodes: Int,
    val totalExercises: Int,
    val processedExercises: Int,
    val audioFilesGenerated: Int,
    val currentModule: String?,
    val currentEpisode: String?,
    val message: String,
    val progressPercentage: Int,
    val errors: List<String>,
    val error: String?
)
