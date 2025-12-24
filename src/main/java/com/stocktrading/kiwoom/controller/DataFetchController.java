package com.stocktrading.kiwoom.controller;

import com.stocktrading.kiwoom.domain.model.DataFetchProgress;
import com.stocktrading.kiwoom.dto.InvestorTradingRequest;
import com.stocktrading.kiwoom.service.EnhancedInvestorTradingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 대규모 데이터 조회 API 컨트롤러
 *
 * - 비동기 데이터 조회
 * - 진행률 실시간 모니터링
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/data-fetch")
@RequiredArgsConstructor
public class DataFetchController {

    private final EnhancedInvestorTradingService enhancedService;

    /**
     * 대규모 데이터 조회 (비동기)
     *
     * POST /api/v1/data-fetch/start
     * {
     *   "stk_cd": "005930",
     *   "dt": "20250101",
     *   "trde_tp": "0",
     *   "amt_qty_tp": "1",
     *   "unit_tp": "1000"
     * }
     *
     * @return 작업 ID
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startDataFetch(@RequestBody InvestorTradingRequest request) {
        log.info("대규모 데이터 조회 요청 - 종목: {}, 일자: {}", request.getStkCd(), request.getDt());

        // 비동기 실행
        CompletableFuture.runAsync(() -> {
            try {
                enhancedService.fetchLargeDataSafely(request);
            } catch (Exception e) {
                log.error("데이터 조회 실패: {}", e.getMessage(), e);
            }
        });

        Map<String, String> response = new HashMap<>();
        response.put("status", "STARTED");
        response.put("message", "데이터 조회가 시작되었습니다. 진행률은 /progress/{jobId}에서 확인하세요.");
        response.put("stockCode", request.getStkCd());

        return ResponseEntity.ok(response);
    }

    /**
     * 진행률 조회
     *
     * GET /api/v1/data-fetch/progress/{jobId}
     *
     * @param jobId 작업 ID
     * @return 진행률 정보
     */
    @GetMapping("/progress/{jobId}")
    public ResponseEntity<DataFetchProgress> getProgress(@PathVariable String jobId) {
        DataFetchProgress progress = enhancedService.getProgress(jobId);

        if (progress == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(progress);
    }

    /**
     * 종목별 진행률 조회 (간단 버전)
     *
     * GET /api/v1/data-fetch/progress-simple?stockCode=005930
     *
     * @param stockCode 종목코드
     * @return 진행률 요약
     */
    @GetMapping("/progress-simple")
    public ResponseEntity<Map<String, Object>> getProgressSimple(@RequestParam String stockCode) {
        // Redis에서 최근 작업 조회 로직 (생략)
        // 실제 구현 시 Redis에서 stockCode로 검색

        Map<String, Object> response = new HashMap<>();
        response.put("stockCode", stockCode);
        response.put("message", "진행률 조회는 jobId가 필요합니다");

        return ResponseEntity.ok(response);
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Enhanced Investor Trading Service");
        response.put("features", "Rate Limiter, Batch Processing, Retry Logic, Progress Monitoring");

        return ResponseEntity.ok(response);
    }
}
