package com.vocabee.service

import com.vocabee.domain.model.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}")
    private val secret: String,

    @Value("\${jwt.expiration}")
    private val expiration: Long
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    companion object {
        private const val CLAIM_USER_ID = "userId"
        private const val CLAIM_EMAIL = "email"
        private const val CLAIM_ROLES = "roles"
    }

    fun generateToken(user: User): String {
        val claims = mapOf(
            CLAIM_USER_ID to user.id,
            CLAIM_EMAIL to user.email,
            CLAIM_ROLES to user.roles.map { it.role }
        )

        return Jwts.builder()
            .claims(claims)
            .subject(user.email)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserEmailFromToken(token: String): String? {
        return try {
            getClaims(token).subject
        } catch (e: Exception) {
            null
        }
    }

    fun getUserIdFromToken(token: String): Long? {
        return try {
            getClaims(token).get(CLAIM_USER_ID, Number::class.java)?.toLong()
        } catch (e: Exception) {
            null
        }
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
