package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(
    name = "modules",
    uniqueConstraints = [UniqueConstraint(columnNames = ["course_id", "module_number"])]
)
data class Module(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val courseId: Long,

    @Column(nullable = false)
    val moduleNumber: Int,

    @Column(nullable = false)
    val title: String,

    val theme: String? = null,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    val objectives: JsonNode? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    val vocabularyFocus: JsonNode? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    val grammarFocus: JsonNode? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    val episodeOutline: JsonNode? = null,

    val estimatedMinutes: Int = 120,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: ModuleStatus = ModuleStatus.PLANNED,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class ModuleStatus {
    PLANNED,
    DRAFTING,
    READY,
    PUBLISHED
}
