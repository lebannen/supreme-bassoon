package com.vocabee.service

import com.vocabee.config.StorageProperties
import com.vocabee.domain.model.FileType
import org.apache.tika.Tika
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
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

    fun uploadFile(file: MultipartFile, fileType: FileType, folder: String? = null): UploadResult {
        if (file.isEmpty) throw IllegalArgumentException("File is empty")

        val detectedContentType = tika.detect(file.inputStream)
        logger.debug("Detected content type: $detectedContentType for file: ${file.originalFilename}")

        return upload(
            inputStream = file.inputStream,
            contentLength = file.size,
            contentType = detectedContentType,
            originalFilename = file.originalFilename ?: "file",
            fileType = fileType,
            folder = folder
        )
    }

    fun uploadFile(
        inputStream: InputStream,
        originalFilename: String,
        contentType: String,
        fileType: FileType,
        folder: String? = null
    ): UploadResult {
        val fileBytes = inputStream.readAllBytes()
        return upload(
            inputStream = fileBytes.inputStream(),
            contentLength = fileBytes.size.toLong(),
            contentType = contentType,
            originalFilename = originalFilename,
            fileType = fileType,
            folder = folder
        )
    }

    private fun upload(
        inputStream: InputStream,
        contentLength: Long,
        contentType: String,
        originalFilename: String,
        fileType: FileType,
        folder: String?
    ): UploadResult {
        if (!FileType.isValidMimeType(contentType, fileType)) {
            throw IllegalArgumentException("Invalid file type. Expected ${fileType.mimeTypes}, but got $contentType")
        }

        val fileSizeMb = contentLength / (1024 * 1024)
        if (fileSizeMb > fileType.maxSizeMb) {
            throw IllegalArgumentException("File size ($fileSizeMb MB) exceeds maximum allowed size (${fileType.maxSizeMb} MB)")
        }

        val bucket = when (fileType) {
            FileType.AUDIO -> storageProperties.s3.buckets.audio
            FileType.IMAGE, FileType.DOCUMENT -> storageProperties.s3.buckets.files
        }

        val key = generateFileKey(originalFilename, folder)
        logger.info("Uploading file to S3: bucket=$bucket, key=$key, size=$contentLength")

        try {
            val putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build()

            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength))

            val url = s3Client.utilities().getUrl { it.bucket(bucket).key(key) }.toExternalForm()
            logger.info("File uploaded successfully: $url")

            return UploadResult(url, bucket, key, contentLength, contentType)
        } catch (e: S3Exception) {
            logger.error("Failed to upload file to S3: ${e.message}", e)
            throw RuntimeException("Failed to upload file: ${e.awsErrorDetails().errorMessage()}", e)
        }
    }

    fun deleteFile(bucket: String, key: String) {
        logger.info("Deleting file from S3: bucket=$bucket, key=$key")
        try {
            s3Client.deleteObject { it.bucket(bucket).key(key) }
            logger.info("File deleted successfully")
        } catch (e: S3Exception) {
            logger.error("Failed to delete file from S3: ${e.message}", e)
            throw RuntimeException("Failed to delete file: ${e.awsErrorDetails().errorMessage()}", e)
        }
    }

    fun fileExists(bucket: String, key: String): Boolean {
        return try {
            s3Client.headObject { it.bucket(bucket).key(key) }
            true
        } catch (e: NoSuchKeyException) {
            false
        } catch (e: S3Exception) {
            logger.error("Error checking file existence: ${e.message}", e)
            false
        }
    }

    private fun generateFileKey(originalFilename: String, folder: String?): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val uuid = UUID.randomUUID().toString().substring(0, 8)
        val extension = originalFilename.substringAfterLast('.', "")
        val sanitizedName = originalFilename.substringBeforeLast(".").replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)
        val filename = "${sanitizedName}_${timestamp}_${uuid}" + if (extension.isNotEmpty()) ".$extension" else ""
        return if (folder != null) "$folder/$filename" else filename
    }
}
