package com.vocabee.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

@Configuration
class S3Config(
    private val storageProperties: StorageProperties
) {
    private val logger = LoggerFactory.getLogger(S3Config::class.java)

    @Bean
    fun s3Client(): S3Client {
        val s3Props = storageProperties.s3

        logger.info("Initializing S3 client with endpoint: ${s3Props.endpoint}")

        val credentials = AwsBasicCredentials.create(
            s3Props.accessKey,
            s3Props.secretKey
        )

        return S3Client.builder()
            .region(Region.of(s3Props.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .endpointOverride(URI.create(s3Props.endpoint))
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(s3Props.pathStyleEnabled)
                    .build()
            )
            .build()
    }
}
