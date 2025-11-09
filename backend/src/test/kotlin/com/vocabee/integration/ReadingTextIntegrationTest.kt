package com.vocabee.integration

import com.vocabee.domain.model.ReadingText
import com.vocabee.domain.model.UserReadingProgress
import com.vocabee.domain.repository.ReadingTextRepository
import com.vocabee.domain.repository.UserReadingProgressRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
class ReadingTextIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var readingTextRepository: ReadingTextRepository

    @Autowired
    private lateinit var progressRepository: UserReadingProgressRepository

    @BeforeEach
    fun setup() {
        progressRepository.deleteAll()
        readingTextRepository.deleteAll()
    }

    @Test
    fun `should save and retrieve reading text`() {
        // Given
        val text = ReadingText(
            title = "Test Text",
            content = "This is a test content for reading.",
            languageCode = "fr",
            level = "A1",
            topic = "daily_life",
            wordCount = 7,
            description = "A test description",
            isPublished = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // When
        val saved = readingTextRepository.save(text)
        val retrieved = readingTextRepository.findById(saved.id!!)

        // Then
        assertTrue(retrieved.isPresent)
        assertEquals("Test Text", retrieved.get().title)
        assertEquals("fr", retrieved.get().languageCode)
        assertEquals("A1", retrieved.get().level)
    }

    @Test
    fun `should find texts by language code`() {
        // Given
        readingTextRepository.save(createText(languageCode = "fr"))
        readingTextRepository.save(createText(languageCode = "fr"))
        readingTextRepository.save(createText(languageCode = "de"))

        // When
        val frenchTexts = readingTextRepository.findByLanguageCode("fr")
        val germanTexts = readingTextRepository.findByLanguageCode("de")

        // Then
        assertEquals(2, frenchTexts.size)
        assertEquals(1, germanTexts.size)
        assertTrue(frenchTexts.all { it.languageCode == "fr" })
    }

    @Test
    fun `should find texts by language and level`() {
        // Given
        readingTextRepository.save(createText(languageCode = "fr", level = "A1"))
        readingTextRepository.save(createText(languageCode = "fr", level = "A1"))
        readingTextRepository.save(createText(languageCode = "fr", level = "A2"))

        // When
        val a1Texts = readingTextRepository.findByLanguageCodeAndLevel("fr", "A1")
        val a2Texts = readingTextRepository.findByLanguageCodeAndLevel("fr", "A2")

        // Then
        assertEquals(2, a1Texts.size)
        assertEquals(1, a2Texts.size)
    }

    @Test
    fun `should find texts by language and topic`() {
        // Given
        readingTextRepository.save(createText(languageCode = "fr", topic = "food"))
        readingTextRepository.save(createText(languageCode = "fr", topic = "travel"))

        // When
        val foodTexts = readingTextRepository.findByLanguageCodeAndTopic("fr", "food")

        // Then
        assertEquals(1, foodTexts.size)
        assertEquals("food", foodTexts[0].topic)
    }

    @Test
    fun `should find published texts only`() {
        // Given
        readingTextRepository.save(createText(isPublished = true))
        readingTextRepository.save(createText(isPublished = true))
        readingTextRepository.save(createText(isPublished = false))

        // When
        val publishedTexts = readingTextRepository.findByIsPublished(true)
        val unpublishedTexts = readingTextRepository.findByIsPublished(false)

        // Then
        assertEquals(2, publishedTexts.size)
        assertEquals(1, unpublishedTexts.size)
    }

    @Test
    fun `should save and retrieve user reading progress`() {
        // Given
        val text = readingTextRepository.save(createText())
        val progress = UserReadingProgress(
            userId = testUser.id!!,
            textId = text.id!!,
            currentPage = 5,
            totalPages = 10,
            completed = false
        )

        // When
        progressRepository.save(progress)
        val retrieved = progressRepository.findByUserIdAndTextId(testUser.id!!, text.id!!)

        // Then
        assertTrue(retrieved.isPresent)
        assertEquals(5, retrieved.get().currentPage)
        assertEquals(10, retrieved.get().totalPages)
        assertFalse(retrieved.get().completed)
    }

    @Test
    fun `should enforce unique constraint on user-text combination`() {
        // Given
        val text = readingTextRepository.save(createText())
        val progress1 = UserReadingProgress(
            userId = testUser.id!!,
            textId = text.id!!,
            currentPage = 5,
            totalPages = 10
        )
        progressRepository.save(progress1)

        // When/Then
        assertThrows(Exception::class.java) {
            val progress2 = UserReadingProgress(
                userId = testUser.id!!,
                textId = text.id!!,
                currentPage = 7,
                totalPages = 10
            )
            progressRepository.save(progress2)
            progressRepository.flush()
        }
    }

    @Test
    fun `should find user progress by userId`() {
        // Given
        val text1 = readingTextRepository.save(createText())
        val text2 = readingTextRepository.save(createText())
        val user2 = createTestUser("user2@example.com")

        progressRepository.save(createProgress(userId = testUser.id!!, textId = text1.id!!))
        progressRepository.save(createProgress(userId = testUser.id!!, textId = text2.id!!))
        progressRepository.save(createProgress(userId = user2.id!!, textId = text1.id!!))

        // When
        val user1Progress = progressRepository.findByUserId(testUser.id!!)
        val user2Progress = progressRepository.findByUserId(user2.id!!)

        // Then
        assertEquals(2, user1Progress.size)
        assertEquals(1, user2Progress.size)
    }

    @Test
    fun `should find completed texts for user`() {
        // Given
        val text1 = readingTextRepository.save(createText())
        val text2 = readingTextRepository.save(createText())
        val text3 = readingTextRepository.save(createText())

        progressRepository.save(createProgress(userId = testUser.id!!, textId = text1.id!!, completed = true))
        progressRepository.save(createProgress(userId = testUser.id!!, textId = text2.id!!, completed = true))
        progressRepository.save(createProgress(userId = testUser.id!!, textId = text3.id!!, completed = false))

        // When
        val completed = progressRepository.findByUserIdAndCompleted(testUser.id!!, true)
        val inProgress = progressRepository.findByUserIdAndCompleted(testUser.id!!, false)

        // Then
        assertEquals(2, completed.size)
        assertEquals(1, inProgress.size)
        assertTrue(completed.all { it.completed })
        assertFalse(inProgress.any { it.completed })
    }

    private fun createText(
        title: String = "Test Text",
        languageCode: String = "fr",
        level: String = "A1",
        topic: String = "test",
        isPublished: Boolean = true
    ): ReadingText {
        return ReadingText(
            title = title,
            content = "Test content for $title",
            languageCode = languageCode,
            level = level,
            topic = topic,
            wordCount = 4,
            isPublished = isPublished,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createProgress(
        userId: Long,
        textId: Long,
        currentPage: Int = 1,
        totalPages: Int = 10,
        completed: Boolean = false
    ): UserReadingProgress {
        return UserReadingProgress(
            userId = userId,
            textId = textId,
            currentPage = currentPage,
            totalPages = totalPages,
            completed = completed,
            completedAt = if (completed) LocalDateTime.now() else null
        )
    }
}
