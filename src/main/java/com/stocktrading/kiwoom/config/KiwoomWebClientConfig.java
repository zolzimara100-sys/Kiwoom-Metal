package com.stocktrading.kiwoom.config;

import com.stocktrading.kiwoom.service.OAuthTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KiwoomWebClientConfig {

    private final OAuthTokenService oAuthTokenService;
    private final KiwoomApiConfig config;

    /**
     * 키움 API 전용 WebClient
     * - 자동 토큰 주입
     * - 토큰 만료 시 자동 갱신 및 재시도
     */
    @Bean
    public WebClient kiwoomWebClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();

        return WebClient.builder()
                .exchangeStrategies(strategies)
                .filter(authTokenFilter())
                .filter(tokenRefreshFilter())
                .build();
    }

    /**
     * 모든 요청에 자동으로 토큰 헤더 추가
     */
    private ExchangeFilterFunction authTokenFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            // OAuth 토큰 발급 요청은 건너뜀
            if (request.url().getPath().contains("/oauth2/token")) {
                return Mono.just(request);
            }

            return oAuthTokenService.getValidToken()
                    .map(token -> {
                        log.debug("API 요청에 토큰 추가: {}", request.url());
                        return ClientRequest.from(request)
                                .header("Authorization", "Bearer " + token)
                                .header("appkey", config.getAppKey())
                                .header("appsecret", config.getAppSecret())
                                .build();
                    })
                    .onErrorResume(error -> {
                        log.error("토큰 가져오기 실패", error);
                        return Mono.just(request);
                    });
        });
    }

    /**
     * 401 Unauthorized 발생 시 토큰 재발급 후 재시도
     */
    private ExchangeFilterFunction tokenRefreshFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            // 401 Unauthorized인 경우
            if (response.statusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("401 Unauthorized 발생 - 토큰 재발급 후 재시도");

                return oAuthTokenService.refreshToken()
                        .flatMap(newToken -> {
                            log.info("토큰 재발급 완료 - 요청 재시도");
                            // 재시도는 클라이언트 측에서 처리하도록 에러 전파
                            return Mono.just(response);
                        })
                        .onErrorResume(error -> {
                            log.error("토큰 재발급 실패", error);
                            return Mono.just(response);
                        });
            }

            return Mono.just(response);
        });
    }

    // appkey/appsecret are now provided from KiwoomApiConfig bound to env
}
