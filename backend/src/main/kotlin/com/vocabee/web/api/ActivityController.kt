package com.vocabee.web.api

import com.vocabee.service.ActivityService
import com.vocabee.service.JwtService
import com.vocabee.web.dto.DailyActivityDto
import com.vocabee.web.dto.DailyGoalsResponse
import com.vocabee.web.dto.RecordActivityRequest
import com.vocabee.web.dto.UserActivitySummaryDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/activity")
class ActivityController(
    private val activityService: ActivityService,
    private val jwtService: JwtService
) {

    @PostMapping("/record")
    fun recordActivity(
        @RequestHeader("Authorization") authorization: String,
        @RequestBody request: RecordActivityRequest
    ): ResponseEntity<DailyActivityDto> {
        val userId = getUserIdFromToken(authorization)

        val activity = activityService.recordActivity(userId, request)
        return ResponseEntity.ok(activity)
    }

    @GetMapping("/today")
    fun getTodayActivity(
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<DailyActivityDto> {
        val userId = getUserIdFromToken(authorization)

        val activity = activityService.getTodayActivity(userId)
        return ResponseEntity.ok(activity)
    }

    @GetMapping("/goals")
    fun getDailyGoals(
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<DailyGoalsResponse> {
        val userId = getUserIdFromToken(authorization)

        val goals = activityService.getDailyGoals(userId)
        val todayActivity = activityService.getTodayActivity(userId)
        val streak = activityService.calculateStreak(userId)

        return ResponseEntity.ok(
            DailyGoalsResponse(
                goals = goals,
                todayActivity = todayActivity,
                streak = streak
            )
        )
    }

    @GetMapping("/summary")
    fun getActivitySummary(
        @RequestHeader("Authorization") authorization: String,
        @RequestParam(defaultValue = "30") days: Int
    ): ResponseEntity<UserActivitySummaryDto> {
        val userId = getUserIdFromToken(authorization)

        val summary = activityService.getActivitySummary(userId, days)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/streak")
    fun getStreak(
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Map<String, Int>> {
        val userId = getUserIdFromToken(authorization)

        val streak = activityService.calculateStreak(userId)
        return ResponseEntity.ok(mapOf("streak" to streak))
    }

    private fun getUserIdFromToken(authorization: String): Long {
        val token = authorization.removePrefix("Bearer ").trim()
        return jwtService.getUserIdFromToken(token)
            ?: throw org.springframework.security.access.AccessDeniedException("Invalid token")
    }
}
