package com.vocabee.service.generation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vocabee.domain.model.*
import com.vocabee.domain.repository.*
import com.vocabee.service.external.gemini.GeminiTextClient
import com.vocabee.web.dto.admin.generation.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Stage 1: Blueprint Generation
 * Generates course structure, plot, characters, and grammar distribution.
 */
@Service
class BlueprintGenerationService(
    private val geminiClient: GeminiTextClient,
    private val blueprintRepository: GenerationBlueprintRepository,
    private val characterRepository: GenerationCharacterRepository,
    private val grammarRuleRepository: GrammarRuleRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun generate(
        generation: CourseGeneration,
        blueprint: GenerationBlueprint,
        feedback: String? = null
    ) {
        logger.info("Generating blueprint for ${generation.languageCode} ${generation.cefrLevel}")

        blueprint.status = GenerationStepStatus.IN_PROGRESS
        blueprintRepository.save(blueprint)

        try {
            // Check for existing grammar rules
            val existingRules = grammarRuleRepository.findByLanguageCodeAndCefrLevel(
                generation.languageCode,
                generation.cefrLevel
            )

            val prompt = buildPrompt(generation, existingRules, feedback)
            val response = geminiClient.generateText(prompt, "application/json")

            blueprint.rawResponse = response

            val result = parseResponse(response)

            // Save grammar rules if new ones were generated
            result.grammarTaxonomy?.forEach { rule ->
                if (!grammarRuleRepository.existsByLanguageCodeAndSlug(generation.languageCode, rule.slug)) {
                    val grammarRule = GrammarRule(
                        languageCode = generation.languageCode,
                        cefrLevel = generation.cefrLevel,
                        slug = rule.slug,
                        name = rule.name,
                        category = rule.category,
                        description = rule.description,
                        examples = rule.examples?.let { examples ->
                            objectMapper.valueToTree(examples)
                        }
                    )
                    grammarRuleRepository.save(grammarRule)
                    logger.info("Saved new grammar rule: ${rule.slug}")
                }
            }

            // Save characters
            result.characters.forEach { characterSeed ->
                val character = GenerationCharacter(
                    generationId = generation.id,
                    name = characterSeed.name,
                    role = characterSeed.role,
                    gender = characterSeed.gender.ifBlank { null },
                    initialDescription = "${characterSeed.name} is a ${characterSeed.gender.lowercase()} in their ${characterSeed.age}. ${characterSeed.background}",
                    ageRange = characterSeed.age,
                    personalityTraits = objectMapper.valueToTree(characterSeed.personality),
                    background = characterSeed.background
                )
                characterRepository.save(character)
                logger.info("Saved character: ${characterSeed.name} (${characterSeed.gender})")
            }

            // Update blueprint
            blueprint.courseTitle = result.courseTitle
            blueprint.courseDescription = result.courseDescription
            blueprint.setting = result.setting
            blueprint.premise = result.premise
            blueprint.plotArc = objectMapper.valueToTree(result.plotArc)
            blueprint.moduleTopics = objectMapper.valueToTree(result.moduleTopics)
            blueprint.grammarDistribution = objectMapper.valueToTree(result.grammarDistribution)
            blueprint.status = GenerationStepStatus.COMPLETED

            blueprintRepository.save(blueprint)
            logger.info("Blueprint generation completed: ${result.courseTitle}")

        } catch (e: Exception) {
            logger.error("Blueprint generation failed", e)
            blueprint.status = GenerationStepStatus.FAILED
            blueprintRepository.save(blueprint)
            throw e
        }
    }

    private fun buildPrompt(
        generation: CourseGeneration,
        existingRules: List<GrammarRule>,
        feedback: String?
    ): String {
        val grammarSection = if (existingRules.isEmpty()) {
            """
            ## Grammar Taxonomy Generation
            Generate a comprehensive grammar taxonomy for ${generation.cefrLevel} level ${generation.languageCode}.
            Include 10-15 grammar rules appropriate for this level.
            Each rule needs:
            - slug: URL-friendly identifier (e.g., "present-er-verbs")
            - name: Human-readable name (e.g., "Present tense: -er verbs")
            - category: Category (VERBS, ARTICLES, PRONOUNS, ADJECTIVES, PREPOSITIONS, etc.)
            - description: Clear explanation
            - examples: Array of {target, native} pairs
            """
        } else {
            """
            ## Grammar Rules (Use existing)
            Use these existing grammar rules for the course:
            ${existingRules.joinToString("\n") { "- ${it.slug}: ${it.name} (${it.category})" }}

            Select appropriate rules for each module from this list.
            Do NOT include grammarTaxonomy in your response (it will be null).
            """
        }

        val feedbackSection = feedback?.let {
            """
            ## Feedback from previous attempt
            Please incorporate this feedback: $it
            """
        } ?: ""

        return """
            You are an expert language curriculum designer. Create a comprehensive blueprint for a language learning course.

            ## Course Parameters
            - Target Language: ${generation.languageCode.uppercase()}
            - CEFR Level: ${generation.cefrLevel}
            - Number of Modules: ${generation.moduleCount}
            - Episodes per Module: ${generation.episodesPerModule}
            ${generation.themePreferences?.let { "- Theme Preferences: $it" } ?: ""}

            $feedbackSection

            ## Requirements

            ### 1. Course Overview
            Create an engaging course title and description that reflects the theme and level.

            ### 2. Setting & Premise
            Design a compelling story setting and premise that will span all modules.
            The premise should:
            - Be appropriate for ${generation.cefrLevel} level learners
            - Provide opportunities for natural language use
            - Be engaging and culturally authentic

            ### 3. Plot Arc
            Define a plot arc point for each module that shows story progression.
            The arc should have a clear beginning, development, and resolution.

            ### 4. Module Topics
            Assign a clear learning topic to each module (e.g., "Greetings & Introductions", "At the Café", "Daily Routine").
            Topics should:
            - Progress logically in difficulty
            - Fit naturally into the story
            - Cover essential vocabulary and situations for ${generation.cefrLevel}

            ### 5. Characters
            Create 2-4 main characters who will appear throughout the course.
            Each character needs:
            - name: A culturally appropriate name
            - role: PROTAGONIST, SUPPORTING, MINOR, or RECURRING
            - gender: MALE or FEMALE (be explicit about gender for consistent visuals)
            - age: Age description (e.g., "mid-20s", "teenage", "elderly")
            - personality: Array of 3-5 personality traits
            - background: Brief background description (include physical appearance details)

            $grammarSection

            ### 6. Grammar Distribution
            Assign 2-3 grammar rules to each module.
            Ensure logical progression (simpler rules first).

            ## Output Format
            Return ONLY valid JSON matching this exact schema (no markdown, no extra text):

            {
              "courseTitle": "Engaging Course Title",
              "courseDescription": "2-3 sentence description of the course",
              "setting": "Detailed description of the story setting",
              "premise": "The story premise that spans the course",
              "plotArc": [
                {"module": 1, "arcPoint": "Introduction - Setting the scene"},
                {"module": 2, "arcPoint": "Development - Challenges arise"}
              ],
              "moduleTopics": [
                {"module": 1, "topic": "Greetings & First Meetings"},
                {"module": 2, "topic": "At the Café"}
              ],
              "characters": [
                {
                  "name": "Marie",
                  "role": "PROTAGONIST",
                  "gender": "FEMALE",
                  "age": "mid-20s",
                  "personality": ["friendly", "curious", "determined"],
                  "background": "A café owner in Paris who loves helping newcomers. She has long brown hair, warm brown eyes, and a welcoming smile."
                }
              ],
              ${
            if (existingRules.isEmpty()) """
              "grammarTaxonomy": [
                {
                  "slug": "present-er-verbs",
                  "name": "Present tense: -er verbs",
                  "category": "VERBS",
                  "description": "How to conjugate regular -er verbs in present tense",
                  "examples": [
                    {"target": "Je parle français", "native": "I speak French"}
                  ]
                }
              ],""" else "\"grammarTaxonomy\": null,"
        }
              "grammarDistribution": [
                {"module": 1, "rules": ["present-er-verbs", "definite-articles"]},
                {"module": 2, "rules": ["present-ir-verbs", "indefinite-articles"]}
              ]
            }

            Generate the blueprint now:
        """.trimIndent()
    }

    private fun parseResponse(response: String): BlueprintGenerationResult {
        val json = extractJson(response)
        return try {
            objectMapper.readValue(json)
        } catch (e: Exception) {
            logger.error("Failed to parse blueprint response. Raw: $json", e)
            throw RuntimeException("Failed to parse blueprint: ${e.message}")
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
