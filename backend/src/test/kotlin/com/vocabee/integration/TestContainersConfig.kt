package com.vocabee.integration

import org.testcontainers.containers.PostgreSQLContainer

/**
 * Singleton container configuration shared across all integration tests.
 * This ensures the same PostgreSQL container is used for all test classes.
 */
object TestContainersConfig {

    val postgresContainer: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
        withDatabaseName("vocabee_test")
        withUsername("test")
        withPassword("test")
        start() // Start immediately when object is loaded
    }

    init {
        // Add shutdown hook to stop container when JVM exits
        Runtime.getRuntime().addShutdownHook(Thread {
            postgresContainer.stop()
        })
    }
}
