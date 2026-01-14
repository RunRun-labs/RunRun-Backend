package com.multi.runrunbackend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : S3PresignerConfig
 * @since : 2025. 12. 24. Wednesday
 */
@Configuration
public class S3PresignerConfig {

    @Value("${AWS_REGION}")
    private String region;

    @Value("${AWS_ACCESS_KEY:}")
    private String accessKeyId;

    @Value("${AWS_SECRET_KEY:}")
    private String secretAccessKey;


    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
            .region(Region.of(region));

        // 환경 변수로 자격 증명이 제공되면 사용
        if (!accessKeyId.isEmpty() && !secretAccessKey.isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId,
                secretAccessKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }
        // 자격 증명이 없으면 기본 자격 증명 체인 사용 (IRSA 등)

        return builder.build();
    }

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
            .region(Region.of(region));

        // 환경 변수로 자격 증명이 제공되면 사용
        if (!accessKeyId.isEmpty() && !secretAccessKey.isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId,
                secretAccessKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }

        return builder.build();
    }
}