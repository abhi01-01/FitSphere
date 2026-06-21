package com.fitness.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class HttpsEnforcementWebFilter implements WebFilter {
    private final GatewaySecurityProperties securityProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!securityProperties.requireHttps()) {
            return chain.filter(exchange);
        }

        String forwardedProto = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");
        boolean secure = "https".equalsIgnoreCase(forwardedProto)
                || "https".equalsIgnoreCase(exchange.getRequest().getURI().getScheme());

        if (secure) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.UPGRADE_REQUIRED);
        return exchange.getResponse().setComplete();
    }
}
