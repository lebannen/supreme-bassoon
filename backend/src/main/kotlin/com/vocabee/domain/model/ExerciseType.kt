package com.vocabee.domain.model

import jakarta.persistence.*

@Entity
@Table(name = "exercise_types")
data class ExerciseType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(unique = true, nullable = false, length = 50)
    val typeKey: String,

    @Column(nullable = false, length = 100)
    val displayName: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(nullable = false, length = 50)
    val category: String,

    @Column(length = 10)
    val difficultyLevel: String? = null,

    @Column(nullable = false)
    val requiresAudio: Boolean = false,

    @Column(nullable = false)
    val requiresImages: Boolean = false,

    @Column(length = 50)
    val interactionType: String? = null
)
