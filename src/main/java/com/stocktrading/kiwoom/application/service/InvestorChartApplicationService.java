package com.stocktrading.kiwoom.application.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stocktrading.kiwoom.domain.model.InvestorChart;
import com.stocktrading.kiwoom.domain.port.in.FetchInvestorChartUseCase;
import com.stocktrading.kiwoom.domain.port.in.QueryInvestorChartUseCase;
import com.stocktrading.kiwoom.domain.port.out.AuthPort;
import com.stocktrading.kiwoom.domain.port.out.InvestorChartPort;
import com.stocktrading.kiwoom.domain.port.out.KiwoomApiPort;
import com.stocktrading.kiwoom.domain.tr.InvestorChartTR;
import com.stocktrading.kiwoom.dto.InvestorChartResponse;
import com.stocktrading.kiwoom.service.OAuthTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 투자자 기관별 차트 Application Service (ka10060)
 * FetchInvestorChartUseCase, QueryInvestorChartUseCase 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestorChartApplicationService implements FetchInvestorChartUseCase, QueryInvestorChartUseCase {

    private final InvestorChartPort chartPort;
    private final KiwoomApiPort apiPort;
    private final AuthPort authPort;
    private final OAuthTokenService oAuthTokenService;
    private final com.stocktrading.kiwoom.domain.port.out.StockListPort stockListPort;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String API_ID = "ka10060";

    // ============================================
    // Fetch UseCase 구현 (API 호출 → DB 저장)
    // ============================================

    @Override
    @Transactional
    public Flux<InvestorChart> fetchByStock(FetchInvestorChartCommand command) {
        String stockCode = command.stockCode();
        String dateStr = command.date().format(DATE_FORMATTER);

        log.info("[{}][{}] 1. 데이터 수집 시작", stockCode, dateStr);

        // 메인 페이지에서 발급받은 토큰 사용 (OAuthTokenService의 캐시된 토큰)
        String cachedToken = oAuthTokenService.getCachedToken();
        if (cachedToken != null && !cachedToken.isEmpty()) {
            log.info("[{}][{}] 2. 글로벌 캐시 토큰 사용", stockCode, dateStr);
            return callInvestorChartApi(command, cachedToken, "N", "")
                    .flatMapMany(response -> parseAndSaveResponse(response, command))
                    .doOnComplete(() -> log.info("[{}][{}] 5. 데이터 수집 완료", stockCode, dateStr))
                    .doOnError(error -> log.error("[{}][{}] 오류 발생: {}", stockCode, dateStr, error.getMessage(), error));
        }

        // 캐시된 토큰이 없으면 기존 방식 사용 (fallback)
        log.warn("[{}][{}] 글로벌 토큰 없음. 기존 AuthPort 사용", stockCode, dateStr);
        return authPort.getToken()
                .flatMap(tokenOpt -> {
                    if (tokenOpt.isEmpty()) {
                        log.error("[{}][{}] 인증 토큰을 찾을 수 없습니다", stockCode, dateStr);
                        return Mono.error(new IllegalStateException(
                                "Token not found. Please connect to Kiwoom server from main page first."));
                    }
                    String token = tokenOpt.get().getAccessToken();
                    log.info("[{}][{}] 2. API 요청 준비 - Token 획득 완료", stockCode, dateStr);
                    return callInvestorChartApi(command, token, "N", "");
                })
                .flatMapMany(response -> parseAndSaveResponse(response, command))
                .doOnComplete(() -> log.info("[{}][{}] 5. 데이터 수집 완료", stockCode, dateStr))
                .doOnError(error -> log.error("[{}][{}] 오류 발생: {}", stockCode, dateStr, error.getMessage(), error));
    }

    @Override
    public Flux<InvestorChart> fetchRecent(String stockCode) {
        LocalDate today = LocalDate.now();
        FetchInvestorChartCommand command = FetchInvestorChartCommand.of(stockCode, today);
        return fetchByStock(command);
    }

    @Override
    @Transactional
    public Flux<InvestorChart> fetchAllWithContinuation(FetchInvestorChartCommand command) {
        log.info("투자자 차트 연속조회 시작 - 종목: {}", command.stockCode());

        // 글로벌 캐시 토큰 우선 사용
        String cachedToken = oAuthTokenService.getCachedToken();
        if (cachedToken != null && !cachedToken.isEmpty()) {
            log.info("글로벌 캐시 토큰 사용 (연속조회)");
            return fetchWithContinuation(command, cachedToken, "N", "")
                    .doOnComplete(() -> log.info("투자자 차트 연속조회 완료"))
                    .doOnError(error -> log.error("투자자 차트 연속조회 실패: {}", error.getMessage()));
        }

        // fallback
        return authPort.getToken()
                .flatMapMany(tokenOpt -> {
                    if (tokenOpt.isEmpty()) {
                        return Flux.error(new IllegalStateException(
                                "Token not found. Please connect to Kiwoom server from main page first."));
                    }
                    String token = tokenOpt.get().getAccessToken();
                    return fetchWithContinuation(command, token, "N", "");
                })
                .doOnComplete(() -> log.info("투자자 차트 연속조회 완료"))
                .doOnError(error -> log.error("투자자 차트 연속조회 실패: {}", error.getMessage()));
    }

    /**
     * 날짜 범위 배치 수집 (5요청 단위 배치 저장)
     */
    @Transactional
    public Mono<BatchCollectionResult> fetchByDateRange(
            String stockCode, LocalDate startDate, LocalDate endDate,
            String amtQtyTp, String trdeTp, InvestorChart.UnitType unitType) {

        log.info("========================================");
        log.info("날짜 범위 배치 수집 시작");
        log.info("종목: {}, 기간: {} ~ {}", stockCode, startDate, endDate);
        log.info("========================================");

        String cachedToken = oAuthTokenService.getCachedToken();
        if (cachedToken == null || cachedToken.isEmpty()) {
            log.error("인증 토큰을 찾을 수 없습니다");
            return Mono.just(BatchCollectionResult.error("인증 토큰을 찾을 수 없습니다"));
        }

        List<LocalDate> dates = startDate.datesUntil(endDate.plusDays(1))
                .collect(Collectors.toList());

        log.info("총 {}일의 데이터를 수집합니다", dates.size());

        return Flux.fromIterable(dates)
                .concatMap(date -> {
                    FetchInvestorChartCommand command = new FetchInvestorChartCommand(
                            stockCode, date, amtQtyTp, trdeTp, unitType);
                    return callInvestorChartApi(command, cachedToken, "N", "")
                            .flatMapMany(response -> parseResponse(response, command))
                            .collectList()
                            .onErrorResume(error -> {
                                log.error("[{}][{}] API 호출 실패: {}",
                                        stockCode, date.format(DATE_FORMATTER), error.getMessage());
                                return Mono.just(java.util.Collections.<InvestorChart>emptyList());
                            });
                })
                .buffer(5) // 5요청(일자) 단위로 버퍼링
                .flatMap(batchLists -> {
                    List<InvestorChart> batch = batchLists.stream()
                            .flatMap(java.util.Collection::stream)
                            .collect(java.util.stream.Collectors.toList());
                    log.info("배치 저장(5요청 단위) 시작 - 포함 일자:{}, 총 데이터:{}건",
                            batchLists.size(), batch.size());
                    try {
                        List<InvestorChart> saved = chartPort.saveAll(batch);
                        log.info("배치 저장(5요청 단위) 완료 - {}건", saved.size());
                        return Flux.just(saved.size());
                    } catch (Exception e) {
                        log.error("배치 저장(5요청 단위) 실패: {}", e.getMessage(), e);
                        return Flux.just(0);
                    }
                })
                .reduce(new BatchCollectionResult(stockCode, startDate, endDate), (result, savedCount) -> {
                    result.addSavedCount(savedCount);
                    return result;
                })
                .doOnSuccess(result -> {
                    log.info("========================================");
                    log.info("배치 수집 완료!");
                    log.info("종목: {}, 기간: {} ~ {}", stockCode, startDate, endDate);
                    log.info("총 저장: {}건", result.getSavedCount());
                    log.info("========================================");
                })
                .doOnError(error -> {
                    log.error("배치 수집 실패: {}", error.getMessage(), error);
                });
    }

    /**
     * 배치 수집 결과
     */
    public static class BatchCollectionResult {
        private final String stockCode;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private int savedCount = 0;
        private String errorMessage;

        public BatchCollectionResult(String stockCode, LocalDate startDate, LocalDate endDate) {
            this.stockCode = stockCode;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public static BatchCollectionResult error(String errorMessage) {
            BatchCollectionResult result = new BatchCollectionResult(null, null, null);
            result.errorMessage = errorMessage;
            return result;
        }

        public void addSavedCount(int count) {
            this.savedCount += count;
        }

        public String getStockCode() {
            return stockCode;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public int getSavedCount() {
            return savedCount;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }
    }

    // ============================================
    // Query UseCase 구현 (DB 조회)
    // ============================================

    @Override
    public InvestorChart queryByStockAndDate(String stockCode, LocalDate date) {
        return chartPort.findByStockAndDate(stockCode, date).orElse(null);
    }

    @Override
    public List<InvestorChart> queryByStockAndPeriod(String stockCode, LocalDate startDate, LocalDate endDate) {
        return chartPort.findByStockAndPeriod(stockCode, startDate, endDate);
    }

    @Override
    public List<InvestorChart> queryByDate(LocalDate date) {
        return chartPort.findByDate(date);
    }

    @Override
    public InvestorChart queryLatestByStock(String stockCode) {
        return chartPort.findLatestByStock(stockCode).orElse(null);
    }

    @Override
    public List<InstitutionTrend> queryInstitutionTrend(String stockCode, LocalDate startDate, LocalDate endDate) {
        List<InvestorChart> charts = chartPort.findByStockAndPeriod(stockCode, startDate, endDate);

        return charts.stream()
                .map(chart -> new InstitutionTrend(
                        chart.getDate(),
                        chart.getInstitutionBreakdown().getFinancialInvest(),
                        chart.getInstitutionBreakdown().getInsurance(),
                        chart.getInstitutionBreakdown().getInvestment(),
                        chart.getInstitutionBreakdown().getEtcFinancial(),
                        chart.getInstitutionBreakdown().getBank(),
                        chart.getInstitutionBreakdown().getPensionFund(),
                        chart.getInstitutionBreakdown().getPrivateFund(),
                        chart.getInstitutionBreakdown().getNation(),
                        chart.getInstitutionBreakdown().getEtcCorporation()))
                .collect(Collectors.toList());
    }

    @Override
    public List<InvestorChart> queryForeignerInstitutionBuyStocks(LocalDate date) {
        return chartPort.findForeignerInstitutionBuyStocks(date);
    }

    @Override
    public List<InvestorChart> queryPensionFundBuyStocks(LocalDate date) {
        return chartPort.findPensionFundBuyStocks(date);
    }

    @Override
    public StockDateRange queryDateRangeByStock(String stockCode) {
        var dateRange = chartPort.findDateRangeByStock(stockCode);
        return new StockDateRange(dateRange.minDate(), dateRange.maxDate());
    }

    // ============================================
    // Private Methods
    // ============================================

    /**
     * 키움 API 호출 (운영서버만 사용)
     */
    private Mono<InvestorChartTR.Response> callInvestorChartApi(
            FetchInvestorChartCommand command, String token, String contYn, String nextKey) {

        String stockCode = command.stockCode();
        String dateStr = command.date().format(DATE_FORMATTER);

        // Request body를 Map으로 생성
        var requestBody = java.util.Map.of(
                "dt", dateStr,
                "stk_cd", stockCode,
                "amt_qty_tp", command.amtQtyTp(),
                "trde_tp", command.trdeTp(),
                "unit_tp", command.unitType().getCode());

        log.info("[{}][{}] 2. API 요청 전송 - Body: {}", stockCode, dateStr, requestBody);

        return apiPort.postWithHeaders(API_ID, requestBody, token, contYn, nextKey)
                .doOnSuccess(response -> {
                    log.info("[{}][{}] 3. API 응답 수신 완료 - 응답 길이: {} bytes",
                            stockCode, dateStr, response != null ? response.length() : 0);
                    if (response != null && response.length() > 0) {
                        String preview = response.length() > 200 ? response.substring(0, 200) + "..." : response;
                        log.debug("[{}][{}] 응답 내용 미리보기: {}", stockCode, dateStr, preview);
                    }
                })
                .doOnError(error -> {
                    log.error("[{}][{}] 3. API 요청 실패: {}", stockCode, dateStr, error.getMessage(), error);
                })
                .map(this::parseApiResponse);
    }

    /**
     * API 응답만 가져오고 저장/파싱을 나중에 처리 (상태 분류용)
     */
    public Mono<InvestorChartTR.Response> fetchApiResponse(FetchInvestorChartCommand command) {
        String cachedToken = oAuthTokenService.getCachedToken();
        if (cachedToken != null && !cachedToken.isEmpty()) {
            return callInvestorChartApi(command, cachedToken, "N", "");
        }
        return authPort.getToken()
                .flatMap(tokenOpt -> {
                    if (tokenOpt.isEmpty()) {
                        return Mono.error(new IllegalStateException(
                                "Token not found. Please connect to Kiwoom server from main page first."));
                    }
                    String token = tokenOpt.get().getAccessToken();
                    return callInvestorChartApi(command, token, "N", "");
                });
    }

    /**
     * 단일 일자 수집 - 저장/중복/오류 상태 포함
     */
    public Mono<InvestorChartResponse> collectWithStatus(FetchInvestorChartCommand command) {
        return fetchApiResponse(command)
                .flatMap(raw -> parseResponse(raw, command).collectList())
                .map(parsedCharts -> {
                    List<LocalDate> duplicateDates = new java.util.ArrayList<>();
                    List<InvestorChart> newCharts = new java.util.ArrayList<>();
                    for (InvestorChart c : parsedCharts) {
                        boolean exists = chartPort.findByStockAndDate(command.stockCode(), c.getDate()).isPresent();
                        if (exists) duplicateDates.add(c.getDate()); else newCharts.add(c);
                    }
                    List<InvestorChart> saved = newCharts.isEmpty() ? java.util.Collections.emptyList() : chartPort.saveAll(newCharts);
                    List<String> logs = java.util.List.of(
                            "수신:" + parsedCharts.size(),
                            "저장:" + saved.size(),
                            "중복:" + duplicateDates.size());
                    return InvestorChartResponse.fromWithStatus(command.stockCode(), saved, parsedCharts, duplicateDates, logs);
                });
    }

    /**
     * 구간 수집 - 저장/중복/오류 상태 포함
     */
    public Mono<BatchStatusResult> collectBatchWithStatus(
            String stockCode, LocalDate startDate, LocalDate endDate,
            String amtQtyTp, String trdeTp, InvestorChart.UnitType unitType) {

        List<LocalDate> dates = startDate.datesUntil(endDate.plusDays(1)).collect(java.util.stream.Collectors.toList());
        BatchStatusResult acc = new BatchStatusResult(stockCode, startDate, endDate, dates.size());

        String cachedToken = oAuthTokenService.getCachedToken();
        if (cachedToken == null || cachedToken.isEmpty()) {
            return Mono.error(new IllegalStateException("인증 토큰을 찾을 수 없습니다"));
        }

        return Flux.fromIterable(dates)
                .concatMap(date -> {
                    FetchInvestorChartCommand command = new FetchInvestorChartCommand(
                            stockCode, date, amtQtyTp, trdeTp, unitType);
                    return callInvestorChartApi(command, cachedToken, "N", "")
                            .flatMapMany(resp -> parseResponse(resp, command))
                            .collectList()
                            .map(parsed -> new java.util.AbstractMap.SimpleEntry<>(date, parsed))
                            .onErrorReturn(new java.util.AbstractMap.SimpleEntry<>(date, java.util.Collections.emptyList()));
                })
                .reduce(acc, (result, entry) -> {
                    LocalDate date = entry.getKey();
                    List<InvestorChart> parsed = entry.getValue();
                    if (parsed.isEmpty()) {
                        // 오류 또는 수신 0건 처리: 오류로 집계
                        result.errorDates.add(date);
                        result.errorCount++;
                        return result;
                    }
                    result.receivedCount += 1; // 해당 일자 수신됨

                    // 중복 필터링 후, 새 데이터만 버퍼에 적재 (5요청 단위 저장)
                    List<InvestorChart> newCharts = new java.util.ArrayList<>();
                    boolean hasDuplicate = false;
                    for (InvestorChart c : parsed) {
                        boolean exists = chartPort.findByStockAndDate(stockCode, c.getDate()).isPresent();
                        if (exists) {
                            hasDuplicate = true;
                        } else {
                            newCharts.add(c);
                        }
                    }

                    if (!newCharts.isEmpty()) {
                        result.savedDates.add(date);
                        result.savedCount++;
                        result.buffer.addAll(newCharts);
                    } else if (hasDuplicate) {
                        if (!result.duplicateDates.contains(date)) {
                            result.duplicateDates.add(date);
                            result.duplicateCount++;
                        }
                    }

                    // 5요청 단위로 버퍼 플러시
                    if (result.receivedCount % 5 == 0) {
                        try {
                            if (!result.buffer.isEmpty()) {
                                List<InvestorChart> saved = chartPort.saveAll(result.buffer);
                                log.info("배치 저장(5요청 단위) 완료 - {}건", saved.size());
                                result.buffer.clear();
                            }
                        } catch (Exception e) {
                            log.error("배치 저장(5요청 단위) 실패: {}", e.getMessage(), e);
                            // 실패하더라도 다음 요청 처리 계속
                        }
                    }

                    // 마지막 요청 처리 후 잔여 버퍼 플러시
                    if (result.receivedCount == result.requestedCount) {
                        try {
                            if (!result.buffer.isEmpty()) {
                                List<InvestorChart> saved = chartPort.saveAll(result.buffer);
                                log.info("최종 배치 저장 완료 - {}건", saved.size());
                                result.buffer.clear();
                            }
                        } catch (Exception e) {
                            log.error("최종 배치 저장 실패: {}", e.getMessage(), e);
                        }
                    }
                    return result;
                })
                .doOnSuccess(r -> log.info("배치 상태 수집 완료 - 종목:{}, 요청:{}, 수신:{}, 저장:{}, 중복:{}, 오류:{}",
                        stockCode, r.requestedCount, r.receivedCount, r.savedCount, r.duplicateCount, r.errorCount));
    }

    @lombok.Data
    public static class BatchStatusResult {
        private final String stockCode;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final int requestedCount;
        private int receivedCount = 0;
        private int savedCount = 0;
        private int duplicateCount = 0;
        private int errorCount = 0;
        private final java.util.List<LocalDate> savedDates = new java.util.ArrayList<>();
        private final java.util.List<LocalDate> duplicateDates = new java.util.ArrayList<>();
        private final java.util.List<LocalDate> errorDates = new java.util.ArrayList<>();
        // 5요청 단위 저장을 위한 버퍼
        private final java.util.List<InvestorChart> buffer = new java.util.ArrayList<>();

        public BatchStatusResult(String stockCode, LocalDate startDate, LocalDate endDate, int requestedCount) {
            this.stockCode = stockCode;
            this.startDate = startDate;
            this.endDate = endDate;
            this.requestedCount = requestedCount;
        }
    }

    /**
     * API 응답 파싱
     */
    private InvestorChartTR.Response parseApiResponse(String response) {
        InvestorChartTR tr = new InvestorChartTR();
        return tr.parseResponse(response);
    }

    /** 헤더 포함 호출 */
    private Mono<HeaderedResponse> callInvestorChartApiWithHeaders(
            FetchInvestorChartCommand command, String token, String contYn, String nextKey) {
        String stockCode = command.stockCode();
        String dateStr = command.date().format(DATE_FORMATTER);
        var requestBody = java.util.Map.of(
                "dt", dateStr,
                "stk_cd", stockCode,
                "amt_qty_tp", command.amtQtyTp(),
                "trde_tp", command.trdeTp(),
                "unit_tp", command.unitType().getCode());
        return apiPort.postWithHeadersAndReturnHeaders(API_ID, requestBody, token, contYn, nextKey)
                .map(bh -> new HeaderedResponse(parseApiResponse(bh.body()),
                        bh.contYn() == null ? "N" : bh.contYn(),
                        bh.nextKey() == null ? "" : bh.nextKey()));
    }

    private record HeaderedResponse(InvestorChartTR.Response response, String contYn, String nextKey) {}

    /**
     * 연속조회 처리
     */
    private Flux<InvestorChart> fetchWithContinuation(
            FetchInvestorChartCommand command, String token, String contYn, String nextKey) {

        return callInvestorChartApi(command, token, contYn, nextKey)
                .flatMapMany(response -> {
                    Flux<InvestorChart> currentData = parseAndSaveResponse(response, command);

                    // 연속조회 필요 여부 확인 (실제 구현 시 응답 헤더에서 확인)
                    // 현재는 단일 조회만 처리
                    return currentData;
                });
    }

    /**
     * API 응답 파싱 (DB 저장 없이 파싱만)
     */
    public Flux<InvestorChart> parseResponse(
            InvestorChartTR.Response response, FetchInvestorChartCommand command) {

        String stockCode = command.stockCode();
        String dateStr = command.date().format(DATE_FORMATTER);

        if (!response.isSuccess()) {
            log.error("[{}][{}] API 응답 오류 - Code: {}, Message: {}",
                    stockCode, dateStr, response.getReturnCode(), response.getReturnMsg());
            return Flux.empty();
        }

        if (response.getChartDataList().isEmpty()) {
            log.warn("[{}][{}] → 수신 데이터: 0건", stockCode, dateStr);
            return Flux.empty();
        }

        log.info("[{}][{}] → 수신 데이터: {}건", stockCode, dateStr, response.getChartDataList().size());

        try {
            List<InvestorChart> charts = response.getChartDataList().stream()
                    .map(data -> mapToInvestorChart(data, command))
                    .collect(Collectors.toList());

            return Flux.fromIterable(charts);

        } catch (Exception e) {
            log.error("[{}][{}] 데이터 파싱 실패: {}", stockCode, dateStr, e.getMessage(), e);
            return Flux.error(e);
        }
    }

    /**
     * API 응답 파싱 및 DB 저장
     */
    private Flux<InvestorChart> parseAndSaveResponse(
            InvestorChartTR.Response response, FetchInvestorChartCommand command) {

        String stockCode = command.stockCode();
        String dateStr = command.date().format(DATE_FORMATTER);

        if (!response.isSuccess()) {
            log.error("[{}][{}] 3. API 응답 오류 - Code: {}, Message: {}",
                    stockCode, dateStr, response.getReturnCode(), response.getReturnMsg());
            return Flux.empty();
        }

        log.info("[{}][{}] 3. API 응답 정상 - 데이터 개수: {}건",
                stockCode, dateStr, response.getChartDataList().size());

        if (!response.getChartDataList().isEmpty()) {
            var firstData = response.getChartDataList().get(0);
            log.info("[{}][{}] 샘플 데이터 - 개인: {}, 외국인: {}, 기관: {}",
                    stockCode, dateStr,
                    firstData.getIndInvsr(),
                    firstData.getFrgnrInvsr(),
                    firstData.getOrgn());
        }

        try {
            log.info("[{}][{}] 4. 데이터 파싱 시작", stockCode, dateStr);

            List<InvestorChart> charts = response.getChartDataList().stream()
                    .map(data -> mapToInvestorChart(data, command))
                    .collect(Collectors.toList());

            log.info("[{}][{}] 4. 데이터 파싱 완료 - {}건", stockCode, dateStr, charts.size());

            if (charts.isEmpty()) {
                log.warn("[{}][{}] 4. 파싱된 데이터가 없습니다 - API 응답은 있었으나 변환 가능한 데이터 없음",
                        stockCode, dateStr);
                return Flux.empty();
            }

            // DB 저장 전 로그
            log.info("[{}][{}] 4. DB 저장 시작 - 저장할 데이터: {}건", stockCode, dateStr, charts.size());

            List<InvestorChart> savedCharts;
            try {
                savedCharts = chartPort.saveAll(charts);

                if (savedCharts.isEmpty()) {
                    log.error("[{}][{}] 4. DB 저장 실패 - 저장 시도 {}건, 실제 저장 0건",
                            stockCode, dateStr, charts.size());
                    log.error("[{}][{}] DB 저장 0건 원인: 중복 데이터이거나 제약조건 위반 가능성",
                            stockCode, dateStr);
                } else if (savedCharts.size() < charts.size()) {
                    log.warn("[{}][{}] 4. DB 일부 저장 - 저장 시도 {}건, 실제 저장 {}건",
                            stockCode, dateStr, charts.size(), savedCharts.size());
                    log.warn("[{}][{}] 일부만 저장된 원인: 일부 데이터가 중복이거나 제약조건 위반",
                            stockCode, dateStr);
                } else {
                    log.info("[{}][{}] 4. DB 저장 완료 - {}건", stockCode, dateStr, savedCharts.size());
                }

            } catch (Exception dbException) {
                log.error("[{}][{}] 4. DB 저장 중 예외 발생: {}",
                        stockCode, dateStr, dbException.getMessage(), dbException);
                log.error("[{}][{}] DB 오류 상세: {}",
                        stockCode, dateStr, dbException.getClass().getSimpleName());
                return Flux.error(new RuntimeException("DB 저장 실패: " + dbException.getMessage(), dbException));
            }

            return Flux.fromIterable(savedCharts);

        } catch (Exception e) {
            log.error("[{}][{}] 4. 데이터 파싱/저장 실패: {}", stockCode, dateStr, e.getMessage(), e);
            return Flux.error(e);
        }
    }

    /**
     * TR 응답 → 도메인 모델 변환
     */
    private InvestorChart mapToInvestorChart(InvestorChartTR.ChartData data, FetchInvestorChartCommand command) {
        LocalDate date = LocalDate.parse(data.getDt(), DATE_FORMATTER);

        return InvestorChart.builder()
                .stockCode(command.stockCode())
                .date(date)
                .unitType(command.unitType())
                // 시세 정보
                .currentPrice(data.getCurPrc())
                .previousDayDifference(data.getPredPre())
                .accumulatedTradeAmount(data.getAccTrdePrica())
                // 투자자별 데이터
                .investorData(InvestorChart.InvestorData.builder()
                        .individual(data.getIndInvsr())
                        .foreigner(data.getFrgnrInvsr())
                        .institution(data.getOrgn())
                        .nationalForeign(data.getNatfor())
                        .build())
                // 기관 세부 내역
                .institutionBreakdown(InvestorChart.InstitutionBreakdown.builder()
                        .financialInvest(data.getFnncInvt())
                        .insurance(data.getInsrnc())
                        .investment(data.getInvtrt())
                        .etcFinancial(data.getEtcFnnc())
                        .bank(data.getBank())
                        .pensionFund(data.getPenfndEtc())
                        .privateFund(data.getSamoFund())
                        .nation(data.getNatn())
                        .etcCorporation(data.getEtcCorp())
                        .build())
                .build();
    }

        /**
         * 프론트로부터 받은 ChartDataDto 목록을 저장 (배치 저장)
         */
        public int saveBatchFromDto(java.util.List<com.stocktrading.kiwoom.dto.InvestorChartResponse.ChartDataDto> items) {
        if (items == null || items.isEmpty()) return 0;
        List<InvestorChart> charts = items.stream().map(dto -> {
            InvestorChart.UnitType unit = InvestorChart.UnitType.valueOf(dto.getUnitType());
            return InvestorChart.builder()
                .stockCode(dto.getStockCode())
                .date(dto.getDate())
                .unitType(unit)
                .currentPrice(dto.getCurrentPrice())
                .previousDayDifference(dto.getPreviousDayDifference())
                .accumulatedTradeAmount(dto.getAccumulatedTradeAmount())
                .investorData(InvestorChart.InvestorData.builder()
                    .individual(dto.getInvestor().getIndividual())
                    .foreigner(dto.getInvestor().getForeigner())
                    .institution(dto.getInvestor().getInstitution())
                    .nationalForeign(dto.getInvestor().getNationalForeign())
                    .build())
                .institutionBreakdown(InvestorChart.InstitutionBreakdown.builder()
                    .financialInvest(dto.getInstitutionBreakdown().getFinancialInvest())
                    .insurance(dto.getInstitutionBreakdown().getInsurance())
                    .investment(dto.getInstitutionBreakdown().getInvestment())
                    .etcFinancial(dto.getInstitutionBreakdown().getEtcFinancial())
                    .bank(dto.getInstitutionBreakdown().getBank())
                    .pensionFund(dto.getInstitutionBreakdown().getPensionFund())
                    .privateFund(dto.getInstitutionBreakdown().getPrivateFund())
                    .nation(dto.getInstitutionBreakdown().getNation())
                    .etcCorporation(dto.getInstitutionBreakdown().getEtcCorporation())
                    .build())
                .build();
        }).collect(java.util.stream.Collectors.toList());
        List<InvestorChart> saved = chartPort.saveAll(charts);
        return saved.size();
        }

    /**
     * 단일 날짜 연속조회 (cont-yn="Y" 처리)
     * 100건씩 수신할 때마다 누적 개수를 emit하는 Flux 반환
     */
    /**
     * 단일 날짜 연속조회 (cont-yn="Y" 처리) - 중단 조건 포함
     * 100건씩 수신할 때마다 누적 개수를 emit하는 Flux 반환
     * 
     * @param stopDate    이 날짜 이하의 데이터가 수신되면 연속조회 중단 (null이면 중단 조건 없음)
     * @param stopReached 중단 조건 도달 여부를 저장하는 AtomicBoolean
     */
    private Flux<Integer> fetchSingleDateWithContinuationFlux(
            String stockCode,
            LocalDate date,
            String amtQtyTp,
            String trdeTp,
            InvestorChart.UnitType unitType,
            String contYn,
            String nextKey,
            java.util.concurrent.atomic.AtomicInteger totalReceived,
            LocalDate stopDate,
            java.util.concurrent.atomic.AtomicBoolean stopReached
    ) {
        String cachedToken = oAuthTokenService.getCachedToken();
        if (cachedToken == null || cachedToken.isEmpty()) {
            return Flux.error(new IllegalStateException("인증 토큰을 찾을 수 없습니다"));
        }

        // 이미 중단 조건에 도달했으면 더 이상 진행하지 않음
        if (stopReached != null && stopReached.get()) {
            log.info("[{}] 중단 조건 도달 - 연속조회 종료", stockCode);
            return Flux.just(totalReceived.get());
        }

        FetchInvestorChartCommand command = new FetchInvestorChartCommand(
                stockCode, date, amtQtyTp, trdeTp, unitType
        );

        log.info("[{}][{}] 연속조회 요청 - cont-yn: {}, next-key: {}, stopDate: {}",
                stockCode, date.format(DATE_FORMATTER), contYn, nextKey,
                stopDate != null ? stopDate.format(DATE_FORMATTER) : "없음");

        return callInvestorChartApiWithHeaders(command, cachedToken, contYn, nextKey)
                .flatMapMany(headeredResponse -> {
                    // 1. 데이터 파싱 (저장 전 필터링)
                    return parseResponseWithStopCondition(headeredResponse.response(), command, stopDate, stopReached)
                            .collectList()
                            .flatMapMany(filteredCharts -> {
                                // 필터링된 데이터만 저장
                                List<InvestorChart> savedCharts = filteredCharts.isEmpty()
                                        ? java.util.Collections.emptyList()
                                        : chartPort.saveAll(filteredCharts);

                                int currentReceived = savedCharts.size();
                                totalReceived.addAndGet(currentReceived);
                                int currentTotal = totalReceived.get();

                                log.info("[{}][{}] 수신 완료 - 현재 {}건, 누적 {}건, cont-yn: {}, 중단도달: {}",
                                        stockCode, date.format(DATE_FORMATTER),
                                        currentReceived, currentTotal, headeredResponse.contYn(),
                                        stopReached != null ? stopReached.get() : false);

                                // 2. 중단 조건 도달 시 연속조회 종료
                                if (stopReached != null && stopReached.get()) {
                                    log.info("[{}][{}] 중단 조건 도달 - stopDate({}) 이하 데이터 수신, 연속조회 종료",
                                            stockCode, date.format(DATE_FORMATTER),
                                            stopDate != null ? stopDate.format(DATE_FORMATTER) : "N/A");
                                    return Flux.just(currentTotal);
                                }

                                // 3. 연속조회 필요 여부 확인
                                if ("Y".equals(headeredResponse.contYn()) &&
                                    headeredResponse.nextKey() != null &&
                                    !headeredResponse.nextKey().isEmpty()) {

                                    log.info("[{}][{}] 연속조회 대기 중 (2.5초)...",
                                            stockCode, date.format(DATE_FORMATTER));
                                    
                                    // 재귀 호출로 다음 100건 수신 (2.5초 대기 후)
                                    return Flux.concat(
                                        Flux.just(currentTotal),
                                        Mono.delay(java.time.Duration.ofMillis(2500))
                                            .thenMany(fetchSingleDateWithContinuationFlux(
                                                stockCode, date, amtQtyTp, trdeTp, unitType,
                                                "Y", headeredResponse.nextKey(), totalReceived,
                                                stopDate, stopReached
                                            ))
                                    );
                                } else {
                                    // 연속조회 종료
                                    log.info("[{}][{}] 연속조회 완료 - 총 {}건 수신",
                                            stockCode, date.format(DATE_FORMATTER), currentTotal);
                                    return Flux.just(currentTotal);
                                }
                            });
                })
                .onErrorResume(error -> {
                    log.error("[{}][{}] 연속조회 오류: {}",
                            stockCode, date.format(DATE_FORMATTER), error.getMessage());
                    return Flux.just(totalReceived.get());
                });
    }

    /**
     * 중단 조건을 적용하여 응답 파싱 (stopDate 이하 데이터는 제외하고 중단 플래그 설정)
     */
    private Flux<InvestorChart> parseResponseWithStopCondition(
            InvestorChartTR.Response response,
            FetchInvestorChartCommand command,
            LocalDate stopDate,
            java.util.concurrent.atomic.AtomicBoolean stopReached) {

        String stockCode = command.stockCode();
        String dateStr = command.date().format(DATE_FORMATTER);

        if (!response.isSuccess()) {
            log.error("[{}][{}] API 응답 오류 - Code: {}, Message: {}",
                    stockCode, dateStr, response.getReturnCode(), response.getReturnMsg());
            return Flux.empty();
        }

        if (response.getChartDataList().isEmpty()) {
            log.warn("[{}][{}] → 수신 데이터: 0건", stockCode, dateStr);
            return Flux.empty();
        }

        log.info("[{}][{}] → 원본 수신 데이터: {}건", stockCode, dateStr, response.getChartDataList().size());

        try {
            List<InvestorChart> filteredCharts = new java.util.ArrayList<>();

            for (var data : response.getChartDataList()) {
                LocalDate dataDate = LocalDate.parse(data.getDt(), DATE_FORMATTER);

                // 중단 조건 체크: stopDate 이하의 데이터가 있으면 중단 플래그 설정
                if (stopDate != null && !dataDate.isAfter(stopDate)) {
                    log.info("[{}] 중단 조건 감지 - 수신 dt: {}, stopDate: {}",
                            stockCode, dataDate.format(DATE_FORMATTER), stopDate.format(DATE_FORMATTER));
                    if (stopReached != null) {
                        stopReached.set(true);
                    }
                    // stopDate 이하 데이터는 저장하지 않음
                    continue;
                }

                // 유효한 데이터만 변환하여 추가
                filteredCharts.add(mapToInvestorChart(data, command));
            }

            log.info("[{}][{}] → 필터링 후 저장 대상: {}건 (중단도달: {})",
                    stockCode, dateStr, filteredCharts.size(),
                    stopReached != null ? stopReached.get() : false);

            return Flux.fromIterable(filteredCharts);

        } catch (Exception e) {
            log.error("[{}][{}] 데이터 파싱 실패: {}", stockCode, dateStr, e.getMessage(), e);
            return Flux.error(e);
        }
    }

    /**
     * 단일 날짜 연속조회 (cont-yn="Y" 처리) - 기존 호환성 유지
     * @return 수신한 총 데이터 개수
     */
    private Mono<Integer> fetchSingleDateWithContinuation(
            String stockCode,
            LocalDate date,
            String amtQtyTp,
            String trdeTp,
            InvestorChart.UnitType unitType,
            String contYn,
            String nextKey,
            java.util.concurrent.atomic.AtomicInteger totalReceived
    ) {
        return fetchSingleDateWithContinuationFlux(
                stockCode, date, amtQtyTp, trdeTp, unitType,
                contYn, nextKey, totalReceived,
                null, null  // 중단 조건 없음
        ).reduce(0, (acc, val) -> val);  // 마지막 값만 반환
    }

    /**
     * 최신 데이터 수집 (오늘 → maxStockDate까지)
     * @param stockCode    종목코드
     * @param maxStockDate DB의 최신 날짜 (이 날짜 이하 데이터는 저장 안함)
     * @return 수신한 데이터 개수를 emit하는 Flux
     */
    private Flux<Integer> fetchRecentDataFlux(
            String stockCode,
            LocalDate maxStockDate,
            String amtQtyTp,
            String trdeTp,
            InvestorChart.UnitType unitType,
            java.util.concurrent.atomic.AtomicInteger totalReceived) {
        LocalDate startDate = LocalDate.now();  // 오늘부터 시작

        log.info("[{}] 최신 데이터 수집 시작 - 오늘({}) → maxStockDate({})까지",
                stockCode, startDate.format(DATE_FORMATTER),
                maxStockDate != null ? maxStockDate.format(DATE_FORMATTER) : "없음");

        java.util.concurrent.atomic.AtomicBoolean stopReached = new java.util.concurrent.atomic.AtomicBoolean(false);

        return fetchSingleDateWithContinuationFlux(
                stockCode, startDate, amtQtyTp, trdeTp, unitType,
                "N", "", totalReceived,
                maxStockDate, stopReached  // maxStockDate 이하 데이터 수신 시 중단
        );
    }

    /**
     * 과거 데이터 수집 (minStockDate-1 → minTargetDate까지)
     * @param stockCode     종목코드
     * @param minStockDate  DB의 가장 오래된 날짜
     * @param minTargetDate 목표 최소 날짜 (2000-01-02, 이 날짜 이하 데이터는 저장 안함)
     * @return 수신한 데이터 개수를 emit하는 Flux
     */
    private Flux<Integer> fetchPastDataFlux(
            String stockCode,
            LocalDate minStockDate,
            LocalDate minTargetDate,
            String amtQtyTp,
            String trdeTp,
            InvestorChart.UnitType unitType,
            java.util.concurrent.atomic.AtomicInteger totalReceived) {
        LocalDate startDate = minStockDate.minusDays(1);  // minStockDate - 1일부터 시작

        log.info("[{}] 과거 데이터 수집 시작 - minStockDate-1({}) → minTargetDate({})까지",
                stockCode, startDate.format(DATE_FORMATTER), minTargetDate.format(DATE_FORMATTER));

        java.util.concurrent.atomic.AtomicBoolean stopReached = new java.util.concurrent.atomic.AtomicBoolean(false);

        return fetchSingleDateWithContinuationFlux(
                stockCode, startDate, amtQtyTp, trdeTp, unitType,
                "N", "", totalReceived,
                minTargetDate, stopReached  // minTargetDate 이하 데이터 수신 시 중단
        );
    }

    /**
     * 역방향 연속조회 시작 (특정 날짜부터 과거로) - 중단 조건 없음
     * 100건씩 수신할 때마다 진행상황을 emit하는 Flux 반환
     */
    private Flux<Integer> fetchBackwardWithContinuationFlux(
            String stockCode,
            LocalDate targetDate,
            String amtQtyTp,
            String trdeTp,
            InvestorChart.UnitType unitType,
            java.util.concurrent.atomic.AtomicInteger totalReceived
    ) {
        log.info("[{}][{}] 역방향 수집 시작", stockCode, targetDate.format(DATE_FORMATTER));

        return fetchSingleDateWithContinuationFlux(
                stockCode, targetDate, amtQtyTp, trdeTp, unitType,
                "N", "", totalReceived,
                null, null  // 중단 조건 없음
        );
    }

    /**
     * 역방향 연속조회 시작 (특정 날짜부터 과거로) - 기존 호환성 유지
     */
    private Mono<Integer> fetchBackwardWithContinuation(
            String stockCode,
            LocalDate targetDate,
            String amtQtyTp,
            String trdeTp,
            InvestorChart.UnitType unitType
    ) {
        java.util.concurrent.atomic.AtomicInteger totalReceived =
                new java.util.concurrent.atomic.AtomicInteger(0);
        
        return fetchBackwardWithContinuationFlux(
                stockCode, targetDate, amtQtyTp, trdeTp, unitType, totalReceived
        ).reduce(0, (acc, val) -> val);  // 마지막 값만 반환
    }

    /**
     * 한 종목에 대해 2가지 수집 작업 수행:
     * 1. 최신 데이터: 오늘 → maxStockDate (수신 dt <= maxStockDate이면 중단)
     * 2. 과거 데이터: minStockDate-1 → 2000-01-02 (수신 dt <= 2000-01-02이면 중단)
     * 100건씩 수신할 때마다 진행상황을 emit
     */
    private Flux<Kospi200BatchProgress> fetchStockUntilTargetDate(
            com.stocktrading.kiwoom.domain.model.StockInfo stock,
            LocalDate minTargetDate,  // 2000-01-02
            String amtQtyTp,
            String trdeTp,
            String unitTp,
            java.util.concurrent.atomic.AtomicInteger processedCount,
            int totalCount,
            java.util.List<String> allErrors,
            java.util.concurrent.atomic.AtomicInteger totalReceivedAll,
            java.util.concurrent.atomic.AtomicInteger totalSavedAll
    ) {
        return Flux.defer(() -> {
            try {
                LocalDate maxTargetDate = LocalDate.now();  // 오늘

                // 1. DB에서 날짜 범위 조회
                InvestorChartPort.DateRange dateRange = chartPort.findDateRangeByStock(stock.getCode());
                LocalDate minStockDate = dateRange != null ? dateRange.minDate() : null;
                LocalDate maxStockDate = dateRange != null ? dateRange.maxDate() : null;

                log.info("[{}/{}] {} ({}) - DB 날짜 범위: {} ~ {}",
                        processedCount.get() + 1, totalCount,
                        stock.getName(), stock.getCode(),
                        minStockDate != null ? minStockDate.format(DATE_FORMATTER) : "없음",
                        maxStockDate != null ? maxStockDate.format(DATE_FORMATTER) : "없음");

                // 2. 수집 필요 여부 판단
                boolean needRecentData = false;
                boolean needPastData = false;

                if (minStockDate == null || maxStockDate == null) {
                    // DB에 데이터 없음 → 둘 다 수집 필요
                    needRecentData = true;
                    needPastData = true;
                    log.info("[{}] DB에 데이터 없음 → 전체 수집", stock.getCode());
                } else {
                    // 최신 데이터 수집 필요 여부: 오늘 > maxStockDate
                    if (maxTargetDate.isAfter(maxStockDate)) {
                        needRecentData = true;
                        log.info("[{}] 최신 데이터 수집 필요: 오늘({}) > maxStockDate({})",
                                stock.getCode(), maxTargetDate.format(DATE_FORMATTER), maxStockDate.format(DATE_FORMATTER));
                    }
                    // 과거 데이터 수집 필요 여부: minTargetDate < minStockDate
                    if (minTargetDate.isBefore(minStockDate)) {
                        needPastData = true;
                        log.info("[{}] 과거 데이터 수집 필요: minTargetDate({}) < minStockDate({})",
                                stock.getCode(), minTargetDate.format(DATE_FORMATTER), minStockDate.format(DATE_FORMATTER));
                    }
                }

                // 3. 양쪽 모두 수집 불필요 → 스킵
                if (!needRecentData && !needPastData) {
                    int current = processedCount.incrementAndGet();
                    log.info("[{}/{}] {} - 이미 완전 수집됨 (스킵): {} ~ {}",
                            current, totalCount, stock.getName(),
                            minStockDate.format(DATE_FORMATTER), maxStockDate.format(DATE_FORMATTER));

                    Kospi200BatchProgress progress = new Kospi200BatchProgress();
                    progress.currentStockCode = stock.getCode();
                    progress.currentStockName = stock.getName();
                    progress.processedCount = current;
                    progress.totalCount = totalCount;
                    progress.receivedCount = 0;
                    progress.savedCount = 0;
                    progress.duplicateCount = 0;
                    progress.errorCount = 0;
                    progress.cumulativeReceivedCount = totalReceivedAll.get();
                    progress.cumulativeSavedCount = totalSavedAll.get();
                    progress.errors = new java.util.ArrayList<>(allErrors);
                    progress.completed = (current == totalCount);
                    return Flux.just(progress);
                }

                // 4. 수집 실행
                int currentDisplay = processedCount.get();
                java.util.concurrent.atomic.AtomicInteger currentStockReceived = new java.util.concurrent.atomic.AtomicInteger(0);
                java.util.concurrent.atomic.AtomicInteger currentStockSaved = new java.util.concurrent.atomic.AtomicInteger(0);

                final boolean finalNeedRecentData = needRecentData;
                final boolean finalNeedPastData = needPastData;
                final LocalDate finalMinStockDate = minStockDate;
                final LocalDate finalMaxStockDate = maxStockDate;

                // 최신 데이터 수집 Flux
                Flux<Integer> recentDataFlux;
                if (finalNeedRecentData) {
                    recentDataFlux = fetchRecentDataFlux(
                            stock.getCode(), finalMaxStockDate, amtQtyTp, trdeTp,
                            InvestorChart.UnitType.fromCode(unitTp), currentStockReceived)
                            .doOnNext(count -> currentStockSaved.set(count));
                } else {
                    recentDataFlux = Flux.empty();
                }

                // 과거 데이터 수집 Flux (최신 수집 후 2.5초 대기)
                Flux<Integer> pastDataFlux;
                if (finalNeedPastData) {
                    LocalDate pastStartDate = finalMinStockDate != null ? finalMinStockDate : maxTargetDate;
                    pastDataFlux = Mono.delay(java.time.Duration.ofMillis(2500))
                            .thenMany(fetchPastDataFlux(
                                    stock.getCode(), pastStartDate, minTargetDate, amtQtyTp, trdeTp,
                                    InvestorChart.UnitType.fromCode(unitTp), currentStockReceived)
                                    .doOnNext(count -> currentStockSaved.set(count)));
                } else {
                    pastDataFlux = Flux.empty();
                }

                // 순차 실행 및 진행상황 emit
                return Flux.concat(recentDataFlux, pastDataFlux)
                        .map(savedCount -> {
                            int currentReceived = currentStockReceived.get();
                            int tempCumulativeReceived = totalReceivedAll.get() + currentReceived;
                            int tempCumulativeSaved = totalSavedAll.get() + savedCount;

                            Kospi200BatchProgress progress = new Kospi200BatchProgress();
                            progress.currentStockCode = stock.getCode();
                            progress.currentStockName = stock.getName();
                            progress.processedCount = currentDisplay;
                            progress.totalCount = totalCount;
                            progress.receivedCount = currentReceived;
                            progress.savedCount = savedCount;
                            progress.cumulativeReceivedCount = tempCumulativeReceived;
                            progress.cumulativeSavedCount = tempCumulativeSaved;
                            progress.errors = new java.util.ArrayList<>(allErrors);
                            progress.completed = false;

                            log.info("[{}/{}] {} - 중간 업데이트: 현재 종목(수신 {}건, 저장 {}건), 전체 누적(수신 {}건, 저장 {}건)",
                                    currentDisplay, totalCount, stock.getName(),
                                    currentReceived, savedCount, tempCumulativeReceived, tempCumulativeSaved);

                            return progress;
                        })
                        .concatWith(
                                // 모든 수집 완료 후 종목 완료 처리
                                Flux.defer(() -> {
                                    int finalSavedCount = currentStockSaved.get();
                                    int finalReceivedCount = currentStockReceived.get();
                                    totalReceivedAll.addAndGet(finalReceivedCount);
                                    totalSavedAll.addAndGet(finalSavedCount);
                                    int completedCount = processedCount.incrementAndGet();

                                    log.info("[{}/{}] {} - 종목 수집 완료! 수신 {}건, 저장 {}건",
                                            completedCount, totalCount, stock.getName(), finalReceivedCount, finalSavedCount);

                                    Kospi200BatchProgress finalProgress = new Kospi200BatchProgress();
                                    finalProgress.currentStockCode = stock.getCode();
                                    finalProgress.currentStockName = stock.getName();
                                    finalProgress.processedCount = completedCount;
                                    finalProgress.totalCount = totalCount;
                                    finalProgress.receivedCount = finalReceivedCount;
                                    finalProgress.savedCount = finalSavedCount;
                                    finalProgress.cumulativeReceivedCount = totalReceivedAll.get();
                                    finalProgress.cumulativeSavedCount = totalSavedAll.get();
                                    finalProgress.errors = new java.util.ArrayList<>(allErrors);
                                    finalProgress.completed = (completedCount == totalCount);
                                    return Flux.just(finalProgress);
                                }));

            } catch (Exception e) {
                int current = processedCount.incrementAndGet();
                String errorMsg = String.format("%s (%s): %s", stock.getName(), stock.getCode(), e.getMessage());
                allErrors.add(errorMsg);
                log.error("[{}/{}] 처리 실패: {}", current, totalCount, errorMsg);

                Kospi200BatchProgress progress = new Kospi200BatchProgress();
                progress.currentStockCode = stock.getCode();
                progress.currentStockName = stock.getName();
                progress.processedCount = current;
                progress.totalCount = totalCount;
                progress.errorCount = 1;
                progress.cumulativeReceivedCount = totalReceivedAll.get();
                progress.cumulativeSavedCount = totalSavedAll.get();
                progress.errors = new java.util.ArrayList<>(allErrors);
                progress.completed = (current == totalCount);
                return Flux.just(progress);
            }
        });
    }

    /**
     * KOSPI200 전체 종목 배치 수집 (순차 처리, 종목간 2.5초 대기)
     * 각 종목에 대해 2가지 수집 작업 수행:
     * 1. 최신 데이터: 오늘 → maxStockDate
     * 2. 과거 데이터: minStockDate-1 → 2000-01-02
     */
    public Flux<Kospi200BatchProgress> fetchKospi200Batch(String amtQtyTp, String trdeTp, String unitTp) {
        log.info("========================================");
        log.info("KOSPI200 배치 수집 시작");
        log.info("========================================");

        // 1. KOSPI200 종목 목록 조회 (tb_stock_list_meta에서)
        List<com.stocktrading.kiwoom.domain.model.StockInfo> kospi200Stocks =
            stockListPort.findAllKospi200Stocks();

        if (kospi200Stocks.isEmpty()) {
            log.error("KOSPI200 종목이 없습니다. 먼저 종목 리스트를 수집해주세요.");
            return Flux.error(new IllegalStateException("KOSPI200 종목이 없습니다"));
        }

        final int totalCount = kospi200Stocks.size();
        final LocalDate TARGET_START_DATE = LocalDate.of(2000, 1, 2);
        log.info("KOSPI200 종목 총 {}개 수집 시작 (tb_stock_list_meta)", totalCount);
        log.info("목표 시작 날짜: {} (이 날짜까지 역방향 수집)", TARGET_START_DATE);

        final java.util.concurrent.atomic.AtomicInteger processedCount =
            new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.List<String> allErrors = new java.util.concurrent.CopyOnWriteArrayList<>();

        // 누적 통계 카운터 (모든 종목의 합계)
        final java.util.concurrent.atomic.AtomicInteger totalReceivedAll =
            new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger totalSavedAll =
            new java.util.concurrent.atomic.AtomicInteger(0);

        // 2. 각 종목에 대해 2가지 수집 작업 순차 처리
        return Flux.fromIterable(kospi200Stocks)
            .concatMap(stock -> {
                log.info("[{}/{}] 종목 시작: {} ({})",
                        processedCount.get() + 1, totalCount, stock.getName(), stock.getCode());

                // 각 종목에 대해 최신+과거 데이터 수집
                return fetchStockUntilTargetDate(
                        stock, TARGET_START_DATE, amtQtyTp, trdeTp, unitTp,
                        processedCount, totalCount, allErrors,
                        totalReceivedAll, totalSavedAll
                ).concatWith(
                    // 종목 완료 후 2.5초 대기
                    Mono.delay(java.time.Duration.ofMillis(2500)).then(Mono.empty())
                );
            })
            .doOnComplete(() -> {
                log.info("========================================");
                log.info("KOSPI200 배치 수집 완료!");
                log.info("총 {}개 종목 처리, 오류: {}건", totalCount, allErrors.size());
                log.info("총 누적 수신: {}건, 총 누적 저장: {}건",
                    totalReceivedAll.get(), totalSavedAll.get());
                log.info("========================================");
            });
    }

    /**
     * KOSPI200 배치 처리 진행상황 DTO
     */
    @lombok.Data
    public static class Kospi200BatchProgress {
        private String currentStockCode;
        private String currentStockName;
        private int processedCount;
        private int totalCount;
        private int receivedCount;     // 현재 종목의 수신 건수
        private int savedCount;        // 현재 종목의 저장 건수
        private int duplicateCount;    // 현재 종목의 중복 건수
        private int errorCount;        // 현재 종목의 오류 건수
        private int cumulativeReceivedCount;  // 전체 누적 수신 건수 (모든 종목 합계)
        private int cumulativeSavedCount;     // 전체 누적 저장 건수 (모든 종목 합계)
        private java.util.List<String> errors;  // 누적 오류 목록
        private boolean completed;
    }
}
