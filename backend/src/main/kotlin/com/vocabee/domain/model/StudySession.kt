package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "study_sessions")
data class StudySession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_set_id")
    val wordSet: WordSet? = null,

    @Column(name = "started_at", nullable = false)
    val startedAt: Instant = Instant.now(),

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SessionStatus = SessionStatus.ACTIVE,

    @Column(name = "session_size", nullable = false)
    val sessionSize: Int,

    @Column(name = "total_words", nullable = false)
    val totalWords: Int,

    @Column(name = "words_completed", nullable = false)
    var wordsCompleted: Int = 0,

    @Column(name = "total_attempts", nullable = false)
    var totalAttempts: Int = 0,

    @Column(name = "correct_attempts", nullable = false)
    var correctAttempts: Int = 0,

    @Column(name = "incorrect_attempts", nullable = false)
    var incorrectAttempts: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", length = 50)
    var sessionType: SessionType? = null,

    @Column(name = "last_shown_word_id")
    var lastShownWordId: Long? = null,

    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val items: MutableList<StudySessionItem> = mutableListOf()
) {
    fun recordAnswer(correct: Boolean) {
        totalAttempts++
        if (correct) {
            correctAttempts++
        } else {
            incorrectAttempts++
        }
    }

    fun completeWord() {
        wordsCompleted++
    }

    fun complete() {
        status = SessionStatus.COMPLETED
        completedAt = Instant.now()
    }

    fun abandon() {
        status = SessionStatus.ABANDONED
        completedAt = Instant.now()
    }

    val accuracy: Double
        get() = if (totalAttempts > 0) (correctAttempts.toDouble() / totalAttempts) * 100 else 0.0
}

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    ABANDONED
}

enum class SessionType {
    WORD_SET,
    VOCABULARY,
    DUE_REVIEW
}
