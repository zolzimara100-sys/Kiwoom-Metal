package com.stocktrading.kiwoom.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorDaily;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockInvestorDailyRepository;
import com.stocktrading.kiwoom.config.KiwoomApiConfig;
import com.stocktrading.kiwoom.dto.InvestorTradingRequest;
import com.stocktrading.kiwoom.dto.InvestorTradingResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 투자자별 거래내역(ka10059) 서비스
 * 키움 API 호출 및 DB 저장 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestorTradingService {

    private final WebClient webClient;
    private final KiwoomApiConfig config;
    private final KiwoomAuthService authService;
    private final StockInvestorDailyRepository repository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String API_PATH = "/uapi/domestic-stock/v1/quotations/inquire-investor-trading";

    /**
     * 투자자별 거래내역 조회 및 저장
     */
    @Transactional
    public Mono<List<StockInvestorDaily>> fetchAndSaveInvestorTrading(InvestorTradingRequest request) {
        String token = authService.getValidToken();

        log.info("투자자별 거래내역 조회 시작 - 종목: {}, 기간: {} ~ {}",
                request.getStkCd(), request.getInqrStrtDt(), request.getInqrEndDt());

        return callInvestorTradingApi(token, request)
                .map(response -> convertAndSave(response, request))
                .doOnSuccess(entities -> log.info("투자자별 거래내역 저장 완료 - {} 건", entities.size()))
                .doOnError(error -> log.error("투자자별 거래내역 조회/저장 실패: {}", error.getMessage(), error));
    }

    /**
     * 키움 API 호출
     */
    private Mono<InvestorTradingResponse> callInvestorTradingApi(String token, InvestorTradingRequest request) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(API_PATH)
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J") // 시장구분 (J:주식)
                        .queryParam("FID_INPUT_ISCD", request.getStkCd())
                        .queryParam("FID_INPUT_DATE_1", request.getInqrStrtDt())
                        .queryParam("FID_INPUT_DATE_2", request.getInqrEndDt())
                        .queryParam("FID_DIV_CLS_CODE", request.getTrdeTp())
                        .queryParam("FID_INPUT_PRICE_1", request.getAmtQtyTp())
                        .queryParam("FID_RANK_SORT_CLS_CODE", request.getUnitTp())
                        .build())
                .header("Authorization", "Bearer " + token)
                .header("appkey", config.getAppKey())
                .header("appsecret", config.getAppSecret())
                .header("tr_id", "FHKST01010400") // 거래ID (투자자별 거래내역)
                .retrieve()
                .bodyToMono(InvestorTradingResponse.class)
                .doOnNext(response -> log.debug("API 응답: returnCode={}, msg={}",
                        response.getReturnCode(), response.getReturnMsg()));
    }

    /**
     * API 응답을 Entity로 변환하고 DB에 저장
     */
    private List<StockInvestorDaily> convertAndSave(InvestorTradingResponse response, InvestorTradingRequest request) {
        if (response.getOutput() == null || response.getOutput().isEmpty()) {
            log.warn("API 응답 데이터가 비어있습니다");
            return new ArrayList<>();
        }

        List<StockInvestorDaily> entities = new ArrayList<>();

        for (InvestorTradingResponse.InvestorTradingData data : response.getOutput()) {
            try {
                LocalDate date = LocalDate.parse(data.getDt(), DATE_FORMATTER);

                // 중복 체크
                if (repository.existsByStkCdAndDtAndTrdeTpAndAmtQtyTp(
                        request.getStkCd(), date, request.getTrdeTp(), request.getAmtQtyTp())) {
                    log.debug("이미 존재하는 데이터 스킵 - 종목: {}, 일자: {}", request.getStkCd(), date);
                    continue;
                }

                StockInvestorDaily entity = StockInvestorDaily.builder()
                        // Primary Keys
                        .stkCd(request.getStkCd())
                        .dt(date)
                        .trdeTp(request.getTrdeTp())
                        .amtQtyTp(request.getAmtQtyTp())
                        // Metadata
                        .unitTp(request.getUnitTp())
                        // Market Data
                        .curPrc(parseLong(data.getCurPrc()))
                        .preSig(data.getPreSig())
                        .predPre(parseLong(data.getPredPre()))
                        .fluRt(parseBigDecimal(data.getFluRt()))
                        .accTrdeQty(parseLong(data.getAccTrdeQty()))
                        .accTrdePrica(parseLong(data.getAccTrdePrica()))
                        // Investor Breakdown
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
                        // Audit
                        .regDt(LocalDateTime.now())
                        .build();

                entities.add(entity);
                log.debug("Entity 생성 완료 - 종목: {}, 일자: {}", request.getStkCd(), date);

            } catch (Exception e) {
                log.error("데이터 변환 중 오류 발생 - 일자: {}, 오류: {}",
                        data.getDt(), e.getMessage(), e);
            }
        }

        if (!entities.isEmpty()) {
            List<StockInvestorDaily> saved = repository.saveAll(entities);
            log.info("DB 저장 완료 - {} 건", saved.size());
            return saved;
        }

        return entities;
    }

    /**
     * 특정 종목의 특정 일자 데이터 조회
     */
    public List<StockInvestorDaily> getInvestorTrading(String stkCd, LocalDate date) {
        return repository.findByStkCdAndDt(stkCd, date);
    }

    /**
     * 특정 기간의 데이터 조회
     */
    public List<StockInvestorDaily> getInvestorTradingByPeriod(String stkCd, LocalDate startDate, LocalDate endDate) {
        return repository.findByStkCdAndDtBetween(stkCd, startDate, endDate);
    }

    /**
     * 특정 일자의 모든 종목 데이터 조회
     */
    public List<StockInvestorDaily> getInvestorTradingByDate(LocalDate date) {
        return repository.findByDt(date);
    }

    // ============================================
    // Helper Methods
    // ============================================

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            // 부호 제거 및 파싱
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
