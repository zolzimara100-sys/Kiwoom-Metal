package com.stocktrading.kiwoom.controller;

import com.stocktrading.kiwoom.domain.model.Token;
import com.stocktrading.kiwoom.domain.port.in.AuthenticateUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 인증 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticateUseCase authenticateUseCase;

    /**
     * 토큰 발급
     * POST /api/v1/auth/token
     */
    @PostMapping("/token")
    public Mono<ResponseEntity<TokenResponse>> issueToken() {
        log.info("토큰 발급 요청");

        return authenticateUseCase.issueToken()
                .map(token -> ResponseEntity.ok(TokenResponse.from(token)))
                .doOnSuccess(response -> log.info("토큰 발급 성공"))
                .doOnError(error -> log.error("토큰 발급 실패: {}", error.getMessage()));
    }

    /**
     * 토큰 상태 확인
     * GET /api/v1/auth/token/status
     */
    @GetMapping("/token/status")
    public Mono<ResponseEntity<TokenStatusResponse>> checkTokenStatus() {
        return authenticateUseCase.checkTokenStatus()
                .map(status -> ResponseEntity.ok(TokenStatusResponse.from(status)));
    }

    /**
     * 유효한 토큰 조회
     * GET /api/v1/auth/token/valid
     */
    @GetMapping("/token/valid")
    public Mono<ResponseEntity<TokenResponse>> getValidToken() {
        return authenticateUseCase.getValidToken()
                .map(token -> ResponseEntity.ok(TokenResponse.from(token)))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    /**
     * 토큰 삭제
     * DELETE /api/v1/auth/token
     */
    @DeleteMapping("/token")
    public Mono<ResponseEntity<Void>> clearToken() {
        log.info("토큰 삭제 요청");

        return authenticateUseCase.clearToken()
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .doOnSuccess(response -> log.info("토큰 삭제 완료"));
    }

    /**
     * 토큰 응답 DTO
     */
    public record TokenResponse(
            String accessToken,
            String tokenType,
            String expiresAt,
            String scope
    ) {
        public static TokenResponse from(Token token) {
            return new TokenResponse(
                    token.getAccessToken(),
                    token.getTokenType(),
                    token.getExpiresAt().toString(),
                    token.getScope()
            );
        }
    }

    /**
     * 토큰 상태 응답 DTO
     */
    public record TokenStatusResponse(
            boolean exists,
            boolean valid,
            boolean expiringSoon
    ) {
        public static TokenStatusResponse from(AuthenticateUseCase.TokenStatus status) {
            return new TokenStatusResponse(
                    status.exists(),
                    status.valid(),
                    status.expiringSoon()
            );
        }
    }
}
