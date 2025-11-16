package com.vocabee.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.vocabee.domain.model.Exercise
import com.vocabee.domain.repository.ExerciseRepository
import com.vocabee.domain.repository.ExerciseTypeRepository
import com.vocabee.web.dto.*
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ExerciseImportService(
    private val exerciseRepository: ExerciseRepository,
    private val exerciseTypeRepository: ExerciseTypeRepository,
    private val audioGenerationService: AudioGenerationService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun importFromJson(
        moduleData: ModuleExerciseData,
        generateAudio: Boolean = true,
        overwriteExisting: Boolean = false
    ): ExerciseImportResult {
        logger.info("Importing ${moduleData.exercises.size} exercises from module ${moduleData.module}")

        var imported = 0
        var skipped = 0
        var audioGenerated = 0
        var audioFailed = 0
        val errors = mutableListOf<String>()
        val importedExercises = mutableListOf<ImportedExerciseDto>()

        for (exerciseData in moduleData.exercises) {
            try {
                // Get exercise type
                val exerciseType = exerciseTypeRepository.findByTypeKey(exerciseData.type)
                    ?: throw EntityNotFoundException("Exercise type not found: ${exerciseData.type}")

                // Check if exercise already exists
                if (!overwriteExisting) {
                    val existing = exerciseRepository.findByTitleAndLanguageCode(
                        exerciseData.title,
                        moduleData.languageCode
                    )
                    if (existing != null) {
                        logger.info("Skipping existing exercise: ${exerciseData.title}")
                        skipped++
                        importedExercises.add(
                            ImportedExerciseDto(
                                id = existing.id,
                                title = existing.title,
                                type = exerciseData.type,
                                status = ExerciseImportStatus.SKIPPED
                            )
                        )
                        continue
                    }
                }

                // Process content
                var content = exerciseData.content
                var audioUrl: String? = null

                // Handle audio generation for listening exercises
                if (exerciseData.type == "listening" && generateAudio) {
                    try {
                        val transcript = content.get("transcript")?.asText()
                        if (transcript != null) {
                            logger.info("Generating audio for transcript: '$transcript'")

                            // Generate audio and get MinIO URL
                            audioUrl = audioGenerationService.generateAudio(
                                transcript = transcript,
                                languageCode = moduleData.languageCode,
                                moduleNumber = moduleData.module,
                                voice = getVoiceForLanguage(moduleData.languageCode)
                            )

                            // Update content with generated audio URL
                            (content as ObjectNode).put("audioUrl", audioUrl)
                            audioGenerated++

                            logger.info("Generated audio for '${exerciseData.title}': $audioUrl")
                        } else {
                            logger.warn("No transcript found for listening exercise: ${exerciseData.title}")
                            errors.add("No transcript for ${exerciseData.title}")
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to generate audio for ${exerciseData.title}", e)
                        errors.add("Audio generation failed for ${exerciseData.title}: ${e.message}")
                        audioFailed++
                    }
                }

                // Create exercise entity
                val exercise = Exercise(
                    exerciseType = exerciseType,
                    languageCode = moduleData.languageCode,
                    cefrLevel = moduleData.cefrLevel,
                    title = exerciseData.title,
                    instructions = exerciseData.instructions,
                    content = content,
                    estimatedDurationSeconds = estimateDuration(exerciseData.type),
                    pointsValue = 10,
                    difficultyRating = 1.0,
                    isPublished = true,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                    createdBy = "import"
                )

                // Save exercise
                val savedExercise = exerciseRepository.save(exercise)
                imported++

                importedExercises.add(
                    ImportedExerciseDto(
                        id = savedExercise.id,
                        title = savedExercise.title,
                        type = exerciseData.type,
                        audioUrl = audioUrl,
                        status = ExerciseImportStatus.IMPORTED
                    )
                )

                logger.info("Imported exercise: ${exerciseData.title}")

            } catch (e: Exception) {
                logger.error("Failed to import exercise: ${exerciseData.title}", e)
                errors.add("Failed to import ${exerciseData.title}: ${e.message}")
                importedExercises.add(
                    ImportedExerciseDto(
                        id = null,
                        title = exerciseData.title,
                        type = exerciseData.type,
                        status = ExerciseImportStatus.FAILED
                    )
                )
            }
        }

        logger.info("Import complete: imported=$imported, skipped=$skipped, audioGenerated=$audioGenerated, audioFailed=$audioFailed")

        return ExerciseImportResult(
            imported = imported,
            skipped = skipped,
            audioGenerated = audioGenerated,
            audioFailed = audioFailed,
            errors = errors,
            exercises = importedExercises
        )
    }

    private fun extractTopic(title: String): String {
        // Extract topic from title (e.g., "Listening: 'Salut'" -> "Listening")
        return title.split(":").firstOrNull()?.trim() ?: title
    }

    private fun estimateDuration(exerciseType: String): Int {
        return when (exerciseType) {
            "multiple_choice" -> 60
            "fill_in_blank" -> 90
            "sentence_scramble" -> 120
            "matching" -> 120
            "listening" -> 90
            "cloze_reading" -> 180
            else -> 90
        }
    }

    private fun getVoiceForLanguage(languageCode: String): String {
        return when (languageCode) {
            "fr" -> "Leda"  // French Female
            "en" -> "Puck"  // English Neutral
            "de" -> "Kore"  // German Female
            else -> "Leda"  // Default to Leda
        }
    }
}
