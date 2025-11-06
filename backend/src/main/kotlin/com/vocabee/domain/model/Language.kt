package com.vocabee.domain.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "languages",
    indexes = [Index(name = "idx_languages_code", columnList = "code")]
)
data class Language(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(length = 10, nullable = false, unique = true)
    val code: String,

    @Column(length = 100, nullable = false)
    val name: String,

    @Column(name = "entry_count")
    val entryCount: Int = 0,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null
)
