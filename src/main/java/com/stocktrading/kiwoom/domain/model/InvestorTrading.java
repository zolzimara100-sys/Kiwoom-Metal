package com.stocktrading.kiwoom.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 투자자별 거래 도메인 모델 (순수 비즈니스 로직)
 * Infrastructure(JPA)와 독립적
 */
@Value
@Builder
public class InvestorTrading {

    // 식별자
    String stockCode;
    LocalDate date;
    TradeType tradeType;
    AmountQuantityType amountQuantityType;

    // 단위
    UnitType unitType;

    // 시세 정보
    Long currentPrice;
    PriceSign priceSign;
    Long previousDayDifference;
    BigDecimal fluctuationRate;
    Long accumulatedTradeQuantity;
    Long accumulatedTradeAmount;

    // 투자자별 데이터
    InvestorBreakdown investorBreakdown;

    /**
     * 매매구분
     */
    public enum TradeType {
        NET_BUY("0", "순매수"),
        BUY("1", "매수"),
        SELL("2", "매도");

        private final String code;
        private final String description;

        TradeType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static TradeType fromCode(String code) {
            for (TradeType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid trade type code: " + code);
        }
    }

    /**
     * 금액수량구분
     */
    public enum AmountQuantityType {
        AMOUNT("1", "금액"),
        QUANTITY("2", "수량");

        private final String code;
        private final String description;

        AmountQuantityType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static AmountQuantityType fromCode(String code) {
            for (AmountQuantityType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid amount quantity type code: " + code);
        }
    }

    /**
     * 단위구분
     */
    public enum UnitType {
        SINGLE("1", "단주"),
        THOUSAND("1000", "천주");

        private final String code;
        private final String description;

        UnitType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static UnitType fromCode(String code) {
            for (UnitType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid unit type code: " + code);
        }
    }

    /**
     * 대비기호
     */
    public enum PriceSign {
        UPPER_LIMIT("1", "상한"),
        UP("2", "상승"),
        UNCHANGED("3", "보합"),
        DOWN("4", "하한"),
        LOWER_LIMIT("5", "하한");

        private final String code;
        private final String description;

        PriceSign(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static PriceSign fromCode(String code) {
            for (PriceSign sign : values()) {
                if (sign.code.equals(code)) {
                    return sign;
                }
            }
            throw new IllegalArgumentException("Invalid price sign code: " + code);
        }
    }

    /**
     * 투자자 분류별 데이터
     */
    @Value
    @Builder
    public static class InvestorBreakdown {
        Long individual;        // 개인투자자
        Long foreigner;         // 외국인투자자
        Long institution;       // 기관계
        Long financialInvest;   // 금융투자
        Long insurance;         // 보험
        Long investment;        // 투신
        Long etcFinancial;      // 기타금융
        Long bank;              // 은행
        Long pensionFund;       // 연기금등
        Long privateFund;       // 사모펀드
        Long nation;            // 국가
        Long etcCorporation;    // 기타법인
        Long nationalForeign;   // 내외국인

        /**
         * 특정 투자자 타입의 데이터 조회
         */
        public Long getByType(InvestorType type) {
            return switch (type) {
                case INDIVIDUAL -> individual;
                case FOREIGNER -> foreigner;
                case INSTITUTION -> institution;
                case FINANCIAL_INVEST -> financialInvest;
                case INSURANCE -> insurance;
                case INVESTMENT -> investment;
                case ETC_FINANCIAL -> etcFinancial;
                case BANK -> bank;
                case PENSION_FUND -> pensionFund;
                case PRIVATE_FUND -> privateFund;
                case NATION -> nation;
                case ETC_CORPORATION -> etcCorporation;
                case NATIONAL_FOREIGN -> nationalForeign;
            };
        }
    }

    /**
     * 투자자 타입
     */
    public enum InvestorType {
        INDIVIDUAL("개인투자자"),
        FOREIGNER("외국인투자자"),
        INSTITUTION("기관계"),
        FINANCIAL_INVEST("금융투자"),
        INSURANCE("보험"),
        INVESTMENT("투신"),
        ETC_FINANCIAL("기타금융"),
        BANK("은행"),
        PENSION_FUND("연기금등"),
        PRIVATE_FUND("사모펀드"),
        NATION("국가"),
        ETC_CORPORATION("기타법인"),
        NATIONAL_FOREIGN("내외국인");

        private final String description;

        InvestorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 순매수 금액이 양수인지 확인
     */
    public boolean isNetBuyPositive() {
        return tradeType == TradeType.NET_BUY &&
               investorBreakdown.individual != null &&
               investorBreakdown.individual > 0;
    }

    /**
     * 외국인 매수 우위인지 확인
     */
    public boolean isForeignerBuyDominant() {
        return investorBreakdown.foreigner != null &&
               investorBreakdown.foreigner > 0;
    }
}
