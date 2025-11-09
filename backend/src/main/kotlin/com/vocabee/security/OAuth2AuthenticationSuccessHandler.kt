package com.vocabee.security

import com.vocabee.service.JwtService
import com.vocabee.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2AuthenticationSuccessHandler(
    private val userService: UserService,
    private val jwtService: JwtService
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oAuth2User = authentication.principal as OAuth2User

        val email = oAuth2User.getAttribute<String>("email")
            ?: throw IllegalStateException("Email not found from OAuth2 provider")

        val name = oAuth2User.getAttribute<String>("name")
        val provider = determineProvider(request)
        val providerId = oAuth2User.getAttribute<String>("sub")
            ?: throw IllegalStateException("Provider ID not found")

        // Find or create user
        val user = userService.findOrCreateOAuthUser(
            email = email,
            displayName = name,
            provider = provider,
            oauthId = providerId
        )

        // Update last login
        user.id?.let { userService.updateLastLogin(it) }

        // Generate JWT token
        val token = jwtService.generateToken(user)

        // Redirect to frontend with token
        val targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/auth/callback")
            .queryParam("token", token)
            .build()
            .toUriString()

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }

    private fun determineProvider(request: HttpServletRequest): String {
        val requestUri = request.requestURI
        return when {
            requestUri.contains("google") -> "google"
            requestUri.contains("apple") -> "apple"
            else -> "unknown"
        }
    }
}
