package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "courses")
data class Course(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false, length = 100)
    val slug: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, length = 10)
    val languageCode: String,

    @Column(length = 10)
    val cefrLevel: String = "A1",

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    val objectives: JsonNode? = null,

    val estimatedHours: Int = 40,

    @Column(columnDefinition = "TEXT")
    val seriesContext: String? = null, // The "Bible": Characters, Setting, Backstory

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val status: CourseStatus = CourseStatus.DRAFT,

    val isPublished: Boolean = false,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: String = "system"
)

enum class CourseStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
