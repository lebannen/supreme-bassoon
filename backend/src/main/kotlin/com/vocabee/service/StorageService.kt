package com.vocabee.service

import com.vocabee.config.StorageProperties
import com.vocabee.domain.model.FileType
import org.apache.tika.Tika
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class UploadResult(
    val url: String,
    val bucket: String,
    val key: String,
    val size: Long,
    val contentType: String
)

@Service
class StorageService(
    private val s3Client: S3Client,
    private val storageProperties: StorageProperties
) {
    private val logger = LoggerFactory.getLogger(StorageService::class.java)
    private val tika = Tika()

    /**
     * Upload a file to S3-compatible storage
     * @param file The multipart file to upload
     * @param fileType The type of file (AUDIO, IMAGE, DOCUMENT)
     * @param folder Optional subfolder within the bucket
     * @return UploadResult with the file URL and metadata
     */
    fun uploadFile(
        file: MultipartFile,
        fileType: FileType,
        folder: String? = null
    ): UploadResult {
        // Validate file is not empty
        if (file.isEmpty) {
            throw IllegalArgumentException("File is empty")
        }

        // Detect content type
        val detectedContentType = tika.detect(file.inputStream)
        logger.debug("Detected content type: $detectedContentType for file: ${file.originalFilename}")

        // Validate content type
        if (!FileType.isValidMimeType(detectedContentType, fileType)) {
            throw IllegalArgumentException(
                "Invalid file type. Expected ${fileType.mimeTypes}, but got $detectedContentType"
            )
        }

        // Validate file size
        val fileSizeMb = file.size / (1024 * 1024)
        if (fileSizeMb > fileType.maxSizeMb) {
            throw IllegalArgumentException(
                "File size ($fileSizeMb MB) exceeds maximum allowed size (${fileType.maxSizeMb} MB)"
            )
        }

        // Determine bucket based on file type
        val bucket = when (fileType) {
            FileType.AUDIO -> storageProperties.s3.buckets.audio
            FileType.IMAGE, FileType.DOCUMENT -> storageProperties.s3.buckets.files
        }

        // Generate unique file key
        val key = generateFileKey(file.originalFilename ?: "file", folder)

        logger.info("Uploading file to S3: bucket=$bucket, key=$key, size=${file.size}")

        try {
            // Upload to S3
            val putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(detectedContentType)
                .contentLength(file.size)
                .build()

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.inputStream, file.size))

            // Generate public URL
            val url = generatePublicUrl(bucket, key)

            logger.info("File uploaded successfully: $url")

            return UploadResult(
                url = url,
                bucket = bucket,
                key = key,
                size = file.size,
                contentType = detectedContentType
            )
        } catch (e: S3Exception) {
            logger.error("Failed to upload file to S3: ${e.message}", e)
            throw RuntimeException("Failed to upload file: ${e.awsErrorDetails().errorMessage()}", e)
        }
    }

    /**
     * Upload a file from input stream
     */
    fun uploadFile(
        inputStream: InputStream,
        originalFilename: String,
        contentType: String,
        fileType: FileType,
        folder: String? = null
    ): UploadResult {
        val fileBytes = inputStream.readBytes()
        val fileSize = fileBytes.size.toLong()

        // Validate content type
        if (!FileType.isValidMimeType(contentType, fileType)) {
            throw IllegalArgumentException(
                "Invalid file type. Expected ${fileType.mimeTypes}, but got $contentType"
            )
        }

        // Validate file size
        val fileSizeMb = fileSize / (1024 * 1024)
        if (fileSizeMb > fileType.maxSizeMb) {
            throw IllegalArgumentException(
                "File size ($fileSizeMb MB) exceeds maximum allowed size (${fileType.maxSizeMb} MB)"
            )
        }

        val bucket = when (fileType) {
            FileType.AUDIO -> storageProperties.s3.buckets.audio
            FileType.IMAGE, FileType.DOCUMENT -> storageProperties.s3.buckets.files
        }

        val key = generateFileKey(originalFilename, folder)

        logger.info("Uploading file to S3: bucket=$bucket, key=$key, size=$fileSize")

        try {
            val putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(fileSize)
                .build()

            s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes))

            val url = generatePublicUrl(bucket, key)

            logger.info("File uploaded successfully: $url")

            return UploadResult(
                url = url,
                bucket = bucket,
                key = key,
                size = fileSize,
                contentType = contentType
            )
        } catch (e: S3Exception) {
            logger.error("Failed to upload file to S3: ${e.message}", e)
            throw RuntimeException("Failed to upload file: ${e.awsErrorDetails().errorMessage()}", e)
        }
    }

    /**
     * Delete a file from storage
     */
    fun deleteFile(bucket: String, key: String) {
        logger.info("Deleting file from S3: bucket=$bucket, key=$key")

        try {
            val deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()

            s3Client.deleteObject(deleteRequest)

            logger.info("File deleted successfully")
        } catch (e: S3Exception) {
            logger.error("Failed to delete file from S3: ${e.message}", e)
            throw RuntimeException("Failed to delete file: ${e.awsErrorDetails().errorMessage()}", e)
        }
    }

    /**
     * Check if a file exists
     */
    fun fileExists(bucket: String, key: String): Boolean {
        return try {
            val headRequest = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()

            s3Client.headObject(headRequest)
            true
        } catch (e: NoSuchKeyException) {
            false
        } catch (e: S3Exception) {
            logger.error("Error checking file existence: ${e.message}", e)
            false
        }
    }

    /**
     * Generate a unique file key with timestamp and UUID
     */
    private fun generateFileKey(originalFilename: String, folder: String?): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val uuid = UUID.randomUUID().toString().substring(0, 8)
        val extension = originalFilename.substringAfterLast(".", "")
        val sanitizedName = originalFilename
            .substringBeforeLast(".")
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(50)

        val filename = "${sanitizedName}_${timestamp}_${uuid}" + if (extension.isNotEmpty()) ".$extension" else ""

        return if (folder != null) {
            "$folder/$filename"
        } else {
            filename
        }
    }

    /**
     * Generate public URL for accessing the file
     */
    private fun generatePublicUrl(bucket: String, key: String): String {
        val endpoint = storageProperties.s3.endpoint
        return "$endpoint/$bucket/$key"
    }
}
