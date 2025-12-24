package com.stocktrading.kiwoom.service;

import com.stocktrading.kiwoom.config.KiwoomApiConfig;
import com.stocktrading.kiwoom.dto.OAuthTokenRequest;
import com.stocktrading.kiwoom.dto.OAuthTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthTokenService {

    private final WebClient webClient;
    private final KiwoomApiConfig config;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private OAuthTokenResponse cachedTokenResponse;

    /**
     * 토큰 발급 (내부용)
     */
    private Mono<OAuthTokenResponse> issueTokenInternal() {
        log.info("OAuth 토큰 발급 요청 시작");

        OAuthTokenRequest request = OAuthTokenRequest.builder()
            .grant_type("client_credentials")
            .appkey(config.getAppKey())
            .secretkey(config.getAppSecret())
            .build();

        String authUrl = config.getAuthUrl() != null ? config.getAuthUrl() : "https://api.kiwoom.com";
        log.info("요청 URL: {}", authUrl + "/oauth2/token");

        return webClient.post()
            .uri(authUrl + "/oauth2/token")
                .header("Content-Type", "application/json;charset=UTF-8")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OAuthTokenResponse.class)
                .doOnSuccess(response -> {
                    log.info("OAuth 응답 수신 - Code: {}, Message: {}",
                            response.getReturnCode(), response.getReturnMsg());

                    if (response.getReturnCode() != null && response.getReturnCode() == 0) {
                        response.setSuccess(true);
                        response.setMessage("토큰 발급 성공");
                        log.info("OAuth 토큰 발급 성공 - 만료일: {}", response.getExpiresDt());

                        // 캐시 저장 (동기화)
                        tokenLock.lock();
                        try {
                            cachedTokenResponse = response;
                        } finally {
                            tokenLock.unlock();
                        }
                    } else {
                        response.setSuccess(false);
                        response.setMessage(response.getReturnMsg());
                        log.error("OAuth 토큰 발급 실패 - Code: {}, Message: {}",
                                response.getReturnCode(), response.getReturnMsg());
                    }
                })
                .doOnError(error -> {
                    log.error("OAuth 토큰 발급 API 호출 실패", error);
                })
                .onErrorResume(error -> {
                    OAuthTokenResponse errorResponse = OAuthTokenResponse.builder()
                            .success(false)
                            .message("토큰 발급 실패: " + error.getMessage())
                            .build();
                    return Mono.just(errorResponse);
                });
    }

    /**
     * 유효한 토큰 가져오기 (자동 갱신 포함)
     * - 토큰이 없으면 새로 발급
     * - 토큰이 만료되었으면 재발급
     * - 토큰이 곧 만료될 예정이면 재발급 (5분 전)
     */
    public Mono<String> getValidToken() {
        tokenLock.lock();
        try {
            // 1. 캐시된 토큰이 없는 경우
            if (cachedTokenResponse == null) {
                log.info("캐시된 토큰이 없습니다. 새로 발급합니다.");
                tokenLock.unlock();
                return issueTokenInternal()
                        .map(OAuthTokenResponse::getToken);
            }

            // 2. 토큰이 만료되었거나 곧 만료될 예정인 경우
            if (cachedTokenResponse.isExpired()) {
                log.warn("토큰이 만료되었습니다. 재발급합니다.");
                tokenLock.unlock();
                return issueTokenInternal()
                        .map(OAuthTokenResponse::getToken);
            }

            if (cachedTokenResponse.isExpiringSoon()) {
                log.info("토큰이 곧 만료될 예정입니다 (5분 이내). 재발급합니다.");
                tokenLock.unlock();
                return issueTokenInternal()
                        .map(OAuthTokenResponse::getToken);
            }

            // 3. 유효한 토큰 반환
            log.debug("캐시된 유효한 토큰 반환 - 만료일: {}", cachedTokenResponse.getExpiresDt());
            String token = cachedTokenResponse.getToken();
            return Mono.just(token);

        } finally {
            // unlock이 이미 호출되었는지 확인
            if (tokenLock.isHeldByCurrentThread()) {
                tokenLock.unlock();
            }
        }
    }

    /**
     * 토큰 강제 재발급 (만료 오류 발생 시 사용)
     */
    public Mono<String> refreshToken() {
        log.info("토큰 강제 재발급 요청");
        tokenLock.lock();
        try {
            cachedTokenResponse = null;  // 캐시 무효화
        } finally {
            tokenLock.unlock();
        }
        return getValidToken();
    }

    /**
     * 수동 토큰 발급 (API 엔드포인트용)
     */
    public Mono<OAuthTokenResponse> issueToken() {
        return issueTokenInternal();
    }

    /**
     * 현재 캐시된 토큰 가져오기 (deprecated)
     */
    @Deprecated
    public String getCachedToken() {
        tokenLock.lock();
        try {
            return cachedTokenResponse != null ? cachedTokenResponse.getToken() : null;
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * 현재 캐시된 만료일 가져오기 (deprecated)
     */
    @Deprecated
    public String getCachedExpiresDt() {
        tokenLock.lock();
        try {
            return cachedTokenResponse != null ? cachedTokenResponse.getExpiresDt() : null;
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * 현재 토큰 상태 정보 가져오기
     */
    public OAuthTokenResponse getTokenStatus() {
        tokenLock.lock();
        try {
            if (cachedTokenResponse == null) {
                return OAuthTokenResponse.builder()
                        .success(false)
                        .message("토큰이 없습니다")
                        .build();
            }

            return OAuthTokenResponse.builder()
                    .token(cachedTokenResponse.getToken())
                    .expiresDt(cachedTokenResponse.getExpiresDt())
                    .success(!cachedTokenResponse.isExpired())
                    .message(cachedTokenResponse.isExpired() ? "토큰이 만료되었습니다" :
                            cachedTokenResponse.isExpiringSoon() ? "토큰이 곧 만료됩니다" : "토큰이 유효합니다")
                    .build();
        } finally {
            tokenLock.unlock();
        }
    }
}
