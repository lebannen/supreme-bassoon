package com.vocabee.domain.validation

enum class ErrorSeverity {
    CRITICAL,  // Blocks save (e.g., malformed JSON, missing required fields)
    ERROR,     // Should fix (e.g., 3 speakers, invalid format)
    WARNING    // Can ignore (e.g., exercise count mismatch, optional field missing)
}

data class ValidationIssue(
    val field: String,
    val message: String,
    val severity: ErrorSeverity
)

data class EpisodeValidationResult(
    val isValid: Boolean,
    val issues: List<ValidationIssue>
) {
    val errors: List<ValidationIssue>
        get() = issues.filter { it.severity == ErrorSeverity.ERROR || it.severity == ErrorSeverity.CRITICAL }

    val warnings: List<ValidationIssue>
        get() = issues.filter { it.severity == ErrorSeverity.WARNING }

    val hasErrors: Boolean
        get() = errors.isNotEmpty()

    val hasWarnings: Boolean
        get() = warnings.isNotEmpty()
}
