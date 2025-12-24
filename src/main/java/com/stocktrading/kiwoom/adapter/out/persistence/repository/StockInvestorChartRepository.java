package com.stocktrading.kiwoom.adapter.out.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorChart;
import com.stocktrading.kiwoom.adapter.out.persistence.entity.StockInvestorChartId;

/**
 * 종목별 투자자 기관별 차트 Repository (ka10060)
 */
@Repository
public interface StockInvestorChartRepository extends JpaRepository<StockInvestorChart, StockInvestorChartId> {

       /**
        * 종목+일자로 조회
        */
       Optional<StockInvestorChart> findByStkCdAndDt(String stkCd, LocalDate dt);

       /**
        * 종목+기간으로 조회 (날짜 오름차순)
        */
       @Query("SELECT c FROM StockInvestorChart c " +
                     "WHERE c.stkCd = :stkCd " +
                     "AND c.dt BETWEEN :startDate AND :endDate " +
                     "ORDER BY c.dt ASC")
       List<StockInvestorChart> findByStkCdAndDtBetweenOrderByDtAsc(
                     @Param("stkCd") String stkCd,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       /**
        * 특정 일자 전체 조회
        */
       List<StockInvestorChart> findByDt(LocalDate dt);

       /**
        * 종목의 최신 데이터 조회
        */
       Optional<StockInvestorChart> findFirstByStkCdOrderByDtDesc(String stkCd);

       /**
        * 외국인+기관 동반 매수 종목 조회
        */
       @Query("SELECT c FROM StockInvestorChart c " +
                     "WHERE c.dt = :date " +
                     "AND c.frgnrInvsr > 0 " +
                     "AND c.orgn > 0 " +
                     "ORDER BY c.frgnrInvsr + c.orgn DESC")
       List<StockInvestorChart> findForeignerInstitutionBuyStocks(@Param("date") LocalDate date);

       /**
        * 연기금 순매수 종목 조회
        */
       @Query("SELECT c FROM StockInvestorChart c " +
                     "WHERE c.dt = :date " +
                     "AND c.penfndEtc > 0 " +
                     "ORDER BY c.penfndEtc DESC")
       List<StockInvestorChart> findPensionFundBuyStocks(@Param("date") LocalDate date);

       /**
        * 존재 여부 확인
        */
       boolean existsByStkCdAndDt(String stkCd, LocalDate dt);

       /**
        * 삭제
        */
       void deleteByStkCdAndDt(String stkCd, LocalDate dt);

       /**
        * 기관 세부 유형별 상위 종목 조회 (금융투자)
        */
       @Query("SELECT c FROM StockInvestorChart c " +
                     "WHERE c.dt = :date " +
                     "AND c.fnncInvt > 0 " +
                     "ORDER BY c.fnncInvt DESC")
       List<StockInvestorChart> findTopByFinancialInvest(@Param("date") LocalDate date);

       /**
        * 기관 세부 유형별 상위 종목 조회 (보험)
        */
       @Query("SELECT c FROM StockInvestorChart c " +
                     "WHERE c.dt = :date " +
                     "AND c.insrnc > 0 " +
                     "ORDER BY c.insrnc DESC")
       List<StockInvestorChart> findTopByInsurance(@Param("date") LocalDate date);

       /**
        * 기관 세부 유형별 상위 종목 조회 (투신)
        */
       @Query("SELECT c FROM StockInvestorChart c " +
                     "WHERE c.dt = :date " +
                     "AND c.invtrt > 0 " +
                     "ORDER BY c.invtrt DESC")
       List<StockInvestorChart> findTopByInvestment(@Param("date") LocalDate date);

       /**
        * 종목의 최소 날짜 조회
        */
       @Query("SELECT MIN(c.dt) FROM StockInvestorChart c WHERE c.stkCd = :stkCd")
       Optional<LocalDate> findMinDateByStock(@Param("stkCd") String stkCd);

       /**
        * 종목의 최대 날짜 조회
        */
       @Query("SELECT MAX(c.dt) FROM StockInvestorChart c WHERE c.stkCd = :stkCd")
       Optional<LocalDate> findMaxDateByStock(@Param("stkCd") String stkCd);

       /**
        * 여러 종목의 날짜 범위 일괄 조회 (성능 최적화용)
        * 
        * @return Object[0]=stkCd, Object[1]=minDate, Object[2]=maxDate
        */
       @Query("SELECT c.stkCd, MIN(c.dt), MAX(c.dt) FROM StockInvestorChart c " +
                     "WHERE c.stkCd IN :stockCodes GROUP BY c.stkCd")
       List<Object[]> findDateRangesByStocks(@Param("stockCodes") List<String> stockCodes);

       /**
        * 모든 종목 코드 조회 (DISTINCT)
        */
       @Query("SELECT DISTINCT c.stkCd FROM StockInvestorChart c")
       List<String> findDistinctStockCodes();
}
