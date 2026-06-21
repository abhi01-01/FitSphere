package com.fitness.activityservice.security;

import com.fitness.activityservice.error.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@Slf4j
public class InternalRequestInterceptor implements HandlerInterceptor {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Value("${app.internal.token:fitsphere-internal-dev-token}")
    private String internalToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        String presentedToken = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (!StringUtils.hasText(presentedToken) || !internalToken.equals(presentedToken)) {
            log.warn("audit_event=internal_auth_rejected correlation_id={} path={}", correlationId, request.getRequestURI());
            throw new UnauthorizedException("Internal service authentication required");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove("correlationId");
    }
}
