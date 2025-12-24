package com.stocktrading.kiwoom.service;

import java.util.Map;

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
    
    private final WebClient webClient;
    private final KiwoomApiConfig config;
    private final KiwoomAuthService authService;
    
    /**
     * 계좌 잔고 조회 예제
     * 실제 API 엔드포인트는 키움증권 문서 참고 필요
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getAccountBalance() {
        String token = authService.getValidToken();
        
        log.info("계좌 잔고 조회 시작");
        
        return webClient.get()
                .uri(config.getBaseUrl() + "/api/v1/account/balance")
                .header("Authorization", "Bearer " + token)
                .header("appkey", config.getAppKey())
                .header("appsecret", config.getAppSecret())
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
        String token = authService.getValidToken();
        
        log.info("주식 현재가 조회 시작: {}", stockCode);
        
        return webClient.get()
                .uri(config.getBaseUrl() + "/api/v1/stock/price?code=" + stockCode)
                .header("Authorization", "Bearer " + token)
                .header("appkey", config.getAppKey())
                .header("appsecret", config.getAppSecret())
                .retrieve()
                .bodyToMono((Class<Map<String, Object>>)(Class<?>)Map.class)
                .doOnSuccess(response -> log.info("주식 현재가 조회 성공: {}", stockCode))
                .doOnError(error -> log.error("주식 현재가 조회 실패: {}", error.getMessage()));
    }
}
