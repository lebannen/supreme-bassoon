package com.vocabee.config

import com.vocabee.security.JwtAuthenticationFilter
import com.vocabee.security.OAuth2AuthenticationSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**", "/login/**", "/oauth2/**", "/error").permitAll()
                    .requestMatchers("/api/v1/**").permitAll()
                    .requestMatchers("/api/files/**").permitAll()  // File uploads
                    .requestMatchers("/api/reading/texts", "/api/reading/texts/*", "/api/reading/texts/import", "/api/reading/texts/*/audio").permitAll()  // Public text browsing, import, and audio updates
                    .requestMatchers("/api/reading/**").authenticated()  // Progress tracking requires auth
                    .requestMatchers("/api/exercises/*/attempt", "/api/exercises/*/progress").authenticated()  // Attempts and progress require auth (must be before general exercises)
                    .requestMatchers("/api/exercises/**").permitAll()  // Public exercise browsing
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureUrl("/login?error=true")
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { request, response, _ ->
                    // For API requests, return 401 instead of redirecting
                    if (request.requestURI.startsWith("/api/")) {
                        response.sendError(401, "Unauthorized")
                    } else {
                        // For non-API requests, redirect to login page
                        response.sendRedirect("/login")
                    }
                }
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:5173", "http://localhost:5174", "http://localhost:8080")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
