package com.stocktrading.kiwoom.controller;

import com.stocktrading.kiwoom.domain.model.StockInfo;
import com.stocktrading.kiwoom.domain.port.in.FetchStockListUseCase;
import com.stocktrading.kiwoom.domain.port.out.StockListPort;
import com.stocktrading.kiwoom.dto.StockListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 종목 리스트 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stock-list")
@RequiredArgsConstructor
public class StockListController {

    private final FetchStockListUseCase fetchStockListUseCase;
    private final StockListPort stockListPort;

    /**
     * 전체 시장 종목 리스트 갱신
     * GET 요청으로 간단히 호출 가능
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<StockListResponse>> refreshAllMarkets() {
        log.info("전체 종목 리스트 갱신 API 호출");

        return fetchStockListUseCase.fetchAllMarketsAndRefresh()
                .map(result -> {
                    StockListResponse response = StockListResponse.from(result);
                    
                    if (result.success()) {
                        log.info("전체 종목 리스트 갱신 성공 - 총 {}건", result.totalCount());
                        return ResponseEntity.ok(response);
                    } else {
                        log.error("전체 종목 리스트 갱신 실패 - {}", result.message());
                        return ResponseEntity.internalServerError().body(response);
                    }
                })
                .onErrorResume(e -> {
                    log.error("전체 종목 리스트 갱신 API 오류", e);
                    StockListResponse errorResponse = StockListResponse.builder()
                            .success(false)
                            .message("API 오류: " + e.getMessage())
                            .build();
                    return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
                });
    }

    /**
     * 특정 시장 종목 리스트 조회
     */
    @GetMapping("/market/{marketType}")
    public Mono<ResponseEntity<StockListResponse>> fetchByMarket(
            @PathVariable String marketType
    ) {
        log.info("시장별 종목 리스트 조회 API 호출 - 시장: {}", marketType);

        return fetchStockListUseCase.fetchByMarket(marketType)
                .map(result -> {
                    StockListResponse response = StockListResponse.from(result);

                    if (result.success()) {
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.badRequest().body(response);
                    }
                });
    }

    /**
     * 종목명으로 검색 (LIKE 검색)
     * GET /api/v1/stock-list/search?keyword={name}
     */
    @GetMapping("/search")
    public ResponseEntity<List<StockSearchDto>> searchStocks(
            @RequestParam String keyword
    ) {
        log.info("종목 검색 API 호출 - 키워드: {}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<StockInfo> stocks = stockListPort.searchByName(keyword.trim());
        // 이미 Adapter에서 시장명 우선 정렬 적용됨. 여기서는 그대로 전달.
        List<StockSearchDto> result = stocks.stream()
            .map(stock -> new StockSearchDto(
                stock.getCode(),
                stock.getName(),
                stock.getMarketName(),
                stock.getSector()
            ))
            .collect(Collectors.toList());

        log.info("종목 검색 결과 - {}건", result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * KOSPI200 종목 리스트 조회 (tb_stock_list_meta에서)
     * GET /api/v1/stock-list/kospi200
     */
    @GetMapping("/kospi200")
    public ResponseEntity<List<StockSearchDto>> getKospi200Stocks() {
        log.info("KOSPI200 종목 리스트 조회 API 호출");

        List<StockInfo> stocks = stockListPort.findAllKospi200Stocks();
        List<StockSearchDto> result = stocks.stream()
            .map(stock -> new StockSearchDto(
                stock.getCode(),
                stock.getName(),
                "KOSPI200",  // tb_stock_list_meta에는 marketName이 없으므로 고정값 사용
                stock.getSector()
            ))
            .collect(Collectors.toList());

        log.info("KOSPI200 종목 리스트 조회 완료 - {}건", result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 종목 검색 결과 DTO
     */
    public record StockSearchDto(
            String code,
            String name,
            String marketName,
            String sector
    ) {}
}
