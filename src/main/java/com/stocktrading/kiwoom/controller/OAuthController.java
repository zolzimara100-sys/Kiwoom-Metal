package com.stocktrading.kiwoom.controller;

import com.stocktrading.kiwoom.dto.OAuthTokenResponse;
import com.stocktrading.kiwoom.service.OAuthTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthTokenService oAuthTokenService;

    @PostMapping("/token")
    public Mono<ResponseEntity<OAuthTokenResponse>> issueToken() {
        log.info("OAuth 토큰 발급 API 호출");
        
        return oAuthTokenService.issueToken()
                .map(response -> {
                    if (response.isSuccess()) {
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(500).body(response);
                    }
                });
    }

    @GetMapping("/token/status")
    public ResponseEntity<OAuthTokenResponse> getTokenStatus() {
        OAuthTokenResponse status = oAuthTokenService.getTokenStatus();
        return ResponseEntity.ok(status);
    }

    @PostMapping("/token/refresh")
    public Mono<ResponseEntity<OAuthTokenResponse>> refreshToken() {
        log.info("토큰 강제 갱신 API 호출");

        return oAuthTokenService.refreshToken()
                .flatMap(token -> oAuthTokenService.issueToken())
                .map(response -> {
                    if (response.isSuccess()) {
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(500).body(response);
                    }
                });
    }
}
