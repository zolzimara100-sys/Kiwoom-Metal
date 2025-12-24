package com.stocktrading.kiwoom.domain.port.out;

import com.stocktrading.kiwoom.domain.model.InvestorChart;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 투자자 기관별 차트 저장소 Port (ka10060)
 */
public interface InvestorChartPort {

    /**
     * 저장 (upsert)
     */
    InvestorChart save(InvestorChart chart);

    /**
     * 다건 저장
     */
    List<InvestorChart> saveAll(List<InvestorChart> charts);

    /**
     * 종목+일자로 조회
     */
    Optional<InvestorChart> findByStockAndDate(String stockCode, LocalDate date);

    /**
     * 종목+기간으로 조회
     */
    List<InvestorChart> findByStockAndPeriod(String stockCode, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 일자 전체 조회
     */
    List<InvestorChart> findByDate(LocalDate date);

    /**
     * 종목의 최신 데이터 조회
     */
    Optional<InvestorChart> findLatestByStock(String stockCode);

    /**
     * 외국인+기관 동반 매수 종목 조회
     */
    List<InvestorChart> findForeignerInstitutionBuyStocks(LocalDate date);

    /**
     * 연기금 순매수 종목 조회
     */
    List<InvestorChart> findPensionFundBuyStocks(LocalDate date);

    /**
     * 존재 여부 확인
     */
    boolean existsByStockAndDate(String stockCode, LocalDate date);

    /**
     * 삭제
     */
    void deleteByStockAndDate(String stockCode, LocalDate date);

    /**
     * 종목의 날짜 범위 조회 (최소, 최대)
     */
    DateRange findDateRangeByStock(String stockCode);

    /**
     * 여러 종목의 날짜 범위 일괄 조회 (최적화용)
     * @param stockCodes 종목코드 목록
     * @return 종목코드 → DateRange 맵
     */
    java.util.Map<String, DateRange> findDateRangesByStocks(List<String> stockCodes);

    /**
     * 날짜 범위 데이터 클래스
     */
    record DateRange(LocalDate minDate, LocalDate maxDate) {}
}
