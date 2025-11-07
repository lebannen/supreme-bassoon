package com.vocabee.web.api

import com.vocabee.service.JwtService
import com.vocabee.service.UserService
import com.vocabee.web.dto.AuthResponse
import com.vocabee.web.dto.LoginRequest
import com.vocabee.web.dto.RegisterRequest
import com.vocabee.web.dto.UserDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
    private val jwtService: JwtService
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        // Check if email already exists
        if (userService.existsByEmail(request.email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }

        // Create user
        val user = userService.createUser(
            email = request.email,
            displayName = request.displayName,
            password = request.password
        )

        // Generate token
        val token = jwtService.generateToken(user)

        val response = AuthResponse(
            token = token,
            user = UserDto(
                id = user.id!!,
                email = user.email,
                displayName = user.displayName,
                nativeLanguage = user.nativeLanguage,
                learningLanguages = user.learningLanguages,
                roles = user.roles.map { it.role }
            )
        )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val user = userService.findByEmail(request.email)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (!userService.validatePassword(user, request.password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        // Update last login
        user.id?.let { userService.updateLastLogin(it) }

        // Generate token
        val token = jwtService.generateToken(user)

        val response = AuthResponse(
            token = token,
            user = UserDto(
                id = user.id!!,
                email = user.email,
                displayName = user.displayName,
                nativeLanguage = user.nativeLanguage,
                learningLanguages = user.learningLanguages,
                roles = user.roles.map { it.role }
            )
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/me")
    fun getCurrentUser(@RequestHeader("Authorization") authorization: String): ResponseEntity<UserDto> {
        val token = authorization.removePrefix("Bearer ")

        val email = jwtService.getUserEmailFromToken(token)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val user = userService.findByEmail(email)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val userDto = UserDto(
            id = user.id!!,
            email = user.email,
            displayName = user.displayName,
            nativeLanguage = user.nativeLanguage,
            learningLanguages = user.learningLanguages,
            roles = user.roles.map { it.role }
        )

        return ResponseEntity.ok(userDto)
    }
}
