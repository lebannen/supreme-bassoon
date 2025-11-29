package com.vocabee.domain.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "course_reviews")
data class CourseReview(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "course_id", nullable = false)
    val courseId: Long,

    @Column(name = "review_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val reviewType: ReviewType = ReviewType.FULL_REVIEW,

    @Column(name = "model_used")
    val modelUsed: String? = null,

    @Column(name = "overall_score")
    val overallScore: Int? = null,

    @Column(name = "cefr_alignment_score")
    val cefrAlignmentScore: Int? = null,

    @Column(name = "structure_score")
    val structureScore: Int? = null,

    @Column(name = "content_quality_score")
    val contentQualityScore: Int? = null,

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    val summary: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths", columnDefinition = "jsonb")
    val strengths: JsonNode? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weaknesses", columnDefinition = "jsonb")
    val weaknesses: JsonNode? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendations", columnDefinition = "jsonb")
    val recommendations: JsonNode? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "module_feedback", columnDefinition = "jsonb")
    val moduleFeedback: JsonNode? = null,

    @Column(name = "detailed_analysis", columnDefinition = "TEXT")
    val detailedAnalysis: String? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "reviewed_by")
    val reviewedBy: String? = null
)

enum class ReviewType {
    FULL_REVIEW,
    QUICK_CHECK,
    CEFR_ALIGNMENT
}
