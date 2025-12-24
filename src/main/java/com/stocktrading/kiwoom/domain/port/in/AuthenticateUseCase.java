package com.stocktrading.kiwoom.domain.port.in;

import com.stocktrading.kiwoom.domain.model.Token;
import reactor.core.publisher.Mono;

/**
 * 인증 Use Case
 */
public interface AuthenticateUseCase {

    /**
     * 토큰 발급 (비동기)
     */
    Mono<Token> issueToken();

    /**
     * 토큰 발급 (동기)
     */
    Token issueTokenSync();

    /**
     * 유효한 토큰 조회 (자동 갱신)
     */
    Mono<Token> getValidToken();

    /**
     * 토큰 상태 확인
     */
    Mono<TokenStatus> checkTokenStatus();

    /**
     * 토큰 삭제
     */
    Mono<Void> clearToken();

    /**
     * 토큰 상태
     */
    record TokenStatus(
            boolean exists,
            boolean valid,
            boolean expiringSoon
    ) {}
}
