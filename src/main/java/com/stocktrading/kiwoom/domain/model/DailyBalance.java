package com.stocktrading.kiwoom.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 일별 잔고 도메인 모델
 */
@Value
@Builder
public class DailyBalance {

    LocalDate date;
    Long totalBuyAmount;
    Long totalEvaluationAmount;
    Long totalEvaluationProfit;
    BigDecimal totalProfitRate;
    Long depositBalance;
    Long dayStockAsset;
    BigDecimal buyWeight;

    @Singular
    List<BalanceItem> items;

    /**
     * 수익 중인지 확인
     */
    public boolean isProfitable() {
        return totalEvaluationProfit != null && totalEvaluationProfit > 0;
    }

    /**
     * 손실 중인지 확인
     */
    public boolean isLoss() {
        return totalEvaluationProfit != null && totalEvaluationProfit < 0;
    }

    /**
     * 목표 수익률 달성 여부 확인
     */
    public boolean hasReachedTargetProfitRate(BigDecimal targetRate) {
        if (totalProfitRate == null) {
            return false;
        }
        return totalProfitRate.compareTo(targetRate) >= 0;
    }

    /**
     * 특정 종목의 잔고 조회
     */
    public BalanceItem findItemByStockCode(String stockCode) {
        return items.stream()
                .filter(item -> item.getStockCode().equals(stockCode))
                .findFirst()
                .orElse(null);
    }

    /**
     * 잔고 항목
     */
    @Value
    @Builder
    public static class BalanceItem {
        String stockCode;
        String stockName;
        Long currentPrice;
        Long remainQuantity;
        Long buyUnitValue;
        BigDecimal buyWeight;
        Long evaluationProfit;
        BigDecimal profitRate;
        Long evaluationAmount;
        BigDecimal evaluationWeight;

        /**
         * 해당 종목이 수익 중인지 확인
         */
        public boolean isProfitable() {
            return evaluationProfit != null && evaluationProfit > 0;
        }

        /**
         * 해당 종목이 목표 수익률 달성했는지 확인
         */
        public boolean hasReachedTargetProfitRate(BigDecimal targetRate) {
            if (profitRate == null) {
                return false;
            }
            return profitRate.compareTo(targetRate) >= 0;
        }

        /**
         * 손절 기준 이하인지 확인
         */
        public boolean isBelowStopLoss(BigDecimal stopLossRate) {
            if (profitRate == null) {
                return false;
            }
            return profitRate.compareTo(stopLossRate) < 0;
        }
    }
}
