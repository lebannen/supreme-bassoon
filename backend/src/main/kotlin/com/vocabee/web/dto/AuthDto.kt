package com.vocabee.web.dto

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String?
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: Long,
    val email: String,
    val displayName: String?,
    val nativeLanguage: String,
    val learningLanguages: List<String>,
    val roles: List<String>
)
