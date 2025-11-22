package com.vocabee.service.content

import com.vocabee.domain.generation.GeneratedModule
import org.springframework.stereotype.Component

@Component
class ContentValidator {

    private val validVoices = setOf(
        // Female
        "Puck", "Laomedeia", "Zephyr", "Leda", "Kore", "Erinome", "Autonoe",
        "Achernar", "Vindemiatrix", "Despina", "Sulafat", "Aoede", "Callirrhoe",
        "Rasalgethi", "Sadaltager", "Pulcherrima", "Gacrux",
        // Male
        "Algieba", "Charon", "Enceladus", "Achird", "Umbriel", "Zubenelgenubi",
        "Iapetus", "Orus", "Alnilam", "Schedar", "Fenrir", "Sadachbia", "Algenib"
    )

    fun validate(module: GeneratedModule): List<String> {
        val errors = mutableListOf<String>()

        if (module.title.isBlank()) errors.add("Module title is empty")
        if (module.episodes.isEmpty()) errors.add("Module has no episodes")

        module.episodes.forEachIndexed { index, episode ->
            if (episode.title.isBlank()) errors.add("Episode ${index + 1} title is empty")
            
            // Validate Dialogue
            if (episode.type == "DIALOGUE" || episode.type == "STORY") {
                if (episode.dialogue == null) {
                    errors.add("Episode ${index + 1} (${episode.type}) missing dialogue")
                } else {
                    episode.dialogue.speakers.forEach { (name, speaker) ->
                        if (speaker.voice !in validVoices) {
                            errors.add("Episode ${index + 1}: Invalid voice '${speaker.voice}' for speaker '$name'")
                        }
                    }
                    if (episode.dialogue.lines.isEmpty()) {
                        errors.add("Episode ${index + 1}: Dialogue has no lines")
                    }
                }
            }

            // Validate Content Items
            if (episode.contentItems.isEmpty()) {
                errors.add("Episode ${index + 1} has no exercises")
            }
        }

        return errors
    }
}
