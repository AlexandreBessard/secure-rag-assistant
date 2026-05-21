package com.lexoft.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.comprehend.ComprehendClient;

@Configuration
public class ComprehendConfig {

    @Bean
    public ComprehendClient comprehendClient(@Value("${app.comprehend.region}") String region) {
        return ComprehendClient.builder()
                .region(Region.of(region))
                .build();
    }
}
