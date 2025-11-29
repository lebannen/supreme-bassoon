package com.vocabee.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.vocabee.domain.model.ContentItemType
import com.vocabee.domain.model.CourseReview
import com.vocabee.domain.model.ReviewType
import com.vocabee.domain.repository.*
import com.vocabee.service.external.gemini.GeminiTextClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CourseReviewService(
    private val courseRepository: CourseRepository,
    private val moduleRepository: ModuleRepository,
    private val episodeRepository: EpisodeRepository,
    private val episodeContentItemRepository: EpisodeContentItemRepository,
    private val exerciseRepository: ExerciseRepository,
    private val courseReviewRepository: CourseReviewRepository,
    private val geminiTextClient: GeminiTextClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun reviewCourse(courseId: Long, reviewedBy: String? = "system"): CourseReview {
        logger.info("Starting AI review for course $courseId")

        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }

        // Build course export for review
        val modules = moduleRepository.findByCourseIdOrderByModuleNumber(courseId)
        val courseData = buildCourseDataForReview(course, modules)

        // Generate review prompt
        val prompt = buildReviewPrompt(course, courseData)

        logger.info("Sending course to Gemini for review (${prompt.length} chars, model: ${geminiTextClient.getModelName()})")

        // Call Gemini for review
        val reviewJson = geminiTextClient.generateText(prompt, "application/json")

        logger.info("Received review response from Gemini")

        // Parse response
        val reviewData = objectMapper.readTree(reviewJson)

        // Create and save review
        val review = CourseReview(
            courseId = courseId,
            reviewType = ReviewType.FULL_REVIEW,
            modelUsed = geminiTextClient.getModelName(),
            overallScore = reviewData.path("overallScore").asInt(0),
            cefrAlignmentScore = reviewData.path("cefrAlignmentScore").asInt(0),
            structureScore = reviewData.path("structureScore").asInt(0),
            contentQualityScore = reviewData.path("contentQualityScore").asInt(0),
            summary = reviewData.path("summary").asText("No summary provided"),
            strengths = reviewData.path("strengths").takeIf { it.isArray },
            weaknesses = reviewData.path("weaknesses").takeIf { it.isArray },
            recommendations = reviewData.path("recommendations").takeIf { it.isArray },
            moduleFeedback = reviewData.path("moduleFeedback").takeIf { it.isArray },
            detailedAnalysis = reviewData.path("detailedAnalysis").asText(null),
            reviewedBy = reviewedBy
        )

        val savedReview = courseReviewRepository.save(review)
        logger.info("Saved course review with id ${savedReview.id}, overall score: ${savedReview.overallScore}")

        return savedReview
    }

    fun getLatestReview(courseId: Long): CourseReview? {
        return courseReviewRepository.findFirstByCourseIdOrderByCreatedAtDesc(courseId)
    }

    fun getReviewHistory(courseId: Long): List<CourseReview> {
        return courseReviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
    }

    private fun buildCourseDataForReview(
        course: com.vocabee.domain.model.Course,
        modules: List<com.vocabee.domain.model.Module>
    ): Map<String, Any?> {
        return mapOf(
            "course" to mapOf(
                "name" to course.name,
                "languageCode" to course.languageCode,
                "cefrLevel" to course.cefrLevel,
                "description" to course.description,
                "seriesContext" to course.seriesContext,
                "estimatedHours" to course.estimatedHours
            ),
            "modules" to modules.map { module ->
                val episodes = episodeRepository.findByModuleIdOrderByEpisodeNumber(module.id!!)

                mapOf(
                    "moduleNumber" to module.moduleNumber,
                    "title" to module.title,
                    "theme" to module.theme,
                    "description" to module.description,
                    "objectives" to module.objectives?.let { parseJsonArray(it) },
                    "vocabularyFocus" to module.vocabularyFocus?.let { parseJsonArray(it) },
                    "grammarFocus" to module.grammarFocus?.let { parseJsonArray(it) },
                    "episodes" to episodes.map { episode ->
                        val exercises = episodeContentItemRepository.findByEpisodeIdOrderByOrderIndex(episode.id!!)
                            .filter { it.contentType == ContentItemType.EXERCISE }
                            .mapNotNull { item ->
                                exerciseRepository.findById(item.contentId).orElse(null)?.let { ex ->
                                    mapOf(
                                        "type" to ex.exerciseType.typeKey,
                                        "content" to ex.content
                                    )
                                }
                            }

                        mapOf(
                            "episodeNumber" to episode.episodeNumber,
                            "type" to episode.episodeType.name,
                            "title" to episode.title,
                            "summary" to episode.summary,
                            "content" to episode.content.take(2000), // Limit content length
                            "hasAudio" to (episode.audioUrl != null),
                            "exerciseCount" to exercises.size,
                            "exerciseTypes" to exercises.map { it["type"] }.distinct()
                        )
                    }
                )
            },
            "statistics" to mapOf(
                "totalModules" to modules.size,
                "totalEpisodes" to modules.sumOf { m ->
                    episodeRepository.findByModuleIdOrderByEpisodeNumber(m.id!!).size
                }
            )
        )
    }

    private fun buildReviewPrompt(
        course: com.vocabee.domain.model.Course,
        courseData: Map<String, Any?>
    ): String {
        val courseJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(courseData)

        return """
You are an expert language education reviewer specializing in CEFR-aligned course design.

Review the following ${course.languageCode.uppercase()} language course (claimed level: ${course.cefrLevel}) and provide detailed feedback.

## Course Data:
```json
$courseJson
```

## Review Criteria:

1. **CEFR Alignment** (cefrAlignmentScore: 0-100)
   - Does vocabulary complexity match the stated level?
   - Are grammar structures appropriate for this level?
   - Is the content progression suitable?

2. **Course Structure** (structureScore: 0-100)
   - Is there logical progression between modules?
   - Do episode types vary appropriately?
   - Is the pacing reasonable?

3. **Content Quality** (contentQualityScore: 0-100)
   - Are dialogues/stories natural and engaging?
   - Do exercises reinforce learning objectives?
   - Is vocabulary recycled appropriately?

4. **Overall Assessment** (overallScore: 0-100)
   - Weighted average considering all factors

## Response Format (JSON):
```json
{
  "overallScore": 75,
  "cefrAlignmentScore": 80,
  "structureScore": 70,
  "contentQualityScore": 75,
  "summary": "Brief 2-3 sentence overall assessment",
  "strengths": [
    "Strength point 1",
    "Strength point 2",
    "Strength point 3"
  ],
  "weaknesses": [
    "Weakness/issue 1",
    "Weakness/issue 2"
  ],
  "recommendations": [
    "Specific actionable recommendation 1",
    "Specific actionable recommendation 2",
    "Specific actionable recommendation 3"
  ],
  "moduleFeedback": [
    {
      "moduleNumber": 1,
      "score": 80,
      "feedback": "Brief feedback for this module"
    }
  ],
  "detailedAnalysis": "Longer detailed analysis with specific examples from the course content..."
}
```

Provide honest, constructive feedback. Be specific with examples from the course content when pointing out issues.
        """.trimIndent()
    }

    private fun parseJsonArray(node: JsonNode): List<String> {
        return if (node.isArray) node.map { it.asText() } else emptyList()
    }
}
