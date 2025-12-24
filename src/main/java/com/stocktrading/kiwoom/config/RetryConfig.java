package com.stocktrading.kiwoom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

/**
 * 재시도 로직 설정
 * - 일시적 네트워크 오류 자동 복구
 * - 지수 백오프(Exponential Backoff) 적용
 */
@Configuration
@EnableRetry
public class RetryConfig {

    /**
     * 키움 API용 RetryTemplate
     * - 최대 3회 재시도
     * - 초기 대기: 1초, 최대 대기: 10초, 배율: 2.0
     */
    @Bean
    public RetryTemplate kiwoomApiRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // 재시도 정책 설정
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();

        // 재시도할 예외들
        retryableExceptions.put(SocketTimeoutException.class, true);  // 네트워크 타임아웃
        retryableExceptions.put(WebClientResponseException.ServiceUnavailable.class, true);  // 503 에러
        retryableExceptions.put(WebClientResponseException.TooManyRequests.class, true);  // 429 에러
        retryableExceptions.put(WebClientResponseException.InternalServerError.class, true);  // 500 에러 (일시적)

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);

        // 지수 백오프 정책 설정
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);  // 초기 1초 대기
        backOffPolicy.setMaxInterval(10000);     // 최대 10초 대기
        backOffPolicy.setMultiplier(2.0);        // 2배씩 증가
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    /**
     * 안전한 재시도 정책 (더 긴 대기 시간)
     * - 최대 5회 재시도
     * - 초기 대기: 2초, 최대 대기: 30초
     */
    @Bean
    public RetryTemplate safeRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(SocketTimeoutException.class, true);
        retryableExceptions.put(WebClientResponseException.ServiceUnavailable.class, true);
        retryableExceptions.put(WebClientResponseException.TooManyRequests.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000);  // 초기 2초
        backOffPolicy.setMaxInterval(30000);     // 최대 30초
        backOffPolicy.setMultiplier(2.0);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}
