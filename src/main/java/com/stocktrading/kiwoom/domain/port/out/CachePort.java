package com.stocktrading.kiwoom.domain.port.out;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

/**
 * 캐시 저장소 Port (Redis)
 */
public interface CachePort {

    /**
     * 값 저장
     */
    <T> Mono<Void> set(String key, T value);

    /**
     * 값 저장 (TTL 포함)
     */
    <T> Mono<Void> set(String key, T value, Duration ttl);

    /**
     * 값 조회
     */
    <T> Mono<Optional<T>> get(String key, Class<T> type);

    /**
     * 값 삭제
     */
    Mono<Boolean> delete(String key);

    /**
     * 존재 여부 확인
     */
    Mono<Boolean> exists(String key);

    /**
     * TTL 설정
     */
    Mono<Boolean> expire(String key, Duration ttl);

    /**
     * Hash 저장
     */
    <T> Mono<Void> hSet(String key, String field, T value);

    /**
     * Hash 조회
     */
    <T> Mono<Optional<T>> hGet(String key, String field, Class<T> type);

    /**
     * Hash 삭제
     */
    Mono<Boolean> hDelete(String key, String... fields);

    /**
     * Pub/Sub 발행
     */
    Mono<Long> publish(String channel, String message);

    /**
     * 증가
     */
    Mono<Long> increment(String key);

    /**
     * 증가 (TTL 포함)
     */
    Mono<Long> increment(String key, Duration ttl);
}
