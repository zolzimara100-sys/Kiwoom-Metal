package com.stocktrading.kiwoom.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.stocktrading.kiwoom.config.KiwoomApiConfig;
import com.stocktrading.kiwoom.dto.TokenResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class KiwoomAuthService {
    
    private final WebClient webClient;
    private final KiwoomApiConfig config;
    
    private String accessToken;
    private long tokenExpireTime;
    
    /**
     * 토큰 발급 - OAuth 2.0 방식
     */
    public Mono<TokenResponse> issueToken() {
        log.info("토큰 발급 요청 시작");
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("grant_type", "client_credentials");
        requestBody.put("appkey", config.getAppKey());
        requestBody.put("secretkey", config.getAppSecret());
        
        return webClient.post()
                .uri(config.getAuthUrl() + "/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .doOnNext(response -> {
                    log.info("========================================");
                    log.info("키움 API 응답: return_code={}, return_msg={}", 
                            response.getReturnCode(), response.getReturnMsg());
                    log.info("========================================");
                    
                    if (response.getReturnCode() == 0 && response.getToken() != null) {
                        this.accessToken = response.getToken();
                        this.tokenExpireTime = System.currentTimeMillis() + (response.getExpiresIn() * 1000);
                        
                        System.out.println("\n" + "=".repeat(80));
                        System.out.println("✅ 키움증권 토큰 발급 성공!");
                        System.out.println("=".repeat(80));
                        System.out.println("� 토큰: " + response.getToken());
                        System.out.println("📅 만료일시: " + response.getExpiresDt());
                        System.out.println("📝 메시지: " + response.getReturnMsg());
                        System.out.println("=".repeat(80) + "\n");
                        
                        log.info("토큰 발급 성공: {}", response.getToken().substring(0, 20) + "...");
                    } else {
                        System.out.println("\n" + "=".repeat(80));
                        System.out.println("❌ 키움증권 API 오류:");
                        System.out.println("=".repeat(80));
                        System.out.println("코드: " + response.getReturnCode());
                        System.out.println("메시지: " + response.getReturnMsg());
                        System.out.println("=".repeat(80) + "\n");
                        
                        log.warn("토큰 발급 실패: code={}, msg={}", 
                                response.getReturnCode(), response.getReturnMsg());
                    }
                })
                .doOnError(error -> {
                    log.error("토큰 발급 실패: {}", error.getMessage());
                    System.out.println("\n❌ 토큰 발급 실패: " + error.getMessage() + "\n");
                })
                .timeout(Duration.ofSeconds(30));
    }
    
    /**
     * 토큰 발급 (동기 방식)
     */
    public TokenResponse issueTokenSync() {
        return issueToken().block();
    }
    
    /**
     * 유효한 토큰 반환 (만료 시 자동 갱신)
     */
    public String getValidToken() {
        if (isTokenExpired()) {
            log.info("토큰이 만료되었습니다. 새로운 토큰을 발급합니다.");
            issueTokenSync();
        }
        return accessToken;
    }
    
    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired() {
        if (accessToken == null) {
            return true;
        }
        // 만료 5분 전에 갱신
        return System.currentTimeMillis() >= (tokenExpireTime - 300000);
    }
    
    /**
     * 현재 저장된 액세스 토큰 반환
     */
    public String getAccessToken() {
        return accessToken;
    }
    
    /**
     * 토큰 만료 시간 반환
     */
    public long getTokenExpireTime() {
        return tokenExpireTime;
    }
    
    /**
     * 토큰 초기화
     */
    public void clearToken() {
        this.accessToken = null;
        this.tokenExpireTime = 0;
        log.info("토큰이 초기화되었습니다.");
    }
}
