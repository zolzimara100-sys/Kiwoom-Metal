package com.stocktrading.kiwoom.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 키움 API 호출 Port
 */
public interface KiwoomApiPort {

    /**
     * GET 요청
     */
    Mono<String> get(String apiId, String tradeId, Map<String, String> queryParams, String token);

    /**
     * POST 요청
     */
    Mono<String> post(String apiId, String tradeId, Map<String, String> body, String token);

    /**
     * POST 요청 (연속조회 헤더 포함) - ka10060용
     */
    Mono<String> postWithHeaders(String apiId, Object body, String token, String contYn, String nextKey);

    /**
     * OAuth 토큰 발급
     */
    Mono<TokenResponse> issueToken(String appKey, String appSecret);

    /**
     * 토큰 응답 DTO
     */
    record TokenResponse(
            String accessToken,
            String tokenType,
            Integer expiresIn,
            String scope
    ) {}

    // Body + 연속조회 헤더 DTO
    record BodyAndHeaders(String body, String contYn, String nextKey) {}

    // POST 요청 (Body + 연속조회 헤더 동시 반환)
    Mono<BodyAndHeaders> postWithHeadersAndReturnHeaders(String apiId, Object body, String token, String contYn, String nextKey);
}
