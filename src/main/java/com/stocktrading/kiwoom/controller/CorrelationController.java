package com.stocktrading.kiwoom.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorCorrDailyEntity;
import com.stocktrading.kiwoom.adapter.out.persistence.repository.StockInvestorCorrDailyRepository;

@RestController
@RequestMapping("/api/statistics/correlation")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
public class CorrelationController {

    private final StockInvestorCorrDailyRepository repository;

    @GetMapping("/chart/{stkCd}")
    public ResponseEntity<CorrChartResponse> getCorrelationChart(
            @PathVariable String stkCd,
            @RequestParam(defaultValue = "20") int corrDays, // 20일 상관계수 or 60일
            @RequestParam(defaultValue = "1500") int limit, // 조회 개수 (최근 N일)
            @RequestParam(required = false) String beforeDate) {

        log.info("상관계수 차트 조회 - 종목: {}, 기간: {}, Limit: {}, Before: {}", stkCd, corrDays, limit, beforeDate);

        try {
            List<StockInvestorCorrDailyEntity> entities;

            if (beforeDate != null && !beforeDate.isEmpty()) {
                entities = repository.findByStkCdAndCorrDaysBeforeDate(stkCd, corrDays, beforeDate, limit);
            } else {
                entities = repository.findRecentByStkCdAndCorrDays(stkCd, corrDays, limit);
            }

            if (entities.isEmpty()) {
                return ResponseEntity.ok(CorrChartResponse.builder()
                        .stkCd(stkCd)
                        .message("데이터가 없습니다.")
                        .data(Collections.emptyList())
                        .build());
            }

            // 날짜 오름차순 정렬 (DB에서 내림차순으로 가져왔으므로 뒤집음)
            Collections.reverse(entities);

            List<CorrDataPoint> dataPoints = new ArrayList<>();
            for (StockInvestorCorrDailyEntity entity : entities) {
                dataPoints.add(CorrDataPoint.builder()
                        .dt(entity.getDt())
                        .curPrc(entity.getCurPrc())
                        // 주요 투자자
                        .frgnrCorr(entity.getFrgnrInvsrCorr())
                        .orgnCorr(entity.getOrgnCorr())
                        .indCorr(entity.getIndInvsrCorr())
                        // 상세 기관
                        .fnncInvtCorr(entity.getFnncInvtCorr())
                        .insrncCorr(entity.getInsrncCorr())
                        .invtrtCorr(entity.getInvtrtCorr())
                        .bankCorr(entity.getBankCorr())
                        .penfndEtcCorr(entity.getPenfndEtcCorr())
                        .samoFundCorr(entity.getSamoFundCorr())
                        .natnCorr(entity.getNatnCorr())
                        .etcCorpCorr(entity.getEtcCorpCorr())
                        .natforCorr(entity.getNatforCorr())
                        .build());
            }

            return ResponseEntity.ok(CorrChartResponse.builder()
                    .stkCd(stkCd)
                    .corrDays(corrDays)
                    .sector(entities.get(0).getSector())
                    .data(dataPoints)
                    .message("조회 성공")
                    .build());

        } catch (Exception e) {
            log.error("상관계수 차트 조회 실패", e);
            return ResponseEntity.internalServerError().body(CorrChartResponse.builder()
                    .message("에러: " + e.getMessage())
                    .build());
        }
    }

    @Data
    @Builder
    public static class CorrChartResponse {
        private String stkCd;
        private String sector;
        private int corrDays;
        private String message;
        private List<CorrDataPoint> data;
    }

    @Data
    @Builder
    public static class CorrDataPoint {
        private String dt;
        private Long curPrc;

        private BigDecimal frgnrCorr;
        private BigDecimal orgnCorr;
        private BigDecimal indCorr;

        private BigDecimal fnncInvtCorr;
        private BigDecimal insrncCorr;
        private BigDecimal invtrtCorr;
        private BigDecimal bankCorr;
        private BigDecimal penfndEtcCorr;
        private BigDecimal samoFundCorr;
        private BigDecimal natnCorr;
        private BigDecimal etcCorpCorr;
        private BigDecimal natforCorr;
    }
}
