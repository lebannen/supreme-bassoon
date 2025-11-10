package com.vocabee.service

import com.vocabee.domain.model.ReadingText
import com.vocabee.domain.model.UserReadingProgress
import com.vocabee.domain.repository.ReadingTextRepository
import com.vocabee.domain.repository.UserReadingProgressRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ReadingTextService(
    private val readingTextRepository: ReadingTextRepository,
    private val progressRepository: UserReadingProgressRepository
) {
    private val logger = LoggerFactory.getLogger(ReadingTextService::class.java)

    /**
     * Get all texts with optional filtering
     */
    fun getAllTexts(
        languageCode: String?,
        level: String?,
        topic: String?,
        publishedOnly: Boolean = true
    ): List<ReadingText> {
        logger.info("Fetching texts: language=$languageCode, level=$level, topic=$topic, publishedOnly=$publishedOnly")

        return when {
            languageCode != null && level != null && topic != null ->
                readingTextRepository.findByLanguageCodeAndLevelAndTopic(languageCode, level, topic)
            languageCode != null && level != null ->
                readingTextRepository.findByLanguageCodeAndLevel(languageCode, level)
            languageCode != null && topic != null ->
                readingTextRepository.findByLanguageCodeAndTopic(languageCode, topic)
            languageCode != null -> {
                if (publishedOnly) {
                    readingTextRepository.findByLanguageCodeAndIsPublished(languageCode, true)
                } else {
                    readingTextRepository.findByLanguageCode(languageCode)
                }
            }
            else -> {
                if (publishedOnly) {
                    readingTextRepository.findByIsPublished(true)
                } else {
                    readingTextRepository.findAll()
                }
            }
        }
    }

    /**
     * Get text by ID
     */
    fun getTextById(id: Long): ReadingText? {
        return readingTextRepository.findById(id).orElse(null)
    }

    /**
     * Import/create a new reading text
     */
    @Transactional
    fun createText(
        title: String,
        content: String,
        languageCode: String,
        level: String? = null,
        topic: String? = null,
        description: String? = null,
        author: String? = null,
        source: String? = null,
        audioUrl: String? = null
    ): ReadingText {
        logger.info("Creating new reading text: $title")

        // Calculate word count
        val wordCount = content.split(Regex("\\s+")).size

        // Estimate reading time (assuming 200 words per minute)
        val estimatedMinutes = (wordCount / 200).coerceAtLeast(1)

        val text = ReadingText(
            title = title,
            content = content,
            languageCode = languageCode,
            level = level,
            topic = topic,
            wordCount = wordCount,
            description = description,
            estimatedMinutes = estimatedMinutes,
            author = author,
            source = source,
            audioUrl = audioUrl,
            isPublished = true  // Auto-publish imported texts
        )

        return readingTextRepository.save(text)
    }

    /**
     * Update audio URL for a reading text
     */
    @Transactional
    fun updateAudioUrl(textId: Long, audioUrl: String): ReadingText {
        logger.info("Updating audio URL for text $textId")

        val text = readingTextRepository.findById(textId).orElse(null)
            ?: throw IllegalArgumentException("Reading text not found with id: $textId")

        val updatedText = text.copy(audioUrl = audioUrl)
        return readingTextRepository.save(updatedText)
    }

    /**
     * Get user's progress for a specific text
     */
    fun getUserProgress(userId: Long, textId: Long): UserReadingProgress? {
        return progressRepository.findByUserIdAndTextId(userId, textId).orElse(null)
    }

    /**
     * Get all progress for a user
     */
    fun getAllUserProgress(userId: Long): List<UserReadingProgress> {
        return progressRepository.findByUserId(userId)
    }

    /**
     * Update user's reading progress
     */
    @Transactional
    fun updateProgress(
        userId: Long,
        textId: Long,
        currentPage: Int,
        totalPages: Int
    ): UserReadingProgress {
        logger.info("Updating progress for user $userId on text $textId: page $currentPage/$totalPages")

        val existing = progressRepository.findByUserIdAndTextId(userId, textId).orElse(null)

        val progress = if (existing != null) {
            // Update existing progress
            existing.copy(
                currentPage = currentPage,
                totalPages = totalPages,
                completed = currentPage >= totalPages,
                completedAt = if (currentPage >= totalPages && existing.completedAt == null)
                    LocalDateTime.now() else existing.completedAt,
                lastReadAt = LocalDateTime.now()
            )
        } else {
            // Create new progress
            UserReadingProgress(
                userId = userId,
                textId = textId,
                currentPage = currentPage,
                totalPages = totalPages,
                completed = currentPage >= totalPages,
                completedAt = if (currentPage >= totalPages) LocalDateTime.now() else null
            )
        }

        return progressRepository.save(progress)
    }

    /**
     * Mark text as completed
     */
    @Transactional
    fun markCompleted(userId: Long, textId: Long): UserReadingProgress {
        logger.info("Marking text $textId as completed for user $userId")

        val existing = progressRepository.findByUserIdAndTextId(userId, textId).orElse(null)

        val progress = if (existing != null) {
            existing.copy(
                completed = true,
                completedAt = LocalDateTime.now(),
                lastReadAt = LocalDateTime.now()
            )
        } else {
            UserReadingProgress(
                userId = userId,
                textId = textId,
                completed = true,
                completedAt = LocalDateTime.now()
            )
        }

        return progressRepository.save(progress)
    }

    /**
     * Get completed texts for a user
     */
    fun getCompletedTexts(userId: Long): List<UserReadingProgress> {
        return progressRepository.findByUserIdAndCompleted(userId, true)
    }

    /**
     * Get in-progress texts for a user
     */
    fun getInProgressTexts(userId: Long): List<UserReadingProgress> {
        return progressRepository.findByUserIdAndCompleted(userId, false)
    }
}
