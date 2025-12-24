package com.stocktrading.kiwoom.domain.port.in;

import java.time.LocalDate;

import com.stocktrading.kiwoom.domain.model.InvestorChart;

import reactor.core.publisher.Flux;

/**
 * 투자자 기관별 차트 데이터 수집 UseCase (ka10060)
 */
public interface FetchInvestorChartUseCase {

    /**
     * 특정 종목의 투자자 차트 데이터 수집 (API 호출 후 DB 저장)
     */
    Flux<InvestorChart> fetchByStock(FetchInvestorChartCommand command);

    /**
     * 최근 30일 데이터 수집
     */
    Flux<InvestorChart> fetchRecent(String stockCode);

    /**
     * 연속조회로 전체 데이터 수집
     */
    Flux<InvestorChart> fetchAllWithContinuation(FetchInvestorChartCommand command);

    /**
     * 수집 명령
     */
    record FetchInvestorChartCommand(
            String stockCode,
            LocalDate date,
            String amtQtyTp,
            String trdeTp,
            InvestorChart.UnitType unitType) {
        public static FetchInvestorChartCommand of(String stockCode, LocalDate date) {
            return new FetchInvestorChartCommand(
                    stockCode,
                    date,
                    "2",  // 기본값: 수량
                    "0",  // 기본값: 순매수
                    InvestorChart.UnitType.SINGLE);
        }
    }
}
