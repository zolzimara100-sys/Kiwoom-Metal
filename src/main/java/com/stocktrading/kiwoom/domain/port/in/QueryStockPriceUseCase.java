package com.stocktrading.kiwoom.domain.port.in;

import com.stocktrading.kiwoom.domain.model.StockPrice;
import reactor.core.publisher.Mono;

/**
 * 주가 조회 Use Case
 */
public interface QueryStockPriceUseCase {

    /**
     * 실시간 주가 조회 (캐시 우선)
     */
    Mono<StockPrice> queryRealTimePrice(String stockCode);

    /**
     * 주가 조회 (API 호출 강제)
     */
    Mono<StockPrice> queryPriceFromApi(String stockCode);
}
