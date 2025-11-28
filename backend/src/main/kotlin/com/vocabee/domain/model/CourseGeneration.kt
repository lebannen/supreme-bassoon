package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Tracks the state of a course generation workflow.
 * Each generation goes through multiple stages: BLUEPRINT -> MODULE_PLANNING ->
 * EPISODE_CONTENT -> VOCABULARY_LINKING -> CHARACTER_PROFILES -> EXERCISES -> MEDIA -> COMPLETED
 */
@Entity
@Table(name = "course_generations")
data class CourseGeneration(
    @Id
    val id: UUID = UUID.randomUUID(),

    /** Link to the final course (populated when published) */
    @Column(name = "course_id")
    var courseId: Long? = null,

    /** Target language for the course */
    @Column(nullable = false, length = 10)
    val languageCode: String,

    /** CEFR level (A1, A2, B1, etc.) */
    @Column(nullable = false, length = 10)
    val cefrLevel: String,

    /** Number of modules to generate */
    @Column(nullable = false)
    val moduleCount: Int,

    /** Episodes per module */
    @Column(nullable = false)
    val episodesPerModule: Int = 2,

    /** Optional theme/setting preferences from user */
    @Column(columnDefinition = "TEXT")
    val themePreferences: String? = null,

    /** When true, automatically proceed through all stages without manual approval */
    @Column(nullable = false)
    val autoMode: Boolean = false,

    /** Current stage in the generation pipeline */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var currentStage: GenerationStage = GenerationStage.BLUEPRINT,

    /** Error message if generation failed */
    @Column(columnDefinition = "TEXT")
    var errorMessage: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var completedAt: LocalDateTime? = null
)

/**
 * Stages in the course generation pipeline
 */
enum class GenerationStage {
    BLUEPRINT,
    MODULE_PLANNING,
    EPISODE_CONTENT,
    VOCABULARY_LINKING,
    CHARACTER_PROFILES,
    EXERCISES,
    MEDIA,
    COMPLETED,
    FAILED
}

/**
 * Status for individual generation steps
 */
enum class GenerationStepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
