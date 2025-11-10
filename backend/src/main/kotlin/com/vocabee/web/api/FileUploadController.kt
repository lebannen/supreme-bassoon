package com.vocabee.web.api

import com.vocabee.domain.model.FileType
import com.vocabee.service.StorageService
import com.vocabee.web.dto.FileUploadError
import com.vocabee.web.dto.FileUploadResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/files")
class FileUploadController(
    private val storageService: StorageService
) {
    private val logger = LoggerFactory.getLogger(FileUploadController::class.java)

    @PostMapping("/upload/audio")
    fun uploadAudio(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) folder: String?
    ): ResponseEntity<Any> {
        return uploadFile(file, FileType.AUDIO, folder)
    }

    @PostMapping("/upload/image")
    fun uploadImage(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) folder: String?
    ): ResponseEntity<Any> {
        return uploadFile(file, FileType.IMAGE, folder)
    }

    @PostMapping("/upload/document")
    fun uploadDocument(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false) folder: String?
    ): ResponseEntity<Any> {
        return uploadFile(file, FileType.DOCUMENT, folder)
    }

    private fun uploadFile(
        file: MultipartFile,
        fileType: FileType,
        folder: String?
    ): ResponseEntity<Any> {
        try {
            logger.info("Uploading ${fileType.name.lowercase()} file: ${file.originalFilename}, size: ${file.size}")

            val result = storageService.uploadFile(file, fileType, folder)

            val response = FileUploadResponse(
                url = result.url,
                filename = file.originalFilename ?: "unknown",
                size = result.size,
                contentType = result.contentType
            )

            return ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            logger.warn("File upload validation failed: ${e.message}")
            return ResponseEntity
                .badRequest()
                .body(FileUploadError("VALIDATION_ERROR", e.message ?: "Invalid file"))
        } catch (e: Exception) {
            logger.error("File upload failed: ${e.message}", e)
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FileUploadError("UPLOAD_ERROR", "Failed to upload file: ${e.message}"))
        }
    }
}
