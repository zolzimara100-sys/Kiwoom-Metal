package com.stocktrading.kiwoom.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stocktrading.kiwoom.domain.model.InvestorChart;
import com.stocktrading.kiwoom.domain.port.in.FetchInvestorChartUseCase;
import com.stocktrading.kiwoom.domain.port.in.FetchInvestorChartUseCase.FetchInvestorChartCommand;
import com.stocktrading.kiwoom.domain.port.in.QueryInvestorChartUseCase;
import com.stocktrading.kiwoom.dto.InstitutionTrendResponse;
import com.stocktrading.kiwoom.dto.InvestorChartRequest;
import com.stocktrading.kiwoom.dto.InvestorChartResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 종목별 투자자 기관별 차트 API Controller (ka10060)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/investor-chart")
@RequiredArgsConstructor
public class InvestorChartController {

        private final FetchInvestorChartUseCase fetchUseCase;
        private final QueryInvestorChartUseCase queryUseCase;

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

        // ============================================
        // Fetch APIs (운영서버에서 데이터 수집)
        // ============================================

        /**
         * 투자자 차트 데이터 수집 (API 호출 → DB 저장)
         * POST /api/v1/investor-chart/fetch
         */
        @PostMapping("/fetch")
        public Mono<ResponseEntity<InvestorChartResponse>> fetchInvestorChart(
                        @RequestBody InvestorChartRequest request) {

                log.info("투자자 차트 데이터 수집 요청 - 종목: {}, 일자: {}", request.getStkCd(), request.getDt());

                try {
                        LocalDate date = LocalDate.parse(request.getDt(), DATE_FORMATTER);
                        FetchInvestorChartCommand command = new FetchInvestorChartCommand(
                                        request.getStkCd(),
                                        date,
                                        request.getAmtQtyTp(),
                                        request.getTrdeTp(),
                                        InvestorChart.UnitType.fromCode(request.getUnitTp()));

                        return fetchUseCase.fetchByStock(command)
                                        .collectList()
                                        .map(charts -> ResponseEntity.ok(
                                                        InvestorChartResponse.from(request.getStkCd(), charts)))
                                        .defaultIfEmpty(ResponseEntity.notFound().build())
                                        .onErrorResume(error -> {
                                                log.error("투자자 차트 데이터 수집 실패 - 종목: {}, 일자: {}, 오류: {}",
                                                                request.getStkCd(), request.getDt(), error.getMessage(),
                                                                error);
                                                return Mono.just(ResponseEntity.status(500)
                                                                .body(InvestorChartResponse.error(error.getMessage())));
                                        });
                } catch (Exception e) {
                        log.error("투자자 차트 데이터 수집 요청 처리 실패 - 종목: {}, 일자: {}, 오류: {}",
                                        request.getStkCd(), request.getDt(), e.getMessage(), e);
                        return Mono.just(ResponseEntity.status(500)
                                        .body(InvestorChartResponse.error(e.getMessage())));
                }
        }

        /**
         * 투자자 차트 데이터 수집 (저장/중복 상태 포함)
         * POST /api/v1/investor-chart/fetch/status
         */
        @PostMapping("/fetch/status")
        public Mono<ResponseEntity<InvestorChartResponse>> fetchInvestorChartWithStatus(
                @RequestBody InvestorChartRequest request) {
            log.info("[STATUS] 투자자 차트 데이터 수집 요청 - 종목: {}, 일자: {}", request.getStkCd(), request.getDt());
            try {
                LocalDate date = LocalDate.parse(request.getDt(), DATE_FORMATTER);
                FetchInvestorChartCommand command = new FetchInvestorChartCommand(
                        request.getStkCd(), date, request.getAmtQtyTp(), request.getTrdeTp(), InvestorChart.UnitType.fromCode(request.getUnitTp()));

                if (!(fetchUseCase instanceof com.stocktrading.kiwoom.application.service.InvestorChartApplicationService)) {
                    return Mono.just(ResponseEntity.status(500).body(InvestorChartResponse.error("Status 기능을 사용할 수 없습니다")));
                }
                com.stocktrading.kiwoom.application.service.InvestorChartApplicationService service = 
                    (com.stocktrading.kiwoom.application.service.InvestorChartApplicationService) fetchUseCase;

                return service.collectWithStatus(command)
                        .map(ResponseEntity::ok)
                        .onErrorResume(err -> Mono.just(ResponseEntity.status(500).body(InvestorChartResponse.error(err.getMessage()))));
            } catch (Exception e) {
                log.error("[STATUS] 요청 처리 실패 - 종목: {}, 오류: {}", request.getStkCd(), e.getMessage(), e);
                return Mono.just(ResponseEntity.status(500)
                        .body(InvestorChartResponse.error(e.getMessage())));
            }
        }

                /**
                 * 저장 없이 파싱된 데이터만 응답 (RAW)
                 * POST /api/v1/investor-chart/fetch/raw
                 */
                @PostMapping("/fetch/raw")
                public Mono<ResponseEntity<InvestorChartResponse>> fetchInvestorChartRaw(
                                @RequestBody InvestorChartRequest request) {
                        log.info("[RAW] 투자자 차트 데이터 수집(저장 없음) - 종목: {}, 일자: {}", request.getStkCd(), request.getDt());
                        try {
                                LocalDate date = LocalDate.parse(request.getDt(), DATE_FORMATTER);
                                FetchInvestorChartCommand command = new FetchInvestorChartCommand(
                                                request.getStkCd(), date, request.getAmtQtyTp(), request.getTrdeTp(), InvestorChart.UnitType.fromCode(request.getUnitTp()));

                                if (!(fetchUseCase instanceof com.stocktrading.kiwoom.application.service.InvestorChartApplicationService)) {
                                        return Mono.just(ResponseEntity.status(500).body(InvestorChartResponse.error("RAW 기능을 사용할 수 없습니다")));
                                }
                                com.stocktrading.kiwoom.application.service.InvestorChartApplicationService service = 
                                    (com.stocktrading.kiwoom.application.service.InvestorChartApplicationService) fetchUseCase;

                                return service.fetchApiResponse(command)
                                                .flatMapMany(resp -> service.parseResponse(resp, command))
                                                .collectList()
                                                .map(charts -> ResponseEntity.ok(InvestorChartResponse.from(request.getStkCd(), charts)))
                                                .onErrorResume(err -> Mono.just(ResponseEntity.status(500).body(InvestorChartResponse.error(err.getMessage()))));
                        } catch (Exception e) {
                                log.error("[RAW] 요청 처리 실패 - 종목:{}, 오류:{}", request.getStkCd(), e.getMessage(), e);
                                return Mono.just(ResponseEntity.status(500).body(InvestorChartResponse.error(e.getMessage())));
                        }
                }

                /**
                 * 5요청 단위 메모리 버퍼 저장 API
                 * POST /api/v1/investor-chart/save-batch
                 */
                @PostMapping("/save-batch")
                public ResponseEntity<?> saveBatch(@RequestBody java.util.List<InvestorChartResponse.ChartDataDto> items) {
                        try {
                                if (!(fetchUseCase instanceof com.stocktrading.kiwoom.application.service.InvestorChartApplicationService)) {
                                        return ResponseEntity.status(500).body(java.util.Map.of("success", false, "error", "배치 저장 기능을 사용할 수 없습니다"));
                                }
                                com.stocktrading.kiwoom.application.service.InvestorChartApplicationService service = 
                                    (com.stocktrading.kiwoom.application.service.InvestorChartApplicationService) fetchUseCase;
                                // DTO -> 도메인 모델 간단 변환은 서비스로 위임
                                int saved = service.saveBatchFromDto(items);
                                return ResponseEntity.ok(java.util.Map.of("success", true, "savedCount", saved));
                        } catch (Exception e) {
                                log.error("배치 저장 실패: {}", e.getMessage(), e);
                                return ResponseEntity.status(500).body(java.util.Map.of("success", false, "error", e.getMessage()));
                        }
                }

        /**
         * 최근 30일 데이터 수집
         * POST /api/v1/investor-chart/fetch/recent/{stockCode}
         */
        @PostMapping("/fetch/recent/{stockCode}")
        public Mono<ResponseEntity<InvestorChartResponse>> fetchRecentChart(
                        @PathVariable String stockCode) {

                log.info("최근 투자자 차트 데이터 수집 - 종목: {}", stockCode);

                return fetchUseCase.fetchRecent(stockCode)
                                .collectList()
                                .map(charts -> ResponseEntity.ok(
                                                InvestorChartResponse.from(stockCode, charts)))
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        /**
         * 최근 데이터 수집 (상태 포함)
         * 기본값: 수량(2), 순매수(0), 원/주(1)
         * POST /api/v1/investor-chart/fetch/recent/status/{stockCode}
         */
        @PostMapping("/fetch/recent/status/{stockCode}")
        public Mono<ResponseEntity<InvestorChartResponse>> fetchRecentChartWithStatus(
                        @PathVariable String stockCode,
                        @RequestParam(name = "amtQtyTp", defaultValue = "2") String amtQtyTp,
                        @RequestParam(name = "trdeTp", defaultValue = "0") String trdeTp,
                        @RequestParam(name = "unitTp", defaultValue = "1") String unitTp) {

                if (!(fetchUseCase instanceof com.stocktrading.kiwoom.application.service.InvestorChartApplicationService)) {
                        return Mono.just(ResponseEntity.status(500)
                                        .body(InvestorChartResponse.error("Status 기능을 사용할 수 없습니다")));
                }
                com.stocktrading.kiwoom.application.service.InvestorChartApplicationService service = 
                    (com.stocktrading.kiwoom.application.service.InvestorChartApplicationService) fetchUseCase;

                LocalDate today = LocalDate.now();
                FetchInvestorChartCommand command = new FetchInvestorChartCommand(
                                stockCode, today, amtQtyTp, trdeTp, InvestorChart.UnitType.fromCode(unitTp));

                return service.collectWithStatus(command)
                                .map(ResponseEntity::ok)
                                .onErrorResume(err -> Mono.just(ResponseEntity.status(500)
                                                .body(InvestorChartResponse.error(err.getMessage()))));
        }

        /**
         * 날짜 범위 배치 수집 (메모리 버퍼링 + 30건씩 배치 저장)
         * POST /api/v1/investor-chart/fetch/batch?withStatus=true
         */
        @PostMapping("/fetch/batch")
        public Mono<ResponseEntity<?>> fetchBatchByDateRange(
                        @RequestBody InvestorChartBatchRequest request,
                        @RequestParam(required = false, defaultValue = "false") boolean withStatus) {

                log.info("배치 수집 요청 - 종목: {}, 기간: {} ~ {}, withStatus: {}",
                                request.getStkCd(), request.getDateFrom(), request.getDateTo(), withStatus);

                try {
                        LocalDate startDate = LocalDate.parse(request.getDateFrom(), DATE_FORMATTER);
                        LocalDate endDate = LocalDate.parse(request.getDateTo(), DATE_FORMATTER);

                        if (startDate.isAfter(endDate)) {
                                return Mono.just(ResponseEntity.badRequest()
                                                .body("시작일이 종료일보다 늦을 수 없습니다"));
                        }

                        if (!(fetchUseCase instanceof com.stocktrading.kiwoom.application.service.InvestorChartApplicationService)) {
                                return Mono.just(ResponseEntity.status(500)
                                                .body("배치 수집 기능을 사용할 수 없습니다"));
                        }

                        com.stocktrading.kiwoom.application.service.InvestorChartApplicationService service =
                                        (com.stocktrading.kiwoom.application.service.InvestorChartApplicationService) fetchUseCase;

                        // If withStatus=true, use collectBatchWithStatus
                        if (withStatus) {
                                return service.collectBatchWithStatus(
                                                request.getStkCd(), startDate, endDate,
                                                request.getAmtQtyTp(), request.getTrdeTp(),
                                                InvestorChart.UnitType.fromCode(request.getUnitTp()))
                                        .<ResponseEntity<?>>map(result -> {
                                                java.util.Map<String, Object> response = new java.util.HashMap<>();
                                                response.put("success", true);
                                                response.put("stockCode", result.getStockCode());
                                                response.put("startDate", result.getStartDate());
                                                response.put("endDate", result.getEndDate());
                                                response.put("requestedCount", result.getRequestedCount());
                                                response.put("receivedCount", result.getReceivedCount());
                                                response.put("savedCount", result.getSavedCount());
                                                response.put("duplicateCount", result.getDuplicateCount());
                                                response.put("errorCount", result.getErrorCount());
                                                response.put("savedDates", result.getSavedDates());
                                                response.put("duplicateDates", result.getDuplicateDates());
                                                response.put("errorDates", result.getErrorDates());
                                                return ResponseEntity.ok(response);
                                        })
                                        .onErrorResume(err -> {
                                                java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
                                                errorResponse.put("success", false);
                                                errorResponse.put("error", err.getMessage());
                                                ResponseEntity<?> errorEntity = ResponseEntity.status(500).body(errorResponse);
                                                return Mono.just(errorEntity);
                                        });
                        }

                        // Otherwise use fetchByDateRange
                        return service.fetchByDateRange(
                                        request.getStkCd(),
                                        startDate,
                                        endDate,
                                        request.getAmtQtyTp(),
                                        request.getTrdeTp(),
                                        InvestorChart.UnitType.fromCode(request.getUnitTp()))
                                        .<ResponseEntity<?>>map(result -> {
                                                if (result.isSuccess()) {
                                                        return ResponseEntity.ok(java.util.Map.of(
                                                                        "success", true,
                                                                        "stockCode", result.getStockCode(),
                                                                        "startDate", result.getStartDate().toString(),
                                                                        "endDate", result.getEndDate().toString(),
                                                                        "savedCount", result.getSavedCount()));
                                                } else {
                                                        return ResponseEntity.status(500).body(java.util.Map.of(
                                                                        "success", false,
                                                                        "error", result.getErrorMessage()));
                                                }
                                        })
                                        .onErrorResume(error -> {
                                                log.error("배치 수집 실패: {}", error.getMessage(), error);
                                                return Mono.just(ResponseEntity.status(500).body(java.util.Map.of(
                                                                "success", false,
                                                                "error", error.getMessage())));
                                        });

                } catch (Exception e) {
                        log.error("배치 수집 요청 처리 실패: {}", e.getMessage(), e);
                        return Mono.just(ResponseEntity.status(500)
                                        .body(java.util.Map.of(
                                                        "success", false,
                                                        "error", e.getMessage())));
                }
        }

        /**
         * 배치 수집 요청 DTO
         */
        public static class InvestorChartBatchRequest {
                private String stkCd;
                private String dateFrom; // YYYYMMDD
                private String dateTo; // YYYYMMDD
                private String amtQtyTp;
                private String trdeTp;
                private String unitTp;

                public String getStkCd() {
                        return stkCd;
                }

                public void setStkCd(String stkCd) {
                        this.stkCd = stkCd;
                }

                public String getDateFrom() {
                        return dateFrom;
                }

                public void setDateFrom(String dateFrom) {
                        this.dateFrom = dateFrom;
                }

                public String getDateTo() {
                        return dateTo;
                }

                public void setDateTo(String dateTo) {
                        this.dateTo = dateTo;
                }

                public String getAmtQtyTp() {
                        return amtQtyTp;
                }

                public void setAmtQtyTp(String amtQtyTp) {
                        this.amtQtyTp = amtQtyTp;
                }

                public String getTrdeTp() {
                        return trdeTp;
                }

                public void setTrdeTp(String trdeTp) {
                        this.trdeTp = trdeTp;
                }

                public String getUnitTp() {
                        return unitTp;
                }

                public void setUnitTp(String unitTp) {
                        this.unitTp = unitTp;
                }
        }

        // ============================================
        // Query APIs (DB 조회)
        // ============================================

        /**
         * 종목+일자로 조회
         * GET /api/v1/investor-chart/{stockCode}?date=2024-11-07
         */
        @GetMapping("/{stockCode}")
        public ResponseEntity<InvestorChartResponse.ChartDataDto> getByStockAndDate(
                        @PathVariable String stockCode,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

                InvestorChart chart = queryUseCase.queryByStockAndDate(stockCode, date);
                if (chart == null) {
                        return ResponseEntity.notFound().build();
                }
                return ResponseEntity.ok(InvestorChartResponse.ChartDataDto.from(chart));
        }

        /**
         * 종목+기간으로 조회 (추이 분석용)
         * GET
         * /api/v1/investor-chart/{stockCode}/period?startDate=2024-10-01&endDate=2024-11-07
         */
        @GetMapping("/{stockCode}/period")
        public ResponseEntity<InvestorChartResponse> getByStockAndPeriod(
                        @PathVariable String stockCode,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

                List<InvestorChart> charts = queryUseCase.queryByStockAndPeriod(stockCode, startDate, endDate);
                return ResponseEntity.ok(InvestorChartResponse.from(stockCode, charts));
        }

        /**
         * 기관 세부 유형별 추이 조회
         * GET
         * /api/v1/investor-chart/{stockCode}/institution-trend?startDate=2024-10-01&endDate=2024-11-07
         */
        @GetMapping("/{stockCode}/institution-trend")
        public ResponseEntity<InstitutionTrendResponse> getInstitutionTrend(
                        @PathVariable String stockCode,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

                var trends = queryUseCase.queryInstitutionTrend(stockCode, startDate, endDate);
                return ResponseEntity.ok(InstitutionTrendResponse.from(stockCode, startDate, endDate, trends));
        }

        /**
         * 특정 일자의 모든 종목 조회
         * GET /api/v1/investor-chart/by-date?date=2024-11-07
         */
        @GetMapping("/by-date")
        public ResponseEntity<List<InvestorChartResponse.ChartDataDto>> getByDate(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

                List<InvestorChart> charts = queryUseCase.queryByDate(date);
                List<InvestorChartResponse.ChartDataDto> result = charts.stream()
                                .map(InvestorChartResponse.ChartDataDto::from)
                                .toList();
                return ResponseEntity.ok(result);
        }

        /**
         * 종목의 최신 데이터 조회
         * GET /api/v1/investor-chart/{stockCode}/latest
         */
        @GetMapping("/{stockCode}/latest")
        public ResponseEntity<InvestorChartResponse.ChartDataDto> getLatestByStock(
                        @PathVariable String stockCode) {

                InvestorChart chart = queryUseCase.queryLatestByStock(stockCode);
                if (chart == null) {
                        return ResponseEntity.notFound().build();
                }
                return ResponseEntity.ok(InvestorChartResponse.ChartDataDto.from(chart));
        }

        /**
         * 종목의 날짜 범위 조회 (최소, 최대)
         * GET /api/v1/investor-chart/{stockCode}/date-range
         */
        @GetMapping("/{stockCode}/date-range")
        public ResponseEntity<DateRangeDto> getDateRangeByStock(
                        @PathVariable String stockCode) {

                var dateRange = queryUseCase.queryDateRangeByStock(stockCode);
                if (dateRange.startDate() == null && dateRange.endDate() == null) {
                        return ResponseEntity.notFound().build();
                }
                return ResponseEntity.ok(new DateRangeDto(dateRange.startDate(), dateRange.endDate()));
        }

        /**
         * 날짜 범위 DTO
         */
        public record DateRangeDto(LocalDate startDate, LocalDate endDate) {}

        /**
         * 외국인+기관 동반 매수 종목 조회
         * GET /api/v1/investor-chart/foreigner-institution-buy?date=2024-11-07
         */
        @GetMapping("/foreigner-institution-buy")
        public ResponseEntity<List<InvestorChartResponse.ChartDataDto>> getForeignerInstitutionBuyStocks(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

                List<InvestorChart> charts = queryUseCase.queryForeignerInstitutionBuyStocks(date);
                List<InvestorChartResponse.ChartDataDto> result = charts.stream()
                                .map(InvestorChartResponse.ChartDataDto::from)
                                .toList();
                return ResponseEntity.ok(result);
        }

        /**
         * 연기금 순매수 종목 조회
         * GET /api/v1/investor-chart/pension-fund-buy?date=2024-11-07
         */
        @GetMapping("/pension-fund-buy")
        public ResponseEntity<List<InvestorChartResponse.ChartDataDto>> getPensionFundBuyStocks(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

                List<InvestorChart> charts = queryUseCase.queryPensionFundBuyStocks(date);
                List<InvestorChartResponse.ChartDataDto> result = charts.stream()
                                .map(InvestorChartResponse.ChartDataDto::from)
                                .toList();
                return ResponseEntity.ok(result);
        }

        /**
         * KOSPI200 전체 종목 배치 수집 (Server-Sent Events)
         * POST /api/v1/investor-chart/fetch/kospi200-batch
         */
        @PostMapping(value = "/fetch/kospi200-batch", produces = "text/event-stream")
        public reactor.core.publisher.Flux<org.springframework.http.codec.ServerSentEvent<Kospi200ProgressDto>> fetchKospi200Batch(
                        @RequestBody Kospi200BatchRequest request) {

                log.info("KOSPI200 배치 수집 요청 - amtQtyTp: {}, trdeTp: {}, unitTp: {}",
                                request.getAmtQtyTp(), request.getTrdeTp(), request.getUnitTp());

                if (!(fetchUseCase instanceof com.stocktrading.kiwoom.application.service.InvestorChartApplicationService)) {
                        return reactor.core.publisher.Flux.error(new IllegalStateException("KOSPI200 배치 기능을 사용할 수 없습니다"));
                }

                com.stocktrading.kiwoom.application.service.InvestorChartApplicationService service =
                                (com.stocktrading.kiwoom.application.service.InvestorChartApplicationService) fetchUseCase;

                return service.fetchKospi200Batch(
                                request.getAmtQtyTp(),
                                request.getTrdeTp(),
                                request.getUnitTp())
                                .map(progress -> {
                                        log.info("SSE 전송 - 종목: {} ({}), 진행: {}/{}, 현재 종목(수신: {}, 저장: {}), 전체 누적(수신: {}, 저장: {})",
                                                progress.getCurrentStockName(),
                                                progress.getCurrentStockCode(),
                                                progress.getProcessedCount(),
                                                progress.getTotalCount(),
                                                progress.getReceivedCount(),
                                                progress.getSavedCount(),
                                                progress.getCumulativeReceivedCount(),
                                                progress.getCumulativeSavedCount());
                                        Kospi200ProgressDto dto = new Kospi200ProgressDto(
                                                        progress.getCurrentStockCode(),
                                                        progress.getCurrentStockName(),
                                                        progress.getProcessedCount(),
                                                        progress.getTotalCount(),
                                                        progress.getReceivedCount(),
                                                        progress.getSavedCount(),
                                                        progress.getDuplicateCount(),
                                                        progress.getErrorCount(),
                                                        progress.getCumulativeReceivedCount(),  // 누적 수신
                                                        progress.getCumulativeSavedCount(),     // 누적 저장
                                                        progress.getErrors(),
                                                        progress.isCompleted());
                                        return org.springframework.http.codec.ServerSentEvent.<Kospi200ProgressDto>builder()
                                                        .data(dto)
                                                        .build();
                                })
                                .onErrorResume(error -> {
                                        log.error("KOSPI200 배치 수집 실패: {}", error.getMessage(), error);
                                        Kospi200ProgressDto errorDto = new Kospi200ProgressDto(
                                                        null, null, 0, 0, 0, 0, 0, 0, 0, 0,  // 누적 필드 추가
                                                        java.util.List.of("배치 처리 오류: " + error.getMessage()),
                                                        true);
                                        return reactor.core.publisher.Flux.just(
                                                        org.springframework.http.codec.ServerSentEvent.<Kospi200ProgressDto>builder()
                                                                        .data(errorDto)
                                                                        .build());
                                });
        }

        /**
         * KOSPI200 배치 요청 DTO
         */
        public static class Kospi200BatchRequest {
                private String amtQtyTp = "2"; // 기본값: 수량
                private String trdeTp = "0";   // 기본값: 순매수
                private String unitTp = "1";   // 기본값: 원/주

                public String getAmtQtyTp() {
                        return amtQtyTp;
                }

                public void setAmtQtyTp(String amtQtyTp) {
                        this.amtQtyTp = amtQtyTp;
                }

                public String getTrdeTp() {
                        return trdeTp;
                }

                public void setTrdeTp(String trdeTp) {
                        this.trdeTp = trdeTp;
                }

                public String getUnitTp() {
                        return unitTp;
                }

                public void setUnitTp(String unitTp) {
                        this.unitTp = unitTp;
                }
        }

        /**
         * KOSPI200 진행상황 DTO
         */
        public record Kospi200ProgressDto(
                        String currentStockCode,
                        String currentStockName,
                        int processedCount,
                        int totalCount,
                        int receivedCount,     // 현재 종목 수신
                        int savedCount,        // 현재 종목 저장
                        int duplicateCount,    // 현재 종목 중복
                        int errorCount,        // 현재 종목 오류
                        int cumulativeReceivedCount,  // 전체 누적 수신
                        int cumulativeSavedCount,     // 전체 누적 저장
                        java.util.List<String> errors,
                        boolean completed) {
        }
}
