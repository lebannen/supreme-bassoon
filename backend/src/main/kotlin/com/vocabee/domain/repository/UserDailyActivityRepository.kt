package com.vocabee.domain.repository

import com.vocabee.domain.model.UserDailyActivity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface UserDailyActivityRepository : JpaRepository<UserDailyActivity, Long> {
    fun findByUserIdAndActivityDate(userId: Long, activityDate: LocalDate): UserDailyActivity?

    fun findByUserIdOrderByActivityDateDesc(userId: Long): List<UserDailyActivity>

    fun findByUserIdAndActivityDateBetweenOrderByActivityDateDesc(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<UserDailyActivity>

    @Query(
        """
        SELECT a FROM UserDailyActivity a
        WHERE a.userId = :userId
        AND a.activityDate >= :startDate
        ORDER BY a.activityDate DESC
    """
    )
    fun findRecentActivity(userId: Long, startDate: LocalDate): List<UserDailyActivity>
}
