package com.vocabee.domain.model

import jakarta.persistence.*

@Entity
@Table(name = "word_set_items")
data class WordSetItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_set_id", nullable = false)
    val wordSet: WordSet,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "word_id", nullable = false)
    val word: Word,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0
)
