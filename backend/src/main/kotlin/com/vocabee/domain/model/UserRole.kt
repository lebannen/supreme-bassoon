package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "user_roles")
data class UserRole(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, length = 50)
    val role: String,

    @Column(name = "granted_at", nullable = false, updatable = false)
    val grantedAt: Instant = Instant.now()
)
