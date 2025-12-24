package com.stocktrading.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stocktrading.kiwoom.domain.model.InvestorChart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 종목별 투자자 기관별 차트 API 응답 DTO (ka10060)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestorChartResponse {

    private String stockCode;
    private List<ChartDataDto> chartData;
    private Integer returnCode;
    private String returnMsg;
    private List<String> logs;  // 처리 과정 로그
    private Integer dbSavedCount;  // DB 저장 건수
    // 수집 구간 요약 (요구사항 9)
    private LocalDate fromDate;   // 수집된 최소 일자
    private LocalDate toDate;     // 수집된 최대 일자
        // 추가: 저장/중복 구분 (요구사항 9 확장)
        private List<LocalDate> savedDates;       // 새로 저장된 일자 목록
        private List<LocalDate> duplicateDates;   // 중복(이미 존재)으로 저장되지 않은 일자 목록
        private Integer duplicateCount;           // 중복 건수

    /**
     * 도메인 모델 리스트 → DTO 변환
     */
    public static InvestorChartResponse from(String stockCode, List<InvestorChart> charts) {
        List<ChartDataDto> chartDataList = charts.stream()
                .map(ChartDataDto::from)
                .collect(Collectors.toList());

        LocalDate from = chartDataList.isEmpty() ? null :
                chartDataList.stream().map(ChartDataDto::getDate).min(LocalDate::compareTo).orElse(null);
        LocalDate to = chartDataList.isEmpty() ? null :
                chartDataList.stream().map(ChartDataDto::getDate).max(LocalDate::compareTo).orElse(null);

        return InvestorChartResponse.builder()
                .stockCode(stockCode)
                .chartData(chartDataList)
                .returnCode(0)
                .returnMsg("Success")
                .dbSavedCount(charts.size())
                .fromDate(from)
                .toDate(to)
                .savedDates(chartDataList.stream().map(ChartDataDto::getDate).collect(Collectors.toList()))
                .duplicateDates(java.util.Collections.emptyList())
                .duplicateCount(0)
                .build();
    }

    /**
     * 로그를 포함한 성공 응답 생성
     */
    public static InvestorChartResponse fromWithLogs(String stockCode, List<InvestorChart> charts, List<String> logs) {
        List<ChartDataDto> chartDataList = charts.stream()
                .map(ChartDataDto::from)
                .collect(Collectors.toList());

        LocalDate from = chartDataList.isEmpty() ? null :
                chartDataList.stream().map(ChartDataDto::getDate).min(LocalDate::compareTo).orElse(null);
        LocalDate to = chartDataList.isEmpty() ? null :
                chartDataList.stream().map(ChartDataDto::getDate).max(LocalDate::compareTo).orElse(null);

        return InvestorChartResponse.builder()
                .stockCode(stockCode)
                .chartData(chartDataList)
                .returnCode(0)
                .returnMsg("Success")
                .logs(logs)
                .dbSavedCount(charts.size())
                .fromDate(from)
                .toDate(to)
                .savedDates(chartDataList.stream().map(ChartDataDto::getDate).collect(Collectors.toList()))
                .duplicateDates(java.util.Collections.emptyList())
                .duplicateCount(0)
                .build();
    }

    /**
     * 오류 응답 생성
     */
    public static InvestorChartResponse error(String errorMessage) {
        return InvestorChartResponse.builder()
                .returnCode(-1)
                .returnMsg(errorMessage)
                .savedDates(java.util.Collections.emptyList())
                .duplicateDates(java.util.Collections.emptyList())
                .duplicateCount(0)
                .build();
    }

    /**
     * 오류 응답 생성 (로그 포함)
     */
    public static InvestorChartResponse errorWithLogs(String errorMessage, List<String> logs) {
        return InvestorChartResponse.builder()
                .returnCode(-1)
                .returnMsg(errorMessage)
                .logs(logs)
                .savedDates(java.util.Collections.emptyList())
                .duplicateDates(java.util.Collections.emptyList())
                .duplicateCount(0)
                .build();
    }

    /**
     * 저장/중복 상태를 반영한 커스텀 생성자
     */
    public static InvestorChartResponse fromWithStatus(String stockCode, List<InvestorChart> savedCharts,
                                                       List<InvestorChart> allParsedCharts,
                                                       List<LocalDate> duplicateDates,
                                                       List<String> logs) {
        List<ChartDataDto> chartDataList = savedCharts.stream()
                .map(ChartDataDto::from)
                .collect(Collectors.toList());

        LocalDate from = allParsedCharts.isEmpty() ? null :
                allParsedCharts.stream().map(InvestorChart::getDate).min(LocalDate::compareTo).orElse(null);
        LocalDate to = allParsedCharts.isEmpty() ? null :
                allParsedCharts.stream().map(InvestorChart::getDate).max(LocalDate::compareTo).orElse(null);

        return InvestorChartResponse.builder()
                .stockCode(stockCode)
                .chartData(chartDataList)
                .returnCode(0)
                .returnMsg("Success")
                .logs(logs)
                .dbSavedCount(savedCharts.size())
                .fromDate(from)
                .toDate(to)
                .savedDates(savedCharts.stream().map(InvestorChart::getDate).collect(Collectors.toList()))
                .duplicateDates(duplicateDates)
                .duplicateCount(duplicateDates.size())
                .build();
    }

    /**
     * 차트 데이터 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChartDataDto {
        private String stockCode;
        private LocalDate date;
        private String unitType;
        private Long currentPrice;
        private Long previousDayDifference;
        private Long accumulatedTradeAmount;

        // 주요 투자자
        private InvestorDataDto investor;

        // 기관 세부 내역
        private InstitutionBreakdownDto institutionBreakdown;

        public static ChartDataDto from(InvestorChart chart) {
            return ChartDataDto.builder()
                    .stockCode(chart.getStockCode())
                    .date(chart.getDate())
                    .unitType(chart.getUnitType().name())
                    .currentPrice(chart.getCurrentPrice())
                    .previousDayDifference(chart.getPreviousDayDifference())
                    .accumulatedTradeAmount(chart.getAccumulatedTradeAmount())
                    .investor(InvestorDataDto.from(chart.getInvestorData()))
                    .institutionBreakdown(InstitutionBreakdownDto.from(chart.getInstitutionBreakdown()))
                    .build();
        }
    }

    /**
     * 주요 투자자 DTO (개인/외국인/기관계)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvestorDataDto {
        private Long individual;        // 개인투자자
        private Long foreigner;         // 외국인투자자
        private Long institution;       // 기관계
        private Long nationalForeign;   // 내외국인

        public static InvestorDataDto from(InvestorChart.InvestorData data) {
            return InvestorDataDto.builder()
                    .individual(data.getIndividual())
                    .foreigner(data.getForeigner())
                    .institution(data.getInstitution())
                    .nationalForeign(data.getNationalForeign())
                    .build();
        }
    }

    /**
     * 기관 세부 내역 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InstitutionBreakdownDto {
        private Long financialInvest;   // 금융투자
        private Long insurance;         // 보험
        private Long investment;        // 투신
        private Long etcFinancial;      // 기타금융
        private Long bank;              // 은행
        private Long pensionFund;       // 연기금등
        private Long privateFund;       // 사모펀드
        private Long nation;            // 국가
        private Long etcCorporation;    // 기타법인

        public static InstitutionBreakdownDto from(InvestorChart.InstitutionBreakdown breakdown) {
            return InstitutionBreakdownDto.builder()
                    .financialInvest(breakdown.getFinancialInvest())
                    .insurance(breakdown.getInsurance())
                    .investment(breakdown.getInvestment())
                    .etcFinancial(breakdown.getEtcFinancial())
                    .bank(breakdown.getBank())
                    .pensionFund(breakdown.getPensionFund())
                    .privateFund(breakdown.getPrivateFund())
                    .nation(breakdown.getNation())
                    .etcCorporation(breakdown.getEtcCorporation())
                    .build();
        }
    }
}
