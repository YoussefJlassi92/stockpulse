package com.stockpulse.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final List<String> EXCLUDED_PREFIXES = List.of("/api/v1/auth/", "/actuator/", "/ws");

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Missing or malformed Authorization header for path={}", path);
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            log.debug("Invalid JWT token for path={}", path);
            return unauthorized(exchange);
        }

        String userId = jwtUtil.extractUserId(token);
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
