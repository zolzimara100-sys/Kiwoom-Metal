package com.stocktrading.kiwoom.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorDaily;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockInvestorDailyRepository;
import com.stocktrading.kiwoom.config.KiwoomApiConfig;
import com.stocktrading.kiwoom.domain.model.DataFetchProgress;
import com.stocktrading.kiwoom.domain.model.DataFetchProgress.FetchStatus;
import com.stocktrading.kiwoom.dto.InvestorTradingRequest;
import com.stocktrading.kiwoom.dto.InvestorTradingResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 개선된 투자자별 거래내역 서비스
 *
 * [적용된 기능]
 * 1. Rate Limiter: 초당 2회 제한 (키움 API 제약 준수)
 * 2. Batch Processing: 100건씩 나눠서 DB 저장 (메모리 효율)
 * 3. 연속조회 안전장치: 최대 페이지, 타임아웃, 중복 키 감지
 * 4. 재시도 로직: 네트워크 오류 시 자동 재시도 (지수 백오프)
 * 5. 진행률 모니터링: Redis 기반 실시간 진행률 추적
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedInvestorTradingService {

    private final WebClient webClient;
    private final KiwoomApiConfig config;
    private final KiwoomAuthService authService;
    private final StockInvestorDailyRepository repository;

    @Qualifier("kiwoomApiSafeRateLimiter")
    private final RateLimiter rateLimiter; // 우선순위 1: Rate Limiter

    private final RetryTemplate kiwoomApiRetryTemplate; // 우선순위 4: 재시도 로직

    private final RedisTemplate<String, Object> redisTemplate; // 우선순위 5: 진행률 모니터링

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String API_PATH = "/api/dostk/chart"; // ka10060 endpoint

    // ========================================
    // 우선순위 3: 연속조회 안전장치 설정
    // ========================================
    private static final int MAX_PAGES = 1000; // 최대 1000페이지
    private static final Duration MAX_FETCH_DURATION = Duration.ofMinutes(30); // 최대 30분
    private static final int BATCH_SIZE = 100; // 우선순위 2: Batch 크기

    /**
     * 대규모 데이터 조회 (모든 안전장치 적용)
     *
     * @param request 조회 요청
     * @return 진행률 정보
     */
    public DataFetchProgress fetchLargeDataSafely(InvestorTradingRequest request) {
        String jobId = generateJobId(request);
        DataFetchProgress progress = initializeProgress(jobId, request);

        try {
            log.info("========================================");
            log.info("대규모 데이터 조회 시작");
            log.info("종목: {}, 기간: {} ~ {}",
                    request.getStkCd(), request.getInqrStrtDt(), request.getInqrEndDt());
            log.info("작업 ID: {}", jobId);
            log.info("========================================");

            progress.setStatus(FetchStatus.RUNNING);
            progress.setStartTime(Instant.now());
            saveProgress(progress);

            // 연속조회 실행 (모든 안전장치 적용)
            executeWithSafetyGuards(request, progress);

            // 완료 처리
            progress.setStatus(FetchStatus.COMPLETED);
            progress.setEndTime(Instant.now());
            saveProgress(progress);

            logCompletionSummary(progress);

            return progress;

        } catch (Exception e) {
            log.error("데이터 조회 실패: {}", e.getMessage(), e);
            progress.setStatus(FetchStatus.FAILED);
            progress.setErrorMessage(e.getMessage());
            progress.setEndTime(Instant.now());
            saveProgress(progress);

            throw new RuntimeException("데이터 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 안전장치가 적용된 연속조회 실행
     */
    private void executeWithSafetyGuards(InvestorTradingRequest request, DataFetchProgress progress) {
        Instant startTime = Instant.now();
        Set<String> visitedKeys = new HashSet<>(); // 중복 키 감지
        String nextKey = null;
        boolean hasNext = true;
        int pageCount = 0;

        while (hasNext) {
            // ============================================
            // 우선순위 3: 안전장치 체크
            // ============================================

            // 1. 최대 페이지 제한
            if (pageCount >= MAX_PAGES) {
                log.warn("최대 페이지 수({})에 도달. 조회 중단", MAX_PAGES);
                break;
            }

            // 2. 타임아웃 체크
            Duration elapsed = Duration.between(startTime, Instant.now());
            if (elapsed.compareTo(MAX_FETCH_DURATION) > 0) {
                log.warn("최대 실행 시간({})을 초과. 조회 중단", MAX_FETCH_DURATION);
                break;
            }

            // 3. 중복 키 감지
            if (nextKey != null && visitedKeys.contains(nextKey)) {
                log.error("순환 참조 감지! 이미 조회한 키: {}", nextKey);
                break;
            }

            pageCount++;
            log.info("페이지 {}/{} 조회 중...", pageCount, MAX_PAGES);

            // ============================================
            // 우선순위 1: Rate Limiter 적용
            // ============================================
            log.debug("Rate Limiter 대기 중...");
            rateLimiter.acquire(); // API 호출 전 속도 제한
            log.debug("Rate Limiter 통과");

            // ============================================
            // 우선순위 4: 재시도 로직 적용
            // ============================================
            final String currentKey = nextKey; // Lambda에서 사용하기 위해 final 변수로 복사
            InvestorTradingResponse response = kiwoomApiRetryTemplate.execute(context -> {
                log.debug("API 호출 시도 (재시도 횟수: {})", context.getRetryCount());

                if (context.getRetryCount() > 0) {
                    progress.setRetryCount(progress.getRetryCount() + 1);
                    saveProgress(progress);
                }

                return callKiwoomApi(request, currentKey);
            });

            // 응답 처리
            if (response == null || response.getOutput() == null || response.getOutput().isEmpty()) {
                log.warn("빈 응답 수신. 조회 종료");
                break;
            }

            // ============================================
            // 우선순위 2: Batch Processing
            // ============================================
            processBatch(response.getOutput(), request, progress);

            // 다음 키 확인
            if (nextKey != null) {
                visitedKeys.add(nextKey);
            }

            String contYn = response.getHeader() != null ? response.getHeader().getContYn() : "N";
            nextKey = response.getHeader() != null ? response.getHeader().getNextKey() : null;

            hasNext = "Y".equals(contYn) && nextKey != null && !nextKey.isEmpty();

            // ============================================
            // 우선순위 5: 진행률 업데이트
            // ============================================
            progress.setCurrentPage(pageCount);
            progress.setLastUpdateTime(Instant.now());
            saveProgress(progress);

            logProgress(progress);
        }

        log.info("연속조회 완료 - 총 {}페이지 조회", pageCount);
    }

    /**
     * 키움 API 호출
     */
    private InvestorTradingResponse callKiwoomApi(InvestorTradingRequest request, String nextKey) {
        String token = authService.getValidToken();

        WebClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(config.getBaseUrl() + API_PATH)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .header("api-id", "ka10060")
                .bodyValue(request);

        // 연속조회 헤더 추가
        if (nextKey != null && !nextKey.isEmpty()) {
            spec.header("cont-yn", "Y");
            spec.header("next-key", nextKey);
        }

        return spec.retrieve()
                .bodyToMono(InvestorTradingResponse.class)
                .block();
    }

    /**
     * 우선순위 2: Batch Processing 구현
     * 100건씩 나눠서 DB에 저장
     */
    @Transactional
    protected void processBatch(List<InvestorTradingResponse.InvestorTradingData> dataList,
            InvestorTradingRequest request,
            DataFetchProgress progress) {

        List<StockInvestorDaily> batch = new ArrayList<>();
        int totalProcessed = 0;

        for (InvestorTradingResponse.InvestorTradingData data : dataList) {
            try {
                StockInvestorDaily entity = convertToEntity(data, request);

                // 중복 체크
                if (!isDuplicate(entity)) {
                    batch.add(entity);
                }

                // Batch 크기에 도달하면 저장
                if (batch.size() >= BATCH_SIZE) {
                    saveBatch(batch, progress);
                    totalProcessed += batch.size();
                    batch.clear(); // 메모리 해제

                    log.debug("Batch 저장 완료 - {}건 (누적: {}건)", BATCH_SIZE, totalProcessed);
                }

            } catch (Exception e) {
                log.error("데이터 변환 실패: {}", e.getMessage());
                progress.setFailureCount(progress.getFailureCount() + 1);
            }
        }

        // 남은 데이터 저장
        if (!batch.isEmpty()) {
            saveBatch(batch, progress);
            totalProcessed += batch.size();
            log.debug("마지막 Batch 저장 완료 - {}건", batch.size());
        }

        log.info("총 {}건 처리 완료", totalProcessed);
    }

    /**
     * Batch 저장 (트랜잭션)
     */
    @Transactional
    protected void saveBatch(List<StockInvestorDaily> batch, DataFetchProgress progress) {
        try {
            repository.saveAll(batch);

            progress.setSuccessCount(progress.getSuccessCount() + batch.size());
            progress.setCurrentCount(progress.getCurrentCount() + batch.size());

            log.debug("Batch 저장 성공: {}건", batch.size());

        } catch (Exception e) {
            log.error("Batch 저장 실패: {}", e.getMessage(), e);
            progress.setFailureCount(progress.getFailureCount() + batch.size());
            throw e;
        }
    }

    /**
     * Entity 변환
     */
    private StockInvestorDaily convertToEntity(InvestorTradingResponse.InvestorTradingData data,
            InvestorTradingRequest request) {
        LocalDate date = LocalDate.parse(data.getDt(), DATE_FORMATTER);

        return StockInvestorDaily.builder()
                .stkCd(request.getStkCd())
                .dt(date)
                .trdeTp(request.getTrdeTp())
                .amtQtyTp(request.getAmtQtyTp())
                .unitTp(request.getUnitTp())
                .curPrc(parseLong(data.getCurPrc()))
                .preSig(data.getPreSig())
                .predPre(parseLong(data.getPredPre()))
                .fluRt(parseBigDecimal(data.getFluRt()))
                .accTrdeQty(parseLong(data.getAccTrdeQty()))
                .accTrdePrica(parseLong(data.getAccTrdePrica()))
                .indInvsr(parseLong(data.getIndInvsr()))
                .frgnrInvsr(parseLong(data.getFrgnrInvsr()))
                .orgn(parseLong(data.getOrgn()))
                .fnncInvt(parseLong(data.getFnncInvt()))
                .insrnc(parseLong(data.getInsrnc()))
                .invtrt(parseLong(data.getInvtrt()))
                .etcFnnc(parseLong(data.getEtcFnnc()))
                .bank(parseLong(data.getBank()))
                .penfndEtc(parseLong(data.getPenfndEtc()))
                .samoFund(parseLong(data.getSamoFund()))
                .natn(parseLong(data.getNatn()))
                .etcCorp(parseLong(data.getEtcCorp()))
                .natfor(parseLong(data.getNatfor()))
                .regDt(LocalDateTime.now())
                .build();
    }

    /**
     * 중복 체크
     */
    private boolean isDuplicate(StockInvestorDaily entity) {
        return repository.existsByStkCdAndDtAndTrdeTpAndAmtQtyTp(
                entity.getStkCd(), entity.getDt(), entity.getTrdeTp(), entity.getAmtQtyTp());
    }

    // ========================================
    // 우선순위 5: 진행률 모니터링
    // ========================================

    /**
     * 진행률 초기화
     */
    private DataFetchProgress initializeProgress(String jobId, InvestorTradingRequest request) {
        return DataFetchProgress.builder()
                .jobId(jobId)
                .stockCode(request.getStkCd())
                .status(FetchStatus.PENDING)
                .totalCount(0) // 연속조회는 전체 건수를 미리 알 수 없음
                .build();
    }

    /**
     * 진행률 저장 (Redis)
     */
    private void saveProgress(DataFetchProgress progress) {
        String key = "kiwoom:fetch:progress:" + progress.getJobId();
        redisTemplate.opsForValue().set(key, progress, 24, TimeUnit.HOURS);
    }

    /**
     * 진행률 조회
     */
    public DataFetchProgress getProgress(String jobId) {
        String key = "kiwoom:fetch:progress:" + jobId;
        return (DataFetchProgress) redisTemplate.opsForValue().get(key);
    }

    /**
     * 진행률 로그 출력
     */
    private void logProgress(DataFetchProgress progress) {
        log.info("========================================");
        log.info("진행 상황: {}", progress.getProgressString());
        log.info("경과 시간: {}초", progress.getElapsedTime().getSeconds());
        log.info("처리 속도: {:.2f}건/초", progress.getProcessingRate());
        log.info("성공률: {:.1f}%", progress.getSuccessRate());
        log.info("========================================");
    }

    /**
     * 완료 요약 로그
     */
    private void logCompletionSummary(DataFetchProgress progress) {
        log.info("========================================");
        log.info("데이터 조회 완료!");
        log.info("========================================");
        log.info("종목: {}", progress.getStockCode());
        log.info("상태: {}", progress.getStatus().getDescription());
        log.info("총 처리: {}건", progress.getCurrentCount());
        log.info("성공: {}건", progress.getSuccessCount());
        log.info("실패: {}건", progress.getFailureCount());
        log.info("재시도: {}회", progress.getRetryCount());
        log.info("소요 시간: {}초", progress.getElapsedTime().getSeconds());
        log.info("평균 속도: {:.2f}건/초", progress.getProcessingRate());
        log.info("성공률: {:.1f}%", progress.getSuccessRate());
        log.info("========================================");
    }

    /**
     * Job ID 생성
     */
    private String generateJobId(InvestorTradingRequest request) {
        return String.format("stock-%s-%s-%s-%s",
                request.getStkCd(),
                request.getInqrStrtDt(),
                request.getInqrEndDt(),
                System.currentTimeMillis());
    }

    // ========================================
    // Helper Methods
    // ========================================

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            log.warn("Long 파싱 실패: {}", value);
            return 0L;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warn("BigDecimal 파싱 실패: {}", value);
            return BigDecimal.ZERO;
        }
    }
}
