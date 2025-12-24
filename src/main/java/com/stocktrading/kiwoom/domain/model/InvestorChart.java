package com.stocktrading.kiwoom.domain.model;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Value;

/**
 * 종목별 투자자 기관별 차트 도메인 모델 (ka10060)
 * 기관 세부 내역별 추이 포함
 */
@Value
@Builder
public class InvestorChart {

    // 식별자
    String stockCode;
    LocalDate date;

    UnitType unitType;

    // 시세 정보
    Long currentPrice;
    Long previousDayDifference;
    Long accumulatedTradeAmount;

    // 투자자별 데이터
    InvestorData investorData;

    // 기관 세부 내역
    InstitutionBreakdown institutionBreakdown;

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
     * 주요 투자자 데이터 (개인/외국인/기관계)
     */
    @Value
    @Builder
    public static class InvestorData {
        Long individual; // 개인투자자
        Long foreigner; // 외국인투자자
        Long institution; // 기관계
        Long nationalForeign; // 내외국인

        /**
         * 외국인 순매수 우위 여부
         */
        public boolean isForeignerNetBuy() {
            return foreigner != null && foreigner > 0;
        }

        /**
         * 기관 순매수 우위 여부
         */
        public boolean isInstitutionNetBuy() {
            return institution != null && institution > 0;
        }
    }

    /**
     * 기관 세부 내역 (금융투자, 보험, 투신 등)
     */
    @Value
    @Builder
    public static class InstitutionBreakdown {
        Long financialInvest; // 금융투자
        Long insurance; // 보험
        Long investment; // 투신
        Long etcFinancial; // 기타금융
        Long bank; // 은행
        Long pensionFund; // 연기금등
        Long privateFund; // 사모펀드
        Long nation; // 국가
        Long etcCorporation; // 기타법인

        /**
         * 기관 유형별 데이터 조회
         */
        public Long getByType(InstitutionType type) {
            return switch (type) {
                case FINANCIAL_INVEST -> financialInvest;
                case INSURANCE -> insurance;
                case INVESTMENT -> investment;
                case ETC_FINANCIAL -> etcFinancial;
                case BANK -> bank;
                case PENSION_FUND -> pensionFund;
                case PRIVATE_FUND -> privateFund;
                case NATION -> nation;
                case ETC_CORPORATION -> etcCorporation;
            };
        }

        /**
         * 가장 큰 순매수 기관 유형
         */
        public InstitutionType getTopBuyer() {
            InstitutionType topType = InstitutionType.FINANCIAL_INVEST;
            Long maxValue = financialInvest != null ? financialInvest : Long.MIN_VALUE;

            for (InstitutionType type : InstitutionType.values()) {
                Long value = getByType(type);
                if (value != null && value > maxValue) {
                    maxValue = value;
                    topType = type;
                }
            }
            return topType;
        }

        /**
         * 가장 큰 순매도 기관 유형
         */
        public InstitutionType getTopSeller() {
            InstitutionType topType = InstitutionType.FINANCIAL_INVEST;
            Long minValue = financialInvest != null ? financialInvest : Long.MAX_VALUE;

            for (InstitutionType type : InstitutionType.values()) {
                Long value = getByType(type);
                if (value != null && value < minValue) {
                    minValue = value;
                    topType = type;
                }
            }
            return topType;
        }
    }

    /**
     * 기관 세부 유형
     */
    public enum InstitutionType {
        FINANCIAL_INVEST("금융투자"),
        INSURANCE("보험"),
        INVESTMENT("투신"),
        ETC_FINANCIAL("기타금융"),
        BANK("은행"),
        PENSION_FUND("연기금등"),
        PRIVATE_FUND("사모펀드"),
        NATION("국가"),
        ETC_CORPORATION("기타법인");

        private final String description;

        InstitutionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 외국인/기관 동반 매수 여부
     */
    public boolean isForeignerAndInstitutionBuy() {
        return investorData.isForeignerNetBuy() && investorData.isInstitutionNetBuy();
    }

    /**
     * 연기금 순매수 여부
     */
    public boolean isPensionFundNetBuy() {
        return institutionBreakdown.pensionFund != null && institutionBreakdown.pensionFund > 0;
    }
}
