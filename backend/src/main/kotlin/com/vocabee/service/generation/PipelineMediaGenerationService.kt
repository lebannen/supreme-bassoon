package com.vocabee.service.generation

import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.model.*
import com.vocabee.domain.repository.*
import com.vocabee.service.AudioGenerationService
import com.vocabee.service.ImageGenerationService
import com.vocabee.service.SpeakerVoiceConfig
import com.vocabee.service.StorageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Stage 6: Media Generation
 * Generates audio and scene images for episodes using character voice assignments and image prompts.
 */
@Service
class PipelineMediaGenerationService(
    private val audioGenerationService: AudioGenerationService,
    private val imageGenerationService: ImageGenerationService,
    private val storageService: StorageService,
    private val modulePlanRepository: GenerationModulePlanRepository,
    private val episodePlanRepository: GenerationEpisodePlanRepository,
    private val episodeContentRepository: GenerationEpisodeContentRepository,
    private val characterRepository: GenerationCharacterRepository,
    private val mediaRepository: GenerationMediaRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun generateAll(generation: CourseGeneration) {
        logger.info("Generating media for all episodes in generation ${generation.id}")

        val characters = characterRepository.findByGenerationId(generation.id)
        val modulePlans = modulePlanRepository.findByGenerationIdOrderByModuleNumber(generation.id)

        modulePlans.forEach { modulePlan ->
            val episodePlans = episodePlanRepository.findByModulePlanIdOrderByEpisodeNumber(modulePlan.id)

            episodePlans.forEach { episodePlan ->
                // Generate audio
                try {
                    generateEpisodeAudio(generation, modulePlan, episodePlan, characters)
                } catch (e: Exception) {
                    logger.error("Failed to generate audio for episode ${episodePlan.title}: ${e.message}", e)
                }

                // Generate scene images
                try {
                    generateEpisodeImages(generation, modulePlan, episodePlan, characters)
                } catch (e: Exception) {
                    logger.error("Failed to generate images for episode ${episodePlan.title}: ${e.message}", e)
                }
            }
        }

        logger.info("Media generation completed for generation ${generation.id}")
    }

    @Transactional
    fun generateEpisodeAudio(
        generation: CourseGeneration,
        modulePlan: GenerationModulePlan,
        episodePlan: GenerationEpisodePlan,
        characters: List<GenerationCharacter>
    ): GenerationMedia? {
        logger.info("Generating audio for episode: ${episodePlan.title}")

        // Check if already exists
        val existingMedia = mediaRepository.findByEpisodePlanId(episodePlan.id)
            .find { it.mediaType == GenerationMediaType.EPISODE_AUDIO.name }
        if (existingMedia?.status == GenerationStepStatus.COMPLETED) {
            logger.info("Audio already exists for episode ${episodePlan.title}")
            return existingMedia
        }

        // Create media record
        val media = existingMedia ?: GenerationMedia(
            generationId = generation.id,
            episodePlanId = episodePlan.id,
            mediaType = GenerationMediaType.EPISODE_AUDIO.name,
            status = GenerationStepStatus.PENDING
        )

        media.status = GenerationStepStatus.IN_PROGRESS
        mediaRepository.save(media)

        try {
            // Get episode content
            val content = episodeContentRepository.findByEpisodePlanId(episodePlan.id)
                ?: throw IllegalStateException("Episode content not found")

            // Get character voice assignments
            val episodeCharacterIds = episodePlan.characterIds?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
            } ?: emptyList()

            val episodeCharacters = characters.filter { char ->
                episodeCharacterIds.contains(char.id.toString())
            }

            // Build speaker configurations
            val speakers = episodeCharacters.take(2).map { char ->
                SpeakerVoiceConfig(
                    name = char.name,
                    voice = char.voiceId ?: getDefaultVoice(generation.languageCode, episodeCharacters.indexOf(char))
                )
            }

            // Generate transcript
            val transcript = content.content ?: throw IllegalStateException("No content text found")

            // Generate audio
            val audioUrl = audioGenerationService.generateAudio(
                transcript = transcript,
                languageCode = generation.languageCode,
                moduleNumber = modulePlan.moduleNumber,
                speakers = speakers,
                stylePrompt = "Natural, conversational tone. Moderate pace suitable for language learners."
            )

            // Update media record
            media.url = audioUrl
            media.metadata = objectMapper.valueToTree(
                mapOf(
                "episodeTitle" to episodePlan.title,
                "moduleNumber" to modulePlan.moduleNumber,
                "speakers" to speakers.map { mapOf("name" to it.name, "voice" to it.voice) }
            ))
            media.status = GenerationStepStatus.COMPLETED

            mediaRepository.save(media)

            logger.info("Generated audio for episode ${episodePlan.title}: $audioUrl")
            return media

        } catch (e: Exception) {
            logger.error("Audio generation failed for episode ${episodePlan.title}", e)
            media.status = GenerationStepStatus.FAILED
            media.errorMessage = e.message
            mediaRepository.save(media)
            throw e
        }
    }

    private fun getDefaultVoice(languageCode: String, speakerIndex: Int): String {
        val voices = when (languageCode) {
            "fr" -> listOf("Leda", "Charon")
            "de" -> listOf("Kore", "Charon")
            "es" -> listOf("Leda", "Charon")
            else -> listOf("Puck", "Charon")
        }
        return voices.getOrElse(speakerIndex) { voices.first() }
    }

    /**
     * Generate scene images for an episode based on stored image prompts.
     */
    @Transactional
    fun generateEpisodeImages(
        generation: CourseGeneration,
        modulePlan: GenerationModulePlan,
        episodePlan: GenerationEpisodePlan,
        characters: List<GenerationCharacter>
    ): List<GenerationMedia> {
        logger.info("Generating scene images for episode: ${episodePlan.title}")

        // Get episode content with image prompts
        val content = episodeContentRepository.findByEpisodePlanId(episodePlan.id)
            ?: throw IllegalStateException("Episode content not found")

        val imagePrompts = content.imagePrompts?.let { node ->
            objectMapper.convertValue(
                node,
                object : com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {})
        } ?: emptyList()

        if (imagePrompts.isEmpty()) {
            logger.info("No image prompts for episode ${episodePlan.title}, skipping image generation")
            return emptyList()
        }

        // Get character reference images for consistency
        val episodeCharacterIds = episodePlan.characterIds?.let { node ->
            objectMapper.convertValue(node, object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
        } ?: emptyList()

        val characterReferenceUrls = characters
            .filter { episodeCharacterIds.contains(it.id.toString()) }
            .mapNotNull { it.referenceImageUrl }

        if (characterReferenceUrls.isNotEmpty()) {
            logger.info("Using ${characterReferenceUrls.size} character reference images for consistency")
        }

        val generatedMedia = mutableListOf<GenerationMedia>()

        imagePrompts.forEachIndexed { index, promptData ->
            val description = promptData["description"] ?: return@forEachIndexed
            val sceneContext = promptData["sceneContext"] ?: ""

            try {
                logger.info("Generating image ${index + 1}/${imagePrompts.size} for episode ${episodePlan.title}")

                // Generate image using Gemini with character references
                val generatedImage = imageGenerationService.generateImage(
                    prompt = description,
                    referenceImageUrls = characterReferenceUrls
                )

                // Upload to storage
                val uploadResult = storageService.uploadFile(
                    inputStream = generatedImage.imageData.inputStream(),
                    originalFilename = "episode_${episodePlan.episodeNumber}_scene_${index + 1}.png",
                    contentType = generatedImage.mimeType,
                    fileType = FileType.IMAGE,
                    folder = "generations/${generation.id}/episodes/module_${modulePlan.moduleNumber}"
                )

                // Create media record
                val media = GenerationMedia(
                    generationId = generation.id,
                    episodePlanId = episodePlan.id,
                    mediaType = GenerationMediaType.SCENE_IMAGE.name,
                    url = uploadResult.url,
                    metadata = objectMapper.valueToTree(
                        mapOf(
                            "episodeTitle" to episodePlan.title,
                            "moduleNumber" to modulePlan.moduleNumber,
                            "imageIndex" to index,
                            "description" to description.take(500),
                            "sceneContext" to sceneContext
                        )
                    ),
                    status = GenerationStepStatus.COMPLETED
                )
                mediaRepository.save(media)
                generatedMedia.add(media)

                logger.info("Generated scene image ${index + 1} for episode ${episodePlan.title}: ${uploadResult.url}")

            } catch (e: Exception) {
                logger.error("Failed to generate image ${index + 1} for episode ${episodePlan.title}: ${e.message}", e)

                // Record failed media entry
                val failedMedia = GenerationMedia(
                    generationId = generation.id,
                    episodePlanId = episodePlan.id,
                    mediaType = GenerationMediaType.SCENE_IMAGE.name,
                    metadata = objectMapper.valueToTree(
                        mapOf(
                            "episodeTitle" to episodePlan.title,
                            "imageIndex" to index,
                            "description" to description.take(500)
                        )
                    ),
                    status = GenerationStepStatus.FAILED,
                    errorMessage = e.message
                )
                mediaRepository.save(failedMedia)
            }
        }

        logger.info("Generated ${generatedMedia.size}/${imagePrompts.size} scene images for episode ${episodePlan.title}")
        return generatedMedia
    }
}
