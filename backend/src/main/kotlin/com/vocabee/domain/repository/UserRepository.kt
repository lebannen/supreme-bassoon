package com.vocabee.domain.repository

import com.vocabee.domain.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun findByOauthProviderAndOauthId(provider: String, oauthId: String): Optional<User>
    fun existsByEmail(email: String): Boolean
}
