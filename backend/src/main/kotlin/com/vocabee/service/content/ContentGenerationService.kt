package com.vocabee.service.content

import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.generation.GeneratedContentItem
import com.vocabee.domain.generation.GeneratedModule
import com.vocabee.service.external.gemini.GeminiTextClient
import com.vocabee.web.dto.admin.generation.GenerateExerciseRequest
import com.vocabee.web.dto.admin.generation.GenerateModuleRequest
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
}
