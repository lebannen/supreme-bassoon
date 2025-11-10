package com.vocabee.web.dto

data class FileUploadResponse(
    val url: String,
    val filename: String,
    val size: Long,
    val contentType: String
)

data class FileUploadError(
    val error: String,
    val message: String
)
