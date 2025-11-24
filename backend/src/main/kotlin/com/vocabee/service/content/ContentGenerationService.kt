package com.vocabee.service.content

import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.generation.*
import com.vocabee.service.external.gemini.GeminiTextClient
import com.vocabee.web.dto.admin.generation.*
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
            
            SCHEMAS (CRITICAL - Follow EXACTLY):

            1. MULTIPLE_CHOICE:
            {
              "type": "EXERCISE",
              "exercise": {
                "type": "MULTIPLE_CHOICE",
                "content": {
                  "question": { "content": "Question text in ${request.targetLanguage}" },
                  "options": [
                    { "id": "opt1", "text": "Option A", "isCorrect": false },
                    { "id": "opt2", "text": "Option B", "isCorrect": true },
                    { "id": "opt3", "text": "Option C", "isCorrect": false },
                    { "id": "opt4", "text": "Option D", "isCorrect": false }
                  ]
                }
              }
            }

            2. FILL_IN_THE_BLANK (CRITICAL - Use "text" and "blanks" structure):
            {
              "type": "EXERCISE",
              "exercise": {
                "type": "FILL_IN_THE_BLANK",
                "content": {
                  "text": "Sentence with a _____ blank.",
                  "blanks": [
                    {
                      "correctAnswer": "missing",
                      "acceptableAnswers": ["missing", "Missing"],
                      "options": ["missing", "wrong1", "wrong2", "wrong3"]
                    }
                  ],
                  "translation": "Optional English translation",
                  "hint": "Optional hint"
                }
              }
            }

            3. MATCHING:
            {
              "type": "EXERCISE",
              "exercise": {
                "type": "MATCHING",
                "content": {
                  "pairs": [
                    { "left": "Bonjour", "right": "Hello" },
                    { "left": "Au revoir", "right": "Goodbye" },
                    { "left": "Merci", "right": "Thank you" },
                    { "left": "Oui", "right": "Yes" }
                  ]
                }
              }
            }

            4. SENTENCE_SCRAMBLE (SIMPLIFIED - Store only the correct sentence):
            {
              "type": "EXERCISE",
              "exercise": {
                "type": "SENTENCE_SCRAMBLE",
                "content": {
                  "sentence": "Je m'appelle Marie",
                  "translation": "My name is Marie",
                  "explanation": "Optional explanation",
                  "hint": "Optional hint"
                }
              }
            }

            NOTE FOR SENTENCE_SCRAMBLE:
            - "sentence" is the correct sentence in the target language
            - The frontend will automatically split it into words and shuffle them for the user
            - Keep sentences clear and grammatically correct
            - Example: "sentence": "La salle est très belle"

            5. CLOZE_READING:
            {
              "type": "EXERCISE",
              "exercise": {
                "type": "CLOZE_READING",
                "content": {
                  "text": "This is a {blank1} with multiple {blank2} in the text.",
                  "blanks": [
                    { "id": "blank1", "correctAnswer": "paragraph" },
                    { "id": "blank2", "correctAnswer": "blanks" }
                  ],
                  "hint": "Optional hint"
                }
              }
            }

            FINAL VALIDATION CHECKLIST:
            ✓ For SENTENCE_SCRAMBLE: "sentence" is a string containing the correct sentence
            ✓ For FILL_IN_THE_BLANK: "blanks" is an array, not nested under "question"
            ✓ For MATCHING: "pairs" is directly in "content", not nested
            ✓ All exercises have valid JSON structure
            ✓ No markdown formatting (no ```json blocks)

            Now generate the exercises as a JSON array:
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

            Generate a list of 2 modules that form a coherent narrative arc based on the Series Context.
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

            Generate a list of 2 episodes for this module.
            The episodes should follow a logical sequence (e.g., Introduction -> Practice).
            
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

    fun generateModulePlan(request: GenerateModulePlanRequest): GeneratedModulePlan {
        logger.info("Generating module plan for module ${request.moduleNumber}: ${request.moduleTitle}")
        val prompt = buildModulePlanPrompt(request)
        val response = geminiClient.generateText(prompt)
        return parseModulePlanResponse(response)
    }

    private fun buildModulePlanPrompt(request: GenerateModulePlanRequest): String {
        return """
            You are an expert language curriculum designer. Create a detailed plan for a specific module in a language course.

            Target Language: ${request.targetLanguage}
            Level: ${request.level}
            Series Context: ${request.seriesContext}

            Module Number: ${request.moduleNumber}
            Module Title: ${request.moduleTitle}
            Module Theme: ${request.moduleTheme}
            Module Description: ${request.moduleDescription}

            Generate a comprehensive module plan that includes:
            1. A detailed, multi-paragraph description of what happens in this module (story-wise)
            2. 3-5 specific learning objectives (what students will be able to do)
            3. Key vocabulary themes/words that should be covered (as an array of strings)
            4. Key grammar points that should be covered (as an array of strings)
            5. An outline of 3-5 episodes that tell the module's story

            IMPORTANT CONSTRAINTS FOR EPISODES:
            - Each episode must be either type "DIALOGUE" (conversation between characters) or "STORY" (narrative text)
            - DIALOGUE episodes: A conversation that teaches language through natural dialogue
            - STORY episodes: A narrative that advances the plot while teaching language
            - Episodes are LINEAR content (not interactive games, puzzles, or drills)
            - Exercises (multiple choice, fill-in-blank, etc.) will be generated AUTOMATICALLY from the episode content
            - Do NOT describe episodes as "puzzles", "drills", "interactive scenes", or "mini-games"

            The episodes should follow a logical narrative arc within the module's theme.

            IMPORTANT: Return ONLY valid JSON with no additional text or markdown. Follow this exact schema:

            {
              "detailedDescription": "Multi-paragraph description...",
              "objectives": [
                "Objective 1",
                "Objective 2",
                "Objective 3"
              ],
              "vocabularyFocus": [
                "Vocab theme 1",
                "Vocab theme 2",
                "Vocab theme 3"
              ],
              "grammarFocus": [
                "Grammar point 1",
                "Grammar point 2"
              ],
              "episodeOutline": [
                {
                  "episodeNumber": 1,
                  "title": "At the Café",
                  "type": "DIALOGUE",
                  "summary": "Alex meets Marie at a local café and they have a conversation about ordering food and drinks. Through their dialogue, learners are introduced to common café vocabulary and polite request forms."
                },
                {
                  "episodeNumber": 2,
                  "title": "The Discovery",
                  "type": "STORY",
                  "summary": "Alex walks through the market and discovers a clue in an old bookshop. The narrative describes the setting, Alex's thoughts, and the mysterious note he finds, introducing descriptive vocabulary and past tense."
                },
                {
                  "episodeNumber": 3,
                  "title": "Calling the Detective",
                  "type": "DIALOGUE",
                  "summary": "Alex calls his friend Sophie to discuss the clue. Their phone conversation covers making plans, expressing opinions, and discussing possibilities, teaching communication phrases and future tense."
                }
              ]
            }

            Note: vocabularyFocus and grammarFocus MUST be arrays of strings, not objects.
        """.trimIndent()
    }

    private fun parseModulePlanResponse(response: String): GeneratedModulePlan {
        val json = extractJson(response)
        try {
            return objectMapper.readValue(json, GeneratedModulePlan::class.java)
        } catch (e: Exception) {
            logger.error("Failed to parse module plan response. Raw JSON: $json")
            throw RuntimeException("Failed to parse module plan: ${e.message}. Please try regenerating.", e)
        }
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
        logger.info("Generating exercises for episode: ${request.episodeTitle}")
        val exerciseRequest = GenerateBatchExercisesRequest(
            context = cleanedContent,
            targetLanguage = request.targetLanguage,
            level = request.level,
            exerciseCounts = mapOf(
                "MULTIPLE_CHOICE" to 4,
                "FILL_IN_THE_BLANK" to 4,
                "SENTENCE_SCRAMBLE" to 2,
                "CLOZE_READING" to 1,
                "MATCHING" to 2
            )
        )
        val exercises = try {
            generateBatchExercises(exerciseRequest)
        } catch (e: Exception) {
            logger.error("Failed to generate exercises for episode ${request.episodeTitle}: ${e.message}", e)
            emptyList() // Return empty list if exercise generation fails
        }
        logger.info("Generated ${exercises.size}/13 exercises for episode: ${request.episodeTitle}")

        // 3. Generate Image Prompts
        logger.info("Generating image prompts for episode: ${request.episodeTitle}")
        val imagePrompts = try {
            generateImagePrompts(cleanedContent, request)
        } catch (e: Exception) {
            logger.error("Failed to generate image prompts for episode ${request.episodeTitle}: ${e.message}", e)
            emptyList()
        }
        logger.info("Generated ${imagePrompts.size} image prompts for episode: ${request.episodeTitle}")

        // 4. Construct Response
        return if (request.episodeType == "DIALOGUE") {
            // Parse dialogue lines for structured storage, filtering out Narrator lines
            val lines = cleanedContent.lines()
                .filter { it.isNotBlank() && it.contains(":") }
                .mapNotNull {
                    val parts = it.split(":", limit = 2)
                    val speaker = parts[0].trim()
                    // Filter out Narrator lines as a safety measure
                    if (speaker.equals("Narrator", ignoreCase = true)) {
                        logger.warn("Filtered out Narrator line in dialogue: $it")
                        null
                    } else {
                        com.vocabee.domain.generation.GeneratedDialogueLine(speaker, parts[1].trim())
                    }
                }

            // Validate speaker count for audio generation compatibility
            val uniqueSpeakers = lines.map { it.speaker }.toSet()
            if (uniqueSpeakers.size != 2) {
                logger.warn("Dialogue has ${uniqueSpeakers.size} unique speakers (expected 2): $uniqueSpeakers")
            }

            GeneratedEpisodeContent(
                dialogue = GeneratedDialogue(lines = lines, speakers = emptyMap()), // Speakers can be inferred or left empty for now
                exercises = exercises,
                imagePrompts = imagePrompts
            )
        } else {
            GeneratedEpisodeContent(
                story = cleanedContent,
                exercises = exercises,
                imagePrompts = imagePrompts
            )
        }
    }

    private fun buildEpisodeContentPrompt(request: GenerateEpisodeContentRequest): String {
        val typeSpecificRules = if (request.episodeType == "DIALOGUE") {
            """
            DIALOGUE FORMAT RULES (CRITICAL - Audio system requires exactly 2 speakers):
            1. Use EXACTLY 2 speakers throughout the entire dialogue
            2. Speaker names MUST remain consistent - do NOT change speaker names mid-dialogue
               ❌ BAD: "Voix mystérieuse" changing to "Le Conservateur"
               ✅ GOOD: Use "Le Conservateur" from the start, or use "Sam" and "Le Conservateur"
            3. NEVER use "Narrator" as a speaker - our audio system only supports 2 speaking characters
            4. To convey sound effects or actions, integrate them into the dialogue naturally:
               ❌ BAD: "Narrator: Le téléphone sonne. Dring ! Dring !"
               ✅ GOOD: "Sam: Allô ? [le téléphone a sonné]" OR just start with "Sam: Allô ?"
               ❌ BAD: "Narrator: Clic."
               ✅ GOOD: End the dialogue without narrating the hang-up
            5. Each line format: "SpeakerName: The dialogue text"
            6. Keep it conversational and natural for the language level (${request.level})
            7. Do NOT use quotation marks around the dialogue text
            8. The dialogue should be 10-15 exchanges to provide enough learning content
            """.trimIndent()
        } else {
            """
            STORY FORMAT RULES:
            1. Write a continuous narrative text in the target language
            2. Use 'Narrator: ' prefix for each paragraph or write plain paragraphs
            3. Keep the story appropriate for level ${request.level}
            4. Make it engaging and relevant to the episode theme
            5. Story should be 150-200 words
            """.trimIndent()
        }

        return """
            You are an expert language course creator for ${request.targetLanguage} at ${request.level} level.

            Target Language: ${request.targetLanguage}
            CEFR Level: ${request.level}
            Module Theme: "${request.moduleTheme}"
            Episode Title: "${request.episodeTitle}"
            Episode Summary: "${request.episodeSummary}"
            Series Context: ${request.seriesContext ?: "N/A"}

            $typeSpecificRules

            GENERAL RULES:
            1. Output ONLY the dialogue/story text - no JSON, no markdown, no extra formatting
            2. Use only vocabulary and grammar appropriate for ${request.level} level
            3. Make the content engaging and educational

            Now generate the ${request.episodeType.lowercase()} content:
        """.trimIndent()
    }

    private fun generateImagePrompts(
        content: String,
        request: GenerateEpisodeContentRequest
    ): List<com.vocabee.domain.generation.ImagePrompt> {
        val prompt = """
            Based on the following ${request.episodeType.lowercase()} content from a ${request.level} level ${request.targetLanguage} language learning episode, generate 3-4 vivid image prompts that would help visualize key scenes or moments.

            Episode Title: "${request.episodeTitle}"
            Episode Type: ${request.episodeType}
            Content:
            ```
            $content
            ```

            For each image prompt, provide:
            1. A detailed visual description suitable for AI image generation (in English)
            2. The scene context explaining what's happening in this moment of the episode

            Guidelines:
            - Focus on key moments, settings, or emotional beats from the content
            - Use descriptive, visual language (colors, lighting, composition, mood)
            - Make images culturally appropriate and educational
            - Avoid text or speech bubbles in the images
            - Each prompt should be distinct and capture a different scene/moment

            Return ONLY valid JSON in this format (no markdown, no extra text):
            {
              "imagePrompts": [
                {
                  "description": "A cozy Parisian café with outdoor seating, warm afternoon sunlight streaming through trees, bistro tables with red checkered tablecloths, the Eiffel Tower visible in the distant background",
                  "sceneContext": "The opening scene where the characters meet at the café"
                },
                {
                  "description": "Two people at a café table, one gesturing expressively while talking, coffee cups and pastries on the table, friendly animated conversation, urban Parisian street setting",
                  "sceneContext": "The main dialogue between the characters discussing their day"
                }
              ]
            }
        """.trimIndent()

        val response = geminiClient.generateText(prompt, "application/json")
        val json = extractJson(response)

        return try {
            val jsonNode = objectMapper.readTree(json)
            val promptsNode = jsonNode.get("imagePrompts")

            if (promptsNode == null || !promptsNode.isArray) {
                logger.warn("Invalid image prompts response format, returning empty list")
                return emptyList()
            }

            promptsNode.map { node ->
                com.vocabee.domain.generation.ImagePrompt(
                    description = node.get("description")?.asText() ?: "",
                    sceneContext = node.get("sceneContext")?.asText() ?: ""
                )
            }.filter { it.description.isNotBlank() }
        } catch (e: Exception) {
            logger.error("Failed to parse image prompts response: ${e.message}", e)
            emptyList()
        }
    }
}
