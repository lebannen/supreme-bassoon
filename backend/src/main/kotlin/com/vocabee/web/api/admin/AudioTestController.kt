package com.vocabee.web.api.admin

import com.vocabee.service.AudioGenerationService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/audio")
class AudioTestController(
    private val audioGenerationService: AudioGenerationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/generate")
    fun generateAudio(@RequestBody request: GenerateAudioRequest): ResponseEntity<GenerateAudioResponse> {
        logger.info("Audio generation request: '${request.text}'")

        return try {
            val audioUrl = audioGenerationService.generateAudio(
                transcript = request.text,
                languageCode = request.languageCode,
                moduleNumber = 0, // Test files go to module_0
                voice = request.voice
            )

            ResponseEntity.ok(
                GenerateAudioResponse(
                    success = true,
                    audioUrl = audioUrl,
                    message = "Audio generated successfully"
                )
            )
        } catch (e: Exception) {
            logger.error("Audio generation failed", e)
            ResponseEntity.ok(
                GenerateAudioResponse(
                    success = false,
                    audioUrl = null,
                    message = "Error: ${e.message}"
                )
            )
        }
    }
}

data class GenerateAudioRequest(
    val text: String,
    val languageCode: String = "fr",
    val voice: String = "Leda"
)

data class GenerateAudioResponse(
    val success: Boolean,
    val audioUrl: String?,
    val message: String
)
