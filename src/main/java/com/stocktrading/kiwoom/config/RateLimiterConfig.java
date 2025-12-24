package com.stocktrading.kiwoom.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Rate Limiter 설정
 * 키움 API는 "1초에 2회 이하" 제한이 있음
 */
@Configuration
public class RateLimiterConfig {

    /**
     * 키움 API Rate Limiter
     * - 초당 2.0회 허용 (키움 제약: 1초에 2회 이하)
     * - 모든 API 호출이 이 Rate Limiter를 공유함
     */
    @Bean(name = "kiwoomApiRateLimiter")
    public RateLimiter kiwoomApiRateLimiter() {
        // 2.0 permits per second = 500ms 간격
        return RateLimiter.create(2.0);
    }

    /**
     * 안전 여유를 둔 Rate Limiter (권장) - 기본 Bean
     * - 초당 1.5회 허용 (약 666ms 간격)
     * - 네트워크 지연 고려하여 여유있게 설정
     */
    @Primary
    @Bean(name = "kiwoomApiSafeRateLimiter")
    public RateLimiter kiwoomApiSafeRateLimiter() {
        return RateLimiter.create(1.5);
    }
}
