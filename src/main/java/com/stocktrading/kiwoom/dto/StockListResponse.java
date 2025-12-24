package com.stocktrading.kiwoom.dto;

import lombok.Builder;

import java.util.Map;

/**
 * 종목 리스트 조회 응답 DTO
 */
@Builder
public record StockListResponse(
        int totalCount,
        Map<String, Integer> marketCounts,
        long executionTimeMs,
        boolean success,
        String message
) {
    public static StockListResponse from(
            com.stocktrading.kiwoom.domain.port.in.FetchStockListUseCase.FetchResult result
    ) {
        return StockListResponse.builder()
                .totalCount(result.totalCount())
                .marketCounts(result.marketCounts())
                .executionTimeMs(result.executionTimeMs())
                .success(result.success())
                .message(result.message())
                .build();
    }
}
