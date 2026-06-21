package com.fitness.activityservice.config;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(UserServiceClientProperties.class)
public class WebClientConfig {
    @Value("${app.internal.token:fitsphere-internal-dev-token}")
    private String internalToken;

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder(UserServiceClientProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(properties.getConnectTimeout().toMillis()))
                .responseTimeout(properties.getResponseTimeout())
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(properties.getWriteTimeout().toMillis(), TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean
    public WebClient userServiceWebClient(WebClient.Builder webClientBuilder, UserServiceClientProperties properties) {
        return webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }
}
