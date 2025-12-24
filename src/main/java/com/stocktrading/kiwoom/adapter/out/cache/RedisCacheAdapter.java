package com.stocktrading.kiwoom.adapter.out.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktrading.kiwoom.domain.port.out.CachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 캐시 Adapter
 * CachePort 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheAdapter implements CachePort {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public <T> Mono<Void> set(String key, T value) {
        log.debug("Redis SET - key: {}", key);
        return reactiveRedisTemplate.opsForValue()
                .set(key, value)
                .doOnSuccess(result -> log.debug("Redis SET 성공 - key: {}, result: {}", key, result))
                .doOnError(error -> log.error("Redis SET 실패 - key: {}, error: {}", key, error.getMessage()))
                .then();
    }

    @Override
    public <T> Mono<Void> set(String key, T value, Duration ttl) {
        log.debug("Redis SET with TTL - key: {}, ttl: {}", key, ttl);
        return reactiveRedisTemplate.opsForValue()
                .set(key, value, ttl)
                .doOnSuccess(result -> log.debug("Redis SET 성공 - key: {}, ttl: {}", key, ttl))
                .doOnError(error -> log.error("Redis SET 실패 - key: {}, error: {}", key, error.getMessage()))
                .then();
    }

    @Override
    public <T> Mono<Optional<T>> get(String key, Class<T> type) {
        log.debug("Redis GET - key: {}, type: {}", key, type.getSimpleName());
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .map(value -> {
                    try {
                        T result = objectMapper.convertValue(value, type);
                        log.debug("Redis GET 성공 - key: {}", key);
                        return Optional.of(result);
                    } catch (Exception e) {
                        log.error("Redis GET 변환 실패 - key: {}, error: {}", key, e.getMessage());
                        return Optional.<T>empty();
                    }
                })
                .defaultIfEmpty(Optional.empty())
                .doOnError(error -> log.error("Redis GET 실패 - key: {}, error: {}", key, error.getMessage()));
    }

    @Override
    public Mono<Boolean> delete(String key) {
        log.debug("Redis DELETE - key: {}", key);
        return reactiveRedisTemplate.opsForValue()
                .delete(key)
                .doOnSuccess(result -> log.debug("Redis DELETE 성공 - key: {}, result: {}", key, result))
                .doOnError(error -> log.error("Redis DELETE 실패 - key: {}, error: {}", key, error.getMessage()));
    }

    @Override
    public Mono<Boolean> exists(String key) {
        log.debug("Redis EXISTS - key: {}", key);
        return reactiveRedisTemplate.hasKey(key)
                .doOnSuccess(result -> log.debug("Redis EXISTS 결과 - key: {}, exists: {}", key, result))
                .doOnError(error -> log.error("Redis EXISTS 실패 - key: {}, error: {}", key, error.getMessage()));
    }

    @Override
    public Mono<Boolean> expire(String key, Duration ttl) {
        log.debug("Redis EXPIRE - key: {}, ttl: {}", key, ttl);
        return reactiveRedisTemplate.expire(key, ttl)
                .doOnSuccess(result -> log.debug("Redis EXPIRE 성공 - key: {}, ttl: {}", key, ttl))
                .doOnError(error -> log.error("Redis EXPIRE 실패 - key: {}, error: {}", key, error.getMessage()));
    }

    @Override
    public <T> Mono<Void> hSet(String key, String field, T value) {
        log.debug("Redis HSET - key: {}, field: {}", key, field);
        return reactiveRedisTemplate.opsForHash()
                .put(key, field, value)
                .doOnSuccess(result -> log.debug("Redis HSET 성공 - key: {}, field: {}", key, field))
                .doOnError(error -> log.error("Redis HSET 실패 - key: {}, field: {}, error: {}", key, field, error.getMessage()))
                .then();
    }

    @Override
    public <T> Mono<Optional<T>> hGet(String key, String field, Class<T> type) {
        log.debug("Redis HGET - key: {}, field: {}", key, field);
        return reactiveRedisTemplate.opsForHash()
                .get(key, field)
                .map(value -> {
                    try {
                        T result = objectMapper.convertValue(value, type);
                        log.debug("Redis HGET 성공 - key: {}, field: {}", key, field);
                        return Optional.of(result);
                    } catch (Exception e) {
                        log.error("Redis HGET 변환 실패 - key: {}, field: {}, error: {}", key, field, e.getMessage());
                        return Optional.<T>empty();
                    }
                })
                .defaultIfEmpty(Optional.empty())
                .doOnError(error -> log.error("Redis HGET 실패 - key: {}, field: {}, error: {}", key, field, error.getMessage()));
    }

    @Override
    public Mono<Boolean> hDelete(String key, String... fields) {
        log.debug("Redis HDEL - key: {}, fields: {}", key, fields);
        return reactiveRedisTemplate.opsForHash()
                .remove(key, (Object[]) fields)
                .map(count -> count > 0)
                .doOnSuccess(result -> log.debug("Redis HDEL 성공 - key: {}, deleted: {}", key, result))
                .doOnError(error -> log.error("Redis HDEL 실패 - key: {}, error: {}", key, error.getMessage()));
    }

    @Override
    public Mono<Long> publish(String channel, String message) {
        log.debug("Redis PUBLISH - channel: {}, message: {}", channel, message);
        return reactiveRedisTemplate.convertAndSend(channel, message)
                .doOnSuccess(count -> log.debug("Redis PUBLISH 성공 - channel: {}, subscribers: {}", channel, count))
                .doOnError(error -> log.error("Redis PUBLISH 실패 - channel: {}, error: {}", channel, error.getMessage()));
    }

    @Override
    public Mono<Long> increment(String key) {
        log.debug("Redis INCR - key: {}", key);
        return reactiveRedisTemplate.opsForValue()
                .increment(key)
                .doOnSuccess(result -> log.debug("Redis INCR 성공 - key: {}, value: {}", key, result))
                .doOnError(error -> log.error("Redis INCR 실패 - key: {}, error: {}", key, error.getMessage()));
    }

    @Override
    public Mono<Long> increment(String key, Duration ttl) {
        log.debug("Redis INCR with TTL - key: {}, ttl: {}", key, ttl);
        return reactiveRedisTemplate.opsForValue()
                .increment(key)
                .flatMap(value -> reactiveRedisTemplate.expire(key, ttl).thenReturn(value))
                .doOnSuccess(result -> log.debug("Redis INCR with TTL 성공 - key: {}, value: {}, ttl: {}", key, result, ttl))
                .doOnError(error -> log.error("Redis INCR with TTL 실패 - key: {}, error: {}", key, error.getMessage()));
    }
}
