package com.vocabee.web.api

import com.vocabee.service.EnrollmentService
import com.vocabee.service.EnrollmentWithProgress
import com.vocabee.service.JwtService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/enrollments")
class EnrollmentController(
    private val enrollmentService: EnrollmentService,
    private val jwtService: JwtService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getMyEnrollments(
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<List<EnrollmentWithProgress>> {
        val userId = jwtService.getUserIdFromToken(authHeader.removePrefix("Bearer "))
            ?: return ResponseEntity.status(401).build()

        val enrollments = enrollmentService.getUserEnrollments(userId)
        return ResponseEntity.ok(enrollments)
    }

    @GetMapping("/course/{courseId}")
    fun getEnrollment(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable courseId: Long
    ): ResponseEntity<EnrollmentWithProgress> {
        val userId = jwtService.getUserIdFromToken(authHeader.removePrefix("Bearer "))
            ?: return ResponseEntity.status(401).build()

        val enrollment = enrollmentService.getEnrollment(userId, courseId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(enrollment)
    }

    @GetMapping("/course/{courseId}/check")
    fun checkEnrollment(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable courseId: Long
    ): ResponseEntity<Map<String, Boolean>> {
        val userId = jwtService.getUserIdFromToken(authHeader.removePrefix("Bearer "))
            ?: return ResponseEntity.status(401).build()

        val isEnrolled = enrollmentService.isEnrolled(userId, courseId)
        return ResponseEntity.ok(mapOf("enrolled" to isEnrolled))
    }

    @PostMapping("/course/{courseId}")
    fun enrollInCourse(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable courseId: Long
    ): ResponseEntity<EnrollmentWithProgress> {
        val userId = jwtService.getUserIdFromToken(authHeader.removePrefix("Bearer "))
            ?: return ResponseEntity.status(401).build()

        return try {
            val enrollment = enrollmentService.enrollInCourse(userId, courseId)
            ResponseEntity.ok(enrollment)
        } catch (e: IllegalArgumentException) {
            logger.error("Failed to enroll: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/course/{courseId}/position")
    fun updatePosition(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable courseId: Long,
        @RequestBody request: UpdatePositionRequest
    ): ResponseEntity<Void> {
        val userId = jwtService.getUserIdFromToken(authHeader.removePrefix("Bearer "))
            ?: return ResponseEntity.status(401).build()

        return try {
            enrollmentService.updateCurrentPosition(userId, courseId, request.moduleId, request.episodeId)
            ResponseEntity.ok().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/episodes/{episodeId}/complete")
    fun completeEpisode(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable episodeId: Long
    ): ResponseEntity<Map<String, Any>> {
        val userId = jwtService.getUserIdFromToken(authHeader.removePrefix("Bearer "))
            ?: return ResponseEntity.status(401).build()

        return try {
            val progress = enrollmentService.markEpisodeCompleted(userId, episodeId)
            ResponseEntity.ok(
                mapOf(
                    "episodeId" to episodeId,
                    "isCompleted" to progress.isCompleted
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.error("Failed to complete episode: ${e.message}")
            ResponseEntity.badRequest().build()
        }
    }
}

data class UpdatePositionRequest(
    val moduleId: Long,
    val episodeId: Long
)
