package com.stocktrading.kiwoom.domain.port.in;

import com.stocktrading.kiwoom.domain.model.InvestorChart;

import java.time.LocalDate;
import java.util.List;

/**
 * 투자자 기관별 차트 데이터 조회 UseCase (ka10060)
 */
public interface QueryInvestorChartUseCase {

    /**
     * 종목+일자로 조회
     */
    InvestorChart queryByStockAndDate(String stockCode, LocalDate date);

    /**
     * 종목+기간으로 조회 (추이 분석용)
     */
    List<InvestorChart> queryByStockAndPeriod(String stockCode, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 일자의 모든 종목 조회
     */
    List<InvestorChart> queryByDate(LocalDate date);

    /**
     * 종목의 최신 데이터 조회
     */
    InvestorChart queryLatestByStock(String stockCode);

    /**
     * 기관 세부 유형별 추이 조회
     */
    List<InstitutionTrend> queryInstitutionTrend(String stockCode, LocalDate startDate, LocalDate endDate);

    /**
     * 외국인+기관 동반 매수 종목 조회
     */
    List<InvestorChart> queryForeignerInstitutionBuyStocks(LocalDate date);

    /**
     * 연기금 순매수 종목 조회
     */
    List<InvestorChart> queryPensionFundBuyStocks(LocalDate date);

    /**
     * 종목의 날짜 범위 조회 (최소, 최대)
     */
    StockDateRange queryDateRangeByStock(String stockCode);

    /**
     * 기관 세부 추이 데이터
     */
    record InstitutionTrend(
            LocalDate date,
            Long financialInvest,
            Long insurance,
            Long investment,
            Long etcFinancial,
            Long bank,
            Long pensionFund,
            Long privateFund,
            Long nation,
            Long etcCorporation
    ) {}

    /**
     * 종목 날짜 범위
     */
    record StockDateRange(LocalDate startDate, LocalDate endDate) {}
}
