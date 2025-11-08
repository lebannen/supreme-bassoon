package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "study_session_items")
data class StudySessionItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: StudySession,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "word_id", nullable = false)
    val word: Word,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_vocabulary_id")
    val userVocabulary: UserVocabulary? = null,

    @Column(nullable = false)
    var attemptsCount: Int = 0,

    @Column(name = "correct_count", nullable = false)
    var correctCount: Int = 0,

    @Column(name = "incorrect_count", nullable = false)
    var incorrectCount: Int = 0,

    @Column(name = "consecutive_correct", nullable = false)
    var consecutiveCorrect: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ItemStatus = ItemStatus.NEW,

    @Column(name = "last_shown_at")
    var lastShownAt: Instant? = null,

    @Column(name = "display_order")
    var displayOrder: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "final_result", length = 20)
    var finalResult: ItemResult? = null,

    @OneToMany(mappedBy = "sessionItem", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val attemptsList: MutableList<StudySessionAttempt> = mutableListOf()
) {
    fun recordAnswer(correct: Boolean) {
        attemptsCount++

        if (correct) {
            correctCount++
            consecutiveCorrect++

            // Check if item is completed (2 consecutive correct)
            if (consecutiveCorrect >= 2 && status != ItemStatus.COMPLETED) {
                status = ItemStatus.COMPLETED
                finalResult = ItemResult.SUCCESS
            } else if (status == ItemStatus.NEW) {
                status = ItemStatus.LEARNING
            }
        } else {
            incorrectCount++
            consecutiveCorrect = 0
            status = ItemStatus.LEARNING
        }

        lastShownAt = Instant.now()
    }

    fun skip() {
        finalResult = ItemResult.SKIPPED
    }

    val isCompleted: Boolean
        get() = status == ItemStatus.COMPLETED
}

enum class ItemStatus {
    NEW,
    LEARNING,
    COMPLETED
}

enum class ItemResult {
    SUCCESS,
    FAILED,
    SKIPPED
}
