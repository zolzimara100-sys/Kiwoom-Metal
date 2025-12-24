package com.stocktrading.kiwoom.domain.port.in;

import com.stocktrading.kiwoom.domain.model.StockInfo;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 종목 리스트 조회 UseCase
 */
public interface FetchStockListUseCase {

    /**
     * 특정 시장의 종목 리스트 조회
     *
     * @param marketType 시장구분 (0:코스피, 10:코스닥, 8:ETF 등)
     * @return 조회 결과 요약
     */
    Mono<FetchResult> fetchByMarket(String marketType);

    /**
     * 모든 시장의 종목 리스트 조회 및 DB 갱신
     * 기존 데이터 전체 삭제 후 새 데이터 저장
     *
     * @return 조회 결과 요약
     */
    Mono<FetchResult> fetchAllMarketsAndRefresh();

    /**
     * 조회 결과 DTO
     */
    record FetchResult(
            int totalCount,
            Map<String, Integer> marketCounts,
            long executionTimeMs,
            boolean success,
            String message
    ) {
        public static FetchResult success(int totalCount, Map<String, Integer> marketCounts, long executionTimeMs) {
            return new FetchResult(totalCount, marketCounts, executionTimeMs, true, "종목 정보 갱신 완료");
        }

        public static FetchResult failure(String message) {
            return new FetchResult(0, Map.of(), 0, false, message);
        }
    }
}
