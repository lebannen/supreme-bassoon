package com.vocabee.integration

import com.vocabee.domain.model.*
import com.vocabee.domain.repository.*
import com.vocabee.service.JwtService
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("test")
abstract class BaseIntegrationTest {

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    protected lateinit var restTemplate: TestRestTemplate

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var wordRepository: WordRepository

    @Autowired
    protected lateinit var languageRepository: LanguageRepository

    @Autowired
    protected lateinit var userVocabularyRepository: UserVocabularyRepository

    @Autowired
    protected lateinit var wordSetRepository: WordSetRepository

    @Autowired
    protected lateinit var wordSetItemRepository: WordSetItemRepository

    @Autowired
    protected lateinit var studySessionRepository: StudySessionRepository

    @Autowired
    protected lateinit var studySessionItemRepository: StudySessionItemRepository

    @Autowired
    protected lateinit var studySessionAttemptRepository: StudySessionAttemptRepository

    @Autowired
    protected lateinit var jwtService: JwtService

    protected lateinit var testUser: User
    protected lateinit var testToken: String

    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            val container = TestContainersConfig.postgresContainer
            registry.add("spring.datasource.url", container::getJdbcUrl)
            registry.add("spring.datasource.username", container::getUsername)
            registry.add("spring.datasource.password", container::getPassword)
        }
    }

    @BeforeEach
    fun baseSetup() {
        // Clean up database before each test
        studySessionAttemptRepository.deleteAll()
        studySessionItemRepository.deleteAll()
        studySessionRepository.deleteAll()
        userVocabularyRepository.deleteAll()
        wordSetItemRepository.deleteAll()
        wordSetRepository.deleteAll()
        wordRepository.deleteAll()
        userRepository.deleteAll()
        languageRepository.deleteAll()

        // Create test user
        testUser = createTestUser()
        testToken = jwtService.generateToken(testUser)
    }

    protected fun createTestUser(email: String = "test@example.com"): User {
        val user = User(
            email = email,
            passwordHash = "hashed_password",
            displayName = "Test User"
        )
        return userRepository.save(user)
    }

    protected fun createTestLanguage(code: String = "fr", name: String = "French"): Language {
        val language = Language(
            code = code,
            name = name
        )
        return languageRepository.save(language)
    }

    protected fun createTestWord(
        lemma: String,
        languageCode: String = "fr",
        partOfSpeech: String = "verb"
    ): Word {
        val word = Word(
            lemma = lemma,
            normalized = lemma.lowercase(),
            languageCode = languageCode,
            partOfSpeech = partOfSpeech
        )
        val savedWord = wordRepository.save(word)

        // Add a definition
        val definition = Definition(
            word = savedWord,
            definitionNumber = 1,
            definitionText = "Test definition for $lemma"
        )
        savedWord.definitions.add(definition)

        // Add an example
        val example = Example(
            definition = definition,
            sentenceText = "Example sentence with $lemma",
            translation = "Example translation"
        )
        definition.examples.add(example)

        return wordRepository.save(savedWord)
    }

    protected fun createTestWordSet(
        name: String,
        languageCode: String = "fr",
        words: List<Word> = emptyList()
    ): WordSet {
        val wordSet = WordSet(
            name = name,
            description = "Test word set",
            languageCode = languageCode,
            level = "A1",
            theme = "test"
        )
        val savedWordSet = wordSetRepository.save(wordSet)

        words.forEachIndexed { index, word ->
            savedWordSet.addWord(word, index)
        }

        return wordSetRepository.save(savedWordSet)
    }

    protected fun createTestUserVocabulary(
        user: User = testUser,
        word: Word,
        nextReviewAt: Instant? = null,
        consecutiveSuccesses: Int = 0
    ): UserVocabulary {
        val vocab = UserVocabulary(
            user = user,
            word = word,
            nextReviewAt = nextReviewAt,
            consecutiveSuccesses = consecutiveSuccesses
        )
        return userVocabularyRepository.save(vocab)
    }

    protected fun baseUrl(): String = "http://localhost:$port"

    protected fun <T> authenticatedGet(
        url: String,
        responseType: Class<T>,
        token: String = testToken
    ): ResponseEntity<T> {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $token")
        }
        return restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity(null, headers),
            responseType
        )
    }

    protected fun <T, R> authenticatedPost(
        url: String,
        body: T,
        responseType: Class<R>,
        token: String = testToken
    ): ResponseEntity<R> {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $token")
            set("Content-Type", "application/json")
        }
        return restTemplate.exchange(
            url,
            HttpMethod.POST,
            HttpEntity(body, headers),
            responseType
        )
    }

    protected fun <T> authenticatedDelete(
        url: String,
        responseType: Class<T>,
        token: String = testToken
    ): ResponseEntity<T> {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $token")
        }
        return restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            HttpEntity(null, headers),
            responseType
        )
    }
}
