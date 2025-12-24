package com.stocktrading.kiwoom.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stocktrading.kiwoom.dto.TokenResponse;
import com.stocktrading.kiwoom.service.KiwoomApiService;
import com.stocktrading.kiwoom.service.KiwoomAuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/kiwoom")
@RequiredArgsConstructor
public class KiwoomController {
    
    private final KiwoomAuthService authService;
    private final KiwoomApiService apiService;
    
    /**
     * 토큰 발급 API
     */
    @PostMapping("/auth/token")
    public Mono<ResponseEntity<TokenResponse>> issueToken() {
        log.info("토큰 발급 API 호출");
        return authService.issueToken()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("토큰 발급 실패", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
    
    /**
     * 토큰 발급 (동기 방식) API
     */
    @PostMapping("/auth/token-sync")
    public ResponseEntity<TokenResponse> issueTokenSync() {
        log.info("토큰 발급(동기) API 호출");
        try {
            TokenResponse token = authService.issueTokenSync();
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            log.error("토큰 발급 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 현재 토큰 상태 조회 API
     */
    @GetMapping("/auth/token-status")
    public ResponseEntity<Map<String, Object>> getTokenStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("hasToken", authService.getAccessToken() != null);
        status.put("isExpired", authService.isTokenExpired());
        status.put("expireTime", authService.getTokenExpireTime());
        
        if (authService.getAccessToken() != null) {
            String maskedToken = authService.getAccessToken().substring(0, 10) + "...";
            status.put("token", maskedToken);
        }
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 유효한 토큰 조회 API (만료시 자동 갱신)
     */
    @GetMapping("/auth/valid-token")
    public ResponseEntity<Map<String, String>> getValidToken() {
        try {
            String token = authService.getValidToken();
            Map<String, String> response = new HashMap<>();
            response.put("accessToken", token);
            response.put("message", "유효한 토큰을 반환했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("토큰 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 토큰 초기화 API
     */
    @DeleteMapping("/auth/token")
    public ResponseEntity<Map<String, String>> clearToken() {
        authService.clearToken();
        Map<String, String> response = new HashMap<>();
        response.put("message", "토큰이 초기화되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 헬스 체크 API
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Kiwoom API Authentication Service");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 주식 현재가 조회 API
     * @param stockCode 종목코드 (예: 015760)
     */
    @GetMapping("/stock/{stockCode}")
    public Mono<ResponseEntity<Map<String, Object>>> getStockPrice(@org.springframework.web.bind.annotation.PathVariable String stockCode) {
        log.info("주식 현재가 조회 API 호출: {}", stockCode);
        return apiService.getStockPrice(stockCode)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("주식 현재가 조회 실패", e);
                    return Mono.just(ResponseEntity.status(500).build());
                });
    }
}
