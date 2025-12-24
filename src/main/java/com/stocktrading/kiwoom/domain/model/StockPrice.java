package com.stocktrading.kiwoom.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주가 정보 도메인 모델
 */
@Value
@Builder
public class StockPrice {

    String stockCode;
    String stockName;
    Long currentPrice;
    Long openPrice;
    Long highPrice;
    Long lowPrice;
    Long previousClosePrice;
    Long volume;
    Long tradingValue;
    BigDecimal changeRate;
    Long changeAmount;
    LocalDateTime timestamp;

    /**
     * 상승장인지 확인
     */
    public boolean isRising() {
        return changeAmount != null && changeAmount > 0;
    }

    /**
     * 하락장인지 확인
     */
    public boolean isFalling() {
        return changeAmount != null && changeAmount < 0;
    }

    /**
     * 보합인지 확인
     */
    public boolean isUnchanged() {
        return changeAmount != null && changeAmount == 0;
    }

    /**
     * 변동률이 특정 퍼센트 이상인지 확인
     */
    public boolean isVolatilityAbove(BigDecimal threshold) {
        if (changeRate == null) {
            return false;
        }
        return changeRate.abs().compareTo(threshold) > 0;
    }

    /**
     * 거래량이 특정 값 이상인지 확인
     */
    public boolean isVolumeAbove(Long threshold) {
        return volume != null && volume > threshold;
    }
}
