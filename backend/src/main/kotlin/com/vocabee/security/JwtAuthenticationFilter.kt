package com.vocabee.security

import com.vocabee.service.JwtService
import com.vocabee.service.UserService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userService: UserService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Try to get token from Authorization header first
        val authHeader = request.getHeader("Authorization")
        val token: String? = if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else {
            // Fall back to query parameter (for EventSource/SSE endpoints)
            request.getParameter("token")
        }

        if (token == null) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            if (jwtService.validateToken(token)) {
                val userId = jwtService.getUserIdFromToken(token)

                if (userId != null && SecurityContextHolder.getContext().authentication == null) {
                    val user = userService.findById(userId)

                    if (user != null) {
                        val authorities = user.roles.map { SimpleGrantedAuthority("ROLE_${it.role}") }
                        val authToken = UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            authorities
                        )
                        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authToken
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("JWT authentication failed: ${e.message}")
        }

        filterChain.doFilter(request, response)
    }
}
