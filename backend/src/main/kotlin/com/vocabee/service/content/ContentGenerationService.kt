package com.vocabee.service.content

import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.generation.GeneratedContentItem
import com.vocabee.domain.generation.GeneratedDialogue
import com.vocabee.domain.generation.GeneratedExercise
import com.vocabee.domain.generation.GeneratedEpisodeContent
import com.vocabee.domain.generation.GeneratedModule
import com.vocabee.domain.generation.GeneratedOutline
import com.vocabee.domain.generation.GeneratedSyllabus
import com.vocabee.service.external.gemini.GeminiTextClient
import com.vocabee.web.dto.admin.generation.GenerateBatchExercisesRequest
import com.vocabee.web.dto.admin.generation.GenerateEpisodeContentRequest
import com.vocabee.web.dto.admin.generation.GenerateExerciseRequest
import com.vocabee.web.dto.admin.generation.GenerateModuleRequest
import com.vocabee.web.dto.admin.generation.GenerateOutlineRequest
import com.vocabee.web.dto.admin.generation.GenerateSyllabusRequest
import com.vocabee.web.dto.admin.generation.GenerateStructureRequest
import com.vocabee.web.dto.admin.generation.ModuleGenerationResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ContentGenerationService(
    private val geminiClient: GeminiTextClient,
    private val contentValidator: ContentValidator,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun generateModule(request: GenerateModuleRequest): ModuleGenerationResponse {
        logger.info("Generating module for theme: ${request.theme}")
        
        val prompt = buildPrompt(request)
        val jsonResponse = geminiClient.generateText(prompt)
        
        try {
            val generatedModule = objectMapper.readValue(jsonResponse, GeneratedModule::class.java)
            val errors = contentValidator.validate(generatedModule)
            
            return ModuleGenerationResponse(
                module = generatedModule,
                validationErrors = errors
            )
        } catch (e: Exception) {
            logger.error("Failed to parse generated JSON", e)
            throw RuntimeException("Failed to parse generated content: ${e.message}")
        }
    }

    fun generateDialogue(request: GenerateModuleRequest): String {
        logger.info("Generating dialogue for theme: ${request.theme}")
        val prompt = buildDialoguePrompt(request)
        // Use text/plain for dialogue to avoid JSON wrapping
        val rawText = geminiClient.generateText(prompt, "text/plain")
        
        // Post-processing: Remove quotes if Gemini included them despite instructions
        return rawText.lines().joinToString("\n") { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                val speaker = parts[0].trim()
                val text = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                "$speaker: $text"
            } else {
                line // Return as is if not in Speaker: Text format
            }
        }
    }

    fun generateStructure(request: GenerateStructureRequest): ModuleGenerationResponse {
        logger.info("Parsing structure for script length: ${request.script.length}")
        
        // Programmatic parsing instead of AI
        val lines = request.script.lines().filter { it.isNotBlank() }
        val dialogueLines = mutableListOf<com.vocabee.domain.generation.GeneratedDialogueLine>()
        val speakers = mutableSetOf<String>()
        
        val speakerRegex = Regex("^([^:]+):\\s*(.+)$")
        
        lines.forEach { line ->
            val match = speakerRegex.find(line)
            if (match != null) {
                val (speaker, text) = match.destructured
                // Remove quotes if present
                val cleanText = text.trim().removeSurrounding("\"").removeSurrounding("'")
                speakers.add(speaker.trim())
                dialogueLines.add(com.vocabee.domain.generation.GeneratedDialogueLine(speaker.trim(), cleanText))
            } else {
                // Handle narrative or malformed lines (assign to 'Narrator' or skip)
                if (line.isNotBlank()) {
                    speakers.add("Narrator")
                    dialogueLines.add(com.vocabee.domain.generation.GeneratedDialogueLine("Narrator", line.trim()))
                }
            }
        }

        // Assign default voices (round-robin from valid list)
        val validVoices = listOf("Puck", "Laomedeia", "Zephyr", "Leda", "Kore", "Erinome", "Autonoe", "Achernar", "Vindemiatrix", "Despina", "Sulafat", "Aoede", "Callirrhoe", "Rasalgethi", "Sadaltager", "Pulcherrima", "Gacrux", "Algieba", "Charon", "Enceladus", "Achird", "Umbriel", "Zubenelgenubi", "Iapetus", "Orus", "Alnilam", "Schedar", "Fenrir", "Sadachbia", "Algenib")
        val speakerMap = speakers.mapIndexed { index, name ->
            name to com.vocabee.domain.generation.GeneratedSpeaker(
                voice = validVoices[index % validVoices.size],
                description = "Detected speaker"
            )
        }.toMap()

        val generatedModule = GeneratedModule(
            title = "${request.theme} - ${request.level}",
            theme = request.theme,
            description = "Generated module for ${request.theme}",
            objectives = listOf("Learn vocabulary related to ${request.theme}"),
            episodes = listOf(
                com.vocabee.domain.generation.GeneratedEpisode(
                    title = "Main Episode",
                    type = "DIALOGUE",
                    dialogue = com.vocabee.domain.generation.GeneratedDialogue(
                        lines = dialogueLines,
                        speakers = speakerMap
                    ),
                    contentItems = emptyList()
                )
            )
        )

        return ModuleGenerationResponse(
            module = generatedModule,
            validationErrors = emptyList() // No validation needed for programmatic generation usually, or we can run validator
        )
    }

    fun generateExercise(request: GenerateExerciseRequest): GeneratedContentItem {
        logger.info("Generating exercise of type: ${request.type}")
        val prompt = buildExercisePrompt(request)
        // Use application/json for exercises
        val jsonResponse = geminiClient.generateText(prompt, "application/json")

        try {
            return objectMapper.readValue(jsonResponse, GeneratedContentItem::class.java)
        } catch (e: Exception) {
            logger.error("Failed to parse generated exercise JSON", e)
            throw RuntimeException("Failed to parse generated exercise: ${e.message}")
        }
    }

    fun generateBatchExercises(request: GenerateBatchExercisesRequest): List<GeneratedContentItem> {
        logger.info("Generating batch exercises: ${request.exerciseCounts}")
        val prompt = buildBatchExercisesPrompt(request)
        val jsonResponse = geminiClient.generateText(prompt, "application/json")

        try {
            val typeRef = object : com.fasterxml.jackson.core.type.TypeReference<List<GeneratedContentItem>>() {}
            return objectMapper.readValue(jsonResponse, typeRef)
        } catch (e: Exception) {
            logger.error("Failed to parse generated batch exercises JSON", e)
            throw RuntimeException("Failed to parse generated batch exercises: ${e.message}")
        }
    }

    private fun buildPrompt(request: GenerateModuleRequest): String {
        return """
            You are an expert language course creator. Create a JSON module for a language learning app.
            
            You are an expert language course creator. Create a JSON module for a language learning app.
            
            Target Language: ${request.targetLanguage}
            Level: ${request.level}
            Theme: "${request.theme}"
            Focus: ${request.focus ?: "General communication"}
            
            ${request.additionalInstructions?.let { "Additional Instructions: $it" } ?: ""}
            
            STRICT RULES:
            1. Output MUST be valid JSON matching the schema below.
            2. Do NOT include markdown formatting (like ```json). Just the raw JSON.
            3. Instructions in exercises MUST be language-agnostic or in French (Immersion).
            4. Use valid voices from this list: [Puck, Laomedeia, Zephyr, Leda, Kore, Erinome, Autonoe, Achernar, Vindemiatrix, Despina, Sulafat, Aoede, Callirrhoe, Rasalgethi, Sadaltager, Pulcherrima, Gacrux, Algieba, Charon, Enceladus, Achird, Umbriel, Zubenelgenubi, Iapetus, Orus, Alnilam, Schedar, Fenrir, Sadachbia, Algenib]
            
            SCHEMA:
            {
              "title": "Module Title",
              "theme": "Theme",
              "description": "Description",
              "objectives": ["Objective 1", "Objective 2"],
              "episodes": [
                {
                  "title": "Episode Title",
                  "type": "DIALOGUE" | "STORY",
                  "dialogue": {
                    "lines": [{"speaker": "Name", "text": "French text"}],
                    "speakers": {"Name": {"voice": "VoiceName", "description": "Desc"}}
                  },
                  "contentItems": [
                    {
                      "type": "EXERCISE",
                      "exercise": {
                        "type": "multiple_choice",
                        "meta": {"context": "optional context"},
                        "content": {
                          "question": {"type": "text", "content": "Question in French"},
                          "options": [{"id": "a", "text": "Option A", "isCorrect": true}]
                        }
                      }
                    }
                  ]
                }
              ]
            }
            
            Generate 2 episodes:
            1. A Dialogue related to the theme.
            2. A Story or another Dialogue.
            Include 3-4 exercises per episode.
        """.trimIndent()
    }

    private fun buildDialoguePrompt(request: GenerateModuleRequest): String {
        val typeInstruction = if (request.contentType == "STORY") "Write a short story." else "Write a dialogue."
        
        return """
            You are an expert language course creator. $typeInstruction
            
            Target Language: ${request.targetLanguage}
            Level: ${request.level}
            Theme: "${request.theme}"
            Focus: ${request.focus ?: "General communication"}
            
            ${request.additionalInstructions?.let { "Additional Instructions: $it" } ?: ""}
            
            STRICT RULES:
            1. Output ONLY the text of the dialogue/story.
            2. Do NOT include JSON or any other formatting.
            3. Use standard script format (Speaker: Line). For stories, use 'Narrator: Line'.
            4. Ensure the content is appropriate for the requested level (${request.level}).
            5. Do NOT use quotation marks around the dialogue lines.
            
            Example Output:
            Alex: Bonjour!
            Marie: Bonjour Alex, comment ça va?
            Alex: Ça va bien, merci.
        """.trimIndent()
    }

    // buildStructurePrompt is no longer needed


    private fun buildExercisePrompt(request: GenerateExerciseRequest): String {
        return """
            You are an expert language course creator. Create a single exercise based on the provided context.
            
            Target Language: ${request.targetLanguage}
            Level: ${request.level}
            Type: ${request.type}
            
            Context (Dialogue/Text):
            ${request.context}
            
            ${request.instructions?.let { "Instructions: $it" } ?: ""}
            
            STRICT RULES:
            1. Output MUST be a valid JSON object for a single 'contentItem'.
            2. Do NOT include markdown formatting.
            3. The exercise content must be relevant to the context provided.
            
            SCHEMA:
            {
              "type": "EXERCISE",
              "exercise": {
                "type": "${request.type}",
                "meta": {"context": "brief excerpt from context"},
                "content": {
                  "question": {"type": "text", "content": "Question in Target Language"},
                  "options": [{"id": "a", "text": "Option A", "isCorrect": true}]
                }
              }
            }
        """.trimIndent()
    }

    private fun buildBatchExercisesPrompt(request: GenerateBatchExercisesRequest): String {
        val countInstructions = request.exerciseCounts.entries.joinToString(", ") { "${it.value} ${it.key}" }
        
        return """
            You are an expert language course creator. Create a batch of exercises based on the provided context.
            
            Target Language: ${request.targetLanguage}
            Level: ${request.level}
            
            CONTEXT:
            ${request.context}
            
            REQUESTED EXERCISES:
            Generate exactly: $countInstructions.
            
            STRICT RULES:
            1. Output MUST be a valid JSON Array of objects matching the schema below.
            2. Do NOT include markdown formatting.
            3. Ensure questions test different skills (Vocabulary, Grammar, Comprehension).
            4. "id" should be a unique string (e.g., "ex-1", "ex-2").
            
            SCHEMAS:
            
            1. MULTIPLE_CHOICE:
            {
              "type": "EXERCISE",
              "exercise": {
                "type": "MULTIPLE_CHOICE",
                "content": {
                  "question": { "content": "Question text" },
                  "options": [
                    { "id": "opt1", "text": "Option A", "isCorrect": false },
                    { "id": "opt2", "text": "Option B", "isCorrect": true }
                  ]
                }
              }
            }
            
            2. FILL_IN_THE_BLANK:
            {
              "type": "EXERCISE",
              "exercise": {
                "type": "FILL_IN_THE_BLANK",
                "content": {
                  "question": { "content": "Sentence with a ______." },
                  "options": [
                     { "id": "ans", "text": "missing word", "isCorrect": true } 
                  ]
                }
              }
            }
            
            3. MATCHING:
            {
              "type": "EXERCISE",
              "exercise": {
                "type": "MATCHING",
                "content": {
                  "question": { "content": "Match the pairs" },
                  "pairs": [
                    { "left": "French Word", "right": "English Meaning" },
                    { "left": "Chat", "right": "Cat" }
                  ]
                }
              }
            }
            
            4. SENTENCE_SCRAMBLE:
            {
              "type": "EXERCISE",
              "exercise": {
                "type": "SENTENCE_SCRAMBLE",
                "content": {
                  "sentence": "The correct full sentence.",
                  "words": ["The", "correct", "full", "sentence"],
                  "translation": "Optional translation"
                }
              }
            }
            
            5. CLOZE_READING:
            {
              "type": "EXERCISE",
              "exercise": {
                "type": "CLOZE_READING",
                "content": {
                  "text": "This is a {blank1} with multiple {blank2}.",
                  "blanks": [
                    { "id": "blank1", "correctAnswer": "text" },
                    { "id": "blank2", "correctAnswer": "blanks" }
                  ]
                }
              }
            }
        """.trimIndent()
    }

    fun generateSyllabus(request: GenerateSyllabusRequest): GeneratedSyllabus {
        logger.info("Generating syllabus for ${request.targetLanguage} ${request.level}")
        val prompt = buildSyllabusPrompt(request)
        val response = geminiClient.generateText(prompt)
        return parseSyllabusResponse(response)
    }

    private fun buildSyllabusPrompt(request: GenerateSyllabusRequest): String {
        return """
            You are an expert language curriculum designer. Create a syllabus for a language course.
            
            Target Language: ${request.targetLanguage}
            Level: ${request.level}
            Series Context (The "Bible"): ${request.seriesContext}
            
            Generate a list of 10 modules that form a coherent narrative arc based on the Series Context.
            Each module should have a clear theme and a progression in difficulty.
            
            Return the response in strict JSON format matching this schema:
            {
              "modules": [
                {
                  "moduleNumber": 1,
                  "title": "Module Title",
                  "theme": "Main Theme (e.g. Greetings)",
                  "description": "Brief description of what happens in this module."
                }
              ]
            }
        """.trimIndent()
    }

    private fun parseSyllabusResponse(response: String): GeneratedSyllabus {
        val json = extractJson(response)
        return objectMapper.readValue(json, GeneratedSyllabus::class.java)
    }

    fun generateOutline(request: GenerateOutlineRequest): GeneratedOutline {
        logger.info("Generating outline for module: ${request.moduleTheme}")
        val prompt = buildOutlinePrompt(request)
        val response = geminiClient.generateText(prompt)
        return parseOutlineResponse(response)
    }

    private fun buildOutlinePrompt(request: GenerateOutlineRequest): String {
        return """
            You are an expert language curriculum designer. Create an outline of episodes for a specific module.
            
            Target Language: ${request.targetLanguage}
            Level: ${request.level}
            Module Theme: ${request.moduleTheme}
            Module Description: ${request.moduleDescription}
            Series Context: ${request.seriesContext ?: "N/A"}
            
            Generate a list of 3-5 episodes for this module.
            The episodes should follow a logical sequence (e.g., Introduction -> Conflict -> Resolution -> Educational Summary).
            
            Types of episodes:
            - DIALOGUE: A conversation between characters.
            - STORY: A narrative text.
            
            Return the response in strict JSON format matching this schema:
            {
              "episodes": [
                {
                  "episodeNumber": 1,
                  "title": "Episode Title",
                  "type": "DIALOGUE",
                  "summary": "Detailed summary of what happens in this episode and what is learned."
                }
              ]
            }
        """.trimIndent()
    }

    private fun parseOutlineResponse(response: String): GeneratedOutline {
        val json = extractJson(response)
        return objectMapper.readValue(json, GeneratedOutline::class.java)
    }

    private fun extractJson(response: String): String {
        // Remove markdown code blocks if present
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
    fun generateEpisodeContent(request: GenerateEpisodeContentRequest): GeneratedEpisodeContent {
        logger.info("Generating content for episode: ${request.episodeTitle} (${request.episodeType})")

        // 1. Generate Main Content (Dialogue or Story)
        val contentPrompt = buildEpisodeContentPrompt(request)
        // Use text/plain to avoid JSON issues for the creative writing part
        val rawContent = geminiClient.generateText(contentPrompt, "text/plain")
        
        // Clean up the content
        val cleanedContent = if (request.episodeType == "DIALOGUE") {
            rawContent.lines().joinToString("\n") { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val speaker = parts[0].trim()
                    val text = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                    "$speaker: $text"
                } else {
                    line
                }
            }
        } else {
            rawContent // Keep stories as is
        }

        // 2. Generate Exercises based on the content
        val exerciseRequest = GenerateBatchExercisesRequest(
            context = cleanedContent,
            targetLanguage = request.targetLanguage,
            level = request.level,
            exerciseCounts = mapOf(
                "MULTIPLE_CHOICE" to 2,
                "FILL_IN_THE_BLANK" to 2,
                "MATCHING" to 1
            )
        )
        val exercises = generateBatchExercises(exerciseRequest)

        // 3. Construct Response
        return if (request.episodeType == "DIALOGUE") {
            // Parse dialogue lines for structured storage
            val lines = cleanedContent.lines().filter { it.isNotBlank() && it.contains(":") }.map {
                val parts = it.split(":", limit = 2)
                com.vocabee.domain.generation.GeneratedDialogueLine(parts[0].trim(), parts[1].trim())
            }
            
            GeneratedEpisodeContent(
                dialogue = GeneratedDialogue(lines = lines, speakers = emptyMap()), // Speakers can be inferred or left empty for now
                exercises = exercises
            )
        } else {
            GeneratedEpisodeContent(
                story = cleanedContent,
                exercises = exercises
            )
        }
    }

    private fun buildEpisodeContentPrompt(request: GenerateEpisodeContentRequest): String {
        val typeInstruction = if (request.episodeType == "STORY") "Write a short story." else "Write a dialogue."
        
        return """
            You are an expert language course creator. $typeInstruction
            
            Target Language: ${request.targetLanguage}
            Level: ${request.level}
            Module Theme: "${request.moduleTheme}"
            Episode Title: "${request.episodeTitle}"
            Episode Summary: "${request.episodeSummary}"
            Series Context: ${request.seriesContext ?: "N/A"}
            
            STRICT RULES:
            1. Output ONLY the text of the dialogue/story.
            2. Do NOT include JSON or any other formatting.
            3. Use standard script format (Speaker: Line). For stories, use 'Narrator: Line' or just text.
            4. Ensure the content is appropriate for the requested level (${request.level}).
            5. Do NOT use quotation marks around the dialogue lines.
        """.trimIndent()
    }
}
