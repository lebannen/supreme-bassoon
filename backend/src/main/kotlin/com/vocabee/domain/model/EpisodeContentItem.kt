package com.vocabee.domain.model

import jakarta.persistence.*

enum class ContentItemType {
    EXERCISE,
    GRAMMAR_RULE,
    VOCABULARY_LIST,
    CULTURAL_NOTE
}

@Entity
@Table(
    name = "episode_content_items",
    uniqueConstraints = [UniqueConstraint(columnNames = ["episode_id", "order_index"])]
)
data class EpisodeContentItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val episodeId: Long,

    @Column(nullable = false)
    val orderIndex: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val contentType: ContentItemType,

    @Column(nullable = false)
    val contentId: Long,

    val isRequired: Boolean = true,

    val pointsValue: Int = 0
)
