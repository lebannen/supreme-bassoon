package com.vocabee.web.api

import com.vocabee.domain.model.ImportProgress
import com.vocabee.domain.model.ImportStatus
import com.vocabee.service.ImportService
import com.vocabee.web.dto.ImportProgressDto
import com.vocabee.web.dto.ImportStartResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
@RequestMapping("/api/import")
class ImportController(
    private val importService: ImportService
) {
    private val logger = LoggerFactory.getLogger(ImportController::class.java)

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadLanguageFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("languageCode") languageCode: String
    ): ResponseEntity<ImportStartResponse> {
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(
                ImportStartResponse(
                    importId = "",
                    message = "File is empty"
                )
            )
        }

        val filename = file.originalFilename ?: ""
        val isGzipped = filename.endsWith(".gz")

        logger.info("Received file upload: $filename for language: $languageCode")

        val importId = importService.createImport(languageCode)

        // Copy file bytes to memory before async processing to avoid ClosedChannelException
        val fileBytes = file.bytes

        // Start async import
        importService.importLanguageData(
            importId = importId,
            languageCode = languageCode,
            inputStream = fileBytes.inputStream(),
            isGzipped = isGzipped
        )

        return ResponseEntity.ok(
            ImportStartResponse(
                importId = importId,
                message = "Import started successfully"
            )
        )
    }

    @GetMapping("/progress/{importId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamProgress(@PathVariable importId: String): Flux<ServerSentEvent<ImportProgressDto>> {
        return Flux.interval(Duration.ofMillis(500))
            .map {
                val progress = importService.getProgress(importId)
                if (progress != null) {
                    ServerSentEvent.builder(toDto(progress))
                        .id(it.toString())
                        .event("progress")
                        .build()
                } else {
                    ServerSentEvent.builder<ImportProgressDto>()
                        .id(it.toString())
                        .event("not_found")
                        .comment("Import not found")
                        .build()
                }
            }
            .takeUntil { event ->
                // Stop streaming when import is completed or failed
                event.data()?.let { dto ->
                    dto.status == ImportStatus.COMPLETED ||
                    dto.status == ImportStatus.FAILED ||
                    dto.status == ImportStatus.CANCELLED
                } ?: false
            }
            .concatWith(
                Flux.just(
                    ServerSentEvent.builder<ImportProgressDto>()
                        .event("complete")
                        .comment("Stream completed")
                        .build()
                )
            )
    }

    @GetMapping("/progress")
    fun getAllProgress(): ResponseEntity<List<ImportProgressDto>> {
        val allProgress = importService.getAllProgress().map { toDto(it) }
        return ResponseEntity.ok(allProgress)
    }

    @GetMapping("/progress/{importId}/status")
    fun getImportStatus(@PathVariable importId: String): ResponseEntity<ImportProgressDto> {
        val progress = importService.getProgress(importId)
        return if (progress != null) {
            ResponseEntity.ok(toDto(progress))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/clear")
    fun clearAllWords(): ResponseEntity<Map<String, Any>> {
        logger.info("Received request to clear all words from database")
        val deletedCount = importService.clearAllWords()
        logger.info("Cleared $deletedCount words from database")
        return ResponseEntity.ok(mapOf(
            "message" to "Successfully cleared all words from database",
            "deletedCount" to deletedCount
        ))
    }

    private fun toDto(progress: ImportProgress): ImportProgressDto {
        return ImportProgressDto(
            importId = progress.importId,
            languageCode = progress.languageCode,
            languageName = progress.languageName,
            status = progress.status,
            totalEntries = progress.totalEntries,
            processedEntries = progress.processedEntries,
            successfulEntries = progress.successfulEntries,
            failedEntries = progress.failedEntries,
            progressPercentage = progress.progressPercentage,
            message = progress.message,
            startedAt = progress.startedAt,
            completedAt = progress.completedAt,
            error = progress.error
        )
    }
}
