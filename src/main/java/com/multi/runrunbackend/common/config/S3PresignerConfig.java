package com.multi.runrunbackend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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


    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
            .region(Region.of(region))
            .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.of(region))
            .build();
    }
}


