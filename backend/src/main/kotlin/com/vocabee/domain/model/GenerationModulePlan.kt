package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*

/**
 * Stage 2 output: Module-level plan with episode outlines.
 */
@Entity
@Table(
    name = "generation_module_plans",
    uniqueConstraints = [UniqueConstraint(columnNames = ["generation_id", "module_number"])]
)
data class GenerationModulePlan(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val generationId: UUID,

    @Column(nullable = false)
    val moduleNumber: Int,

    /** Module title */
    @Column(length = 200)
    var title: String? = null,

    /** Module theme */
    @Column(length = 200)
    var theme: String? = null,

    /** Module description */
    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    /**
     * Learning objectives for this module.
     * Format: ["Learn to introduce yourself", "Order food at a café"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    var objectives: JsonNode? = null,

    /** Plot progression summary for this module */
    @Column(columnDefinition = "TEXT")
    var plotSummary: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: GenerationStepStatus = GenerationStepStatus.PENDING,

    /** Raw AI response for debugging */
    @Column(columnDefinition = "TEXT")
    var rawResponse: String? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
