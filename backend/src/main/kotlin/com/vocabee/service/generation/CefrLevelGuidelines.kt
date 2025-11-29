package com.vocabee.service.generation

/**
 * CEFR Level Guidelines for content generation.
 * Provides specific, actionable constraints for each proficiency level.
 */
object CefrLevelGuidelines {

    /**
     * Language-agnostic functional progression for A1 level.
     * Defines what learners can DO at each stage, not specific grammar.
     * The AI determines appropriate grammar/vocabulary for the target language.
     */
    private val a1ModuleFunctions = mapOf(
        1 to ModuleFunctions(
            name = "First Contact",
            functions = listOf(
                "Greet and respond to greetings",
                "Introduce oneself (name only)",
                "Basic politeness expressions (please, thank you, excuse me)"
            ),
            restrictions = listOf(
                "NO describing people or objects yet",
                "NO questions beyond 'What is your name?'",
                "NO colors, sizes, or adjectives"
            )
        ),
        2 to ModuleFunctions(
            name = "Describing People",
            functions = listOf(
                "Describe people using basic adjectives (tall, short, young, old)",
                "Express simple states (tired, happy, sad, ready)",
                "Identify people (this is..., that is...)"
            ),
            restrictions = listOf(
                "NO colors yet",
                "NO describing objects",
                "NO locations or places"
            )
        ),
        3 to ModuleFunctions(
            name = "Describing Objects",
            functions = listOf(
                "Describe object size (big, small)",
                "Describe object colors",
                "Express simple possession (my, your, his/her)"
            ),
            restrictions = listOf(
                "NO locations yet",
                "NO actions beyond 'have' and 'be'",
                "NO time expressions"
            )
        ),
        4 to ModuleFunctions(
            name = "Locations & Places",
            functions = listOf(
                "Express location (here, there)",
                "Name common places (home, office, street, shop, school)",
                "Use basic prepositions (in, on, at, near)"
            ),
            restrictions = listOf(
                "NO movement verbs yet (go, come)",
                "NO time expressions",
                "NO complex directions"
            )
        ),
        5 to ModuleFunctions(
            name = "Wants & Needs",
            functions = listOf(
                "Express wanting something",
                "Express needing something",
                "Make simple requests",
                "Express basic likes/dislikes"
            ),
            restrictions = listOf(
                "NO future tense",
                "NO conditional",
                "NO complex reasoning (because...)"
            )
        ),
        6 to ModuleFunctions(
            name = "Daily Actions",
            functions = listOf(
                "Describe common actions (go, eat, drink, see, hear, take)",
                "Use basic time expressions (today, now, morning, afternoon, evening)",
                "Describe simple daily routines"
            ),
            restrictions = listOf(
                "NO past tense yet",
                "NO future tense",
                "Keep to present/habitual actions"
            )
        ),
        7 to ModuleFunctions(
            name = "Simple Questions",
            functions = listOf(
                "Ask and answer yes/no questions",
                "Ask 'what' questions",
                "Ask 'where' questions",
                "Ask 'who' questions"
            ),
            restrictions = listOf(
                "NO 'why' questions (requires complex answers)",
                "NO 'how' questions beyond 'how are you'",
                "NO embedded questions"
            )
        ),
        8 to ModuleFunctions(
            name = "Numbers & Quantities",
            functions = listOf(
                "Use numbers 1-20",
                "Express quantities (some, many, few)",
                "Ask 'how many'",
                "Simple prices and counting"
            ),
            restrictions = listOf(
                "NO complex math",
                "NO time telling (hours/minutes)",
                "NO dates"
            )
        )
    )

    data class ModuleFunctions(
        val name: String,
        val functions: List<String>,
        val restrictions: List<String>
    )

    data class LevelGuidelines(
        val level: String,
        val vocabularyGuidelines: String,
        val grammarGuidelines: String,
        val sentenceGuidelines: String,
        val topicGuidelines: String,
        val contentGuidelines: String
    )

    private val guidelines = mapOf(
        "A1" to LevelGuidelines(
            level = "A1",
            vocabularyGuidelines = """
                - Use ONLY the 300-500 most common words in the target language
                - Stick to concrete, everyday nouns: food, family, objects, places
                - Basic verbs only: être/avoir/aller/faire/vouloir/pouvoir (or equivalents)
                - Simple adjectives: grand, petit, bon, mauvais, nouveau, vieux
                - Numbers 1-100, days, months, colors
                - NO abstract vocabulary (mystery, intrigue, enigma, etc.)
                - NO idiomatic expressions or figurative language
            """.trimIndent(),
            grammarGuidelines = """
                - Present tense ONLY (90% of content)
                - Minimal passé composé for simple past actions (10%)
                - NO imparfait, NO subjunctive, NO conditional
                - NO relative clauses (qui/que as relative pronouns)
                - Simple negation only (ne...pas)
                - Basic question forms (est-ce que, inversion for common phrases only)
                - Definite/indefinite articles only
                - NO complex prepositions
            """.trimIndent(),
            sentenceGuidelines = """
                - Maximum 6-8 words per sentence
                - Simple Subject-Verb-Object structure
                - One clause per sentence - NO compound sentences
                - Connectors limited to: et, mais, ou, parce que
                - NO subordinate clauses
                - Repeat key vocabulary for reinforcement
            """.trimIndent(),
            topicGuidelines = """
                - Personal information (name, age, nationality)
                - Family and friends (basic descriptions)
                - Daily routine (simple actions)
                - Food and drink (ordering, preferences)
                - Shopping (prices, basic transactions)
                - Directions (simple: left, right, straight)
                - Weather (basic descriptions)
                - NO abstract discussions, opinions, or complex narratives
            """.trimIndent(),
            contentGuidelines = """
                - Dialogues: 8-12 short exchanges maximum
                - Each line: 4-8 words
                - Heavy repetition of target vocabulary
                - Predictable, simple situations
                - Clear context clues for new words
                - Stories should be linear, simple sequences of events
            """.trimIndent()
        ),

        "A2" to LevelGuidelines(
            level = "A2",
            vocabularyGuidelines = """
                - Use the 1000-1500 most common words
                - Expand to more specific nouns and verbs
                - Include common phrasal expressions
                - Simple opinion words: je pense, je crois, c'est bien
                - Time expressions: hier, demain, la semaine dernière
                - Avoid highly specialized or technical vocabulary
                - Limited idiomatic expressions (only very common ones)
            """.trimIndent(),
            grammarGuidelines = """
                - Present tense (60%)
                - Passé composé (25%)
                - Simple future with aller + infinitive (10%)
                - Imparfait for descriptions only (5%)
                - NO subjunctive, NO conditional
                - Simple relative clauses with qui/que
                - Comparative forms (plus...que, moins...que)
                - Object pronouns (le, la, les, lui, leur)
            """.trimIndent(),
            sentenceGuidelines = """
                - Maximum 10-12 words per sentence
                - Can combine 2 simple clauses with et, mais, ou, donc
                - One relative clause per sentence maximum
                - Connectors: aussi, alors, ensuite, après, avant
                - Some variation in sentence structure
            """.trimIndent(),
            topicGuidelines = """
                - Travel and transportation
                - Past experiences (simple narratives)
                - Future plans (simple)
                - Hobbies and interests
                - Health and body (basic)
                - Work and studies (basic descriptions)
                - Comparisons between things
                - Simple opinions and preferences
            """.trimIndent(),
            contentGuidelines = """
                - Dialogues: 12-16 exchanges
                - Each line: 6-12 words
                - Introduce some narrative complexity
                - Past and future references
                - Simple problem-solving scenarios
            """.trimIndent()
        ),

        "B1" to LevelGuidelines(
            level = "B1",
            vocabularyGuidelines = """
                - Use 2000-3000 word vocabulary
                - Include abstract nouns (idea, problem, solution)
                - Emotional vocabulary
                - Discourse markers (cependant, néanmoins, en effet)
                - Some idiomatic expressions
                - Topic-specific vocabulary for common themes
            """.trimIndent(),
            grammarGuidelines = """
                - All indicative tenses including imparfait
                - Conditional for politeness and hypotheticals
                - Subjunctive for common expressions (il faut que, je veux que)
                - Passive voice (simple forms)
                - Complex relative clauses (dont, où, lequel)
                - Reported speech
            """.trimIndent(),
            sentenceGuidelines = """
                - Up to 15-18 words per sentence
                - Multiple clauses allowed
                - Varied sentence structures
                - Complex connectors: bien que, puisque, afin de
                - Paragraph-level coherence
            """.trimIndent(),
            topicGuidelines = """
                - Current events (simplified)
                - Personal opinions with justification
                - Dreams and ambitions
                - Cultural topics
                - Abstract concepts (friendship, success)
                - Hypothetical situations
            """.trimIndent(),
            contentGuidelines = """
                - Dialogues: 16-20 exchanges
                - Varied line lengths
                - Complex narratives with subplots
                - Character development
                - Multiple perspectives
            """.trimIndent()
        ),

        "B2" to LevelGuidelines(
            level = "B2",
            vocabularyGuidelines = """
                - 4000-5000 word vocabulary
                - Abstract and nuanced vocabulary
                - Idiomatic expressions freely used
                - Register variation (formal/informal)
                - Specialized vocabulary for topics
                - Collocations and fixed expressions
            """.trimIndent(),
            grammarGuidelines = """
                - Full range of tenses and moods
                - Subjunctive in all common contexts
                - Complex passive constructions
                - Nominalization
                - Nuanced use of aspect
                - Stylistic variation
            """.trimIndent(),
            sentenceGuidelines = """
                - No strict word limit
                - Complex, varied syntax
                - Emphasis and focus structures
                - Sophisticated connectors
                - Rhetorical devices
            """.trimIndent(),
            topicGuidelines = """
                - Social issues
                - Professional contexts
                - Cultural analysis
                - Abstract debates
                - Complex narratives
                - Nuanced opinions
            """.trimIndent(),
            contentGuidelines = """
                - Natural, authentic dialogues
                - Literary-quality narratives
                - Implicit meaning and subtext
                - Humor and irony
                - Cultural references
            """.trimIndent()
        )
    )

    /**
     * Get comprehensive guidelines for a CEFR level.
     */
    fun getGuidelines(level: String): LevelGuidelines {
        return guidelines[level.uppercase()]
            ?: guidelines["A1"]!! // Default to A1 if unknown level
    }

    /**
     * Get a formatted prompt section with all guidelines for content generation.
     */
    fun getContentPromptSection(level: String): String {
        val g = getGuidelines(level)
        return """
            ## CRITICAL: ${g.level} Level Language Constraints

            You MUST strictly follow these ${g.level} level requirements. Content that exceeds this level is UNACCEPTABLE.

            ### Vocabulary Rules (${g.level})
            ${g.vocabularyGuidelines}

            ### Grammar Rules (${g.level})
            ${g.grammarGuidelines}

            ### Sentence Structure Rules (${g.level})
            ${g.sentenceGuidelines}

            ### Content Guidelines (${g.level})
            ${g.contentGuidelines}

            IMPORTANT: Before finalizing, review every sentence to ensure it meets ${g.level} constraints.
            If a sentence is too complex, simplify it. If vocabulary is too advanced, replace it.
        """.trimIndent()
    }

    /**
     * Get a formatted prompt section for curriculum/blueprint generation.
     */
    fun getCurriculumPromptSection(level: String): String {
        val g = getGuidelines(level)
        return """
            ## ${g.level} Level Curriculum Constraints

            This course is for ${g.level} level learners. All content must be appropriate for this level.

            ### Suitable Topics for ${g.level}
            ${g.topicGuidelines}

            ### Vocabulary Scope for ${g.level}
            ${g.vocabularyGuidelines}

            ### Grammar Scope for ${g.level}
            ${g.grammarGuidelines}

            Ensure all module topics, vocabulary lists, and grammar points align with ${g.level} requirements.
        """.trimIndent()
    }

    /**
     * Get vocabulary-specific guidelines for module planning.
     */
    fun getVocabularyPromptSection(level: String): String {
        val g = getGuidelines(level)
        return """
            ## ${g.level} Vocabulary Selection Rules

            ${g.vocabularyGuidelines}

            When selecting vocabulary for episodes:
            - Every word MUST be verifiable as ${g.level}-appropriate
            - Prefer high-frequency words over rare synonyms
            - Include only words that fit naturally in the scene
            - Avoid words that require complex explanations
        """.trimIndent()
    }

    /**
     * Get functional progression constraints for a specific module.
     * This defines what language functions are allowed/forbidden based on module number.
     * Language-agnostic: AI determines appropriate grammar/vocabulary for the target language.
     */
    fun getModuleFunctionalConstraints(level: String, moduleNumber: Int, totalModules: Int): String {
        if (level.uppercase() != "A1") {
            // For now, only A1 has detailed functional progression
            // Other levels can be added later
            return ""
        }

        // Map the module number to our 8-module progression
        // If course has different number of modules, we scale accordingly
        val mappedModule = if (totalModules == 8) {
            moduleNumber
        } else {
            // Scale to 8-module progression
            ((moduleNumber - 1) * 8 / totalModules) + 1
        }.coerceIn(1, 8)

        val currentFunctions = a1ModuleFunctions[mappedModule]
            ?: return ""

        // Collect all functions from previous modules (cumulative)
        val previousFunctions = (1 until mappedModule).mapNotNull { a1ModuleFunctions[it] }
            .flatMap { it.functions }

        return """
            ## CRITICAL: Functional Progression Constraints (Module $moduleNumber)

            ### What Learners Can Do in This Module
            Primary focus for this module:
            ${currentFunctions.functions.joinToString("\n") { "- $it" }}

            ${
            if (previousFunctions.isNotEmpty()) """
            Functions from previous modules (can be recycled):
            ${previousFunctions.joinToString("\n") { "- $it" }}
            """ else ""
        }

            ### RESTRICTIONS for This Module
            ${currentFunctions.restrictions.joinToString("\n") { "- $it" }}

            ### How to Apply These Constraints
            1. The STORY must work within these functional limits
            2. Choose vocabulary that serves ONLY the allowed functions
            3. The AI must determine appropriate grammar for the target language to express these functions
            4. If a scene requires a forbidden function, REWRITE the scene
            5. Repetition of allowed functions is GOOD - it reinforces learning

            IMPORTANT: Do NOT introduce language functions from later modules, even if the story would be more interesting.
            Pedagogical progression is MORE important than narrative complexity at A1 level.
        """.trimIndent()
    }
}
