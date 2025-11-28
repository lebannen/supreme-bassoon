package com.vocabee.service

import com.vocabee.domain.model.UserCourseEnrollment
import com.vocabee.domain.model.UserEpisodeProgress
import com.vocabee.domain.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class EnrollmentService(
    private val enrollmentRepository: UserCourseEnrollmentRepository,
    private val episodeProgressRepository: UserEpisodeProgressRepository,
    private val courseRepository: CourseRepository,
    private val moduleRepository: ModuleRepository,
    private val episodeRepository: EpisodeRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getUserEnrollments(userId: Long): List<EnrollmentWithProgress> {
        val enrollments = enrollmentRepository.findByUserIdOrderByLastActivityDesc(userId)
        return enrollments.map { enrollment ->
            buildEnrollmentWithProgress(userId, enrollment)
        }
    }

    fun getEnrollment(userId: Long, courseId: Long): EnrollmentWithProgress? {
        val enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
            ?: return null
        return buildEnrollmentWithProgress(userId, enrollment)
    }

    fun isEnrolled(userId: Long, courseId: Long): Boolean {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)
    }

    @Transactional
    fun enrollInCourse(userId: Long, courseId: Long): EnrollmentWithProgress {
        // Check if already enrolled
        val existing = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
        if (existing != null) {
            logger.info("User $userId already enrolled in course $courseId")
            return buildEnrollmentWithProgress(userId, existing)
        }

        val course = courseRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }

        // Find first module and episode
        val firstModule = moduleRepository.findByCourseIdOrderByModuleNumber(courseId).firstOrNull()
        val firstEpisode = firstModule?.let {
            episodeRepository.findByModuleIdOrderByEpisodeNumber(it.id!!).firstOrNull()
        }

        val enrollment = UserCourseEnrollment(
            userId = userId,
            courseId = courseId,
            currentModuleId = firstModule?.id,
            currentEpisodeId = firstEpisode?.id,
            enrolledAt = LocalDateTime.now(),
            lastActivityAt = LocalDateTime.now()
        )

        val saved = enrollmentRepository.save(enrollment)
        logger.info("User $userId enrolled in course $courseId")

        return buildEnrollmentWithProgress(userId, saved)
    }

    @Transactional
    fun updateCurrentPosition(userId: Long, courseId: Long, moduleId: Long, episodeId: Long) {
        val enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
            ?: throw IllegalArgumentException("Enrollment not found")

        val updated = enrollment.copy(
            currentModuleId = moduleId,
            currentEpisodeId = episodeId,
            lastActivityAt = LocalDateTime.now()
        )
        enrollmentRepository.save(updated)
    }

    @Transactional
    fun markEpisodeCompleted(userId: Long, episodeId: Long): UserEpisodeProgress {
        val episode = episodeRepository.findById(episodeId)
            .orElseThrow { IllegalArgumentException("Episode not found: $episodeId") }

        val module = moduleRepository.findById(episode.moduleId)
            .orElseThrow { IllegalArgumentException("Module not found") }

        // Get or create episode progress
        var progress = episodeProgressRepository.findByUserIdAndEpisodeId(userId, episodeId)
        if (progress == null) {
            progress = UserEpisodeProgress(
                userId = userId,
                episodeId = episodeId
            )
        }

        // Mark as completed
        val updated = progress.copy(
            isCompleted = true,
            hasReadContent = true,
            completedAt = LocalDateTime.now(),
            lastActivityAt = LocalDateTime.now()
        )
        val saved = episodeProgressRepository.save(updated)

        // Update enrollment last activity
        enrollmentRepository.findByUserIdAndCourseId(userId, module.courseId)?.let { enrollment ->
            val updatedEnrollment = enrollment.copy(lastActivityAt = LocalDateTime.now())
            enrollmentRepository.save(updatedEnrollment)
        }

        logger.info("User $userId completed episode $episodeId")
        return saved
    }

    private fun buildEnrollmentWithProgress(userId: Long, enrollment: UserCourseEnrollment): EnrollmentWithProgress {
        val course = courseRepository.findById(enrollment.courseId).orElse(null)
        val totalEpisodes = course?.let { countTotalEpisodes(it.id!!) } ?: 0
        val completedEpisodes = episodeProgressRepository.countCompletedEpisodesByCourse(userId, enrollment.courseId)
        val progress = if (totalEpisodes > 0) (completedEpisodes.toDouble() / totalEpisodes * 100).toInt() else 0

        // Get current module and episode names
        val currentModule = enrollment.currentModuleId?.let { moduleRepository.findById(it).orElse(null) }
        val currentEpisode = enrollment.currentEpisodeId?.let { episodeRepository.findById(it).orElse(null) }

        return EnrollmentWithProgress(
            id = enrollment.id!!,
            courseId = enrollment.courseId,
            courseSlug = course?.slug ?: "",
            courseName = course?.name ?: "",
            languageCode = course?.languageCode ?: "",
            cefrLevel = course?.cefrLevel ?: "",
            currentModuleId = enrollment.currentModuleId,
            currentModuleName = currentModule?.title,
            currentEpisodeId = enrollment.currentEpisodeId,
            currentEpisodeName = currentEpisode?.title,
            completedEpisodes = completedEpisodes,
            totalEpisodes = totalEpisodes,
            progress = progress,
            enrolledAt = enrollment.enrolledAt,
            lastActivityAt = enrollment.lastActivityAt
        )
    }

    private fun countTotalEpisodes(courseId: Long): Int {
        val modules = moduleRepository.findByCourseIdOrderByModuleNumber(courseId)
        return modules.sumOf { module ->
            episodeRepository.findByModuleIdOrderByEpisodeNumber(module.id!!).size
        }
    }
}

data class EnrollmentWithProgress(
    val id: Long,
    val courseId: Long,
    val courseSlug: String,
    val courseName: String,
    val languageCode: String,
    val cefrLevel: String,
    val currentModuleId: Long?,
    val currentModuleName: String?,
    val currentEpisodeId: Long?,
    val currentEpisodeName: String?,
    val completedEpisodes: Int,
    val totalEpisodes: Int,
    val progress: Int,
    val enrolledAt: LocalDateTime,
    val lastActivityAt: LocalDateTime
)
