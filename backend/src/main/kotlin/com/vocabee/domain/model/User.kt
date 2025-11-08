package com.vocabee.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(name = "password_hash")
    val passwordHash: String? = null,

    @Column(name = "display_name", length = 100)
    var displayName: String? = null,

    @Column(name = "native_language", length = 10)
    var nativeLanguage: String = "en",

    @Column(name = "learning_languages", columnDefinition = "varchar[]")
    var learningLanguages: List<String> = listOf("fr", "de"),

    @Column(name = "oauth_provider", length = 50)
    val oauthProvider: String? = null,

    @Column(name = "oauth_id")
    val oauthId: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "last_login")
    var lastLogin: Instant? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "email_verified", nullable = false)
    val emailVerified: Boolean = false
) {
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    val roles: MutableSet<UserRole> = mutableSetOf()
    fun hasRole(roleName: String): Boolean {
        return roles.any { it.role == roleName }
    }

    fun addRole(roleName: String) {
        roles.add(UserRole(user = this, role = roleName))
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
}

@Converter
class StringArrayConverter : AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(attribute: List<String>?): String {
        return attribute?.joinToString(",") ?: ""
    }

    override fun convertToEntityAttribute(dbData: String?): List<String> {
        return dbData?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
}
