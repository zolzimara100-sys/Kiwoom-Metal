package com.stocktrading.kiwoom.adapter.out.external;

import com.stocktrading.kiwoom.domain.port.out.KiwoomApiPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 키움 API 호출 Adapter
 * KiwoomApiPort 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KiwoomApiAdapter implements KiwoomApiPort {

    @Override
    public Mono<BodyAndHeaders> postWithHeadersAndReturnHeaders(String apiId, Object body, String token, String contYn, String nextKey) {
        String path = "KA10060".equalsIgnoreCase(apiId) ? INVESTOR_CHART_PATH
                : "ka10099".equalsIgnoreCase(apiId) ? STOCK_LIST_PATH
                : null;
        if (path == null) {
            log.error("지원하지 않는 API ID: {}", apiId);
            return Mono.error(new IllegalArgumentException("Unsupported API ID: " + apiId));
        }
        String fullUrl = apiUrl + path;
        return webClient.mutate().build()
                .post()
                .uri(fullUrl)
                .header("Content-Type", "application/json;charset=UTF-8")
                .header("authorization", "Bearer " + token)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("cont-yn", contYn != null ? contYn : "N")
                .header("next-key", nextKey != null ? nextKey : "")
                .header("api-id", apiId)
                .bodyValue(body)
                .exchangeToMono(resp -> resp.toEntity(String.class))
                .map(entity -> new BodyAndHeaders(
                        entity.getBody(),
                        entity.getHeaders().getFirst("cont-yn"),
                        entity.getHeaders().getFirst("next-key")));
    }

    private final WebClient webClient;

    @Value("${kiwoom.api.app-key}")
    private String appKey;

    @Value("${kiwoom.api.app-secret}")
    private String appSecret;

    @Value("${kiwoom.api.auth-url}")
    private String authUrl;

    private static final String INVESTOR_TRADING_PATH = "/uapi/domestic-stock/v1/quotations/inquire-investor-trading";
    private static final String STOCK_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String INVESTOR_CHART_PATH = "/api/dostk/chart";  // ka10060
    private static final String STOCK_LIST_PATH = "/api/dostk/stkinfo";  // ka10099

    @Value("${kiwoom.api-url:https://api.kiwoom.com}")
    private String apiUrl;

    @Override
    public Mono<String> get(String apiId, String tradeId, Map<String, String> queryParams, String token) {
        String path = getApiPath(apiId);

        log.debug("키움 API GET 호출 - API ID: {}, Trade ID: {}", apiId, tradeId);

        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(path);
                    queryParams.forEach(builder::queryParam);
                    return builder.build();
                })
                .header("Authorization", "Bearer " + token)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", tradeId)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.debug("API 호출 성공 - API ID: {}", apiId))
                .doOnError(error -> log.error("API 호출 실패 - API ID: {}, 오류: {}", apiId, error.getMessage()));
    }

    @Override
    public Mono<String> post(String apiId, String tradeId, Map<String, String> body, String token) {
        String path = getApiPath(apiId);

        log.debug("키움 API POST 호출 - API ID: {}, Trade ID: {}", apiId, tradeId);

        return webClient.post()
                .uri(path)
                .header("Authorization", "Bearer " + token)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", tradeId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.debug("API 호출 성공 - API ID: {}", apiId))
                .doOnError(error -> log.error("API 호출 실패 - API ID: {}, 오류: {}", apiId, error.getMessage()));
    }

    @Override
    public Mono<String> postWithHeaders(String apiId, Object body, String token, String contYn, String nextKey) {
        log.debug("키움 API POST 호출 (연속조회) - API ID: {}, cont-yn: {}", apiId, contYn);

        // API ID에 따라 경로 선택
        String path;
        if ("KA10060".equalsIgnoreCase(apiId)) {
            path = INVESTOR_CHART_PATH;  // /api/dostk/chart
        } else if ("ka10099".equalsIgnoreCase(apiId)) {
            path = STOCK_LIST_PATH;  // /api/dostk/stkinfo
        } else {
            log.error("지원하지 않는 API ID: {}", apiId);
            return Mono.error(new IllegalArgumentException("Unsupported API ID: " + apiId));
        }

        String fullUrl = apiUrl + path;
        log.debug("API 호출 URL: {}, Body: {}", fullUrl, body);

        return webClient.mutate().build()
                .post()
                .uri(fullUrl)
                .header("Content-Type", "application/json;charset=UTF-8")
                .header("authorization", "Bearer " + token)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("cont-yn", contYn != null ? contYn : "N")
                .header("next-key", nextKey != null ? nextKey : "")
                .header("api-id", apiId)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("API 오류 응답 - API ID: {}, Status: {}, Body: {}",
                                            apiId, response.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException(
                                            "API 오류 [" + response.statusCode() + "]: " + errorBody));
                                }))
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.debug("API 호출 성공 - API ID: {}", apiId))
                .doOnError(error -> log.error("API 호출 실패 - API ID: {}, 오류: {}", apiId, error.getMessage()));
    }

    @Override
    public Mono<TokenResponse> issueToken(String appKey, String appSecret) {
        log.info("OAuth 토큰 발급 요청");

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("grant_type", "client_credentials");
        requestBody.put("appkey", appKey);
        requestBody.put("secretkey", appSecret);

        return webClient.post()
                .uri(authUrl + "/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(KiwoomTokenResponse.class)
                .map(kiwoomResponse -> new TokenResponse(
                        kiwoomResponse.token(),
                        kiwoomResponse.tokenType(),
                        kiwoomResponse.expiresIn(),
                        kiwoomResponse.scope()
                ))
                .doOnSuccess(response -> log.info("토큰 발급 성공"))
                .doOnError(error -> log.error("토큰 발급 실패: {}", error.getMessage()));
    }

    /**
     * API ID에 따른 경로 매핑
     */
    private String getApiPath(String apiId) {
        switch (apiId) {
            case "ka10059":
                return INVESTOR_TRADING_PATH;
            case "ka10060":
                return INVESTOR_CHART_PATH;
            case "stockprice":
                return STOCK_PRICE_PATH;
            default:
                throw new IllegalArgumentException("Unknown API ID: " + apiId);
        }
    }

    /**
     * 키움 API 토큰 응답 (내부용)
     */
    private record KiwoomTokenResponse(
            String token,
            String tokenType,
            Integer expiresIn,
            String scope
    ) {}
}
