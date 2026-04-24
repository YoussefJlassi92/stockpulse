package com.stockpulse.gateway.api;

import com.stockpulse.gateway.domain.LoginRequest;
import com.stockpulse.gateway.domain.LoginResponse;
import com.stockpulse.gateway.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtUtil jwtUtil;
    private final long expiration;

    public AuthController(JwtUtil jwtUtil, @Value("${app.jwt.expiration}") long expiration) {
        this.jwtUtil = jwtUtil;
        this.expiration = expiration;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest request) {
        if (request.userId() == null || request.userId().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        String token = jwtUtil.generateToken(request.userId());
        log.debug("Issued token for userId={}", request.userId());
        return Mono.just(ResponseEntity.ok(new LoginResponse(token, request.userId(), expiration)));
    }

    @PostMapping("/validate")
    public Mono<ResponseEntity<Void>> validate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        String token = authHeader.substring(7);
        if (jwtUtil.validateToken(token)) {
            return Mono.just(ResponseEntity.ok().<Void>build());
        }
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
