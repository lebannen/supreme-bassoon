package com.vocabee.web.api.admin

import com.vocabee.service.ExerciseImportService
import com.vocabee.web.dto.ExerciseImportResult
import com.vocabee.web.dto.ModuleExerciseData
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/exercises")
class ExerciseImportController(
    private val exerciseImportService: ExerciseImportService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/import")
    fun importExercises(
        @RequestBody moduleData: ModuleExerciseData,
        @RequestParam(defaultValue = "true") generateAudio: Boolean,
        @RequestParam(defaultValue = "false") overwriteExisting: Boolean
    ): ResponseEntity<ExerciseImportResult> {
        logger.info("Import request received for module ${moduleData.module} (${moduleData.languageCode})")
        logger.info("Parameters: generateAudio=$generateAudio, overwriteExisting=$overwriteExisting")

        return try {
            val result = exerciseImportService.importFromJson(
                moduleData = moduleData,
                generateAudio = generateAudio,
                overwriteExisting = overwriteExisting
            )

            logger.info("Import completed: ${result.imported} imported, ${result.skipped} skipped, " +
                    "${result.audioGenerated} audio generated, ${result.audioFailed} audio failed")

            ResponseEntity.ok(result)
        } catch (e: Exception) {
            logger.error("Import failed", e)
            ResponseEntity.internalServerError().body(
                ExerciseImportResult(
                    imported = 0,
                    skipped = 0,
                    audioGenerated = 0,
                    audioFailed = 0,
                    errors = listOf("Import failed: ${e.message}"),
                    exercises = emptyList()
                )
            )
        }
    }
}
