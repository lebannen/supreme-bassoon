package com.vocabee.service

import com.vocabee.domain.model.User
import com.vocabee.domain.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional(readOnly = true)
    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email).orElse(null)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }

    @Transactional
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
            emailVerified = oauthProvider != null
        )

        user.addRole("USER")

        return userRepository.save(user)
    }

    @Transactional
    fun findOrCreateOAuthUser(
        email: String,
        displayName: String?,
        provider: String,
        oauthId: String
    ): User {
        return userRepository.findByOauthProviderAndOauthId(provider, oauthId)
            .orElseGet {
                userRepository.findByEmail(email).orElseGet {
                    createUser(
                        email = email,
                        displayName = displayName,
                        oauthProvider = provider,
                        oauthId = oauthId
                    )
                }
            }
    }

    @Transactional
    fun updateLastLogin(userId: Long) {
        userRepository.updateLastLogin(userId, Instant.now())
    }

    @Transactional(readOnly = true)
    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    fun validatePassword(user: User, password: String): Boolean {
        return user.passwordHash?.let {
            passwordEncoder.matches(password, it)
        } ?: false
    }

    @Transactional
    fun updateProfile(
        userId: Long,
        displayName: String?,
        nativeLanguage: String?,
        learningLanguages: List<String>?
    ): User? {
        return userRepository.findById(userId).map { user ->
            displayName?.let { user.displayName = it }
            nativeLanguage?.let { user.nativeLanguage = it }
            learningLanguages?.let { user.learningLanguages = it }
            userRepository.save(user)
        }.orElse(null)
    }
}
