package com.vocabee.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "storage")
data class StorageProperties(
    var type: String = "s3",
    var s3: S3Properties = S3Properties()
)

data class S3Properties(
    var endpoint: String = "http://localhost:9000",
    var region: String = "us-east-1",
    var accessKey: String = "minioadmin",
    var secretKey: String = "minioadmin123",
    var pathStyleEnabled: Boolean = true,
    var buckets: BucketProperties = BucketProperties()
)

data class BucketProperties(
    var audio: String = "vocabee-audio",
    var files: String = "vocabee-files"
)
