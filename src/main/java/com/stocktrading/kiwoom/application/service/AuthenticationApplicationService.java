package com.stocktrading.kiwoom.application.service;

import com.stocktrading.kiwoom.domain.model.Token;
import com.stocktrading.kiwoom.domain.port.in.AuthenticateUseCase;
import com.stocktrading.kiwoom.domain.port.out.AuthPort;
import com.stocktrading.kiwoom.domain.port.out.KiwoomApiPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 인증 Application Service (Use Case 구현)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationApplicationService implements AuthenticateUseCase {

    private final AuthPort authPort;
    private final KiwoomApiPort apiPort;

    // 직접 키 입력
    private static final String APP_KEY = "CPMeK4v9_31_OHONendVOsD1riEAPRr20I55ri9VWbY";
    private static final String APP_SECRET = "UnM8nInWzsoHW2z07pnOGahSUDtkBetfbBKOjUVG16Y";

    private String appKey = APP_KEY;
    private String appSecret = APP_SECRET;

    private static final long TOKEN_EXPIRY_BUFFER_MINUTES = 5;

    @Override
    public Mono<Token> issueToken() {
        log.info("토큰 발급 요청");

        return apiPort.issueToken(appKey, appSecret)
                .map(this::convertToToken)
                .flatMap(token -> authPort.saveToken(token).thenReturn(token))
                .doOnSuccess(token -> log.info("토큰 발급 및 저장 완료"))
                .doOnError(error -> log.error("토큰 발급 실패: {}", error.getMessage()));
    }

    @Override
    public Token issueTokenSync() {
        return issueToken().block();
    }

    @Override
    public Mono<Token> getValidToken() {
        return authPort.getToken()
                .flatMap(tokenOpt -> {
                    if (tokenOpt.isEmpty()) {
                        log.info("토큰이 없습니다. 새로 발급합니다.");
                        return issueToken();
                    }

                    Token token = tokenOpt.get();
                    if (token.isExpiringSoon(TOKEN_EXPIRY_BUFFER_MINUTES)) {
                        log.info("토큰이 곧 만료됩니다. 새로 발급합니다.");
                        return issueToken();
                    }

                    return Mono.just(token);
                });
    }

    @Override
    public Mono<TokenStatus> checkTokenStatus() {
        return authPort.getToken()
                .map(tokenOpt -> {
                    if (tokenOpt.isEmpty()) {
                        return new TokenStatus(false, false, false);
                    }

                    Token token = tokenOpt.get();
                    boolean valid = token.isValid();
                    boolean expiringSoon = token.isExpiringSoon(TOKEN_EXPIRY_BUFFER_MINUTES);

                    return new TokenStatus(true, valid, expiringSoon);
                });
    }

    @Override
    public Mono<Void> clearToken() {
        log.info("토큰 삭제 요청");
        return authPort.deleteToken()
                .doOnSuccess(v -> log.info("토큰 삭제 완료"));
    }

    /**
     * API 응답을 도메인 모델로 변환
     */
    private Token convertToToken(KiwoomApiPort.TokenResponse response) {
        // expiresIn이 null인 경우 기본값 사용 (24시간 = 86400초)
        int expiresInSeconds = response.expiresIn() != null ? response.expiresIn() : 86400;

        if (response.expiresIn() == null) {
            log.warn("expiresIn이 null입니다. 기본값 24시간(86400초)을 사용합니다.");
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresInSeconds);

        return Token.builder()
                .accessToken(response.accessToken())
                .tokenType(response.tokenType())
                .expiresAt(expiresAt)
                .scope(response.scope())
                .build();
    }
}
