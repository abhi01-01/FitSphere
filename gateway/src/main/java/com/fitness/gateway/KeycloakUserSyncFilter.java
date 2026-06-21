package com.fitness.gateway;

import com.fitness.gateway.user.RegisterRequest;
import com.fitness.gateway.user.UserService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements WebFilter {
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    public static final String USER_ID_HEADER = "X-User-ID";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    private final UserService userService;
    private final GatewaySecurityProperties securityProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(authentication -> syncAndForward(exchange, chain, authentication))
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<Void> syncAndForward(
            ServerWebExchange exchange,
            WebFilterChain chain,
            JwtAuthenticationToken authentication) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(authentication.getToken().getClaimAsString("email"));
        registerRequest.setKeycloakId(authentication.getToken().getSubject());
        registerRequest.setFirstName(authentication.getToken().getClaimAsString("given_name"));
        registerRequest.setLastName(authentication.getToken().getClaimAsString("family_name"));

        String userId = registerRequest.getKeycloakId();
        if (userId == null) {
            return chain.filter(exchange);
        }

        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationIdWebFilter.HEADER_NAME);
        String roleHeader = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .sorted()
                .collect(Collectors.joining(","));

        return userService.validateUser(userId, correlationId)
                .flatMap(exists -> {
                    if (exists) {
                        log.info("audit_event=user_sync_skip correlation_id={} keycloak_id={}", correlationId, userId);
                        return Mono.empty();
                    }

                    log.info("audit_event=user_sync_register correlation_id={} keycloak_id={} email={}",
                            correlationId,
                            userId,
                            registerRequest.getEmail());
                    return userService.registerUser(registerRequest, correlationId).then();
                })
                .then(Mono.defer(() -> {
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header(USER_ID_HEADER, userId)
                            .header(USER_ROLES_HEADER, roleHeader)
                            .header(INTERNAL_TOKEN_HEADER, securityProperties.resolvedInternalToken())
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                }));
    }
}
