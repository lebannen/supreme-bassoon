package com.vocabee.service.generation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vocabee.domain.model.*
import com.vocabee.domain.repository.*
import com.vocabee.service.external.gemini.GeminiTextClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Stage 5: Exercise Generation
 * Generates exercises for each episode based on vocabulary and grammar targets.
 */
@Service
class PipelineExerciseGenerationService(
    private val geminiClient: GeminiTextClient,
    private val modulePlanRepository: GenerationModulePlanRepository,
    private val episodePlanRepository: GenerationEpisodePlanRepository,
    private val episodeContentRepository: GenerationEpisodeContentRepository,
    private val exercisesRepository: GenerationExercisesRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun generateAll(generation: CourseGeneration) {
        logger.info("Generating exercises for all episodes in generation ${generation.id}")

        val modulePlans = modulePlanRepository.findByGenerationIdOrderByModuleNumber(generation.id)

        modulePlans.forEach { modulePlan ->
            val episodePlans = episodePlanRepository.findByModulePlanIdOrderByEpisodeNumber(modulePlan.id)

            episodePlans.forEach { episodePlan ->
                try {
                    generateEpisodeExercises(generation, episodePlan)
                } catch (e: Exception) {
                    logger.error("Failed to generate exercises for episode ${episodePlan.title}: ${e.message}", e)
                }
            }
        }

        logger.info("Exercise generation completed for generation ${generation.id}")
    }

    @Transactional
    fun generateEpisodeExercises(
        generation: CourseGeneration,
        episodePlan: GenerationEpisodePlan
    ): GenerationExercises {
        logger.info("Generating exercises for episode: ${episodePlan.title}")

        // Get or create exercises record
        var exercises = exercisesRepository.findByEpisodePlanId(episodePlan.id)
        if (exercises == null) {
            exercises = GenerationExercises(
                episodePlanId = episodePlan.id,
                status = GenerationStepStatus.PENDING
            )
            exercisesRepository.save(exercises)
        }

        exercises.status = GenerationStepStatus.IN_PROGRESS
        exercisesRepository.save(exercises)

        try {
            // Get episode content
            val content = episodeContentRepository.findByEpisodePlanId(episodePlan.id)
                ?: throw IllegalStateException("Episode content not found")

            // Get vocabulary and grammar targets
            val vocabulary = episodePlan.vocabulary?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
            } ?: emptyList()

            val grammarRules = episodePlan.grammarRules?.let { node ->
                objectMapper.convertValue(
                    node,
                    object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
            } ?: emptyList()

            val prompt = buildExercisePrompt(
                generation = generation,
                episodePlan = episodePlan,
                content = content,
                vocabulary = vocabulary,
                grammarRules = grammarRules
            )

            val response = geminiClient.generateText(prompt, "application/json")
            exercises.rawResponse = response

            val generatedExercises = parseExerciseResponse(response)

            // Calculate coverage
            val vocabularyCoverage = calculateVocabularyCoverage(generatedExercises, vocabulary)
            val grammarCoverage = grammarRules // Assume all grammar is covered if exercises generated

            exercises.exercises = objectMapper.valueToTree(generatedExercises)
            exercises.exerciseCount = generatedExercises.size
            exercises.vocabularyCoverage = objectMapper.valueToTree(vocabularyCoverage)
            exercises.grammarCoverage = objectMapper.valueToTree(grammarCoverage)
            exercises.status = GenerationStepStatus.COMPLETED

            exercisesRepository.save(exercises)

            logger.info("Generated ${generatedExercises.size} exercises for episode: ${episodePlan.title}")
            return exercises

        } catch (e: Exception) {
            logger.error("Exercise generation failed for episode ${episodePlan.title}", e)
            exercises.status = GenerationStepStatus.FAILED
            exercisesRepository.save(exercises)
            throw e
        }
    }

    private fun buildExercisePrompt(
        generation: CourseGeneration,
        episodePlan: GenerationEpisodePlan,
        content: GenerationEpisodeContent,
        vocabulary: List<String>,
        grammarRules: List<String>
    ): String {
        return """
            You are an expert language course creator. Generate exercises for a ${generation.cefrLevel} ${generation.languageCode.uppercase()} lesson.

            ## Episode Content
            ${content.content}

            ## Target Vocabulary (exercises MUST test these words)
            ${vocabulary.joinToString(", ")}

            ## Target Grammar (exercises should demonstrate these rules)
            ${grammarRules.joinToString(", ")}

            ## Exercise Requirements
            Generate exactly 13 exercises:
            - 4 MULTIPLE_CHOICE exercises
            - 4 FILL_IN_THE_BLANK exercises
            - 2 SENTENCE_SCRAMBLE exercises
            - 1 CLOZE_READING exercise
            - 2 MATCHING exercises

            ## Exercise Schemas (CRITICAL - Follow EXACTLY)

            ### 1. MULTIPLE_CHOICE
            {
              "type": "MULTIPLE_CHOICE",
              "content": {
                "question": {"content": "Question in ${generation.languageCode.uppercase()}"},
                "options": [
                  {"id": "opt1", "text": "Option A", "isCorrect": false},
                  {"id": "opt2", "text": "Option B", "isCorrect": true},
                  {"id": "opt3", "text": "Option C", "isCorrect": false},
                  {"id": "opt4", "text": "Option D", "isCorrect": false}
                ]
              }
            }

            ### 2. FILL_IN_THE_BLANK
            {
              "type": "FILL_IN_THE_BLANK",
              "content": {
                "text": "Sentence with a _____ blank.",
                "blanks": [{
                  "correctAnswer": "missing",
                  "acceptableAnswers": ["missing", "Missing"],
                  "options": ["missing", "wrong1", "wrong2", "wrong3"]
                }],
                "translation": "English translation",
                "hint": "Optional hint"
              }
            }

            ### 3. SENTENCE_SCRAMBLE
            {
              "type": "SENTENCE_SCRAMBLE",
              "content": {
                "sentence": "The correct sentence in target language",
                "translation": "English translation",
                "hint": "Optional hint"
              }
            }

            ### 4. CLOZE_READING
            {
              "type": "CLOZE_READING",
              "content": {
                "text": "A paragraph with {blank1} multiple {blank2} blanks.",
                "blanks": [
                  {"id": "blank1", "correctAnswer": "word1"},
                  {"id": "blank2", "correctAnswer": "word2"}
                ],
                "hint": "Optional hint"
              }
            }

            ### 5. MATCHING
            {
              "type": "MATCHING",
              "content": {
                "pairs": [
                  {"left": "Word in target language", "right": "Translation"},
                  {"left": "Another word", "right": "Another translation"}
                ]
              }
            }

            ## Guidelines
            1. Questions should test comprehension of the episode content
            2. Use target vocabulary words in exercises
            3. Keep difficulty appropriate for ${generation.cefrLevel} level
            4. Provide helpful hints where appropriate
            5. Make matching exercises use 4-6 pairs

            Return ONLY a JSON array of exercises (no markdown):
            [
              {"type": "MULTIPLE_CHOICE", "content": {...}},
              ...
            ]
        """.trimIndent()
    }

    private fun parseExerciseResponse(response: String): List<Map<String, Any>> {
        val json = extractJson(response)
        return try {
            objectMapper.readValue(json)
        } catch (e: Exception) {
            logger.error("Failed to parse exercise response: ${e.message}")
            emptyList()
        }
    }

    private fun calculateVocabularyCoverage(
        exercises: List<Map<String, Any>>,
        targetVocabulary: List<String>
    ): List<String> {
        val exerciseText = objectMapper.writeValueAsString(exercises).lowercase()
        return targetVocabulary.filter { word ->
            exerciseText.contains(word.lowercase())
        }
    }

    private fun extractJson(response: String): String {
        var json = response.trim()
        if (json.startsWith("```json")) {
            json = json.substring(7)
        } else if (json.startsWith("```")) {
            json = json.substring(3)
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length - 3)
        }
        return json.trim()
    }
}
