package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "word_sets")
data class WordSet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "language_code", nullable = false)
    var languageCode: String,

    @Column(length = 10)
    var level: String? = null,  // A1, A2, B1, B2, C1, C2

    @Column
    var theme: String? = null,

    @Column(name = "word_count", nullable = false)
    var wordCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "wordSet", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val items: MutableList<WordSetItem> = mutableListOf()
) {
    fun addWord(word: Word, displayOrder: Int = 0) {
        val item = WordSetItem(
            wordSet = this,
            word = word,
            displayOrder = displayOrder
        )
        items.add(item)
        wordCount = items.size
        updatedAt = Instant.now()
    }
}
