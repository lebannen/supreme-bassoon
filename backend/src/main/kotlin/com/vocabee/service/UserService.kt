package com.vocabee.service

import com.vocabee.domain.model.User
import com.vocabee.domain.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email).orElse(null)
    }

    fun findById(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }

    fun createUser(
        email: String,
        displayName: String?,
        password: String? = null,
        oauthProvider: String? = null,
        oauthId: String? = null
    ): User {
        val passwordHash = password?.let { passwordEncoder.encode(it) }

        val user = User(
            email = email,
            displayName = displayName ?: email.substringBefore("@"),
            passwordHash = passwordHash,
            oauthProvider = oauthProvider,
            oauthId = oauthId,
            emailVerified = oauthProvider != null  // Auto-verify OAuth users
        )

        user.addRole("USER")

        return userRepository.save(user)
    }

    fun findOrCreateOAuthUser(
        email: String,
        displayName: String?,
        provider: String,
        oauthId: String
    ): User {
        // Try to find by OAuth provider and ID first
        return userRepository.findByOauthProviderAndOauthId(provider, oauthId)
            .orElseGet {
                // If not found, check if user exists by email
                userRepository.findByEmail(email).orElseGet {
                    // Create new user
                    createUser(
                        email = email,
                        displayName = displayName,
                        oauthProvider = provider,
                        oauthId = oauthId
                    )
                }
            }
    }

    fun updateLastLogin(userId: Long) {
        userRepository.findById(userId).ifPresent { user ->
            user.lastLogin = Instant.now()
            userRepository.save(user)
        }
    }

    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    fun validatePassword(user: User, password: String): Boolean {
        return user.passwordHash?.let {
            passwordEncoder.matches(password, it)
        } ?: false
    }
}
