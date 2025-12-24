package com.stocktrading.kiwoom.dto;

import com.stocktrading.kiwoom.domain.port.in.QueryInvestorChartUseCase.InstitutionTrend;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 기관 세부 유형별 추이 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionTrendResponse {

    private String stockCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<TrendDataDto> trends;

    public static InstitutionTrendResponse from(
            String stockCode, LocalDate startDate, LocalDate endDate, List<InstitutionTrend> trends) {

        List<TrendDataDto> trendDataList = trends.stream()
                .map(TrendDataDto::from)
                .collect(Collectors.toList());

        return InstitutionTrendResponse.builder()
                .stockCode(stockCode)
                .startDate(startDate)
                .endDate(endDate)
                .trends(trendDataList)
                .build();
    }

    /**
     * 일별 추이 데이터
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendDataDto {
        private LocalDate date;
        private Long financialInvest;   // 금융투자
        private Long insurance;         // 보험
        private Long investment;        // 투신
        private Long etcFinancial;      // 기타금융
        private Long bank;              // 은행
        private Long pensionFund;       // 연기금등
        private Long privateFund;       // 사모펀드
        private Long nation;            // 국가
        private Long etcCorporation;    // 기타법인

        public static TrendDataDto from(InstitutionTrend trend) {
            return TrendDataDto.builder()
                    .date(trend.date())
                    .financialInvest(trend.financialInvest())
                    .insurance(trend.insurance())
                    .investment(trend.investment())
                    .etcFinancial(trend.etcFinancial())
                    .bank(trend.bank())
                    .pensionFund(trend.pensionFund())
                    .privateFund(trend.privateFund())
                    .nation(trend.nation())
                    .etcCorporation(trend.etcCorporation())
                    .build();
        }
    }
}
