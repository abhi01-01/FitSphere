package com.fitness.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CorrelationIdWebFilter implements WebFilter {
    public static final String HEADER_NAME = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(HEADER_NAME, correlationId)
                .build();
        exchange.getResponse().getHeaders().set(HEADER_NAME, correlationId);
        exchange.getAttributes().put(HEADER_NAME, correlationId);

        log.debug("correlation_id={} method={} path={}",
                correlationId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath().value());

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}
