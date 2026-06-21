package com.fitness.activityservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "client.user-service")
public class UserServiceClientProperties {
    private String baseUrl = "http://USER-SERVICE";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration responseTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(3);
    private Duration writeTimeout = Duration.ofSeconds(3);
    private Duration requestTimeout = Duration.ofSeconds(4);
    private int retries = 2;
    private Duration retryBackoff = Duration.ofMillis(200);
}
