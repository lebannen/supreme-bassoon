package com.vocabee.web.dto.admin

import com.fasterxml.jackson.databind.JsonNode
import com.vocabee.domain.model.CourseReview

data class CourseReviewDto(
    val id: Long,
    val courseId: Long,
    val reviewType: String,
    val modelUsed: String?,
    val overallScore: Int?,
    val cefrAlignmentScore: Int?,
    val structureScore: Int?,
    val contentQualityScore: Int?,
    val summary: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val recommendations: List<String>,
    val moduleFeedback: List<ModuleFeedbackDto>,
    val detailedAnalysis: String?,
    val createdAt: String,
    val reviewedBy: String?
)

data class ModuleFeedbackDto(
    val moduleNumber: Int,
    val score: Int?,
    val feedback: String
)

fun CourseReview.toDto(): CourseReviewDto {
    return CourseReviewDto(
        id = id!!,
        courseId = courseId,
        reviewType = reviewType.name,
        modelUsed = modelUsed,
        overallScore = overallScore,
        cefrAlignmentScore = cefrAlignmentScore,
        structureScore = structureScore,
        contentQualityScore = contentQualityScore,
        summary = summary,
        strengths = strengths?.let { parseJsonArrayToStrings(it) } ?: emptyList(),
        weaknesses = weaknesses?.let { parseJsonArrayToStrings(it) } ?: emptyList(),
        recommendations = recommendations?.let { parseJsonArrayToStrings(it) } ?: emptyList(),
        moduleFeedback = moduleFeedback?.let { parseModuleFeedback(it) } ?: emptyList(),
        detailedAnalysis = detailedAnalysis,
        createdAt = createdAt.toString(),
        reviewedBy = reviewedBy
    )
}

private fun parseJsonArrayToStrings(node: JsonNode): List<String> {
    return if (node.isArray) node.map { it.asText() } else emptyList()
}

private fun parseModuleFeedback(node: JsonNode): List<ModuleFeedbackDto> {
    return if (node.isArray) {
        node.map { item ->
            ModuleFeedbackDto(
                moduleNumber = item.path("moduleNumber").asInt(0),
                score = item.path("score").asInt().takeIf { it > 0 },
                feedback = item.path("feedback").asText("")
            )
        }
    } else emptyList()
}
