package com.fitness.aiservice.service;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class GeminiService {

    private final WebClient webClient;
    private final String geminiApiKey;
    private final String geminiModel;
    private final Duration requestTimeout;
    private final int retries;
    private final Duration retryBackoff;

    public GeminiService(
            WebClient.Builder webClientBuilder,
            @Value("${gemini.api.base-url}") String geminiApiBaseUrl,
            @Value("${gemini.api.key}") String geminiApiKey,
            @Value("${gemini.api.model}") String geminiModel,
            @Value("${gemini.api.connect-timeout:PT2S}") Duration connectTimeout,
            @Value("${gemini.api.response-timeout:PT10S}") Duration responseTimeout,
            @Value("${gemini.api.read-timeout:PT10S}") Duration readTimeout,
            @Value("${gemini.api.write-timeout:PT10S}") Duration writeTimeout,
            @Value("${gemini.api.request-timeout:PT12S}") Duration requestTimeout,
            @Value("${gemini.api.retries:2}") int retries,
            @Value("${gemini.api.retry-backoff:PT0.5S}") Duration retryBackoff) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectTimeout.toMillis()))
                .responseTimeout(responseTimeout)
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeout.toMillis(), TimeUnit.MILLISECONDS)));

        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(geminiApiBaseUrl)
                .build();
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
        this.requestTimeout = requestTimeout;
        this.retries = retries;
        this.retryBackoff = retryBackoff;
    }

    public String getAnswer(String question) {
        if (!StringUtils.hasText(geminiApiKey)) {
            throw new GeminiServiceException("GEMINI_API_KEY is not configured", null);
        }

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[]{
                                Map.of("text", question)
                        })
                }
        );

        try {
            return webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", geminiApiKey)
                            .build(geminiModel))
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(requestTimeout)
                    .retryWhen(Retry.backoff(retries, retryBackoff)
                            .filter(this::isRetryable)
                            .maxBackoff(Duration.ofSeconds(4))
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                    .block();
        } catch (Exception exception) {
            throw new GeminiServiceException(toFailureMessage(exception), exception);
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

    private String toFailureMessage(Throwable throwable) {
        Throwable rootCause = rootCause(throwable);
        if (rootCause instanceof TimeoutException
                || rootCause instanceof io.netty.handler.timeout.ReadTimeoutException) {
            return "Gemini API timed out";
        }
        if (throwable instanceof WebClientResponseException responseException) {
            return "Gemini API returned HTTP " + responseException.getStatusCode().value();
        }
        if (throwable instanceof WebClientRequestException) {
            return "Gemini API request failed";
        }
        return "Gemini API call failed";
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
