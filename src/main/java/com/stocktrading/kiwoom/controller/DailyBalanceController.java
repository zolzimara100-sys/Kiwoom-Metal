package com.stocktrading.kiwoom.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stocktrading.kiwoom.dto.DailyBalanceResponse;
import com.stocktrading.kiwoom.service.DailyBalanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 일별 잔고 수익률 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/kiwoom/balance")
@RequiredArgsConstructor
public class DailyBalanceController {
    
    private final DailyBalanceService dailyBalanceService;
    
    /**
     * 일별 잔고 수익률 조회 API
     * @param queryDate 조회일자 (YYYYMMDD)
     * @return 일별 잔고 수익률 응답
     */
    @GetMapping("/daily")
    public Mono<ResponseEntity<DailyBalanceResponse>> getDailyBalance(
            @RequestParam(name = "queryDate", required = false) String queryDate) {
        
        String resolvedQueryDate = resolveQueryDate(queryDate);
        
        log.info("일별 잔고 수익률 조회 API 호출: queryDate={}", resolvedQueryDate);
        
        return dailyBalanceService.getDailyBalance(resolvedQueryDate)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("일별 잔고 수익률 조회 실패", e);
                    return Mono.just(ResponseEntity.status(500).build());
                });
    }
    
    /**
     * 일별 잔고 수익률 조회 API (연속조회 지원)
     * @param queryDate 조회일자 (YYYYMMDD)
     * @param contYn 연속조회여부 (Y/N)
     * @param nextKey 연속조회키
     * @return 일별 잔고 수익률 응답
     */
    @GetMapping("/daily/continue")
    public Mono<ResponseEntity<DailyBalanceResponse>> getDailyBalanceContinue(
            @RequestParam(name = "queryDate", required = false) String queryDate,
            @RequestParam(name = "contYn", defaultValue = "N") String contYn,
            @RequestParam(name = "nextKey", defaultValue = "") String nextKey) {
        
        String resolvedQueryDate = resolveQueryDate(queryDate);
        
        log.info("일별 잔고 수익률 연속조회 API 호출: queryDate={}, contYn={}, nextKey={}", 
                resolvedQueryDate, contYn, nextKey);
        
        return dailyBalanceService.getDailyBalance(resolvedQueryDate, contYn, nextKey)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("일별 잔고 수익률 연속조회 실패", e);
                    return Mono.just(ResponseEntity.status(500).build());
                });
    }
    
    private String resolveQueryDate(String queryDate) {
        if (queryDate == null || queryDate.isBlank()) {
            return LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);
        }
        return queryDate;
    }
}
