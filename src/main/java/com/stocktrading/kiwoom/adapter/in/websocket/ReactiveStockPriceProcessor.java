package com.stocktrading.kiwoom.adapter.in.websocket;

import com.stocktrading.kiwoom.domain.port.out.CachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * 실시간 주가 데이터 Reactive 처리 파이프라인
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveStockPriceProcessor {

    private final CachePort cachePort;

    /**
     * 실시간 주가 데이터 처리 파이프라인
     *
     * 1. Redis에 즉시 캐싱 (TTL: 5초)
     * 2. Redis Pub/Sub으로 이벤트 발행
     * 3. 배치 집계 큐에 추가
     */
    public Mono<Void> process(Map<String, Object> priceData) {
        String stockCode = (String) priceData.get("stock_code");
        log.debug("실시간 주가 처리 시작 - 종목: {}", stockCode);

        return Mono.just(priceData)
                .flatMap(this::cacheRealTimePrice)
                .flatMap(this::publishPriceEvent)
                .flatMap(this::addToBatchQueue)
                .doOnSuccess(v -> log.debug("실시간 주가 처리 완료 - 종목: {}", stockCode))
                .doOnError(error -> log.error("실시간 주가 처리 실패 - 종목: {}, 오류: {}",
                        stockCode, error.getMessage()));
    }

    /**
     * 1단계: Redis 캐싱 (실시간 조회용)
     */
    private Mono<Map<String, Object>> cacheRealTimePrice(Map<String, Object> priceData) {
        String stockCode = (String) priceData.get("stock_code");
        String cacheKey = "realtime:price:" + stockCode;

        return cachePort.set(cacheKey, priceData, Duration.ofSeconds(5))
                .thenReturn(priceData);
    }

    /**
     * 2단계: Redis Pub/Sub 이벤트 발행
     */
    private Mono<Map<String, Object>> publishPriceEvent(Map<String, Object> priceData) {
        String stockCode = (String) priceData.get("stock_code");
        String channel = "stock:price:" + stockCode;

        String message = String.format("{\"stock_code\":\"%s\",\"price\":%s,\"timestamp\":%s}",
                stockCode, priceData.get("price"), priceData.get("timestamp"));

        return cachePort.publish(channel, message)
                .thenReturn(priceData);
    }

    /**
     * 3단계: 배치 집계 큐에 추가
     */
    private Mono<Void> addToBatchQueue(Map<String, Object> priceData) {
        String stockCode = (String) priceData.get("stock_code");
        String queueKey = "batch:queue:price";
        String field = stockCode + ":" + System.currentTimeMillis();

        return cachePort.hSet(queueKey, field, priceData);
    }
}
