package com.vocabee.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class ParsedReadingText(
    val title: String,
    val content: String,
    val wordCount: Int,
    val estimatedMinutes: Int
)

@Service
class MarkdownParsingService {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Parse a markdown or text file and extract metadata
     */
    fun parseMarkdownText(rawContent: String): ParsedReadingText {
        val lines = rawContent.lines()

        // Extract title from first # heading
        val title = extractTitle(lines)

        // Clean content: remove title heading, trim empty lines
        val content = cleanContent(rawContent, title)

        // Calculate word count
        val wordCount = calculateWordCount(content)

        // Estimate reading time (assuming 200 words per minute for A1-A2 learners)
        val estimatedMinutes = maxOf(1, (wordCount / 200.0).toInt())

        logger.info("Parsed text: title='$title', wordCount=$wordCount, estimatedMinutes=$estimatedMinutes")

        return ParsedReadingText(
            title = title,
            content = content.trim(),
            wordCount = wordCount,
            estimatedMinutes = estimatedMinutes
        )
    }

    /**
     * Extract title from markdown heading or use first line
     */
    private fun extractTitle(lines: List<String>): String {
        // Find first # heading
        val headingLine = lines.firstOrNull { it.trim().startsWith("#") }

        return if (headingLine != null) {
            // Remove # symbols and trim
            headingLine.trim().removePrefix("#").trim()
        } else {
            // Use first non-empty line as title
            lines.firstOrNull { it.isNotBlank() }?.take(100) ?: "Untitled"
        }
    }

    /**
     * Clean content by removing title heading and extra whitespace
     */
    private fun cleanContent(rawContent: String, title: String): String {
        var content = rawContent

        // Remove the title heading if it exists
        val titlePattern = Regex("^#\\s+.*?$", RegexOption.MULTILINE)
        val firstHeadingMatch = titlePattern.find(content)

        if (firstHeadingMatch != null) {
            content = content.replaceFirst(firstHeadingMatch.value, "")
        }

        // Remove horizontal rules (---)
        content = content.replace(Regex("^---+$", RegexOption.MULTILINE), "\n")

        // Normalize multiple blank lines to single blank lines
        content = content.replace(Regex("\n{3,}"), "\n\n")

        return content.trim()
    }

    /**
     * Calculate word count from text content
     */
    private fun calculateWordCount(content: String): Int {
        // Split by whitespace and count non-empty tokens
        return content.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .size
    }
}
