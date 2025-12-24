package com.stocktrading.kiwoom.domain.port.out;

import com.stocktrading.kiwoom.domain.model.Token;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * 인증 토큰 저장소 Port
 */
public interface AuthPort {

    /**
     * 토큰 저장
     */
    Mono<Void> saveToken(Token token);

    /**
     * 토큰 조회
     */
    Mono<Optional<Token>> getToken();

    /**
     * 토큰 삭제
     */
    Mono<Void> deleteToken();

    /**
     * 토큰 존재 여부
     */
    Mono<Boolean> existsToken();
}
