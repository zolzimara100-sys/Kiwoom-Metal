package com.stocktrading.kiwoom.domain.port.out;

import com.stocktrading.kiwoom.domain.model.StockPrice;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * 주가 정보 저장소 Port
 */
public interface StockPricePort {

    /**
     * 캐시에서 주가 조회
     */
    Mono<Optional<StockPrice>> findCachedPrice(String stockCode);

    /**
     * 주가 캐시 저장
     */
    Mono<Void> cachePrice(StockPrice stockPrice);

    /**
     * API에서 주가 조회
     */
    Mono<StockPrice> fetchPriceFromApi(String stockCode, String token);
}
