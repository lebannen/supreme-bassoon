package com.vocabee.domain.model

enum class FileType(val mimeTypes: List<String>, val maxSizeMb: Long) {
    AUDIO(
        mimeTypes = listOf(
            "audio/mpeg",      // MP3
            "audio/mp4",       // M4A
            "audio/wav",       // WAV
            "audio/x-wav",     // WAV alternative
            "audio/vnd.wave",  // WAV alternative (RFC 2361)
            "audio/wave",      // WAV alternative
            "audio/ogg",       // OGG
            "audio/webm"       // WebM audio
        ),
        maxSizeMb = 50
    ),

    IMAGE(
        mimeTypes = listOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml"
        ),
        maxSizeMb = 10
    ),

    DOCUMENT(
        mimeTypes = listOf(
            "application/pdf",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOCX
            "application/msword" // DOC
        ),
        maxSizeMb = 20
    );

    companion object {
        fun fromMimeType(mimeType: String): FileType? {
            return values().find { fileType ->
                fileType.mimeTypes.any { it.equals(mimeType, ignoreCase = true) }
            }
        }

        fun isValidMimeType(mimeType: String, fileType: FileType): Boolean {
            return fileType.mimeTypes.any { it.equals(mimeType, ignoreCase = true) }
        }
    }
}
