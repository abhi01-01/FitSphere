package com.fitness.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record GatewaySecurityProperties(
        String allowedOrigins,
        String internalToken,
        boolean requireHttps) {

    public String resolvedAllowedOrigins() {
        return allowedOrigins != null && !allowedOrigins.isBlank()
                ? allowedOrigins
                : "http://localhost:5173";
    }

    public String resolvedInternalToken() {
        return internalToken != null && !internalToken.isBlank()
                ? internalToken
                : "fitsphere-internal-dev-token";
    }
}
