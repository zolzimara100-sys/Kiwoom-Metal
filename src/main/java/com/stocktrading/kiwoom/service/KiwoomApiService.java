package com.stocktrading.kiwoom.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.stocktrading.kiwoom.config.KiwoomApiConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 키움증권 API 호출 서비스
 * 인증 토큰을 사용하여 실제 API를 호출하는 예제 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KiwoomApiService {

    private final @Qualifier("kiwoomWebClient") WebClient kiwoomWebClient;  // 키움 API 전용 WebClient (토큰 자동 주입)
    private final KiwoomApiConfig config;

    /**
     * 계좌 잔고 조회 예제
     * 실제 API 엔드포인트는 키움증권 문서 참고 필요
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getAccountBalance() {
        log.info("계좌 잔고 조회 시작");

        // kiwoomWebClient 사용 (토큰 자동 주입)
        return kiwoomWebClient.get()
                .uri(config.getBaseUrl() + "/api/v1/account/balance")
                .retrieve()
                .bodyToMono((Class<Map<String, Object>>)(Class<?>)Map.class)
                .doOnSuccess(response -> log.info("계좌 잔고 조회 성공"))
                .doOnError(error -> log.error("계좌 잔고 조회 실패: {}", error.getMessage()));
    }

    /**
     * 주식 현재가 조회 예제
     * @param stockCode 종목코드
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getStockPrice(String stockCode) {
        log.info("주식 현재가 조회 시작: {}", stockCode);

        // kiwoomWebClient 사용 (토큰 자동 주입)
        return kiwoomWebClient.get()
                .uri(config.getBaseUrl() + "/api/v1/stock/price?code=" + stockCode)
                .retrieve()
                .bodyToMono((Class<Map<String, Object>>)(Class<?>)Map.class)
                .doOnSuccess(response -> log.info("주식 현재가 조회 성공: {}", stockCode))
                .doOnError(error -> log.error("주식 현재가 조회 실패: {}", error.getMessage()));
    }
}
