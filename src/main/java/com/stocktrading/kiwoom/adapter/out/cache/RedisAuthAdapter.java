package com.stocktrading.kiwoom.adapter.out.cache;

import com.stocktrading.kiwoom.domain.model.Token;
import com.stocktrading.kiwoom.domain.port.out.AuthPort;
import com.stocktrading.kiwoom.domain.port.out.CachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Redis 기반 인증 토큰 저장소 Adapter
 * AuthPort 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAuthAdapter implements AuthPort {

    private final CachePort cachePort;

    private static final String TOKEN_KEY = "kiwoom:auth:token";

    @Override
    public Mono<Void> saveToken(Token token) {
        log.info("토큰 Redis 저장 - 만료시각: {}", token.getExpiresAt());

        // 만료 시각까지의 TTL 계산
        Duration ttl = Duration.between(LocalDateTime.now(), token.getExpiresAt());

        return cachePort.set(TOKEN_KEY, token, ttl)
                .doOnSuccess(v -> log.info("토큰 Redis 저장 완료 - TTL: {}초", ttl.getSeconds()))
                .doOnError(error -> log.error("토큰 Redis 저장 실패: {}", error.getMessage()));
    }

    @Override
    public Mono<Optional<Token>> getToken() {
        log.debug("토큰 Redis 조회");
        return cachePort.get(TOKEN_KEY, Token.class)
                .doOnSuccess(tokenOpt -> {
                    if (tokenOpt.isPresent()) {
                        log.debug("토큰 Redis 조회 성공");
                    } else {
                        log.debug("토큰이 Redis에 없음");
                    }
                })
                .doOnError(error -> log.error("토큰 Redis 조회 실패: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> deleteToken() {
        log.info("토큰 Redis 삭제");
        return cachePort.delete(TOKEN_KEY)
                .doOnSuccess(deleted -> log.info("토큰 Redis 삭제 완료 - deleted: {}", deleted))
                .doOnError(error -> log.error("토큰 Redis 삭제 실패: {}", error.getMessage()))
                .then();
    }

    @Override
    public Mono<Boolean> existsToken() {
        log.debug("토큰 존재 여부 확인");
        return cachePort.exists(TOKEN_KEY)
                .doOnSuccess(exists -> log.debug("토큰 존재 여부: {}", exists))
                .doOnError(error -> log.error("토큰 존재 여부 확인 실패: {}", error.getMessage()));
    }
}
