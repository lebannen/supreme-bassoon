package com.vocabee.service

import com.vocabee.domain.model.ReadingText
import com.vocabee.domain.model.UserReadingProgress
import com.vocabee.domain.repository.ReadingTextRepository
import com.vocabee.domain.repository.UserReadingProgressRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import java.util.*

class ReadingTextServiceTest {

    private lateinit var readingTextRepository: ReadingTextRepository
    private lateinit var progressRepository: UserReadingProgressRepository
    private lateinit var service: ReadingTextService

    @BeforeEach
    fun setup() {
        readingTextRepository = mockk()
        progressRepository = mockk()
        service = ReadingTextService(readingTextRepository, progressRepository)
    }

    @Test
    fun `createText should calculate word count and estimated time`() {
        // Given
        val content = "This is a test content with exactly ten words here."
        val slot = slot<ReadingText>()

        every { readingTextRepository.save(capture(slot)) } answers { slot.captured }

        // When
        val result = service.createText(
            title = "Test Title",
            content = content,
            languageCode = "fr",
            level = "A1",
            topic = "test"
        )

        // Then
        assertEquals(10, result.wordCount)
        assertEquals(1, result.estimatedMinutes) // 10 words = 1 minute minimum
        assertEquals("Test Title", result.title)
        assertEquals("fr", result.languageCode)
        assertEquals("A1", result.level)
        assertTrue(result.isPublished)

        verify { readingTextRepository.save(any()) }
    }

    @Test
    fun `createText with longer content should calculate correct estimated time`() {
        // Given
        val content = "word ".repeat(500).trim() // 500 words
        val slot = slot<ReadingText>()

        every { readingTextRepository.save(capture(slot)) } answers { slot.captured }

        // When
        val result = service.createText(
            title = "Long Text",
            content = content,
            languageCode = "de"
        )

        // Then
        assertEquals(500, result.wordCount)
        assertEquals(2, result.estimatedMinutes) // 500 words / 200 = 2.5, rounds to 2
    }

    @Test
    fun `getAllTexts should filter by language code`() {
        // Given
        val frenchTexts = listOf(
            createSampleText(id = 1, languageCode = "fr"),
            createSampleText(id = 2, languageCode = "fr")
        )

        every { readingTextRepository.findByLanguageCodeAndIsPublished("fr", true) } returns frenchTexts

        // When
        val result = service.getAllTexts(languageCode = "fr", level = null, topic = null)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.languageCode == "fr" })

        verify { readingTextRepository.findByLanguageCodeAndIsPublished("fr", true) }
    }

    @Test
    fun `getAllTexts should filter by language and level`() {
        // Given
        val texts = listOf(createSampleText(id = 1, languageCode = "fr", level = "A1"))

        every { readingTextRepository.findByLanguageCodeAndLevel("fr", "A1") } returns texts

        // When
        val result = service.getAllTexts(languageCode = "fr", level = "A1", topic = null)

        // Then
        assertEquals(1, result.size)
        assertEquals("A1", result[0].level)

        verify { readingTextRepository.findByLanguageCodeAndLevel("fr", "A1") }
    }

    @Test
    fun `updateProgress should create new progress if none exists`() {
        // Given
        val userId = 1L
        val textId = 1L
        val slot = slot<UserReadingProgress>()

        every { progressRepository.findByUserIdAndTextId(userId, textId) } returns Optional.empty()
        every { progressRepository.save(capture(slot)) } answers { slot.captured }

        // When
        val result = service.updateProgress(userId, textId, currentPage = 5, totalPages = 10)

        // Then
        assertEquals(5, result.currentPage)
        assertEquals(10, result.totalPages)
        assertFalse(result.completed)
        assertNull(result.completedAt)

        verify { progressRepository.save(any()) }
    }

    @Test
    fun `updateProgress should mark as completed when reaching last page`() {
        // Given
        val userId = 1L
        val textId = 1L
        val slot = slot<UserReadingProgress>()

        every { progressRepository.findByUserIdAndTextId(userId, textId) } returns Optional.empty()
        every { progressRepository.save(capture(slot)) } answers { slot.captured }

        // When
        val result = service.updateProgress(userId, textId, currentPage = 10, totalPages = 10)

        // Then
        assertEquals(10, result.currentPage)
        assertEquals(10, result.totalPages)
        assertTrue(result.completed)
        assertNotNull(result.completedAt)
    }

    @Test
    fun `updateProgress should update existing progress`() {
        // Given
        val userId = 1L
        val textId = 1L
        val existing = UserReadingProgress(
            id = 1L,
            userId = userId,
            textId = textId,
            currentPage = 3,
            totalPages = 10
        )
        val slot = slot<UserReadingProgress>()

        every { progressRepository.findByUserIdAndTextId(userId, textId) } returns Optional.of(existing)
        every { progressRepository.save(capture(slot)) } answers { slot.captured }

        // When
        val result = service.updateProgress(userId, textId, currentPage = 7, totalPages = 10)

        // Then
        assertEquals(1L, result.id)
        assertEquals(7, result.currentPage)
        assertFalse(result.completed)
    }

    @Test
    fun `markCompleted should mark text as completed`() {
        // Given
        val userId = 1L
        val textId = 1L
        val existing = UserReadingProgress(
            id = 1L,
            userId = userId,
            textId = textId,
            currentPage = 8,
            totalPages = 10,
            completed = false
        )
        val slot = slot<UserReadingProgress>()

        every { progressRepository.findByUserIdAndTextId(userId, textId) } returns Optional.of(existing)
        every { progressRepository.save(capture(slot)) } answers { slot.captured }

        // When
        val result = service.markCompleted(userId, textId)

        // Then
        assertTrue(result.completed)
        assertNotNull(result.completedAt)
    }

    @Test
    fun `getTextById should return text when exists`() {
        // Given
        val text = createSampleText(id = 1)
        every { readingTextRepository.findById(1L) } returns Optional.of(text)

        // When
        val result = service.getTextById(1L)

        // Then
        assertNotNull(result)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `getTextById should return null when not exists`() {
        // Given
        every { readingTextRepository.findById(999L) } returns Optional.empty()

        // When
        val result = service.getTextById(999L)

        // Then
        assertNull(result)
    }

    private fun createSampleText(
        id: Long = 1L,
        title: String = "Test Title",
        languageCode: String = "fr",
        level: String = "A1",
        content: String = "Sample content"
    ): ReadingText {
        return ReadingText(
            id = id,
            title = title,
            content = content,
            languageCode = languageCode,
            level = level,
            topic = "test",
            wordCount = 2,
            isPublished = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
