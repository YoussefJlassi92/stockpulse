package com.stockpulse.gateway.api;

import com.stockpulse.gateway.domain.LoginRequest;
import com.stockpulse.gateway.domain.LoginResponse;
import com.stockpulse.gateway.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(AuthController.class)
@TestPropertySource(properties = "app.jwt.expiration=86400000")
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void login_withValidCredentials_returns200WithToken() {
        when(jwtUtil.generateToken("user-1")).thenReturn("mocked.jwt.token");

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("user-1", "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .value(resp -> {
                    assertThat(resp.token()).isEqualTo("mocked.jwt.token");
                    assertThat(resp.userId()).isEqualTo("user-1");
                    assertThat(resp.expiresIn()).isEqualTo(86_400_000L);
                });

        verify(jwtUtil).generateToken("user-1");
    }

    @Test
    void login_withBlankUserId_returns400() {
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("", "password"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void login_withBlankPassword_returns400() {
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("user-1", ""))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void login_withNullUserId_returns400() {
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(null, "password"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void validate_withValidBearerToken_returns200() {
        when(jwtUtil.validateToken("valid.token")).thenReturn(true);

        webTestClient.post()
                .uri("/api/v1/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void validate_withInvalidToken_returns401() {
        when(jwtUtil.validateToken("bad.token")).thenReturn(false);

        webTestClient.post()
                .uri("/api/v1/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer bad.token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void validate_withMissingAuthorizationHeader_returns401() {
        webTestClient.post()
                .uri("/api/v1/auth/validate")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void validate_withNonBearerHeader_returns401() {
        webTestClient.post()
                .uri("/api/v1/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
