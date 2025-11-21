package com.vocabee.domain.repository

import com.vocabee.domain.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun findByOauthProviderAndOauthId(provider: String, oauthId: String): Optional<User>
    fun existsByEmail(email: String): Boolean

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    fun updateLastLogin(@Param("userId") userId: Long, @Param("lastLogin") lastLogin: Instant)
}
