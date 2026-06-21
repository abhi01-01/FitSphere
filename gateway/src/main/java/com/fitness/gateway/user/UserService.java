package com.fitness.gateway.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final WebClient userServiceWebClient;
    private final UserServiceClientProperties properties;

    public Mono<Boolean> validateUser(String userId, String correlationId) {
        log.info("Calling User Validation API for userId: {}", userId);
        return applyResilience(userServiceWebClient.get()
                .uri("/api/users/{userId}/validate", userId)
                .header("X-Correlation-ID", correlationId)
                .retrieve()
                .bodyToMono(Boolean.class))
                .onErrorMap(throwable -> mapValidationException(throwable, userId));
    }

    public Mono<UserResponse> registerUser(RegisterRequest request, String correlationId) {
        log.info("Calling User Registration API for email: {}", request.getEmail());
        return applyResilience(userServiceWebClient.post()
                .uri("/api/users/register")
                .header("X-Correlation-ID", correlationId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserResponse.class))
                .onErrorMap(this::mapRegistrationException);
    }

    private <T> Mono<T> applyResilience(Mono<T> request) {
        return request
                .timeout(properties.getRequestTimeout())
                .retryWhen(Retry.backoff(properties.getRetries(), properties.getRetryBackoff())
                        .filter(this::isRetryable)
                        .maxBackoff(Duration.ofSeconds(2))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
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

    private Throwable mapValidationException(Throwable throwable, String userId) {
        if (throwable instanceof ResponseStatusException) {
            return throwable;
        }
        if (throwable instanceof WebClientResponseException responseException
                && responseException.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid user validation request for userId: " + userId);
        }
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "User service validation endpoint is unavailable");
    }

    private Throwable mapRegistrationException(Throwable throwable) {
        if (throwable instanceof ResponseStatusException) {
            return throwable;
        }
        if (throwable instanceof WebClientResponseException responseException
                && responseException.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User registration request was rejected");
        }
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "User service registration endpoint is unavailable");
    }
}
