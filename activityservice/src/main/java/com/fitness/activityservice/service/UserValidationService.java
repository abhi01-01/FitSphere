package com.fitness.activityservice.service;

import com.fitness.activityservice.config.UserServiceClientProperties;
import com.fitness.activityservice.error.BadRequestException;
import com.fitness.activityservice.error.UpstreamServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserValidationService {
    private final WebClient userServiceWebClient;
    private final UserServiceClientProperties properties;

    public boolean validateUser(String userId) {
        log.info("Calling User Validation API for userId: {}", userId);
        try{
            Boolean response = userServiceWebClient.get()
                    .uri("/api/users/{userId}/validate", userId)
                    .header("X-Correlation-ID", currentCorrelationId())
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .timeout(properties.getRequestTimeout())
                    .retryWhen(Retry.backoff(properties.getRetries(), properties.getRetryBackoff())
                            .filter(this::isRetryable)
                            .maxBackoff(Duration.ofSeconds(2))
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                    .block();
            if (response == null) {
                throw new UpstreamServiceException("User validation service returned no response");
            }
            return response;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new BadRequestException("Invalid user validation request for userId: " + userId);
            }
            throw new UpstreamServiceException("User validation service is unavailable");
        } catch (Exception e) {
            if (isTimeoutOrNetworkFailure(e)) {
                throw new UpstreamServiceException("User validation service timed out");
            }
            throw new UpstreamServiceException("User validation service is unavailable");
        }
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof TimeoutException || throwable instanceof WebClientRequestException) {
            return true;
        }
        if (throwable instanceof WebClientResponseException responseException) {
            return responseException.getStatusCode().is5xxServerError()
                    || responseException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
        }
        return false;
    }

    private boolean isTimeoutOrNetworkFailure(Throwable throwable) {
        if (throwable instanceof TimeoutException || throwable instanceof WebClientRequestException) {
            return true;
        }
        return throwable.getCause() instanceof TimeoutException
                || throwable.getCause() instanceof WebClientRequestException;
    }

    private String currentCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : "activityservice-no-correlation";
    }
}
